package io.github.mwangiiharun.aletheia.providers;

import io.github.mwangiiharun.aletheia.SecretProvider;
import io.github.mwangiiharun.aletheia.config.AletheiaConfig;
import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Map;

public class FileProvider implements SecretProvider {

    private static Map<String, String> cache;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getSecret(String key) throws ProviderException {
        try {
            if (cache == null) {
                String path = System.getProperty("file.path");
                if (path == null) path = AletheiaConfig.get("file.path");
                if (path == null || path.isBlank()) {
                    // Nothing configured, silently skip
                    return null;
                }

                File file = new File(path);
                if (!file.exists()) {
                    // File not found, don't throw — just return null so fallback providers can continue
                    return null;
                }

                cache = mapper.readValue(file, Map.class);
            }

            return cache.getOrDefault(key, null);
        } catch (Exception e) {
            throw new ProviderException("FileProvider", e);
        }
    }

    @Override
    public boolean supports(String key) {
        return true;
    }
}