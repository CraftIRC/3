package com.ensifera.animosity.craftirc;

import java.util.HashMap;
import java.util.Map;

public final class RelayedCommand extends RelayedMessage {

    RelayedCommand(CraftIRC plugin, EndPoint source, CommandEndPoint target) {
        super(plugin, source, target, "command");
    }

    public void act() {
        this.post(DeliveryMethod.COMMAND, null);
    }
}
