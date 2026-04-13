package com.scopeflow.application.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Outbox Event entity (Transactional Outbox Pattern — D8).
 *
 * Represents a domain event persisted in the database for reliable,
 * asynchronous publishing. Events are inserted within the same transaction
 * as domain changes, guaranteeing atomicity.
 *
 * Lifecycle:
 * 1. Service executes: TX starts
 * 2. Domain aggregate is modified and persisted
 * 3. OutboxEvent is inserted in same TX
 * 4. TX commits: both aggregate and event are durable
 * 5. OutboxEventPublisher scheduler (every 5s) polls unpublished events
 * 6. Event is deserialized and published to RabbitMQ
 * 7. published_at timestamp is set
 * 8. Event listener processes async and marks idempotency

 * This solves the "dual-write" problem: if the broker is down when TX commits,
 * the event is safe in the database and will be published on next poll.
 *
 * Schema reference: V5__outbox_event_schema.sql
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Fully qualified event class name for deserialization.
     * Example: "com.scopeflow.core.domain.user.event.UserRegisteredEvent"
     */
    @Column(nullable = false, length = 255)
    private String eventType;

    /**
     * ID of the aggregate that triggered this event.
     * Example: userId, proposalId, workspaceId, sessionId
     */
    @Column(nullable = false)
    private UUID aggregateId;

    /**
     * Type of aggregate (for organizing/debugging).
     * Example: "User", "Proposal", "BriefingSession"
     */
    @Column(nullable = false, length = 100)
    private String aggregateType;

    /**
     * JSON serialization of the domain event.
     * Stored as JSONB in PostgreSQL for queryability.
     * Reconstructed via: ObjectMapper.readValue(payload, Class.forName(eventType))
     */
    @Column(nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    /**
     * Timestamp when event was successfully published to RabbitMQ.
     * NULL = not yet published. Poller queries: WHERE published_at IS NULL
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Timestamp when event was created (inserted into DB).
     * Used for ordering, eventual replay, and auditing.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of last update (set by published_at update).
     */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Empty constructor for Hibernate.
     */
    public OutboxEvent() {
    }

    /**
     * Constructor for creating a new outbox event (unpublished).
     */
    public OutboxEvent(String eventType, UUID aggregateId, String aggregateType, String payload) {
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType cannot be null");
        this.payload = Objects.requireNonNull(payload, "payload cannot be null");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.publishedAt = null; // Unpublished by default
    }

    // ============ Accessors ============

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ============ Business Methods ============

    /**
     * Mark this event as successfully published.
     * Called by OutboxEventPublisher after RabbitMQ publishes event.
     */
    public void markAsPublished() {
        this.publishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Check if event has been published.
     */
    public boolean isPublished() {
        return publishedAt != null;
    }

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEvent)) return false;
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "id=" + id +
                ", eventType='" + eventType + '\'' +
                ", aggregateId=" + aggregateId +
                ", aggregateType='" + aggregateType + '\'' +
                ", isPublished=" + (publishedAt != null) +
                ", createdAt=" + createdAt +
                '}';
    }
}
