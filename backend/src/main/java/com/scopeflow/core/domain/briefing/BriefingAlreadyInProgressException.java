package com.scopeflow.core.domain.briefing;

/**
 * Thrown when attempting to start a new briefing while one is already in progress for same client/service.
 * Error code: BRIEFING-006
 */
public class BriefingAlreadyInProgressException extends BriefingDomainException {
    public BriefingAlreadyInProgressException(String message) {
        super("BRIEFING-006", message);
    }

    public BriefingAlreadyInProgressException(String message, Throwable cause) {
        super("BRIEFING-006", message, cause);
    }
}
