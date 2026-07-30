package com.spearsmp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Tiny static helper for turning legacy '&'-coded strings into Adventure
 * {@link Component}s from contexts that don't have easy access to a
 * ConfigManager instance (e.g. static item-building utilities).
 */
public final class ConfigTextUtil {

    private ConfigTextUtil() {
    }

    public static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }
}
