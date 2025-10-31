package com.aletheia.providers;

import com.aletheia.SecretProvider;
import com.aletheia.config.AletheiaConfig;
import com.aletheia.config.ApplicationPropertiesLoader;
import com.aletheia.exceptions.ProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Secret provider for HashiCorp Vault.
 * Detects configuration from environment, Aletheia, Spring Boot, or Quarkus properties.
 * Maintains a single HTTP client and supports graceful shutdown.
 */
public class VaultProvider implements SecretProvider {

    private static final Logger log = LoggerFactory.getLogger(VaultProvider.class);

    private final String vaultUrl;
    private final String token;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CloseableHttpClient client;

    public VaultProvider() {
        this.vaultUrl = resolveVaultUrl();
        this.token = resolveVaultToken();

        // Only create an HTTP client when properly configured
        if (isValid(vaultUrl) && isValid(token)) {
            this.client = HttpClients.createDefault();
        } else {
            this.client = null;
        }
    }

    // ------------------------------------------------------------
    // Secret Retrieval
    // ------------------------------------------------------------

    @Override
    public String getSecret(String key) throws ProviderException {
        if (!isValid(vaultUrl) || !isValid(token)) {
            return null; // Not configured → skip gracefully
        }

        if (client == null) {
            log.warn("VaultProvider initialized without HTTP client (no Vault config)");
            return null;
        }

        String requestUrl = vaultUrl.endsWith("/")
                ? vaultUrl + "v1/" + key
                : vaultUrl + "/v1/" + key;

        try {
            HttpGet get = new HttpGet(requestUrl);
            get.addHeader("X-Vault-Token", token);

            var response = client.execute(get);
            int status = response.getCode();

            if (status == 404) return null;
            if (status != 200) {
                log.warn("Vault returned {} for {}", status, key);
                return null;
            }

            String json = EntityUtils.toString(response.getEntity());
            JsonNode root = mapper.readTree(json);
            JsonNode dataNode = root.path("data");
            if (dataNode.has("data")) dataNode = dataNode.path("data");

            JsonNode valueNode = dataNode.path(key);
            if (valueNode.isMissingNode() && dataNode.size() == 1) {
                var fields = dataNode.fieldNames();
                if (fields.hasNext()) {
                    valueNode = dataNode.path(fields.next());
                }
            }

            return valueNode.isMissingNode() ? null : valueNode.asText();

        } catch (HttpHostConnectException e) {
            log.warn("⚠️ Vault unreachable at {}: {}", vaultUrl, e.getMessage());
            return null; // ✅ gracefully handle Vault offline
        } catch (IOException e) {
            if (e.getCause() instanceof java.net.ConnectException) {
                log.warn("⚠️ Vault unreachable ({}): {}", vaultUrl, e.getMessage());
                return null;
            }
            throw new ProviderException("VaultProvider", e);
        } catch (ParseException e) {
            throw new ProviderException("VaultProvider parse error", e);
        }
    }

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------

    @Override
    public boolean supports(String key) {
        return isValid(vaultUrl) && isValid(token);
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
                log.info("Closed VaultProvider HTTP client.");
            } catch (IOException e) {
                log.warn("Error closing VaultProvider HTTP client: {}", e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------
    // Configuration Helpers
    // ------------------------------------------------------------

    private boolean isValid(String s) {
        return s != null && !s.isBlank();
    }

    private String resolveVaultUrl() {
        String url = System.getenv("VAULT_ADDR");
        if (isValid(url)) return url;

        url = AletheiaConfig.get("vault.url");
        if (isValid(url)) return url;

        Properties props = ApplicationPropertiesLoader.load();
        url = props.getProperty("spring.cloud.vault.uri");
        if (isValid(url)) return url;

        url = props.getProperty("quarkus.vault.url");
        if (isValid(url)) return url;

        return null;
    }

    private String resolveVaultToken() {
        String tok = System.getenv("VAULT_TOKEN");
        if (isValid(tok)) return tok;

        tok = AletheiaConfig.get("vault.token");
        if (isValid(tok)) return tok;

        Properties props = ApplicationPropertiesLoader.load();
        tok = props.getProperty("spring.cloud.vault.token");
        if (isValid(tok)) return tok;

        tok = props.getProperty("quarkus.vault.authentication.client-token");
        if (isValid(tok)) return tok;

        return null;
    }
}