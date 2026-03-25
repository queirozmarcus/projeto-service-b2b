package com.scopeflow.application.fixtures;

import com.scopeflow.application.outbox.OutboxEvent;
import com.scopeflow.core.domain.briefing.event.BriefingCompletedEvent;
import com.scopeflow.core.domain.proposal.event.ProposalApprovedEvent;
import com.scopeflow.core.domain.user.event.UserRegisteredEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Test fixtures for messaging/event integration tests (Sprint 4).
 *
 * Centralizes event creation with sensible defaults so test methods stay concise.
 * Use the static factory methods; override individual fields only when the test depends on them.
 */
public final class MessagingEventFixtures {

    // Shared default values referenced by multiple tests
    public static final String DEFAULT_USER_EMAIL = "alice@agency.com";
    public static final String DEFAULT_USER_NAME = "Alice Freelancer";
    public static final String DEFAULT_CLIENT_EMAIL = "client@startup.com";
    public static final String DEFAULT_CLIENT_NAME = "Bob Client";

    private MessagingEventFixtures() {}

    // ============ UserRegisteredEvent ============

    public static UserRegisteredEvent userRegisteredEvent() {
        return UserRegisteredEvent.of(
                UUID.randomUUID(),
                DEFAULT_USER_EMAIL,
                DEFAULT_USER_NAME,
                UUID.randomUUID()
        );
    }

    public static UserRegisteredEvent userRegisteredEvent(UUID userId, UUID workspaceId) {
        return UserRegisteredEvent.of(userId, DEFAULT_USER_EMAIL, DEFAULT_USER_NAME, workspaceId);
    }

    public static UserRegisteredEvent userRegisteredEvent(
            UUID userId,
            String email,
            String fullName,
            UUID workspaceId
    ) {
        return UserRegisteredEvent.of(userId, email, fullName, workspaceId);
    }

    // ============ ProposalApprovedEvent ============

    public static ProposalApprovedEvent proposalApprovedEvent() {
        return ProposalApprovedEvent.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DEFAULT_CLIENT_EMAIL,
                DEFAULT_CLIENT_NAME,
                UUID.randomUUID()
        );
    }

    public static ProposalApprovedEvent proposalApprovedEvent(UUID proposalId, UUID workflowId) {
        return ProposalApprovedEvent.of(
                proposalId,
                workflowId,
                DEFAULT_CLIENT_EMAIL,
                DEFAULT_CLIENT_NAME,
                UUID.randomUUID()
        );
    }

    public static ProposalApprovedEvent proposalApprovedEvent(
            UUID proposalId,
            UUID workflowId,
            String clientEmail
    ) {
        return ProposalApprovedEvent.of(
                proposalId,
                workflowId,
                clientEmail,
                DEFAULT_CLIENT_NAME,
                UUID.randomUUID()
        );
    }

    // ============ BriefingCompletedEvent ============

    public static BriefingCompletedEvent briefingCompletedEvent() {
        return BriefingCompletedEvent.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                85,
                "No critical gaps"
        );
    }

    public static BriefingCompletedEvent briefingCompletedEvent(UUID sessionId) {
        return BriefingCompletedEvent.of(
                sessionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                85,
                "No critical gaps"
        );
    }

    // ============ OutboxEvent builders ============

    /**
     * Builds an unpublished OutboxEvent for a UserRegisteredEvent.
     * The payload must be a valid JSON serialization of the event for deserialization tests.
     */
    public static OutboxEvent unpublishedOutboxEvent(String eventType, UUID aggregateId) {
        String minimalPayload = "{\"userId\":\"" + aggregateId + "\","
                + "\"email\":\"" + DEFAULT_USER_EMAIL + "\","
                + "\"fullName\":\"" + DEFAULT_USER_NAME + "\","
                + "\"workspaceId\":\"" + UUID.randomUUID() + "\","
                + "\"occurredAt\":\"" + Instant.now() + "\"}";
        return new OutboxEvent(eventType, aggregateId, "User", minimalPayload);
    }

    /**
     * Builds an unpublished OutboxEvent with explicit JSON payload.
     */
    public static OutboxEvent unpublishedOutboxEvent(
            String eventType,
            UUID aggregateId,
            String aggregateType,
            String payload
    ) {
        return new OutboxEvent(eventType, aggregateId, aggregateType, payload);
    }
}
