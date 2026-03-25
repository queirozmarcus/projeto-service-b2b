-- V6: Idempotency table for event listener deduplication (Sprint 4, Phase 2)
-- Implements idempotent event processing (D9).
-- Prevents duplicate side effects (email sent twice, PDF generated twice, etc.)

CREATE TABLE IF NOT EXISTS idempotency_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listener_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_data JSONB DEFAULT NULL,
    CONSTRAINT unique_listener_idempotency UNIQUE (listener_id, idempotency_key)
);

-- Index for fast lookup during listener processing
CREATE INDEX IF NOT EXISTS idx_idempotency_key
ON idempotency_record(listener_id, idempotency_key);

-- Index for cleanup/auditing
CREATE INDEX IF NOT EXISTS idx_idempotency_processed_at
ON idempotency_record(processed_at);

-- Comment documenting the pattern
COMMENT ON TABLE idempotency_record IS
'Idempotency tracking table (Pattern: Idempotent Consumer).
Each event listener records a (listener_id, idempotency_key) tuple when processing succeeds.
If the same message arrives again (e.g., message broker retry), listener checks this table:
  - If record exists: skip processing (already done)
  - If not exists: process and insert record

Example idempotency_key: "approval-listener:proposal-id:approval-event-id"
This ensures:
  - Email sent once (not twice on retry)
  - PDF generated once
  - Database updates idempotent (can safely retry)';

COMMENT ON COLUMN idempotency_record.listener_id IS
'Logical ID of the listener (e.g., "user-registration-listener", "approval-listener").
Allows different listeners to handle the same event without conflict.';

COMMENT ON COLUMN idempotency_record.idempotency_key IS
'Unique key for this event processing instance.
Format: "{listener_id}:{aggregate_id}:{event_id}" or similar.
Must be unique per (listener_id, idempotency_key) pair.';

COMMENT ON COLUMN idempotency_record.result_data IS
'Optional JSON result data from processing (e.g., email ID, PDF URL, etc.)
Useful for auditing, debugging, and potential result caching.';
