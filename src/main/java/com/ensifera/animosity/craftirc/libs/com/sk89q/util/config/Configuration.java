package com.ensifera.animosity.craftirc.libs.com.sk89q.util.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * YAML configuration loader. To use this class, construct it with path to
 * a file and call its load() method. For specifying node paths in the
 * various get*() methods, they support SK's path notation, allowing you to
 * select child nodes by delimiting node names with periods.
 *
 * <p>
 * For example, given the following configuration file:</p>
 *
 * <pre>members:
 *     - Hollie
 *     - Jason
 *     - Bobo
 *     - Aya
 *     - Tetsu
 * worldguard:
 *     fire:
 *         spread: false
 *         blocks: [cloth, rock, glass]
 * sturmeh:
 *     cool: false
 *     eats:
 *         babies: true</pre>
 *
 * <p>Calling code could access sturmeh's baby eating state by using
 * <code>getBoolean("sturmeh.eats.babies", false)</code>. For lists, there are
 * methods such as <code>getStringList</code> that will return a type safe list.
 */

public class Configuration extends ConfigurationNode {
    private final Yaml yaml;
    private final File file;

    public Configuration(File file) {
        super(new HashMap<String, Object>());

        DumperOptions options = new DumperOptions();

        options.setIndent(4);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        yaml = new Yaml(new SafeConstructor(), new EmptyNullRepresenter(), options);

        this.file = file;
    }

    /**
     * Loads the configuration file. All errors are thrown away.
     */
    public void load() {
        FileInputStream stream = null;

        try {
            stream = new FileInputStream(file);
            read(yaml.load(new UnicodeReader(stream)));
        } catch (IOException | ConfigurationException e) {
            root = new HashMap<>();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void read(Object input) throws ConfigurationException {
        try {
            if (null == input) {
                root = new HashMap<>();
            } else {
                root = (Map<String, Object>) input;
            }
        } catch (ClassCastException e) {
            throw new ConfigurationException("Root document must be an key-value structure");
        }
    }

    /**
     * This method returns an empty ConfigurationNode for using as a
     * default in methods that select a node from a node list.
     *
     * @return The empty node.
     */
    public static ConfigurationNode getEmptyNode() {
        return new ConfigurationNode(new HashMap<String, Object>());
    }
}
