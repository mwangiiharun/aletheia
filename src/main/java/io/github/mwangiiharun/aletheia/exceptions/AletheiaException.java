package io.github.mwangiiharun.aletheia.exceptions;

/**
 * Base checked exception for all recoverable Aletheia errors.
 */
public class AletheiaException extends Exception {

    public AletheiaException(String message) {
        super(message);
    }

    public AletheiaException(String message, Throwable cause) {
        super(message, cause);
    }
}