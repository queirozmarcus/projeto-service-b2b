package com.scopeflow.core.domain.proposal;

import java.util.Objects;
import java.util.UUID;

/**
 * ProposalVersionId value object.
 */
public record ProposalVersionId(UUID value) {
    public ProposalVersionId {
        Objects.requireNonNull(value, "ProposalVersionId value cannot be null");
    }

    public static ProposalVersionId generate() {
        return new ProposalVersionId(UUID.randomUUID());
    }
}
