package com.ensifera.animosity.craftirc;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class IRCCommandSender implements ConsoleCommandSender {

    private final RelayedCommand cmd;
    private final EndPoint console;
    private ConsoleCommandSender sender;

    IRCCommandSender(Server server, RelayedCommand cmd, EndPoint console, ConsoleCommandSender sender) {
        this.cmd = cmd;
        this.console = console;
    }

    public String getField(String name) {
        return this.cmd.getField(name);
    }

    @Override
    public void sendMessage(String message) {
        try {
            final RelayedMessage msg = this.cmd.getPlugin().newMsgToTag(this.console, this.cmd.getField("source"), "generic");
            msg.post();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return this.sender.getName();
    }

    @Override
    public Server getServer() {
        return this.sender.getServer();
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0) {
        return this.sender.addAttachment(arg0);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, int arg1) {
        return this.sender.addAttachment(arg0, arg1);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2) {
        return this.sender.addAttachment(arg0, arg1, arg2);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2, int arg3) {
        return this.sender.addAttachment(arg0, arg1, arg2, arg3);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return this.sender.getEffectivePermissions();
    }

    @Override
    public boolean hasPermission(String arg0) {
        return this.sender.hasPermission(arg0);
    }

    @Override
    public boolean hasPermission(Permission arg0) {
        return this.sender.hasPermission(arg0);
    }

    @Override
    public boolean isPermissionSet(String arg0) {
        return this.sender.isPermissionSet(arg0);
    }

    @Override
    public boolean isPermissionSet(Permission arg0) {
        return this.sender.isPermissionSet(arg0);
    }

    @Override
    public void recalculatePermissions() {
        this.sender.recalculatePermissions();
    }

    @Override
    public void removeAttachment(PermissionAttachment arg0) {
        this.sender.removeAttachment(arg0);
    }

    @Override
    public void setOp(boolean arg0) {
        this.sender.setOp(arg0);
    }

    @Override
    public boolean isOp() {
        return this.sender.isOp();
    }
}
