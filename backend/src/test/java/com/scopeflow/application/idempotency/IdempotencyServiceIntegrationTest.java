package com.scopeflow.application.idempotency;

import com.scopeflow.application.fixtures.MessagingIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for IdempotencyService (Sprint 4, Phase 2 + D9).
 *
 * Validates:
 * - isProcessed() returns false before any record exists
 * - markAsProcessed() persists record with (listenerId, idempotencyKey) unique constraint
 * - isProcessed() returns true after markAsProcessed()
 * - Duplicate markAsProcessed() calls are idempotent (no exception)
 * - Result data is stored and retrievable via getResultData()
 * - countProcessed() counts only records for the given listenerId
 *
 * Infrastructure: PostgreSQL (Testcontainers) + Flyway V6.
 */
@DisplayName("IdempotencyService — Integration Tests")
class IdempotencyServiceIntegrationTest extends MessagingIntegrationTestBase {

    @Autowired
    private IdempotencyService idempotencyService;

    private static final String LISTENER_ID = "test-listener";

    @AfterEach
    void cleanup() {
        idempotencyRepository.deleteAll();
    }

    @Nested
    @DisplayName("isProcessed")
    class IsProcessedTests {

        @Test
        @DisplayName("returns false when no record exists")
        void shouldReturnFalseWhenNoRecordExists() {
            // When / Then
            assertThat(idempotencyService.isProcessed(LISTENER_ID, "key-never-seen"))
                    .isFalse();
        }

        @Test
        @DisplayName("returns true after markAsProcessed")
        void shouldReturnTrueAfterMarkAsProcessed() {
            // Given
            String key = "user-" + UUID.randomUUID();

            // When
            idempotencyService.markAsProcessed(LISTENER_ID, key);

            // Then
            assertThat(idempotencyService.isProcessed(LISTENER_ID, key)).isTrue();
        }

        @Test
        @DisplayName("different listeners with same key are independent")
        void shouldNotConflictAcrossDifferentListeners() {
            // Given — same key, two different listeners
            String key = "proposal-" + UUID.randomUUID();
            idempotencyService.markAsProcessed("listener-a", key);

            // Then — listener-b has NOT processed it
            assertThat(idempotencyService.isProcessed("listener-b", key)).isFalse();
            assertThat(idempotencyService.isProcessed("listener-a", key)).isTrue();
        }

        @Test
        @DisplayName("different keys for same listener are independent")
        void shouldNotConflictAcrossDifferentKeys() {
            // Given
            String key1 = "event-" + UUID.randomUUID();
            String key2 = "event-" + UUID.randomUUID();
            idempotencyService.markAsProcessed(LISTENER_ID, key1);

            // Then — key2 not processed
            assertThat(idempotencyService.isProcessed(LISTENER_ID, key1)).isTrue();
            assertThat(idempotencyService.isProcessed(LISTENER_ID, key2)).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsProcessed")
    class MarkAsProcessedTests {

        @Test
        @DisplayName("persists record with correct listener and key")
        void shouldPersistRecordWithCorrectFields() {
            // Given
            String key = "user-" + UUID.randomUUID();

            // When
            idempotencyService.markAsProcessed(LISTENER_ID, key);

            // Then — verify record in DB directly
            Optional<IdempotencyRecord> record =
                    idempotencyRepository.findByListenerIdAndKey(LISTENER_ID, key);
            assertThat(record).isPresent();
            assertThat(record.get().getListenerId()).isEqualTo(LISTENER_ID);
            assertThat(record.get().getIdempotencyKey()).isEqualTo(key);
            assertThat(record.get().getProcessedAt()).isNotNull();
            assertThat(record.get().getResultData()).isNull();  // no result data in this overload
        }

        @Test
        @DisplayName("duplicate call is silently ignored (idempotent insert)")
        void shouldSilentlyIgnoreDuplicateCalls() {
            // Given
            String key = "user-" + UUID.randomUUID();
            idempotencyService.markAsProcessed(LISTENER_ID, key);

            // When — same call again (simulates broker redelivery race condition)
            idempotencyService.markAsProcessed(LISTENER_ID, key);

            // Then — still exactly one record in DB
            long count = idempotencyRepository.countByListenerId(LISTENER_ID);
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("stores result data when provided")
        void shouldStoreResultDataWhenProvided() {
            // Given
            String key = "proposal-" + UUID.randomUUID();
            String resultData = "{\"pdfUrl\":\"https://s3.example.com/proposal.pdf\",\"emailSent\":true}";

            // When
            idempotencyService.markAsProcessed(LISTENER_ID, key, resultData);

            // Then
            Optional<String> retrieved = idempotencyService.getResultData(LISTENER_ID, key);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get()).isEqualTo(resultData);
        }

        @Test
        @DisplayName("getResultData returns empty when key not found")
        void shouldReturnEmptyWhenKeyNotFound() {
            Optional<String> result = idempotencyService.getResultData(LISTENER_ID, "ghost-key");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countProcessed")
    class CountProcessedTests {

        @Test
        @DisplayName("counts only records for the given listenerId")
        void shouldCountOnlyForGivenListener() {
            // Given
            idempotencyService.markAsProcessed("listener-x", "key-1");
            idempotencyService.markAsProcessed("listener-x", "key-2");
            idempotencyService.markAsProcessed("listener-y", "key-3");

            // When / Then
            assertThat(idempotencyService.countProcessed("listener-x")).isEqualTo(2L);
            assertThat(idempotencyService.countProcessed("listener-y")).isEqualTo(1L);
            assertThat(idempotencyService.countProcessed("listener-z")).isEqualTo(0L);
        }
    }
}
