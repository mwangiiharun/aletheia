package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.providers.CachedProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CachedProviderTests {

    static class DummyProvider implements SecretProvider {
        private int callCount = 0;
        @Override public String getSecret(String key) {
            callCount++;
            return "secret-" + callCount;
        }
        @Override public boolean supports(String key) { return true; }
        int getCallCount() { return callCount; }
    }
    @BeforeAll
    static void enableTestMode() {
        System.setProperty("java.test", "true");
    }

    @Test
    void shouldCacheWithinTtl() throws Exception {
        DummyProvider base = new DummyProvider();
        CachedProvider cached = new CachedProvider(base, 2); // 2 seconds TTL

        String v1 = cached.getSecret("X");
        String v2 = cached.getSecret("X");

        assertEquals(v1, v2);
        assertEquals(1, base.getCallCount(), "Base provider should only be called once");
    }

    @Test
    void shouldExpireAfterTtl() throws Exception {
        DummyProvider base = new DummyProvider();
        CachedProvider cached = new CachedProvider(base, 1); // 1 second TTL

        String v1 = cached.getSecret("X");
        Thread.sleep(1100); // expire
        String v2 = cached.getSecret("X");

        assertNotEquals(v1, v2);
        assertTrue(base.getCallCount() >= 2);
    }
}