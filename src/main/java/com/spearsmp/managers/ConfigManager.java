package com.spearsmp.managers;

import com.spearsmp.spears.SpearType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

/**
 * Central place for reading config.yml and messages.yml.
 *
 * <p>Every value the plugin needs at runtime is read through this class so
 * that {@code /spear reload} only has to refresh two files here rather than
 * hunting through abilities/listeners for cached values.</p>
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadAll();
    }

    /** Re-reads config.yml (already reloaded by the plugin) and messages.yml from disk. */
    public void reloadAll() {
        this.config = plugin.getConfig();

        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration raw() {
        return config;
    }

    // ---------------------------------------------------------------
    // Spear-level accessors
    // ---------------------------------------------------------------

    public ConfigurationSection getSpearSection(SpearType type) {
        return config.getConfigurationSection("spears." + type.configKey());
    }

    public ConfigurationSection getAbilitySection(SpearType type, String abilityKey) {
        return config.getConfigurationSection("spears." + type.configKey() + ".abilities." + abilityKey);
    }

    public ConfigurationSection getRecipeSection(SpearType type) {
        return config.getConfigurationSection("spears." + type.configKey() + ".recipe");
    }

    public String getStorageType() {
        return config.getString("storage.type", "yaml");
    }

    public int getSecondaryActivationWindowSeconds() {
        return config.getInt("activation.secondary-activation-window-seconds", 5);
    }

    // ---------------------------------------------------------------
    // Messages / ActionBar
    // ---------------------------------------------------------------

    /** Returns a raw legacy-color-coded message string with the prefix prepended. */
    public String getRawMessage(String key) {
        String prefix = messages.getString("messages.prefix", "");
        String msg = messages.getString("messages." + key, "&cMissing message: " + key);
        return prefix + msg;
    }

    /** Returns a message with placeholders substituted, formatted as a Component. */
    public Component getMessage(String key, Map<String, String> placeholders) {
        String raw = getRawMessage(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public Component getMessage(String key) {
        return getMessage(key, Map.of());
    }

    public String getActionBarCooldownFormat() {
        return messages.getString("actionbar.cooldown-format", "&c%spear% on cooldown: &f%seconds%s");
    }

    public String getActionBarInvisFormat() {
        return messages.getString("actionbar.invis-countdown-format", "&d&lInvisible: &f%seconds%s remaining");
    }

    /** Renders arbitrary legacy-color text (used for item display names/lore). */
    public Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
