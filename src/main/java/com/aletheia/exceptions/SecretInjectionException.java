package com.aletheia.exceptions;

/**
 * Thrown when Aletheia fails to inject a resolved secret into a target field.
 */
public class SecretInjectionException extends AletheiaException {

    public SecretInjectionException(String fieldName, Throwable cause) {
        super("Failed to inject secret into field '" + fieldName + "'", cause);
    }
}