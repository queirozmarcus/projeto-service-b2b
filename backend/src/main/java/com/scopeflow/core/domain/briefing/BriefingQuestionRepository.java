package com.scopeflow.core.domain.briefing;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BriefingQuestion persistence (Port).
 * No Spring annotations: pure domain port.
 * Implementation provided by adapter layer (JPA).
 */
public interface BriefingQuestionRepository {
    /**
     * Find a question by session ID and step number.
     */
    Optional<BriefingQuestion> findBySessionAndStep(BriefingSessionId sessionId, int step);

    /**
     * Find all questions for a session, ordered by step.
     */
    List<BriefingQuestion> findBySession(BriefingSessionId sessionId);

    /**
     * Save a new question.
     */
    void save(BriefingQuestion question);

    /**
     * Get total number of questions for a service type.
     */
    long countByServiceType(ServiceType serviceType);
}
