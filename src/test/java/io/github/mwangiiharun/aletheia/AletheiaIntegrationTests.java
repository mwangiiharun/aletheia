package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.annotations.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AletheiaIntegrationTests {

    private Path secretsFile;

    @BeforeEach
    void setup() throws Exception {
        secretsFile = Files.createTempFile("aletheia", ".json");
        String json = "{ \"DB_USER\": \"aletheia\", \"DB_PASS\": \"top-secret\" }";
        try (FileWriter fw = new FileWriter(secretsFile.toFile())) {
            fw.write(json);
        }

        System.setProperty("file.path", secretsFile.toString());
        System.setProperty("aletheia.providers", "FILE,ENV");
        System.setProperty("aletheia.cache.ttl.seconds", "30");

        Aletheia.reinitializeProviders();
    }

    @Test
    void shouldInjectSecretsFromFileAndCache() throws Exception {
        class Config {
            @Secret("DB_USER") String user;
            @Secret("DB_PASS") String pass;
        }

        Config cfg = new Config();
        Aletheia.injectSecrets(cfg);

        assertEquals("aletheia", cfg.user);
        assertEquals("top-secret", cfg.pass);

        // Second run should hit cache, not re-read file
        Config cfg2 = new Config();
        Aletheia.injectSecrets(cfg2);
        assertEquals(cfg.user, cfg2.user);
    }
}