package com.aletheia.providers;

import com.aletheia.SecretProvider;
import com.aletheia.config.AletheiaConfig;
import com.aletheia.config.ApplicationPropertiesLoader;
import com.aletheia.exceptions.ProviderException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.secretmanager.v1.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * GcpSecretsProvider
 *
 * Provides secret retrieval via Google Cloud Secret Manager.
 * Automatically detects configuration from:
 *  - Environment variables
 *  - AletheiaConfig
 *  - Spring Boot or Quarkus properties
 *
 * In "test-light" profile, provider will always be inactive.
 */
public class GcpSecretsProvider implements SecretProvider {

    private final String projectId;
    private final Credentials credentials;
    private final boolean active;

    public GcpSecretsProvider() {
        String project = detectProjectId();
        Credentials creds = detectCredentials();

        boolean isTestLight = Optional.ofNullable(System.getProperty("aletheia.profile"))
                .map(p -> p.equalsIgnoreCase("test-light"))
                .orElse(false);

        // Determine if credentials are truly usable
        boolean validCreds = false;
        if (creds != null) {
            if (creds instanceof GoogleCredentials gc) {
                try {
                    gc.refreshIfExpired();
                    validCreds = gc.getAccessToken() != null;
                } catch (IOException ignored) {
                }
            } else {
                validCreds = true;
            }
        }

        this.projectId = project;
        this.credentials = creds;
        this.active = !isTestLight && project != null && !project.isBlank() && validCreds;

        if (!active) {
            System.err.println("⚠️  GcpSecretsProvider inactive: projectId=" + project + ", credsValid=" + validCreds);
        }
    }

    @Override
    public String getSecret(String key) throws ProviderException {
        if (!active) return null;

        try (SecretManagerServiceClient client = createClient()) {
            SecretVersionName name = SecretVersionName.of(projectId, key, "latest");
            AccessSecretVersionResponse response = client.accessSecretVersion(name);
            if (response == null || !response.hasPayload()) return null;
            return response.getPayload().getData().toStringUtf8();

        } catch (NotFoundException e) {
            return null;
        } catch (IOException e) {
            System.err.println("⚠️  GCP Secret Manager I/O error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            throw new ProviderException("GcpSecretsProvider", e);
        }
    }

    private SecretManagerServiceClient createClient() throws IOException {
        SecretManagerServiceSettings settings = SecretManagerServiceSettings
                .newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
        return SecretManagerServiceClient.create(settings);
    }

    @Override
    public boolean supports(String key) {
        return active && key != null && !key.isBlank();
    }

    // ------------------------------------------------------------
    // 🔍 Configuration detection
    // ------------------------------------------------------------

    private String detectProjectId() {
        Properties props = ApplicationPropertiesLoader.load();

        for (String env : new String[]{"GCP_PROJECT_ID", "GOOGLE_CLOUD_PROJECT"}) {
            String val = System.getenv(env);
            if (val != null && !val.isBlank()) return val;
        }

        for (String key : new String[]{
                "spring.cloud.gcp.project-id",
                "spring.cloud.gcp.projectId",
                "quarkus.google.cloud.project-id",
                "quarkus.google.cloud.projectId",
                "gcp.project.id",
                "google.cloud.project-id"
        }) {
            String val = props.getProperty(key);
            if (val != null && !val.isBlank()) return val;
        }

        for (String key : new String[]{"gcp.project.id", "google.cloud.project-id"}) {
            String val = AletheiaConfig.get(key);
            if (val != null && !val.isBlank()) return val;
        }

        return null;
    }

    private Credentials detectCredentials() {
        Properties props = ApplicationPropertiesLoader.load();

        String credPath = Optional.ofNullable(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"))
                .orElseGet(() -> Optional.ofNullable(AletheiaConfig.get("gcp.credentials.file")).orElse(null));

        if (credPath == null)
            credPath = props.getProperty("spring.cloud.gcp.credentials.location",
                    props.getProperty("quarkus.google.cloud.credentials.location"));

        try {
            if (credPath != null) {
                java.io.File f = new java.io.File(credPath);
                if (f.exists()) {
                    return ServiceAccountCredentials.fromStream(new FileInputStream(f));
                }
            }
            return GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            System.err.println("⚠️  Failed to load GCP credentials: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        // no-op
    }
}