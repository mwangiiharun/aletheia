package com.aletheia;

import com.aletheia.annotations.Secret;
import com.aletheia.exceptions.AletheiaException;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for provider ordering and fallback scenarios.
 * Tests all possible provider combinations and orders.
 */
class ProviderOrderIntegrationTests {

    private Path tempFile;
    private Path vaultProps;

    @BeforeEach
    void setup() throws Exception {
        tempFile = Files.createTempFile("secrets", ".json");
        vaultProps = Files.createTempFile("application", ".properties");
        cleanup();
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(vaultProps);
        System.clearProperty("aletheia.providers");
        System.clearProperty("file.path");
        System.clearProperty("vault.url");
        System.clearProperty("vault.token");
        System.clearProperty("AWS_REGION");
        System.clearProperty("GCP_PROJECT_ID");
        System.clearProperty("TEST_ENV_SECRET");
        System.clearProperty("user.dir");
    }

    @Test
    void shouldResolveFromEnvOnly() throws AletheiaException {
        System.setProperty("TEST_SECRET", "from-env");
        System.setProperty("aletheia.providers", "ENV");
        Aletheia.reinitializeProviders();

        String result = Aletheia.getSecret("TEST_SECRET");
        assertEquals("from-env", result, "Should resolve from ENV provider");
        
        System.clearProperty("TEST_SECRET");
    }

    @Test
    void shouldResolveFromFileOnly() throws Exception {
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"FILE_SECRET\": \"from-file\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "FILE");
        Aletheia.reinitializeProviders();

        String result = Aletheia.getSecret("FILE_SECRET");
        assertEquals("from-file", result, "Should resolve from FILE provider");
    }

    @Test
    void shouldFallbackEnvToFile() throws Exception {
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"FALLBACK_SECRET\": \"from-file\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "ENV,FILE");
        Aletheia.reinitializeProviders();

        // ENV doesn't have this secret, should fallback to FILE
        String result = Aletheia.getSecret("FALLBACK_SECRET");
        assertEquals("from-file", result, "Should fallback from ENV to FILE");
    }

    @Test
    void shouldFallbackFileToEnv() throws AletheiaException {
        System.setProperty("ENV_ONLY_SECRET", "from-env");
        System.setProperty("aletheia.providers", "FILE,ENV");
        Aletheia.reinitializeProviders();

        // FILE provider won't have this (no file configured), should fallback to ENV
        String result = Aletheia.getSecret("ENV_ONLY_SECRET");
        assertEquals("from-env", result, "Should fallback from FILE to ENV");
        
        System.clearProperty("ENV_ONLY_SECRET");
    }

    @Test
    void shouldResolveInPriorityOrderEnvFile() throws Exception {
        // Both providers have the secret, ENV should win
        System.setProperty("PRIORITY_SECRET", "from-env");
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"PRIORITY_SECRET\": \"from-file\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "ENV,FILE");
        Aletheia.reinitializeProviders();

        String result = Aletheia.getSecret("PRIORITY_SECRET");
        assertEquals("from-env", result, "ENV should take priority over FILE");
        
        System.clearProperty("PRIORITY_SECRET");
    }

    @Test
    void shouldResolveInPriorityOrderFileEnv() throws Exception {
        // Both providers have the secret, FILE should win
        System.setProperty("PRIORITY_SECRET", "from-env");
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"PRIORITY_SECRET\": \"from-file\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "FILE,ENV");
        Aletheia.reinitializeProviders();

        String result = Aletheia.getSecret("PRIORITY_SECRET");
        assertEquals("from-file", result, "FILE should take priority over ENV");
        
        System.clearProperty("PRIORITY_SECRET");
    }

    @Test
    void shouldHandleThreeProviderChain() throws Exception {
        // Configure Vault (will be inactive but included)
        try (FileWriter fw = new FileWriter(vaultProps.toFile())) {
            fw.write("vault.url=http://localhost:8200\n");
            fw.write("vault.token=test-token\n");
        }
        System.setProperty("user.dir", vaultProps.getParent().toString());

        // Set up FILE and ENV
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"CHAIN_SECRET\": \"from-file\"}");
        }
        System.setProperty("file.path", tempFile.toString());
        System.setProperty("TEST_ENV_SECRET", "from-env");

        System.setProperty("aletheia.providers", "VAULT,FILE,ENV");
        Aletheia.reinitializeProviders();

        // Should resolve from FILE (VAULT inactive, FILE has it)
        String fileResult = Aletheia.getSecret("CHAIN_SECRET");
        assertEquals("from-file", fileResult);

        // Should resolve from ENV (VAULT inactive, FILE doesn't have it)
        String envResult = Aletheia.getSecret("TEST_ENV_SECRET");
        assertEquals("from-env", envResult);
        
        System.clearProperty("TEST_ENV_SECRET");
    }

    @Test
    void shouldInjectSecretsWithMultipleProviders() throws Exception {
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"FILE_SECRET\": \"file-value\", \"COMMON_SECRET\": \"file-value\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("ENV_SECRET", "env-value");
        System.setProperty("aletheia.providers", "ENV,FILE");
        Aletheia.reinitializeProviders();

        class MultiConfig {
            @Secret("FILE_SECRET") String fromFile;
            @Secret("ENV_SECRET") String fromEnv;
            @Secret("COMMON_SECRET") String common; // Should come from ENV (first in chain)
        }

        MultiConfig config = new MultiConfig();
        Aletheia.injectSecrets(config);
        
        assertEquals("file-value", config.fromFile);
        assertEquals("env-value", config.fromEnv);
        // Common secret: ENV doesn't have it, so FILE should provide it
        // But wait, ENV is checked first. If ENV doesn't have it, FILE will provide it
        assertEquals("file-value", config.common);
        
        System.clearProperty("ENV_SECRET");
    }

    @Test
    void shouldThrowWhenSecretNotFoundInAnyProvider() throws AletheiaException {
        System.setProperty("aletheia.providers", "ENV,FILE");
        Aletheia.reinitializeProviders();

        assertThrows(AletheiaException.class, () -> Aletheia.getSecret("NON_EXISTENT_SECRET"),
                "Should throw exception when secret not found in any provider");
    }

    @Test
    void shouldResolveWithAllProvidersConfigured() throws Exception {
        // Configure all providers (most will be inactive, but should still work)
        System.setProperty("AWS_REGION", "us-east-1");
        System.setProperty("AWS_ACCESS_KEY_ID", "test-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret");
        
        System.setProperty("GCP_PROJECT_ID", "test-project");
        
        try (FileWriter fw = new FileWriter(vaultProps.toFile())) {
            fw.write("vault.url=http://localhost:8200\n");
            fw.write("vault.token=test-token\n");
        }
        System.setProperty("user.dir", vaultProps.getParent().toString());

        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"ALL_PROVIDERS_SECRET\": \"from-file\"}");
        }
        System.setProperty("file.path", tempFile.toString());
        System.setProperty("TEST_ENV_SECRET", "from-env");

        System.setProperty("aletheia.providers", "GCP,AWS,VAULT,FILE,ENV");
        Aletheia.reinitializeProviders();

        // Should resolve from FILE (cloud providers inactive)
        String fileResult = Aletheia.getSecret("ALL_PROVIDERS_SECRET");
        assertEquals("from-file", fileResult);

        // Should resolve from ENV
        String envResult = Aletheia.getSecret("TEST_ENV_SECRET");
        assertEquals("from-env", envResult);
        
        System.clearProperty("TEST_ENV_SECRET");
    }

    @Test
    void shouldHandleEmptyProviderList() throws AletheiaException {
        System.setProperty("aletheia.providers", "");
        Aletheia.reinitializeProviders();

        // Should default to ENV only
        System.setProperty("DEFAULT_SECRET", "from-env");
        String result = Aletheia.getSecret("DEFAULT_SECRET");
        assertEquals("from-env", result);
        
        System.clearProperty("DEFAULT_SECRET");
    }
}

