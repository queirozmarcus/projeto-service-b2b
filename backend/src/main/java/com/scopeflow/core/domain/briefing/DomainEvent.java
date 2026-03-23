package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;

/**
 * Sealed interface for Briefing domain events.
 * All domain events must implement this interface for Outbox pattern serialization.
 */
public sealed interface DomainEvent permits
        BriefingSessionStartedEvent,
        QuestionAskedEvent,
        AnswerSubmittedEvent,
        FollowupQuestionGeneratedEvent,
        BriefingCompletedEvent,
        BriefingAbandonedEvent {

    /**
     * The aggregate ID (session) that generated this event.
     */
    BriefingSessionId aggregateId();

    /**
     * The workspace ID for multi-tenancy filtering.
     */
    WorkspaceId workspaceId();

    /**
     * When the event occurred.
     */
    Instant occurredAt();

    /**
     * Event type for Kafka topic routing.
     */
    String eventType();
}
