package io.github.mwangiiharun.aletheia.exceptions;

/**
 * Unchecked exception used when Aletheia cannot initialize itself properly,
 * such as when configuration loading fails at startup.
 * This is intentional, as such errors are not recoverable.
 */
public class AletheiaInitializationException extends RuntimeException {

    public AletheiaInitializationException(String message) {
        super(message);
    }

    public AletheiaInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}