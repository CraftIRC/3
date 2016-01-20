/* 
Copyright Paul James Mutton, 2001-2007, http://www.jibble.org/

This file is part of PircBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

 */

package com.ensifera.animosity.craftirc.libs.org.jibble.pircbot;

import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PircBot is a Java framework for writing IRC bots quickly and easily.
 * <p>
 * It provides an event-driven architecture to handle common IRC events, flood
 * protection, DCC support, ident support, and more. The comprehensive logfile
 * format is suitable for use with pisg to generate channel statistics.
 * <p>
 * Methods of the PircBot class can be called to send events to the IRC server
 * that it connects to. For example, calling the sendMessage method will send a
 * message to a channel or user on the IRC server. Multiple servers can be
 * supported using multiple instances of PircBot.
 * <p>
 * To perform an action when the PircBot receives a normal message from the IRC
 * server, you would override the onMessage method defined in the PircBot class.
 * All on<i>XYZ</i> methods in the PircBot class are automatically called when
 * the event <i>XYZ</i> happens, so you would override these if you wish to do
 * something when it does happen.
 * <p>
 * Some event methods, such as onPing, should only really perform a specific
 * function (i.e. respond to a PING from the server). For your convenience, such
 * methods are already correctly implemented in the PircBot and should not
 * normally need to be overridden. Please read the full documentation for each
 * method to see which ones are already implemented by the PircBot class.
 * <p>
 * Please visit the (original) PircBot homepage at <a
 * href="http://www.jibble.org/pircbot.php"
 * >http://www.jibble.org/pircbot.php</a> for full revision history, a beginners
 * guide to creating your first PircBot and a list of some existing Java IRC
 * bots and clients that use the PircBot framework.
 * <p>
 * This PircBot-PPF project is developed as a sub project of the PPF project. <a
 * href="http://ppf.sf.net">ppf.sf.net</a>
 *
 * @author PircBot-PPF project
 * @version 1.0.0
 */
public abstract class PircBot implements ReplyConstants, PircBotLogger {
    /**
     * The definitive version number of this release of PircBot. (Note: Change
     * this before automatically building releases)
     */
    public static final String VERSION = "1.5.1-ppf-ssl";

    private static final String PREFIX_DEF = "PREFIX=";
    private static final String NETWORK_DEF = "NETWORK="; // CrapIRC
    private SocketFactory _socketFactory;
    private SocketAddress bindLocalAddr; // CraftIRC

    /**
     * Constructs a PircBot with the default settings. Your own constructors in
     * classes which extend the PircBot abstract class should be responsible for
     * changing the default settings if required.
     */
    public PircBot() {
        // set a default mode/prefix for the users in case the server doesn't
        // have it defined properly
        // default to PREFIX=(ov)@+
        _userPrefixes.put("o", "@");
        _userPrefixes.put("v", "+");
    }

    public boolean bindLocalAddr(String localAddr, int port) {
        try {
            this.bindLocalAddr = new InetSocketAddress(InetAddress.getByName(localAddr), port);
            return true;
        } catch (Exception e) {
            this.bindLocalAddr = null;
            return false;
        }
    }

    /**
     * Sets the SocketFactory used by the PircBot. This will override the
     * default SocketFactory and can be used to make SSL connections.
     *
     * @param socketFactory The SocketFactory that should be used to make connections.
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        _socketFactory = socketFactory;
    }

    /**
     * Returns the last SocketFactory that we used to connect to an IRC server.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method). A value of null
     * is returned if the PircBot has never tried to connect to a server using a
     * SocketFactory.
     *
     * @return The last SocketFactory that we used when connecting to an IRC
     * server. Returns null if we have not previously connected using a
     * SocketFactory.
     */
    public final SocketFactory getSocketFactory() {
        return _socketFactory;
    }

    /**
     * Attempt to connect to the specified IRC server. The onConnect method is
     * called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname) throws IOException, IrcException {
        this.connect(hostname, 6667, null, null);
    }

    /**
     * Attempt to connect to the specified IRC server and port number. The
     * onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname, int port, SocketFactory socketFactory) throws IOException,
            IrcException {
        this.connect(hostname, port, null, socketFactory);
    }

    /**
     * Attempt to connect to the specified IRC server and port number. The
     * onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     * @param password The password to use to join the server.
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname, int port, String password) throws IOException,
            IrcException {
        this.connect(hostname, port, password, null);
    }

    /**
     * Attempt to connect to the specified IRC server using the supplied
     * password. The onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     * @param password The password to use to join the server.
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname, int port, String password, SocketFactory socketFactory)
            throws IOException, IrcException {

        _server = hostname;
        _port = port;
        _password = password;

        if (isConnected()) {
            throw new IOException("The PircBot is already connected to an IRC server.  Disconnect first.");
        }

        // Don't clear the outqueue - there might be something important in it!

        // Clear everything we may have know about channels.
        this.removeAllChannels();

        InetSocketAddress ircServer = new InetSocketAddress(InetAddress.getByName(hostname), port);
        Socket socket;

        // Connect to the server.
        if (socketFactory == null) {
            //socket = new Socket(hostname, port);
            socket = new Socket();
        } else {
            //socket = socketFactory.createSocket(hostname, port);
            socket = socketFactory.createSocket();
        }
        // CraftIRC Start
        if (this.bindLocalAddr != null) {
            //socket.setReuseAddress(true);
            socket.bind(this.bindLocalAddr);
        }
        // CraftIRC End
        socket.connect(ircServer);
        try {
            this.log("*** Connected to server.");

            _inetAddress = socket.getLocalAddress();

            Reader inputStreamReader = createInputStreamReader(socket.getInputStream());
            Writer outputStreamWriter = createOutputStreamReader(socket.getOutputStream());

            BufferedReader breader = new BufferedReader(inputStreamReader);
            BufferedWriter bwriter = new BufferedWriter(outputStreamWriter);

            // Attempt to join the server.
            if (password != null && !password.equals("")) {
                OutputThread.sendRawLine(this, bwriter, "PASS " + password);
            }
            String nick = this.getName();
            OutputThread.sendRawLine(this, bwriter, "NICK " + nick);
            OutputThread.sendRawLine(this, bwriter, "USER " + this.getLogin() + " 8 * :" + this.getVersion());

            _inputThread = new InputThread(this, socket, breader, bwriter);

            // Read stuff back from the server to see if we connected.
            String line;
            int tries = 1;
            while ((line = breader.readLine()) != null) {

                this.handleLine(line);

                int firstSpace = line.indexOf(" ");
                int secondSpace = line.indexOf(" ", firstSpace + 1);
                if (secondSpace >= 0) {
                    String code = line.substring(firstSpace + 1, secondSpace);

                    if (code.equals("004")) {
                        // We're connected to the server.
                        break;
                    } else if (code.equals("433")) {
                        if (_autoNickChange) {
                            tries++;
                            nick = getName() + tries;
                            OutputThread.sendRawLine(this, bwriter, "NICK " + nick);
                        } else {
                            socket.close();
                            _inputThread = null;
                            throw new NickAlreadyInUseException(line);
                        }
                    } else if (code.equals(String.valueOf(ERR_NOMOTD))) {
                        log("Received response code 422 from server.  Server is missing a MOTD.  Ignoring.");
                    } else if (code.equals("439")) {
                        // seems to be sent from some servers to trick fake
                        // clients into disconnecting
                        log("Received response code 439 from server.  Ignoring as this shouldn't mean anything as it is sometimes used to trap bots and spammers.");
                    } else if (code.startsWith("5") || code.startsWith("4")) {
                        socket.close();
                        _inputThread = null;
                        throw new IrcException("Could not log into the IRC server: " + line);
                    }
                }
                this.setNick(nick);

            }

            this.log("*** Logged onto server.");

            // This makes the socket timeout on read operations after 5 minutes.
            // Maybe in some future version I will let the user change this at
            // runtime.
            socket.setSoTimeout(5 * 60 * 1000);

        } catch (IOException e) {
            _inputThread = null;
            try {
                socket.close();
            } catch (IOException ee) {
                // ignore
            }
            throw e;
        }

        // Now start the InputThread to read all other lines from the server.
        _inputThread.start();

        // Now start the outputThread that will be used to send all messages.
        if (_outputThread == null) {
            _outputThread = new OutputThread(this, _outQueue);
            _outputThread.start();
        }

        this.onConnect();

    }

    public Reader createInputStreamReader(InputStream inputStream) throws UnsupportedEncodingException {
        if (getFallbackEncoding() != null) {
            return new FallbackInputStreamReader(this, inputStream, getEncoding(), getFallbackEncoding());
        } else if (getEncoding() != null) {
            // Assume the specified encoding is valid for this JVM.
            return new InputStreamReader(inputStream, getEncoding());
        } else {
            // Otherwise, just use the JVM's default encoding.
            return new InputStreamReader(inputStream);
        }
    }

    public Writer createOutputStreamReader(OutputStream outputStream) throws UnsupportedEncodingException {
        if (getEncoding() != null) {
            // Assume the specified encoding is valid for this JVM.
            return new OutputStreamWriter(outputStream, getEncoding());
        } else {
            // Otherwise, just use the JVM's default encoding.
            return new OutputStreamWriter(outputStream);
        }
    }

    /**
     * Reconnects to the IRC server that we were previously connected to. If
     * necessary, the appropriate port number and password will be used. This
     * method will throw an IrcException if we have never connected to an IRC
     * server previously.
     *
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void reconnect() throws IOException, IrcException {
        if (getServer() == null) {
            throw new IrcException(
                    "Cannot reconnect to an IRC server because we were never connected to one previously!");
        }
        connect(getServer(), getPort(), getPassword(), getSocketFactory());
    }

    /**
     * This method disconnects from the server cleanly by calling the
     * quitServer() method. Providing the PircBot was connected to an IRC
     * server, the onDisconnect() will be called as soon as the disconnection is
     * made by the server.
     *
     * @see #quitServer() quitServer
     * @see #quitServer(String) quitServer
     */
    public final synchronized void disconnect() {
        this.quitServer();
    }

    /**
     * When you connect to a server and your nick is already in use and this is
     * set to true, a new nick will be automatically chosen. This is done by
     * adding numbers to the end of the nick until an available nick is found.
     *
     * @param autoNickChange Set to true if you want automatic nick changes during
     * connection.
     */
    public void setAutoNickChange(boolean autoNickChange) {
        _autoNickChange = autoNickChange;
    }

    /**
     * Starts an ident server (Identification Protocol Server, RFC 1413).
     * <p>
     * Most IRC servers attempt to contact the ident server on connecting hosts
     * in order to determine the user's identity. A few IRC servers will not
     * allow you to connect unless this information is provided.
     * <p>
     * So when a PircBot is run on a machine that does not run an ident server,
     * it may be necessary to call this method to start one up.
     * <p>
     * Calling this method starts up an ident server which will respond with the
     * login provided by calling getLogin() and then shut down immediately. It
     * will also be shut down if it has not been contacted within 60 seconds of
     * creation.
     * <p>
     * If you require an ident response, then the correct procedure is to start
     * the ident server and then connect to the IRC server. The IRC server may
     * then contact the ident server to get the information it needs.
     * <p>
     * The ident server will fail to start if there is already an ident server
     * running on port 113, or if you are running as an unprivileged user who is
     * unable to create a server socket on that port number.
     * <p>
     * If it is essential for you to use an ident server when connecting to an
     * IRC server, then make sure that port 113 on your machine is visible to
     * the IRC server so that it may contact the ident server.
     */
    public final void startIdentServer() {
        new IdentServer(this, getLogin());
    }

    /**
     * Joins a channel.
     *
     * @param channel The name of the channel to join (eg "#cs").
     */
    public final void joinChannel(String channel) {
        this.sendRawLine("JOIN " + channel);
        this.sendRawLine("MODE " + channel); // Added to parse the +c mode on join
    }

    /**
     * Joins a channel with a key.
     *
     * @param channel The name of the channel to join (eg "#cs").
     * @param key The key that will be used to join the channel.
     */
    public final void joinChannel(String channel, String key) {
        this.joinChannel(channel + " " + key);
    }

    /**
     * Parts a channel.
     *
     * @param channel The name of the channel to leave.
     */
    public final void partChannel(String channel) {
        this.sendRawLine("PART " + channel);
    }

    /**
     * Parts a channel, giving a reason.
     *
     * @param channel The name of the channel to leave.
     * @param reason The reason for parting the channel.
     */
    public final void partChannel(String channel, String reason) {
        this.sendRawLine("PART " + channel + " :" + reason);
    }

    /**
     * Quits from the IRC server. Providing we are actually connected to an IRC
     * server, the onDisconnect() method will be called as soon as the IRC
     * server disconnects us.
     */
    public final void quitServer() {
        this.quitServer("");
    }

    /**
     * Quits from the IRC server with a reason. Providing we are actually
     * connected to an IRC server, the onDisconnect() method will be called as
     * soon as the IRC server disconnects us.
     *
     * @param reason The reason for quitting the server.
     */
    public final void quitServer(String reason) {
        this.sendRawLine("QUIT :" + reason);
        if (isConnected()) {
            _inputThread.quitServer();
        }
    }

    /**
     * Sends a raw line to the IRC server as soon as possible, bypassing the
     * outgoing message queue.
     *
     * @param line The raw line to send to the IRC server.
     */
    public final synchronized void sendRawLine(String line) {
        if (isConnected()) {
            _inputThread.sendRawLine(line);
        }
    }

    /**
     * Sends a raw line through the outgoing message queue.
     *
     * @param line The raw line to send to the IRC server.
     */
    public final synchronized void sendRawLineViaQueue(String line) {
        if (line == null) {
            throw new NullPointerException("Cannot send null messages to server");
        }
        if (isConnected() && _outQueue.size() < _queueSize) {
            _outQueue.add(line);
        }
    }

    /**
     * Sends a message to a channel or a private message to a user. These
     * messages are added to the outgoing message queue and sent at the earliest
     * possible opportunity.
     * <p>
     * Some examples: -
     *
     * <pre>
     * // Send the message &quot;Hello!&quot; to the channel #cs.
     * sendMessage(&quot;#cs&quot;, &quot;Hello!&quot;);
     *
     * // Send a private message to Paul that says &quot;Hi&quot;.
     * sendMessage(&quot;Paul&quot;, &quot;Hi&quot;);
     * </pre>
     *
     * You may optionally apply colours, boldness, underlining, etc to the
     * message by using the <code>Colors</code> class.
     *
     * @param target The name of the channel or user nick to send to.
     * @param message The message to send.
     * @see Colors
     */
    public final void sendMessage(String target, String message) {
        _outQueue.add("PRIVMSG " + target + " :" + message);
    }

    /**
     * Sends an action to the channel or to a user.
     *
     * @param target The name of the channel or user nick to send to.
     * @param action The action to send.
     * @see Colors
     */
    public final void sendAction(String target, String action) {
        sendCTCPCommand(target, "ACTION " + action);
    }

    /**
     * Sends a notice to the channel or to a user.
     *
     * @param target The name of the channel or user nick to send to.
     * @param notice The notice to send.
     */
    public final void sendNotice(String target, String notice) {
        _outQueue.add("NOTICE " + target + " :" + notice);
    }

    /**
     * Sends a CTCP command to a channel or user. (Client to client protocol).
     * Examples of such commands are "PING <number>", "FINGER", "VERSION", etc.
     * For example, if you wish to request the version of a user called "Dave",
     * then you would call <code>sendCTCPCommand("Dave", "VERSION");</code>. The
     * type of response to such commands is largely dependant on the target
     * client software.
     *
     * @param target The name of the channel or user to send the CTCP message to.
     * @param command The CTCP command to send.
     */
    public final void sendCTCPCommand(String target, String command) {
        _outQueue.add("PRIVMSG " + target + " :\u0001" + command + "\u0001");
    }

    /**
     * Attempt to change the current nick (nickname) of the bot when it is
     * connected to an IRC server. After confirmation of a successful nick
     * change, the getNick method will return the new nick.
     *
     * @param newNick The new nick to use.
     */
    public final void changeNick(String newNick) {
        this.sendRawLine("NICK " + newNick);
    }

    /**
     * Identify the bot with NickServ, supplying the appropriate password. Some
     * IRC Networks (such as freenode) require users to <i>register</i> and
     * <i>identify</i> with NickServ before they are able to send private
     * messages to other users, thus reducing the amount of spam. If you are
     * using an IRC network where this kind of policy is enforced, you will need
     * to make your bot <i>identify</i> itself to NickServ before you can send
     * private messages. Assuming you have already registered your bot's nick
     * with NickServ, this method can be used to <i>identify</i> with the
     * supplied password. It usually makes sense to identify with NickServ
     * immediately after connecting to a server.
     * <p>
     * This method issues a raw NICKSERV command to the server, and is therefore
     * safer than the alternative approach of sending a private message to
     * NickServ. The latter approach is considered dangerous, as it may cause
     * you to inadvertently transmit your password to an untrusted party if you
     * connect to a network which does not run a NickServ service and where the
     * untrusted party has assumed the nick "NickServ". However, if your IRC
     * network is only compatible with the private message approach, you may
     * typically identify like so:
     *
     * <pre>
     * sendMessage(&quot;NickServ&quot;, &quot;identify PASSWORD&quot;);
     * </pre>
     *
     * @param password The password which will be used to identify with NickServ.
     */
    public final void identify(String password) {
        this.sendRawLine("NICKSERV IDENTIFY " + password);
    }

    /**
     * Set the mode of a channel. This method attempts to set the mode of a
     * channel. This may require the bot to have operator status on the channel.
     * For example, if the bot has operator status, we can grant operator status
     * to "Dave" on the #cs channel by calling setMode("#cs", "+o Dave"); An
     * alternative way of doing this would be to use the op method.
     *
     * @param channel The channel on which to perform the mode change.
     * @param mode The new mode to apply to the channel. This may include zero or
     * more arguments if necessary.
     * @see #op(String, String) op
     */
    public final void setMode(String channel, String mode) {
        this.sendRawLine("MODE " + channel + " " + mode);
    }

    /**
     * Sends an invitation to join a channel. Some channels can be marked as
     * "invite-only", so it may be useful to allow a bot to invite people into
     * it.
     *
     * @param nick The nick of the user to invite
     * @param channel The channel you are inviting the user to join.
     */
    public final void sendInvite(String nick, String channel) {
        this.sendRawLine("INVITE " + nick + " :" + channel);
    }

    /**
     * Bans a user from a channel. An example of a valid hostmask is
     * "*!*compu@*.18hp.net". This may be used in conjunction with the kick
     * method to permanently remove a user from a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel to ban the user from.
     * @param hostmask A hostmask representing the user we're banning.
     */
    public final void ban(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " +b " + hostmask);
    }

    /**
     * Unbans a user from a channel. An example of a valid hostmask is
     * "*!*compu@*.18hp.net". Successful use of this method may require the bot
     * to have operator status itself.
     *
     * @param channel The channel to unban the user from.
     * @param hostmask A hostmask representing the user we're unbanning.
     */
    public final void unBan(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " -b " + hostmask);
    }

    /**
     * Grants operator privilidges to a user on a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel we're opping the user on.
     * @param nick The nick of the user we are opping.
     */
    public final void op(String channel, String nick) {
        this.setMode(channel, "+o " + nick);
    }

    /**
     * Removes operator privilidges from a user on a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel we're deopping the user on.
     * @param nick The nick of the user we are deopping.
     */
    public final void deOp(String channel, String nick) {
        this.setMode(channel, "-o " + nick);
    }

    /**
     * Grants voice privilidges to a user on a channel. Successful use of this
     * method may require the bot to have operator status itself.
     *
     * @param channel The channel we're voicing the user on.
     * @param nick The nick of the user we are voicing.
     */
    public final void voice(String channel, String nick) {
        this.setMode(channel, "+v " + nick);
    }

    /**
     * Removes voice privilidges from a user on a channel. Successful use of
     * this method may require the bot to have operator status itself.
     *
     * @param channel The channel we're devoicing the user on.
     * @param nick The nick of the user we are devoicing.
     */
    public final void deVoice(String channel, String nick) {
        this.setMode(channel, "-v " + nick);
    }

    /**
     * Set the topic for a channel. This method attempts to set the topic of a
     * channel. This may require the bot to have operator status if the topic is
     * protected.
     *
     * @param channel The channel on which to perform the mode change.
     * @param topic The new topic for the channel.
     */
    public final void setTopic(String channel, String topic) {
        this.sendRawLine("TOPIC " + channel + " :" + topic);
    }

    /**
     * Kicks a user from a channel. This method attempts to kick a user from a
     * channel and may require the bot to have operator status in the channel.
     *
     * @param channel The channel to kick the user from.
     * @param nick The nick of the user to kick.
     */
    public final void kick(String channel, String nick) {
        this.kick(channel, nick, "");
    }

    /**
     * Kicks a user from a channel, giving a reason. This method attempts to
     * kick a user from a channel and may require the bot to have operator
     * status in the channel.
     *
     * @param channel The channel to kick the user from.
     * @param nick The nick of the user to kick.
     * @param reason A description of the reason for kicking a user.
     */
    public final void kick(String channel, String nick, String reason) {
        this.sendRawLine("KICK " + channel + " " + nick + " :" + reason);
    }

    /**
     * Issues a request for a list of all channels on the IRC server. When the
     * PircBot receives information for each channel, it will call the
     * onChannelInfo method, which you will need to override if you want it to
     * do anything useful.
     *
     * @see #onChannelInfo(String, int, String) onChannelInfo
     */
    public final void listChannels() {
        this.listChannels(null);
    }

    /**
     * Issues a request for a list of all channels on the IRC server. When the
     * PircBot receives information for each channel, it will call the
     * onChannelInfo method, which you will need to override if you want it to
     * do anything useful.
     * <p>
     * Some IRC servers support certain parameters for LIST requests. One
     * example is a parameter of ">10" to list only those channels that have
     * more than 10 users in them. Whether these parameters are supported or not
     * will depend on the IRC server software.
     *
     * @param parameters The parameters to supply when requesting the list.
     * @see #onChannelInfo(String, int, String) onChannelInfo
     */
    public final void listChannels(String parameters) {
        if (parameters == null) {
            this.sendRawLine("LIST");
        } else {
            this.sendRawLine("LIST " + parameters);
        }
    }

    /**
     * Sends a file to another user. Resuming is supported. The other user must
     * be able to connect directly to your bot to be able to receive the file.
     * <p>
     * You may throttle the speed of this file transfer by calling the
     * setPacketDelay method on the DccFileTransfer that is returned.
     * <p>
     * This method may not be overridden.
     *
     * @param file The file to send.
     * @param nick The user to whom the file is to be sent.
     * @param timeout The number of milliseconds to wait for the recipient to
     * acccept the file (we recommend about 120000).
     * @return The DccFileTransfer that can be used to monitor this transfer.
     * @see DccFileTransfer
     */
    public final DccFileTransfer dccSendFile(File file, String nick, int timeout) {
        DccFileTransfer transfer = new DccFileTransfer(this, _dccManager, file, nick, timeout);
        transfer.doSend(true);
        return transfer;
    }

    /**
     * Receives a file that is being sent to us by a DCC SEND request. Please
     * use the onIncomingFileTransfer method to receive files.
     *
     * @deprecated As of PircBot 1.2.0, use
     * {@link #onIncomingFileTransfer(DccFileTransfer)}
     */
    protected final void dccReceiveFile(File file, long address, int port, int size) {
        throw new RuntimeException("dccReceiveFile is deprecated, please use sendFile");
    }

    /**
     * Attempts to establish a DCC CHAT session with a client. This method
     * issues the connection request to the client and then waits for the client
     * to respond. If the connection is successfully made, then a DccChat object
     * is returned by this method. If the connection is not made within the time
     * limit specified by the timeout value, then null is returned.
     * <p>
     * It is <b>strongly recommended</b> that you call this method within a new
     * Thread, as it may take a long time to return.
     * <p>
     * This method may not be overridden.
     *
     * @param nick The nick of the user we are trying to establish a chat with.
     * @param timeout The number of milliseconds to wait for the recipient to accept
     * the chat connection (we recommend about 120000).
     * @return a DccChat object that can be used to send and recieve lines of
     * text. Returns <b>null</b> if the connection could not be made.
     * @see DccChat
     */
    public final DccChat dccSendChatRequest(String nick, int timeout) {
        DccChat chat = null;
        try {
            ServerSocket ss = null;

            int[] ports = getDccPorts();
            if (ports == null) {
                // Use any free port.
                ss = new ServerSocket(0);
            } else {
                for (int port : ports) {
                    try {
                        ss = new ServerSocket(port);
                        // Found a port number we could use.
                        break;
                    } catch (Exception e) {
                        // Do nothing; go round and try another port.
                    }
                }
                if (ss == null) {
                    // No ports could be used.
                    throw new IOException("All ports returned by getDccPorts() are in use.");
                }
            }

            ss.setSoTimeout(timeout);
            int port = ss.getLocalPort();

            InetAddress inetAddress = getDccInetAddress();
            if (inetAddress == null) {
                inetAddress = getInetAddress();
            }
            byte[] ip = inetAddress.getAddress();
            long ipNum = ipToLong(ip);

            sendCTCPCommand(nick, "DCC CHAT chat " + ipNum + " " + port);

            // The client may now connect to us to chat.
            Socket socket = ss.accept();

            // Close the server socket now that we've finished with it.
            ss.close();

            chat = new DccChat(this, nick, socket);
        } catch (Exception e) {
            // Do nothing.
        }
        return chat;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.ensifera.animosity.craftirc.libs.org.jibble.pircbot.PircBotLogger#log(java.lang.String)
     */
    public void log(String line) {
        if (_verbose) {
            System.out.println(System.currentTimeMillis() + " " + line);
        }
    }

    /**
     * This method handles events when any line of text arrives from the server,
     * then calling the appropriate method in the PircBot. This method is
     * protected and only called by the InputThread for this instance.
     * <p>
     * This method may not be overridden!
     *
     * @param line The raw line of text from the server.
     */
    protected void handleLine(String line) {
        // an empty line from a (non-standard) server causes a failure later on,
        // so skip it
        if (line.length() == 0) {
            return;
        }

        this.log(line);

        // Check for server pings.
        if (line.startsWith("PING ")) {
            // Respond to the ping and return immediately.
            this.onServerPing(line.substring(5));
            return;
        }

        String sourceNick = "";
        String sourceLogin = "";
        String sourceHostname = "";

        StringTokenizer tokenizer = new StringTokenizer(line);
        String senderInfo = tokenizer.nextToken();
        String command = tokenizer.nextToken();
        String target = null;

        int exclamation = senderInfo.indexOf("!");
        int at = senderInfo.indexOf("@");
        if (senderInfo.startsWith(":")) {
            if (exclamation > 0 && at > 0 && exclamation < at) {
                sourceNick = senderInfo.substring(1, exclamation);
                sourceLogin = senderInfo.substring(exclamation + 1, at);
                sourceHostname = senderInfo.substring(at + 1);
            } else {

                if (tokenizer.hasMoreTokens()) {

                    int code = -1;
                    try {
                        code = Integer.parseInt(command);
                    } catch (NumberFormatException e) {
                        // Keep the existing value.
                    }

                    if (code != -1) {
                        String response = line
                                .substring(line.indexOf(command, senderInfo.length()) + 4, line.length());
                        this.processServerResponse(code, response);
                        // Return from the method.
                        return;
                    } else {
                        // This is not a server response.
                        // It must be a nick without login and hostname.
                        // (or maybe a NOTICE or suchlike from the server)
                        sourceNick = senderInfo;
                        target = command;
                    }
                } else {
                    // We don't know what this line means.
                    this.onUnknown(line);
                    // Return from the method;
                    return;
                }

            }
        }

        command = command.toUpperCase();
        if (sourceNick.startsWith(":")) {
            sourceNick = sourceNick.substring(1);
        }
        if (target == null) {
            target = tokenizer.nextToken();
        }
        if (target.startsWith(":")) {
            target = target.substring(1);
        }

        // Check for CTCP requests.
        if (command.equals("PRIVMSG") && line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
            String request = line.substring(line.indexOf(":\u0001") + 2, line.length() - 1);
            if (request.equals("VERSION")) {
                // VERSION request
                this.onVersion(sourceNick, sourceLogin, sourceHostname, target);
            } else if (request.startsWith("ACTION ")) {
                // ACTION request
                this.onAction(sourceNick, sourceLogin, sourceHostname, target, request.substring(7));
            } else if (request.startsWith("PING ")) {
                // PING request
                this.onPing(sourceNick, sourceLogin, sourceHostname, target, request.substring(5));
            } else if (request.equals("TIME")) {
                // TIME request
                this.onTime(sourceNick, sourceLogin, sourceHostname, target);
            } else if (request.equals("FINGER")) {
                // FINGER request
                this.onFinger(sourceNick, sourceLogin, sourceHostname, target);
            } else if ((tokenizer = new StringTokenizer(request)).countTokens() >= 5
                    && tokenizer.nextToken().equals("DCC")) {
                // This is a DCC request.
                boolean success = _dccManager.processRequest(sourceNick, sourceLogin, sourceHostname, request);
                if (!success) {
                    // The DccManager didn't know what to do with the line.
                    this.onUnknown(line);
                }
            } else {
                // An unknown CTCP message - ignore it.
                this.onUnknown(line);
            }
        } else if (command.equals("PRIVMSG") && _channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            // This is a normal message to a channel.
            this.onMessage(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("PRIVMSG")) {
            // This is a private message to us.
            this.onPrivateMessage(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("JOIN")) {
            // Someone is joining a channel.
            this.addUser(target, new User("", sourceNick));
            this.onJoin(target, sourceNick, sourceLogin, sourceHostname);
        } else if (command.equals("PART")) {
            // Someone is parting from a channel.
            this.removeUser(target, sourceNick);
            if (sourceNick.equals(this.getNick())) {
                this.removeChannel(target);
            }
            int start = line.indexOf(" :");
            if (start > 0) {
                String reason = line.substring(start + 2);
                this.onPart(target, sourceNick, sourceLogin, sourceHostname, reason);
            } else {
                this.onPart(target, sourceNick, sourceLogin, sourceHostname, "");
            }
            this.onPart(target, sourceNick, sourceLogin, sourceHostname, "");
        } else if (command.equals("NICK")) {
            // ADDED BY Protected FOR CraftIRC
            Enumeration<String> enumeration = _channels.keys();
            while (enumeration.hasMoreElements()) {
                String channel = enumeration.nextElement();
                if (this.getUser(sourceNick, channel) != null) {
                    this.onChannelNickChange(channel, sourceNick, sourceLogin, sourceHostname, target);
                }
            }
            // Somebody is changing their nick.
            this.renameUser(sourceNick, target);
            if (sourceNick.equals(this.getNick())) {
                // Update our nick if it was us that changed nick.
                this.setNick(target);
            }
            this.onNickChange(sourceNick, sourceLogin, sourceHostname, target);
        } else if (command.equals("NOTICE")) {
            // Someone is sending a notice.
            this.onNotice(sourceNick, sourceLogin, sourceHostname, target, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("QUIT")) {
            // ADDED BY Protected FOR CraftIRC
            Enumeration<String> enumeration = _channels.keys();
            while (enumeration.hasMoreElements()) {
                String channel = enumeration.nextElement();
                if (this.getUser(sourceNick, channel) != null) {
                    this.onChannelQuit(channel, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
                }
            }
            // Someone has quit from the IRC server.
            if (sourceNick.equals(this.getNick())) {
                this.removeAllChannels();
            } else {
                this.removeUser(sourceNick);
            }
            _outQueue.clear();
            this.onQuit(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("KICK")) {
            // Somebody has been kicked from a channel.
            String recipient = tokenizer.nextToken();
            if (recipient.equals(this.getNick())) {
                this.removeChannel(target);
            }
            this.removeUser(target, recipient);
            this.onKick(target, sourceNick, sourceLogin, sourceHostname, recipient,
                    line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("MODE")) {
            // Somebody is changing the mode on a channel or user.
            String mode = line.substring(line.indexOf(target, 2) + target.length() + 1);
            if (mode.startsWith(":")) {
                mode = mode.substring(1);
            }
            this.processMode(target, sourceNick, sourceLogin, sourceHostname, mode);
        } else if (command.equals("TOPIC")) {
            // Someone is changing the topic.
            this.onTopic(target, line.substring(line.indexOf(" :") + 2), sourceNick, System.currentTimeMillis(), true);
        } else if (command.equals("INVITE")) {
            // Somebody is inviting somebody else into a channel.
            this.onInvite(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else {
            // If we reach this point, then we've found something that the
            // PircBot
            // Doesn't currently deal with.
            this.onUnknown(line);
        }

    }

    /**
     * This method is called once the PircBot has successfully connected to the
     * IRC server.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     */
    protected void onConnect() {
    }

    /**
     * This method carries out the actions to be performed when the PircBot gets
     * disconnected. This may happen if the PircBot quits from the server, or if
     * the connection is unexpectedly lost.
     * <p>
     * Disconnection from the IRC server is detected immediately if either we or
     * the server close the connection normally. If the connection to the server
     * is lost, but neither we nor the server have explicitly closed the
     * connection, then it may take a few minutes to detect (this is commonly
     * referred to as a "ping timeout").
     * <p>
     * If you wish to get your IRC bot to automatically rejoin a server after
     * the connection has been lost, then this is probably the ideal method to
     * override to implement such functionality.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     */
    protected void onDisconnect() {
    }

    /**
     * This method is called by the PircBot when a numeric response is received
     * from the IRC server. We use this method to allow PircBot to process
     * various responses from the server before then passing them on to the
     * onServerResponse method.
     * <p>
     * Note that this method is private and should not appear in any of the
     * javadoc generated documenation.
     *
     * @param code The three-digit numerical code for the response.
     * @param response The full response from the IRC server.
     */
    private void processServerResponse(int code, String response) {

        if (code == RPL_LIST) {
            // This is a bit of information about a channel.
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int thirdSpace = response.indexOf(' ', secondSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            int userCount = 0;
            try {
                userCount = Integer.parseInt(response.substring(secondSpace + 1, thirdSpace));
            } catch (NumberFormatException e) {
                // Stick with the value of zero.
            }
            String topic = response.substring(colon + 1);
            this.onChannelInfo(channel, userCount, topic);
        } else if (code == RPL_TOPIC) {
            // This is topic information about a channel we've just joined.
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            String topic = response.substring(colon + 1);

            _topics.put(channel, topic);
        } else if (code == RPL_TOPICINFO) {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String channel = tokenizer.nextToken();
            String setBy = tokenizer.nextToken();
            long date = 0;
            try {
                date = Long.parseLong(tokenizer.nextToken()) * 1000;
            } catch (NumberFormatException e) {
                // Stick with the default value of zero.
            }

            String topic = _topics.get(channel);
            _topics.remove(channel);

            this.onTopic(channel, topic, setBy, date, false);
        } else if (code == RPL_NAMREPLY) {
            // This is a list of nicks in a channel that we've just joined.
            int channelEndIndex = response.indexOf(" :");
            String channel = response.substring(response.lastIndexOf(' ', channelEndIndex - 1) + 1, channelEndIndex);

            StringTokenizer tokenizer = new StringTokenizer(response.substring(response.indexOf(" :") + 2));
            while (tokenizer.hasMoreTokens()) {
                String nick = tokenizer.nextToken();
                String prefix = "";
                while (nick.length() > 0) {
                    char first = nick.charAt(0);
                    if (first >= 0x41 && first <= 0x7D) {
                        break;
                    } else {
                        prefix += first;
                        nick = nick.substring(1);
                    }
                }
                this.addUser(channel, new User(prefix, nick));
            }
        } else if (code == RPL_ENDOFNAMES) {
            // This is the end of a NAMES list, so we know that we've got
            // the full list of users in the channel that we just joined.
            String channel = response.substring(response.indexOf(' ') + 1, response.indexOf(" :"));
            User[] users = this.getUsers(channel);
            this.onUserList(channel, users);
        } else if (code == RPL_BOUNCE) {
            String[] split = response.split("\\s+");

            for (String aSplit : split) {
                // Populate PREFIXES
                if (aSplit.startsWith(PREFIX_DEF)) {
                    List<String> prefixes = null;
                    List<String> modes = null;
                    String prefixresponse = aSplit.substring(aSplit.indexOf(PREFIX_DEF) + PREFIX_DEF.length());
                    Pattern pat = Pattern.compile("\\(([A-Za-z]+)\\)(.*+)");
                    Matcher matches = pat.matcher(prefixresponse);

                    if (matches.find()) {
                        setSupportedModes(matches.group(1));
                        setSupportedPrefixes(matches.group(2));
                        prefixes = getSupportedPrefixes();
                        modes = getSupportedModes();
                    }

                    if ((modes != null && prefixes != null) && (modes.size() == prefixes.size())) {

                        _userPrefixes.clear();
                        _userPrefixOrder = "";

                        for (int x = 0; x < modes.size(); x++) {
                            _userPrefixes.put(modes.get(x), prefixes.get(x));
                            _userPrefixOrder += prefixes.get(x);
                        }
                    }
                    log("*** SERVER SUPPORTS PREFIXES: " + _userPrefixes.toString() + " | Order: " + _userPrefixOrder);
                } else if (aSplit.startsWith(NETWORK_DEF)) {
                    this.networkName = aSplit.substring(aSplit.indexOf("=") + 1);
                }
            }
        } else if (code == RPL_CHANNELMODEIS) {
            // Reply to the MODE #channel command sent after join
            String[] split = response.split("\\s+");
            if (split.length >= 3) {
                String channel = split[1];
                // We're only interested in the +c part.
                if (split[2].contains("c")) {
                    onBlockColors(channel, "", "", "");
                } else {
                    onUnblockColors(channel, "", "", "");
                }
            }
        }

        this.onServerResponse(code, response);
    }

    /**
     * This method is called when we receive a numeric response from the IRC
     * server.
     * <p>
     * Numerics in the range from 001 to 099 are used for client-server
     * connections only and should never travel between servers. Replies
     * generated in response to commands are found in the range from 200 to 399.
     * Error replies are found in the range from 400 to 599.
     * <p>
     * For example, we can use this method to discover the topic of a channel
     * when we join it. If we join the channel #test which has a topic of
     * &quot;I am King of Test&quot; then the response will be &quot;
     * <code>PircBot #test :I Am King of Test</code>&quot; with a code of 332 to
     * signify that this is a topic. (This is just an example - note that
     * overriding the <code>onTopic</code> method is an easier way of finding
     * the topic for a channel). Check the IRC RFC for the full list of other
     * command response codes.
     * <p>
     * PircBot implements the interface ReplyConstants, which contains
     * contstants that you may find useful here.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param code The three-digit numerical code for the response.
     * @param response The full response from the IRC server.
     * @see ReplyConstants
     */
    protected void onServerResponse(int code, String response) {
    }

    /**
     * This method is called when we receive a user list from the server after
     * joining a channel.
     * <p>
     * Shortly after joining a channel, the IRC server sends a list of all users
     * in that channel. The PircBot collects this information and calls this
     * method as soon as it has the full list.
     * <p>
     * To obtain the nick of each user in the channel, call the getNick() method
     * on each User object in the array.
     * <p>
     * At a later time, you may call the getUsers method to obtain an up to date
     * list of the users in the channel.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The name of the channel.
     * @param users An array of User objects belonging to this channel.
     * @see User
     */
    protected void onUserList(String channel, User[] users) {
    }

    /**
     * This method is called whenever a message is sent to a channel.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel to which the message was sent.
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
    }

    /**
     * This method is called whenever a private message is sent to the PircBot.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sender The nick of the person who sent the private message.
     * @param login The login of the person who sent the private message.
     * @param hostname The hostname of the person who sent the private message.
     * @param message The actual message.
     */
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
    }

    /**
     * This method is called whenever an ACTION is sent from a user. E.g. such
     * events generated by typing "/me goes shopping" in most IRC clients.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sender The nick of the user that sent the action.
     * @param login The login of the user that sent the action.
     * @param hostname The hostname of the user that sent the action.
     * @param target The target of the action, be it a channel or our nick.
     * @param action The action carried out by the user.
     */
    protected void onAction(String sender, String login, String hostname, String target, String action) {
    }

    /**
     * This method is called whenever we receive a notice.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param target The target of the notice, be it our nick or a channel name.
     * @param notice The notice message.
     */
    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
    }

    /**
     * This method is called whenever someone (possibly us) joins a channel
     * which we are on.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    protected void onJoin(String channel, String sender, String login, String hostname) {
    }

    /**
     * This method is called whenever someone (possibly us) parts a channel
     * which we are on.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     * <p>
     * <b>Note: </b>this method is always called to maintain backwards
     * compatibility with PircBot 1.4.4 implementations, the onPart(String
     * channel, String sender, String login, String hostname, String reason)
     * version is also called as well.
     *
     * @param channel The channel which somebody parted from.
     * @param sender The nick of the user who parted from the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     * @deprecated use
     * {@link #onPart(String channel, String sender, String login, String hostname, String reason)}
     * instead.
     */
    protected void onPart(String channel, String sender, String login, String hostname) {
    }

    /**
     * This method is called whenever someone (possibly us) parts a channel
     * which we are on. The reason is included when used, an empty String
     * otherwise.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     * <p>
     * <b>Note: </b>onPart(String channel, String sender, String login, String
     * hostname) is still called as well to ensure backwards compatibility with
     * PircBot 1.4.4 implementations. It is strongly recommended to switch to
     * using this new method as the original onPart() is now deprecated and will
     * be removed in a future version,
     *
     * @param channel The channel which somebody parted from.
     * @param sender The nick of the user who parted from the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     * @param reason The reason for the user parting the channel.
     */
    protected void onPart(String channel, String sender, String login, String hostname, String reason) {
    }

    /**
     * This method is called whenever someone (possibly us) changes nick on any
     * of the channels that we are on.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
    }

    // ADDED BY Protected FOR CraftIRC
    protected void onChannelNickChange(String channel, String oldNick, String login, String hostname, String newNick) {
    }

    /**
     * This method is called whenever someone (possibly us) is kicked from any
     * of the channels that we are in.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel from which the recipient was kicked.
     * @param kickerNick The nick of the user who performed the kick.
     * @param kickerLogin The login of the user who performed the kick.
     * @param kickerHostname The hostname of the user who performed the kick.
     * @param recipientNick The unfortunate recipient of the kick.
     * @param reason The reason given by the user who performed the kick.
     */
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
                          String recipientNick, String reason) {
    }

    /**
     * This method is called whenever someone (possibly us) quits from the
     * server. We will only observe this if the user was in one of the channels
     * to which we are connected.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param sourceNick The nick of the user that quit from the server.
     * @param sourceLogin The login of the user that quit from the server.
     * @param sourceHostname The hostname of the user that quit from the server.
     * @param reason The reason given for quitting the server.
     */
    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    }

    // ADDED BY Protected FOR CraftIRC
    protected void onChannelQuit(String channel, String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    }

    /**
     * This method is called whenever a user sets the topic, or when PircBot
     * joins a new channel and discovers its topic.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel that the topic belongs to.
     * @param topic The topic for the channel.
     * @param setBy The nick of the user that set the topic.
     * @param date When the topic was set (milliseconds since the epoch).
     * @param changed True if the topic has just been changed, false if the topic
     * was already there.
     */
    protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
    }

    /**
     * After calling the listChannels() method in PircBot, the server will start
     * to send us information about each channel on the server. You may override
     * this method in order to receive the information about each channel as
     * soon as it is received.
     * <p>
     * Note that certain channels, such as those marked as hidden, may not
     * appear in channel listings.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The name of the channel.
     * @param userCount The number of users visible in this channel.
     * @param topic The topic for this channel.
     * @see #listChannels() listChannels
     */
    protected void onChannelInfo(String channel, int userCount, String topic) {
    }

    /**
     * Called when the mode of a channel is set. We process this in order to
     * call the appropriate onOp, onDeop, etc method before finally calling the
     * override-able onMode method.
     * <p>
     * Note that this method is private and is not intended to appear in the
     * javadoc generated documentation.
     *
     * @param target The channel or nick that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode The mode that has been set.
     */
    private void processMode(String target, String sourceNick, String sourceLogin, String sourceHostname,
                             String mode) {

        if (_channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            // The mode of a channel is being changed.
            StringTokenizer tok = new StringTokenizer(mode);
            String[] params = new String[tok.countTokens()];

            int t = 0;
            while (tok.hasMoreTokens()) {
                params[t] = tok.nextToken();
                t++;
            }

            char pn = ' ';
            int p = 1;

            // All of this is very large and ugly, but it's the only way of
            // providing
            // what the users want :-/
            for (int i = 0; i < params[0].length(); i++) {
                char atPos = params[0].charAt(i);

                if (atPos == '+' || atPos == '-') {
                    pn = atPos;
                } else if (_userPrefixes.containsKey(atPos + "")) {
                    this.updateUser(target, pn, _userPrefixes.get(atPos + ""), params[p]);
                    // now deal with the known(standard) user modes
                    if (atPos == 'o') {
                        if (pn == '+') {
                            onOp(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                        } else {
                            onDeop(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                        }
                    } else if (atPos == 'v') {
                        if (pn == '+') {
                            onVoice(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                        } else {
                            onDeVoice(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                        }
                    }
                    p++;
                } else if (atPos == 'k') {
                    if (pn == '+') {
                        onSetChannelKey(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        onRemoveChannelKey(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'l') {
                    if (pn == '+') {
                        onSetChannelLimit(target, sourceNick, sourceLogin, sourceHostname, Integer.parseInt(params[p]));
                        p++;
                    } else {
                        onRemoveChannelLimit(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'b') {
                    if (pn == '+') {
                        onSetChannelBan(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        onRemoveChannelBan(target, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 't') {
                    if (pn == '+') {
                        onSetTopicProtection(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveTopicProtection(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'n') {
                    if (pn == '+') {
                        onSetNoExternalMessages(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveNoExternalMessages(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'i') {
                    if (pn == '+') {
                        onSetInviteOnly(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveInviteOnly(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'm') {
                    if (pn == '+') {
                        onSetModerated(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveModerated(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'p') {
                    if (pn == '+') {
                        onSetPrivate(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemovePrivate(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 's') {
                    if (pn == '+') {
                        onSetSecret(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveSecret(target, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'c') {
                    if (pn == '+') {
                        onBlockColors(target, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onUnblockColors(target, sourceNick, sourceLogin, sourceHostname);
                    }
                }
            }

            this.onMode(target, sourceNick, sourceLogin, sourceHostname, mode);
        } else {
            // The mode of a user is being changed.
            this.onUserMode(target, sourceNick, sourceLogin, sourceHostname, mode);
        }
    }

    /**
     * Called when the mode of a channel is set.
     * <p>
     * You may find it more convenient to decode the meaning of the mode string
     * by overriding the onOp, onDeOp, onVoice, onDeVoice, onChannelKey,
     * onDeChannelKey, onChannelLimit, onDeChannelLimit, onChannelBan or
     * onDeChannelBan methods as appropriate.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode The mode that has been set.
     */
    protected void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    /**
     * Called when the mode of a user is set.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param targetNick The nick that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode The mode that has been set.
     */
    protected void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname,
                              String mode) {
    }

    /**
     * Called when a user (possibly us) gets granted operator status for a
     * channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'opped'.
     */
    protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    /**
     * Called when a user (possibly us) gets operator status taken away.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'deopped'.
     */
    protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    /**
     * Called when a user (possibly us) gets voice status granted in a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'voiced'.
     */
    protected void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                           String recipient) {
    }

    /**
     * Called when a user (possibly us) gets voice status removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'devoiced'.
     */
    protected void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                             String recipient) {
    }

    /**
     * Called when a channel key is set. When the channel key has been set,
     * other users may only join that channel if they know the key. Channel keys
     * are sometimes referred to as passwords.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param key The new key for the channel.
     */
    protected void onSetChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                                   String key) {
    }

    /**
     * Called when a channel key is removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param key The key that was in use before the channel key was removed.
     */
    protected void onRemoveChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                                      String key) {
    }

    /**
     * Called when a user limit is set for a channel. The number of users in the
     * channel cannot exceed this limit.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param limit The maximum number of users that may be in this channel at the
     * same time.
     */
    protected void onSetChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                                     int limit) {
    }

    /**
     * Called when the user limit is removed for a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a user (possibly us) gets banned from a channel. Being banned
     * from a channel prevents any user with a matching hostmask from joining
     * the channel. For this reason, most bans are usually directly followed by
     * the user being kicked :-)
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param hostmask The hostmask of the user that has been banned.
     */
    protected void onSetChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                                   String hostmask) {
    }

    /**
     * Called when a hostmask ban is removed from a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param hostmask host mask
     */
    protected void onRemoveChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname,
                                      String hostmask) {
    }

    /**
     * Called when topic protection is enabled for a channel. Topic protection
     * means that only operators in a channel may change the topic.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when topic protection is removed for a channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to only allow messages from users that are
     * in the channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to allow messages from any user, even if
     * they are not actually in the channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveNoExternalMessages(String channel, String sourceNick, String sourceLogin,
                                              String sourceHostname) {
    }

    /**
     * Called when a channel is set to 'invite only' mode. A user may only join
     * the channel if they are invited by someone who is already in the channel.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel has 'invite only' removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to 'moderated' mode. If a channel is
     * moderated, then only users who have been 'voiced' or 'opped' may speak or
     * change their nicks.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel has moderated mode removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is marked as being in private mode.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetPrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is marked as not being in private mode.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemovePrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to be in 'secret' mode. Such channels
     * typically do not appear on a server's channel listing.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel has 'secret' mode removed.
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to +c
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onBlockColors(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when a channel is set to -c
     * <p>
     * This is a type of mode change and is also passed to the onMode method in
     * the PircBot class.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onUnblockColors(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    /**
     * Called when we are invited to a channel by a user.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param targetNick The nick of the user being invited - should be us!
     * @param sourceNick The nick of the user that sent the invitation.
     * @param sourceLogin The login of the user that sent the invitation.
     * @param sourceHostname The hostname of the user that sent the invitation.
     * @param channel The channel that we're being invited to.
     */
    protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname,
                            String channel) {
    }

    /**
     * This method used to be called when a DCC SEND request was sent to the
     * PircBot. Please use the onIncomingFileTransfer method to receive files,
     * as it has better functionality and supports resuming.
     *
     * @deprecated As of PircBot 1.2.0, use
     * {@link #onIncomingFileTransfer(DccFileTransfer)}
     */
    protected void onDccSendRequest(String sourceNick, String sourceLogin, String sourceHostname, String filename,
                                    long address, int port, int size) {
    }

    /**
     * This method used to be called when a DCC CHAT request was sent to the
     * PircBot. Please use the onIncomingChatRequest method to accept chats, as
     * it has better functionality.
     *
     * @deprecated As of PircBot 1.2.0, use
     * {@link #onIncomingChatRequest(DccChat)}
     */
    protected void onDccChatRequest(String sourceNick, String sourceLogin, String sourceHostname, long address, int port) {
    }

    /**
     * This method is called whenever a DCC SEND request is sent to the PircBot.
     * This means that a client has requested to send a file to us. This
     * abstract implementation performs no action, which means that all DCC SEND
     * requests will be ignored by default. If you wish to receive the file,
     * then you may override this method and call the receive method on the
     * DccFileTransfer object, which connects to the sender and downloads the
     * file.
     * <p>
     * Example:
     *
     * <pre>
     * public void onIncomingFileTransfer(DccFileTransfer transfer) {
     *     // Use the suggested file name.
     *     File file = transfer.getFile();
     *     // Receive the transfer and save it to the file, allowing resuming.
     *     transfer.receive(file, true);
     * }
     * </pre>
     * <p>
     * <b>Warning:</b> Receiving an incoming file transfer will cause a file to
     * be written to disk. Please ensure that you make adequate security checks
     * so that this file does not overwrite anything important!
     * <p>
     * Each time a file is received, it happens within a new Thread in order to
     * allow multiple files to be downloaded by the PircBot at the same time.
     * <p>
     * If you allow resuming and the file already partly exists, it will be
     * appended to instead of overwritten. If resuming is not enabled, the file
     * will be overwritten if it already exists.
     * <p>
     * You can throttle the speed of the transfer by calling the setPacketDelay
     * method on the DccFileTransfer object, either before you receive the file
     * or at any moment during the transfer.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param transfer The DcccFileTransfer that you may accept.
     * @see DccFileTransfer
     */
    protected void onIncomingFileTransfer(DccFileTransfer transfer) {
    }

    /**
     * This method gets called when a DccFileTransfer has finished. If there was
     * a problem, the Exception will say what went wrong. If the file was sent
     * successfully, the Exception will be null.
     * <p>
     * Both incoming and outgoing file transfers are passed to this method. You
     * can determine the type by calling the isIncoming or isOutgoing methods on
     * the DccFileTransfer object.
     *
     * @param transfer The DccFileTransfer that has finished.
     * @param e null if the file was transfered successfully, otherwise this
     * will report what went wrong.
     * @see DccFileTransfer
     */
    protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
    }

    /**
     * This method will be called whenever a DCC Chat request is received. This
     * means that a client has requested to chat to us directly rather than via
     * the IRC server. This is useful for sending many lines of text to and from
     * the bot without having to worry about flooding the server or any
     * operators of the server being able to "spy" on what is being said. This
     * abstract implementation performs no action, which means that all DCC CHAT
     * requests will be ignored by default.
     * <p>
     * If you wish to accept the connection, then you may override this method
     * and call the accept() method on the DccChat object, which connects to the
     * sender of the chat request and allows lines to be sent to and from the
     * bot.
     * <p>
     * Your bot must be able to connect directly to the user that sent the
     * request.
     * <p>
     * Example:
     *
     * <pre>
     * public void onIncomingChatRequest(DccChat chat) {
     *     try {
     *         // Accept all chat, whoever it's from.
     *         chat.accept();
     *         chat.sendLine(&quot;Hello&quot;);
     *         String response = chat.readLine();
     *         chat.close();
     *     } catch (IOException e) {
     *     }
     * }
     * </pre>
     *
     * Each time this method is called, it is called from within a new Thread so
     * that multiple DCC CHAT sessions can run concurrently.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param chat A DccChat object that represents the incoming chat request.
     * @see DccChat
     */
    protected void onIncomingChatRequest(DccChat chat) {
    }

    /**
     * This method is called whenever we receive a VERSION request. This
     * abstract implementation responds with the PircBot's _version string, so
     * if you override this method, be sure to either mimic its functionality or
     * to call super.onVersion(...);
     *
     * @param sourceNick The nick of the user that sent the VERSION request.
     * @param sourceLogin The login of the user that sent the VERSION request.
     * @param sourceHostname The hostname of the user that sent the VERSION request.
     * @param target The target of the VERSION request, be it our nick or a channel
     * name.
     */
    protected void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001VERSION " + _version + "\u0001");
    }

    /**
     * This method is called whenever we receive a PING request from another
     * user.
     * <p>
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onPing(...);
     *
     * @param sourceNick The nick of the user that sent the PING request.
     * @param sourceLogin The login of the user that sent the PING request.
     * @param sourceHostname The hostname of the user that sent the PING request.
     * @param target The target of the PING request, be it our nick or a channel
     * name.
     * @param pingValue The value that was supplied as an argument to the PING
     * command.
     */
    protected void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001PING " + pingValue + "\u0001");
    }

    /**
     * The actions to perform when a PING request comes from the server.
     * <p>
     * This sends back a correct response, so if you override this method, be
     * sure to either mimic its functionality or to call
     * super.onServerPing(response);
     *
     * @param response The response that should be given back in your PONG.
     */
    protected void onServerPing(String response) {
        this.sendRawLine("PONG " + response);
    }

    /**
     * This method is called whenever we receive a TIME request.
     * <p>
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onTime(...);
     *
     * @param sourceNick The nick of the user that sent the TIME request.
     * @param sourceLogin The login of the user that sent the TIME request.
     * @param sourceHostname The hostname of the user that sent the TIME request.
     * @param target The target of the TIME request, be it our nick or a channel
     * name.
     */
    protected void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001TIME " + new Date().toString() + "\u0001");
    }

    /**
     * This method is called whenever we receive a FINGER request.
     * <p>
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onFinger(...);
     *
     * @param sourceNick The nick of the user that sent the FINGER request.
     * @param sourceLogin The login of the user that sent the FINGER request.
     * @param sourceHostname The hostname of the user that sent the FINGER request.
     * @param target The target of the FINGER request, be it our nick or a channel
     * name.
     */
    protected void onFinger(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001FINGER " + _finger + "\u0001");
    }

    /**
     * This method is called whenever we receive a line from the server that the
     * PircBot has not been programmed to recognise.
     * <p>
     * The implementation of this method in the PircBot abstract class performs
     * no actions and may be overridden as required.
     *
     * @param line The raw line that was received from the server.
     */
    protected void onUnknown(String line) {
        // And then there were none :)
    }

    /**
     * Sets the verbose mode. If verbose mode is set to true, then log entries
     * will be printed to the standard output (through the {@link #log(String)}
     * method, which can be overridden). The default value is false and will
     * result in no output. For general development, we strongly recommend
     * setting the verbose mode to true.
     *
     * @param verbose true if verbose mode is to be used. Default is false.
     */
    public final void setVerbose(boolean verbose) {
        _verbose = verbose;
    }

    /**
     * Sets the name of the bot, which will be used as its nick when it tries to
     * join an IRC server. This should be set before joining any servers,
     * otherwise the default nick will be used. You would typically call this
     * method from the constructor of the class that extends PircBot.
     * <p>
     * The changeNick method should be used if you wish to change your nick when
     * you are connected to a server.
     *
     * @param name The new name of the Bot.
     */
    protected final void setName(String name) {
        _name = name;
    }

    /**
     * Sets the internal nick of the bot. This is only to be called by the
     * PircBot class in response to notification of nick changes that apply to
     * us.
     *
     * @param nick The new nick.
     */
    private void setNick(String nick) {
        _nick = nick;
    }

    /**
     * Sets the internal login of the Bot. This should be set before joining any
     * servers.
     *
     * @param login The new login of the Bot.
     */
    protected final void setLogin(String login) {
        _login = login;
    }

    /**
     * Sets the internal version of the Bot. This should be set before joining
     * any servers.
     *
     * @param version The new version of the Bot.
     */
    protected final void setVersion(String version) {
        _version = version;
    }

    /**
     * Sets the interal finger message. This should be set before joining any
     * servers.
     *
     * @param finger The new finger message for the Bot.
     */
    protected final void setFinger(String finger) {
        _finger = finger;
    }

    /**
     * Gets the verbose mode. If verbose mode is set to true, then log entries
     * will be printed to the standard output (through the {@link #log(String)}
     * method, which can be overridden). The default value is false and will
     * result in no output. For general development, we strongly recommend
     * setting the verbose mode to true.
     *
     * @return true if verbose mode is enabled.
     */
    public boolean isVerbose() {
        return _verbose;
    }

    /**
     * Gets the name of the PircBot. This is the name that will be used as as a
     * nick when we try to join servers.
     *
     * @return The name of the PircBot.
     */
    public final String getName() {
        return _name;
    }

    /**
     * Returns the current nick of the bot. Note that if you have just changed
     * your nick, this method will still return the old nick until confirmation
     * of the nick change is received from the server.
     * <p>
     * The nick returned by this method is maintained only by the PircBot class
     * and is guaranteed to be correct in the context of the IRC server.
     *
     * @return The current nick of the bot.
     */
    public String getNick() {
        return _nick;
    }

    /**
     * Gets the internal login of the PircBot.
     *
     * @return The login of the PircBot.
     */
    public final String getLogin() {
        return _login;
    }

    /**
     * Gets the internal version of the PircBot.
     *
     * @return The version of the PircBot.
     */
    public final String getVersion() {
        return _version;
    }

    /**
     * Gets the internal finger message of the PircBot.
     *
     * @return The finger message of the PircBot.
     */
    public final String getFinger() {
        return _finger;
    }

    /**
     * Returns whether or not the PircBot is currently connected to a server.
     * The result of this method should only act as a rough guide, as the result
     * may not be valid by the time you act upon it.
     *
     * @return True if and only if the PircBot is currently connected to a
     * server.
     */
    public final synchronized boolean isConnected() {
        return _inputThread != null && _inputThread.isConnected();
    }

    /**
     * Sets the number of milliseconds to delay between consecutive messages
     * when there are multiple messages waiting in the outgoing message queue.
     * This has a default value of 1000ms. It is a good idea to stick to this
     * default value, as it will prevent your bot from spamming servers and
     * facing the subsequent wrath! However, if you do need to change this delay
     * value (<b>not recommended</b>), then this is the method to use.
     *
     * @param delay The number of milliseconds between each outgoing message.
     */
    public final void setMessageDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Cannot have a negative time.");
        }
        _messageDelay = delay;
    }

    /**
     * Returns the number of milliseconds that will be used to separate
     * consecutive messages to the server from the outgoing message queue.
     *
     * @return Number of milliseconds.
     */
    public final long getMessageDelay() {
        return _messageDelay;
    }

    //Added by Protected
    public void setQueueSize(long size) {
        if (size < 2) {
            throw new IllegalArgumentException("Cannot have a negative time.");
        }
        _queueSize = size;
    }

    public long getQueueSize() {
        return _queueSize;
    }

    /**
     * Gets the maximum length of any line that is sent via the IRC protocol.
     * The IRC RFC specifies that line lengths, including the trailing \r\n must
     * not exceed 512 bytes. Hence, there is currently no option to change this
     * value in PircBot. All lines greater than this length will be truncated
     * before being sent to the IRC server.
     *
     * @return The maximum line length (currently fixed at 512)
     */
    public final int getMaxLineLength() {
        return InputThread.MAX_LINE_LENGTH;
    }

    /**
     * Gets the number of lines currently waiting in the outgoing message Queue.
     * If this returns 0, then the Queue is empty and any new message is likely
     * to be sent to the IRC server immediately.
     *
     * @return The number of lines in the outgoing message Queue.
     */
    public final int getOutgoingQueueSize() {
        return _outQueue.size();
    }

    /**
     * Returns the name of the last IRC server the PircBot tried to connect to.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method). A value of null
     * is returned if the PircBot has never tried to connect to a server.
     *
     * @return The name of the last machine we tried to connect to. Returns null
     * if no connection attempts have ever been made.
     */
    public final String getServer() {
        return _server;
    }

    /**
     * Returns the port number of the last IRC server that the PircBot tried to
     * connect to. This does not imply that the connection attempt to the server
     * was successful (we suggest you look at the onConnect method). A value of
     * -1 is returned if the PircBot has never tried to connect to a server.
     *
     * @return The port number of the last IRC server we connected to. Returns
     * -1 if no connection attempts have ever been made.
     */
    public final int getPort() {
        return _port;
    }

    /**
     * Returns the last password that we used when connecting to an IRC server.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method). A value of null
     * is returned if the PircBot has never tried to connect to a server using a
     * password.
     *
     * @return The last password that we used when connecting to an IRC server.
     * Returns null if we have not previously connected using a
     * password.
     */
    public final String getPassword() {
        return _password;
    }

    /**
     * A convenient method that accepts an IP address represented as a long and
     * returns an integer array of size 4 representing the same IP address.
     *
     * @param address the long value representing the IP address.
     * @return An int[] of size 4.
     */
    public int[] longToIp(long address) {
        int[] ip = new int[4];
        for (int i = 3; i >= 0; i--) {
            ip[i] = (int) (address % 256);
            address = address / 256;
        }
        return ip;
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of
     * size 4 and returns this as a long representation of the same IP address.
     *
     * @param address the byte[] of size 4 representing the IP address.
     * @return a long representation of the IP address.
     */
    public long ipToLong(byte[] address) {
        if (address.length != 4) {
            throw new IllegalArgumentException("byte array must be of length 4");
        }
        long ipNum = 0;
        long multiplier = 1;
        for (int i = 3; i >= 0; i--) {
            int byteVal = (address[i] + 256) % 256;
            ipNum += byteVal * multiplier;
            multiplier *= 256;
        }
        return ipNum;
    }

    /**
     * Sets the encoding charset to be used when sending or receiving lines from
     * the IRC server. If set to null, then the platform's default charset is
     * used. You should only use this method if you are trying to send text to
     * an IRC server in a different charset, e.g. "GB2312" for Chinese encoding.
     * If a PircBot is currently connected to a server, then it must reconnect
     * before this change takes effect.
     *
     * @param charset The new encoding charset to be used by PircBot.
     * @throws UnsupportedEncodingException If the named charset is not supported.
     */
    public void setEncoding(String charset) throws UnsupportedEncodingException {
        // Just try to see if the charset is supported first...
        "".getBytes(charset);

        _charset = charset;
    }

    /**
     * Returns the encoding used to send and receive lines from the IRC server,
     * or null if not set. Use the setEncoding method to change the encoding
     * charset.
     *
     * @return The encoding used to send outgoing messages, or null if not set.
     */
    public String getEncoding() {
        return _charset;
    }

    /**
     * Sets the encoding charset to be used when sending or receiving lines from
     * the IRC server. If set to null, then the platform's default charset is
     * used. You should only use this method if you are trying to send text to
     * an IRC server in a different charset, e.g. "GB2312" for Chinese encoding.
     * If a PircBot is currently connected to a server, then it must reconnect
     * before this change takes effect.
     *
     * @param charset The new encoding charset to be used by PircBot.
     * @throws UnsupportedEncodingException If the named charset is not supported.
     * @since PircBot 1.0.4
     */
    public void setFallbackEncoding(String charset) throws UnsupportedEncodingException {
        // Just try to see if the charset is supported first...
        "".getBytes(charset);

        _fallbackCharset = charset;
    }

    /**
     * Returns the encoding used to send and receive lines from the IRC server,
     * or null if not set. Use the setEncoding method to change the encoding
     * charset.
     *
     * @return The encoding used to send outgoing messages, or null if not set.
     * @since PircBot 1.0.4
     */
    public String getFallbackEncoding() {
        return _fallbackCharset;
    }

    /**
     * Returns the InetAddress used by the PircBot. This can be used to find the
     * I.P. address from which the PircBot is connected to a server.
     *
     * @return The current local InetAddress, or null if never connected.
     */
    public InetAddress getInetAddress() {
        return _inetAddress;
    }

    /**
     * Sets the InetAddress to be used when sending DCC chat or file transfers.
     * This can be very useful when you are running a bot on a machine which is
     * behind a firewall and you need to tell receiving clients to connect to a
     * NAT/router, which then forwards the connection.
     *
     * @param dccInetAddress The new InetAddress, or null to use the default.
     */
    public void setDccInetAddress(InetAddress dccInetAddress) {
        _dccInetAddress = dccInetAddress;
    }

    /**
     * Returns the InetAddress used when sending DCC chat or file transfers. If
     * this is null, the default InetAddress will be used.
     *
     * @return The current DCC InetAddress, or null if left as default.
     */
    public InetAddress getDccInetAddress() {
        return _dccInetAddress;
    }

    /**
     * Returns the set of port numbers to be used when sending a DCC chat or
     * file transfer. This is useful when you are behind a firewall and need to
     * set up port forwarding. The array of port numbers is traversed in
     * sequence until a free port is found to listen on. A DCC tranfer will fail
     * if all ports are already in use. If set to null, <i>any</i> free port
     * number will be used.
     *
     * @return An array of port numbers that PircBot can use to send DCC
     * transfers, or null if any port is allowed.
     */
    public int[] getDccPorts() {
        if (_dccPorts == null || _dccPorts.length == 0) {
            return null;
        }
        // Clone the array to prevent external modification.
        return _dccPorts.clone();
    }

    /**
     * Sets the choice of port numbers that can be used when sending a DCC chat
     * or file transfer. This is useful when you are behind a firewall and need
     * to set up port forwarding. The array of port numbers is traversed in
     * sequence until a free port is found to listen on. A DCC tranfer will fail
     * if all ports are already in use. If set to null, <i>any</i> free port
     * number will be used.
     *
     * @param ports The set of port numbers that PircBot may use for DCC
     * transfers, or null to let it use any free port (default).
     */
    public void setDccPorts(int[] ports) {
        if (ports == null || ports.length == 0) {
            _dccPorts = null;
        } else {
            // Clone the array to prevent external modification.
            _dccPorts = ports.clone();
        }
    }

    /**
     * Returns true if and only if the object being compared is the exact same
     * instance as this PircBot. This may be useful if you are writing a
     * multiple server IRC bot that uses more than one instance of PircBot.
     *
     * @return true if and only if Object o is a PircBot and equal to this.
     */
    public boolean equals(Object o) {
        // This probably has the same effect as Object.equals, but that may
        // change...
        if (o instanceof PircBot) {
            PircBot other = (PircBot) o;
            return other == this;
        }
        return false;
    }

    /**
     * Returns the hashCode of this PircBot. This method can be called by hashed
     * collection classes and is useful for managing multiple instances of
     * PircBots in such collections.
     *
     * @return the hash code for this instance of PircBot.
     */
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a String representation of this object. You may find this useful
     * for debugging purposes, particularly if you are using more than one
     * PircBot instance to achieve multiple server connectivity. The format of
     * this String may change between different versions of PircBot but is
     * currently something of the form <code>
     * Version{PircBot x.y.z Java IRC Bot - www.jibble.org}
     * Connected{true}
     * Server{irc.dal.net}
     * Port{6667}
     * Password{}
     * </code>
     *
     * @return a String representation of this object.
     */
    public String toString() {
        return "Version{" + _version + "}" + " Connected{" + isConnected() + "}" + " Server{" + _server + "}"
                + " Port{" + _port + "}" + " Password{" + _password + "}";
    }

    /**
     * Returns an array of all users in the specified channel.
     * <p>
     * There are some important things to note about this method:-
     * <ul>
     * <li>This method may not return a full list of users if you call it before
     * the complete nick list has arrived from the IRC server.</li>
     * <li>If you wish to find out which users are in a channel as soon as you
     * join it, then you should override the onUserList method instead of
     * calling this method, as the onUserList method is only called as soon as
     * the full user list has been received.</li>
     * <li>This method will return immediately, as it does not require any
     * interaction with the IRC server.</li>
     * <li>The bot must be in a channel to be able to know which users are in
     * it.</li>
     * </ul>
     *
     * @param channel The name of the channel to list.
     * @return An array of User objects. This array is empty if we are not in
     * the channel.
     * @see #onUserList(String, User[]) onUserList
     */
    public final User[] getUsers(String channel) {
        channel = channel.toLowerCase();
        User[] userArray = new User[0];
        synchronized (_channels) {
            Hashtable<User, User> users = _channels.get(channel);
            if (users != null) {
                userArray = new User[users.size()];
                Enumeration<User> enumeration = users.elements();
                for (int i = 0; i < userArray.length; i++) {
                    User user = enumeration.nextElement();
                    userArray[i] = user;
                }
            }
        }
        return userArray;
    }

    /**
     * Returns the User object for the specified nick in the specified channel.
     * <p>
     * There are some important things to note about this method:-
     * <ul>
     * <li>This method may not return the User if you call it before the
     * complete nick list has arrived from the IRC server.</li>
     * <li>This method will return immediately, as it does not require any
     * interaction with the IRC server.</li>
     * <li>The bot must be in a channel to be able to know which users are in
     * it.</li>
     * </ul>
     *
     * @param sourceNick The nick of the user to find.
     * @param channel The name of the channel to look for the user in.
     * @return User object for the user if found, <code>null</code> if not
     * found.
     */
    public final User getUser(String sourceNick, String channel) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            Hashtable<User, User> users = _channels.get(channel);
            if (users != null) {
                Enumeration<User> enumeration = users.elements();
                while (enumeration.hasMoreElements()) {
                    User user = enumeration.nextElement();
                    if (user.getNick().equalsIgnoreCase(sourceNick)) {
                        return user;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a custom object for the specified user.
     * <p>
     * There are some important things to note about this method:-
     * <ul>
     * <li>This method may not return the Object if you call it before the
     * complete nick list has arrived from the IRC server.</li>
     * <li>This method will return immediately, as it does not require any
     * interaction with the IRC server.</li>
     * <li>The bot must be in a channel to be able to know which users are in
     * it.</li>
     * </ul>
     *
     * @param sourceNick The nick of the user to find.
     * @return Object a custom object associated with this user,
     * <code>null</code> if none found.
     */
    public final Object getUserInfo(String sourceNick) {
        synchronized (_channels) {
            Enumeration<Hashtable<User, User>> channelEnum = _channels.elements();
            while (channelEnum.hasMoreElements()) {
                Hashtable<User, User> users = channelEnum.nextElement();
                if (users != null) {
                    Enumeration<User> enumeration = users.elements();
                    while (enumeration.hasMoreElements()) {
                        User userObj = enumeration.nextElement();
                        if (userObj.getNick().equalsIgnoreCase(sourceNick)) {
                            return userObj.getInfo();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Stores a developer created Object with this User.
     *
     * As the Users are stored by channel, this updates each User instance
     * making this info available for the User with the given nick no matter
     * which channel they may be in.
     *
     * @param nick nick of the user to add the info to
     * @param userInfo the custom user info
     */
    public final void setUserInfo(String nick, Object userInfo) {
        synchronized (_channels) {
            Enumeration<Hashtable<User, User>> channelEnum = _channels.elements();
            while (channelEnum.hasMoreElements()) {
                Hashtable<User, User> users = channelEnum.nextElement();
                if (users != null) {
                    Enumeration<User> enumeration = users.elements();
                    while (enumeration.hasMoreElements()) {
                        User userObj = enumeration.nextElement();
                        if (userObj.getNick().equalsIgnoreCase(nick)) {
                            userObj.setInfo(userInfo);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns an array of all channels that we are in. Note that if you call
     * this method immediately after joining a new channel, the new channel may
     * not appear in this array as it is not possible to tell if the join was
     * successful until a response is received from the IRC server.
     *
     * @return A String array containing the names of all channels that we are
     * in.
     */
    public final String[] getChannels() {
        String[] channels;
        synchronized (_channels) {
            channels = new String[_channels.size()];
            Enumeration<String> enumeration = _channels.keys();
            for (int i = 0; i < channels.length; i++) {
                channels[i] = enumeration.nextElement();
            }
        }
        return channels;
    }

    /**
     * A convenience method to check that a user, by nick, is in a channel (any
     * channel) with the bot.
     *
     * Note that if you call this method immediately after joining a new
     * channel, the new channel may not appear in this array as it is not
     * possible to tell if the join was successful until a response is received
     * from the IRC server.
     *
     * @param nick the nick to check for
     * @return true if the bot and user are in any channel together, false if
     * not
     */
    public final boolean isInCommonChannel(String nick) {
        synchronized (_channels) {
            Enumeration<Hashtable<User, User>> channelEnum = _channels.elements();
            while (channelEnum.hasMoreElements()) {
                Hashtable<User, User> users = channelEnum.nextElement();
                if (users != null) {
                    Enumeration<User> enumeration = users.elements();
                    while (enumeration.hasMoreElements()) {
                        User userObj = enumeration.nextElement();
                        if (userObj.getNick().equalsIgnoreCase(nick)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the number of channels that we and the given user are in.
     *
     * Note that if you call this method immediately after joining a new
     * channel, the new channel may not appear in this array as it is not
     * possible to tell if the join was successful until a response is received
     * from the IRC server.
     *
     * @param nick the nick to check for
     * @return the number of common channels that we and the user are in.
     */
    public final int numCommonChannels(String nick) {
        int chanCount = 0;

        synchronized (_channels) {
            Enumeration<Hashtable<User, User>> channelEnum = _channels.elements();
            while (channelEnum.hasMoreElements()) {
                Hashtable<User, User> users = channelEnum.nextElement();
                if (users != null) {
                    Enumeration<User> enumeration = users.elements();
                    while (enumeration.hasMoreElements()) {
                        User userObj = enumeration.nextElement();
                        if (userObj.getNick().equalsIgnoreCase(nick)) {
                            chanCount++;
                        }
                    }
                }
            }
        }

        return chanCount;
    }

    /**
     * Returns an array of all channels that we and the given user are in.
     *
     * Note that if you call this method immediately after joining a new
     * channel, the new channel may not appear in this array as it is not
     * possible to tell if the join was successful until a response is received
     * from the IRC server.
     *
     * @param nick the nick to check for
     * @return A String array containing the names of all common channels that
     * we and the user are in.
     */
    public final String[] getCommonChannels(String nick) {
        String[] channels = new String[_channels.size()];
        int count = 0;
        synchronized (_channels) {
            Enumeration<String> chans = _channels.keys();
            while (chans.hasMoreElements()) {
                String chan = chans.nextElement();

                Hashtable<User, User> users = _channels.get(chan);
                if (users != null) {
                    Enumeration<User> enumeration = users.elements();
                    while (enumeration.hasMoreElements()) {
                        User user = enumeration.nextElement();
                        if (user.getNick().equalsIgnoreCase(nick)) {
                            channels[count] = chan;
                            count++;
                            break;
                        }
                    }
                }
            }
        }

        String[] retChannels = new String[count];
        if (count > 0) {
            System.arraycopy(channels, 0, retChannels, 0, count);
        }
        return retChannels;
    }

    /**
     * Returns the single, highest (most powerful), mode that this user instance
     * has.
     *
     * @param user to get highest mode for
     * @return the highest user mode, or an empty String if none found
     */
    public final String getHighestUserPrefix(User user) {
        if (user == null) {
            return "";
        }
        for (int x = 0; x < _userPrefixOrder.length(); x++) {
            if (user.getPrefix().contains(_userPrefixOrder.charAt(x) + "")) {
                return _userPrefixOrder.charAt(x) + "";
            }
        }

        return "";
    }

    /**
     * Return the user prefixes known to be available on this server. The
     * highest (most powerful) mode being the leftmost.
     *
     * @return the user modes in power order
     */
    public final String getUserPrefixesInOrder() {
        return _userPrefixOrder;
    }

    /* CRAFTIRC */
    public final String getChannelPrefixes() {
        return _channelPrefixes;
    }

    /**
     * Disposes of all thread resources used by this PircBot. This may be useful
     * when writing bots or clients that use multiple servers (and therefore
     * multiple PircBot instances) or when integrating a PircBot with an
     * existing program.
     * <p>
     * Each PircBot runs its own threads for dispatching messages from its
     * outgoing message queue and receiving messages from the server. Calling
     * dispose() ensures that these threads are stopped, thus freeing up system
     * resources and allowing the PircBot object to be garbage collected if
     * there are no other references to it.
     * <p>
     * Once a PircBot object has been disposed, it should not be used again.
     * Attempting to use a PircBot that has been disposed may result in
     * unpredictable behaviour.
     *
     * @since 1.2.2
     */
    public synchronized void dispose() {
        _outputThread.interrupt();
        _inputThread.dispose();
    }

    /**
     * Add a user to the specified channel in our memory. Overwrite the existing
     * entry if it exists.
     */
    private void addUser(String channel, User user) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            Hashtable<User, User> users = _channels.get(channel);
            if (users == null) {
                users = new Hashtable<>();
                _channels.put(channel, users);
            }
            this.log("Adding " + user.toString() + " to " + channel);
            users.put(user, user);
        }
    }

    /**
     * Remove a user from the specified channel in our memory.
     */
    private User removeUser(String channel, String nick) {
        channel = channel.toLowerCase();
        User user = new User("", nick);
        synchronized (_channels) {
            Hashtable<User, User> users = _channels.get(channel);
            if (users != null) {
                return users.remove(user);
            }
        }
        return null;
    }

    /**
     * Remove a user from all channels in our memory.
     */
    private void removeUser(String nick) {
        synchronized (_channels) {
            Enumeration<String> enumeration = _channels.keys();
            while (enumeration.hasMoreElements()) {
                String channel = enumeration.nextElement();
                this.removeUser(channel, nick);
            }
        }
    }

    /**
     * Rename a user if they appear in any of the channels we know about.
     */
    private void renameUser(String oldNick, String newNick) {
        synchronized (_channels) {
            Enumeration<String> enumeration = _channels.keys();
            while (enumeration.hasMoreElements()) {
                String channel = enumeration.nextElement();
                User user = getUser(oldNick, channel);
                if (user == null) {
                    user = new User("", newNick);
                    this.addUser(channel, user);
                }
                // keep the original info object
                Object obj = user.getInfo();
                user = this.removeUser(channel, oldNick);
                if (user != null) {
                    user = new User(user.getPrefix(), newNick);
                    // also put the info object back with this user
                    user.setInfo(obj);
                    this.addUser(channel, user);
                }
            }
        }
    }

    /**
     * Removes an entire channel from our memory of users.
     */
    private void removeChannel(String channel) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            _channels.remove(channel);
        }
    }

    /**
     * Removes all channels from our memory of users.
     */
    private void removeAllChannels() {
        synchronized (_channels) {
            _channels = new Hashtable<>();
        }
    }

    private void updateUser(String channel, char giveTake, String userPrefix, String nick) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            Hashtable<User, User> users = _channels.get(channel);
            if (users != null) {
                Enumeration<User> enumeration = users.elements();
                while (enumeration.hasMoreElements()) {
                    User userObj = enumeration.nextElement();
                    if (userObj.getNick().equalsIgnoreCase(nick)) {
                        if (giveTake == '+') {
                            userObj.addPrefix(userPrefix);
                        } else {
                            if (userObj.hasPrefix(userPrefix)) {
                                userObj.removePrefix(userPrefix);
                            }
                        }
                    }
                }
            } else {
                // just in case ...
                users = new Hashtable<>();
                User newUser = new User("", nick);
                users.put(newUser, newUser);
            }
        }
    }

    /**
     * Sets the supported prefixes
     *
     * @param prefixes The supportedPrefixes to set.
     */
    private void setSupportedPrefixes(String prefixes) {
        this.supportedPrefixes.clear();
        for (int x = 0; x < prefixes.length(); x++) {
            this.supportedPrefixes.add(String.valueOf(prefixes.charAt(x)));
        }
    }

    /**
     * Returns the supportedPrefixes.
     *
     * @return The supportedPrefixes.
     */
    public List<String> getSupportedPrefixes() {
        return supportedPrefixes;
    }

    /**
     * Returns the supportedModes.
     *
     * @return The supportedModes.
     */
    public List<String> getSupportedModes() {
        return supportedModes;
    }

    /**
     * Sets supportedModes to the value given.
     *
     * @param modes The supportedModes to set.
     */
    private void setSupportedModes(String modes) {
        this.supportedModes.clear();
        for (int x = 0; x < modes.length(); x++) {
            this.supportedModes.add(String.valueOf(modes.charAt(x)));
        }
    }

    public String getNetworkName() {
        return this.networkName;
    }

    // Connection stuff.
    private InputThread _inputThread;
    private OutputThread _outputThread;
    private String _charset;
    private String _fallbackCharset;
    private InetAddress _inetAddress;

    // Details about the last server that we connected to.
    private String _server;
    private int _port = -1;
    private String _password;

    // Outgoing message stuff.
    private final Queue _outQueue = new Queue();
    private long _messageDelay = 1000;
    private long _queueSize = 5;

    // A Hashtable of channels that points to a selfreferential Hashtable of
    // User objects (used to remember which users are in which channels).
    private Hashtable<String, Hashtable<User, User>> _channels = new Hashtable<>();

    // A Hashtable to temporarily store channel topics when we join them
    // until we find out who set that topic.
    private final Hashtable<String, String> _topics = new Hashtable<>();

    // DccManager to process and handle all DCC events.
    private final DccManager _dccManager = new DccManager(this);
    private int[] _dccPorts;
    private InetAddress _dccInetAddress;

    // Default settings for the PircBot.
    private boolean _autoNickChange = false;
    private boolean _verbose = false;
    private String _name = "PircBot";
    private String _nick = _name;
    private String _login = "PircBot";
    private String _version = "PircBot " + VERSION + " Java IRC Bot - www.jibble.org";
    private String _finger = "You ought to be arrested for fingering a bot!";

    // A Hashtable to store the available prefixes and associated mode operator
    private final Map<String, String> _userPrefixes = new HashMap<>();
    // prefixes as delivered from the server .. highest to lowest - default to
    // op/voice
    private String _userPrefixOrder = "~&@%+";
    private final String _channelPrefixes = "#&+!";
    private final List<String> supportedPrefixes = new ArrayList<>(Arrays.asList("~", "&", "@", "%", "+"));
    private final List<String> supportedModes = new ArrayList<>(Arrays.asList("q", "a", "o", "h", "v"));

    private String networkName;
}
