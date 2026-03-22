package com.scopeflow.core.domain.user;

import java.util.Objects;

/**
 * PasswordHash value object: enforces bcrypt format.
 * Uses record for immutability + compact constructor for validation.
 *
 * NOTE: In real implementation, bcrypt library would be added:
 * - org.springframework.security:spring-security-crypto (for BCryptPasswordEncoder)
 * - OR at.favre.lib:bcrypt (standalone library)
 *
 * For now, we validate format only (tests will use real bcrypt).
 */
public record PasswordHash(String value) {
    private static final String BCRYPT_REGEX = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    public PasswordHash {
        Objects.requireNonNull(value, "PasswordHash value cannot be null");
        if (!value.matches(BCRYPT_REGEX)) {
            throw new IllegalArgumentException("Invalid bcrypt hash format");
        }
    }

    /**
     * Verify plaintext password against this hash.
     * In real implementation, uses BCryptPasswordEncoder.matches().
     */
    public boolean matches(String plaintext) {
        // Placeholder: real implementation would use BCryptPasswordEncoder
        // return BCryptPasswordEncoder.matches(plaintext, this.value);
        throw new UnsupportedOperationException(
            "Requires BCryptPasswordEncoder from Spring Security. Use in adapter layer only."
        );
    }

    @Override
    public String toString() {
        return "PasswordHash(***redacted***)";
    }
}
