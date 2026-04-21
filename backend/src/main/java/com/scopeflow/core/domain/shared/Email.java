package com.scopeflow.core.domain.shared;

import com.scopeflow.core.domain.common.InvalidValueObjectException;
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
            throw new InvalidValueObjectException("Email", "Email cannot be empty");
        }
        if (!trimmed.matches(EMAIL_REGEX)) {
            throw new InvalidValueObjectException("Email", "Invalid email format: " + value);
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
