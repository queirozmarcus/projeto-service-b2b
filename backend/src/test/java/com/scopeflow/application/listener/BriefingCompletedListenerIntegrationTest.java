package com.scopeflow.application.listener;

import com.scopeflow.application.fixtures.MessagingEventFixtures;
import com.scopeflow.application.fixtures.MessagingIntegrationTestBase;
import com.scopeflow.application.idempotency.IdempotencyRecord;
import com.scopeflow.core.domain.briefing.event.BriefingCompletedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for BriefingCompletedListener (Sprint 4, Phase 2).
 *
 * The listener performs a lightweight action (fallback question generation stub)
 * and idempotency tracking. No external services are called (no mocks required).
 *
 * Validates:
 * 1. BriefingCompletedEvent arrives on queue "briefing.completed"
 * 2. Listener processes and marks idempotency record
 * 3. Duplicate events are skipped (idempotency check)
 *
 * Infrastructure:
 * - RabbitMQ (Testcontainers) — real broker
 * - PostgreSQL (Testcontainers) — real idempotency_record table
 */
@DisplayName("BriefingCompletedListener — Integration Tests")
class BriefingCompletedListenerIntegrationTest extends MessagingIntegrationTestBase {

    @AfterEach
    void cleanup() {
        idempotencyRepository.deleteAll();
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPathTests {

        @Test
        @DisplayName("should process BriefingCompletedEvent and persist idempotency record")
        void shouldProcessEventAndMarkAsProcessed() {
            // Given
            UUID sessionId = UUID.randomUUID();
            BriefingCompletedEvent event = MessagingEventFixtures.briefingCompletedEvent(sessionId);

            // When
            rabbitTemplate.convertAndSend("briefing.completed", event);

            // Then — idempotency record created for this session
            await().atMost(10, SECONDS).untilAsserted(() -> {
                Optional<IdempotencyRecord> record =
                        idempotencyRepository.findByListenerIdAndKey(
                                "briefing-completed-listener",
                                "briefing-" + sessionId
                        );
                assertThat(record).isPresent();
                assertThat(record.get().getListenerId()).isEqualTo("briefing-completed-listener");
                assertThat(record.get().getIdempotencyKey()).isEqualTo("briefing-" + sessionId);
                assertThat(record.get().getProcessedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("should handle high-score briefing and low-score briefing independently")
        void shouldProcessEventsForDifferentSessions() {
            // Given
            UUID session1 = UUID.randomUUID();
            UUID session2 = UUID.randomUUID();

            BriefingCompletedEvent highScore = BriefingCompletedEvent.of(
                    session1, UUID.randomUUID(), UUID.randomUUID(), 95, "No gaps");
            BriefingCompletedEvent lowScore = BriefingCompletedEvent.of(
                    session2, UUID.randomUUID(), UUID.randomUUID(), 82, "Minor gaps in timeline");

            // When
            rabbitTemplate.convertAndSend("briefing.completed", highScore);
            rabbitTemplate.convertAndSend("briefing.completed", lowScore);

            // Then — both sessions have idempotency records
            await().atMost(15, SECONDS).untilAsserted(() -> {
                assertThat(idempotencyRepository
                        .findByListenerIdAndKey("briefing-completed-listener", "briefing-" + session1))
                        .isPresent();
                assertThat(idempotencyRepository
                        .findByListenerIdAndKey("briefing-completed-listener", "briefing-" + session2))
                        .isPresent();
            });
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("should process a session only once when event arrives twice")
        void shouldSkipDuplicateEventForSameSession() throws Exception {
            // Given
            UUID sessionId = UUID.randomUUID();
            BriefingCompletedEvent event = MessagingEventFixtures.briefingCompletedEvent(sessionId);

            // When — first delivery
            rabbitTemplate.convertAndSend("briefing.completed", event);
            await().atMost(10, SECONDS).untilAsserted(() ->
                    assertThat(idempotencyRepository
                            .findByListenerIdAndKey("briefing-completed-listener", "briefing-" + sessionId))
                            .isPresent()
            );

            // When — second delivery (simulating broker redelivery)
            rabbitTemplate.convertAndSend("briefing.completed", event);
            Thread.sleep(3000);

            // Then — still exactly one record in DB
            assertThat(idempotencyRepository.countByListenerId("briefing-completed-listener"))
                    .isEqualTo(1L);
        }
    }
}
