package com.scopeflow.core.domain.workspace;

/**
 * Domain exception: Member not found.
 */
public class MemberNotFoundException extends RuntimeException {
    private static final String ERROR_CODE = "WORKSPACE-005";

    public MemberNotFoundException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
