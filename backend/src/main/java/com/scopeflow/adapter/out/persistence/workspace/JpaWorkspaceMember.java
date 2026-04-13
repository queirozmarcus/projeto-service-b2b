package com.scopeflow.adapter.out.persistence.workspace;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for WorkspaceMember aggregate.
 * Maps to 'workspace_members' table (created by V2 migration).
 */
@Entity
@Table(
        name = "workspace_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workspace_members_unique", columnNames = {"workspace_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_workspace_members_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_workspace_members_user_id", columnList = "user_id"),
                @Index(name = "idx_workspace_members_role", columnList = "role"),
                @Index(name = "idx_workspace_members_status", columnList = "status")
        }
)
public class JpaWorkspaceMember {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaWorkspaceMember() {
        // JPA only
    }

    public JpaWorkspaceMember(
            UUID id,
            UUID workspaceId,
            UUID userId,
            String role,
            String status,
            Instant joinedAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.workspaceId = Objects.requireNonNull(workspaceId);
        this.userId = Objects.requireNonNull(userId);
        this.role = Objects.requireNonNull(role);
        this.status = Objects.requireNonNull(status);
        this.joinedAt = Objects.requireNonNull(joinedAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getUserId() { return userId; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaWorkspaceMember that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
