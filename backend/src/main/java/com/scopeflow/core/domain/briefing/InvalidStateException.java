package com.scopeflow.core.domain.briefing;

/**
 * Thrown when an operation is invalid for the current briefing state.
 * Error code: BRIEFING-007
 */
public class InvalidStateException extends BriefingDomainException {
    public InvalidStateException(String message) {
        super("BRIEFING-007", message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super("BRIEFING-007", message, cause);
    }
}
