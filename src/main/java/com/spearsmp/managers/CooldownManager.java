package com.spearsmp.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks ability cooldowns per player per ability key (e.g. "legendary_dasher").
 *
 * <p>Backed by a {@link ConcurrentHashMap} so it is safe to read/write from
 * async contexts if ever needed; all current call sites are on the main
 * thread since they originate from Bukkit events, but this removes a whole
 * class of future bugs for free.</p>
 */
public final class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * @return remaining cooldown in seconds, or 0 if the ability is ready to use.
     */
    public long getRemainingSeconds(UUID playerId, String abilityKey) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) {
            return 0;
        }
        Long expiresAt = playerMap.get(abilityKey);
        if (expiresAt == null) {
            return 0;
        }
        long remainingMillis = expiresAt - System.currentTimeMillis();
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000; // ceil to seconds
    }

    public boolean isOnCooldown(UUID playerId, String abilityKey) {
        return getRemainingSeconds(playerId, abilityKey) > 0;
    }

    public void setCooldown(UUID playerId, String abilityKey, int seconds) {
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(abilityKey, System.currentTimeMillis() + (seconds * 1000L));
    }

    /** Clears every cooldown for a player (e.g. on quit, to free memory). */
    public void clearAll(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
