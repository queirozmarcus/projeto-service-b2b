package com.scopeflow.core.domain.proposal;

import java.util.Objects;
import java.util.UUID;

/**
 * ProposalId value object.
 */
public record ProposalId(UUID value) {
    public ProposalId {
        Objects.requireNonNull(value, "ProposalId value cannot be null");
    }

    public static ProposalId generate() {
        return new ProposalId(UUID.randomUUID());
    }

    public static ProposalId of(UUID value) {
        return new ProposalId(value);
    }
}
