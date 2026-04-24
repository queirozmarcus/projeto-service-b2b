package com.scopeflow.user.domain.exception;

import com.scopeflow.user.domain.model.UserId;

import java.util.UUID;

/**
 * Domain exception: workspace already assigned to user.
 * Error code: USER-020
 */
public class WorkspaceAlreadyAssignedException extends RuntimeException {
    private static final String ERROR_CODE = "USER-020";

    public WorkspaceAlreadyAssignedException(UserId userId, UUID existingWorkspaceId) {
        super("User " + userId.value() + " already has workspace assigned: " + existingWorkspaceId);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
