package com.scopeflow.core.domain.proposal;

/**
 * Thrown when a client tries to use an expired or invalid approval token.
 */
public class ApprovalTokenExpiredException extends RuntimeException {

    private static final String ERROR_CODE = "PROPOSAL-003";

    public ApprovalTokenExpiredException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
