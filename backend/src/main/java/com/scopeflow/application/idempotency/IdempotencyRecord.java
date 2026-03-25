package com.scopeflow.application.idempotency;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Idempotency Record entity (Idempotent Consumer Pattern — D9).
 *
 * Tracks processed events to prevent duplicate side effects.
 *
 * Example Scenario:
 * 1. UserRegisteredEvent arrives → UserRegistrationListener processes
 * 2. Listener sends welcome email → inserts IdempotencyRecord(listener="user-registration", key="user-123")
 * 3. Email service crashes, but DB transaction committed
 * 4. Message broker retries → UserRegisteredEvent arrives again
 * 5. Listener checks IdempotencyRecord: found! → skips processing
 * 6. Welcome email sent only ONCE (not twice)
 *
 * Key Properties:
 * - (listener_id, idempotency_key) is unique → prevents duplicate inserts
 * - processed_at timestamp for auditing and cleanup
 * - result_data (optional) stores side effect results for reference
 *
 * Usage in Listener:
 * ```java
 * @RabbitListener(queues = "user.registered")
 * public void onUserRegistered(UserRegisteredEvent event) {
 *     String idempotencyKey = "user-registration:" + event.userId();
 *
 *     // Check if already processed
 *     if (idempotencyService.isProcessed("user-registration-listener", idempotencyKey)) {
 *         log.info("Already processed, skipping");
 *         return;
 *     }
 *
 *     // Process: send email, generate PDF, etc.
 *     emailService.sendWelcomeEmail(event.email());
 *
 *     // Mark as processed (in same TX if possible)
 *     idempotencyService.markAsProcessed("user-registration-listener", idempotencyKey);
 * }
 * ```
 *
 * Design Notes:
 * - listener_id distinguishes different listeners processing the same event
 * - idempotency_key includes aggregate ID (user, proposal, etc.) for uniqueness
 * - Unique constraint enforces single processing per (listener, key) pair
 * - result_data is optional but useful for audit trails
 */
@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_listener_idempotency",
            columnNames = {"listener_id", "idempotency_key"}
        )
    }
)
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Logical ID of the listener (e.g., "user-registration-listener").
     * Allows different listeners to independently track event processing.
     */
    @Column(nullable = false, length = 100)
    private String listenerId;

    /**
     * Unique key for this event instance.
     * Format: "listener-id:aggregate-id:event-id" or similar.
     * Must be deterministic and uniquely identify the event.
     */
    @Column(nullable = false, length = 255)
    private String idempotencyKey;

    /**
     * Timestamp when this event was processed.
     * Used for auditing, cleanup (delete old records), and ordering.
     */
    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    /**
     * Optional JSON data from the side effect (e.g., email ID, PDF URL, webhook response).
     * Stored as JSONB in PostgreSQL for queryability.
     * Useful for auditing and potential future result caching.
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultData;

    /**
     * Empty constructor for Hibernate.
     */
    public IdempotencyRecord() {
    }

    /**
     * Constructor for creating a new idempotency record.
     */
    public IdempotencyRecord(String listenerId, String idempotencyKey) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey cannot be null");
        this.processedAt = Instant.now();
    }

    /**
     * Constructor with result data.
     */
    public IdempotencyRecord(String listenerId, String idempotencyKey, String resultData) {
        this(listenerId, idempotencyKey);
        this.resultData = resultData;
    }

    // ============ Accessors ============

    public UUID getId() {
        return id;
    }

    public String getListenerId() {
        return listenerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getResultData() {
        return resultData;
    }

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotencyRecord)) return false;
        IdempotencyRecord that = (IdempotencyRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "IdempotencyRecord{" +
                "id=" + id +
                ", listenerId='" + listenerId + '\'' +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}
