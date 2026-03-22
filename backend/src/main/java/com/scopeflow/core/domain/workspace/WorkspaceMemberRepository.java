package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.util.List;
import java.util.Optional;

/**
 * WorkspaceMemberRepository interface (domain layer, port).
 */
public interface WorkspaceMemberRepository {

    /**
     * Save a member (create or update).
     */
    void save(WorkspaceMember member);

    /**
     * Find member by workspace + user.
     */
    Optional<WorkspaceMember> findByWorkspaceAndUser(WorkspaceId workspaceId, UserId userId);

    /**
     * List all members in a workspace.
     */
    List<WorkspaceMember> findAllByWorkspace(WorkspaceId workspaceId);

    /**
     * Find all active members in a workspace.
     */
    List<WorkspaceMember> findActiveMembers(WorkspaceId workspaceId);

    /**
     * Count active members (OWNER role).
     */
    int countOwnersByWorkspace(WorkspaceId workspaceId);

    /**
     * Delete a member.
     */
    void delete(WorkspaceId workspaceId, UserId userId);
}
