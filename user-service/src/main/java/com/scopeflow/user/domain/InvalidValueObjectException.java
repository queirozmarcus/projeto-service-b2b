package com.scopeflow.user.domain;

/**
 * Exception thrown when a Value Object fails validation.
 *
 * <p>This exception should be used by all Value Objects (Email, PasswordHash, etc.)
 * instead of generic {@link IllegalArgumentException} to enable proper HTTP error mapping.
 *
 * <p>Example usage in a Value Object:
 * <pre>{@code
 * public record Email(String value) {
 *     public Email {
 *         if (!value.matches(EMAIL_REGEX)) {
 *             throw new InvalidValueObjectException("Email", "Invalid email format: " + value);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>The {@code GlobalExceptionHandler} maps this to HTTP 400 Bad Request with RFC 9457 Problem Details.
 *
 * @see com.scopeflow.user.adapter.in.web.GlobalExceptionHandler
 */
public class InvalidValueObjectException extends RuntimeException {

    private final String voType;
    private final String errorCode;

    /**
     * Creates a new InvalidValueObjectException.
     *
     * @param voType the type of Value Object that failed validation (e.g., "Email", "PasswordHash")
     * @param message detailed validation error message
     */
    public InvalidValueObjectException(String voType, String message) {
        super(message);
        this.voType = voType;
        this.errorCode = "VO-001";
    }

    /**
     * Returns the type of Value Object that failed validation.
     *
     * @return the VO type (e.g., "Email")
     */
    public String getVoType() {
        return voType;
    }

    /**
     * Returns the error code for HTTP mapping.
     *
     * @return always "VO-001"
     */
    public String getErrorCode() {
        return errorCode;
    }
}
