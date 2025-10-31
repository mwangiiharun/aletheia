package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.annotations.Secret;
import io.github.mwangiiharun.aletheia.config.AletheiaConfig;
import io.github.mwangiiharun.aletheia.exceptions.*;
import io.github.mwangiiharun.aletheia.providers.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core entry point for Aletheia secret resolution and injection.
 *
 * Thread-safe, framework-agnostic secret manager with pluggable providers.
 * Supports circular-reference detection and reflection-based field injection.
 */
public final class Aletheia {

    private static final Logger log = LoggerFactory.getLogger(Aletheia.class);

    /** Thread-safe provider chain */
    private static final List<SecretProvider> providers = new CopyOnWriteArrayList<>();

    /** Circular lookup guard per-thread */
    private static final ThreadLocal<Set<String>> activeLookups = ThreadLocal.withInitial(HashSet::new);

    static {
        try {
            initializeProviders();
        } catch (AletheiaException e) {
            throw new AletheiaInitializationException("Failed to initialize Aletheia providers", e);
        }
    }

    private Aletheia() {
        // utility class
    }

    // ---------------------------------------------------------------------
    //  Provider Initialization
    // ---------------------------------------------------------------------

    private static void initializeProviders() throws AletheiaException {
        try {
            List<String> order = Optional.ofNullable(AletheiaConfig.getProviderOrder())
                    .filter(list -> !list.isEmpty())
                    .orElse(List.of("ENV"));

            long ttlSeconds = parseTtl(AletheiaConfig.get("aletheia.cache.ttl.seconds", "3600"));

            for (String type : order) {
                if (type == null || type.isBlank()) continue;

                SecretProvider baseProvider = switch (type.trim().toUpperCase(Locale.ROOT)) {
                    case "VAULT" -> new VaultProvider();
                    case "AWS"   -> new AwsSecretsProvider();
                    case "GCP"   -> new GcpSecretsProvider();
                    case "FILE"  -> new FileProvider();
                    case "ENV"   -> new EnvProvider();
                    default -> {
                        log.warn("Unknown provider type '{}'; defaulting to EnvProvider", type);
                        yield new EnvProvider();
                    }
                };

                providers.add(new CachedProvider(baseProvider, ttlSeconds));
            }

            if (providers.isEmpty()) {
                providers.add(new CachedProvider(new EnvProvider(), ttlSeconds));
            }

        } catch (Exception e) {
            throw new ProviderException("Initialization", e);
        }
    }

    private static long parseTtl(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            log.warn("Invalid TTL '{}'; defaulting to 3600s", raw);
            return 3600L;
        }
    }

    /** Force re-initialization — mainly for tests */
    public static synchronized void reinitializeProviders() throws AletheiaException {
        providers.clear();

        // reset static caches (FileProvider etc.)
        try {
            var field = FileProvider.class.getDeclaredField("cache");
            field.setAccessible(true);
            field.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Failed to reset FileProvider cache: {}", e.getMessage());
        }

        initializeProviders();
    }

    // ---------------------------------------------------------------------
    //  Secret Retrieval
    // ---------------------------------------------------------------------

    public static String getSecret(String key) throws AletheiaException {
        if (key == null || key.isBlank()) {
            throw new InvalidSecretKeyException("Secret key cannot be null or blank");
        }

        Set<String> lookups = activeLookups.get();
        if (lookups.contains(key)) {
            throw new CircularSecretReferenceException("Circular reference detected for key: " + key);
        }

        lookups.add(key);
        try {
            for (SecretProvider provider : providers) {
                try {
                    String value = provider.getSecret(key);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                } catch (ProviderException e) {
                    log.debug("{} error: {}", provider.getClass().getSimpleName(), e.getMessage());
                } catch (Exception e) {
                    throw new ProviderException(provider.getClass().getSimpleName(), e);
                }
            }
        } finally {
            lookups.remove(key);
        }

        throw new SecretNotFoundException(key);
    }

    // ---------------------------------------------------------------------
    //  Injection Support
    // ---------------------------------------------------------------------

    public static void injectSecrets(Object target) throws AletheiaException {
        if (target == null) {
            throw new AletheiaException("Target object cannot be null for injection");
        }

        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                Secret annotation = field.getAnnotation(Secret.class);
                if (annotation == null) continue;

                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                        java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    log.debug("Skipping static/final field: {}", field.getName());
                    continue;
                }

                String value = resolveSecretValue(annotation);
                try {
                    field.setAccessible(true);
                    field.set(target, value);
                } catch (IllegalAccessException e) {
                    throw new SecretInjectionException(field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static String resolveSecretValue(Secret annotation) throws AletheiaException {
        try {
            String value = getSecret(annotation.value());
            return (value != null && !value.isEmpty()) ? value : annotation.defaultValue();
        } catch (SecretNotFoundException e) {
            if (annotation.required() && annotation.defaultValue().isEmpty()) throw e;
            return annotation.defaultValue();
        }
    }

    // ---------------------------------------------------------------------
    //  Utilities
    // ---------------------------------------------------------------------

    public static List<SecretProvider> getProviders() {
        return List.copyOf(providers);
    }

    static synchronized void setProvidersForTesting(List<SecretProvider> newProviders) {
        providers.clear();
        if (newProviders != null) {
            providers.addAll(newProviders);
        }
    }

    public static void shutdown() {
        for (SecretProvider provider : providers) {
            try {
                provider.close();
            } catch (Exception e) {
                log.warn("Failed to close provider {}: {}", provider.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}