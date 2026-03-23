package com.scopeflow.core.domain.briefing;

import java.util.List;

/**
 * Repository interface for AIGeneration persistence (Port).
 * No Spring annotations: pure domain port.
 * Implementation provided by adapter layer (JPA).
 */
public interface AIGenerationRepository {
    /**
     * Save a new AI generation record (audit trail).
     */
    void save(AIGeneration generation);

    /**
     * Find all AI generation records for a briefing session (audit trail).
     */
    List<AIGeneration> findBySession(BriefingSessionId sessionId);

    /**
     * Find all AI generation records of a specific type for a session.
     */
    List<AIGeneration> findBySessionAndType(BriefingSessionId sessionId, GenerationType type);
}
