package com.scopeflow.core.domain.user;

/**
 * Domain exception: Invalid role specified.
 * Error code: USER-013
 */
public class InvalidRoleException extends RuntimeException {
    private static final String ERROR_CODE = "USER-013";

    public InvalidRoleException(String message) {
        super(message);
    }

    public InvalidRoleException(String role) {
        super("Invalid role: " + role);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
