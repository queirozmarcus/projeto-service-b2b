package com.scopeflow.core.domain.briefing.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event: Briefing session completed (listener-facing version).
 *
 * This is the event published to RabbitMQ via the Outbox pattern.
 * Uses plain UUIDs instead of value objects for easier JSON serialization.
 *
 * Consumed by:
 *  - BriefingCompletedListener → fallback question generation
 *
 * Note: The domain-layer BriefingCompletedEvent (in com.scopeflow.core.domain.briefing)
 * uses rich value objects. This version is the integration event for the messaging layer.
 */
public record BriefingCompletedEvent(
        UUID sessionId,
        UUID workspaceId,
        UUID clientId,
        int completionScore,
        String gapsSummary,
        Instant occurredAt
) {
    public BriefingCompletedEvent {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(clientId, "clientId cannot be null");
        if (completionScore < 0 || completionScore > 100) {
            throw new IllegalArgumentException("completionScore must be 0-100");
        }
        Objects.requireNonNull(gapsSummary, "gapsSummary cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    public static BriefingCompletedEvent of(
            UUID sessionId,
            UUID workspaceId,
            UUID clientId,
            int completionScore,
            String gapsSummary
    ) {
        return new BriefingCompletedEvent(
                sessionId, workspaceId, clientId, completionScore, gapsSummary, Instant.now()
        );
    }
}
