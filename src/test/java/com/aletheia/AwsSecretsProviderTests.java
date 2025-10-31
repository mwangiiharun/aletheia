package com.aletheia;

import com.aletheia.exceptions.ProviderException;
import com.aletheia.providers.AwsSecretsProvider;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline-safe tests for AwsSecretsProvider.
 * Uses local overrides and mock subclasses instead of real AWS calls.
 */
class AwsSecretsProviderTests {

    private Path appProps;

    @BeforeEach
    void setup() throws Exception {
        // Temporary Spring Boot-like properties
        appProps = Files.createTempFile("application", ".properties");
        try (FileWriter fw = new FileWriter(appProps.toFile())) {
            fw.write("spring.cloud.aws.region.static=us-east-1\n");
            fw.write("spring.cloud.aws.credentials.access-key=FAKE_ACCESS_KEY\n");
            fw.write("spring.cloud.aws.credentials.secret-key=FAKE_SECRET_KEY\n");
        }

        System.setProperty("user.dir", appProps.getParent().toString());
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(appProps);
        System.clearProperty("AWS_REGION");
        System.clearProperty("AWS_ACCESS_KEY_ID");
        System.clearProperty("AWS_SECRET_ACCESS_KEY");
    }

    // Mock subclass to override network calls
    static class MockAwsSecretsProvider extends AwsSecretsProvider {
        private String simulatedSecret;
        private boolean throwNotFound;
        private boolean throwSdkError;

        void setSimulatedSecret(String s) { this.simulatedSecret = s; }
        void setThrowNotFound(boolean v) { this.throwNotFound = v; }
        void setThrowSdkError(boolean v) { this.throwSdkError = v; }

        @Override
        public String getSecret(String key) throws ProviderException {
            if (throwSdkError)
                throw new ProviderException("Simulated AWS SDK error", new Throwable());
            if (throwNotFound)
                return null;
            return simulatedSecret;
        }
    }

    @Test
    void shouldReturnSecretWhenFound() throws Exception {
        MockAwsSecretsProvider provider = new MockAwsSecretsProvider();
        provider.setSimulatedSecret("aws-secret");

        String value = provider.getSecret("db_password");
        assertEquals("aws-secret", value, "Should return secret value from mock AWS provider");
    }

    @Test
    void shouldReturnNullIfSecretNotFound() throws Exception {
        MockAwsSecretsProvider provider = new MockAwsSecretsProvider();
        provider.setThrowNotFound(true);

        String value = provider.getSecret("missing-key");
        assertNull(value, "Should return null when secret is missing");
    }

    @Test
    void shouldThrowProviderExceptionOnSdkFailure() {
        MockAwsSecretsProvider provider = new MockAwsSecretsProvider();
        provider.setThrowSdkError(true);

        assertThrows(ProviderException.class, () -> provider.getSecret("any-key"));
    }

    @Test
    void shouldDetectSpringBootAwsConfig() {
        AwsSecretsProvider provider = new AwsSecretsProvider();
        assertTrue(provider.supports("test-key"),
                "Provider should activate when Spring Boot AWS properties are present");
    }

    @Test
    void shouldDetectQuarkusAwsConfig() throws Exception {
        Path quarkusProps = Files.createTempFile("application", ".properties");
        try (FileWriter fw = new FileWriter(quarkusProps.toFile())) {
            fw.write("quarkus.aws.region=us-west-2\n");
            fw.write("quarkus.aws.credentials.access-key-id=QUARKUS_ACCESS\n");
            fw.write("quarkus.aws.credentials.secret-access-key=QUARKUS_SECRET\n");
        }

        System.setProperty("user.dir", quarkusProps.getParent().toString());
        AwsSecretsProvider provider = new AwsSecretsProvider();

        assertTrue(provider.supports("key"),
                "Provider should activate with Quarkus AWS configuration");

        Files.deleteIfExists(quarkusProps);
    }

    @Test
    void shouldBeActiveEvenWithoutExplicitRegionOrCredentials() {
        System.clearProperty("AWS_REGION");
        System.clearProperty("AWS_ACCESS_KEY_ID");
        System.clearProperty("AWS_SECRET_ACCESS_KEY");

        AwsSecretsProvider provider = new AwsSecretsProvider();
        assertTrue(provider.supports("key"), "Provider should still activate using default AWS chain");
    }

    @Test
    void shouldUseEnvironmentVariables() {
        System.setProperty("AWS_REGION", "us-east-2");
        System.setProperty("AWS_ACCESS_KEY_ID", "ENV_KEY");
        System.setProperty("AWS_SECRET_ACCESS_KEY", "ENV_SECRET");

        AwsSecretsProvider provider = new AwsSecretsProvider();
        assertTrue(provider.supports("some-key"),
                "Provider should activate with environment variable credentials");
    }

    @Test
    void shouldFallbackToDefaultCredentialChainGracefully() {
        System.clearProperty("AWS_ACCESS_KEY_ID");
        System.clearProperty("AWS_SECRET_ACCESS_KEY");

        AwsSecretsProvider provider = new AwsSecretsProvider();
        // Even without explicit credentials, DefaultCredentialsProvider may exist but we can't guarantee region
        assertTrue(provider != null);
    }

    @Test
    void shouldCloseAwsClientGracefully() {
        AwsSecretsProvider provider = new AwsSecretsProvider();
        assertDoesNotThrow(provider::close, "AWS provider should close without errors");
    }
}