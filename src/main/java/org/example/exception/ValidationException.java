package org.example.exception;

/**
 * Custom exception for validation errors.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 7417854054901739808L;

    public ValidationException() {
        super();
    }

    /**
     * Constructs a ValidationException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a ValidationException with the specified detail message
     * and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a ValidationException with the specified cause.
     *
     * @param cause the cause of the exception.
     */
    public ValidationException(Throwable cause) {
        super(cause);
    }
}
