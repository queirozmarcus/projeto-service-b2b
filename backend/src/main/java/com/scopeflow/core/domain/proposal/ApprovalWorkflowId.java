package com.scopeflow.core.domain.proposal;

import java.util.Objects;
import java.util.UUID;

/**
 * ApprovalWorkflowId value object.
 */
public record ApprovalWorkflowId(UUID value) {
    public ApprovalWorkflowId {
        Objects.requireNonNull(value, "ApprovalWorkflowId value cannot be null");
    }

    public static ApprovalWorkflowId generate() {
        return new ApprovalWorkflowId(UUID.randomUUID());
    }
}
