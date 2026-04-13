package com.scopeflow.core.domain.user.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event: User registered successfully.
 *
 * Published when a new user completes registration.
 * Consumed by:
 *  - UserRegistrationListener → sends welcome email
 *
 * Serialized as JSON and stored in outbox_event table (payload column).
 * Deserialized by OutboxEventPublisher before publishing to RabbitMQ.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String fullName,
        UUID workspaceId,
        Instant occurredAt
) {
    public UserRegisteredEvent {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(fullName, "fullName cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    /**
     * Factory for convenience in application services and tests.
     */
    public static UserRegisteredEvent of(
            UUID userId,
            String email,
            String fullName,
            UUID workspaceId
    ) {
        return new UserRegisteredEvent(userId, email, fullName, workspaceId, Instant.now());
    }
}
