package com.scopeflow.core.domain.workspace;

/**
 * Domain exception: Member already exists in workspace (invariant violation).
 */
public class MemberAlreadyExistsException extends RuntimeException {
    private static final String ERROR_CODE = "WORKSPACE-004";

    public MemberAlreadyExistsException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
