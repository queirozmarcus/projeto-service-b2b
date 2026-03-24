package com.scopeflow.adapter.out.persistence.proposal;

import com.scopeflow.core.domain.proposal.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing ApprovalWorkflowRepository domain port.
 */
@Component
public class JpaApprovalWorkflowRepositoryAdapter implements ApprovalWorkflowRepository {

    private final JpaApprovalWorkflowSpringRepository workflowRepo;
    private final JpaApprovalSpringRepository approvalRepo;

    public JpaApprovalWorkflowRepositoryAdapter(
            JpaApprovalWorkflowSpringRepository workflowRepo,
            JpaApprovalSpringRepository approvalRepo
    ) {
        this.workflowRepo = workflowRepo;
        this.approvalRepo = approvalRepo;
    }

    @Override
    public void save(ApprovalWorkflow workflow) {
        // Upsert workflow
        workflowRepo.findById(workflow.id().value()).ifPresentOrElse(
                existing -> {
                    existing.setStatus(workflow.status().name());
                    existing.setCompletedAt(workflow.completedAt());
                    workflowRepo.save(existing);
                },
                () -> {
                    JpaApprovalWorkflow jpaWorkflow = new JpaApprovalWorkflow(
                            workflow.id().value(),
                            workflow.proposalId().value(),
                            workflow.status().name(),
                            workflow.initiatedAt(),
                            workflow.completedAt()
                    );
                    workflowRepo.save(jpaWorkflow);
                }
        );

        // Upsert each approval
        for (Approval approval : workflow.approvals()) {
            approvalRepo.findByWorkflowIdAndApproverEmail(
                    workflow.id().value(), approval.approverEmail()
            ).ifPresentOrElse(
                    existing -> {
                        existing.setApproverName(approval.approverName());
                        existing.setStatus(approval.status().name());
                        existing.setIpAddress(approval.ipAddress());
                        existing.setUserAgent(approval.userAgent());
                        existing.setApprovedAt(approval.approvedAt());
                        approvalRepo.save(existing);
                    },
                    () -> approvalRepo.save(new JpaApproval(
                            approval.id() != null ? approval.id() : UUID.randomUUID(),
                            workflow.id().value(),
                            approval.approverName(),
                            approval.approverEmail(),
                            approval.status().name(),
                            approval.ipAddress(),
                            approval.userAgent(),
                            approval.approvedAt()
                    ))
            );
        }
    }

    @Override
    public Optional<ApprovalWorkflow> findById(ApprovalWorkflowId id) {
        return workflowRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<ApprovalWorkflow> findByProposalId(ProposalId proposalId) {
        return workflowRepo.findByProposalId(proposalId.value()).map(this::toDomain);
    }

    // ============ JPA → Domain ============

    private ApprovalWorkflow toDomain(JpaApprovalWorkflow entity) {
        List<JpaApproval> jpaApprovals = approvalRepo.findByWorkflowId(entity.getId());
        List<Approval> approvals = jpaApprovals.stream().map(this::approvalToDomain).toList();

        return new ApprovalWorkflow(
                new ApprovalWorkflowId(entity.getId()),
                ProposalId.of(entity.getProposalId()),
                ApprovalStatus.valueOf(entity.getStatus()),
                approvals,
                entity.getInitiatedAt(),
                entity.getCompletedAt()
        );
    }

    private Approval approvalToDomain(JpaApproval entity) {
        return new Approval(
                entity.getId(),
                new ApprovalWorkflowId(entity.getWorkflowId()),
                entity.getApproverName(),
                entity.getApproverEmail(),
                ApprovalStatus.valueOf(entity.getStatus()),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getApprovedAt()
        );
    }
}
