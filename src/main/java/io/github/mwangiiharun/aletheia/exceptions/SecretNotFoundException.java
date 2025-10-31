package io.github.mwangiiharun.aletheia.exceptions;

/**
 * Thrown when a required secret could not be resolved by any provider.
 */
public class SecretNotFoundException extends AletheiaException {

    public SecretNotFoundException(String key) {
        super("Missing required secret: " + key);
    }
}