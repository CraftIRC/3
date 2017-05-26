package com.ensifera.animosity.craftirc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

final class AdvancementsListener implements Listener {
    private final CraftIRC plugin;

    AdvancementsListener(CraftIRC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.ADVANCEMENTS)) {
            return;
        }
        if (event.getAdvancement() == null) {
            return;
        }
        final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "achievement");
        if (msg == null) {
            return;
        }
        if (event.getAdvancement().getKey().getKey().startsWith("recipes/")) {
            return;
        }
        msg.setField("sender", event.getPlayer().getDisplayName());
        msg.setField("message", event.getAdvancement().getKey().getKey());
        msg.setField("world", event.getPlayer().getWorld().getName());
        msg.setField("realSender", event.getPlayer().getName());
        msg.setField("prefix", this.plugin.getPrefix(event.getPlayer()));
        msg.setField("suffix", this.plugin.getSuffix(event.getPlayer()));
        msg.doNotColor("message");
        msg.post();
    }
}
