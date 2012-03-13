package com.ensifera.animosity.craftirc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class CraftIRCListener implements Listener {

    private CraftIRC plugin = null;

    public CraftIRCListener(CraftIRC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        try {
            final String[] split = event.getMessage().split(" ");
            // ACTION/EMOTE can't be claimed, so use onPlayerCommandPreprocess
            if (split[0].equalsIgnoreCase("/me")) {
                final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "action");
                if (msg == null) {
                    return;
                }
                msg.setField("sender", event.getPlayer().getDisplayName());
                msg.setField("message", Util.combineSplit(1, split, " "));
                msg.setField("world", event.getPlayer().getWorld().getName());
                msg.setField("realSender", event.getPlayer().getName());
                msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
                msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
                msg.doNotColor("message");
                msg.post();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(PlayerChatEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) {
            return;
        }
        try {
            if (this.plugin.cCancelChat()) {
                event.setCancelled(true);
            }
            RelayedMessage msg;
            if (event.isCancelled()) {
                msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cCancelledTag()), null, "chat");
            } else {
                msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "chat");
            }
            if (msg == null) {
                return;
            }
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("message", event.getMessage());
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
            msg.doNotColor("message");
            msg.post();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.JOINS)) {
            return;
        }
        try {
            final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "join");
            if (msg == null) {
                return;
            }
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
            msg.post();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.QUITS)) {
            return;
        }
        try {
            final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "quit");
            if (msg == null) {
                return;
            }
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
            msg.post();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
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
        try {
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
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
