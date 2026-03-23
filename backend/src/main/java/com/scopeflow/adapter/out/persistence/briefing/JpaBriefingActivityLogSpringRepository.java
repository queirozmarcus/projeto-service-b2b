package com.scopeflow.adapter.out.persistence.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaBriefingActivityLog.
 * Provides query methods for activity log audit trail.
 */
@Repository
public interface JpaBriefingActivityLogSpringRepository extends JpaRepository<JpaBriefingActivityLog, UUID> {

    /**
     * Find all activity logs for a briefing session.
     * Uses index: idx_briefing_activity_logs_session_id.
     */
    @Query("""
        SELECT bal FROM JpaBriefingActivityLog bal
        WHERE bal.briefingSessionId = :sessionId
        ORDER BY bal.createdAt ASC
    """)
    List<JpaBriefingActivityLog> findBySessionOrderByCreatedAt(@Param("sessionId") UUID sessionId);

    /**
     * Find activity logs by action type.
     * Uses index: idx_briefing_activity_logs_action.
     */
    @Query("""
        SELECT bal FROM JpaBriefingActivityLog bal
        WHERE bal.briefingSessionId = :sessionId
          AND bal.action = :action
        ORDER BY bal.createdAt DESC
    """)
    List<JpaBriefingActivityLog> findBySessionAndAction(
        @Param("sessionId") UUID sessionId,
        @Param("action") String action
    );
}
