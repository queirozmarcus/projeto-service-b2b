package com.scopeflow.adapter.in.web.proposal.dto;

import com.scopeflow.core.domain.proposal.Proposal;
import com.scopeflow.core.domain.proposal.ProposalScope;

import java.time.Instant;
import java.util.UUID;

/**
 * Proposal response DTO.
 */
public record ProposalResponse(
        UUID id,
        UUID workspaceId,
        UUID clientId,
        UUID briefingId,
        String proposalName,
        String status,
        ProposalScope scope,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProposalResponse from(Proposal proposal) {
        return new ProposalResponse(
                proposal.getId().value(),
                proposal.getWorkspaceId().value(),
                proposal.getClientId(),
                proposal.getBriefingId().value(),
                proposal.getProposalName(),
                proposal.status().name(),
                proposal.getScope(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }
}
