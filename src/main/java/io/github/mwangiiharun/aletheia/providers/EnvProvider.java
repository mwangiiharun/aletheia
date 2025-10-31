package io.github.mwangiiharun.aletheia.providers;

import io.github.mwangiiharun.aletheia.SecretProvider;

public class EnvProvider implements SecretProvider {

    @Override
    public String getSecret(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(key);
        }
        return value;
    }

    @Override
    public boolean supports(String key) {
        return true;
    }
}