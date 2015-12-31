package com.ensifera.animosity.craftirc;

public final class RelayedCommand extends RelayedMessage {
    RelayedCommand(CraftIRC plugin, EndPoint source, CommandEndPoint target) {
        super(plugin, source, target, "command");
    }

    public void act() {
        this.post(DeliveryMethod.COMMAND, null);
    }
}
