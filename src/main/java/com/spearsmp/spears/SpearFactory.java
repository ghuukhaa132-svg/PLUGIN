package com.spearsmp.spears;

import com.spearsmp.managers.ConfigManager;
import com.spearsmp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Builds ready-to-give {@link ItemStack}s for any {@link SpearType}, reading
 * display name, lore, material, CustomModelData, and glow/unbreakable flags
 * straight out of config.yml. This is the single place that turns config
 * into an item, used by both {@code /spear give} and the crafting listener.
 */
public final class SpearFactory {

    private final ConfigManager configManager;
    private final NamespacedKey spearTypeKey;

    public SpearFactory(Plugin plugin, ConfigManager configManager) {
        this.configManager = configManager;
        this.spearTypeKey = new NamespacedKey(plugin, ItemBuilder.SPEAR_TYPE_KEY);
    }

    public NamespacedKey getSpearTypeKey() {
        return spearTypeKey;
    }

    public ItemStack build(SpearType type) {
        ConfigurationSection section = configManager.getSpearSection(type);
        if (section == null) {
            throw new IllegalStateException("Missing config section for spear: " + type.configKey());
        }

        String displayName = section.getString("display-name", type.name());
        List<String> lore = section.getStringList("lore");
        int customModelData = section.getInt("custom-model-data", 0);
        Material material = Material.matchMaterial(section.getString("material", "TRIDENT"));
        if (material == null) {
            material = Material.TRIDENT;
        }
        boolean glow = section.getBoolean("glow", true);
        boolean unbreakable = section.getBoolean("unbreakable", true);

        return ItemBuilder.buildSpear(type, spearTypeKey, displayName, lore, customModelData, material, glow, unbreakable);
    }

    /** Reads the spear type off an item, or null if it isn't a spear from this plugin. */
    public SpearType identify(ItemStack item) {
        return ItemBuilder.readSpearType(item, spearTypeKey);
    }
}
