package com.ensifera.animosity.craftirc;

import java.util.HashMap;
import java.util.Map;

public class RelayedCommand extends RelayedMessage {

    static String typeString = "CMD";

    private final Map<String, Boolean> flags; //Command flags

    RelayedCommand(CraftIRC plugin, EndPoint source, CommandEndPoint target) {
        super(plugin, source, target, "command");
        this.flags = new HashMap<String, Boolean>();
    }

    public void setFlag(String key, boolean value) {
        this.flags.put(key, value);
    }

    public boolean getFlag(String key) {
        return this.flags.get(key);
    }

    public void act() {
        this.post(DeliveryMethod.COMMAND, null);
    }

}
