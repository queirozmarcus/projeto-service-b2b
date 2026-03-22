package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event: AI-generated follow-up question.
 * Published when the system detects gaps and auto-generates a follow-up question.
 */
public record FollowupQuestionGeneratedEvent(
        BriefingSessionId sessionId,
        WorkspaceId workspaceId,
        QuestionId followupQuestionId,
        QuestionId parentQuestionId,
        String gapReason,
        Instant occurredAt
) implements DomainEvent {
    public FollowupQuestionGeneratedEvent {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(followupQuestionId, "followupQuestionId cannot be null");
        Objects.requireNonNull(parentQuestionId, "parentQuestionId cannot be null");
        Objects.requireNonNull(gapReason, "gapReason cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public BriefingSessionId aggregateId() {
        return sessionId;
    }

    @Override
    public String eventType() {
        return "briefing.followup.generated";
    }
}
