package com.aletheia;

import com.aletheia.annotations.Secret;
import com.aletheia.spring.AletheiaAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Aletheia auto-configures under Spring Boot,
 * automatically injecting @Secret values into Spring-managed beans.
 */
@SpringBootTest(classes = {
        AletheiaAutoConfiguration.class,
        AletheiaSpringIntegrationTest.TestConfig.class
})
class AletheiaSpringIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Configuration
    static class TestConfig {

        @Bean
        TestBean testBean() {
            return new TestBean();
        }

        // Mock provider initialization
        static {
            try {
                // Inject mock secrets into Aletheia's file provider or env
                System.setProperty("DB_PASSWORD", "super-secret");
                System.setProperty("API_KEY", "test-api-key");
                Aletheia.reinitializeProviders();
            } catch (Exception ignored) { }
        }
    }

    @Component
    static class TestBean {
        @Secret("DB_PASSWORD")
        private String dbPassword;

        @Secret(value = "API_KEY", required = false, defaultValue = "fallback-key")
        private String apiKey;

        public String getDbPassword() { return dbPassword; }
        public String getApiKey() { return apiKey; }
    }

    @Test
    void shouldInjectSecretsIntoSpringBean() {
        TestBean bean = applicationContext.getBean(TestBean.class);
        assertNotNull(bean, "TestBean should be initialized");
        assertEquals("super-secret", bean.getDbPassword());
        assertEquals("test-api-key", bean.getApiKey());
    }

    @Test
    void shouldReinitializeProvidersSuccessfully() {
        assertDoesNotThrow(Aletheia::reinitializeProviders);
    }
}