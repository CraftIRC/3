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
        String message = null;
        String eventType = null;
        if (event.getCommand().toLowerCase().startsWith("say") && event.getCommand().length() > 4) {
            message = event.getCommand().substring(4);
            eventType = "say";
        }
        if (event.getCommand().toLowerCase().startsWith("me") && event.getCommand().length() > 3) {
            message = event.getCommand().substring(3);
            eventType = "action";
        }
        if (message != null) {
            final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cConsoleTag()), null, eventType);
            if (msg == null) {
                return;
            }
            msg.setField("sender", event.getSender().getName());
            msg.setField("message", message);
            msg.doNotColor("message");
            msg.post();
        }
    }
}
