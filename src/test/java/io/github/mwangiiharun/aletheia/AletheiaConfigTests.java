package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.config.AletheiaConfig;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AletheiaConfigTests {

    @Test
    void shouldLoadDefaultProviderOrder() {
        List<String> order = AletheiaConfig.getProviderOrder();
        assertNotNull(order);
        assertTrue(order.contains("ENV"), "ENV should be default provider");
    }

    @Test
    void shouldReturnDefaultIfMissingKey() {
        String value = AletheiaConfig.get("missing.key", "default-value");
        assertEquals("default-value", value);
    }

    @Test
    void shouldReturnNullForUnknownKey() {
        assertNull(AletheiaConfig.get("nonexistent.key"));
    }
}