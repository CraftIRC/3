package com.ensifera.animosity.craftirc;

import org.bukkit.Server;

import java.util.List;

public final class ConsolePoint implements CommandEndPoint {
    private final Server server;
    private final CraftIRC plugin;

    ConsolePoint(CraftIRC plugin, Server server) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public Type getType() {
        return EndPoint.Type.MINECRAFT;
    }

    @Override
    public Security getSecurity() {
        return SecuredEndPoint.Security.REQUIRE_PATH;
    }

    @Override
    public void messageIn(RelayedMessage msg) {
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        this.plugin.log("(To " + username + ")" + msg.getMessage(this));
        return true;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        this.plugin.log("(To the admins)" + msg.getMessage(this));
        return true;
    }

    @Override
    public List<String> listUsers() {
        return null;
    }

    @Override
    public List<String> listDisplayUsers() {
        return null;
    }

    @Override
    public void commandIn(RelayedCommand cmd) {
        final String command = cmd.getField("command").toLowerCase();
        if (!this.plugin.cCmdWordCmd(null).contains(command)) {
            return;
        }
        final String args = cmd.getField("args");
        String ccmd;
        try {
            ccmd = args.substring(0, args.indexOf(" "));
        } catch (final StringIndexOutOfBoundsException e) {
            ccmd = args;
        }
        if (ccmd.equals("")) {
            return;
        }
        if (!this.plugin.cPathAttribute(cmd.getField("source"), cmd.getField("target"), "attributes.admin")) {
            this.plugin.logWarn("Command could not be run. Admin attribute not enabled for this path. (" + cmd.getMessage() + ")");
            return;
        }
        if (!cmd.getFlag("admin")) {
            this.plugin.logWarn("Command could not be run. The user has no admin flag. (" + cmd.getMessage() + ")");
            return;
        }
        if (!(this.plugin.cConsoleCommands().contains(ccmd) || this.plugin.cConsoleCommands().contains("*") || this.plugin.cConsoleCommands().contains("all"))) {
            this.plugin.logWarn("Command could not be run. Command name not whitelisted in console-commands. (" + cmd.getMessage() + ")");
            return;
        }
        final IRCCommandSender sender = new IRCCommandSender(this.server, cmd, this, this.server.getConsoleSender());
        if (this.plugin.cLog()) {
            String tag = this.plugin.getTag(cmd.getSource());
            String user = cmd.getField("realSender") + "!" + cmd.getField("username") + "@" + cmd.getField("hostname");
            this.plugin.log("[" + tag + "][" + user + "] " + args);
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                ConsolePoint.this.server.dispatchCommand(sender, args);
            }
        });
    }
}
