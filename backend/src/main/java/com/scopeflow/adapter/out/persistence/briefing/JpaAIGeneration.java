package com.scopeflow.adapter.out.persistence.briefing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for AIGeneration audit trail.
 * Maps to ai_generations table.
 * Immutable: audit trail of all LLM calls for cost tracking and reproducibility.
 */
@Entity
@Table(
    name = "ai_generations",
    indexes = {
        @Index(name = "idx_ai_generations_session_id", columnList = "briefing_session_id"),
        @Index(name = "idx_ai_generations_type", columnList = "generation_type"),
        @Index(name = "idx_ai_generations_prompt_version", columnList = "prompt_version"),
        @Index(name = "idx_ai_generations_created_at", columnList = "created_at"),
        @Index(name = "idx_ai_generations_model_used", columnList = "model_used")
    }
)
public class JpaAIGeneration {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "briefing_session_id", nullable = false, updatable = false)
    private UUID briefingSessionId;

    @Column(name = "generation_type", nullable = false, length = 50)
    private String generationType;

    @Column(name = "input_json", nullable = false, columnDefinition = "jsonb")
    private String inputJson;

    @Column(name = "output_json", nullable = false, columnDefinition = "jsonb")
    private String outputJson;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA requires no-arg constructor.
     */
    protected JpaAIGeneration() {
        // JPA only
    }

    /**
     * Domain-driven constructor.
     */
    public JpaAIGeneration(
            UUID id,
            UUID briefingSessionId,
            String generationType,
            String inputJson,
            String outputJson,
            String promptVersion,
            Long latencyMs,
            BigDecimal costUsd,
            String modelUsed,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.briefingSessionId = Objects.requireNonNull(briefingSessionId, "briefingSessionId cannot be null");
        this.generationType = Objects.requireNonNull(generationType, "generationType cannot be null");
        this.inputJson = Objects.requireNonNull(inputJson, "inputJson cannot be null");
        this.outputJson = Objects.requireNonNull(outputJson, "outputJson cannot be null");
        this.promptVersion = Objects.requireNonNull(promptVersion, "promptVersion cannot be null");
        this.latencyMs = latencyMs;
        this.costUsd = costUsd;
        this.modelUsed = modelUsed;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    // Getters only (immutable)

    public UUID getId() {
        return id;
    }

    public UUID getBriefingSessionId() {
        return briefingSessionId;
    }

    public String getGenerationType() {
        return generationType;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaAIGeneration that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "JpaAIGeneration{" +
                "id=" + id +
                ", briefingSessionId=" + briefingSessionId +
                ", generationType='" + generationType + '\'' +
                ", promptVersion='" + promptVersion + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
