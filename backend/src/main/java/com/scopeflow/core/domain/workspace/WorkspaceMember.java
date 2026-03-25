package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * WorkspaceMember aggregate (sealed class).
 *
 * Represents membership relationship between User and Workspace with a Role.
 *
 * States:
 * - MemberActive: currently a member
 * - MemberInvited: invited but not yet accepted
 * - MemberLeft: removed/exited (historical record)
 */
public abstract sealed class WorkspaceMember permits MemberActive, MemberInvited, MemberLeft {
    private final WorkspaceId workspaceId;
    private final UserId userId;
    private final Role role;
    private final Instant joinedAt;
    private final Instant updatedAt;

    protected WorkspaceMember(
            WorkspaceId workspaceId,
            UserId userId,
            Role role,
            Instant joinedAt,
            Instant updatedAt
    ) {
        this.workspaceId = Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "JoinedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    public static MemberActive createActive(WorkspaceId workspaceId, UserId userId, Role role) {
        return new MemberActive(workspaceId, userId, role, Instant.now(), Instant.now());
    }

    public static MemberInvited createInvited(WorkspaceId workspaceId, UserId userId, Role role) {
        return new MemberInvited(workspaceId, userId, role, Instant.now(), Instant.now());
    }

    // ============ Accessors ============

    public WorkspaceId getWorkspaceId() {
        return workspaceId;
    }

    public UserId getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
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
        if (!(o instanceof WorkspaceMember)) return false;
        WorkspaceMember member = (WorkspaceMember) o;
        return Objects.equals(workspaceId, member.workspaceId) &&
                Objects.equals(userId, member.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, userId);
    }

    @Override
    public String toString() {
        return "WorkspaceMember{" +
                "workspaceId=" + workspaceId +
                ", userId=" + userId +
                ", role=" + role +
                ", status=" + status() +
                '}';
    }
}
