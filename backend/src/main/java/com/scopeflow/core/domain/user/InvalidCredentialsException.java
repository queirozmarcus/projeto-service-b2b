package com.scopeflow.core.domain.user;

/**
 * Exception for authentication failures.
 * Post-migration: handled by GlobalExceptionHandler for legacy compatibility.
 */
public class InvalidCredentialsException extends RuntimeException {
    private static final String ERROR_CODE = "USER-002";

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
