package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public class MinecraftPoint implements CommandEndPoint {

    Server server;
    CraftIRC plugin;

    MinecraftPoint(CraftIRC plugin, Server server) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public Type getType() {
        return EndPoint.Type.MINECRAFT;
    }

    @Override
    public Security getSecurity() {
        return SecuredEndPoint.Security.UNSECURED;
    }

    @Override
    public void messageIn(RelayedMessage msg) {
        final String message = msg.getMessage(this);
        final String source = msg.getField("source");
        this.plugin.getLogger().info("[" + source + "] " + message);
        for (final Player p : this.server.getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        final Player p = this.server.getPlayer(username);
        if (p == null) {
            return false;
        }
        final String message = msg.getMessage(this);
        p.sendMessage(message);
        final String source = msg.getField("source");
        this.plugin.getLogger().info("[" + source + "->" + username + "] " + message);
        return true;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        final String message = msg.getMessage(this);
        boolean success = false;
        for (final Player p : this.server.getOnlinePlayers()) {
            if (p.isOp()) {
                p.sendMessage(message);
                success = true;
            }
        }
        return success;
    }

    @Override
    public List<String> listUsers() {
        final LinkedList<String> users = new LinkedList<String>();
        for (final Player p : this.server.getOnlinePlayers()) {
            users.add(p.getName());
        }
        return users;
    }

    @Override
    public List<String> listDisplayUsers() {
        final boolean isVanishEnabled = this.server.getPluginManager().isPluginEnabled("VanishNoPacket");
        final LinkedList<String> users = new LinkedList<String>();
        playerLoop: for (final Player p : this.server.getOnlinePlayers()) {
            if (isVanishEnabled) {
                for (final MetadataValue value : p.getMetadata("vanished")) {
                    if (value.getOwningPlugin().getName().equals("VanishNoPacket") && value.asBoolean()) {
                        continue playerLoop;
                    }
                }
            }
            users.add(p.getName());
        }
        Collections.sort(users);
        return users;
    }

    @Override
    public void commandIn(RelayedCommand cmd) {
        final String command = cmd.getField("command").toLowerCase();
        if (this.plugin.cCmdWordSay(null).contains(command)) {
            final RelayedMessage fwd = this.plugin.newMsg(cmd.getSource(), this, "chat");
            fwd.copyFields(cmd);
            fwd.setField("message", cmd.getField("args"));
            fwd.doNotColor("message");
            this.messageIn(fwd);
        } else if (this.plugin.cCmdWordPlayers(null).contains(command)) {
            final List<String> users = this.listDisplayUsers();
            final int playerCount = users.size();
            String result;
            if (playerCount > 0) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Online (").append(playerCount).append("/").append(this.server.getMaxPlayers()).append("): ");
                for (int i = 0; i < users.size(); i++) {
                    builder.append(users.get(i)).append(" ");
                }
                builder.setLength(builder.length() - 1);
                result = builder.toString();
            } else {
                result = "Nobody is minecrafting right now.";
            }
            final int maxlen = 400; // Arbitrary!
            String[] responses;
            if (result.length() > maxlen) {
                final StringBuilder builder = new StringBuilder(result.length());
                final StringTokenizer tokenizer = new StringTokenizer(result, " ");
                int currentLine = 0;
                while (tokenizer.hasMoreTokens()) {
                    final String nextWord = tokenizer.nextToken();
                    if ((currentLine + nextWord.length()) > maxlen) {
                        builder.append("\n");
                        currentLine = 0;
                    } else {
                        builder.append(' ');
                        currentLine++;
                    }
                    builder.append(nextWord);
                    currentLine += (nextWord.length());
                }
                responses = builder.toString().split("\n");
            } else {
                responses = new String[] { result };
            }
            //Reply to remote endpoint! 
            for (final String message : responses) {
                final RelayedMessage response = this.plugin.newMsgToTag(this, cmd.getField("source"), "");
                response.setField("message", message);
                response.post();
            }
        }
    }

}
