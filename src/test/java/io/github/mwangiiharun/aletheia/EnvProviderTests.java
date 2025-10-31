package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.providers.EnvProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvProviderTests {

    private final EnvProvider provider = new EnvProvider();

    @Test
    void shouldFetchExistingEnvironmentVariableIfPresent() {
        String javaHome = provider.getSecret("JAVA_HOME");
        // If JAVA_HOME not found, fallback for IDE runs
        if (javaHome == null) {
            System.out.println("JAVA_HOME not defined in this environment — skipping check.");
            return;
        }
        assertFalse(javaHome.isEmpty());
    }

    @Test
    void shouldFetchSystemPropertyIfEnvMissing() {
        System.setProperty("CUSTOM_KEY", "secret-value");
        String value = provider.getSecret("CUSTOM_KEY");
        assertEquals("secret-value", value);
    }

    @Test
    void shouldReturnNullForMissingKeys() {
        String result = provider.getSecret("SOME_UNKNOWN_ENV_KEY");
        assertNull(result);
    }
}