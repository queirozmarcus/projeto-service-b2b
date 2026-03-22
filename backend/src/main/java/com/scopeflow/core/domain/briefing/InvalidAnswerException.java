package com.scopeflow.core.domain.briefing;

/**
 * Thrown when an answer is invalid (empty, too long, etc.).
 * Error code: BRIEFING-003
 */
public class InvalidAnswerException extends BriefingDomainException {
    public InvalidAnswerException(String message) {
        super("BRIEFING-003", message);
    }

    public InvalidAnswerException(String message, Throwable cause) {
        super("BRIEFING-003", message, cause);
    }
}
