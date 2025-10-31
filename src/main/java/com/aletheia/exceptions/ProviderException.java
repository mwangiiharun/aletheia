package com.aletheia.exceptions;

/**
 * Thrown when a specific provider (Vault, AWS, etc.) encounters an error
 * fetching a secret or initializing its client.
 */
public class ProviderException extends AletheiaException {

    public ProviderException(String providerName, Throwable cause) {
        super("Provider failure: " + providerName, cause);
    }
}