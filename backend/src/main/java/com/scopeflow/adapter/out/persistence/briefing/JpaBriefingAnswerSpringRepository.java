package com.scopeflow.adapter.out.persistence.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaBriefingAnswer.
 * Provides query methods for briefing answer persistence.
 */
@Repository
public interface JpaBriefingAnswerSpringRepository extends JpaRepository<JpaBriefingAnswer, UUID> {

    /**
     * Find all answers for a briefing session.
     * Uses index: idx_briefing_answers_session_id.
     */
    @Query("""
        SELECT ba FROM JpaBriefingAnswer ba
        WHERE ba.briefingSessionId = :sessionId
        ORDER BY ba.createdAt ASC
    """)
    List<JpaBriefingAnswer> findBySessionOrderByCreatedAt(@Param("sessionId") UUID sessionId);

    /**
     * Find all answers for a specific question.
     * Note: Max 1 answer per question per session (enforced by unique constraint).
     * Uses index: idx_briefing_answers_question_id.
     */
    @Query("""
        SELECT ba FROM JpaBriefingAnswer ba
        WHERE ba.questionId = :questionId
    """)
    List<JpaBriefingAnswer> findByQuestion(@Param("questionId") UUID questionId);

    /**
     * Count follow-up answers for a question.
     * Max 1 follow-up per question (enforced by domain service).
     * This query checks if a follow-up was already generated.
     */
    @Query("""
        SELECT COUNT(bq) FROM JpaBriefingQuestion bq
        WHERE bq.briefingSessionId = (
            SELECT ba.briefingSessionId FROM JpaBriefingAnswer ba WHERE ba.questionId = :questionId
        )
        AND bq.followUpGenerated = true
        AND bq.step > (
            SELECT originalQ.step FROM JpaBriefingQuestion originalQ WHERE originalQ.id = :questionId
        )
    """)
    long countFollowupsByQuestion(@Param("questionId") UUID questionId);
}
