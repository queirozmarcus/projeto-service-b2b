package com.scopeflow.adapter.out.persistence.briefing;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for BriefingActivityLog audit trail.
 * Maps to briefing_activity_logs table.
 * Immutable: audit trail for compliance (LGPD) and debugging.
 */
@Entity
@Table(
    name = "briefing_activity_logs",
    indexes = {
        @Index(name = "idx_briefing_activity_logs_session_id", columnList = "briefing_session_id"),
        @Index(name = "idx_briefing_activity_logs_action", columnList = "action"),
        @Index(name = "idx_briefing_activity_logs_created_at", columnList = "created_at")
    }
)
public class JpaBriefingActivityLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "briefing_session_id", nullable = false, updatable = false)
    private UUID briefingSessionId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA requires no-arg constructor.
     */
    protected JpaBriefingActivityLog() {
        // JPA only
    }

    /**
     * Domain-driven constructor.
     */
    public JpaBriefingActivityLog(
            UUID id,
            UUID briefingSessionId,
            String action,
            String entityType,
            UUID entityId,
            String details,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.briefingSessionId = Objects.requireNonNull(briefingSessionId, "briefingSessionId cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    // Getters only (immutable)

    public UUID getId() {
        return id;
    }

    public UUID getBriefingSessionId() {
        return briefingSessionId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaBriefingActivityLog that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "JpaBriefingActivityLog{" +
                "id=" + id +
                ", briefingSessionId=" + briefingSessionId +
                ", action='" + action + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
