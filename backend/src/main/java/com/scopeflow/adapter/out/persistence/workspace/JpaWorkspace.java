package com.scopeflow.adapter.out.persistence.workspace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Workspace aggregate root.
 * Maps to 'workspaces' table (created by V2 migration).
 */
@Entity
@Table(
        name = "workspaces",
        indexes = {
                @Index(name = "idx_workspaces_owner_id", columnList = "owner_id"),
                @Index(name = "idx_workspaces_status", columnList = "status")
        }
)
public class JpaWorkspace {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "niche", nullable = false, length = 100)
    private String niche;

    @Column(name = "tone_settings", columnDefinition = "jsonb")
    private String toneSettings;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaWorkspace() {
        // JPA only
    }

    public JpaWorkspace(
            UUID id,
            UUID ownerId,
            String name,
            String niche,
            String toneSettings,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.name = Objects.requireNonNull(name);
        this.niche = Objects.requireNonNull(niche);
        this.toneSettings = toneSettings;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void setName(String name) { this.name = name; }
    public void setNiche(String niche) { this.niche = niche; }
    public void setToneSettings(String toneSettings) { this.toneSettings = toneSettings; }
    public void setStatus(String status) { this.status = status; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getNiche() { return niche; }
    public String getToneSettings() { return toneSettings; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaWorkspace that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
