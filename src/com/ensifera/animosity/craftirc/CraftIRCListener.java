package com.ensifera.animosity.craftirc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.SimplePluginManager;

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
            	SimplePluginManager pluginManager = (SimplePluginManager)this.plugin.getServer().getPluginManager();
            	java.lang.reflect.Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
            	commandMapField.setAccessible(true);
            	CommandMap map = (CommandMap)commandMapField.get(pluginManager);
            	Command meCommand = map.getCommand("me");
            	if (!(meCommand instanceof org.bukkit.command.defaults.MeCommand)) return;
            	if (!meCommand.testPermission(event.getPlayer())) return; 
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
            if (split[0].equalsIgnoreCase("/say")) {
            	SimplePluginManager pluginManager = (SimplePluginManager)this.plugin.getServer().getPluginManager();
            	java.lang.reflect.Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
            	commandMapField.setAccessible(true);
            	CommandMap map = (CommandMap)commandMapField.get(pluginManager);
            	Command sayCommand = map.getCommand("say");
            	if (!(sayCommand instanceof org.bukkit.command.defaults.SayCommand)) return;
            	if (!(sayCommand.testPermission(event.getPlayer()))) return;
            	final String message = event.getMessage().substring(5);
                final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cMinecraftTag()), null, "say");
                msg.setField("message", message);
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
    	
    	if (this.plugin.getServer().getPluginManager().isPluginEnabled("VanishNoPacket") && event.getPlayer().hasPermission("vanish.joinwithoutannounce")) {
    		return;
    	}
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
    	if (this.plugin.getServer().getPluginManager().isPluginEnabled("VanishNoPacket") && event.getPlayer().hasPermission("vanish.silentquit")) {
			return;
    	}
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
