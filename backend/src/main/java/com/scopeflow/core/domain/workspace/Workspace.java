package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * Workspace aggregate root (sealed class for type safety).
 *
 * Represents a tenant in ScopeFlow.
 * Every workspace has exactly one OWNER (invariant).
 *
 * States:
 * - WorkspaceActive: normal operation
 * - WorkspaceSuspended: owner paused subscription
 *
 * No framework dependencies. Pure domain logic.
 */
public sealed class Workspace permits WorkspaceActive, WorkspaceSuspended {
    private final WorkspaceId id;
    private final UserId ownerId;
    private final String name;
    private final String niche; // e.g., "social-media", "landing-page"
    private final String toneSettings; // JSONB in DB, here as String
    private final Instant createdAt;
    private final Instant updatedAt;

    protected Workspace(
            WorkspaceId id,
            UserId ownerId,
            String name,
            String niche,
            String toneSettings,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "WorkspaceId cannot be null");
        this.ownerId = Objects.requireNonNull(ownerId, "OwnerId cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.niche = Objects.requireNonNull(niche, "Niche cannot be null");
        this.toneSettings = toneSettings;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    /**
     * Factory method: create a new active workspace.
     */
    public static WorkspaceActive create(
            WorkspaceId id,
            UserId ownerId,
            String name,
            String niche,
            String toneSettings
    ) {
        return new WorkspaceActive(id, ownerId, name, niche, toneSettings, Instant.now(), Instant.now());
    }

    // ============ Accessors ============

    public WorkspaceId getId() {
        return id;
    }

    public UserId getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getNiche() {
        return niche;
    }

    public String getToneSettings() {
        return toneSettings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ============ Abstract Methods ============

    public abstract String status();

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workspace)) return false;
        Workspace workspace = (Workspace) o;
        return Objects.equals(id, workspace.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", name='" + name + '\'' +
                ", niche='" + niche + '\'' +
                ", status=" + status() +
                ", createdAt=" + createdAt +
                '}';
    }
}
