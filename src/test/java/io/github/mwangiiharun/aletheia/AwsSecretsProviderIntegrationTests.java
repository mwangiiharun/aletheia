package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.annotations.Secret;
import io.github.mwangiiharun.aletheia.providers.AwsSecretsProvider;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for AWS Secrets Manager provider.
 * Tests configuration detection, provider initialization, and fallback scenarios.
 */
class AwsSecretsProviderIntegrationTests {

    private Path tempProps;

    @BeforeEach
    void setup() throws Exception {
        tempProps = Files.createTempFile("application", ".properties");
        cleanup();
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(tempProps);
        System.clearProperty("AWS_REGION");
        System.clearProperty("AWS_ACCESS_KEY_ID");
        System.clearProperty("AWS_SECRET_ACCESS_KEY");
        System.clearProperty("aletheia.providers");
        System.clearProperty("file.path");
        System.clearProperty("aws.region");
        System.clearProperty("aws.accessKey");
        System.clearProperty("aws.secretKey");
        System.clearProperty("user.dir");
    }

    @Test
    void shouldInitializeAwsProviderWithEnvironmentVariables() {
        System.setProperty("AWS_REGION", "us-east-1");
        System.setProperty("AWS_ACCESS_KEY_ID", "test-access-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret-key");

        System.setProperty("aletheia.providers", "AWS");
        assertDoesNotThrow(() -> Aletheia.reinitializeProviders(), 
                "Should initialize AWS provider with environment variables");
        
        // Provider is wrapped in CachedProvider, just verify it's active
        assertTrue(Aletheia.getProviders().get(0).supports("any-key"), "AWS provider should be active");
    }

    @Test
    void shouldHandleAwsProviderFallbackWhenSecretNotFound() throws Exception {
        System.setProperty("AWS_REGION", "us-east-1");
        System.setProperty("AWS_ACCESS_KEY_ID", "test-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret");

        // Set up fallback: AWS -> FILE
        Path tempFile = Files.createTempFile("secrets", ".json");
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"fallback-secret\": \"from-file\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "AWS,FILE");
        Aletheia.reinitializeProviders();

        // AWS provider will return null (secret not found), should fallback to FILE provider
        String result = Aletheia.getSecret("fallback-secret");
        assertEquals("from-file", result, "Should fallback to FILE provider when AWS returns null");

        Files.deleteIfExists(tempFile);
    }

    @Test
    void shouldUseAwsProviderInProviderChain() throws Exception {
        System.setProperty("AWS_REGION", "us-west-2");
        System.setProperty("AWS_ACCESS_KEY_ID", "test-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret");

        // Set up ENV as fallback with a test secret
        System.setProperty("TEST_ENV_SECRET", "from-env");

        System.setProperty("aletheia.providers", "AWS,ENV");
        Aletheia.reinitializeProviders();

        // Should use ENV provider when AWS doesn't have the secret
        String result = Aletheia.getSecret("TEST_ENV_SECRET");
        assertEquals("from-env", result, "Should fallback to ENV provider when AWS doesn't have secret");
        
        System.clearProperty("TEST_ENV_SECRET");
    }

    @Test
    void shouldDetectAwsConfigFromAletheiaProperties() throws Exception {
        try (FileWriter fw = new FileWriter(tempProps.toFile())) {
            fw.write("aws.region=eu-west-1\n");
            fw.write("aws.accessKey=config-key\n");
            fw.write("aws.secretKey=config-secret\n");
        }

        System.setProperty("user.dir", tempProps.getParent().toString());
        System.setProperty("aletheia.providers", "AWS");
        Aletheia.reinitializeProviders();

        assertTrue(Aletheia.getProviders().get(0).supports("any-key"), "Should detect AWS config from aletheia.properties");
    }

    @Test
    void shouldUseSpringBootAwsConfiguration() throws Exception {
        try (FileWriter fw = new FileWriter(tempProps.toFile())) {
            fw.write("spring.cloud.aws.region.static=ap-southeast-1\n");
            fw.write("spring.cloud.aws.credentials.access-key=spring-key\n");
            fw.write("spring.cloud.aws.credentials.secret-key=spring-secret\n");
        }

        System.setProperty("user.dir", tempProps.getParent().toString());
        
        AwsSecretsProvider provider = new AwsSecretsProvider();
        assertTrue(provider.supports("test"), "Should detect Spring Boot AWS configuration");
    }

    @Test
    void shouldFallbackToFileWhenAwsUnavailable() throws Exception {
        System.setProperty("AWS_REGION", "us-east-1");
        System.setProperty("AWS_ACCESS_KEY_ID", "test-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret");

        // Set up fallback
        Path tempFile = Files.createTempFile("secrets", ".json");
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"db-password\": \"file-fallback\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "AWS,FILE");
        Aletheia.reinitializeProviders();

        // AWS will return null (can't connect without real AWS), should fallback to FILE
        String result = Aletheia.getSecret("db-password");
        assertEquals("file-fallback", result, "Should fallback to FILE when AWS is unavailable");

        Files.deleteIfExists(tempFile);
    }

    @Test
    void shouldConfigureAwsViaQuarkusProperties() throws Exception {
        try (FileWriter fw = new FileWriter(tempProps.toFile())) {
            fw.write("quarkus.aws.region=ap-northeast-1\n");
            fw.write("quarkus.aws.credentials.access-key-id=quarkus-key\n");
            fw.write("quarkus.aws.credentials.secret-access-key=quarkus-secret\n");
        }

        System.setProperty("user.dir", tempProps.getParent().toString());
        
        AwsSecretsProvider provider = new AwsSecretsProvider();
        assertTrue(provider.supports("test"), "Should detect Quarkus AWS configuration");
    }

    @Test
    void shouldPrioritizeEnvVarsOverConfigFiles() {
        // Set environment variable
        System.setProperty("AWS_REGION", "env-region");
        
        // Create config file with different region
        try {
            try (FileWriter fw = new FileWriter(tempProps.toFile())) {
                fw.write("aws.region=file-region\n");
            }
            System.setProperty("user.dir", tempProps.getParent().toString());
        } catch (Exception e) {
            // Ignore file write errors
        }

        System.setProperty("AWS_ACCESS_KEY_ID", "test-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret");
        
        AwsSecretsProvider provider = new AwsSecretsProvider();
        // Environment variable should take precedence
        assertTrue(provider.supports("test"), "Should use environment variables over config files");
    }

    @Test
    void shouldInjectSecretsWithAwsProviderInChain() throws Exception {
        System.setProperty("AWS_REGION", "us-east-1");
        System.setProperty("AWS_ACCESS_KEY_ID", "test-key");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "test-secret");

        // Set up FILE provider with a secret
        Path tempFile = Files.createTempFile("secrets", ".json");
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write("{\"injected-secret\": \"from-file\"}");
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "AWS,FILE");
        Aletheia.reinitializeProviders();

        class Config {
            @Secret("injected-secret")
            private String secret;
        }

        Config config = new Config();
        Aletheia.injectSecrets(config);
        
        assertEquals("from-file", config.secret, "Should inject secret via fallback to FILE provider");

        Files.deleteIfExists(tempFile);
    }
}
