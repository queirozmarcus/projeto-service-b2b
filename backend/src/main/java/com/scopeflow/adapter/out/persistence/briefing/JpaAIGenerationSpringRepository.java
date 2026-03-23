package com.scopeflow.adapter.out.persistence.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaAIGeneration.
 * Provides query methods for AI generation audit trail.
 */
@Repository
public interface JpaAIGenerationSpringRepository extends JpaRepository<JpaAIGeneration, UUID> {

    /**
     * Find all AI generations for a briefing session.
     * Uses index: idx_ai_generations_session_id.
     */
    @Query("""
        SELECT ag FROM JpaAIGeneration ag
        WHERE ag.briefingSessionId = :sessionId
        ORDER BY ag.createdAt ASC
    """)
    List<JpaAIGeneration> findBySessionOrderByCreatedAt(@Param("sessionId") UUID sessionId);

    /**
     * Find AI generations by type (FOLLOW_UP_QUESTION, GAP_ANALYSIS, COMPLETION_SUMMARY).
     * Uses index: idx_ai_generations_type.
     */
    @Query("""
        SELECT ag FROM JpaAIGeneration ag
        WHERE ag.briefingSessionId = :sessionId
          AND ag.generationType = :type
        ORDER BY ag.createdAt DESC
    """)
    List<JpaAIGeneration> findBySessionAndType(
        @Param("sessionId") UUID sessionId,
        @Param("type") String type
    );
}
