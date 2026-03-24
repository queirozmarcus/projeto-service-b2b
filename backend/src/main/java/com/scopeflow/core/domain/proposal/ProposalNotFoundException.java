package com.scopeflow.core.domain.proposal;

/**
 * Thrown when a proposal cannot be found.
 */
public class ProposalNotFoundException extends RuntimeException {

    private static final String ERROR_CODE = "PROPOSAL-001";

    public ProposalNotFoundException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
