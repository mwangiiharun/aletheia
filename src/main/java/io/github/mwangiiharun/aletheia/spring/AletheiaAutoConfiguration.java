package io.github.mwangiiharun.aletheia.spring;

import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for Aletheia.
 *
 * Registers the secret injector that automatically applies
 * @Secret annotations to Spring-managed beans.
 */
@Configuration
public class AletheiaAutoConfiguration {

    /**
     * Ensures that Aletheia providers are initialized once when
     * the Spring context starts. Since Aletheia is a static utility,
     * there is no need to expose it as a Spring bean.
     */
    @Bean
    public BeanPostProcessor aletheiaSecretInjector() throws AletheiaException {
        // Initialize static provider context only once
        Aletheia.reinitializeProviders();
        return new AletheiaSecretInjector();
    }
}