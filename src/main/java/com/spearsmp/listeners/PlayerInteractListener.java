package com.spearsmp.listeners;

import com.spearsmp.SpearSMPPlugin;
import com.spearsmp.abilities.DasherAbility;
import com.spearsmp.abilities.GodlySmashAbility;
import com.spearsmp.abilities.InvisAbility;
import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.CooldownManager;
import com.spearsmp.spears.SpearFactory;
import com.spearsmp.spears.SpearType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for right-click interactions with spears and routes them to the
 * correct {@link com.spearsmp.abilities.Ability}.
 *
 * <p>Activation rules (see config.yml {@code activation:} section):</p>
 * <ul>
 *     <li>SHIFT + Right Click = primary ability (Dasher / Godly Smash)</li>
 *     <li>SHIFT + Right Click again, within the configured window, while a
 *         spear with a secondary ability is held = secondary ability (Invis)</li>
 * </ul>
 */
public final class PlayerInteractListener implements Listener {

    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final SpearFactory spearFactory;

    private final DasherAbility dasherAbility;
    private final InvisAbility invisAbility;
    private final GodlySmashAbility godlySmashAbility;

    /** Last time (epoch millis) each player triggered a spear's primary ability, for secondary-window detection. */
    private final Map<UUID, Long> lastPrimaryUse = new ConcurrentHashMap<>();

    public PlayerInteractListener(SpearSMPPlugin plugin, ConfigManager configManager, CooldownManager cooldownManager) {
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.spearFactory = plugin.getRecipeManager().getSpearFactory();

        this.dasherAbility = new DasherAbility(plugin, configManager, cooldownManager);
        this.invisAbility = new InvisAbility(plugin, configManager, cooldownManager);
        this.godlySmashAbility = new GodlySmashAbility(plugin, configManager, cooldownManager);

        // Godly Smash needs to cancel its own fall damage, so it listens too.
        plugin.getServer().getPluginManager().registerEvents(godlySmashAbility, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid double-firing for main+off hand
        }
        if (!event.getAction().isRightClick()) {
            return;
        }

        ItemStack item = event.getItem();
        SpearType type = spearFactory.identify(item);
        if (type == null) {
            return;
        }

        var player = event.getPlayer();
        String permission = "spear.use." + type.configKey();
        if (!player.hasPermission(permission)) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        if (!player.isSneaking()) {
            return; // both abilities in this plugin require sneaking to activate
        }

        switch (type) {
            case LEGENDARY -> handleLegendary(player.getUniqueId(), player);
            case GODLY -> godlySmashAbility.activate(player);
        }
    }

    private void handleLegendary(UUID playerId, org.bukkit.entity.Player player) {
        int windowSeconds = configManager.getSecondaryActivationWindowSeconds();
        Long lastUse = lastPrimaryUse.get(playerId);
        long now = System.currentTimeMillis();

        boolean withinSecondaryWindow = lastUse != null && (now - lastUse) <= windowSeconds * 1000L;

        if (withinSecondaryWindow) {
            invisAbility.activate(player);
            lastPrimaryUse.remove(playerId); // consume the window
        } else {
            boolean activated = dasherAbility.activate(player);
            if (activated) {
                lastPrimaryUse.put(playerId, now);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastPrimaryUse.remove(id);
        cooldownManager.clearAll(id);
        InvisAbility.restoreArmor(event.getPlayer());
    }
}
