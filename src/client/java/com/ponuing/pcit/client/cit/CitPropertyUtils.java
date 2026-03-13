package com.ponuing.pcit.client.cit;

import java.util.Properties;

public final class CitPropertyUtils {
    private CitPropertyUtils() {
    }

    public static String getProperty(Properties properties, String key) {
        if (properties == null || key == null) {
            return null;
        }
        return firstNonBlank(
                properties.getProperty(key),
                properties.getProperty("citresewn:" + key),
                properties.getProperty("citresewn." + key),
                properties.getProperty("optifine:" + key),
                properties.getProperty("optifine." + key),
                properties.getProperty("mcpatcher:" + key),
                properties.getProperty("mcpatcher." + key)
        );
    }

    public static String stripNamespacePrefix(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("citresewn:")) {
            return key.substring("citresewn:".length());
        }
        if (key.startsWith("citresewn.")) {
            return key.substring("citresewn.".length());
        }
        if (key.startsWith("optifine:")) {
            return key.substring("optifine:".length());
        }
        if (key.startsWith("optifine.")) {
            return key.substring("optifine.".length());
        }
        if (key.startsWith("mcpatcher:")) {
            return key.substring("mcpatcher:".length());
        }
        if (key.startsWith("mcpatcher.")) {
            return key.substring("mcpatcher.".length());
        }
        return key;
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
