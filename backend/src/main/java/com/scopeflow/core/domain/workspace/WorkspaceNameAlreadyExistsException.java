package com.scopeflow.core.domain.workspace;

/**
 * Domain exception: Workspace name already exists (invariant violation).
 */
public class WorkspaceNameAlreadyExistsException extends RuntimeException {
    private static final String ERROR_CODE = "WORKSPACE-001";

    public WorkspaceNameAlreadyExistsException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
