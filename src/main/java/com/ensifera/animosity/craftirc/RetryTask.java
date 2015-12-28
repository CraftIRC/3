package com.ensifera.animosity.craftirc;

import java.util.TimerTask;

final class RetryTask extends TimerTask {
    private final Minebot bot;
    private final String channel;
    private final CraftIRC plugin;

    RetryTask(CraftIRC plugin, Minebot bot, String channel) {
        this.bot = bot;
        this.channel = channel;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (this.channel == null) {
            if (!this.bot.isConnected()) {
                this.bot.connectToIrc();
                this.plugin.scheduleForRetry(this.bot, null);
            }
        } else {
            if (!this.bot.isIn(this.channel)) {
                this.bot.joinIrcChannel(this.channel);
                this.plugin.scheduleForRetry(this.bot, this.channel);
            }
        }
    }
}
