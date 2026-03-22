package com.scopeflow.core.domain.briefing;

/**
 * Thrown when attempting to modify a briefing session that is already completed.
 * Error code: BRIEFING-002
 */
public class BriefingAlreadyCompletedException extends BriefingDomainException {
    public BriefingAlreadyCompletedException(String message) {
        super("BRIEFING-002", message);
    }

    public BriefingAlreadyCompletedException(String message, Throwable cause) {
        super("BRIEFING-002", message, cause);
    }
}
