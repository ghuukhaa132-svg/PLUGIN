package com.spearsmp.listeners;

import com.spearsmp.SpearSMPPlugin;
import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.PlayerDataManager;
import com.spearsmp.managers.RecipeManager;
import com.spearsmp.spears.SpearType;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Recipe;

/**
 * Enforces the "one spear of each kind, ever, per player" crafting rule.
 *
 * <p>Uses two hooks:</p>
 * <ul>
 *     <li>{@link PrepareItemCraftEvent} - hides the result item in the
 *         crafting grid preview if the player has already crafted this
 *         spear, so it's visually obvious before they even click.</li>
 *     <li>{@link CraftItemEvent} - the authoritative check: cancels the
 *         craft and messages the player if already crafted, otherwise
 *         permanently records the craft.</li>
 * </ul>
 */
public final class CraftListener implements Listener {

    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final RecipeManager recipeManager;

    public CraftListener(SpearSMPPlugin plugin, ConfigManager configManager,
                          PlayerDataManager playerDataManager, RecipeManager recipeManager) {
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.recipeManager = recipeManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        SpearType type = identify(recipe);
        if (type == null) return;

        if (event.getView().getPlayer() instanceof Player player
                && playerDataManager.hasCrafted(player.getUniqueId(), type)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        SpearType type = identify(recipe);
        if (type == null) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (playerDataManager.hasCrafted(player.getUniqueId(), type)) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("already-crafted"));
            return;
        }

        // Recorded immediately (before the item even leaves the crafting grid) so that
        // rapid repeat clicks in the same tick can't slip through a second craft.
        playerDataManager.markCrafted(player.getUniqueId(), type);
    }

    private SpearType identify(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) {
            return null;
        }
        return recipeManager.typeForRecipeKey(keyed.getKey());
    }
}
