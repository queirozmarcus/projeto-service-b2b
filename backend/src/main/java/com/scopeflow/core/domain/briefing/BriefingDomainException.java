package com.scopeflow.core.domain.briefing;

/**
 * Base domain exception for Briefing domain.
 * All domain-specific exceptions inherit from this and have stable error codes.
 */
public abstract class BriefingDomainException extends RuntimeException {
    private final String errorCode;

    protected BriefingDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BriefingDomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
