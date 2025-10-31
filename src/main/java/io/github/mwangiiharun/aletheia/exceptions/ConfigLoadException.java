package io.github.mwangiiharun.aletheia.exceptions;

/**
 * Thrown when Aletheia cannot load or parse its configuration file.
 */
public class ConfigLoadException extends AletheiaException {

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}