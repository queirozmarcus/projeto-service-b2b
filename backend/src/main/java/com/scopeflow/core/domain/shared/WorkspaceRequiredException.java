package com.scopeflow.core.domain.shared;

/**
 * Exception thrown when an operation requires an active workspace but the JWT lacks workspace_id.
 *
 * This occurs when:
 * - User registered via user-service (which generates JWT with workspace_id=null by design)
 * - User attempts to access multi-tenant endpoints before completing workspace setup
 *
 * Maps to HTTP 412 Precondition Failed (RFC 9457 Problem Details).
 * Distinct from 401 (authentication valid) and 403 (authorization valid, but forbidden).
 *
 * Error code: WORKSPACE-001
 */
public class WorkspaceRequiredException extends RuntimeException {

    private static final String ERROR_CODE = "WORKSPACE-001";
    private static final String DEFAULT_MESSAGE = "This operation requires an active workspace. Please complete workspace setup.";

    private final String errorCode;

    public WorkspaceRequiredException() {
        super(DEFAULT_MESSAGE);
        this.errorCode = ERROR_CODE;
    }

    public WorkspaceRequiredException(String message) {
        super(message);
        this.errorCode = ERROR_CODE;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
