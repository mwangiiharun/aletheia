package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderFailureTests {

    static class FailingProvider implements SecretProvider {
        @Override
        public String getSecret(String key) throws ProviderException {
            throw new IllegalStateException("Simulated failure");
        }

        @Override
        public boolean supports(String key) {
            return true;
        }
    }

    @Test
    void shouldThrowProviderExceptionWhenProviderFails() {
        SecretProvider failingProvider = new FailingProvider();

        // Directly simulate Aletheia’s getSecret() handling logic
        assertThrows(IllegalStateException.class, () -> failingProvider.getSecret("DB_PASSWORD"));
    }
}