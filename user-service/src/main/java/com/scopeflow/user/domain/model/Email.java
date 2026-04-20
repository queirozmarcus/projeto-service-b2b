package com.scopeflow.user.domain.model;

import com.scopeflow.user.domain.shared.InvalidValueObjectException;
import java.util.Objects;

/**
 * Email value object: validated, case-insensitive.
 * Uses record for immutability + compact constructor for validation.
 */
public record Email(String value) {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        Objects.requireNonNull(value, "Email value cannot be null");
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new InvalidValueObjectException("Email", "Email cannot be empty");
        }
        if (!normalized.matches(EMAIL_REGEX)) {
            throw new InvalidValueObjectException("Email", "Invalid email format: " + value);
        }
        value = normalized;
    }

    /**
     * Returns the normalized email (already lowercase — same as value).
     * Kept for backward compatibility with existing callers.
     */
    public String normalized() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
