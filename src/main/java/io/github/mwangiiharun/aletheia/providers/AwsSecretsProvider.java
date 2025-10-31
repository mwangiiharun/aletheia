package io.github.mwangiiharun.aletheia.providers;

import io.github.mwangiiharun.aletheia.SecretProvider;
import io.github.mwangiiharun.aletheia.config.AletheiaConfig;
import io.github.mwangiiharun.aletheia.ApplicationPropertiesLoader;
import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.Optional;
import java.util.Properties;

/**
 * Secret provider for AWS Secrets Manager.
 * Detects configuration from environment, Aletheia, Spring Boot, or Quarkus properties.
 * Maintains a single AWS SecretsManagerClient instance with proper shutdown support.
 */
public class AwsSecretsProvider implements SecretProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsProvider.class);

    private final SecretsManagerClient client;
    private final boolean active;

    public AwsSecretsProvider() {
        SecretsManagerClient tempClient = null;
        boolean enabled = false;

        try {
            String region = detectRegion();
            AwsCredentialsProvider creds = detectCredentials();

            if (region != null) {
                tempClient = SecretsManagerClient.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .region(Region.of(region))
                        .credentialsProvider(
                                creds != null ? creds : DefaultCredentialsProvider.create()
                        )
                        .build();

                enabled = true;
                log.debug("AWSSecretsProvider active in region: {}", region);
            } else {
                log.warn("⚠️ AWSSecretsProvider inactive — missing region configuration");
            }
        } catch (Exception e) {
            log.warn("⚠️ AWSSecretsProvider initialization failed: {}", e.getMessage());
        }

        this.client = tempClient;
        this.active = enabled;
    }

    // ------------------------------------------------------------
    // Secret Retrieval
    // ------------------------------------------------------------

    @Override
    public String getSecret(String key) throws ProviderException {
        if (!active || client == null) {
            return null;
        }

        try {
            GetSecretValueResponse response = client.getSecretValue(
                    GetSecretValueRequest.builder().secretId(key).build()
            );

            return (response == null || response.secretString() == null)
                    ? null
                    : response.secretString();

        } catch (ResourceNotFoundException e) {
            return null; // Secret not found → skip gracefully
        } catch (AwsServiceException | SdkClientException e) {
            log.warn("⚠️ AWS error retrieving '{}': {}", key, e.getMessage());
            return null;
        } catch (Exception e) {
            throw new ProviderException("AwsSecretsProvider", e);
        }
    }

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------

    @Override
    public boolean supports(String key) {
        return active;
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
                log.info("Closed AWS SecretsManager client successfully.");
            } catch (Exception e) {
                log.warn("Error closing AWS SecretsManager client: {}", e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------
    // Configuration Resolution
    // ------------------------------------------------------------

    private String detectRegion() {
        Properties props = ApplicationPropertiesLoader.load();

        return Optional.ofNullable(System.getProperty("AWS_REGION"))
                .or(() -> Optional.ofNullable(System.getenv("AWS_REGION")))
                .or(() -> Optional.ofNullable(AletheiaConfig.get("aws.region")))
                .or(() -> Optional.ofNullable(props.getProperty("cloud.aws.region.static")))
                .or(() -> Optional.ofNullable(props.getProperty("spring.cloud.aws.region.static")))
                .or(() -> Optional.ofNullable(props.getProperty("quarkus.aws.region")))
                .orElse(null);
    }

    private AwsCredentialsProvider detectCredentials() {
        Properties props = ApplicationPropertiesLoader.load();

        String accessKey = Optional.ofNullable(System.getenv("AWS_ACCESS_KEY_ID"))
                .orElseGet(() -> System.getProperty("AWS_ACCESS_KEY_ID"));
        String secretKey = Optional.ofNullable(System.getenv("AWS_SECRET_ACCESS_KEY"))
                .orElseGet(() -> System.getProperty("AWS_SECRET_ACCESS_KEY"));

        if (accessKey == null) accessKey = AletheiaConfig.get("aws.accessKey");
        if (secretKey == null) secretKey = AletheiaConfig.get("aws.secretKey");

        // Spring Boot
        if (accessKey == null) accessKey = props.getProperty("spring.cloud.aws.credentials.access-key");
        if (secretKey == null) secretKey = props.getProperty("spring.cloud.aws.credentials.secret-key");

        // Quarkus
        if (accessKey == null) accessKey = props.getProperty("quarkus.aws.credentials.access-key-id");
        if (secretKey == null) secretKey = props.getProperty("quarkus.aws.credentials.secret-access-key");

        if (accessKey != null && secretKey != null) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }

        // fallback to default AWS provider chain
        return DefaultCredentialsProvider.create();
    }
}