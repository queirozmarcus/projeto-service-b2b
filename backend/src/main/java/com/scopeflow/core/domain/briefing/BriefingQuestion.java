package com.scopeflow.core.domain.briefing;

import java.time.Instant;
import java.util.Objects;

/**
 * BriefingQuestion represents a question in the discovery flow.
 * Questions are created by the system based on service type and client context.
 */
public final class BriefingQuestion {
    private final QuestionId id;
    private final BriefingSessionId sessionId;
    private final String text;
    private final int step;
    private final String questionType; // e.g., "OPEN_ENDED", "MULTIPLE_CHOICE", "SCALE"
    private final Instant createdAt;

    public BriefingQuestion(
            QuestionId id,
            BriefingSessionId sessionId,
            String text,
            int step,
            String questionType,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "QuestionId cannot be null");
        this.sessionId = Objects.requireNonNull(sessionId, "BriefingSessionId cannot be null");
        this.text = Objects.requireNonNull(text, "Question text cannot be null");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Question text cannot be blank");
        }
        if (step < 0) {
            throw new IllegalArgumentException("Step must be >= 0, got " + step);
        }
        this.step = step;
        this.questionType = Objects.requireNonNull(questionType, "Question type cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
    }

    // ============ Accessors ============

    public QuestionId getId() {
        return id;
    }

    public BriefingSessionId getSessionId() {
        return sessionId;
    }

    public String getText() {
        return text;
    }

    public int getStep() {
        return step;
    }

    public String getQuestionType() {
        return questionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BriefingQuestion)) return false;
        BriefingQuestion that = (BriefingQuestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BriefingQuestion{" +
                "id=" + id +
                ", step=" + step +
                ", text='" + text + '\'' +
                ", questionType='" + questionType + '\'' +
                '}';
    }
}
