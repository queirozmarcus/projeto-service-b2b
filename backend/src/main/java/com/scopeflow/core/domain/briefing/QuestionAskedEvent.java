package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event: Question asked in briefing.
 * Published when the next question is presented to the client.
 */
public record QuestionAskedEvent(
        BriefingSessionId sessionId,
        WorkspaceId workspaceId,
        QuestionId questionId,
        int step,
        Instant occurredAt
) implements DomainEvent {
    public QuestionAskedEvent {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(questionId, "questionId cannot be null");
        if (step < 0) {
            throw new IllegalArgumentException("step must be >= 0");
        }
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public BriefingSessionId aggregateId() {
        return sessionId;
    }

    @Override
    public String eventType() {
        return "briefing.question.asked";
    }
}
