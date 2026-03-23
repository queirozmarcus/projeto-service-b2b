package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Terminal state: briefing is complete, locked for editing, ready for scope generation.
 */
public final class BriefingCompleted extends BriefingSession {
    private final CompletionScore completionScore;
    private final Instant completedAt;

    public BriefingCompleted(
            BriefingSessionId id,
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType,
            PublicToken publicToken,
            Instant createdAt,
            Instant updatedAt,
            CompletionScore completionScore
    ) {
        super(id, workspaceId, clientId, serviceType, publicToken, createdAt, updatedAt);
        this.completionScore = Objects.requireNonNull(completionScore, "CompletionScore cannot be null");
        this.completedAt = Instant.now();
    }

    @Override
    public String status() {
        return "COMPLETED";
    }

    public CompletionScore getCompletionScore() {
        return completionScore;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Check if briefing is ready for scope generation (>= 80%).
     */
    public boolean isReady() {
        return completionScore.score() >= 80;
    }
}
