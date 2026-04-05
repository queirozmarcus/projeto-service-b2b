package com.scopeflow.user.domain.exception;

/**
 * Thrown when login credentials are invalid.
 * Error code: AUTH-401
 */
public class InvalidCredentialsException extends RuntimeException {
    private static final String ERROR_CODE = "AUTH-401";

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
