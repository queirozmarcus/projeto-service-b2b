package com.scopeflow.user.domain.exception;

import com.scopeflow.user.domain.model.Email;

/**
 * Domain exception: Email already exists.
 * Error code: USER-011
 */
public class DuplicateEmailException extends RuntimeException {
    private static final String ERROR_CODE = "USER-011";

    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(Email email) {
        super("Email already exists: " + email.normalized());
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
