package com.scopeflow.adapter.out.persistence.proposal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaProposal.
 *
 * Note: @SQLRestriction("deleted_at IS NULL") on JpaProposal is applied automatically
 * to all queries here — soft-deleted proposals are never returned.
 */
public interface JpaProposalSpringRepository extends JpaRepository<JpaProposal, UUID> {

    List<JpaProposal> findByWorkspaceId(UUID workspaceId);

    Page<JpaProposal> findByWorkspaceId(UUID workspaceId, Pageable pageable);

    List<JpaProposal> findByWorkspaceIdAndStatus(UUID workspaceId, String status);

    Page<JpaProposal> findByWorkspaceIdAndStatus(UUID workspaceId, String status, Pageable pageable);

    List<JpaProposal> findByClientIdAndWorkspaceId(UUID clientId, UUID workspaceId);

    // Workspace-scoped lookup: prevents cross-workspace data leakage
    Optional<JpaProposal> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
