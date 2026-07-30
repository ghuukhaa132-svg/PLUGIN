package com.spearsmp.abilities;

import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.CooldownManager;
import com.spearsmp.utils.ActionBarUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Legendary Spear - Ability 1: Dasher.
 *
 * <p>Dashes the player forward exactly N blocks (config), dealing exact,
 * armor/enchant-ignoring true damage to the first player struck along the
 * path. Movement is done via incremental teleports rather than velocity so
 * it is immune to slowdown effects (cobwebs, soul sand, weighted damage
 * reduction) and does not touch the food/exhaustion system.</p>
 */
public final class DasherAbility implements Ability {

    private static final String KEY = "legendary_dasher";
    private static final int TICKS_TOTAL = 10; // 0.5s dash, smooth 10-step movement

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;

    public DasherAbility(Plugin plugin, ConfigManager configManager, CooldownManager cooldownManager) {
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
        ConfigurationSection cfg = configManager.getAbilitySection(
                com.spearsmp.spears.SpearType.LEGENDARY, "dasher");
        if (cfg == null) return false;

        if (cooldownManager.isOnCooldown(player.getUniqueId(), KEY)) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), KEY);
            ActionBarUtil.send(player, configManager.getActionBarCooldownFormat()
                    .replace("%spear%", "Dasher").replace("%seconds%", String.valueOf(remaining)));
            return false;
        }

        double distance = cfg.getDouble("distance-blocks", 5.0);
        double damage = cfg.getDouble("damage", 6.0);
        int cooldownSeconds = cfg.getInt("cooldown-seconds", 30);
        boolean ignoreHunger = cfg.getBoolean("ignore_hunger", true);
        Sound sound = safeSound(cfg.getString("sound", "ENTITY_ENDER_DRAGON_FLAP"));
        Particle particle = safeParticle(cfg.getString("particle", "CLOUD"));
        int particleCount = cfg.getInt("particle-count", 40);

        cooldownManager.setCooldown(player.getUniqueId(), KEY, cooldownSeconds);

        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        double stepDistance = distance / TICKS_TOTAL;

        if (sound != null) {
            player.getWorld().playSound(player.getLocation(), sound, 1.5f, 1.0f);
        }

        Set<LivingEntity> alreadyHit = new HashSet<>();

        new BukkitRunnable() {
            int ticksRun = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticksRun >= TICKS_TOTAL) {
                    cancel();
                    return;
                }

                if (ignoreHunger) {
                    player.setExhaustion(0f);
                }

                Location current = player.getLocation();
                Location next = current.clone().add(direction.clone().multiply(stepDistance));

                // Stop early if the next step would put the player inside a solid block.
                if (next.getBlock().getType().isSolid()) {
                    cancel();
                    return;
                }

                player.teleport(new Location(next.getWorld(), next.getX(), current.getY(), next.getZ(),
                        current.getYaw(), current.getPitch()));

                if (particle != null) {
                    player.getWorld().spawnParticle(particle, player.getLocation(), particleCount, 0.3, 0.3, 0.3, 0.01);
                }

                // Hit-detection: anyone inside a small box just ahead of the player takes true damage once.
                BoundingBox hitBox = BoundingBox.of(player.getLocation(), 0.8, 1.2, 0.8);
                for (var entity : player.getWorld().getNearbyEntities(hitBox)) {
                    if (entity instanceof Player target && !target.equals(player)
                            && !alreadyHit.contains(target) && target.getGameMode() != org.bukkit.GameMode.CREATIVE
                            && target.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        alreadyHit.add(target);
                        dealTrueDamage(target, damage);
                    }
                }

                ticksRun++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    /**
     * Deals exact damage that bypasses armor, Protection enchantments, and
     * Netherite armor entirely, by subtracting directly from health rather
     * than routing through Bukkit's damage-reduction pipeline.
     */
    private void dealTrueDamage(LivingEntity target, double damage) {
        target.damage(0.0); // triggers hurt animation/sound without applying reduced damage
        double newHealth = Math.max(0.0, target.getHealth() - damage);
        target.setHealth(newHealth);
    }

    private Sound safeSound(String name) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ENDER_DRAGON_FLAP;
        }
    }

    private Particle safeParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Particle.CLOUD;
        }
    }
}
