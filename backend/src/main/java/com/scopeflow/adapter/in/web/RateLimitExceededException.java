package com.scopeflow.adapter.in.web;

/**
 * Exception thrown when rate limit is exceeded.
 *
 * Rate limits:
 * - Authenticated endpoints: 100 req/min per user
 * - Public endpoints: 10 req/min per IP
 *
 * HTTP Status: 429 Too Many Requests
 * Error Code: RATE-429
 */
public class RateLimitExceededException extends RuntimeException {

    private final String errorCode = "RATE-429";

    public RateLimitExceededException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
