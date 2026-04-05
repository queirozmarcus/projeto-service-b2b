package com.scopeflow.user.domain.exception;

import com.scopeflow.user.domain.model.UserId;

/**
 * Domain exception: Invalid invited_by user.
 * Error code: USER-012
 */
public class InvalidInvitedByUserException extends RuntimeException {
    private static final String ERROR_CODE = "USER-012";

    public InvalidInvitedByUserException(String message) {
        super(message);
    }

    public InvalidInvitedByUserException(UserId userId) {
        super("Invalid invited_by user: " + userId.value());
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
