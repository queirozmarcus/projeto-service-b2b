package com.scopeflow.core.domain.proposal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * ProposalVersion: immutable snapshot of proposal scope at a point in time.
 *
 * Once created, never updated — history is preserved.
 */
public record ProposalVersion(
        ProposalVersionId id,
        ProposalId proposalId,
        ProposalScope scope,
        Instant createdAt,
        UUID createdBy
) {
    public ProposalVersion {
        Objects.requireNonNull(id, "ProposalVersionId cannot be null");
        Objects.requireNonNull(proposalId, "ProposalId cannot be null");
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        Objects.requireNonNull(createdBy, "CreatedBy cannot be null");
    }

    public static ProposalVersion create(ProposalId proposalId, ProposalScope scope, UUID createdBy) {
        return new ProposalVersion(
                ProposalVersionId.generate(),
                proposalId,
                scope,
                Instant.now(),
                createdBy
        );
    }
}
