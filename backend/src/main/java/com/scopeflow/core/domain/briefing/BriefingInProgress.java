package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Active briefing state: client can submit answers, system detects gaps and generates follow-ups.
 */
public final class BriefingInProgress extends BriefingSession {
    private final BriefingProgress progress;

    public BriefingInProgress(
            BriefingSessionId id,
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType,
            PublicToken publicToken,
            Instant createdAt,
            Instant updatedAt,
            BriefingProgress progress
    ) {
        super(id, workspaceId, clientId, serviceType, publicToken, createdAt, updatedAt);
        this.progress = Objects.requireNonNull(progress, "BriefingProgress cannot be null");
    }

    @Override
    public String status() {
        return "IN_PROGRESS";
    }

    public BriefingProgress getProgress() {
        return progress;
    }

    public int getCurrentStep() {
        return progress.currentStep();
    }

    public int getTotalSteps() {
        return progress.totalSteps();
    }

    public int getCompletionPercentage() {
        return progress.completionPercentage();
    }

    /**
     * State transition: complete the briefing (if score >= 80%).
     */
    public BriefingCompleted completeBriefing(CompletionScore score) {
        Objects.requireNonNull(score, "CompletionScore cannot be null");
        return new BriefingCompleted(
                this.getId(),
                this.getWorkspaceId(),
                this.getClientId(),
                this.getServiceType(),
                this.getPublicToken(),
                this.getCreatedAt(),
                Instant.now(),
                score
        );
    }

    /**
     * State transition: abandon the briefing.
     */
    public BriefingAbandoned abandon() {
        return new BriefingAbandoned(
                this.getId(),
                this.getWorkspaceId(),
                this.getClientId(),
                this.getServiceType(),
                this.getPublicToken(),
                this.getCreatedAt(),
                Instant.now(),
                "User abandoned"
        );
    }
}
