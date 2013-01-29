package com.ensifera.animosity.craftirc;

import java.util.List;

import org.bukkit.Server;

public class ConsolePoint implements CommandEndPoint {

    private Server server;
    private CraftIRC plugin;

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
        CraftIRC.dolog(msg.getMessage(this));
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        CraftIRC.dolog("(To " + username + ")" + msg.getMessage(this));
        return true;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        CraftIRC.dolog("(To the admins)" + msg.getMessage(this));
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
        if (this.plugin.cPathAttribute(cmd.getField("source"), cmd.getField("target"), "attributes.admin") && cmd.getFlag("admin")) {
            //Admin commands
            if (this.plugin.cCmdWordCmd(null).contains(command)) {
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
                if (this.plugin.cConsoleCommands().contains(ccmd) || this.plugin.cConsoleCommands().contains("*") || this.plugin.cConsoleCommands().contains("all")) {
                    final IRCCommandSender sender = new IRCCommandSender(this.server, cmd, this, this.server.getConsoleSender());
                    this.plugin.getServer().getScheduler().runTask(this.plugin, new Runnable() {
                        @Override
                        public void run() {
                            ConsolePoint.this.server.dispatchCommand(sender, args);
                        }
                    });
                }
            }
        }
    }

}
