package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * Event: Answer submitted to a question.
 * Published when the client submits an answer.
 */
public record AnswerSubmittedEvent(
        BriefingSessionId sessionId,
        WorkspaceId workspaceId,
        AnswerId answerId,
        QuestionId questionId,
        String answerType, // "DIRECT" or "WITH_FOLLOWUP"
        Instant occurredAt
) implements DomainEvent {
    public AnswerSubmittedEvent {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(answerId, "answerId cannot be null");
        Objects.requireNonNull(questionId, "questionId cannot be null");
        Objects.requireNonNull(answerType, "answerType cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    @Override
    public BriefingSessionId aggregateId() {
        return sessionId;
    }

    @Override
    public String eventType() {
        return "briefing.answer.submitted";
    }
}
