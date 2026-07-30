package com.spearsmp.spears;

import java.util.Locale;
import java.util.Optional;

/**
 * Enumerates every heavenly spear the plugin currently supports.
 *
 * <p>To add a new spear: add an enum constant here, add a matching section
 * under {@code spears:} in config.yml, and implement its Ability class(es).
 * Nothing else in the plugin needs to change - commands, GUIs, recipe
 * registration, and persistence all iterate over this enum.</p>
 */
public enum SpearType {

    LEGENDARY("legendary"),
    GODLY("godly");

    private final String configKey;

    SpearType(String configKey) {
        this.configKey = configKey;
    }

    /**
     * @return the key used to look this spear up under {@code spears:} in config.yml
     */
    public String configKey() {
        return configKey;
    }

    /**
     * Parses a spear type from user/command input, case-insensitively.
     */
    public static Optional<SpearType> fromString(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        for (SpearType type : values()) {
            if (type.configKey.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
