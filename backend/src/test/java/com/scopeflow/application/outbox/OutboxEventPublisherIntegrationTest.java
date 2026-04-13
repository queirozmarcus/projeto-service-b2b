package com.scopeflow.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.application.fixtures.MessagingIntegrationTestBase;
import com.scopeflow.core.domain.user.event.UserRegisteredEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration tests for the Outbox pattern (Sprint 4, Phase 1 + D8).
 *
 * Validates:
 * - OutboxEvent persists correctly in DB with all required fields
 * - OutboxEventPublisher.publishPendingEvents() finds unpublished events
 * - After publish, published_at timestamp is set (not null)
 * - Multiple events are processed in FIFO order (by created_at)
 * - Events with unknown eventType are marked published to prevent infinite retry
 *
 * Infrastructure: PostgreSQL (Testcontainers) + real Flyway V1-V6.
 * RabbitMQ used for actual publish; listener side effects mocked.
 */
@DisplayName("OutboxEventPublisher — Integration Tests")
class OutboxEventPublisherIntegrationTest extends MessagingIntegrationTestBase {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @AfterEach
    void cleanup() {
        outboxEventRepository.deleteAll();
    }

    @Nested
    @DisplayName("Persistence")
    class PersistenceTests {

        @Test
        @DisplayName("should persist OutboxEvent with null published_at and correct metadata")
        void shouldPersistUnpublishedEventWithCorrectMetadata() throws Exception {
            // Given
            UUID aggregateId = UUID.randomUUID();
            UserRegisteredEvent domainEvent = UserRegisteredEvent.of(
                    aggregateId,
                    "alice@agency.com",
                    "Alice",
                    UUID.randomUUID()
            );

            // When
            outboxService.persist(
                    aggregateId,
                    "User",
                    domainEvent
            );

            // Then
            List<OutboxEvent> found = outboxEventRepository.findUnpublishedByAggregateId(aggregateId);
            assertThat(found).hasSize(1);

            OutboxEvent saved = found.get(0);
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getEventType())
                    .isEqualTo("com.scopeflow.core.domain.user.event.UserRegisteredEvent");
            assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
            assertThat(saved.getAggregateType()).isEqualTo("User");
            assertThat(saved.getPayload()).contains("alice@agency.com");
            assertThat(saved.getPublishedAt()).isNull();           // not yet published
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("should persist multiple events for the same aggregate in order")
        void shouldPersistMultipleEventsInOrder() {
            // Given
            UUID aggregateId = UUID.randomUUID();

            outboxService.persist(aggregateId, "User",
                    UserRegisteredEvent.of(aggregateId, "a@b.com", "A", UUID.randomUUID()));
            outboxService.persist(aggregateId, "User",
                    UserRegisteredEvent.of(aggregateId, "b@b.com", "B", UUID.randomUUID()));
            outboxService.persist(aggregateId, "User",
                    UserRegisteredEvent.of(aggregateId, "c@b.com", "C", UUID.randomUUID()));

            // When
            List<OutboxEvent> unpublished = outboxEventRepository
                    .findUnpublishedByAggregateId(aggregateId);

            // Then — ordered by created_at ASC (FIFO)
            assertThat(unpublished).hasSize(3);
            for (int i = 0; i < unpublished.size() - 1; i++) {
                assertThat(unpublished.get(i).getCreatedAt())
                        .isBeforeOrEqualTo(unpublished.get(i + 1).getCreatedAt());
            }
        }

        @Test
        @DisplayName("countUnpublished reflects only unpublished events")
        void shouldCountOnlyUnpublishedEvents() {
            // Given — 2 unpublished
            UUID agg1 = UUID.randomUUID();
            UUID agg2 = UUID.randomUUID();
            outboxService.persist(agg1, "User",
                    UserRegisteredEvent.of(agg1, "x@y.com", "X", UUID.randomUUID()));
            outboxService.persist(agg2, "User",
                    UserRegisteredEvent.of(agg2, "y@y.com", "Y", UUID.randomUUID()));

            long countBefore = outboxEventRepository.countUnpublished();
            assertThat(countBefore).isGreaterThanOrEqualTo(2);

            // When — mark one as published
            List<OutboxEvent> events = outboxEventRepository.findUnpublishedByAggregateId(agg1);
            OutboxEvent event = events.get(0);
            event.markAsPublished();
            outboxEventRepository.save(event);

            // Then — count decreases by 1
            assertThat(outboxEventRepository.countUnpublished())
                    .isEqualTo(countBefore - 1);
        }
    }

    @Nested
    @DisplayName("Scheduler publish cycle")
    class PublishCycleTests {

        @Test
        @DisplayName("publishPendingEvents marks event as published after successful dispatch")
        void shouldMarkEventAsPublishedAfterDispatch() throws Exception {
            // Given — persist event with full class name matching UserRegisteredEvent
            UUID aggregateId = UUID.randomUUID();
            String payload = objectMapper.writeValueAsString(
                    UserRegisteredEvent.of(aggregateId, "test@test.com", "Test", UUID.randomUUID())
            );
            OutboxEvent outboxEvent = new OutboxEvent(
                    "com.scopeflow.core.domain.user.event.UserRegisteredEvent",
                    aggregateId,
                    "User",
                    payload
            );
            outboxEventRepository.save(outboxEvent);

            assertThat(outboxEvent.isPublished()).isFalse();

            // When — trigger one scheduler cycle manually
            outboxEventPublisher.publishPendingEvents();

            // Then — event is now marked as published
            OutboxEvent refreshed = outboxEventRepository.findById(outboxEvent.getId())
                    .orElseThrow();
            assertThat(refreshed.isPublished()).isTrue();
            assertThat(refreshed.getPublishedAt()).isNotNull();
            assertThat(refreshed.getPublishedAt()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("events with unknown class are marked published to avoid infinite retry")
        void shouldMarkEventPublishedWhenClassNotFound() {
            // Given — event with a non-existent class name
            UUID aggregateId = UUID.randomUUID();
            OutboxEvent staleEvent = new OutboxEvent(
                    "com.scopeflow.NonExistentEvent",   // class doesn't exist
                    aggregateId,
                    "Ghost",
                    "{\"id\":\"" + aggregateId + "\"}"
            );
            outboxEventRepository.save(staleEvent);

            // When
            outboxEventPublisher.publishPendingEvents();

            // Then — event is still marked published (to prevent infinite loop)
            OutboxEvent refreshed = outboxEventRepository.findById(staleEvent.getId())
                    .orElseThrow();
            assertThat(refreshed.isPublished()).isTrue();
        }

        @Test
        @DisplayName("multiple unpublished events are all processed in a single cycle")
        void shouldProcessAllPendingEventsInOneCycle() throws Exception {
            // Given — 3 unpublished events for different aggregates
            for (int i = 0; i < 3; i++) {
                UUID id = UUID.randomUUID();
                String payload = objectMapper.writeValueAsString(
                        UserRegisteredEvent.of(id, "user" + i + "@test.com", "User " + i, UUID.randomUUID())
                );
                outboxEventRepository.save(new OutboxEvent(
                        "com.scopeflow.core.domain.user.event.UserRegisteredEvent",
                        id, "User", payload
                ));
            }
            long before = outboxEventRepository.countUnpublished();
            assertThat(before).isGreaterThanOrEqualTo(3);

            // When
            outboxEventPublisher.publishPendingEvents();

            // Then — all events in this run are published (queue depth drops to 0 for our events)
            List<OutboxEvent> remaining = outboxEventRepository.findUnpublished();
            assertThat(remaining).isEmpty();
        }
    }
}
