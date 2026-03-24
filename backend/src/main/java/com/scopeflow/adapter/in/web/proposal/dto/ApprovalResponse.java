package com.scopeflow.adapter.in.web.proposal.dto;

import com.scopeflow.core.domain.proposal.Approval;

import java.time.Instant;
import java.util.UUID;

/**
 * Individual approval response DTO.
 */
public record ApprovalResponse(
        UUID id,
        String approverName,
        String approverEmail,
        String status,
        String ipAddress,
        Instant approvedAt
) {
    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(
                approval.id(),
                approval.approverName(),
                approval.approverEmail(),
                approval.status().name(),
                approval.ipAddress(),
                approval.approvedAt()
        );
    }
}
