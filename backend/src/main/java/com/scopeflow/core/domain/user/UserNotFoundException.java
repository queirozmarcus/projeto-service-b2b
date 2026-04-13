package com.scopeflow.core.domain.user;

/**
 * Domain exception: User not found.
 * Error code: USER-010
 */
public class UserNotFoundException extends RuntimeException {
    private static final String ERROR_CODE = "USER-010";

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(UserId userId) {
        super("User not found: " + userId.value());
    }

    public UserNotFoundException(Email email) {
        super("User not found with email: " + email.normalized());
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
