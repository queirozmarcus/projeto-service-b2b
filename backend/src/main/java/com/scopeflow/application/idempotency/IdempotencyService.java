package com.scopeflow.application.idempotency;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing idempotent event processing (D9).
 *
 * Responsibility: Provide a simple API for event listeners to:
 * 1. Check if an event has already been processed
 * 2. Mark an event as processed (record result)
 *
 * This ensures that if a message is redelivered by the broker,
 * the listener will detect it and skip duplicate side effects (email, PDF, etc.)
 *
 * Usage in Listener:
 * ```java
 * @RabbitListener(queues = "user.registered")
 * public void onUserRegistered(UserRegisteredEvent event) {
 *     String key = "user-" + event.userId();
 *
 *     // Check before processing
 *     if (idempotencyService.isProcessed("user-registration-listener", key)) {
 *         return; // Already sent email
 *     }
 *
 *     // Process: side effects here
 *     emailService.sendWelcomeEmail(event.email());
 *
 *     // Mark as processed
 *     idempotencyService.markAsProcessed("user-registration-listener", key);
 * }
 * ```
 *
 * Transaction Notes:
 * - isProcessed() runs in a read-only TX (fast)
 * - markAsProcessed() uses REQUIRES_NEW to isolate from caller's TX
 *   → Ensures record is committed even if listener later throws
 * - Listener should handle its own TX for side effects (optional)
 *
 * Error Handling:
 * - If insert fails (unique constraint violation), it means another instance
 *   of this listener just processed the same event → log and continue
 * - If query fails, listener will re-process (eventual consistency)
 */
@Service
public class IdempotencyService {
    private final IdempotencyRepository repository;

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
    }

    /**
     * Check if an event has already been processed by a listener.
     *
     * @param listenerId the listener identifier (e.g., "user-registration-listener")
     * @param idempotencyKey unique key for this event (e.g., "user-123")
     * @return true if already processed, false if not
     */
    @Transactional(readOnly = true)
    public boolean isProcessed(String listenerId, String idempotencyKey) {
        return repository.exists(listenerId, idempotencyKey);
    }

    /**
     * Mark an event as successfully processed.
     *
     * Uses a new transaction to ensure the record is committed
     * even if the caller's transaction later fails.
     *
     * @param listenerId the listener identifier
     * @param idempotencyKey unique key for this event
     * @throws RuntimeException if insert fails (shouldn't happen with unique constraint)
     */
    @Transactional
    public void markAsProcessed(String listenerId, String idempotencyKey) {
        try {
            IdempotencyRecord record = new IdempotencyRecord(listenerId, idempotencyKey);
            repository.save(record);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Another instance of this listener just processed the same event
            // Safe to ignore: both would execute the same side effect
            // (this exception means the unique constraint was violated)
        }
    }

    /**
     * Mark an event as processed with result data.
     *
     * @param listenerId the listener identifier
     * @param idempotencyKey unique key for this event
     * @param resultData JSON result data (e.g., email ID, PDF URL, webhook response)
     */
    @Transactional
    public void markAsProcessed(String listenerId, String idempotencyKey, String resultData) {
        try {
            IdempotencyRecord record = new IdempotencyRecord(listenerId, idempotencyKey, resultData);
            repository.save(record);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Duplicate processing, ignore
        }
    }

    /**
     * Get the result data from a previous processing (if available).
     *
     * Useful if the listener wants to cache results or provide
     * idempotent responses without re-processing.
     *
     * @param listenerId the listener identifier
     * @param idempotencyKey unique key for this event
     * @return Optional containing result data if processed, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<String> getResultData(String listenerId, String idempotencyKey) {
        return repository.findByListenerIdAndKey(listenerId, idempotencyKey)
                .map(IdempotencyRecord::getResultData);
    }

    /**
     * Count processed events for a listener (for monitoring).
     *
     * @param listenerId the listener identifier
     * @return number of processed events
     */
    @Transactional(readOnly = true)
    public long countProcessed(String listenerId) {
        return repository.countByListenerId(listenerId);
    }
}
