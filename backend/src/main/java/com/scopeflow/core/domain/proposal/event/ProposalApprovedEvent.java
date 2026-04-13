package com.scopeflow.core.domain.proposal.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event: Proposal approved by client.
 *
 * Published when a client approves a proposal via the public approval link.
 * Consumed by:
 *  - ProposalApprovalListener → generates PDF + sends approval email to client
 *
 * Serialized as JSON and stored in outbox_event table (payload column).
 * Deserialized by OutboxEventPublisher before publishing to RabbitMQ.
 */
public record ProposalApprovedEvent(
        UUID proposalId,
        UUID approvalWorkflowId,
        String clientEmail,
        String clientName,
        UUID workspaceId,
        Instant occurredAt
) {
    public ProposalApprovedEvent {
        Objects.requireNonNull(proposalId, "proposalId cannot be null");
        Objects.requireNonNull(approvalWorkflowId, "approvalWorkflowId cannot be null");
        Objects.requireNonNull(clientEmail, "clientEmail cannot be null");
        Objects.requireNonNull(clientName, "clientName cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    /**
     * Factory for convenience in application services and tests.
     */
    public static ProposalApprovedEvent of(
            UUID proposalId,
            UUID approvalWorkflowId,
            String clientEmail,
            String clientName,
            UUID workspaceId
    ) {
        return new ProposalApprovedEvent(
                proposalId, approvalWorkflowId, clientEmail, clientName, workspaceId, Instant.now()
        );
    }
}
