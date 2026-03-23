package com.scopeflow.core.domain.briefing;

/**
 * Thrown when attempting to complete a briefing with incomplete answers (score < 80%).
 * Error code: BRIEFING-005
 */
public class IncompleteGapsException extends BriefingDomainException {
    public IncompleteGapsException(String message) {
        super("BRIEFING-005", message);
    }

    public IncompleteGapsException(String message, Throwable cause) {
        super("BRIEFING-005", message, cause);
    }
}
