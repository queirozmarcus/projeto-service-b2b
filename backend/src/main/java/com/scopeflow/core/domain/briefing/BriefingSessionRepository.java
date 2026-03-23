package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BriefingSession persistence (Port).
 * No Spring annotations: pure domain port.
 * Implementation provided by adapter layer (JPA).
 */
public interface BriefingSessionRepository {
    /**
     * Find a briefing session by ID.
     */
    Optional<BriefingSession> findById(BriefingSessionId id);

    /**
     * Save or update a briefing session.
     */
    void save(BriefingSession session);

    /**
     * Find active (in-progress) briefing for given client and service.
     * Returns empty if no active briefing exists.
     */
    Optional<BriefingSession> findActiveByClientAndService(ClientId clientId, ServiceType serviceType);

    /**
     * Find all briefings for a workspace with given status.
     */
    List<BriefingSession> findByWorkspaceAndStatus(WorkspaceId workspaceId, String status);

    /**
     * Count answers already submitted for a session.
     */
    long countAnswers(BriefingSessionId sessionId);

    /**
     * Find briefing session by public token (for client-facing public API).
     * Used by public endpoints (no auth required).
     */
    Optional<BriefingSession> findByPublicToken(PublicToken publicToken);
}
