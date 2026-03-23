package com.scopeflow.adapter.out.persistence.briefing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaBriefingSession.
 * Provides query methods for briefing session persistence.
 */
@Repository
public interface JpaBriefingSessionSpringRepository extends JpaRepository<JpaBriefingSession, UUID> {

    /**
     * Find active briefing session by client and service type.
     * Uses partial unique index: idx_briefing_sessions_active_single.
     */
    @Query("""
        SELECT bs FROM JpaBriefingSession bs
        WHERE bs.clientId = :clientId
          AND bs.serviceType = :serviceType
          AND bs.status = 'IN_PROGRESS'
    """)
    Optional<JpaBriefingSession> findActiveByClientAndService(
        @Param("clientId") UUID clientId,
        @Param("serviceType") String serviceType
    );

    /**
     * Find all briefing sessions by workspace and status.
     */
    @Query("""
        SELECT bs FROM JpaBriefingSession bs
        WHERE bs.workspaceId = :workspaceId
          AND bs.status = :status
        ORDER BY bs.createdAt DESC
    """)
    List<JpaBriefingSession> findByWorkspaceAndStatus(
        @Param("workspaceId") UUID workspaceId,
        @Param("status") String status
    );

    /**
     * Find briefing session by public token.
     * Uses unique index: uk_briefing_sessions_public_token.
     */
    Optional<JpaBriefingSession> findByPublicToken(String publicToken);

    /**
     * Find all briefing sessions by workspace with pagination and optional filters.
     */
    @Query("""
        SELECT bs FROM JpaBriefingSession bs
        WHERE bs.workspaceId = :workspaceId
          AND (:status IS NULL OR bs.status = :status)
          AND (:serviceType IS NULL OR bs.serviceType = :serviceType)
          AND (:createdAfter IS NULL OR bs.createdAt >= :createdAfter)
    """)
    Page<JpaBriefingSession> findByWorkspaceWithFilters(
        @Param("workspaceId") UUID workspaceId,
        @Param("status") String status,
        @Param("serviceType") String serviceType,
        @Param("createdAfter") Instant createdAfter,
        Pageable pageable
    );

    /**
     * Count answers for a briefing session.
     * Used by domain service to calculate next step.
     */
    @Query("""
        SELECT COUNT(ba) FROM JpaBriefingAnswer ba
        WHERE ba.briefingSessionId = :sessionId
    """)
    long countAnswers(@Param("sessionId") UUID sessionId);
}
