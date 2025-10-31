package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.annotations.Secret;
import io.github.mwangiiharun.aletheia.config.AletheiaConfig;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration smoke test for Aletheia.
 * Validates configuration loading and annotation-based secret injection
 * with File and Env providers.
 */
class AletheiaIntegrationSmokeTest {

    private Path tempSecretsFile;

    @BeforeEach
    void setup() throws Exception {
        tempSecretsFile = Files.createTempFile("aletheia-integration", ".json");
        String json = "{ \"APP_HOME\": \"/opt/aletheia\", \"CACHE_TTL\": \"60\" }";
        try (FileWriter fw = new FileWriter(tempSecretsFile.toFile())) {
            fw.write(json);
        }

        System.setProperty("file.path", tempSecretsFile.toString());
        System.setProperty("aletheia.providers", "FILE,ENV");
        System.setProperty("aletheia.cache.ttl.seconds", "10");

        Aletheia.reinitializeProviders();
    }

    static class DemoConfig {
        @Secret(value = "APP_HOME", required = true)
        private String home;

        @Secret(value = "SOME_MISSING_KEY", required = false, defaultValue = "demo-default")
        private String fallback;
    }

    @Test
    void shouldLoadPropertiesAndInjectValues() throws AletheiaException {
        assertNotNull(AletheiaConfig.getProviderOrder(), "Provider order should be loaded");

        DemoConfig config = new DemoConfig();
        Aletheia.injectSecrets(config);

        assertEquals("/opt/aletheia", config.home);
        assertEquals("demo-default", config.fallback);
    }
}