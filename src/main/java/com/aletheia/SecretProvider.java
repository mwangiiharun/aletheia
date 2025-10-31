package com.aletheia;

import com.aletheia.exceptions.ProviderException;

/**
 * Aletheia Secret Provider interface.
 *
 * Defines a pluggable contract for resolving secrets from various sources —
 * such as environment variables, files, Vault, AWS Secrets Manager, or GCP Secret Manager.
 *
 * Implementations must be stateless or thread-safe and should fail gracefully
 * when the secret cannot be resolved.
 */
public interface SecretProvider extends AutoCloseable {

    /**
     * Retrieve a secret value for the specified key.
     *
     * @param key the name of the secret (non-null and non-empty)
     * @return the secret value, or {@code null} if not found
     * @throws ProviderException if a provider-specific failure occurs
     */
    String getSecret(String key) throws ProviderException;

    /**
     * Indicates whether this provider is currently active and able to serve secrets.
     *
     * Implementations may use configuration, environment variables,
     * or runtime health checks to determine readiness.
     *
     * @param key optional hint (e.g. namespace or key pattern)
     * @return {@code true} if this provider can be used, otherwise {@code false}
     */
    boolean supports(String key);


    /**
     * Optional lifecycle hook for graceful shutdown.
     *
     * Providers that manage persistent connections or clients
     * (e.g., Vault, AWS, GCP) may override this method to release resources.
     * Default implementation performs no action.
     *
     * This method is invoked automatically by {@link Aletheia#shutdown()}.
     */
    @Override
    default void close() throws Exception {
        // no-op by default
    }
}