package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.providers.FileProvider;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileProvider — ensures secrets are read correctly from JSON files,
 * cached in memory, and handle missing files/keys gracefully.
 */
class FileProviderTests {

    private Path tempFile;
    private FileProvider provider;

    @BeforeEach
    void setup() throws Exception {
        // Create a temporary JSON secrets file
        tempFile = Files.createTempFile("aletheia-secrets", ".json");
        String json = "{ \"API_KEY\": \"abc123\", \"DB_PASS\": \"secret\" }";
        try (FileWriter fw = new FileWriter(tempFile.toFile())) {
            fw.write(json);
        }

        // Tell FileProvider where to find it
        System.setProperty("file.path", tempFile.toString());
        provider = new FileProvider();
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(tempFile);

        // Reset static cache between tests using reflection
        var field = FileProvider.class.getDeclaredField("cache");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void shouldFetchSecretFromJsonFile() throws Exception {
        String value = provider.getSecret("API_KEY");
        assertEquals("abc123", value, "FileProvider should return correct value for existing key");
    }

    @Test
    void shouldReturnNullForUnknownKey() throws Exception {
        String value = provider.getSecret("NON_EXISTENT_KEY");
        assertNull(value, "FileProvider should return null for unknown keys");
    }

    @Test
    void shouldCacheFileContentsAfterFirstRead() throws Exception {
        // First call reads from disk
        String firstRead = provider.getSecret("DB_PASS");
        assertEquals("secret", firstRead);

        // Delete file — provider should now use cached values
        Files.deleteIfExists(tempFile);

        String secondRead = provider.getSecret("DB_PASS");
        assertEquals(firstRead, secondRead, "FileProvider should serve cached values after first load");
    }

    @Test
    void shouldGracefullyHandleMissingFile() throws Exception {
        // Point to a non-existent file
        System.setProperty("file.path", "non_existent_file.json");
        var missingFileProvider = new FileProvider();

        String value = missingFileProvider.getSecret("ANY_KEY");
        assertNull(value, "FileProvider should return null if file is missing");
    }
}