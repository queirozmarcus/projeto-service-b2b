package com.scopeflow.adapter.out.persistence.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaBriefingQuestion.
 * Provides query methods for briefing question persistence.
 */
@Repository
public interface JpaBriefingQuestionSpringRepository extends JpaRepository<JpaBriefingQuestion, UUID> {

    /**
     * Find a specific question by session and step.
     * Used by domain service to get next sequential question.
     * Uses compound index: idx_briefing_questions_step.
     */
    @Query("""
        SELECT bq FROM JpaBriefingQuestion bq
        WHERE bq.briefingSessionId = :sessionId
          AND bq.step = :step
    """)
    Optional<JpaBriefingQuestion> findBySessionAndStep(
        @Param("sessionId") UUID sessionId,
        @Param("step") int step
    );

    /**
     * Find all questions for a session, ordered by step.
     */
    @Query("""
        SELECT bq FROM JpaBriefingQuestion bq
        WHERE bq.briefingSessionId = :sessionId
        ORDER BY bq.step ASC
    """)
    List<JpaBriefingQuestion> findBySessionOrderByStepAsc(@Param("sessionId") UUID sessionId);

    /**
     * Find all auto-generated follow-up questions for a session.
     * Uses partial index: idx_briefing_questions_followup_generated.
     */
    @Query("""
        SELECT bq FROM JpaBriefingQuestion bq
        WHERE bq.briefingSessionId = :sessionId
          AND bq.followUpGenerated = true
        ORDER BY bq.step ASC
    """)
    List<JpaBriefingQuestion> findFollowupQuestions(@Param("sessionId") UUID sessionId);
}
