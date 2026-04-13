package com.scopeflow.user.domain.exception;

/**
 * Domain exception: Invalid role specified.
 * Error code: USER-013
 */
public class InvalidRoleException extends RuntimeException {
    private static final String ERROR_CODE = "USER-013";

    public InvalidRoleException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
