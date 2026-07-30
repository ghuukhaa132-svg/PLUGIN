package com.spearsmp.abilities;

import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.CooldownManager;
import com.spearsmp.spears.SpearType;
import com.spearsmp.utils.ActionBarUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Legendary Spear - Ability 2: Invis.
 *
 * <p>Grants full invisibility (self and worn armor) for a configured
 * duration. Vanilla's Invisibility potion effect does not hide equipped
 * armor, so this temporarily clears the player's armor slots (restoring
 * them automatically when the effect ends, on disconnect, or on death) to
 * achieve true full-body invisibility without needing packet manipulation.</p>
 */
public final class InvisAbility implements Ability {

    private static final String KEY = "legendary_invis";

    /** Armor stored per player while their armor is hidden, so it can be restored. */
    private static final Map<UUID, ItemStack[]> HIDDEN_ARMOR = new HashMap<>();

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;

    public InvisAbility(Plugin plugin, ConfigManager configManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean activate(Player player) {
        ConfigurationSection cfg = configManager.getAbilitySection(SpearType.LEGENDARY, "invis");
        if (cfg == null) return false;

        if (cooldownManager.isOnCooldown(player.getUniqueId(), KEY)) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), KEY);
            ActionBarUtil.send(player, configManager.getActionBarCooldownFormat()
                    .replace("%spear%", "Invis").replace("%seconds%", String.valueOf(remaining)));
            return false;
        }

        int durationSeconds = cfg.getInt("duration-seconds", 15);
        int cooldownSeconds = cfg.getInt("cooldown-seconds", 30);
        boolean hideArmor = cfg.getBoolean("hide-armor", true);
        Sound sound = safeSound(cfg.getString("sound", "ENTITY_ENDERMAN_TELEPORT"));
        Particle particle = safeParticle(cfg.getString("particle", "PORTAL"));
        int particleCount = cfg.getInt("particle-count", 60);

        cooldownManager.setCooldown(player.getUniqueId(), KEY, cooldownSeconds);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationSeconds * 20, 0, false, false, false));

        if (hideArmor) {
            hideArmor(player);
        }

        if (sound != null) {
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        if (particle != null) {
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), particleCount, 0.4, 0.8, 0.4, 0.02);
        }

        ActionBarUtil.countdown(plugin, player, durationSeconds, remaining ->
                configManager.getActionBarInvisFormat().replace("%seconds%", String.valueOf(remaining)));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (hideArmor) {
                    restoreArmor(player);
                }
            }
        }.runTaskLater(plugin, durationSeconds * 20L);

        return true;
    }

    private void hideArmor(Player player) {
        UUID id = player.getUniqueId();
        if (HIDDEN_ARMOR.containsKey(id)) {
            return; // already hidden (re-activation edge case); avoid overwriting stored armor
        }
        HIDDEN_ARMOR.put(id, player.getInventory().getArmorContents().clone());
        player.getInventory().setArmorContents(new ItemStack[4]);
    }

    /** Restores a player's armor if it is currently hidden. Safe to call multiple times. */
    public static void restoreArmor(Player player) {
        ItemStack[] armor = HIDDEN_ARMOR.remove(player.getUniqueId());
        if (armor != null) {
            player.getInventory().setArmorContents(armor);
        }
    }

    private Sound safeSound(String name) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ENDERMAN_TELEPORT;
        }
    }

    private Particle safeParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Particle.PORTAL;
        }
    }
}
