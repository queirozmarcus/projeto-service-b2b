package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ProposalRepository port (domain layer).
 */
public interface ProposalRepository {

    void save(Proposal proposal);

    Optional<Proposal> findById(ProposalId id);

    List<Proposal> findByWorkspaceId(WorkspaceId workspaceId);

    List<Proposal> findByWorkspaceIdAndStatus(WorkspaceId workspaceId, ProposalStatus status);

    List<Proposal> findByClientIdAndWorkspaceId(UUID clientId, WorkspaceId workspaceId);

    void delete(ProposalId id);
}
