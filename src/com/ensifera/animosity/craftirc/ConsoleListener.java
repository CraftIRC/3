package com.ensifera.animosity.craftirc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class ConsoleListener implements Listener {
    CraftIRC plugin;

    public ConsoleListener(CraftIRC craftIRC) {
        this.plugin = craftIRC;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().toLowerCase().startsWith("say")) {
            final String message = event.getCommand().substring(4);
            final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cConsoleTag()), null, "console");
            msg.setField("message", message);
            msg.doNotColor("message");
            msg.post();
        }
    }
}
