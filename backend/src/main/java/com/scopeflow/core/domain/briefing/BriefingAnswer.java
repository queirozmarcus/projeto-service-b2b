package com.scopeflow.core.domain.briefing;

import java.time.Instant;
import java.util.Objects;

/**
 * BriefingAnswer aggregate (sealed class for type safety).
 *
 * States:
 * - AnsweredDirect: direct response with quality score
 * - AnsweredWithFollowup: response with auto-generated follow-up question
 *
 * Immutable: answers cannot be edited once submitted (audit trail).
 */
public abstract sealed class BriefingAnswer permits AnsweredDirect, AnsweredWithFollowup {
    private final AnswerId id;
    private final BriefingSessionId sessionId;
    private final QuestionId questionId;
    private final AnswerText text;
    private final Instant answeredAt;

    protected BriefingAnswer(
            AnswerId id,
            BriefingSessionId sessionId,
            QuestionId questionId,
            AnswerText text,
            Instant answeredAt
    ) {
        this.id = Objects.requireNonNull(id, "AnswerId cannot be null");
        this.sessionId = Objects.requireNonNull(sessionId, "BriefingSessionId cannot be null");
        this.questionId = Objects.requireNonNull(questionId, "QuestionId cannot be null");
        this.text = Objects.requireNonNull(text, "AnswerText cannot be null");
        this.answeredAt = Objects.requireNonNull(answeredAt, "AnsweredAt cannot be null");
    }

    // ============ Accessors ============

    public AnswerId getId() {
        return id;
    }

    public BriefingSessionId getSessionId() {
        return sessionId;
    }

    public QuestionId getQuestionId() {
        return questionId;
    }

    public AnswerText getText() {
        return text;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    // ============ Abstract Methods ============

    public abstract String answerType();

    public abstract int getQualityScore();

    public abstract boolean hasFollowup();

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BriefingAnswer)) return false;
        BriefingAnswer that = (BriefingAnswer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BriefingAnswer{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", questionId=" + questionId +
                ", answerType=" + answerType() +
                ", answeredAt=" + answeredAt +
                '}';
    }
}
