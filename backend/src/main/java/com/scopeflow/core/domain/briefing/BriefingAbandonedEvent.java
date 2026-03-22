package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event: Briefing abandoned by client.
 * Published when the client abandons the briefing (can restart later).
 */
public record BriefingAbandonedEvent(
        BriefingSessionId sessionId,
        WorkspaceId workspaceId,
        ClientId clientId,
        String abandonReason,
        Instant occurredAt
) implements DomainEvent {
    public BriefingAbandonedEvent {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(clientId, "clientId cannot be null");
        Objects.requireNonNull(abandonReason, "abandonReason cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public BriefingSessionId aggregateId() {
        return sessionId;
    }

    @Override
    public String eventType() {
        return "briefing.abandoned";
    }
}
