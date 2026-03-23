package com.scopeflow.core.domain.briefing;

import java.util.Objects;
import java.util.UUID;

/**
 * QuestionId value object: wraps UUID for type safety.
 * Immutable, uses record semantics.
 */
public record QuestionId(UUID value) {
    public QuestionId {
        Objects.requireNonNull(value, "QuestionId value cannot be null");
    }

    public static QuestionId generate() {
        return new QuestionId(UUID.randomUUID());
    }

    public static QuestionId of(String uuidString) {
        return new QuestionId(UUID.fromString(uuidString));
    }
}
