package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Terminal state: client abandoned the briefing, but can restart with new BriefingInProgress.
 */
public final class BriefingAbandoned extends BriefingSession {
    private final String abandonReason;
    private final Instant abandonedAt;

    public BriefingAbandoned(
            BriefingSessionId id,
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType,
            PublicToken publicToken,
            Instant createdAt,
            Instant updatedAt,
            String abandonReason
    ) {
        super(id, workspaceId, clientId, serviceType, publicToken, createdAt, updatedAt);
        this.abandonReason = Objects.requireNonNull(abandonReason, "AbandonReason cannot be null");
        this.abandonedAt = Instant.now();
    }

    @Override
    public String status() {
        return "ABANDONED";
    }

    public String getAbandonReason() {
        return abandonReason;
    }

    public Instant getAbandonedAt() {
        return abandonedAt;
    }
}
