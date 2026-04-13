package com.scopeflow.core.domain.proposal;

import java.util.Optional;

/**
 * ApprovalWorkflowRepository port (domain layer).
 */
public interface ApprovalWorkflowRepository {

    void save(ApprovalWorkflow workflow);

    Optional<ApprovalWorkflow> findById(ApprovalWorkflowId id);

    Optional<ApprovalWorkflow> findByProposalId(ProposalId proposalId);
}
