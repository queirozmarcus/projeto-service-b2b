package com.scopeflow.adapter.out.persistence.proposal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaApprovalWorkflow.
 */
public interface JpaApprovalWorkflowSpringRepository extends JpaRepository<JpaApprovalWorkflow, UUID> {

    Optional<JpaApprovalWorkflow> findByProposalId(UUID proposalId);
}
