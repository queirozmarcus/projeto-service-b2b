package com.scopeflow.adapter.out.persistence.briefing;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for BriefingQuestion.
 * Maps to briefing_questions table.
 * Immutable: questions never change once created.
 */
@Entity
@Table(
    name = "briefing_questions",
    indexes = {
        @Index(name = "idx_briefing_questions_session_id", columnList = "briefing_session_id"),
        @Index(name = "idx_briefing_questions_step", columnList = "briefing_session_id, step"),
        @Index(name = "idx_briefing_questions_type", columnList = "question_type"),
        @Index(name = "idx_briefing_questions_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "idx_briefing_questions_unique_step", columnNames = {"briefing_session_id", "step"})
    }
)
public class JpaBriefingQuestion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "briefing_session_id", nullable = false, updatable = false)
    private UUID briefingSessionId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;

    @Column(name = "ai_prompt_version", length = 50)
    private String aiPromptVersion;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "follow_up_generated", nullable = false)
    private boolean followUpGenerated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA requires no-arg constructor.
     */
    protected JpaBriefingQuestion() {
        // JPA only
    }

    /**
     * Domain-driven constructor.
     */
    public JpaBriefingQuestion(
            UUID id,
            UUID briefingSessionId,
            String questionText,
            int step,
            String questionType,
            String aiPromptVersion,
            boolean required,
            boolean followUpGenerated,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.briefingSessionId = Objects.requireNonNull(briefingSessionId, "briefingSessionId cannot be null");
        this.questionText = Objects.requireNonNull(questionText, "questionText cannot be null");
        if (step <= 0) {
            throw new IllegalArgumentException("step must be > 0");
        }
        this.step = step;
        this.questionType = Objects.requireNonNull(questionType, "questionType cannot be null");
        this.aiPromptVersion = aiPromptVersion != null ? aiPromptVersion : "v1";
        this.required = required;
        this.followUpGenerated = followUpGenerated;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    // Getters only (immutable)

    public UUID getId() {
        return id;
    }

    public UUID getBriefingSessionId() {
        return briefingSessionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public int getStep() {
        return step;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getAiPromptVersion() {
        return aiPromptVersion;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isFollowUpGenerated() {
        return followUpGenerated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaBriefingQuestion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "JpaBriefingQuestion{" +
                "id=" + id +
                ", briefingSessionId=" + briefingSessionId +
                ", step=" + step +
                ", questionType='" + questionType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
