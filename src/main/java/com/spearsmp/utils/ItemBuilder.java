package com.spearsmp.utils;

import com.spearsmp.spears.SpearType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Builds spear {@link ItemStack}s from config-driven values and stamps them
 * with a PersistentDataContainer marker so listeners can reliably identify
 * "is this item a Spear SMP spear, and which one" without relying on
 * display name/lore matching.
 */
public final class ItemBuilder {

    /** PDC key whose string value is the {@link SpearType#configKey()} of the spear. */
    public static final String SPEAR_TYPE_KEY = "spear_type";

    private ItemBuilder() {
    }

    public static ItemStack buildSpear(SpearType type,
                                        NamespacedKey pdcKey,
                                        String displayNameLegacy,
                                        List<String> loreLegacy,
                                        int customModelData,
                                        Material material,
                                        boolean glow,
                                        boolean unbreakable) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Component displayName = ConfigTextUtil.legacy(displayNameLegacy).decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);

        List<Component> lore = loreLegacy.stream()
                .map(line -> ConfigTextUtil.legacy(line).decoration(TextDecoration.ITALIC, false))
                .toList();
        meta.lore(lore);

        meta.setCustomModelData(customModelData);
        meta.setUnbreakable(unbreakable);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);

        if (glow) {
            // Vanilla-safe fake enchant glow: add a harmless enchantment and hide it via ItemFlag.
            meta.addEnchant(Enchantment.LOYALTY, 1, true);
        }

        meta.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, type.configKey());

        item.setItemMeta(meta);
        return item;
    }

    /** Reads back the spear type marker from an item, or null if it isn't a Spear SMP item. */
    public static SpearType readSpearType(ItemStack item, NamespacedKey pdcKey) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String value = meta.getPersistentDataContainer().get(pdcKey, PersistentDataType.STRING);
        return SpearType.fromString(value).orElse(null);
    }
}
