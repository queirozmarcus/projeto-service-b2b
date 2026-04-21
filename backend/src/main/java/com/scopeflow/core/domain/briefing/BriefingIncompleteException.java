package com.scopeflow.core.domain.briefing;

/**
 * Thrown when a proposal requires a completed briefing but the briefing is not in COMPLETED state.
 * Maps to HTTP 409 (PROPOSAL-012).
 */
public class BriefingIncompleteException extends BriefingDomainException {
    private static final String ERROR_CODE = "PROPOSAL-012";

    public BriefingIncompleteException(BriefingSessionId id) {
        super(ERROR_CODE, "Briefing %s is not completed".formatted(id.value()));
    }
}
