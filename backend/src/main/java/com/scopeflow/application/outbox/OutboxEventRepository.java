package com.scopeflow.application.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for OutboxEvent persistence.
 *
 * Handles CRUD and query operations for the Outbox table.
 * Primary use: OutboxEventPublisher scheduler polls findUnpublished() every 5s.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find all unpublished events, ordered by creation time (FIFO).
     *
     * Used by OutboxEventPublisher.publishPendingEvents() scheduler.
     * Query: SELECT * FROM outbox_event WHERE published_at IS NULL ORDER BY created_at ASC
     *
     * @return list of unpublished events (oldest first)
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.publishedAt IS NULL ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findUnpublished();

    /**
     * Find unpublished events for a specific aggregate (for debugging/replay).
     *
     * @param aggregateId the aggregate ID (e.g., userId, proposalId)
     * @return list of unpublished events for that aggregate
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.publishedAt IS NULL AND oe.aggregateId = :aggregateId ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findUnpublishedByAggregateId(UUID aggregateId);

    /**
     * Find unpublished events of a specific type (for per-listener batching).
     *
     * @param eventType the fully qualified event class name
     * @return list of unpublished events of that type
     */
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.publishedAt IS NULL AND oe.eventType = :eventType ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findUnpublishedByEventType(String eventType);

    /**
     * Count unpublished events (useful for monitoring backlog).
     *
     * @return number of events awaiting publication
     */
    @Query("SELECT COUNT(oe) FROM OutboxEvent oe WHERE oe.publishedAt IS NULL")
    long countUnpublished();
}
