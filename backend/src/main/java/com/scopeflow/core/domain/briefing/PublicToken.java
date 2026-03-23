package com.scopeflow.core.domain.briefing;

import java.util.UUID;

/**
 * PublicToken value object: generates secure token for public briefing links.
 * Immutable, uses record with compact constructor validation.
 */
public record PublicToken(String value) {
    public PublicToken {
        if (value == null || value.length() < 32) {
            throw new IllegalArgumentException("PublicToken must be at least 32 characters, got " + (value != null ? value.length() : "null"));
        }
    }

    /**
     * Generate a new secure random public token.
     */
    public static PublicToken generate() {
        // Combine two UUIDs for sufficient entropy (32 chars)
        String token = UUID.randomUUID().toString().replace("-", "") +
                       UUID.randomUUID().toString().replace("-", "");
        return new PublicToken(token.substring(0, 64)); // Use first 64 chars for good randomness
    }
}
