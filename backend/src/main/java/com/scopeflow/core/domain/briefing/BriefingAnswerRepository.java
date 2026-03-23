package com.scopeflow.core.domain.briefing;

import java.util.List;

/**
 * Repository interface for BriefingAnswer persistence (Port).
 * No Spring annotations: pure domain port.
 * Implementation provided by adapter layer (JPA).
 */
public interface BriefingAnswerRepository {
    /**
     * Save a new answer.
     */
    void save(BriefingAnswer answer);

    /**
     * Find all answers for a briefing session.
     */
    List<BriefingAnswer> findBySession(BriefingSessionId sessionId);

    /**
     * Count follow-up answers for a given question (max 1 allowed).
     */
    long countFollowupsByQuestion(QuestionId questionId);

    /**
     * Check if an answer already exists for a question in a session.
     */
    boolean existsBySessionAndQuestion(BriefingSessionId sessionId, QuestionId questionId);
}
