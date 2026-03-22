package com.scopeflow.core.domain.briefing;

/**
 * AnswerText value object: validates non-empty, max length.
 * Immutable, uses record with compact constructor validation.
 */
public record AnswerText(String value) {
    public AnswerText {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Answer cannot be empty");
        }
        if (value.length() > 5000) {
            throw new IllegalArgumentException("Answer cannot exceed 5000 characters, got " + value.length());
        }
    }

    /**
     * Trim whitespace for storage/comparison.
     */
    public String trimmed() {
        return value.trim();
    }
}
