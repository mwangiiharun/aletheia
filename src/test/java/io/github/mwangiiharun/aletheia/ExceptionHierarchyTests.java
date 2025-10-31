package io.github.mwangiiharun.aletheia;

import io.github.mwangiiharun.aletheia.exceptions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionHierarchyTests {

    @Test
    void shouldMaintainHierarchy() {
        assertTrue(AletheiaException.class.isAssignableFrom(SecretNotFoundException.class));
        assertTrue(AletheiaException.class.isAssignableFrom(SecretInjectionException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(AletheiaInitializationException.class));
    }

    @Test
    void shouldContainUsefulMessages() {
        SecretNotFoundException ex = new SecretNotFoundException("TEST_KEY");
        assertTrue(ex.getMessage().contains("TEST_KEY"));

        SecretInjectionException inj = new SecretInjectionException("fieldX", new IllegalAccessException());
        assertTrue(inj.getMessage().contains("fieldX"));

        AletheiaInitializationException init =
                new AletheiaInitializationException("Init failed", new ConfigLoadException("config", new Exception()));
        assertTrue(init.getMessage().contains("Init failed"));
    }
}