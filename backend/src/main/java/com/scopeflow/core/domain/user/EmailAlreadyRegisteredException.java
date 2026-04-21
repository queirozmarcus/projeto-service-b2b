package com.scopeflow.core.domain.user;

/**
 * Exception for email uniqueness violations.
 * Post-migration: handled by GlobalExceptionHandler for legacy compatibility.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {
    private static final String ERROR_CODE = "USER-001";

    public EmailAlreadyRegisteredException(String email) {
        super(String.format("Email '%s' is already registered", email));
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
