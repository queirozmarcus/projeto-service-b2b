package com.scopeflow.core.domain.workspace;

/**
 * Domain exception: Workspace not found.
 */
public class WorkspaceNotFoundException extends RuntimeException {
    private static final String ERROR_CODE = "WORKSPACE-002";

    public WorkspaceNotFoundException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
