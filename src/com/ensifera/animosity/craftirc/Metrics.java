package com.ensifera.animosity.craftirc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;

public class Metrics {

    /**
     * The current revision number
     */
    private final static int REVISION = 5;

    /**
     * The base url of the metrics domain
     */
    private static final String BASE_URL = "http://metrics.griefcraft.com";

    /**
     * The url used to report a server's status
     */
    private static final String REPORT_URL = "/report/%s";

    /**
     * The file where guid and opt out is stored in
     */
    private static final String CONFIG_FILE = "plugins/PluginMetrics/config.yml";

    /**
     * Interval of time to ping (in minutes)
     */
    private final static int PING_INTERVAL = 10;

    /**
     * Encode text as UTF-8
     * 
     * @param text
     * @return
     */
    private static String encode(String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, "UTF-8");
    }

    /**
     * <p>
     * Encode a key/value data pair to be used in a HTTP post request. This INCLUDES a & so the first key/value pair MUST be included manually, e.g:
     * </p>
     * <code>
     * String httpData = encode("guid") + '=' + encode("1234") + encodeDataPair("authors") + "..";
     * </code>
     * 
     * @param key
     * @param value
     * @return
     */
    private static String encodeDataPair(String key, String value) throws UnsupportedEncodingException {
        return '&' + Metrics.encode(key) + '=' + Metrics.encode(value);
    }

    /**
     * The plugin this metrics submits for
     */
    private final Plugin plugin;

    /**
     * The plugin configuration file
     */
    private final YamlConfiguration configuration;

    /**
     * Unique server id
     */
    private final String guid;

    public Metrics(Plugin plugin) throws IOException {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        this.plugin = plugin;

        // load the config
        final File file = new File(Metrics.CONFIG_FILE);
        this.configuration = YamlConfiguration.loadConfiguration(file);

        // add some defaults
        this.configuration.addDefault("opt-out", false);
        this.configuration.addDefault("guid", UUID.randomUUID().toString());

        // Do we need to create the file?
        if (this.configuration.get("guid", null) == null) {
            this.configuration.options().header("http://metrics.griefcraft.com").copyDefaults(true);
            this.configuration.save(file);
        }

        // Load the guid then
        this.guid = this.configuration.getString("guid");
    }

    /**
     * Start measuring statistics. This will immediately create an async repeating task as the plugin and send
     * the initial data to the metrics backend, and then after that it will post in increments of
     * PING_INTERVAL * 1200 ticks.
     */
    public void start() {
        // Did we opt out?
        if (this.configuration.getBoolean("opt-out", false)) {
            return;
        }

        // Begin hitting the server with glorious data
        this.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, new Runnable() {
            private boolean firstPost = true;

            public void run() {
                try {
                    // We use the inverse of firstPost because if it is the first time we are posting,
                    // it is not a interval ping, so it evaluates to FALSE
                    // Each time thereafter it will evaluate to TRUE, i.e PING!
                    Metrics.this.postPlugin(!this.firstPost);

                    // After the first post we set firstPost to false
                    // Each post thereafter will be a ping
                    this.firstPost = false;
                } catch (final IOException e) {
                    System.err.println("[Metrics] " + e.getMessage());
                }
            }
        }, 0, Metrics.PING_INTERVAL * 1200);
    }

    /**
     * Check if mineshafter is present. If it is, we need to bypass it to send POST requests
     * 
     * @return
     */
    private boolean isMineshafterPresent() {
        try {
            Class.forName("mineshafter.MineServer");
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Generic method that posts a plugin to the metrics website
     */
    private void postPlugin(boolean isPing) throws IOException {
        // The plugin's description file containg all of the plugin data such as name, version, author, etc
        final PluginDescriptionFile description = this.plugin.getDescription();

        // Construct the post data
        String data = Metrics.encode("guid") + '=' + Metrics.encode(this.guid) + Metrics.encodeDataPair("version", description.getVersion()) + Metrics.encodeDataPair("server", Bukkit.getVersion()) + Metrics.encodeDataPair("players", Integer.toString(Bukkit.getServer().getOnlinePlayers().length)) + Metrics.encodeDataPair("revision", String.valueOf(Metrics.REVISION));

        // If we're pinging, append it
        if (isPing) {
            data += Metrics.encodeDataPair("ping", "true");
        }

        // Create the url
        final URL url = new URL(Metrics.BASE_URL + String.format(Metrics.REPORT_URL, this.plugin.getDescription().getName()));

        // Connect to the website
        URLConnection connection;

        // Mineshafter creates a socks proxy, so we can safely bypass it
        // It does not reroute POST requests so we need to go around it
        if (this.isMineshafterPresent()) {
            connection = url.openConnection(Proxy.NO_PROXY);
        } else {
            connection = url.openConnection();
        }

        connection.setDoOutput(true);

        // Write the data
        final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(data);
        writer.flush();

        // Now read the response
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final String response = reader.readLine();

        // close resources
        writer.close();
        reader.close();

        if (response.startsWith("ERR")) {
            throw new IOException(response); //Throw the exception
        }
        //if (response.startsWith("OK")) - We should get "OK" followed by an optional description if everything goes right
    }

}