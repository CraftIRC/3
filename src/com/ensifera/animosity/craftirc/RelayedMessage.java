package com.ensifera.animosity.craftirc;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelayedMessage {

    enum DeliveryMethod {
        STANDARD, ADMINS, COMMAND
    }

    static String typeString = "MSG";

    private final CraftIRC plugin;
    private final EndPoint source; //Origin endpoint of the message
    private final EndPoint target; //Target endpoint of the message
    private final String eventType; //Event type
    private LinkedList<EndPoint> cc; //Multiple extra targets for the message
    private String template; //Formatting string
    private final Map<String, String> fields; //All message attributes
    private final Set<String> doNotColorFields; //Do not allow color codes on these fields

    RelayedMessage(CraftIRC plugin, EndPoint source) {
        this(plugin, source, null, "");
    }

    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target) {
        this(plugin, source, target, "");
    }

    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target, String eventType) {
        this.plugin = plugin;
        this.source = source;
        this.target = target;
        if (eventType.equals("")) {
            eventType = "generic";
        }
        this.eventType = eventType;
        this.template = "%message%";
        if ((eventType != null) && (eventType != "") && (target != null)) {
            this.template = plugin.cFormatting(eventType, this);
        }
        this.fields = new HashMap<String, String>();
        this.doNotColorFields = new HashSet<String>();
    }

    public CraftIRC getPlugin() {
        return this.plugin;
    }

    EndPoint getSource() {
        return this.source;
    }

    EndPoint getTarget() {
        return this.target;
    }

    public String getEvent() {
        return this.eventType;
    }

    public RelayedMessage setField(String key, String value) {
        this.fields.put(key, value);
        return this;
    }

    public String getField(String key) {
        return this.fields.get(key);
    }

    public Set<String> setFields() {
        return this.fields.keySet();
    }

    public void copyFields(RelayedMessage msg) {
        if (msg == null) {
            return;
        }
        for (final String key : msg.setFields()) {
            this.setField(key, msg.getField(key));
        }
    }

    public void doNotColor(String key) {
        this.doNotColorFields.add(key);
    }

    public boolean addExtraTarget(EndPoint ep) {
        if (this.cc.contains(ep)) {
            return false;
        }
        this.cc.add(ep);
        return true;
    }

    public String getMessage() {
        return this.getMessage(null);
    }

    public String getMessage(EndPoint currentTarget) {
        String result = this.template;
        EndPoint realTarget;

        //Resolve target
        realTarget = this.target;
        if (realTarget == null) {
            if (currentTarget == null) {
                return fields.get("message");
            }
            realTarget = currentTarget;
            result = this.plugin.cFormatting(this.eventType, this, realTarget);
            if (result == null) {
                result = this.template;
            }
        }

        //IRC color code aliases (actually not recommended)
        if (realTarget.getType() == EndPoint.Type.IRC) {
            result = result.replaceAll("%k([0-9]{1,2})%", Character.toString((char) 3) + "$1");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", Character.toString((char) 3) + "$1,$2");
            result = result.replace("%k%", Character.toString((char) 3));
            result = result.replace("%o%", Character.toString((char) 15));
            result = result.replace("%b%", Character.toString((char) 2));
            result = result.replace("%u%", Character.toString((char) 31));
            result = result.replace("%r%", Character.toString((char) 22));
        } else if (realTarget.getType() == EndPoint.Type.MINECRAFT) {
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", this.plugin.cColorGameFromName("foreground"));
            result = result.replace("%o%", this.plugin.cColorGameFromName("foreground"));
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", "");
        } else {
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", "");
            result = result.replace("%o%", "");
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", "");
        }

        //Fields and named colors (all the important stuff is here actually)
        final Pattern other_vars = Pattern.compile("%([A-Za-z0-9]+)%");
        Matcher find_vars = other_vars.matcher(result);
        while (find_vars.find()) {
            if (this.fields.get(find_vars.group(1)) != null) {
                String replacement = Matcher.quoteReplacement(this.fields.get(find_vars.group(1)));
                if (!this.doNotColorFields.contains(find_vars.group(1))) {
                    replacement = replacement.replaceAll("&([0-9A-Fa-f])", "\u00A7$1");
                }
                result = find_vars.replaceFirst(replacement);
            } else if (realTarget.getType() == EndPoint.Type.IRC) {
                result = find_vars.replaceFirst(Character.toString((char) 3) + String.format("%02d", this.plugin.cColorIrcFromName(find_vars.group(1))));
            } else if (realTarget.getType() == EndPoint.Type.MINECRAFT) {
                result = find_vars.replaceFirst(this.plugin.cColorGameFromName(find_vars.group(1)));
            } else {
                result = find_vars.replaceFirst("");
            }
            find_vars = other_vars.matcher(result);
        }

        //Convert colors
        final boolean colors = this.plugin.cPathAttribute(this.fields.get("source"), this.fields.get("target"), "attributes.colors");
        if (this.source.getType() == EndPoint.Type.MINECRAFT) {
            if ((realTarget.getType() == EndPoint.Type.IRC) && colors) {
                final Pattern color_codes = Pattern.compile("\u00A7([A-Fa-f0-9])?");
                Matcher find_colors = color_codes.matcher(result);
                while (find_colors.find()) {
                    result = find_colors.replaceFirst("\u0003" + Integer.toString(this.plugin.cColorIrcFromGame("\u00A7" + find_colors.group(1))));
                    find_colors = color_codes.matcher(result);
                }
            } else if ((realTarget.getType() != EndPoint.Type.MINECRAFT) || !colors) {
                //Strip colors
                result = result.replaceAll("(\u00A7([A-Fa-f0-9])?)", "");
            }
        }
        if (this.source.getType() == EndPoint.Type.IRC) {
            if ((realTarget.getType() == EndPoint.Type.MINECRAFT) && colors) {
                result = result.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 22) + "|" + Character.toString((char) 31) + ")", "");
                final Pattern color_codes = Pattern.compile(Character.toString((char) 3) + "([01]?[0-9])(,[0-9]{0,2})?");
                Matcher find_colors = color_codes.matcher(result);
                while (find_colors.find()) {
                    result = find_colors.replaceFirst(this.plugin.cColorGameFromIrc(Integer.parseInt(find_colors.group(1))));
                    find_colors = color_codes.matcher(result);
                }
                result = result.replaceAll(Character.toString((char) 15) + "|" + Character.toString((char) 3), this.plugin.cColorGameFromName("foreground"));
            } else if ((realTarget.getType() != EndPoint.Type.IRC) || !colors) {
                //Strip colors
                result = result.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 15) + "|" + Character.toString((char) 22) + Character.toString((char) 31) + "|" + Character.toString((char) 3) + "[0-9]{0,2}(,[0-9]{0,2})?)", "");
            }
        }

        return result;
    }

    public boolean post() {
        return this.post(DeliveryMethod.STANDARD, null);
    }

    public boolean post(boolean admin) {
        return this.post(admin ? DeliveryMethod.ADMINS : DeliveryMethod.STANDARD, null);
    }

    boolean post(DeliveryMethod dm, String username) {
        List<EndPoint> destinations;
        if (this.cc != null) {
            destinations = new LinkedList<EndPoint>(this.cc);
        } else {
            destinations = new LinkedList<EndPoint>();
        }
        if (this.target != null) {
            destinations.add(this.target);
        }
        Collections.reverse(destinations);
        return this.plugin.delivery(this, destinations, username, dm);
    }

    public boolean postToUser(String username) {
        return this.post(DeliveryMethod.STANDARD, username);
    }

    @Override
    public String toString() {
        String rep = "{" + this.eventType + " " + RelayedMessage.typeString + "}";
        for (final String key : this.fields.keySet()) {
            rep = rep + " (" + key + ": " + this.fields.get(key) + ")";
        }
        return rep;
    }

}
