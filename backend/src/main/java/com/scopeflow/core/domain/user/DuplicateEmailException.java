package com.scopeflow.core.domain.user;

/**
 * Exception thrown when attempting to create a user with an email that already exists.
 * Used by UserServiceRestAdapter when user-service returns 409 Conflict.
 */
public class DuplicateEmailException extends RuntimeException {
    private static final String ERROR_CODE = "USER-003";

    public DuplicateEmailException(String email) {
        super(String.format("User with email '%s' already exists", email));
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
