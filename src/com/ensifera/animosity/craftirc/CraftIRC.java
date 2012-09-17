package com.ensifera.animosity.craftirc;

import java.io.*;
import java.lang.StringBuilder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.ensifera.animosity.craftirc.libs.com.sk89q.util.config.Configuration;
import com.ensifera.animosity.craftirc.libs.com.sk89q.util.config.ConfigurationNode;

/**
 * @author Animosity
 * @author ricin
 * @author Protected
 * @author mbaxter
 * 
 */

//TODO: Better handling of null method returns (try to crash the bot and then stop that from happening again)
public class CraftIRC extends JavaPlugin {

    public static final String NAME = "CraftIRC";
    public static String VERSION;
    static final Logger log = Logger.getLogger("Minecraft");

    Configuration configuration;

    //Misc class attributes
    PluginDescriptionFile desc = null;
    public Server server = null;
    private final CraftIRCListener listener = new CraftIRCListener(this);
    private final ConsoleListener sayListener = new ConsoleListener(this);
    private ArrayList<Minebot> instances;
    private boolean debug;
    private Timer holdTimer = new Timer();
    private Timer retryTimer = new Timer();
    Map<HoldType, Boolean> hold;
    Map<String, RetryTask> retry;

    //Bots and channels config storage
    private List<ConfigurationNode> bots;
    private List<ConfigurationNode> colormap;
    private Map<Integer, ArrayList<ConfigurationNode>> channodes;
    private Map<Path, ConfigurationNode> paths;

    //Endpoints
    private Map<String, EndPoint> endpoints;
    private Map<EndPoint, String> tags;
    private Map<String, CommandEndPoint> irccmds;
    private Map<String, List<String>> taggroups;
    private Chat vault;
    
    //Replacement Filters
    private Map<String, Map<String, String>> replaceFilters;
    private boolean cancelChat;

    static void dolog(String message) {
        CraftIRC.log.info("[" + CraftIRC.NAME + "] " + message);
    }

    static void dowarn(String message) {
        CraftIRC.log.log(Level.WARNING, "[" + CraftIRC.NAME + "] " + message);
    }

    /***************************
     * Bukkit stuff
     ***************************/

    @Override
    public void onEnable() {
        try {
        	//Checking if the configuration file exists and imports the default one from the .jar if it doesn't
            final File configFile = new File(this.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                this.saveDefaultConfig();
                this.autoDisable();
                return;
            }
            this.configuration = new Configuration(configFile);
            this.configuration.load();
            this.cancelChat = this.configuration.getBoolean("settings.cancel-chat", false);

            this.endpoints = new HashMap<String, EndPoint>();
            this.tags = new HashMap<EndPoint, String>();
            this.irccmds = new HashMap<String, CommandEndPoint>();
            this.taggroups = new HashMap<String, List<String>>();

            final PluginDescriptionFile desc = this.getDescription();
            CraftIRC.VERSION = desc.getVersion();
            this.server = this.getServer();
            this.bots = new ArrayList<ConfigurationNode>(this.configuration.getNodeList("bots", null));
            this.channodes = new HashMap<Integer, ArrayList<ConfigurationNode>>();
            for (int botID = 0; botID < this.bots.size(); botID++) {
                this.channodes.put(botID, new ArrayList<ConfigurationNode>(this.bots.get(botID).getNodeList("channels", null)));
            }

            this.colormap = new ArrayList<ConfigurationNode>(this.configuration.getNodeList("colormap", null));

            this.paths = new HashMap<Path, ConfigurationNode>();
            for (final ConfigurationNode path : this.configuration.getNodeList("paths", new LinkedList<ConfigurationNode>())) {
                final Path identifier = new Path(path.getString("source"), path.getString("target"));
                if (!identifier.getSourceTag().equals(identifier.getTargetTag()) && !this.paths.containsKey(identifier)) {
                    this.paths.put(identifier, path);
                }
            }
            
            //Replace filters
            this.replaceFilters = new HashMap<String, Map<String, String>>();
            try {
                for (String key : this.configuration.getNode("filters").getKeys()) {
                    //Map key to regex pattern, value to replacement.
                    Map<String, String> replaceMap = new HashMap<String, String>();
                    this.replaceFilters.put(key, replaceMap);
                    for (ConfigurationNode fieldNode : this.configuration.getNodeList("filters." + key, null)) {
                        Map<String, Object> patterns = fieldNode.getAll();
                        if (patterns != null)
                            for (String pattern : patterns.keySet())
                                replaceMap.put(pattern, patterns.get(pattern).toString());
                    }
                    
                    //Also supports non-map entries.
                    for (String unMappedEntry : this.configuration.getStringList("filters." + key, null))
                        if (unMappedEntry.length() > 0 && unMappedEntry.charAt(0) != '{') //mapped toString() begins with {, but regex can't begin with {.
                            replaceMap.put(unMappedEntry, "");
                }
            } catch (NullPointerException e) {
            }

            //Retry timers
            this.retry = new HashMap<String, RetryTask>();
            this.retryTimer = new Timer();

            //Event listeners
            this.getServer().getPluginManager().registerEvents(this.listener, this);
            this.getServer().getPluginManager().registerEvents(this.sayListener, this);

            //Native endpoints!
            if ((this.cMinecraftTag() != null) && !this.cMinecraftTag().equals("")) {
                this.registerEndPoint(this.cMinecraftTag(), new MinecraftPoint(this, this.getServer())); //The minecraft server, no bells and whistles
                for (final String cmd : this.cCmdWordSay(null)) {
                    this.registerCommand(this.cMinecraftTag(), cmd);
                }
                for (final String cmd : this.cCmdWordPlayers(null)) {
                    this.registerCommand(this.cMinecraftTag(), cmd);
                }
                if (!this.cMinecraftTagGroup().equals("")) {
                    this.groupTag(this.cMinecraftTag(), this.cMinecraftTagGroup());
                }
            }
            if ((this.cCancelledTag() != null) && !this.cCancelledTag().equals("")) {
                this.registerEndPoint(this.cCancelledTag(), new MinecraftPoint(this, this.getServer())); //Handles cancelled chat
                if (!this.cMinecraftTagGroup().equals("")) {
                    this.groupTag(this.cCancelledTag(), this.cMinecraftTagGroup());
                }
            }
            if ((this.cConsoleTag() != null) && !this.cConsoleTag().equals("")) {
                this.registerEndPoint(this.cConsoleTag(), new ConsolePoint(this, this.getServer())); //The minecraft console
                for (final String cmd : this.cCmdWordCmd(null)) {
                    this.registerCommand(this.cConsoleTag(), cmd);
                }
                if (!this.cMinecraftTagGroup().equals("")) {
                    this.groupTag(this.cConsoleTag(), this.cMinecraftTagGroup());
                }
            }

            //Create bots
            this.instances = new ArrayList<Minebot>();
            for (int i = 0; i < this.bots.size(); i++) {
                this.instances.add(new Minebot(this, i, this.cDebug()));
            }

            this.loadTagGroups();

            CraftIRC.dolog("Enabled.");

            //Hold timers
            this.hold = new HashMap<HoldType, Boolean>();
            this.holdTimer = new Timer();
            if (this.cHold("chat") > 0) {
                this.hold.put(HoldType.CHAT, true);
                this.holdTimer.schedule(new RemoveHoldTask(this, HoldType.CHAT), this.cHold("chat"));
            } else {
                this.hold.put(HoldType.CHAT, false);
            }
            if (this.cHold("joins") > 0) {
                this.hold.put(HoldType.JOINS, true);
                this.holdTimer.schedule(new RemoveHoldTask(this, HoldType.JOINS), this.cHold("joins"));
            } else {
                this.hold.put(HoldType.JOINS, false);
            }
            if (this.cHold("quits") > 0) {
                this.hold.put(HoldType.QUITS, true);
                this.holdTimer.schedule(new RemoveHoldTask(this, HoldType.QUITS), this.cHold("quits"));
            } else {
                this.hold.put(HoldType.QUITS, false);
            }
            if (this.cHold("kicks") > 0) {
                this.hold.put(HoldType.KICKS, true);
                this.holdTimer.schedule(new RemoveHoldTask(this, HoldType.KICKS), this.cHold("kicks"));
            } else {
                this.hold.put(HoldType.KICKS, false);
            }
            if (this.cHold("bans") > 0) {
                this.hold.put(HoldType.BANS, true);
                this.holdTimer.schedule(new RemoveHoldTask(this, HoldType.BANS), this.cHold("bans"));
            } else {
                this.hold.put(HoldType.BANS, false);
            }
            if (this.cHold("deaths") > 0) {
                this.hold.put(HoldType.DEATHS, true);
                this.holdTimer.schedule(new RemoveHoldTask(this, HoldType.DEATHS), this.cHold("deaths"));
            } else {
                this.hold.put(HoldType.DEATHS, false);
            }

            this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    if (CraftIRC.this.getServer().getPluginManager().isPluginEnabled("Vault")) {
                        try {
                            CraftIRC.this.vault = CraftIRC.this.getServer().getServicesManager().getRegistration(Chat.class).getProvider();
                        } catch (final Exception e) {

                        }
                    }
                }
            });

            this.setDebug(this.cDebug());
        } catch (final Exception e) {
            e.printStackTrace();
        }
        try {
            new Metrics(this).start();
        } catch (final IOException e) {
            //Meh.
        }
    }

    private void autoDisable() {
        CraftIRC.dolog("Auto-disabling...");
        this.getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable() {
        try {
            this.retryTimer.cancel();
            this.holdTimer.cancel();
            //Disconnect bots
            if (this.bots != null) {
                for (int i = 0; i < this.bots.size(); i++) {
                    this.instances.get(i).disconnect();
                    this.instances.get(i).dispose();
                }
            }
            CraftIRC.dolog("Disabled.");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /***************************
     * Minecraft command handling
     ***************************/

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        final String commandName = command.getName().toLowerCase();

        try {
            if (commandName.equals("ircsay")){
                if (!sender.hasPermission("craftirc." + commandName)) {
                    return false;
                }
                return this.cmdMsgSay(sender,args);
            }
            if (commandName.equals("ircmsg")) {
                if (!sender.hasPermission("craftirc." + commandName)) {
                    return false;
                }
                return this.cmdMsgToTag(sender, args);
            } else if (commandName.equals("ircmsguser")) {
                if (!sender.hasPermission("craftirc." + commandName)) {
                    return false;
                }
                return this.cmdMsgToUser(sender, args);
            } else if (commandName.equals("ircusers")) {
                if (!sender.hasPermission("craftirc." + commandName)) {
                    return false;
                }
                return this.cmdGetUserList(sender, args);
            } else if (commandName.equals("admins!")) {
                if (!sender.hasPermission("craftirc.admins")) {
                    return false;
                }
                return this.cmdNotifyIrcAdmins(sender, args);
            } else if (commandName.equals("ircraw")) {
                if (!sender.hasPermission("craftirc." + commandName)) {
                    return false;
                }
                return this.cmdRawIrcCommand(sender, args);
            } else if (commandName.equals("ircreload")) {
                if (!sender.hasPermission("craftirc." + commandName)) {
                    return false;
                }
                this.getServer().getPluginManager().disablePlugin(this);
                this.getServer().getPluginManager().enablePlugin(this);
                return true;
            } else {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean cmdMsgSay(CommandSender sender, String[] args) {
        try{
            RelayedMessage msg = this.newMsg(this.getEndPoint(this.cMinecraftTag()), null, "chat");
            if (msg == null) {
                return true;
            }
            String senderName=sender.getName();
            String world="";
            String prefix="";
            String suffix="";
            if(sender instanceof Player){
                Player player = (Player) sender;
                senderName = player.getDisplayName();
                world = player.getWorld().getName();
                prefix = this.getPrefix(player);
                suffix = this.getSuffix(player);
            }
            msg.setField("sender", senderName);
            msg.setField("message", Util.combineSplit(0, args, " "));
            msg.setField("world", world);
            msg.setField("realSender", sender.getName());
            msg.setField("prefix", prefix);
            msg.setField("suffix", suffix);
            msg.doNotColor("message");
            msg.post();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdMsgToTag(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) {
                CraftIRC.dolog("CraftIRCListener cmdMsgToAll()");
            }
            if (args.length < 2) {
                return false;
            }
            final String msgToSend = Util.combineSplit(1, args, " ");
            final RelayedMessage msg = this.newMsg(this.getEndPoint(this.cMinecraftTag()), this.getEndPoint(args[0]), "chat");
            if (msg == null) {
                return true;
            }
            if (sender instanceof Player) {
                msg.setField("sender", ((Player) sender).getDisplayName());
            } else {
                msg.setField("sender", sender.getName());
            }
            msg.setField("message", msgToSend);
            msg.doNotColor("message");
            msg.post();
            sender.sendMessage("Message sent.");
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdMsgToUser(CommandSender sender, String[] args) {
        try {
            if (args.length < 3) {
                return false;
            }
            final String msgToSend = Util.combineSplit(2, args, " ");
            final RelayedMessage msg = this.newMsg(this.getEndPoint(this.cMinecraftTag()), this.getEndPoint(args[0]), "private");
            if (msg == null) {
                return true;
            }
            if (sender instanceof Player) {
                msg.setField("sender", ((Player) sender).getDisplayName());
            } else {
                msg.setField("sender", sender.getName());
            };
            msg.setField("message", msgToSend);
            msg.doNotColor("message");
            boolean sameEndPoint = this.getEndPoint(this.cMinecraftTag()).equals(this.getEndPoint(args[0]));
            //Don't actually deliver the message if the user is invisible to the sender.
            if (sameEndPoint && sender instanceof Player) {
                Player recipient = getServer().getPlayer(args[1]);
                if (recipient != null && recipient.isOnline() && ((Player)sender).canSee(recipient))
                    msg.postToUser(args[1]);
            } else
                msg.postToUser(args[1]);
            sender.sendMessage("Message sent.");
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdGetUserList(CommandSender sender, String[] args) {
        try {
            if (args.length == 0) {
                return false;
            }
            sender.sendMessage("Users in " + args[0] + ":");
            final List<String> userlists = this.ircUserLists(args[0]);
            for (final String string : userlists) {
                sender.sendMessage(string);
            }
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdNotifyIrcAdmins(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) {
                CraftIRC.dolog("CraftIRCListener cmdNotifyIrcAdmins()");
            }
            if ((args.length == 0) || !(sender instanceof Player)) {
                if (this.isDebug()) {
                    CraftIRC.dolog("CraftIRCListener cmdNotifyIrcAdmins() - args.length == 0 or Sender != player ");
                }
                return false;
            }
            final RelayedMessage msg = this.newMsg(this.getEndPoint(this.cMinecraftTag()), null, "admin");
            if (msg == null) {
                return true;
            }
            msg.setField("sender", ((Player) sender).getDisplayName());
            msg.setField("message", Util.combineSplit(0, args, " "));
            msg.setField("world", ((Player) sender).getWorld().getName());
            msg.doNotColor("message");
            msg.post(true);
            sender.sendMessage("Admin notice sent.");
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdRawIrcCommand(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) {
                CraftIRC.dolog("cmdRawIrcCommand(sender=" + sender.toString() + ", args=" + Util.combineSplit(0, args, " "));
            }
            if (args.length < 2) {
                return false;
            }
            this.sendRawToBot(Util.combineSplit(1, args, " "), Integer.parseInt(args[0]));
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /***************************
     * Endpoint and message interface (to be used by CraftIRC and external plugins)
     ***************************/

    /**
     * Null target: Sends message through all possible paths.
     * 
     * @param source
     * @param target
     * @param eventType
     * @return
     */
    public RelayedMessage newMsg(EndPoint source, EndPoint target, String eventType) {
        if (source == null) {
            return null;
        }
        if ((target == null) || this.cPathExists(this.getTag(source), this.getTag(target))) {
            return new RelayedMessage(this, source, target, eventType);
        } else {
            if (this.isDebug()) {
                CraftIRC.dolog("Failed to prepare message: " + this.getTag(source) + " -> " + this.getTag(target) + " (missing path)");
            }
            return null;
        }
    }

    public RelayedMessage newMsgToTag(EndPoint source, String target, String eventType) {
        if (source == null) {
            return null;
        }
        EndPoint targetpoint = null;
        if (target != null) {
            if (this.cPathExists(this.getTag(source), target)) {
                targetpoint = this.getEndPoint(target);
                if (targetpoint == null) {
                    CraftIRC.dolog("The requested target tag '" + target + "' isn't registered.");
                }
            } else {
                return null;
            }
        }
        return new RelayedMessage(this, source, targetpoint, eventType);
    }

    public RelayedCommand newCmd(EndPoint source, String command) {
        if (source == null) {
            return null;
        }
        final CommandEndPoint target = this.irccmds.get(command);
        if (target == null) {
            return null;
        }
        if (!this.cPathExists(this.getTag(source), this.getTag(target))) {
            return null;
        }
        final RelayedCommand cmd = new RelayedCommand(this, source, target);
        cmd.setField("command", command);
        return cmd;
    }

    public boolean registerEndPoint(String tag, EndPoint ep) {
        if (this.isDebug()) {
            CraftIRC.dolog("Registering endpoint: " + tag);
        }
        if (tag == null) {
            CraftIRC.dolog("Failed to register endpoint - No tag!");
        }
        if ((this.endpoints.get(tag) != null) || (this.tags.get(ep) != null)) {
            CraftIRC.dolog("Couldn't register an endpoint tagged '" + tag + "' because either the tag or the endpoint already exist.");
            return false;
        }
        if (tag == "*") {
            CraftIRC.dolog("Couldn't register an endpoint - the character * can't be used as a tag.");
            return false;
        }
        this.endpoints.put(tag, ep);
        this.tags.put(ep, tag);
        return true;
    }

    public boolean endPointRegistered(String tag) {
        return this.endpoints.get(tag) != null;
    }

    public EndPoint getEndPoint(String tag) {
        return this.endpoints.get(tag);
    }

    public String getTag(EndPoint ep) {
        return this.tags.get(ep);
    }

    public boolean registerCommand(String tag, String command) {
        if (this.isDebug()) {
            CraftIRC.dolog("Registering command: " + command + " to endpoint:" + tag);
        }
        final EndPoint ep = this.getEndPoint(tag);
        if (ep == null) {
            CraftIRC.dolog("Couldn't register the command '" + command + "' at the endpoint tagged '" + tag + "' because there is no such tag.");
            return false;
        }
        if (!(ep instanceof CommandEndPoint)) {
            CraftIRC.dolog("Couldn't register the command '" + command + "' at the endpoint tagged '" + tag + "' because it's not capable of handling commands.");
            return false;
        }
        if (this.irccmds.containsKey(command)) {
            CraftIRC.dolog("Couldn't register the command '" + command + "' at the endpoint tagged '" + tag + "' because that command is already registered.");
            return false;
        }
        this.irccmds.put(command, (CommandEndPoint) ep);
        return true;
    }

    public boolean unregisterCommand(String command) {
        if (!this.irccmds.containsKey(command)) {
            return false;
        }
        this.irccmds.remove(command);
        return true;
    }

    public boolean unregisterEndPoint(String tag) {
        final EndPoint ep = this.getEndPoint(tag);
        if (ep == null) {
            return false;
        }
        this.endpoints.remove(tag);
        this.tags.remove(ep);
        this.ungroupTag(tag);
        if (ep instanceof CommandEndPoint) {
            final CommandEndPoint cep = (CommandEndPoint) ep;
            for (final String cmd : this.irccmds.keySet()) {
                if (this.irccmds.get(cmd) == cep) {
                    this.irccmds.remove(cmd);
                }
            }
        }
        return true;
    }

    public boolean groupTag(String tag, String group) {
        if (this.getEndPoint(tag) == null) {
            return false;
        }
        List<String> tags = this.taggroups.get(group);
        if (tags == null) {
            tags = new ArrayList<String>();
            this.taggroups.put(group, tags);
        }
        tags.add(tag);
        return true;
    }

    public void ungroupTag(String tag) {
        for (final String group : this.taggroups.keySet()) {
            this.taggroups.get(group).remove(tag);
        }
    }

    public void clearGroup(String group) {
        this.taggroups.remove(group);
    }

    public boolean checkTagsGrouped(String tagA, String tagB) {
        for (final String group : this.taggroups.keySet()) {
            if (this.taggroups.get(group).contains(tagA) && this.taggroups.get(group).contains(tagB)) {
                return true;
            }
        }
        return false;
    }

    /***************************
     * Heart of the beast! Unified method with no special cases that replaces the old sendMessage
     ***************************/

    boolean delivery(RelayedMessage msg) {
        return this.delivery(msg, null, null, RelayedMessage.DeliveryMethod.STANDARD);
    }

    boolean delivery(RelayedMessage msg, List<EndPoint> destinations) {
        return this.delivery(msg, destinations, null, RelayedMessage.DeliveryMethod.STANDARD);
    }

    boolean delivery(RelayedMessage msg, List<EndPoint> knownDestinations, String username) {
        return this.delivery(msg, knownDestinations, username, RelayedMessage.DeliveryMethod.STANDARD);
    }

    /**
     * Only successful if all known targets (or if there is none at least one possible target) are successful!
     * 
     * @param msg
     * @param knownDestinations
     * @param username
     * @param dm
     * @return
     */
    boolean delivery(RelayedMessage msg, List<EndPoint> knownDestinations, String username, RelayedMessage.DeliveryMethod dm) {
        final String sourceTag = this.getTag(msg.getSource());
        msg.setField("source", sourceTag);
        List<EndPoint> destinations;
        if (this.isDebug()) {
            CraftIRC.dolog("X->" + (knownDestinations.size() > 0 ? knownDestinations.toString() : "*") + ": " + msg.toString());
        }
        //If we weren't explicitly given a recipient for the message, let's try to find one (or more)
        if (knownDestinations.size() < 1) {
            //Use all possible destinations (auto-targets)
            destinations = new LinkedList<EndPoint>();
            for (final String targetTag : this.cPathsFrom(sourceTag)) {
                final EndPoint ep = this.getEndPoint(targetTag);
                if (ep == null) {
                    continue;
                }
                if ((ep instanceof SecuredEndPoint) && SecuredEndPoint.Security.REQUIRE_TARGET.equals(((SecuredEndPoint) ep).getSecurity())) {
                    continue;
                }
                if (!this.cPathAttribute(sourceTag, targetTag, "attributes." + msg.getEvent())) {
                    continue;
                }
                if ((dm == RelayedMessage.DeliveryMethod.ADMINS) && !this.cPathAttribute(sourceTag, targetTag, "attributes.admin")) {
                    continue;
                }
                destinations.add(ep);
            }
            //Default paths to unsecured destinations (auto-paths)
            if (this.cAutoPaths()) {
                for (final EndPoint ep : this.endpoints.values()) {
                    if (ep == null) {
                        continue;
                    }
                    if (msg.getSource().equals(ep) || destinations.contains(ep)) {
                        continue;
                    }
                    if ((ep instanceof SecuredEndPoint) && !SecuredEndPoint.Security.UNSECURED.equals(((SecuredEndPoint) ep).getSecurity())) {
                        continue;
                    }
                    final String targetTag = this.getTag(ep);
                    if (this.checkTagsGrouped(sourceTag, targetTag)) {
                        continue;
                    }
                    if (!this.cPathAttribute(sourceTag, targetTag, "attributes." + msg.getEvent())) {
                        continue;
                    }
                    if ((dm == RelayedMessage.DeliveryMethod.ADMINS) && !this.cPathAttribute(sourceTag, targetTag, "attributes.admin")) {
                        continue;
                    }
                    if (this.cPathAttribute(sourceTag, targetTag, "disabled")) {
                        continue;
                    }
                    destinations.add(ep);
                }
            }
        } else {
            destinations = new LinkedList<EndPoint>(knownDestinations);
        }
        if (destinations.size() < 1) {
            return false;
        }
        //Deliver the message
        boolean success = true;
        for (final EndPoint destination : destinations) {
            final String targetTag = this.getTag(destination);
            if(targetTag.equals(this.cCancelledTag())){
                continue;
            }
            msg.setField("target", targetTag);
            //Check against path filters
            if ((msg instanceof RelayedCommand) && this.matchesFilter(msg, this.cPathFilters(sourceTag, targetTag))) {
                if (knownDestinations != null) {
                    success = false;
                }
                continue;
            }
            //Finally deliver!
            if (this.isDebug()) {
                CraftIRC.dolog("-->X: " + msg.toString());
            }
            if (username != null) {
                success = success && destination.userMessageIn(username, msg);
            } else if (dm == RelayedMessage.DeliveryMethod.ADMINS) {
                success = destination.adminMessageIn(msg);
            } else if (dm == RelayedMessage.DeliveryMethod.COMMAND) {
                if (!(destination instanceof CommandEndPoint)) {
                    continue;
                }
                ((CommandEndPoint) destination).commandIn((RelayedCommand) msg);
            } else {
                destination.messageIn(msg);
            }
        }
        return success;
    }

    boolean matchesFilter(RelayedMessage msg, List<ConfigurationNode> filters) {
        if (filters == null) {
            return false;
        }
        newFilter: for (final ConfigurationNode filter : filters) {
            for (final String key : filter.getKeys()) {
                final Pattern condition = Pattern.compile(filter.getString(key, ""));
                if (condition == null) {
                    continue newFilter;
                }
                final String subject = msg.getField(key);
                if (subject == null) {
                    continue newFilter;
                }
                final Matcher check = condition.matcher(subject);
                if (!check.find()) {
                    continue newFilter;
                }
            }
            return true;
        }
        return false;
    }

    /***************************
     * Auxiliary methods
     ***************************/
    
    public Minebot getBot(int bot){
        return this.instances.get(bot);
    }

    public void sendRawToBot(String rawMessage, int bot) {
        if (this.isDebug()) {
            CraftIRC.dolog("sendRawToBot(bot=" + bot + ", message=" + rawMessage);
        }
        final Minebot targetBot = this.instances.get(bot);
        targetBot.sendRawLineViaQueue(rawMessage);
    }

    public void sendMsgToTargetViaBot(String message, String target, int bot) {
        final Minebot targetBot = this.instances.get(bot);
        targetBot.sendMessage(target, message);
    }

    public List<String> ircUserLists(String tag) {
        return this.getEndPoint(tag).listDisplayUsers();
    }

    void setDebug(boolean d) {
        this.debug = d;

        for (int i = 0; i < this.bots.size(); i++) {
            this.instances.get(i).setVerbose(d);
        }

        CraftIRC.dolog("DEBUG [" + (d ? "ON" : "OFF") + "]");
    }

    public String getPrefix(Player p) {
        String result = "";
        if (this.vault != null) {
            try {
                result = this.vault.getPlayerPrefix(p);
            } catch (final Exception e) {

            }
        }
        return result;
    }

    public String getSuffix(Player p) {
        String result = "";
        if (this.vault != null) {
            try {
                result = this.vault.getPlayerSuffix(p);
            } catch (final Exception e) {

            }
        }
        return result;
    }

    public boolean isDebug() {
        return this.debug;
    }

    boolean checkPerms(Player pl, String path) {
        return pl.hasPermission(path);
    }

    boolean checkPerms(String pl, String path) {
        final Player pit = this.getServer().getPlayer(pl);
        if (pit != null) {
            return pit.hasPermission(path);
        }
        return false;
    }

    // TODO: Make sure this works
    public String colorizeName(String name) {
        final Pattern color_codes = Pattern.compile(ChatColor.COLOR_CHAR + "[0-9a-fk-r]");
        Matcher find_colors = color_codes.matcher(name);
        while (find_colors.find()) {
            name = find_colors.replaceFirst(Character.toString((char) 3) + this.cColorIrcFromGame(find_colors.group()));
            find_colors = color_codes.matcher(name);
        }
        return name;
    }

    protected void enqueueConsoleCommand(String cmd) {
        try {
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), cmd);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * If the channel is null it's a reconnect, otherwise a rejoin
     * 
     * @param bot
     * @param channel
     */
    void scheduleForRetry(Minebot bot, String channel) {
        this.retryTimer.schedule(new RetryTask(this, bot, channel), this.cRetryDelay());
    }

    /***************************
     * Read stuff from config
     ***************************/

    private ConfigurationNode getChanNode(int bot, String channel) {
        final ArrayList<ConfigurationNode> botChans = this.channodes.get(bot);
        for (final ConfigurationNode chan : botChans) {
            if (chan.getString("name").equalsIgnoreCase(channel)) {
                return chan;
            }
        }
        return Configuration.getEmptyNode();
    }

    public List<ConfigurationNode> cChannels(int bot) {
        return this.channodes.get(bot);
    }

    private ConfigurationNode getPathNode(String source, String target) {
        ConfigurationNode result = this.paths.get(new Path(source, target));
        if (result == null) {
            return this.configuration.getNode("default-attributes");
        }
        ConfigurationNode basepath;
        if (result.getKeys().contains("base") && ((basepath = result.getNode("base")) != null)) {
            final ConfigurationNode basenode = this.paths.get(new Path(basepath.getString("source", ""), basepath.getString("target", "")));
            if (basenode != null) {
                result = basenode;
            }
        }
        return result;
    }

    public String cMinecraftTag() {
        return this.configuration.getString("settings.minecraft-tag", "minecraft");
    }

    public String cCancelledTag() {
        return this.configuration.getString("settings.cancelled-tag", "cancelled");
    }

    public String cConsoleTag() {
        return this.configuration.getString("settings.console-tag", "console");
    }

    public String cMinecraftTagGroup() {
        return this.configuration.getString("settings.minecraft-group-name", "minecraft");
    }

    public String cIrcTagGroup() {
        return this.configuration.getString("settings.irc-group-name", "irc");
    }

    public boolean cAutoPaths() {
        return this.configuration.getBoolean("settings.auto-paths", false);
    }

    public boolean cCancelChat() {
        return cancelChat;
    }

    public boolean cDebug() {
        return this.configuration.getBoolean("settings.debug", false);
    }

    public ArrayList<String> cConsoleCommands() {
        return new ArrayList<String>(this.configuration.getStringList("settings.console-commands", null));
    }

    public int cHold(String eventType) {
        return this.configuration.getInt("settings.hold-after-enable." + eventType, 0);
    }

    public String cFormatting(String eventType, RelayedMessage msg) {
        return this.cFormatting(eventType, msg, null);
    }

    public String cFormatting(String eventType, RelayedMessage msg, EndPoint realTarget) {
        final String source = this.getTag(msg.getSource()), target = this.getTag(realTarget != null ? realTarget : msg.getTarget());
        if ((source == null) || (target == null)) {
            CraftIRC.dowarn("Attempted to obtain formatting for invalid path " + source + " -> " + target + " .");
            return this.cDefaultFormatting(eventType, msg);
        }
        final ConfigurationNode pathConfig = this.paths.get(new Path(source, target));
        if ((pathConfig != null) && (pathConfig.getString("formatting." + eventType, null) != null)) {
            return pathConfig.getString("formatting." + eventType, null);
        } else {
            return this.cDefaultFormatting(eventType, msg);
        }
    }

    public String cDefaultFormatting(String eventType, RelayedMessage msg) {
        if (msg.getSource().getType() == EndPoint.Type.MINECRAFT) {
            return this.configuration.getString("settings.formatting.from-game." + eventType);
        }
        if (msg.getSource().getType() == EndPoint.Type.IRC) {
            return this.configuration.getString("settings.formatting.from-irc." + eventType);
        }
        if (msg.getSource().getType() == EndPoint.Type.PLAIN) {
            return this.configuration.getString("settings.formatting.from-plain." + eventType);
        }
        return "";
    }

    public String cColorIrcFromGame(String game) {
        ConfigurationNode color;
        final Iterator<ConfigurationNode> it = this.colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("game").equals(game)) {
            	//Forces sending two digit colour codes. 
                return (color.getString("irc").length() == 1 ? "0" : "") + color.getString("irc", this.cColorIrcFromName("foreground"));
            }
        }
        return this.cColorIrcFromName("foreground");
    }

    public String cColorIrcFromName(String name) {
        ConfigurationNode color;
        final Iterator<ConfigurationNode> it = this.colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("name").equalsIgnoreCase(name) && (color.getProperty("irc") != null)) {
                return color.getString("irc", "01");
            }
        }
        if (name.equalsIgnoreCase("foreground")) {
            return "01";
        } else {
            return this.cColorIrcFromName("foreground");
        }
    }

    public String cColorGameFromIrc(String irc) {
    	//Always convert to two digits.
    	if (irc.length() == 1)
    		irc = "0"+irc;
        ConfigurationNode color;
        final Iterator<ConfigurationNode> it = this.colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            //Enforce two digit comparisons.
            if (color.getString("irc", "").equals(irc) || "0".concat(color.getString("irc", "")).equals(irc)) {
                return color.getString("game", this.cColorGameFromName("foreground"));
            }
        }
        return this.cColorGameFromName("foreground");
    }

    public String cColorGameFromName(String name) {
        ConfigurationNode color;
        final Iterator<ConfigurationNode> it = this.colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("name").equalsIgnoreCase(name) && (color.getProperty("game") != null)) {
                return color.getString("game", "\u00C2\u00A7f");
            }
        }
        if (name.equalsIgnoreCase("foreground")) {
            return "\u00C2\u00A7f";
        } else {
            return this.cColorGameFromName("foreground");
        }
    }

    public String cBindLocalAddr() {
        return this.configuration.getString("settings.bind-address", "");
    }

    public int cRetryDelay() {
        return this.configuration.getInt("settings.retry-delay", 10) * 1000;
    }

    public String cBotNickname(int bot) {
        return this.bots.get(bot).getString("nickname", "CraftIRCbot");
    }

    public String cBotServer(int bot) {
        return this.bots.get(bot).getString("server", "irc.esper.net");
    }

    public int cBotPort(int bot) {
        return this.bots.get(bot).getInt("port", 6667);
    }
    
    public int cBotBindPort(int bot) {
        return this.bots.get(bot).getInt("bind-port", 0);
    }

    public String cBotLogin(int bot) {
        return this.bots.get(bot).getString("userident", "");
    }

    public String cBotPassword(int bot) {
        return this.bots.get(bot).getString("serverpass", "");
    }

    public boolean cBotSsl(int bot) {
        return this.bots.get(bot).getBoolean("ssl", false);
    }

    public int cBotMessageDelay(int bot) {
        return this.bots.get(bot).getInt("message-delay", 1000);
    }

    public int cBotQueueSize(int bot) {
        return this.bots.get(bot).getInt("queue-size", 5);
    }

    public String cCommandPrefix(int bot) {
        return this.bots.get(bot).getString("command-prefix", this.configuration.getString("settings.command-prefix", "."));
    }

    public List<String> cCmdWordCmd(Integer bot) {
        final List<String> init = new ArrayList<String>();
        init.add("cmd");
        final List<String> result = this.configuration.getStringList("settings.irc-commands.cmd", init);
        if (bot != null) {
            return this.bots.get(bot).getStringList("irc-commands.cmd", result);
        }
        return result;
    }

    public List<String> cCmdWordSay(Integer bot) {
        final List<String> init = new ArrayList<String>();
        init.add("say");
        final List<String> result = this.configuration.getStringList("settings.irc-commands.say", init);
        if (bot != null) {
            return this.bots.get(bot).getStringList("irc-commands.say", result);
        }
        return result;
    }

    public List<String> cCmdWordPlayers(Integer bot) {
        final List<String> init = new ArrayList<String>();
        init.add("players");
        final List<String> result = this.configuration.getStringList("settings.irc-commands.players", init);
        if (bot != null) {
            return this.bots.get(bot).getStringList("irc-commands.players", result);
        }
        return result;
    }

    public ArrayList<String> cBotAdminPrefixes(int bot) {
        return new ArrayList<String>(this.bots.get(bot).getStringList("admin-prefixes", null));
    }

    public ArrayList<String> cBotIgnoredUsers(int bot) {
        return new ArrayList<String>(this.bots.get(bot).getStringList("ignored-users", null));
    }

    public String cBotAuthMethod(int bot) {
        return this.bots.get(bot).getString("auth.method", "nickserv");
    }

    public String cBotAuthUsername(int bot) {
        return this.bots.get(bot).getString("auth.username", "");
    }

    public String cBotAuthPassword(int bot) {
        return this.bots.get(bot).getString("auth.password", "");
    }

    public ArrayList<String> cBotOnConnect(int bot) {
        return new ArrayList<String>(this.bots.get(bot).getStringList("on-connect", null));
    }

    public String cChanName(int bot, String channel) {
        return this.getChanNode(bot, channel).getString("name", "#changeme");
    }

    public String cChanTag(int bot, String channel) {
        return this.getChanNode(bot, channel).getString("tag", String.valueOf(bot) + "_" + channel);
    }

    public String cChanPassword(int bot, String channel) {
        return this.getChanNode(bot, channel).getString("password", "");
    }

    public ArrayList<String> cChanOnJoin(int bot, String channel) {
        return new ArrayList<String>(this.getChanNode(bot, channel).getStringList("on-join", null));
    }

    public List<String> cPathsFrom(String source) {
        final List<String> results = new LinkedList<String>();
        for (final Path path : this.paths.keySet()) {
            if (!path.getSourceTag().equals(source)) {
                continue;
            }
            if (this.paths.get(path).getBoolean("disable", false)) {
                continue;
            }
            results.add(path.getTargetTag());
        }
        return results;
    }

    List<String> cPathsTo(String target) {
        final List<String> results = new LinkedList<String>();
        for (final Path path : this.paths.keySet()) {
            if (!path.getTargetTag().equals(target)) {
                continue;
            }
            if (this.paths.get(path).getBoolean("disable", false)) {
                continue;
            }
            results.add(path.getSourceTag());
        }
        return results;
    }

    public boolean cPathExists(String source, String target) {
        final ConfigurationNode pathNode = this.getPathNode(source, target);
        return (pathNode != null) && !pathNode.getBoolean("disabled", false);
    }

    public boolean cPathAttribute(String source, String target, String attribute) {
        final ConfigurationNode node = this.getPathNode(source, target);
        if (node.getProperty(attribute) != null) {
            return node.getBoolean(attribute, false);
        } else {
            return this.configuration.getNode("default-attributes").getBoolean(attribute, false);
        }
    }

    public List<ConfigurationNode> cPathFilters(String source, String target) {
        return this.getPathNode(source, target).getNodeList("filters", new ArrayList<ConfigurationNode>());
    }
    
    public Map<String, Map<String, String>> cReplaceFilters() {
        return this.replaceFilters;
    }

    void loadTagGroups() {
        final List<String> groups = this.configuration.getKeys("settings.tag-groups");
        if (groups == null) {
            return;
        }
        for (final String group : groups) {
            for (final String tag : this.configuration.getStringList("settings.tag-groups." + group, new ArrayList<String>())) {
                this.groupTag(tag, group);
            }
        }
    }

    boolean cUseMapAsWhitelist(int bot) {
        return this.bots.get(bot).getBoolean("use-map-as-whitelist", false);
    }

    public String cIrcDisplayName(int bot, String nickname) {
        return this.bots.get(bot).getString("irc-nickname-map." + nickname, nickname);
    }

    public boolean cNicknameIsInIrcMap(int bot, String nickname) {
        return this.bots.get(bot).getString("irc-nickname-map." + nickname) != null;
    }

    public enum HoldType {
        CHAT, JOINS, QUITS, KICKS, BANS, DEATHS
    }

    class RemoveHoldTask extends TimerTask {
        private final CraftIRC plugin;
        private final HoldType ht;

        protected RemoveHoldTask(CraftIRC plugin, HoldType ht) {
            super();
            this.plugin = plugin;
            this.ht = ht;
        }

        @Override
        public void run() {
            this.plugin.hold.put(this.ht, false);
        }
    }

    public boolean isHeld(HoldType ht) {
        return this.hold.get(ht);
    }
    
    public List<ConfigurationNode> getColorMap() {
        return this.colormap;
    }

    public String processAntiHighlight(String input) {
        String delimiter = this.configuration.getString("settings.anti-highlight");

        if (delimiter != null && !delimiter.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (int i=0; i < input.length(); i++) {
                char c = input.charAt(i);
                builder.append(c);
                if (c != '\u00A7') {
                    builder.append(delimiter);
                }
            }
            return builder.toString();
        } else {
            return input;
        }
    }
}
