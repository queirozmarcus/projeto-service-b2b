package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event: BriefingSession started.
 * Published when a new briefing is created.
 */
public record BriefingSessionStartedEvent(
        BriefingSessionId sessionId,
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType,
        PublicToken publicToken,
        Instant occurredAt
) implements DomainEvent {
    public BriefingSessionStartedEvent {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(clientId, "clientId cannot be null");
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(publicToken, "publicToken cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public BriefingSessionId aggregateId() {
        return sessionId;
    }

    @Override
    public String eventType() {
        return "briefing.session.started";
    }
}
