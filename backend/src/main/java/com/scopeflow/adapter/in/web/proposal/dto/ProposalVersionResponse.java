package com.scopeflow.adapter.in.web.proposal.dto;

import com.scopeflow.core.domain.proposal.ProposalScope;
import com.scopeflow.core.domain.proposal.ProposalVersion;

import java.time.Instant;
import java.util.UUID;

/**
 * Proposal version (immutable snapshot) response DTO.
 */
public record ProposalVersionResponse(
        UUID versionId,
        UUID proposalId,
        ProposalScope scope,
        Instant createdAt,
        UUID createdBy
) {
    public static ProposalVersionResponse from(ProposalVersion version) {
        return new ProposalVersionResponse(
                version.id().value(),
                version.proposalId().value(),
                version.scope(),
                version.createdAt(),
                version.createdBy()
        );
    }
}
