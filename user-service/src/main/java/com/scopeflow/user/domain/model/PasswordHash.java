package com.scopeflow.user.domain.model;

import java.util.Objects;

/**
 * PasswordHash value object: enforces bcrypt format.
 * Uses record for immutability + compact constructor for validation.
 */
public record PasswordHash(String value) {
    private static final String BCRYPT_REGEX = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    public PasswordHash {
        Objects.requireNonNull(value, "PasswordHash value cannot be null");
        if (!value.matches(BCRYPT_REGEX)) {
            throw new IllegalArgumentException("Invalid bcrypt hash format");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash(***redacted***)";
    }
}
