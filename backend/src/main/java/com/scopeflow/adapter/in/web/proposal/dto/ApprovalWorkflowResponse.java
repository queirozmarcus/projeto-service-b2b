package com.scopeflow.adapter.in.web.proposal.dto;

import com.scopeflow.core.domain.proposal.ApprovalWorkflow;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Approval workflow response DTO.
 */
public record ApprovalWorkflowResponse(
        UUID id,
        UUID proposalId,
        String status,
        List<ApprovalResponse> approvals,
        Instant initiatedAt,
        Instant completedAt
) {
    public static ApprovalWorkflowResponse from(ApprovalWorkflow workflow) {
        List<ApprovalResponse> approvals = workflow.approvals().stream()
                .map(ApprovalResponse::from)
                .toList();
        return new ApprovalWorkflowResponse(
                workflow.id().value(),
                workflow.proposalId().value(),
                workflow.status().name(),
                approvals,
                workflow.initiatedAt(),
                workflow.completedAt()
        );
    }
}
