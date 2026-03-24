package com.scopeflow.adapter.out.persistence.proposal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaApproval.
 */
public interface JpaApprovalSpringRepository extends JpaRepository<JpaApproval, UUID> {

    List<JpaApproval> findByWorkflowId(UUID workflowId);

    Optional<JpaApproval> findByWorkflowIdAndApproverEmail(UUID workflowId, String approverEmail);
}
