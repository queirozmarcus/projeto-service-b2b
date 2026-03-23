package com.scopeflow.adapter.out.persistence.briefing;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for BriefingSession aggregate root.
 * Maps to briefing_sessions table.
 * Immutable: no setters (domain logic enforces state changes).
 */
@Entity
@Table(
    name = "briefing_sessions",
    indexes = {
        @Index(name = "idx_briefing_sessions_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_briefing_sessions_status", columnList = "status"),
        @Index(name = "idx_briefing_sessions_client_id", columnList = "client_id"),
        @Index(name = "idx_briefing_sessions_service_type", columnList = "service_type"),
        @Index(name = "idx_briefing_sessions_public_token", columnList = "public_token"),
        @Index(name = "idx_briefing_sessions_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_briefing_sessions_public_token", columnNames = {"public_token"})
    }
)
public class JpaBriefingSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "service_type", nullable = false, length = 50, updatable = false)
    private String serviceType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "public_token", nullable = false, unique = true, updatable = false)
    private String publicToken;

    @Column(name = "completion_score")
    private Integer completionScore;

    @Column(name = "ai_analysis", columnDefinition = "jsonb")
    private String aiAnalysis;

    @Column(name = "abandoned_reason", length = 500)
    private String abandonedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA requires no-arg constructor (can be protected).
     */
    protected JpaBriefingSession() {
        // JPA only
    }

    /**
     * Domain-driven constructor: all required fields immutable.
     */
    public JpaBriefingSession(
            UUID id,
            UUID workspaceId,
            UUID clientId,
            String serviceType,
            String status,
            String publicToken,
            Integer completionScore,
            String aiAnalysis,
            String abandonedReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId cannot be null");
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.publicToken = Objects.requireNonNull(publicToken, "publicToken cannot be null");
        this.completionScore = completionScore;
        this.aiAnalysis = aiAnalysis;
        this.abandonedReason = abandonedReason;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    // Getters only (immutable)

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getStatus() {
        return status;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public Integer getCompletionScore() {
        return completionScore;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public String getAbandonedReason() {
        return abandonedReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaBriefingSession that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "JpaBriefingSession{" +
                "id=" + id +
                ", workspaceId=" + workspaceId +
                ", clientId=" + clientId +
                ", serviceType='" + serviceType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
