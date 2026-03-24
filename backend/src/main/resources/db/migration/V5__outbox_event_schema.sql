-- V5: Outbox table for transactional event publishing (Sprint 4)
-- Implements the Outbox Pattern (D8) for event atomicity.
-- Events are persisted in DB within the same transaction as domain changes,
-- then published asynchronously by OutboxEventPublisher scheduler.

CREATE TABLE IF NOT EXISTS outbox_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_event_type CHECK (event_type ~ '^[A-Za-z0-9]+Event$')
);

-- Index for polling unpublished events (OKushed every 5s by OutboxEventPublisher)
CREATE INDEX IF NOT EXISTS idx_outbox_event_unpublished
ON outbox_event(created_at)
WHERE published_at IS NULL;

-- Index for aggregate lookups (useful for debugging/replay)
CREATE INDEX IF NOT EXISTS idx_outbox_event_aggregate
ON outbox_event(aggregate_type, aggregate_id);

-- Index for type-based filtering (future: per-listener processing)
CREATE INDEX IF NOT EXISTS idx_outbox_event_type
ON outbox_event(event_type);

-- Comment documenting the pattern
COMMENT ON TABLE outbox_event IS
'Transactional Outbox table (Pattern: Outbox).
Events are inserted within the same TX as domain changes.
Ensures atomicity: if TX commits, event is guaranteed to exist in DB.
OutboxEventPublisher (scheduled every 5s) polls this table and publishes unpublished events.
This pattern solves: "TX committed but broker was down → event lost"
Solution: Event stays in DB, poller retries until success.';

COMMENT ON COLUMN outbox_event.event_type IS
'Fully qualified event class name (e.g., ''UserRegisteredEvent'').
Used by ObjectMapper to deserialize payload back to domain event.';

COMMENT ON COLUMN outbox_event.aggregate_id IS
'ID of the aggregate that triggered the event (e.g., userId, proposalId).
Used for tracking and eventual consistency.';

COMMENT ON COLUMN outbox_event.aggregate_type IS
'Type of the aggregate (e.g., ''User'', ''Proposal'', ''BriefingSession'').
Useful for organizing events by domain entity.';

COMMENT ON COLUMN outbox_event.payload IS
'JSON serialization of the domain event.
Reconstructed by ObjectMapper.readValue(payload, Class.forName(event_type)).';

COMMENT ON COLUMN outbox_event.published_at IS
'Timestamp when event was successfully published to RabbitMQ.
NULL = not yet published. Once set, event is considered processed by listeners.';
