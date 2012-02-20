package com.ensifera.animosity.craftirc.example;

import java.util.List;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.EndPoint;
import com.ensifera.animosity.craftirc.RelayedMessage;

public class PluginExample extends JavaPlugin implements EndPoint {

    protected CraftIRC craftircHandle = null;

    @Override
    public void onEnable() {
        final Plugin checkplugin = this.getServer().getPluginManager().getPlugin("CraftIRC");
        if ((checkplugin == null) || !checkplugin.isEnabled()) {
            this.getServer().getPluginManager().disablePlugin((this));
        } else {
            try {
                this.craftircHandle = (CraftIRC) checkplugin;
                this.craftircHandle.registerEndPoint("mytag", this);
                final RelayedMessage rm = this.craftircHandle.newMsg(this, null, "generic");
                rm.setField("message", "I'm aliiive!");
                rm.post();
            } catch (final ClassCastException ex) {
                ex.printStackTrace();
                this.getServer().getPluginManager().disablePlugin((this));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.craftircHandle != null) {
            this.craftircHandle.unregisterEndPoint("mytag");
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