package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event: Briefing completed successfully.
 * Published when the briefing reaches completion score >= 80% and no critical gaps.
 * Triggers scope generation in Proposal context.
 */
public record BriefingCompletedEvent(
        BriefingSessionId sessionId,
        WorkspaceId workspaceId,
        ClientId clientId,
        int completionScore,
        String gapsSummary,
        Instant occurredAt
) implements DomainEvent {
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

    @Override
    public BriefingSessionId aggregateId() {
        return sessionId;
    }

    @Override
    public String eventType() {
        return "briefing.completed";
    }
}
