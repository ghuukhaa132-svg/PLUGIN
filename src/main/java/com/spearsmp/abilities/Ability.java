package com.spearsmp.abilities;

import org.bukkit.entity.Player;

/**
 * Contract for a single spear ability (e.g. Dasher, Invis, Godly Smash).
 *
 * <p>Implementations own their own config reads, cooldown key, and effect
 * logic. To add a brand-new ability: implement this interface, wire its
 * config section into config.yml, and call it from
 * {@link com.spearsmp.listeners.PlayerInteractListener}. Nothing else in the
 * plugin needs to change.</p>
 */
public interface Ability {

    /**
     * Executes the ability for this player if it is not on cooldown.
     * Implementations are responsible for their own cooldown checks/sets,
     * messaging, sounds, and particles.
     *
     * @return true if the ability actually triggered (was not on cooldown)
     */
    boolean activate(Player player);

    /** Unique key used for cooldown tracking and messaging, e.g. "legendary_dasher". */
    String getKey();
}
