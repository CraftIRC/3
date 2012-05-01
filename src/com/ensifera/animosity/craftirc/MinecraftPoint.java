package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

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
        p.sendMessage(msg.getMessage(this));
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
    	boolean isVanishEnabled = this.server.getPluginManager().isPluginEnabled("VanishNoPacket");
        final LinkedList<String> users = new LinkedList<String>();
        for (final Player p : this.server.getOnlinePlayers()) {
        	if (isVanishEnabled) {
        		try {
        			if (!VanishNoPacket.isVanished(p.getName())) {
        				users.add(p.getName());
        			}
        		} catch (VanishNotLoadedException e) {}
        	} else
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
            String result = "Nobody is minecrafting right now.";
            if (playerCount > 0) {
                final List<String> userlist = this.listDisplayUsers();
                String userstring = (userlist.size() > 0 ? userlist.get(0) : "");
                for (int i = 1; i < userlist.size(); i++) {
                    userstring = userstring + ", " + userlist.get(i);
                }
                result = "Online (" + playerCount + "/" + this.server.getMaxPlayers() + "): " + userstring;
            }
            //Reply to remote endpoint! 
            final RelayedMessage response = this.plugin.newMsgToTag(this, cmd.getField("source"), "");
            response.setField("message", result);
            response.post();
        }
    }

}
