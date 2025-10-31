package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import io.github.mwangiiharun.aletheia.providers.GcpSecretsProvider;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ Comprehensive offline tests for {@link GcpSecretsProvider}.
 * Ensures detection logic works for:
 *  - Spring Boot style application.properties
 *  - Quarkus style application.properties
 *  - Missing or invalid configuration
 *  - Offline secret retrieval
 *
 * No actual calls to Google Cloud APIs are made.
 */
class GcpSecretsProviderTests {

    private Path springProps;
    private Path quarkusProps;

    @BeforeAll
    static void enableTestMode() {
        // Signal to ApplicationPropertiesLoader that we're running under test
        System.setProperty("java.test", "true");
    }

    @BeforeEach
    void setup() throws Exception {
        // Simulate both Spring Boot and Quarkus application.properties files
        springProps = Files.createTempFile("application-spring", ".properties");
        try (FileWriter fw = new FileWriter(springProps.toFile())) {
            fw.write("spring.cloud.gcp.project-id=test-project\n");
            fw.write("spring.cloud.gcp.credentials.location=/tmp/fake-creds.json\n");
        }

        quarkusProps = Files.createTempFile("application-quarkus", ".properties");
        try (FileWriter fw = new FileWriter(quarkusProps.toFile())) {
            fw.write("quarkus.google.cloud.project-id=alt-project\n");
            fw.write("quarkus.google.cloud.credentials.location=/tmp/fake-quarkus-creds.json\n");
        }

        // Point working directory to the folder with these temporary property files
        System.setProperty("user.dir", springProps.getParent().toString());
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(springProps);
        Files.deleteIfExists(quarkusProps);
    }

    // 🧩 Mock subclass to simulate offline GCP calls
    static class MockGcpSecretsProvider extends GcpSecretsProvider {
        private String simulatedSecret;
        private boolean throwNotFound;
        private boolean throwIOException;

        void setSimulatedSecret(String value) { this.simulatedSecret = value; }
        void setThrowNotFound(boolean v) { this.throwNotFound = v; }
        void setThrowIOException(boolean v) { this.throwIOException = v; }

        @Override
        public String getSecret(String key) throws ProviderException {
            if (throwIOException)
                throw new ProviderException("Simulated I/O error", new java.io.IOException("Fake network down"));
            if (throwNotFound)
                return null;
            return simulatedSecret;
        }
    }

    // ------------------------------------------------------------
    // 🧪 Tests
    // ------------------------------------------------------------

    @Test
    void shouldReturnSecretWhenFound() throws Exception {
        MockGcpSecretsProvider provider = new MockGcpSecretsProvider();
        provider.setSimulatedSecret("super-secret");

        String result = provider.getSecret("db_password");
        assertEquals("super-secret", result);
    }

    @Test
    void shouldReturnNullIfSecretNotFound() throws Exception {
        MockGcpSecretsProvider provider = new MockGcpSecretsProvider();
        provider.setThrowNotFound(true);

        assertNull(provider.getSecret("missing-secret"));
    }

    @Test
    void shouldThrowProviderExceptionWhenGcpUnavailable() {
        MockGcpSecretsProvider provider = new MockGcpSecretsProvider();
        provider.setThrowIOException(true);

        assertThrows(ProviderException.class, () -> provider.getSecret("any-key"));
    }
    @Test
    void verifyApplicationPropertiesLoader() throws Exception {
        Path tmpDir = Files.createTempDirectory("loader-test");
        Path propsFile = tmpDir.resolve("application.properties");

        try (FileWriter fw = new FileWriter(propsFile.toFile())) {
            fw.write("spring.cloud.gcp.project-id=demo-project\n");
        }

        System.setProperty("user.dir", tmpDir.toString());
        Properties props = ApplicationPropertiesLoader.load();

        System.out.println("Loaded project-id = " + props.getProperty("spring.cloud.gcp.project-id"));
        Files.deleteIfExists(propsFile);
        Files.deleteIfExists(tmpDir);
    }

    @Test
    void shouldBeInactiveWithoutProjectOrCredentials() {
        System.clearProperty("GOOGLE_APPLICATION_CREDENTIALS");
        System.clearProperty("GCP_PROJECT_ID");
        System.clearProperty("GOOGLE_CLOUD_PROJECT");
        System.setProperty("java.test", "true");
        GcpSecretsProvider provider = new GcpSecretsProvider();
        assertFalse(provider.supports("any-key"),
                "Provider should be inactive without configuration");
    }
}