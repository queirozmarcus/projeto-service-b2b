package com.scopeflow.core.domain.user;

import java.util.UUID;

/**
 * Exception for user not found scenarios.
 * Post-migration: handled by GlobalExceptionHandler for legacy compatibility.
 */
public class UserNotFoundException extends RuntimeException {
    private static final String ERROR_CODE = "USER-004";

    public UserNotFoundException(UUID userId) {
        super(String.format("User not found: %s", userId));
    }

    public UserNotFoundException(String email) {
        super(String.format("User not found: %s", email));
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
