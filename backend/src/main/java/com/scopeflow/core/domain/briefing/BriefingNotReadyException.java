package com.scopeflow.core.domain.briefing;

/**
 * Thrown when a briefing's completeness level is below the required threshold for scope generation.
 * Maps to HTTP 409 (PROPOSAL-013).
 */
public class BriefingNotReadyException extends BriefingDomainException {
    private static final String ERROR_CODE = "PROPOSAL-013";

    public BriefingNotReadyException(BriefingSessionId id, int completeness) {
        super(ERROR_CODE, "Briefing %s completeness is %d%%, minimum 80%% required"
            .formatted(id.value(), completeness));
    }
}
