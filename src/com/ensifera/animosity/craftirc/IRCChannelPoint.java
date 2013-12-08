package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.ensifera.animosity.craftirc.libs.org.jibble.pircbot.Colors;
import com.ensifera.animosity.craftirc.libs.org.jibble.pircbot.User;

public final class IRCChannelPoint implements SecuredEndPoint {
    private class NicknameComparator implements Comparator<String> {
        private Minebot bot;

        private NicknameComparator(Minebot bot) {
            this.bot = bot;
        }

        @Override
        public int compare(String o1, String o2) {
            final String prefixes = this.bot.getUserPrefixesInOrder();
            final String o1sub = o1.substring(0, 1);
            final String o2sub = o2.substring(0, 1);
            if (!prefixes.contains(o1sub) && !prefixes.contains(o2sub)) {
                return o1.compareToIgnoreCase(o2);
            } else if (!prefixes.contains(o1sub)) {
                return 1;
            } else if (!prefixes.contains(o2sub)) {
                return -1;
            } else {
                return prefixes.indexOf(o1sub) - prefixes.indexOf(o2sub);
            }
        }

    }

    private Minebot bot;
    private String channel;
    private boolean allowColors = true;
    private final int maxlen = 400;

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
        if (message.length() > maxlen) {
            String[] messages;
            final StringBuilder builder = new StringBuilder(message.length());
            final StringTokenizer tokenizer = new StringTokenizer(message, " ");
            int currentLine = 0;
            while (tokenizer.hasMoreTokens()) {
                final String nextWord = tokenizer.nextToken();
                if ((currentLine + nextWord.length()) > maxlen) {
                    builder.append("\n");
                    currentLine = 0;
                } else {
                    builder.append(' ');
                    currentLine++;
                }
                builder.append(nextWord);
                currentLine += (nextWord.length());
            }
            messages = builder.toString().split("\n");
            for (final String messagePart : messages) {
                this.bot.sendMessage(this.channel, messagePart);
            }
        } else {
            this.bot.sendMessage(this.channel, message);
        }
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
        boolean success = false;
        final String message = msg.getMessage(this);
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
