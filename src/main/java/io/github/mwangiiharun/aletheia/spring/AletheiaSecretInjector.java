package io.github.mwangiiharun.aletheia.spring;

import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Post-processor that automatically injects secrets
 * into beans annotated with @Secret fields.
 */
public class AletheiaSecretInjector implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        try {
            Aletheia.injectSecrets(bean);
        } catch (AletheiaException e) {
            throw new RuntimeException("Failed to inject secrets into bean: " + beanName, e);
        }
        return bean;
    }
}