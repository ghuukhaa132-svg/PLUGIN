package com.spearsmp.abilities;

import com.spearsmp.managers.ConfigManager;
import com.spearsmp.managers.CooldownManager;
import com.spearsmp.spears.SpearType;
import com.spearsmp.utils.ActionBarUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Godly Spear - Ability 1: Godly Smash.
 *
 * <p>Launches the player straight up exactly N blocks (config), then slams
 * them back down. On landing, every nearby player within the configured
 * radius takes exact true damage (bypassing armor/enchants entirely) plus
 * knockback, with a large particle/sound payoff. The activating player is
 * immune to vanilla fall damage from this ability (tracked via
 * {@link #FALLING_PLAYERS}) - this class registers itself as a listener to
 * cancel that specific fall-damage event.</p>
 */
public final class GodlySmashAbility implements Ability, Listener {

    private static final String KEY = "godly_smash";
    private static final int ASCENT_TICKS = 20;      // ~1s to reach peak
    private static final int MAX_FALL_TICKS = 200;   // safety timeout (10s) in case player never lands

    /** Players currently mid-smash; their next fall-damage tick is cancelled. */
    private static final Set<UUID> FALLING_PLAYERS = new HashSet<>();

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;

    public GodlySmashAbility(Plugin plugin, ConfigManager configManager, CooldownManager cooldownManager) {
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
        ConfigurationSection cfg = configManager.getAbilitySection(SpearType.GODLY, "godly_smash");
        if (cfg == null) return false;

        if (cooldownManager.isOnCooldown(player.getUniqueId(), KEY)) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), KEY);
            ActionBarUtil.send(player, configManager.getActionBarCooldownFormat()
                    .replace("%spear%", "Godly Smash").replace("%seconds%", String.valueOf(remaining)));
            return false;
        }

        double launchHeight = cfg.getDouble("launch-height-blocks", 10.0);
        double damage = cfg.getDouble("damage", 10.0);
        double radius = cfg.getDouble("radius-blocks", 10.0);
        int cooldownSeconds = cfg.getInt("cooldown-seconds", 30);
        double knockback = cfg.getDouble("knockback-strength", 1.5);
        Sound launchSound = safeSound(cfg.getString("sound-launch", "ENTITY_WARDEN_SONIC_BOOM"));
        Sound impactSound = safeSound(cfg.getString("sound-impact", "ENTITY_GENERIC_EXPLODE"));
        Particle particle = safeParticle(cfg.getString("particle", "EXPLOSION_EMITTER"));
        int particleCount = cfg.getInt("particle-count", 3);
        Particle ringParticle = safeParticle(cfg.getString("ring-particle", "FLAME"));
        int ringParticleCount = cfg.getInt("ring-particle-count", 100);

        cooldownManager.setCooldown(player.getUniqueId(), KEY, cooldownSeconds);
        FALLING_PLAYERS.add(player.getUniqueId());

        if (launchSound != null) {
            player.getWorld().playSound(player.getLocation(), launchSound, 2.0f, 1.0f);
        }

        ascend(player, launchHeight, () -> descend(player, damage, radius, knockback, impactSound, particle, particleCount, ringParticle, ringParticleCount));

        return true;
    }

    /** Smoothly teleports the player upward over {@link #ASCENT_TICKS}, then runs onPeak. */
    private void ascend(Player player, double totalHeight, Runnable onPeak) {
        double stepHeight = totalHeight / ASCENT_TICKS;
        player.setVelocity(new Vector(0, 0.01, 0)); // cancel residual downward velocity

        new BukkitRunnable() {
            int ticksRun = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticksRun >= ASCENT_TICKS) {
                    cancel();
                    onPeak.run();
                    return;
                }
                Location current = player.getLocation();
                Location next = current.clone().add(0, stepHeight, 0);
                if (next.getBlock().getType().isSolid()) {
                    cancel();
                    onPeak.run();
                    return;
                }
                player.teleport(next);
                ticksRun++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Rapidly pulls the player back down until they hit the ground, then triggers the smash impact. */
    private void descend(Player player, double damage, double radius, double knockback,
                          Sound impactSound, Particle particle, int particleCount,
                          Particle ringParticle, int ringParticleCount) {
        new BukkitRunnable() {
            int ticksRun = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    FALLING_PLAYERS.remove(player.getUniqueId());
                    return;
                }
                if (player.isOnGround() || ticksRun >= MAX_FALL_TICKS) {
                    cancel();
                    FALLING_PLAYERS.remove(player.getUniqueId());
                    onLand(player, damage, radius, knockback, impactSound, particle, particleCount, ringParticle, ringParticleCount);
                    return;
                }
                player.setVelocity(new Vector(0, -3.0, 0));
                ticksRun++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void onLand(Player player, double damage, double radius, double knockback,
                         Sound impactSound, Particle particle, int particleCount,
                         Particle ringParticle, int ringParticleCount) {
        Location impact = player.getLocation();

        if (impactSound != null) {
            player.getWorld().playSound(impact, impactSound, 3.0f, 0.8f);
        }
        if (particle != null) {
            player.getWorld().spawnParticle(particle, impact, particleCount, 0.5, 0.2, 0.5, 0.0);
        }
        if (ringParticle != null) {
            player.getWorld().spawnParticle(ringParticle, impact, ringParticleCount, radius / 2.5, 0.3, radius / 2.5, 0.05);
        }

        for (var entity : player.getWorld().getNearbyEntities(impact, radius, radius, radius)) {
            if (entity instanceof Player target && !target.equals(player)
                    && target.getGameMode() != GameMode.CREATIVE && target.getGameMode() != GameMode.SPECTATOR) {
                if (target.getLocation().distance(impact) <= radius) {
                    dealTrueDamage(target, damage);
                    Vector direction = target.getLocation().toVector().subtract(impact.toVector());
                    if (direction.lengthSquared() == 0) {
                        direction = new Vector(1, 0, 0);
                    }
                    direction.normalize().multiply(knockback).setY(0.5);
                    target.setVelocity(direction);
                }
            }
        }
    }

    private void dealTrueDamage(LivingEntity target, double damage) {
        target.damage(0.0);
        double newHealth = Math.max(0.0, target.getHealth() - damage);
        target.setHealth(newHealth);
    }

    /**
     * Cancels fall damage for players currently mid-smash. Registered as a
     * normal Bukkit listener in the main plugin class alongside the other
     * listeners.
     */
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && FALLING_PLAYERS.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Sound safeSound(String name) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Particle safeParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
