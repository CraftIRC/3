package com.ensifera.animosity.craftirc;

import com.ensifera.animosity.craftirc.libs.com.sk89q.util.config.ConfigurationNode;
import com.ensifera.animosity.craftirc.libs.org.jibble.pircbot.Colors;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RelayedMessage {
    enum DeliveryMethod {
        STANDARD,
        ADMINS,
        COMMAND
    }

    private static final String IRC_BOLD = Character.toString((char) 2);
    private static final String IRC_COLOR = Character.toString((char) 3);
    private static final String IRC_RESET = Character.toString((char) 15);
    private static final String IRC_REVERSE = Character.toString((char) 22);
    private static final String IRC_UNDERLINE = Character.toString((char) 31);

    private final CraftIRC plugin;
    private final EndPoint source; // Origin endpoint of the message
    private final EndPoint target; // Target endpoint of the message
    private final String eventType; // Event type
    private final LinkedList<EndPoint> cc = new LinkedList<>(); // Multiple extra targets for the message
    private String template; // Formatting string
    private final Map<String, String> fields; // All message attributes
    private final Set<String> doNotColorFields; // Do not allow color codes on these fields
    private final Map<String, Boolean> flags;

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
        if ((!eventType.equals("")) && !eventType.equals("command") && (target != null)) {
            this.template = plugin.cFormatting(eventType, this);
        }
        this.fields = new HashMap<>();
        this.doNotColorFields = new HashSet<>();
        this.flags = new HashMap<>();
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

    public void setFlag(String key, boolean value) {
        this.flags.put(key, value);
    }

    public boolean getFlag(String key) {
        Boolean flag = this.flags.get(key);
        return (flag != null) ? flag : false;
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

        // Resolve target
        realTarget = this.target;
        if (realTarget == null) {
            if (currentTarget == null) {
                return this.fields.get("message");
            }
            realTarget = currentTarget;
            result = this.plugin.cFormatting(this.eventType, this, realTarget);
            if (result == null) {
                result = this.template;
            }
        }

        // IRC color code aliases
        if (realTarget.getType() == EndPoint.Type.IRC) {
            // Replace named colours
            for (ConfigurationNode node : plugin.getColorMap()) {
                String colorName = node.getString("name", "");
                if (colorName.length() > 0) {
                    String c = plugin.cColorIrcNormalize(node.getString("irc", "01"));
                    result = result.replace("%" + colorName + "%", c.equals("-1") ? Colors.NORMAL : "\u0003" + c);
                }
            }
            result = result.replaceAll("%k([0-9]{1,2})%", IRC_COLOR + "$1");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", IRC_COLOR + "$1,$2");
            result = result.replace("%k%", IRC_COLOR);
            result = result.replace("%o%", IRC_RESET);
            result = result.replace("%b%", IRC_BOLD);
            result = result.replace("%u%", IRC_UNDERLINE);
            result = result.replace("%r%", IRC_REVERSE);
        } else if (realTarget.getType() == EndPoint.Type.MINECRAFT) {
            // Replace named colours
            for (ConfigurationNode node : plugin.getColorMap()) {
                String colorName = node.getString("name", "");
                if (colorName.length() > 0)
                    result = result.replace("%" + colorName + "%", plugin.cColorGameFromName(colorName));
            }
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", this.plugin.cColorGameFromName("foreground"));
            result = result.replace("%o%", this.plugin.cColorGameFromName("foreground"));
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", "");
        } else { // EndPoint.Type.PLAIN
            // Replace named colours
            for (ConfigurationNode node : plugin.getColorMap()) {
                String colorName = node.getString("name", "");
                if (colorName.length() > 0)
                    result = result.replace("%" + colorName + "%", "");
            }
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", "");
            result = result.replace("%o%", "");
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", "");
        }

        // Fields
        for (String fieldName : this.fields.keySet()) {
            String replacement = this.fields.get(fieldName);
            if (replacement == null) {
                continue;
            }
            if (!this.doNotColorFields.contains(fieldName)) {
                replacement = ChatColor.translateAlternateColorCodes('&', replacement);
            }
            if (realTarget.getType() == EndPoint.Type.IRC && fieldName.equals("sender")) {
                // if anti-highlight isn't set (disabled), this function returns the same value as the input
                replacement = plugin.processAntiHighlight(replacement);
            }
            // Global find/replacements with regular expressions.
            Map<String, Map<String, String>> replaceFilters = this.plugin.cReplaceFilters();
            if (replaceFilters.containsKey(fieldName))
                for (String search : replaceFilters.get(fieldName).keySet())
                    try {
                        replacement = replacement.replaceAll(search, replaceFilters.get(fieldName).get(search));
                    } catch (PatternSyntaxException e) {
                        this.plugin.logWarn("Pattern is invalid: " + e.getPattern());
                    } catch (IllegalArgumentException e) {
                        if ("Illegal group reference".equals(e.getMessage()))
                            this.plugin.logWarn("Invalid replacement - backreference not found.");
                        else
                            throw e;
                    }
            result = result.replace("%" + fieldName + "%", replacement);
        }

        // Convert colors
        final boolean colors = this.plugin.cPathAttribute(this.fields.get("source"), this.fields.get("target"), "attributes.colors");
        if (this.source.getType() == EndPoint.Type.MINECRAFT) {
            if ((realTarget.getType() == EndPoint.Type.IRC) && colors) {
                final Pattern color_codes = Pattern.compile("\u00A7([A-FK-Ra-fk-r0-9])?");
                Matcher find_colors = color_codes.matcher(result);
                while (find_colors.find()) {
                    result = find_colors.replaceFirst(this.plugin.cColorIrcFromGame("\u00A7" + find_colors.group(1)));
                    find_colors = color_codes.matcher(result);
                }
            } else if ((realTarget.getType() != EndPoint.Type.MINECRAFT) || !colors) {
                // Strip colors
                result = result.replaceAll("(\u00A7([A-FK-Ra-fk-r0-9])?)", "");
            }
        }
        if (this.source.getType() == EndPoint.Type.IRC) {
            if ((realTarget.getType() == EndPoint.Type.MINECRAFT) && colors) {
                result = result.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 22) + "|" + Character.toString((char) 31) + ")", "");
                final Pattern color_codes = Pattern.compile(Character.toString((char) 3) + "([0-9]{1,2})(,[0-9]{1,2})?");
                Matcher find_colors = color_codes.matcher(result);
                while (find_colors.find()) {
                    result = find_colors.replaceFirst(this.plugin.cColorGameFromIrc(find_colors.group(1)));
                    find_colors = color_codes.matcher(result);
                }
                result = result.replaceAll(Character.toString((char) 15) + "|" + Character.toString((char) 3), this.plugin.cColorGameFromName("foreground"));
            } else if ((realTarget.getType() != EndPoint.Type.IRC) || !colors) {
                // Strip colors
                result = result.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 15) + "|" + Character.toString((char) 22) + Character.toString((char) 31) + "|" + Character.toString((char) 3) + "[0-9]{0,2}(,[0-9]{1,2})?)", "");
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
            destinations = new LinkedList<>(this.cc);
        } else {
            destinations = new LinkedList<>();
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
        // create an ordered set so the most important keys are always first
        LinkedHashSet<String> orderedKeys = new LinkedHashSet<>();
        orderedKeys.addAll(Arrays.asList("source", "target", "sender", "message"));
        orderedKeys.addAll(this.fields.keySet());

        StringBuilder builder = new StringBuilder();
        builder.append("{TYPE: ").append(this.eventType).append("}");

        for (final String key : orderedKeys) {
            builder.append(" (").append(key).append(": ").append(this.fields.get(key)).append(")");
        }
        return builder.toString();
    }
}
