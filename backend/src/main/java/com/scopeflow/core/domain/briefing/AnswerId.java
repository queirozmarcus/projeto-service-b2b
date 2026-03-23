package com.scopeflow.core.domain.briefing;

import java.util.Objects;
import java.util.UUID;

/**
 * AnswerId value object: wraps UUID for type safety.
 * Immutable, uses record semantics.
 */
public record AnswerId(UUID value) {
    public AnswerId {
        Objects.requireNonNull(value, "AnswerId value cannot be null");
    }

    public static AnswerId generate() {
        return new AnswerId(UUID.randomUUID());
    }

    public static AnswerId of(String uuidString) {
        return new AnswerId(UUID.fromString(uuidString));
    }
}
