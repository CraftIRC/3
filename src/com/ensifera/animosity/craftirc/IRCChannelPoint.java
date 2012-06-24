package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jibble.pircbot.User;
import org.jibble.pircbot.Colors;

class NicknameComparator implements Comparator<String> {

    Minebot bot;

    NicknameComparator(Minebot bot) {
        this.bot = bot;
    }

    @Override
    public int compare(String o1, String o2) {
        final String prefixes = this.bot.getUserPrefixesInOrder();
        if (!prefixes.contains(o1.substring(0, 1)) && !prefixes.contains(o2.substring(0, 1))) {
            return o1.compareToIgnoreCase(o2);
        } else if (!prefixes.contains(o1.substring(0, 1))) {
            return 1;
        } else if (!prefixes.contains(o2.substring(0, 1))) {
            return -1;
        } else {
            return prefixes.indexOf(o1.substring(0, 1)) - prefixes.indexOf(o2.substring(0, 1));
        }
    }

}

public class IRCChannelPoint implements SecuredEndPoint {

    Minebot bot;
    String channel;
    boolean allowColors = true;

    IRCChannelPoint(Minebot bot, String channel) {
        this.bot = bot;
        this.channel = channel;
    }

    @Override
    public Type getType() {
        return EndPoint.Type.IRC;
    }

    @Override
    public Security getSecurity() {
        return SecuredEndPoint.Security.UNSECURED;
    }

    @Override
    public void messageIn(RelayedMessage msg) {
        String message = msg.getMessage(this);
        if (!this.allowColors) {
            message = Colors.removeFormattingAndColors(message);
        }
        this.bot.sendMessage(this.channel, message);
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        if (this.bot.getChannelPrefixes().contains(username.substring(0, 1))) {
            return false;
        }
        this.bot.sendNotice(username, msg.getMessage(this));
        return true;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        final String message = msg.getMessage(this);
        boolean success = false;
        for (final String nick : this.listDisplayUsers()) {
            if (this.bot.getPlugin().cBotAdminPrefixes(this.bot.getId()).contains(nick.substring(0, 1))) {
                success = true;
                this.bot.sendNotice(nick.substring(1), message);
            }
        }
        return success;
    }

    @Override
    public List<String> listUsers() {
        final List<String> users = new LinkedList<String>();
        for (final User user : this.bot.getUsers(this.channel)) {
            users.add(user.getNick());
        }
        return users;
    }

    @Override
    public List<String> listDisplayUsers() {
        final List<String> users = new LinkedList<String>();
        for (final User user : this.bot.getUsers(this.channel)) {
            users.add(this.bot.getHighestUserPrefix(user) + user.getNick());
        }
        Collections.sort(users, new NicknameComparator(this.bot));
        return users;
    }

    public void setAllowColors(boolean allowColors) {
        this.allowColors = allowColors;
    }

    public boolean getAllowColors() {
        return this.allowColors;
    }
}
