package com.scopeflow.adapter.out.persistence.briefing;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for BriefingAnswer.
 * Maps to briefing_answers table.
 * Immutable: answers are audit trail (enforced by DB trigger, no UPDATE/DELETE allowed).
 */
@Entity
@Table(
    name = "briefing_answers",
    indexes = {
        @Index(name = "idx_briefing_answers_session_id", columnList = "briefing_session_id"),
        @Index(name = "idx_briefing_answers_question_id", columnList = "question_id"),
        @Index(name = "idx_briefing_answers_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_briefing_answers_unique_per_question", columnNames = {"briefing_session_id", "question_id"})
    }
)
public class JpaBriefingAnswer {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "briefing_session_id", nullable = false, updatable = false)
    private UUID briefingSessionId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "answer_json", columnDefinition = "jsonb")
    private String answerJson;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "ai_analysis", columnDefinition = "jsonb")
    private String aiAnalysis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA requires no-arg constructor.
     */
    protected JpaBriefingAnswer() {
        // JPA only
    }

    /**
     * Domain-driven constructor.
     */
    public JpaBriefingAnswer(
            UUID id,
            UUID briefingSessionId,
            UUID questionId,
            String answerText,
            String answerJson,
            Integer qualityScore,
            String aiAnalysis,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.briefingSessionId = Objects.requireNonNull(briefingSessionId, "briefingSessionId cannot be null");
        this.questionId = Objects.requireNonNull(questionId, "questionId cannot be null");
        this.answerText = Objects.requireNonNull(answerText, "answerText cannot be null");
        if (answerText.isBlank()) {
            throw new IllegalArgumentException("answerText cannot be blank");
        }
        this.answerJson = answerJson;
        this.qualityScore = qualityScore;
        this.aiAnalysis = aiAnalysis;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    // Getters only (immutable)

    public UUID getId() {
        return id;
    }

    public UUID getBriefingSessionId() {
        return briefingSessionId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaBriefingAnswer that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "JpaBriefingAnswer{" +
                "id=" + id +
                ", briefingSessionId=" + briefingSessionId +
                ", questionId=" + questionId +
                ", createdAt=" + createdAt +
                '}';
    }
}
