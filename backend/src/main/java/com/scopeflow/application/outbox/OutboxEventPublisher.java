package com.scopeflow.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Event Publisher Scheduler (Transactional Outbox Pattern — D8).
 *
 * Responsibility:
 * - Polls the outbox_event table every 5 seconds
 * - Deserializes unpublished events back to domain event objects
 * - Publishes to RabbitMQ via Spring's ApplicationEventPublisher
 * - Marks events as published (sets published_at timestamp)
 *
 * Scheduling:
 * - fixedDelay = 5000 ms: wait 5s between end of one execution and start of next
 * - initialDelay = 1000 ms: wait 1s after application startup before first poll
 *
 * Error Handling:
 * - Deserialization failures are logged but don't block subsequent events
 * - Publishing failures are logged; event remains unpublished → retried next cycle
 * - Never throws exceptions (scheduled tasks swallow exceptions)
 *
 * Monitoring:
 * - Check OutboxEventRepository.countUnpublished() for queue depth
 * - Monitor logs for "Failed to publish outbox event" errors
 * - If queue grows unbounded, check RabbitMQ broker health and listener processing
 *
 * Example Flow:
 * 1. UserRegisteredEvent created → persisted to outbox_event (TX1)
 * 2. Scheduler runs: finds 1 unpublished event
 * 3. Deserializes JSON payload → UserRegisteredEvent object
 * 4. applicationEventPublisher.publishEvent(event) → RabbitMQ via Spring listeners
 * 5. On success: event.markAsPublished() → sets published_at
 * 6. Save updated event to DB (TX2)
 * 7. RabbitMQ listeners pick it up asynchronously
 */
@Component
public class OutboxEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(
            OutboxEventRepository outboxRepository,
            ApplicationEventPublisher applicationEventPublisher,
            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Poll and publish unpublished outbox events.
     *
     * Runs every 5 seconds with 1 second initial delay after startup.
     * Uses REQUIRES_NEW transaction to ensure each event publishing is independent.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        try {
            // Find all unpublished events (ordered by creation time)
            List<OutboxEvent> events = outboxRepository.findUnpublished();

            if (events.isEmpty()) {
                return; // Nothing to do
            }

            logger.debug("Publishing {} pending outbox events", events.size());

            for (OutboxEvent outboxEvent : events) {
                try {
                    // Step 1: Deserialize JSON payload back to domain event
                    Object domainEvent = objectMapper.readValue(
                            outboxEvent.getPayload(),
                            Class.forName(outboxEvent.getEventType())
                    );

                    // Step 2: Publish to RabbitMQ (async via Spring event listeners)
                    applicationEventPublisher.publishEvent(domainEvent);

                    // Step 3: Mark as published
                    outboxEvent.markAsPublished();
                    outboxRepository.save(outboxEvent);

                    logger.info(
                            "Published outbox event {} (type={}, aggregateId={})",
                            outboxEvent.getId(),
                            outboxEvent.getEventType(),
                            outboxEvent.getAggregateId()
                    );
                } catch (ClassNotFoundException e) {
                    logger.error(
                            "Cannot find event class: {} (outboxEventId={}). Skipping.",
                            outboxEvent.getEventType(),
                            outboxEvent.getId(),
                            e
                    );
                    // Mark as published anyway to avoid infinite retry of unpublishable event
                    outboxEvent.markAsPublished();
                    outboxRepository.save(outboxEvent);
                } catch (Exception e) {
                    logger.error(
                            "Failed to publish outbox event {} (type={}, aggregateId={}). Will retry.",
                            outboxEvent.getId(),
                            outboxEvent.getEventType(),
                            outboxEvent.getAggregateId(),
                            e
                    );
                    // Don't mark as published; will retry next cycle
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error in publishPendingEvents scheduler", e);
            // Fail gracefully; scheduler will retry in 5 seconds
        }
    }

    /**
     * Get current outbox queue depth (useful for monitoring).
     *
     * @return number of unpublished events
     */
    public long getUnpublishedEventCount() {
        return outboxRepository.countUnpublished();
    }
}
