package com.scopeflow.core.domain.proposal;

/**
 * Thrown on invalid proposal state transitions (e.g., cannot publish an already approved proposal).
 */
public class InvalidProposalStateException extends RuntimeException {

    private static final String ERROR_CODE = "PROPOSAL-002";

    public InvalidProposalStateException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
