package com.scopeflow.application.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for IdempotencyRecord persistence.
 *
 * Handles CRUD and query operations for idempotency tracking.
 * Primary use: IdempotencyService checks existence before listener processes events.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    /**
     * Find idempotency record by listener ID and idempotency key.
     *
     * Used by IdempotencyService.isProcessed() to prevent duplicate processing.
     *
     * @param listenerId the listener identifier (e.g., "user-registration-listener")
     * @param idempotencyKey the unique event key (e.g., "user-123:event-456")
     * @return Optional containing the record if found, empty if not processed
     */
    @Query("SELECT ir FROM IdempotencyRecord ir WHERE ir.listenerId = :listenerId AND ir.idempotencyKey = :idempotencyKey")
    Optional<IdempotencyRecord> findByListenerIdAndKey(String listenerId, String idempotencyKey);

    /**
     * Check if an event has already been processed (exists in table).
     *
     * @param listenerId the listener identifier
     * @param idempotencyKey the unique event key
     * @return true if record exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(ir) > 0 THEN true ELSE false END FROM IdempotencyRecord ir WHERE ir.listenerId = :listenerId AND ir.idempotencyKey = :idempotencyKey")
    boolean exists(String listenerId, String idempotencyKey);

    /**
     * Count records for a specific listener (for monitoring).
     *
     * @param listenerId the listener identifier
     * @return number of processed events for this listener
     */
    @Query("SELECT COUNT(ir) FROM IdempotencyRecord ir WHERE ir.listenerId = :listenerId")
    long countByListenerId(String listenerId);
}
