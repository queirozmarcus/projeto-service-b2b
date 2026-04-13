package com.scopeflow.application.listener;

import com.scopeflow.application.fixtures.MessagingEventFixtures;
import com.scopeflow.application.fixtures.MessagingIntegrationTestBase;
import com.scopeflow.application.idempotency.IdempotencyRecord;
import com.scopeflow.application.port.out.EmailException;
import com.scopeflow.application.port.out.EmailService;
import com.scopeflow.core.domain.user.event.UserRegisteredEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Integration tests for UserRegistrationListener (Sprint 4, Phase 2).
 *
 * Validates the full async flow:
 * 1. UserRegisteredEvent arrives on queue "user.registered"
 * 2. Listener performs idempotency check
 * 3. Sends welcome email via EmailService
 * 4. Marks event as processed in idempotency_record table
 *
 * Infrastructure:
 * - RabbitMQ (Testcontainers) — real broker, real queue binding
 * - PostgreSQL (Testcontainers) — real idempotency_record table
 * - EmailService mocked via @MockBean — no real SES calls
 *
 * Async assertions use Awaitility with a generous 10s window to account for
 * listener container startup + message delivery latency in CI.
 */
@DisplayName("UserRegistrationListener — Integration Tests")
class UserRegistrationListenerIntegrationTest extends MessagingIntegrationTestBase {

    @MockBean
    private EmailService emailService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void cleanup() {
        idempotencyRepository.deleteAll();
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPathTests {

        @Test
        @DisplayName("should send welcome email and persist idempotency record")
        void shouldSendWelcomeEmailAndMarkAsProcessed() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            UserRegisteredEvent event = MessagingEventFixtures.userRegisteredEvent(userId, workspaceId);

            // When — send message directly to the queue (simulates Outbox poller publishing)
            rabbitTemplate.convertAndSend("user.registered", event);

            // Then — wait for listener to process
            await().atMost(10, SECONDS).untilAsserted(() -> {
                // Welcome email was sent with correct parameters
                ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<UUID> workspaceCaptor = ArgumentCaptor.forClass(UUID.class);

                verify(emailService, times(1))
                        .sendWelcomeEmail(
                                emailCaptor.capture(),
                                nameCaptor.capture(),
                                workspaceCaptor.capture()
                        );

                assertThat(emailCaptor.getValue()).isEqualTo(MessagingEventFixtures.DEFAULT_USER_EMAIL);
                assertThat(nameCaptor.getValue()).isEqualTo(MessagingEventFixtures.DEFAULT_USER_NAME);
                assertThat(workspaceCaptor.getValue()).isEqualTo(workspaceId);
            });

            // And — idempotency record persisted
            await().atMost(10, SECONDS).untilAsserted(() -> {
                Optional<IdempotencyRecord> record =
                        idempotencyRepository.findByListenerIdAndKey(
                                "user-registration-listener",
                                "user-" + userId
                        );
                assertThat(record).isPresent();
                assertThat(record.get().getProcessedAt()).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Idempotency — duplicate message protection")
    class IdempotencyTests {

        @Test
        @DisplayName("should send email only once when same event arrives twice (broker redelivery)")
        void shouldSendEmailOnlyOnceOnRedelivery() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UserRegisteredEvent event = MessagingEventFixtures.userRegisteredEvent(userId, UUID.randomUUID());

            // When — first delivery
            rabbitTemplate.convertAndSend("user.registered", event);

            // Wait until first processing completes
            await().atMost(10, SECONDS).untilAsserted(() ->
                    verify(emailService, times(1))
                            .sendWelcomeEmail(anyString(), anyString(), any(UUID.class))
            );

            // Reset mock call count tracking but keep the idempotency record in DB
            clearInvocations(emailService);

            // When — second delivery (simulating broker redelivery)
            rabbitTemplate.convertAndSend("user.registered", event);

            // Then — listener skips processing; email not sent again
            // Wait a bit to allow listener time to process if it were going to
            Thread.sleep(3000);
            verify(emailService, never())
                    .sendWelcomeEmail(anyString(), anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("different user IDs generate independent idempotency records")
        void shouldProcessDifferentUsersIndependently() throws Exception {
            // Given — two different users
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            UserRegisteredEvent event1 = MessagingEventFixtures.userRegisteredEvent(userId1, UUID.randomUUID());
            UserRegisteredEvent event2 = MessagingEventFixtures.userRegisteredEvent(userId2, UUID.randomUUID());

            // When
            rabbitTemplate.convertAndSend("user.registered", event1);
            rabbitTemplate.convertAndSend("user.registered", event2);

            // Then — both emails sent, both idempotency records created
            await().atMost(15, SECONDS).untilAsserted(() -> {
                verify(emailService, times(2))
                        .sendWelcomeEmail(anyString(), anyString(), any(UUID.class));

                assertThat(idempotencyRepository
                        .findByListenerIdAndKey("user-registration-listener", "user-" + userId1))
                        .isPresent();
                assertThat(idempotencyRepository
                        .findByListenerIdAndKey("user-registration-listener", "user-" + userId2))
                        .isPresent();
            });
        }
    }

    @Nested
    @DisplayName("Error handling — retry trigger")
    class ErrorHandlingTests {

        @Test
        @DisplayName("email service failure re-throws to trigger RabbitMQ retry")
        void shouldRetryWhenEmailServiceThrows() throws Exception {
            // Given — email service throws on first attempt, succeeds on retry
            UUID userId = UUID.randomUUID();
            UserRegisteredEvent event = MessagingEventFixtures.userRegisteredEvent(userId, UUID.randomUUID());

            doThrow(new EmailException("SES unavailable"))
                    .doNothing()    // succeeds on retry
                    .when(emailService).sendWelcomeEmail(anyString(), anyString(), any(UUID.class));

            // When
            rabbitTemplate.convertAndSend("user.registered", event);

            // Then — listener eventually succeeds after retry
            await().atMost(15, SECONDS).untilAsserted(() -> {
                // Email service was called at least twice (initial + 1 retry)
                verify(emailService, atLeast(2))
                        .sendWelcomeEmail(anyString(), anyString(), any(UUID.class));

                // Idempotency record created after successful retry
                assertThat(idempotencyRepository
                        .findByListenerIdAndKey("user-registration-listener", "user-" + userId))
                        .isPresent();
            });
        }
    }
}
