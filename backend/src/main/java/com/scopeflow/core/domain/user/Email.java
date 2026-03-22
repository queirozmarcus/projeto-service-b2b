package com.scopeflow.core.domain.user;

import java.util.Objects;

/**
 * Email value object: validated, case-insensitive.
 * Uses record for immutability + compact constructor for validation.
 */
public record Email(String value) {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        Objects.requireNonNull(value, "Email value cannot be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!trimmed.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    /**
     * Returns normalized (lowercased) email for case-insensitive lookups.
     */
    public String normalized() {
        return value.toLowerCase();
    }

    @Override
    public String toString() {
        return value;
    }
}
