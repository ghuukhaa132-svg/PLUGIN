package com.spearsmp.managers;

import com.spearsmp.spears.SpearFactory;
import com.spearsmp.spears.SpearType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registers each spear's crafting recipe from the {@code recipe:} section of
 * its config.yml entry, and can unregister/re-register everything on
 * {@code /spear reload} so ingredient/shape changes take effect without a
 * server restart.
 */
public final class RecipeManager {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final SpearFactory spearFactory;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.spearFactory = new SpearFactory(plugin, configManager);
    }

    public SpearFactory getSpearFactory() {
        return spearFactory;
    }

    /** Registers every enabled recipe defined under {@code spears.<type>.recipe} in config.yml. */
    public void registerAllRecipes() {
        for (SpearType type : SpearType.values()) {
            registerRecipe(type);
        }
    }

    private void registerRecipe(SpearType type) {
        ConfigurationSection recipeSection = configManager.getRecipeSection(type);
        if (recipeSection == null || !recipeSection.getBoolean("enabled", true)) {
            return;
        }

        String keyName = recipeSection.getString("key", type.configKey() + "_spear");
        List<String> shape = recipeSection.getStringList("shape");
        ConfigurationSection ingredients = recipeSection.getConfigurationSection("ingredients");
        int resultAmount = recipeSection.getInt("result-amount", 1);

        if (shape.isEmpty() || ingredients == null) {
            plugin.getLogger().warning("Recipe for '" + type.configKey() + "' is misconfigured (missing shape/ingredients) - skipping.");
            return;
        }

        ItemStack result = spearFactory.build(type);
        result.setAmount(Math.max(1, resultAmount));

        NamespacedKey key = new NamespacedKey(plugin, keyName);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape.toArray(new String[0]));

        for (Map.Entry<String, Object> entry : ingredients.getValues(false).entrySet()) {
            char symbol = entry.getKey().charAt(0);
            String materialName = String.valueOf(entry.getValue());
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Unknown material '" + materialName + "' in recipe for " + type.configKey() + " - skipping recipe.");
                return;
            }
            recipe.setIngredient(symbol, material);
        }

        plugin.getServer().addRecipe(recipe);
        registeredKeys.add(key);
    }

    /** Removes every recipe this manager previously registered. Called before re-registering on reload. */
    public void unregisterAllRecipes() {
        for (NamespacedKey key : registeredKeys) {
            plugin.getServer().removeRecipe(key);
        }
        registeredKeys.clear();
    }

    public int getRegisteredRecipeCount() {
        return registeredKeys.size();
    }

    /**
     * Given a recipe key (from a CraftItemEvent), returns which SpearType it
     * belongs to, or null if it's not one of ours.
     */
    public SpearType typeForRecipeKey(NamespacedKey key) {
        for (SpearType type : SpearType.values()) {
            ConfigurationSection recipeSection = configManager.getRecipeSection(type);
            if (recipeSection == null) continue;
            String keyName = recipeSection.getString("key", type.configKey() + "_spear");
            if (key.getKey().equals(keyName)) {
                return type;
            }
        }
        return null;
    }
}
