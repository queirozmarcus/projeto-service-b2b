package com.scopeflow.core.domain.workspace;

/**
 * Domain exception: Cannot remove last OWNER (invariant violation).
 */
public class CannotRemoveLastOwnerException extends RuntimeException {
    private static final String ERROR_CODE = "WORKSPACE-003";

    public CannotRemoveLastOwnerException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
