package com.scopeflow.application.listener;

import com.scopeflow.application.idempotency.IdempotencyService;
import com.scopeflow.core.domain.user.event.UserRegisteredEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event Listener: UserRegisteredEvent → Send Welcome Email.
 *
 * Responsibility:
 * - Listens for UserRegisteredEvent on RabbitMQ queue: user.registered
 * - Sends welcome email to new user
 * - Idempotent: won't send duplicate emails on message redelivery
 *
 * Idempotency Key: "user-{userId}"
 * - Unique per user
 * - Same user registering twice would create two records (different event IDs)
 * - But if the same event is redelivered, email is sent only once
 *
 * Error Handling:
 * - Email service failures: RabbitMQ retry (configured in application.yml)
 * - After 3 retries: message goes to DLQ for manual inspection
 * - Never loses user registration (domain event already persisted)
 *
 * Performance:
 * - Non-blocking: async listener thread
 * - Email sent via SES (integration with external service)
 * - Timeout: 30s (configurable)
 */
@Component
public class UserRegistrationListener {
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationListener.class);
    private static final String LISTENER_ID = "user-registration-listener";

    private final IdempotencyService idempotencyService;
    // private final EmailService emailService;  // TODO: Wire EmailService in Phase 4

    public UserRegistrationListener(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * Handle UserRegisteredEvent.
     *
     * Decorated with @RabbitListener to automatically convert AMQP message
     * from queue "user.registered" to UserRegisteredEvent domain object.
     *
     * @param event the UserRegisteredEvent from RabbitMQ
     */
    @RabbitListener(queues = "user.registered")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        UUID userId = event.userId();
        String idempotencyKey = "user-" + userId;

        // Step 1: Check if already processed (idempotent)
        if (idempotencyService.isProcessed(LISTENER_ID, idempotencyKey)) {
            logger.info(
                    "UserRegisteredEvent already processed (idempotent check), skipping. userId={}",
                    userId
            );
            return;
        }

        try {
            // Step 2: Send welcome email (Phase 4 implementation)
            // TODO: emailService.sendWelcomeEmail(
            //        event.email(),
            //        event.workspaceId(),
            //        "Welcome to ScopeFlow!"
            // );

            logger.info(
                    "Welcome email would be sent to: {} for user: {}",
                    event.email(),
                    userId
            );

            // Step 3: Mark as processed (before TX commits)
            idempotencyService.markAsProcessed(LISTENER_ID, idempotencyKey);

            logger.info("UserRegisteredEvent processed successfully. userId={}", userId);

        } catch (Exception e) {
            // Log the error; RabbitMQ will retry based on configuration
            logger.error(
                    "Failed to process UserRegisteredEvent. userId={}, will retry",
                    userId,
                    e
            );
            // Re-throw to trigger RabbitMQ retry mechanism
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }
}
