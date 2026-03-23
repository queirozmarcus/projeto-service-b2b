package com.scopeflow.core.domain.briefing;

/**
 * Thrown when attempting to generate more follow-up questions than allowed (max 1 per question).
 * Error code: BRIEFING-004
 */
public class MaxFollowupExceededException extends BriefingDomainException {
    public MaxFollowupExceededException(String message) {
        super("BRIEFING-004", message);
    }

    public MaxFollowupExceededException(String message, Throwable cause) {
        super("BRIEFING-004", message, cause);
    }
}
