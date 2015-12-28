package com.ensifera.animosity.craftirc;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public final class IRCCommandSender implements ConsoleCommandSender {
    private final RelayedCommand cmd;
    private final EndPoint console;
    private final ConsoleCommandSender sender;

    IRCCommandSender(Server server, RelayedCommand cmd, EndPoint console, ConsoleCommandSender sender) {
        this.cmd = cmd;
        this.console = console;
        this.sender = sender;
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        this.sender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        this.sender.abandonConversation(conversation, details);
    }

    @Override
    public void acceptConversationInput(String input) {
        this.sender.acceptConversationInput(input);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return this.sender.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return this.sender.addAttachment(plugin, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return this.sender.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return this.sender.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return this.sender.beginConversation(conversation);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return this.sender.getEffectivePermissions();
    }

    public String getField(String name) {
        return this.cmd.getField(name);
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
    public boolean hasPermission(Permission perm) {
        return this.sender.hasPermission(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        return this.sender.hasPermission(name);
    }

    @Override
    public boolean isConversing() {
        return this.sender.isConversing();
    }

    @Override
    public boolean isOp() {
        return this.sender.isOp();
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return this.sender.isPermissionSet(perm);
    }

    @Override
    public boolean isPermissionSet(String name) {
        return this.sender.isPermissionSet(name);
    }

    @Override
    public void recalculatePermissions() {
        this.sender.recalculatePermissions();
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        this.sender.removeAttachment(attachment);
    }

    @Override
    public void sendMessage(String message) {
        sendMessage(message.split("\n"));
    }

    @Override
    public void sendMessage(String[] messages) {
        final boolean isPrivate = this.cmd.getPlugin().cCmdPrivate("cmd");
        for (final String message : messages) {
            final RelayedMessage msg = this.cmd.getPlugin().newMsgToTag(this.console, this.cmd.getField("source"), "command-reply");
            msg.setField("message", message);
            msg.setField("realSender", this.cmd.getField("realSender"));
            msg.setFlag("private", isPrivate);
            msg.post();
        }
    }

    @Override
    public void sendRawMessage(String message) {
        final RelayedMessage msg = this.cmd.getPlugin().newMsgToTag(this.console, this.cmd.getField("source"), "command-reply");
        msg.setField("message", message);
        msg.setField("realSender", this.cmd.getField("realSender"));
        msg.setFlag("private", this.cmd.getPlugin().cCmdPrivate("cmd"));
        msg.post();
    }

    @Override
    public void setOp(boolean value) {
        this.sender.setOp(value);
    }
}
