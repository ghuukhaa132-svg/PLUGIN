package com.spearsmp.managers;

import com.spearsmp.managers.storage.PlayerDataStore;
import com.spearsmp.managers.storage.SQLitePlayerDataStore;
import com.spearsmp.managers.storage.YamlPlayerDataStore;
import com.spearsmp.spears.SpearType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Facade over the configured {@link PlayerDataStore} backend (yaml or sqlite).
 * Every other class in the plugin should depend on this, not on the store
 * implementations directly, so the storage backend can change via config
 * without touching command/listener code.
 */
public final class PlayerDataManager {

    private final PlayerDataStore store;

    public PlayerDataManager(JavaPlugin plugin) {
        String type = plugin.getConfig().getString("storage.type", "yaml");
        this.store = "sqlite".equalsIgnoreCase(type)
                ? new SQLitePlayerDataStore(plugin)
                : new YamlPlayerDataStore(plugin);
        this.store.load();
    }

    /** @return true if the player has already crafted this spear (ever). */
    public boolean hasCrafted(UUID playerId, SpearType type) {
        return store.hasCrafted(playerId, type);
    }

    /** Permanently records that a player has crafted this spear. */
    public void markCrafted(UUID playerId, SpearType type) {
        store.markCrafted(playerId, type);
    }

    /** Admin action: allows a player to craft this specific spear again. */
    public void resetCrafted(UUID playerId, SpearType type) {
        store.resetCrafted(playerId, type);
    }

    /** Admin action: allows a player to craft every spear again. */
    public void resetAllCrafted(UUID playerId) {
        store.resetAllCrafted(playerId);
    }

    /** Forces an immediate flush to disk (called on plugin disable). */
    public void saveNow() {
        store.save();
        store.close();
    }
}
