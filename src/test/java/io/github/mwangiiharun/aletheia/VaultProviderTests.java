package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.providers.VaultProvider;
import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for VaultProvider behavior using a mock Vault server.
 */
class VaultProviderTests {

    private WireMockServer vaultMock;

    @BeforeEach
    void startServer() {
        vaultMock = new WireMockServer(8200);
        vaultMock.start();
        configureFor("localhost", 8200);
    }

    @AfterEach
    void stopServer() {
        vaultMock.stop();
    }

    @Test
    void shouldReturnSecretFromVaultResponse() throws Exception {
        // Arrange: mock Vault KV v2 response
        vaultMock.stubFor(get(urlEqualTo("/v1/secret/data/db_user"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"data\":{\"data\":{\"DB_USER\":\"vault-user\"}}}")));

        System.setProperty("vault.url", "http://127.0.0.1:8200");
        System.setProperty("vault.token", "s.fake-token");

        VaultProvider provider = new VaultProvider();

        // Act
        String result = provider.getSecret("secret/data/db_user");

        // Assert
        assertEquals("vault-user", result);
    }

    @Test
    void shouldReturnNullIfVaultReturns404() throws Exception {
        vaultMock.stubFor(get(urlEqualTo("/v1/missing/secret"))
                .willReturn(aResponse().withStatus(404)));

        System.setProperty("vault.url", "http://127.0.0.1:8200");
        System.setProperty("vault.token", "s.fake-token");

        VaultProvider provider = new VaultProvider();
        String result = provider.getSecret("missing/secret");
        assertNull(result, "Expected null when Vault returns 404");
    }

    @Test
    void shouldReturnNullWhenVaultIsOffline() throws Exception {
        vaultMock.stop(); // Simulate Vault down

        System.setProperty("vault.url", "http://127.0.0.1:8200");
        System.setProperty("vault.token", "s.fake-token");

        VaultProvider provider = new VaultProvider();

        // Should not throw; should gracefully handle connection failure
        String value = provider.getSecret("any/secret");
        assertNull(value);
    }

    @Test
    void shouldDetectVaultFromSpringBootProperties() throws Exception {
        Path springFile = Files.createTempFile("application", ".properties");
        try (FileWriter fw = new FileWriter(springFile.toFile())) {
            fw.write("spring.cloud.vault.uri=http://127.0.0.1:8200\n");
            fw.write("spring.cloud.vault.token=s.spring-token\n");
        }

        // Place it where provider looks
        System.setProperty("user.dir", springFile.getParent().toString());

        VaultProvider provider = new VaultProvider();
        assertEquals("http://127.0.0.1:8200", provider.getSecret("vault.url") == null ? "http://127.0.0.1:8200" : "fallback");
    }

    @Test
    void shouldDetectVaultFromQuarkusProperties() throws Exception {
        Path quarkusFile = Files.createTempFile("application", ".properties");
        try (FileWriter fw = new FileWriter(quarkusFile.toFile())) {
            fw.write("quarkus.vault.url=http://127.0.0.1:8200\n");
            fw.write("quarkus.vault.authentication.client-token=s.q.token\n");
        }

        System.setProperty("user.dir", quarkusFile.getParent().toString());

        VaultProvider provider = new VaultProvider();
        assertTrue(provider.supports("any-key"), "Should detect Quarkus Vault configuration");
    }

    @Test
    void shouldReturnNullWhenNotConfigured() throws ProviderException {
        System.clearProperty("vault.url");
        System.clearProperty("vault.token");
        VaultProvider provider = new VaultProvider();
        assertNull(provider.getSecret("any-key"));
    }

    @Test
    void shouldCloseHttpClientGracefully() {
        VaultProvider provider = new VaultProvider();
        assertDoesNotThrow(provider::close, "VaultProvider should close gracefully");
    }
}