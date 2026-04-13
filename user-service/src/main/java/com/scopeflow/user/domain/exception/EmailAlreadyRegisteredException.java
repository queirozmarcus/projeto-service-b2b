package com.scopeflow.user.domain.exception;

/**
 * Domain exception: Email already registered (invariant violation).
 * Error code: USER-001
 */
public class EmailAlreadyRegisteredException extends RuntimeException {
    private static final String ERROR_CODE = "USER-001";

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
