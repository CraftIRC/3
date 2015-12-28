package com.ensifera.animosity.craftirc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

final class ConsoleListener implements Listener {
    private final CraftIRC plugin;

    ConsoleListener(CraftIRC craftIRC) {
        this.plugin = craftIRC;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        final String[] split = event.getCommand().split(" ");
        String message = null;
        String eventType = null;
        if (split[0].equalsIgnoreCase("say") && event.getCommand().length() > "say ".length()) {
            message = event.getCommand().substring("say ".length());
            eventType = "say";
        }
        if (split[0].equalsIgnoreCase("me") && event.getCommand().length() > "me ".length()) {
            message = event.getCommand().substring("me ".length());
            eventType = "action";
        }
        if (message != null) {
            final RelayedMessage msg = this.plugin.newMsg(this.plugin.getEndPoint(this.plugin.cConsoleTag()), null, eventType);
            if (msg == null) {
                return;
            }
            msg.setField("sender", event.getSender().getName());
            msg.setField("realSender", event.getSender().getName());
            msg.setField("prefix", "");
            msg.setField("suffix", "");
            msg.setField("message", message);
            msg.doNotColor("message");
            msg.post();
        }
    }
}
