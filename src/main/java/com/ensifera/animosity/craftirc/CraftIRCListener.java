package com.ensifera.animosity.craftirc;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class CraftIRCListener implements Listener {
    private final CraftIRC plugin;

    CraftIRCListener(CraftIRC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        final String[] split = event.getMessage().split(" ");
        String message = null;
        String eventType = null;
        // ACTION/EMOTE can't be claimed, so use onPlayerCommandPreprocess
        if (split[0].equalsIgnoreCase("/me") && event.getMessage().length() > 4) {
            eventType = "action";
            message = Util.combineSplit(1, split, " ");
        }
        if (split[0].equalsIgnoreCase("/say") && event.getMessage().length() > 5) {
            eventType = "say";
            if (!(event.getPlayer().hasPermission("bukkit.command.say") && event.getPlayer().hasPermission(Server.BROADCAST_CHANNEL_USERS))) {
                return;
            }
            message = event.getMessage().substring(5);
        }
        if (message != null) {
            final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, eventType);
            if (msg == null) {
                return;
            }
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("message", message);
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
            msg.doNotColor("message");
            msg.post();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) {
            return;
        }
        if (this.plugin.cCancelChat()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChatMonitor(AsyncPlayerChatEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) {
            return;
        }
        final boolean cancelled = event.isCancelled();
        final Player player = event.getPlayer();
        final String message = event.getMessage();
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                RelayedMessage msg;
                if (cancelled) {
                    msg = plugin.newMsg(plugin.getEndPoint(plugin.cCancelledTag()), null, "chat");
                } else {
                    msg = plugin.newMsg(plugin.getEndPoint(plugin.cMinecraftTag()), null, "chat");
                }
                if (msg == null) {
                    return;
                }
                msg.setField("sender", player.getDisplayName());
                msg.setField("message", message);
                msg.setField("world", player.getWorld().getName());
                msg.setField("realSender", player.getName());
                msg.setField("prefix", plugin.getPrefix(player));
                msg.setField("suffix", plugin.getSuffix(player));
                msg.doNotColor("message");
                msg.post();
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.JOINS)) {
            return;
        }
        final boolean cancelled = (event.getJoinMessage() == null);
        final String tag = (cancelled) ? this.plugin.cCancelledTag() : this.plugin.cMinecraftTag();
        final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(tag), null, "join");
        if (msg == null) {
            return;
        }
        msg.setField("sender", event.getPlayer().getDisplayName());
        msg.setField("world", event.getPlayer().getWorld().getName());
        msg.setField("realSender", event.getPlayer().getName());
        msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
        msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
        msg.post();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.QUITS)) {
            return;
        }
        final boolean cancelled = (event.getQuitMessage() == null);
        final String tag = (cancelled) ? this.plugin.cCancelledTag() : this.plugin.cMinecraftTag();
        final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(tag), null, "quit");
        if (msg == null) {
            return;
        }
        msg.setField("sender", event.getPlayer().getDisplayName());
        msg.setField("world", event.getPlayer().getWorld().getName());
        msg.setField("realSender", event.getPlayer().getName());
        msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
        msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
        msg.post();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.KICKS)) {
            return;
        }
        final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "kick");
        if (msg == null) {
            return;
        }
        msg.setField("sender", event.getPlayer().getDisplayName());
        msg.setField("message", (event.getReason().length() == 0) ? "no reason given" : event.getReason());
        msg.setField("realSender", event.getPlayer().getName());
        msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
        msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
        msg.doNotColor("message");
        msg.post();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.DEATHS)) {
            return;
        }
        if (event.getDeathMessage() == null) {
            return;
        }
        final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "death");
        if (msg == null) {
            return;
        }
        msg.setField("sender", event.getEntity().getDisplayName());
        msg.setField("message", event.getDeathMessage());
        msg.setField("world", event.getEntity().getWorld().getName());
        msg.setField("realSender", event.getEntity().getName());
        msg.setField("prefix", this.plugin.getPrefix(event.getEntity()));
        msg.setField("suffix", this.plugin.getSuffix(event.getEntity()));
        msg.post();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAchievement(PlayerAchievementAwardedEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.ACHIEVEMENTS)) {
            return;
        }
        if (event.getAchievement() == null) {
            return;
        }
        final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "achievement");
        if (msg == null) {
            return;
        }
        msg.setField("sender", event.getPlayer().getDisplayName());
        msg.setField("message", event.getAchievement().name());
        msg.setField("world", event.getPlayer().getWorld().getName());
        msg.setField("realSender", event.getPlayer().getName());
        msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
        msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
        msg.doNotColor("message");
        msg.post();
    }
}
