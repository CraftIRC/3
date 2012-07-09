package com.ensifera.animosity.craftirc;

import java.io.IOException;
import java.util.*;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.TrustingSSLSocketFactory;

import com.sk89q.util.config.ConfigurationNode;

/**
 * @author Animosity
 * @author Protected
 */
public class Minebot extends PircBot implements Runnable {

    private CraftIRC plugin = null;
    private final boolean debug;
    private final int botId;
    private String nickname;

    private final Thread thread;

    // Connection attributes
    private boolean ssl;
    private String ircServer;
    private int ircPort;
    private String ircPass;

    // Nickname authentication
    private String authMethod;
    private String authUser;
    private String authPass;

    // Channels
    private Set<String> whereAmI;
    private Map<String, IRCChannelPoint> channels;

    // Other things that may be more efficient to store here
    private List<String> ignores;
    private String cmdPrefix;

    Minebot(CraftIRC plugin, int botId, boolean debug) {
        super();
        this.plugin = plugin;
        this.botId = botId;
        this.debug = debug;
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public synchronized void run() {
        this.setVerbose(this.debug);
        this.setMessageDelay(this.plugin.cBotMessageDelay(this.botId));
        this.setQueueSize(this.plugin.cBotQueueSize(this.botId));
        this.setName(this.plugin.cBotNickname(this.botId));
        this.setFinger(CraftIRC.NAME + " v" + CraftIRC.VERSION);
        this.setLogin(this.plugin.cBotLogin(this.botId));
        this.setVersion(CraftIRC.NAME + " v" + CraftIRC.VERSION);

        this.nickname = this.plugin.cBotNickname(this.botId);

        this.ssl = this.plugin.cBotSsl(this.botId);
        this.ircServer = this.plugin.cBotServer(this.botId);
        this.ircPort = this.plugin.cBotPort(this.botId);
        this.ircPass = this.plugin.cBotPassword(this.botId);

        this.authMethod = this.plugin.cBotAuthMethod(this.botId);
        this.authUser = this.plugin.cBotAuthUsername(this.botId);
        this.authPass = this.plugin.cBotAuthPassword(this.botId);

        this.whereAmI = new HashSet<String>();
        this.channels = new HashMap<String, IRCChannelPoint>();
        for (final ConfigurationNode channelNode : this.plugin.cChannels(this.botId)) {
            final String name = channelNode.getString("name");
            if (this.channels.containsKey(name)) {
                continue;
            }
            final IRCChannelPoint chan = new IRCChannelPoint(this, name);
            if (!this.plugin.registerEndPoint(channelNode.getString("tag"), chan)) {
                continue;
            }
            if (!this.plugin.cIrcTagGroup().equals("")) {
                this.plugin.groupTag(channelNode.getString("tag"), this.plugin.cIrcTagGroup());
            }
            this.channels.put(name, chan);
        }

        this.ignores = this.plugin.cBotIgnoredUsers(this.botId);
        this.cmdPrefix = this.plugin.cCommandPrefix(this.botId);

        try {
            this.start();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
    public void delChannel(String tag){
        IRCChannelPoint point=this.channels.remove(tag);
        if(point==null){
            return;
        }
        this.plugin.unregisterEndPoint(tag);
        this.plugin.ungroupTag(tag);
        this.partChannel("#"+tag, "No longer registered!");
    }

    public void addChannel(String channel){
        if (this.channels.containsKey(channel)) {
            return;
        }
        final IRCChannelPoint chan = new IRCChannelPoint(this, "#"+channel);
        String tag="irc_"+channel;
        if (!this.plugin.registerEndPoint(tag, chan)) {
            return;
        }
        if (!this.plugin.cIrcTagGroup().equals("")) {
            this.plugin.groupTag(tag, this.plugin.cIrcTagGroup());
        }
        this.channels.put("#"+channel, chan);
        this.joinChannel("#"+channel);
    }

    /**
     * Thread start
     */
    void start() {
        try {
            this.setAutoNickChange(true);

            final String localAddr = this.plugin.cBindLocalAddr();
            if (!localAddr.isEmpty()) {

                if (this.bindLocalAddr(localAddr, this.ircPort)) {
                    CraftIRC.dolog("BINDING socket to " + localAddr + ":" + this.ircPort);
                }
            }

            this.connectToIrc();
            this.plugin.scheduleForRetry(this, null);

        } catch (final NumberFormatException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    void connectToIrc() {
        try {
            if (this.ssl) {
                CraftIRC.dolog("Connecting to " + this.ircServer + ":" + this.ircPort + " [SSL]");
                this.connect(this.ircServer, this.ircPort, this.ircPass, new TrustingSSLSocketFactory());
            } else {
                CraftIRC.dolog("Connecting to " + this.ircServer + ":" + this.ircPort);
                this.connect(this.ircServer, this.ircPort, this.ircPass);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final IrcException e) {
            e.printStackTrace();
        }
    }

    boolean isIn(String channel) {
        return this.whereAmI.contains(channel);
    }

    void joinIrcChannel(String chan) {
        this.joinChannel(chan, this.plugin.cChanPassword(this.botId, chan));
    }

    public CraftIRC getPlugin() {
        return this.plugin;
    }

    public int getId() {
        return this.botId;
    }

    @Override
    public void onConnect() {
        CraftIRC.dolog("Connected");
        this.authenticateBot();

        final ArrayList<String> onConnect = this.plugin.cBotOnConnect(this.botId);
        final Iterator<String> it = onConnect.iterator();
        while (it.hasNext()) {
            this.sendRawLineViaQueue(it.next());
        }

        for (final String chan : this.channels.keySet()) {
            this.joinIrcChannel(chan);
        }
    }

    void authenticateBot() {
        if (this.authMethod.equalsIgnoreCase("nickserv") && !this.authPass.isEmpty()) {
            CraftIRC.dolog("Using Nickserv authentication.");
            this.sendMessage("nickserv", "GHOST " + this.nickname + " " + this.authPass);

            // Some IRC servers have quite a delay when ghosting... ***** TO IMPROVE
            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            this.changeNick(this.nickname);
            this.identify(this.authPass);

        } else if (this.authMethod.equalsIgnoreCase("gamesurge")) {
            CraftIRC.dolog("Using GameSurge authentication.");
            this.changeNick(this.nickname);
            this.sendMessage("AuthServ@Services.GameSurge.net", "AUTH " + this.authUser + " " + this.authPass);

        } else if (this.authMethod.equalsIgnoreCase("quakenet")) {
            CraftIRC.dolog("Using QuakeNet authentication.");
            this.changeNick(this.nickname);
            this.sendMessage("Q@CServe.quakenet.org", "AUTH " + this.authUser + " " + this.authPass);
        }

    }

    void amNowInChannel(String channel) {
        CraftIRC.dolog("Joined channel: " + channel);
        this.whereAmI.add(channel);
        final String tag = this.plugin.cChanTag(this.botId, channel);
        if ((tag != null) && !this.plugin.endPointRegistered(tag)) {
            this.plugin.registerEndPoint(tag, this.channels.get(channel));
        }
        for (final String line : this.plugin.cChanOnJoin(this.botId, channel)) {
            this.sendRawLineViaQueue(line);
        }
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
        if (this.channels.containsKey(channel)) {
            if (sender.equals(this.getNick())) {
                this.amNowInChannel(channel);
            } else {
                if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, sender)) {
                    return;
                }
                final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "join");
                if (msg == null) {
                    return;
                }
                msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
                msg.setField("realSender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("username", login);
                msg.setField("hostname", hostname);
                msg.doNotColor("username");
                msg.doNotColor("hostname");
                msg.post();
            }
        }
    }

    void noLongerInChannel(String channel, boolean rejoin) {
        this.whereAmI.remove(channel);
        this.plugin.unregisterEndPoint(this.plugin.cChanTag(this.botId, channel));
        if (rejoin) {
            this.plugin.scheduleForRetry(this, channel);
        }
    }

    @Override
    public void onPart(String channel, String sender, String login, String hostname, String reason) {
        if (sender.equals(this.getNick())) {
            this.noLongerInChannel(channel, true);
        }
        if (this.channels.containsKey(channel)) {
            if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, sender)) {
                return;
            }
            final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "part");
            if (msg == null) {
                return;
            }
            msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
            msg.setField("realSender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("username", login);
            msg.setField("hostname", hostname);
            msg.doNotColor("message");
            msg.doNotColor("username");
            msg.doNotColor("hostname");
            msg.post();
        }
    }

    @Override
    public void onChannelQuit(String channel, String sender, String login, String hostname, String reason) {
        if (sender.equals(this.getNick())) {
            this.noLongerInChannel(channel, false);
        }
        if (this.channels.containsKey(channel)) {
            if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, sender)) {
                return;
            }
            final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "quit");
            if (msg == null) {
                return;
            }
            msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
            msg.setField("realSender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("username", login);
            msg.setField("hostname", hostname);
            msg.doNotColor("message");
            msg.doNotColor("username");
            msg.doNotColor("hostname");
            msg.post();
        }
    }

    @Override
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equals(this.getNick())) {
            this.noLongerInChannel(channel, true);
        }
        if (this.channels.containsKey(channel)) {
            if (recipientNick.equalsIgnoreCase(this.getNick())) {
                this.joinChannel(channel, this.plugin.cChanPassword(this.botId, channel));
            }
            if (this.plugin.cUseMapAsWhitelist(this.botId) && (!this.plugin.cNicknameIsInIrcMap(this.botId, kickerNick) || !this.plugin.cNicknameIsInIrcMap(this.botId, recipientNick))) {
                return;
            }
            final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "kick");
            if (msg == null) {
                return;
            }
            msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, recipientNick));
            msg.setField("realSender", recipientNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("moderator", this.plugin.cIrcDisplayName(this.botId, kickerNick));
            msg.setField("realModerator", kickerNick);
            msg.setField("ircModPrefix", this.getHighestUserPrefix(this.getUser(kickerNick, channel)));
            msg.setField("modUsername", kickerNick);
            msg.setField("modHostname", kickerLogin);
            msg.doNotColor("message");
            msg.doNotColor("modUsername");
            msg.doNotColor("modHostname");
            msg.post();
        }
    }

    @Override
    public void onChannelNickChange(String channel, String oldNick, String login, String hostname, String newNick) {
        if (oldNick.equals(this.getNick())) {
            this.nickname = newNick;
        }
        if (this.channels.containsKey(channel)) {
            if (this.plugin.cUseMapAsWhitelist(this.botId) && (!this.plugin.cNicknameIsInIrcMap(this.botId, oldNick) || !this.plugin.cNicknameIsInIrcMap(this.botId, newNick))) {
                return;
            }
            final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "nick");
            if (msg == null) {
                return;
            }
            msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, oldNick));
            msg.setField("realSender", oldNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", this.plugin.cIrcDisplayName(this.botId, newNick));
            msg.setField("realMessage", newNick);
            msg.setField("username", login);
            msg.setField("hostname", hostname);
            msg.doNotColor("username");
            msg.doNotColor("hostname");
            msg.post();
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (this.ignores.contains(sender)) {
            return;
        }
        try {
            if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, sender)) {
                return;
            }
            final String[] splitMessage = message.split(" ");
            final String command = splitMessage[0];
            final String args = Util.combineSplit(1, splitMessage, " ");
            RelayedCommand cmd = null;
            final String localTag = this.plugin.cChanTag(this.botId, channel);
            final boolean loopbackAdmin = this.plugin.cPathAttribute(localTag, localTag, "attributes.admin");
            final boolean userAdmin = this.plugin.cBotAdminPrefixes(this.botId).contains(this.getHighestUserPrefix(this.getUser(sender, channel)));
            if (this.cmdPrefix.equals("")) {
                final List<String> allCommands = new ArrayList<String>();
                allCommands.addAll(this.plugin.cCmdWordCmd(this.botId));
                allCommands.addAll(this.plugin.cCmdWordSay(this.botId));
                allCommands.addAll(this.plugin.cCmdWordPlayers(this.botId));
                for (final String cmdString : allCommands) {
                    if (command.equals(cmdString)) {
                        cmd = this.plugin.newCmd(this.channels.get(channel), command);
                        break;
                    }
                }
            } else if (command.startsWith(this.cmdPrefix)) {
                cmd = this.plugin.newCmd(this.channels.get(channel), command.substring(this.cmdPrefix.length()));
            }
            if (cmd != null) {
                //Normal command
                cmd.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
                cmd.setField("realSender", sender);
                cmd.setField("srcChannel", channel);
                cmd.setField("args", args);
                cmd.setField("ircPrefix", this.getHighestUserPrefix(this.getUser(sender, channel)));
                cmd.setField("username", login);
                cmd.setField("hostname", hostname);
                cmd.doNotColor("username");
                cmd.doNotColor("hostname");
                cmd.setFlag("admin", userAdmin);
                cmd.act();
            } else if (command.toLowerCase().equals(this.cmdPrefix + "botsay") && loopbackAdmin && userAdmin) {
                if (args == null) {
                    return;
                }
                this.sendMessage(args.substring(0, args.indexOf(" ")), args.substring(args.indexOf(" ") + 1));
            } else if (command.toLowerCase().equals(this.cmdPrefix + "raw") && loopbackAdmin && userAdmin) {
                if (args == null) {
                    return;
                }
                this.sendRawLine(args);
            } else {
                //Not a command
                final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "chat");
                if (msg == null) {
                    return;
                }
                msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
                msg.setField("realSender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("message", message);
                msg.setField("ircPrefix", this.getHighestUserPrefix(this.getUser(sender, channel)));
                msg.setField("username", login);
                msg.setField("hostname", hostname);
                msg.doNotColor("message");
                msg.doNotColor("username");
                msg.doNotColor("hostname");
                msg.post();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            CraftIRC.dowarn("error while relaying IRC message: " + message);
        }
    }

    @Override
    public void onAction(String sender, String login, String hostname, String target, String action) {
        final RelayedMessage msg = this.plugin.newMsg(this.channels.get(target), null, "action");
        if (msg == null) {
            return;
        }
        if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, sender)) {
            return;
        }
        msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
        msg.setField("realSender", sender);
        msg.setField("srcChannel", target);
        msg.setField("message", action);
        msg.setField("ircPrefix", this.getHighestUserPrefix(this.getUser(sender, target)));
        msg.setField("username", login);
        msg.setField("hostname", hostname);
        msg.doNotColor("message");
        msg.doNotColor("username");
        msg.doNotColor("hostname");
        msg.post();
    }

    @Override
    public void onTopic(String channel, String topic, String sender, long date, boolean changed) {
        final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "topic");
        if (msg == null) {
            return;
        }
        if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, sender)) {
            return;
        }
        msg.setField("sender", this.plugin.cIrcDisplayName(this.botId, sender));
        msg.setField("realSender", sender);
        msg.setField("srcChannel", channel);
        msg.setField("message", topic);
        msg.doNotColor("message");
        msg.doNotColor("username");
        msg.doNotColor("hostname");
        msg.post();
    }

    @Override
    protected void onMode(String channel, String moderator, String sourceLogin, String sourceHostname, String mode) {
        final RelayedMessage msg = this.plugin.newMsg(this.channels.get(channel), null, "mode");
        if (msg == null) {
            return;
        }
        if (this.plugin.cUseMapAsWhitelist(this.botId) && !this.plugin.cNicknameIsInIrcMap(this.botId, moderator)) {
            return;
        }
        msg.setField("moderator", this.plugin.cIrcDisplayName(this.botId, moderator));
        msg.setField("realModerator", moderator);
        msg.setField("srcChannel", channel);
        msg.setField("message", mode);
        msg.setField("username", sourceLogin);
        msg.setField("hostname", sourceHostname);
        msg.doNotColor("username");
        msg.doNotColor("hostname");
        msg.post();
    }

    public ArrayList<String> getChannelList() {
        try {
            return new ArrayList<String>(Arrays.asList(this.getChannels()));
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Bot restart upon disconnect, if the plugin is still enabled
    @Override
    public void onDisconnect() {
        try {
            if (this.plugin.isEnabled()) {
                CraftIRC.log.info(CraftIRC.NAME + " - disconnected from IRC server... reconnecting!");

                this.connectToIrc();
                this.plugin.scheduleForRetry(this, null);

            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onBlockColors(String channel, String moderator, String sourceLogin, String sourceHostname) {
        if (this.channels.containsKey(channel)) {
            this.channels.get(channel).setAllowColors(false);
        }
    }

    @Override
    protected void onUnblockColors(String channel, String moderator, String sourceLogin, String sourceHostname) {
        if (this.channels.containsKey(channel)) {
            this.channels.get(channel).setAllowColors(true);
        }
    }

}// EO Minebot
