package com.aletheia;

import com.aletheia.annotations.Secret;
import com.aletheia.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for annotation-based secret injection using the Aletheia core.
 * This version is deterministic and does not depend on real environment variables.
 */
class AletheiaInjectionTests {

    private Path tempSecretsFile;

    @BeforeEach
    void setup() throws Exception {
        // Create a temporary secrets.json for predictable results
        tempSecretsFile = Files.createTempFile("aletheia-inject", ".json");
        String json = "{ \"APP_ENV\": \"test\", \"FILE_SECRET\": \"from-file\" }";
        try (FileWriter fw = new FileWriter(tempSecretsFile.toFile())) {
            fw.write(json);
        }

        // Point Aletheia to use this file
        System.setProperty("file.path", tempSecretsFile.toString());
        System.setProperty("aletheia.providers", "VAULT,FILE,ENV");
        System.setProperty("aletheia.cache.ttl.seconds", "10");

        Aletheia.reinitializeProviders();
    }

    static class ConfigWithFile {
        @Secret("FILE_SECRET")
        private String secret;
    }

    static class ConfigWithDefault {
        @Secret(value = "NON_EXISTENT_KEY", required = false, defaultValue = "fallback")
        private String secret;
    }

    static class ConfigMissingSecret {
        @Secret("NON_EXISTENT_REQUIRED_KEY")
        private String missingSecret;
    }

    @Test
    void shouldInjectSecretFromFile() throws AletheiaException {
        ConfigWithFile config = new ConfigWithFile();
        Aletheia.injectSecrets(config);
        assertEquals("from-file", config.secret, "Secret should be injected from FileProvider");
    }

    @Test
    void shouldInjectDefaultValueIfNotFound() throws AletheiaException {
        ConfigWithDefault config = new ConfigWithDefault();
        Aletheia.injectSecrets(config);
        assertEquals("fallback", config.secret);
    }

    @Test
    void shouldThrowExceptionForMissingRequiredSecret() {
        ConfigMissingSecret config = new ConfigMissingSecret();
        assertThrows(SecretNotFoundException.class, () -> Aletheia.injectSecrets(config));
    }

    @Test
    void shouldRespectCacheOnRepeatedInjection() throws Exception {
        ConfigWithFile first = new ConfigWithFile();
        Aletheia.injectSecrets(first);
        Files.deleteIfExists(tempSecretsFile); // simulate file removal
        ConfigWithFile second = new ConfigWithFile();
        Aletheia.injectSecrets(second);
        assertEquals(first.secret, second.secret, "CachedProvider should return cached value");
    }
}