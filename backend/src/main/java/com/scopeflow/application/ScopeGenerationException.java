package com.scopeflow.application;

/**
 * Thrown when AI scope generation fails (network, timeout, quota exceeded).
 * Maps to HTTP 503 (PROPOSAL-014).
 */
public class ScopeGenerationException extends RuntimeException {
    private static final String ERROR_CODE = "PROPOSAL-014";

    public ScopeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
