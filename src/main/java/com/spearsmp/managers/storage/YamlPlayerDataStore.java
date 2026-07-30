package com.spearsmp.managers.storage;

import com.spearsmp.spears.SpearType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default persistence backend. Stores crafted-spear flags in players.yml as:
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     crafted:
 *       - legendary
 *       - godly
 * </pre>
 *
 * <p>Data is kept in an in-memory map for fast lookups and only written to
 * disk on save() (called after every mutation, and on plugin disable), which
 * keeps normal gameplay reads/writes off the disk I/O path entirely.</p>
 */
public final class YamlPlayerDataStore implements PlayerDataStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Set<String>> craftedBySpear = new ConcurrentHashMap<>();

    public YamlPlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    @Override
    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create players.yml: " + e.getMessage());
            }
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var playersSection = yaml.getConfigurationSection("players");
        craftedBySpear.clear();
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    var crafted = playersSection.getStringList(uuidStr + ".crafted");
                    craftedBySpear.put(uuid, new HashSet<>(crafted));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed UUID entries rather than crashing plugin startup.
                }
            }
        }
    }

    @Override
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : craftedBySpear.entrySet()) {
            yaml.set("players." + entry.getKey() + ".crafted", entry.getValue().stream().toList());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
        }
    }

    @Override
    public boolean hasCrafted(UUID playerId, SpearType type) {
        Set<String> set = craftedBySpear.get(playerId);
        return set != null && set.contains(type.configKey());
    }

    @Override
    public void markCrafted(UUID playerId, SpearType type) {
        craftedBySpear.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(type.configKey());
        save();
    }

    @Override
    public void resetCrafted(UUID playerId, SpearType type) {
        Set<String> set = craftedBySpear.get(playerId);
        if (set != null) {
            set.remove(type.configKey());
            save();
        }
    }

    @Override
    public void resetAllCrafted(UUID playerId) {
        craftedBySpear.remove(playerId);
        save();
    }
}
