package com.scopeflow.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Service for persisting domain events to the Outbox table.
 *
 * Responsibility: Provide a convenient API for application services to
 * persist domain events within the same transaction as domain changes.
 *
 * Usage (in a use case):
 * ```java
 * @Transactional
 * public void execute(Command cmd) {
 *     // 1. Modify aggregate
 *     user.register(cmd.email(), cmd.password());
 *     userRepository.save(user);
 *
 *     // 2. Persist event to Outbox
 *     outboxService.persist(user.getDomainEvents());
 * }
 * ```
 *
 * Pattern: Transactional Outbox (D8)
 * - Event is inserted in DB within the same TX as domain changes
 * - TX commits: both aggregate and event are durable
 * - OutboxEventPublisher scheduler polls and publishes asynchronously
 * - Ensures: atomicity + eventual consistency
 *
 * Error Handling:
 * - If persist() throws, entire TX rolls back (including domain changes)
 * - This is desired: keep domain and events in sync
 */
@Service
public class OutboxService {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persist a domain event to the Outbox table.
     *
     * The event is stored as JSON (JSONB in PostgreSQL) for later deserialization
     * and publishing by OutboxEventPublisher.
     *
     * @param eventType the fully qualified event class name
     * @param aggregateId the ID of the aggregate that triggered the event
     * @param aggregateType the type of aggregate (User, Proposal, etc.)
     * @param event the domain event object
     * @throws IllegalArgumentException if any parameter is null
     * @throws RuntimeException if JSON serialization fails
     */
    public void persist(String eventType, UUID aggregateId, String aggregateType, Object event) {
        if (eventType == null || aggregateId == null || aggregateType == null || event == null) {
            throw new IllegalArgumentException(
                    "eventType, aggregateId, aggregateType, and event cannot be null"
            );
        }

        try {
            // Serialize domain event to JSON
            String payload = objectMapper.writeValueAsString(event);

            // Create and persist outbox event
            OutboxEvent outboxEvent = new OutboxEvent(eventType, aggregateId, aggregateType, payload);
            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to persist outbox event: type=" + eventType +
                            ", aggregateId=" + aggregateId,
                    e
            );
        }
    }

    /**
     * Persist a domain event with inferred types (convenience method).
     *
     * @param aggregateId the ID of the aggregate that triggered the event
     * @param aggregateType the type of aggregate (User, Proposal, etc.)
     * @param event the domain event object
     */
    public void persist(UUID aggregateId, String aggregateType, Object event) {
        String eventType = event.getClass().getName();
        persist(eventType, aggregateId, aggregateType, event);
    }

    /**
     * Get the current Outbox queue depth (for monitoring).
     *
     * @return number of unpublished events
     */
    public long getUnpublishedCount() {
        return outboxRepository.countUnpublished();
    }

    /**
     * Get unpublished events for a specific aggregate (for debugging).
     *
     * @param aggregateId the aggregate ID
     * @return list of unpublished events for that aggregate
     */
    public java.util.List<OutboxEvent> getUnpublishedForAggregate(UUID aggregateId) {
        return outboxRepository.findUnpublishedByAggregateId(aggregateId);
    }
}
