package com.spearsmp;

import com.spearsmp.commands.SpearCommand;
import com.spearsmp.listeners.CraftListener;
import com.spearsmp.listeners.PlayerInteractListener;
import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.CooldownManager;
import com.spearsmp.managers.PlayerDataManager;
import com.spearsmp.managers.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the Spear SMP Plugin.
 *
 * <p>Wires together all managers, registers listeners/commands, and exposes
 * the shared managers to the rest of the plugin via simple getters. Kept
 * intentionally thin: all real logic lives in the managers/abilities/listeners
 * packages so new spears/abilities can be added without touching this class.</p>
 */
public final class SpearSMPPlugin extends JavaPlugin {

    private static SpearSMPPlugin instance;

    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private PlayerDataManager playerDataManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");

        this.configManager = new ConfigManager(this);
        this.cooldownManager = new CooldownManager();
        this.playerDataManager = new PlayerDataManager(this);
        this.recipeManager = new RecipeManager(this, configManager);

        // Register crafting recipes defined in config.yml
        recipeManager.registerAllRecipes();

        // Listeners
        getServer().getPluginManager().registerEvents(
                new PlayerInteractListener(this, configManager, cooldownManager), this);
        getServer().getPluginManager().registerEvents(
                new CraftListener(this, configManager, playerDataManager, recipeManager), this);

        // Commands
        SpearCommand spearCommand = new SpearCommand(this, configManager, playerDataManager, recipeManager);
        var command = getCommand("spear");
        if (command != null) {
            command.setExecutor(spearCommand);
            command.setTabCompleter(spearCommand);
        }

        getLogger().info("Spear SMP Plugin enabled - " + recipeManager.getRegisteredRecipeCount() + " recipe(s) registered.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveNow();
        }
        getLogger().info("Spear SMP Plugin disabled.");
    }

    /**
     * Reloads config.yml, messages.yml, and re-registers all crafting recipes.
     * Called by the /spear reload command.
     */
    public void reloadEverything() {
        reloadConfig();
        configManager.reloadAll();
        recipeManager.unregisterAllRecipes();
        recipeManager.registerAllRecipes();
    }

    private void saveResourceIfMissing(String name) {
        var file = new java.io.File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    public static SpearSMPPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
