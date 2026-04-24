package com.scopeflow.user.domain.exception;

import com.scopeflow.user.domain.model.UserId;

/**
 * Domain exception: operation not allowed in current user state.
 * Error code: USER-021
 */
public class UserStateException extends RuntimeException {
    private static final String ERROR_CODE = "USER-021";

    public UserStateException(UserId userId, String state, String operation) {
        super("User " + userId.value() + " in state " + state + " cannot perform: " + operation);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
