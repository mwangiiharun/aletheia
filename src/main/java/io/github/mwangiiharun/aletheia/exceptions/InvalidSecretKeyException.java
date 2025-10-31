package io.github.mwangiiharun.aletheia.exceptions;

/**
 * Thrown when an invalid secret key is requested.
 *
 * This may occur when the provided key is null, empty, or contains
 * illegal characters depending on the provider’s key format.
 */
public class InvalidSecretKeyException extends AletheiaException {

    public InvalidSecretKeyException(String message) {
        super(message);
    }

    public InvalidSecretKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}