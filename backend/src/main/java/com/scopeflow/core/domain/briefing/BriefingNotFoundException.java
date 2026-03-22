package com.scopeflow.core.domain.briefing;

/**
 * Thrown when a briefing session is not found.
 * Error code: BRIEFING-001
 */
public class BriefingNotFoundException extends BriefingDomainException {
    public BriefingNotFoundException(String message) {
        super("BRIEFING-001", message);
    }

    public BriefingNotFoundException(String message, Throwable cause) {
        super("BRIEFING-001", message, cause);
    }
}
