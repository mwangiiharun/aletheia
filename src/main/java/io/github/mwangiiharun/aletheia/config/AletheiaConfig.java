package io.github.mwangiiharun.aletheia.config;

import io.github.mwangiiharun.aletheia.exceptions.ConfigLoadException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public final class AletheiaConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = AletheiaConfig.class.getResourceAsStream("/aletheia.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("⚠️  aletheia.properties not found in classpath — using defaults.");
            }
        } catch (IOException e) {
            throw new RuntimeException(new ConfigLoadException("Failed to load aletheia.properties", e));
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error initializing Aletheia configuration", e);
        }
    }

    private AletheiaConfig() {}

    /** Retrieve a configuration property value. Checks system properties first, then properties file. */
    public static String get(String key) {
        String value = System.getProperty(key);
        return (value != null) ? value : props.getProperty(key);
    }

    /** ✅ Retrieve a property with a default fallback value. Checks system properties first, then properties file. */
    public static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        return (value != null) ? value : props.getProperty(key, defaultValue);
    }

    /** Returns provider order list; defaults to ["ENV"] if not set. */
    public static List<String> getProviderOrder() {
        // Check system property first (for tests), then fallback to properties file
        String order = System.getProperty("aletheia.providers");
        if (order == null || order.isBlank()) {
            order = props.getProperty("aletheia.providers", "ENV");
        }
        return Arrays.stream(order.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}