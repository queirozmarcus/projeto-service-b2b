package com.scopeflow.adapter.out.persistence.proposal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaProposal.
 */
public interface JpaProposalSpringRepository extends JpaRepository<JpaProposal, UUID> {

    List<JpaProposal> findByWorkspaceId(UUID workspaceId);

    List<JpaProposal> findByWorkspaceIdAndStatus(UUID workspaceId, String status);

    List<JpaProposal> findByClientIdAndWorkspaceId(UUID clientId, UUID workspaceId);
}
