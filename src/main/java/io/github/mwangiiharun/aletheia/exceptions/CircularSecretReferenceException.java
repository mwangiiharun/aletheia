package io.github.mwangiiharun.aletheia.exceptions;

/**
 * Thrown when a circular secret reference is detected.
 *
 * For example, if secret A depends on secret B which in turn references A,
 * Aletheia will detect the recursion and throw this exception.
 */
public class CircularSecretReferenceException extends AletheiaException {

    public CircularSecretReferenceException(String message) {
        super(message);
    }

    public CircularSecretReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}