package com.spearsmp.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Helpers for sending action bar text, including a simple repeating
 * countdown (used for ability cooldown displays and invisibility duration).
 */
public final class ActionBarUtil {

    private ActionBarUtil() {
    }

    public static void send(Player player, String legacyText) {
        player.sendActionBar(ConfigTextUtil.legacy(legacyText));
    }

    /**
     * Ticks a countdown from {@code totalSeconds} down to 0, calling
     * {@code formatter} each second to build the displayed text, sent to the
     * player's action bar. Cancels automatically when the player logs off.
     *
     * @param formatter receives the remaining seconds and returns the legacy-color text to show
     */
    public static void countdown(Plugin plugin, Player player, int totalSeconds, java.util.function.IntFunction<String> formatter) {
        new BukkitRunnable() {
            int remaining = totalSeconds;

            @Override
            public void run() {
                if (!player.isOnline() || remaining <= 0) {
                    cancel();
                    return;
                }
                send(player, formatter.apply(remaining));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
