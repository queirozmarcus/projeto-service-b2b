package com.scopeflow.core.domain.user;

/**
 * Domain exception: Email already registered (invariant violation).
 * Indicates user tried to register with email that already exists.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {
    private static final String ERROR_CODE = "USER-001";

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }

    public EmailAlreadyRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
