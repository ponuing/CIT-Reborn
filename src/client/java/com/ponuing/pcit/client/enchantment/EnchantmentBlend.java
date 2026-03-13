package com.ponuing.pcit.client.enchantment;

import java.util.Locale;

public enum EnchantmentBlend {
    GLINT,
    ADDITIVE,
    TRANSLUCENT,
    SOLID;

    public static EnchantmentBlend parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return GLINT;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "add", "additive" -> ADDITIVE;
            case "alpha", "transparent", "translucent" -> TRANSLUCENT;
            case "solid", "none", "opaque" -> SOLID;
            default -> GLINT;
        };
    }
}
