package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;
import io.github.mwangiiharun.aletheia.mocks.MockProviders;
import io.github.mwangiiharun.aletheia.providers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CloudProviderIntegrationTests {

    private Path tempFile;

    @BeforeEach
    void setup() throws Exception {
        tempFile = Files.createTempFile("aletheia-cloud", ".json");
        String json = "{ \"FILE_ONLY\": \"from-file\", \"COMMON_KEY\": \"from-file\" }";
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write(json);
        }

        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.cache.ttl.seconds", "15");
        System.setProperty("aletheia.providers", "GCP,AWS,VAULT,FILE,ENV");

        Aletheia.reinitializeProviders();

        Field providersField = Aletheia.class.getDeclaredField("providers");
        providersField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SecretProvider> currentProviders = (List<SecretProvider>) providersField.get(null);
        currentProviders.clear();

        // 🧩 Order chosen so FileProvider acts as fallback
        currentProviders.addAll(List.of(
                new MockProviders.MockVaultProvider(),
                new MockProviders.MockAwsProvider(),
                new MockProviders.MockGcpProvider(),
                new FileProvider(),
                new EnvProvider()
        ));
    }

    static class MultiSourceConfig {
        @io.github.mwangiiharun.aletheia.annotations.Secret("GCP_ONLY") String fromGcp;
        @io.github.mwangiiharun.aletheia.annotations.Secret("AWS_ONLY") String fromAws;
        @io.github.mwangiiharun.aletheia.annotations.Secret("VAULT_ONLY") String fromVault;
        @io.github.mwangiiharun.aletheia.annotations.Secret("FILE_ONLY") String fromFile;
        @io.github.mwangiiharun.aletheia.annotations.Secret("COMMON_KEY") String common;
    }

    @Test
    void shouldInjectFromDifferentProviders() throws Exception {
        MultiSourceConfig cfg = new MultiSourceConfig();
        Aletheia.injectSecrets(cfg);

        assertEquals("from-gcp", cfg.fromGcp);
        assertEquals("from-aws", cfg.fromAws);
        assertEquals("from-vault", cfg.fromVault);
        assertEquals("from-file", cfg.fromFile);
    }

    @Test
    void shouldResolveFromEachLayer() throws Exception {
        assertEquals("from-gcp", Aletheia.getSecret("GCP_ONLY"));
        assertEquals("from-aws", Aletheia.getSecret("AWS_ONLY"));
        assertEquals("from-vault", Aletheia.getSecret("VAULT_ONLY"));
        assertEquals("from-file", Aletheia.getSecret("FILE_ONLY"));
    }

    @Test
    void shouldFallbackToFileWhenOthersReturnNull() throws Exception {
        String value = Aletheia.getSecret("COMMON_KEY");
        assertEquals("from-file", value);
    }

    @Test
    void shouldRespectCacheOnRepeatedLookups() throws Exception {
        String first = Aletheia.getSecret("GCP_ONLY");
        String second = Aletheia.getSecret("GCP_ONLY");
        assertSame(first, second);
    }

    @Test
    void shouldThrowForMissingRequiredSecret() {
        assertThrows(AletheiaException.class, () -> Aletheia.getSecret("NON_EXISTENT_KEY"));
    }
}