package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/**
 * BriefingSession aggregate root (sealed class for type safety).
 *
 * States:
 * - BriefingInProgress: can submit answers, detect gaps, generate follow-ups
 * - BriefingCompleted: locked, ready for scope generation
 * - BriefingAbandoned: can restart (create new BriefingInProgress)
 *
 * No framework dependencies. Pure domain logic.
 */
public abstract sealed class BriefingSession permits BriefingInProgress, BriefingCompleted, BriefingAbandoned {
    private final BriefingSessionId id;
    private final WorkspaceId workspaceId;
    private final ClientId clientId;
    private final ServiceType serviceType;
    private final PublicToken publicToken;
    private final Instant createdAt;
    private final Instant updatedAt;

    protected BriefingSession(
            BriefingSessionId id,
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType,
            PublicToken publicToken,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "BriefingSessionId cannot be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        this.clientId = Objects.requireNonNull(clientId, "ClientId cannot be null");
        this.serviceType = Objects.requireNonNull(serviceType, "ServiceType cannot be null");
        this.publicToken = Objects.requireNonNull(publicToken, "PublicToken cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    /**
     * Factory method: create a new briefing session in progress.
     */
    public static BriefingInProgress startNew(
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType
    ) {
        BriefingSessionId sessionId = BriefingSessionId.generate();
        PublicToken token = PublicToken.generate();
        Instant now = Instant.now();
        return new BriefingInProgress(
                sessionId,
                workspaceId,
                clientId,
                serviceType,
                token,
                now,
                now,
                new BriefingProgress(0, 0, 0) // Start with 0/0 steps
        );
    }

    // ============ Accessors ============

    public BriefingSessionId getId() {
        return id;
    }

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public ClientId getClientId() {
        return clientId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public PublicToken getPublicToken() {
        return publicToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ============ Abstract Methods ============

    /**
     * Returns the status of this briefing session.
     */
    public abstract String status();

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BriefingSession)) return false;
        BriefingSession that = (BriefingSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BriefingSession{" +
                "id=" + id +
                ", workspaceId=" + workspaceId +
                ", clientId=" + clientId +
                ", serviceType=" + serviceType +
                ", status=" + status() +
                ", createdAt=" + createdAt +
                '}';
    }
}
