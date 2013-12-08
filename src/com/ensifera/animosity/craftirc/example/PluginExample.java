package com.ensifera.animosity.craftirc.example;

import java.util.List;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.EndPoint;
import com.ensifera.animosity.craftirc.RelayedMessage;

public class PluginExample extends JavaPlugin implements EndPoint {

    private final String exampletag = "exampletag";
    
    @Override
    public void onEnable() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("CraftIRC");
        if ((plugin == null) || !plugin.isEnabled() || !(plugin instanceof CraftIRC)) {
            this.getServer().getPluginManager().disablePlugin((this));
        } else {
            final CraftIRC craftirc = (CraftIRC) plugin;
            craftirc.registerEndPoint(this.exampletag, this);
            final RelayedMessage rm = craftirc.newMsg(this, null, "generic");
            rm.setField("message", "I'm aliiive!");
            rm.post();
        }
    }

    @Override
    public void onDisable() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("CraftIRC");
        if (((plugin != null) && !plugin.isEnabled()) || (plugin instanceof CraftIRC)) {
            ((CraftIRC) plugin).unregisterEndPoint(this.exampletag);
        }
    }

    @Override
    public Type getType() {
        return EndPoint.Type.MINECRAFT;
    }

    @Override
    public void messageIn(RelayedMessage msg) {
        if (msg.getEvent() == "join") {
            this.getServer().broadcastMessage(msg.getField("sender") + " joined da game!");
        }
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        return false;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        return false;
    }

    @Override
    public List<String> listUsers() {
        return null;
    }

    @Override
    public List<String> listDisplayUsers() {
        return null;
    }

}