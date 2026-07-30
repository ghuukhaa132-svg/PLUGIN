package com.spearsmp.managers.storage;

import com.spearsmp.spears.SpearType;

import java.util.UUID;

/**
 * Backend-agnostic persistence contract for "has this player crafted spear X".
 *
 * <p>Implementations: {@link YamlPlayerDataStore} (default, players.yml) and
 * {@link SQLitePlayerDataStore} (optional, players.db). Selected at startup
 * via {@code storage.type} in config.yml.</p>
 */
public interface PlayerDataStore {

    /** Loads all persisted data into memory. Called once on plugin enable. */
    void load();

    /** Flushes in-memory data to disk. Safe to call frequently. */
    void save();

    /** Releases any resources (file handles, DB connections) on shutdown. */
    default void close() {
        // no-op by default
    }

    boolean hasCrafted(UUID playerId, SpearType type);

    void markCrafted(UUID playerId, SpearType type);

    /** Resets the craft flag for a single spear type. */
    void resetCrafted(UUID playerId, SpearType type);

    /** Resets the craft flag for every spear type for this player. */
    void resetAllCrafted(UUID playerId);
}
