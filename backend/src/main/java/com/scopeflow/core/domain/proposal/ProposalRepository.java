package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ProposalRepository port (domain layer).
 *
 * All find methods exclude soft-deleted proposals transparently
 * via @SQLRestriction on the JPA adapter layer.
 */
public interface ProposalRepository {

    void save(Proposal proposal);

    Optional<Proposal> findById(ProposalId id);

    /**
     * Workspace-scoped lookup — prevents cross-workspace data leakage.
     */
    Optional<Proposal> findByIdAndWorkspaceId(ProposalId id, WorkspaceId workspaceId);

    List<Proposal> findByWorkspaceId(WorkspaceId workspaceId);

    List<Proposal> findByWorkspaceIdAndStatus(WorkspaceId workspaceId, ProposalStatus status);

    List<Proposal> findByClientIdAndWorkspaceId(UUID clientId, WorkspaceId workspaceId);

    /**
     * Soft delete: sets deleted_at = now(). Never physically removes the row.
     *
     * @throws ProposalNotFoundException if proposal not found in this workspace
     */
    void softDelete(ProposalId id, WorkspaceId workspaceId);
}
