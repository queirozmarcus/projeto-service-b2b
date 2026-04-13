-- V7: Fix outbox_event_type constraint to allow fully qualified class names.
--
-- Problem: V5 check constraint only allowed simple class names (e.g., "UserRegisteredEvent"),
-- but OutboxService stores the fully qualified class name (e.g.,
-- "com.scopeflow.core.domain.user.event.UserRegisteredEvent") for deserialization.
--
-- Fix: Drop the overly restrictive constraint and replace with a broader one that
-- allows dots, underscores, and package-style naming while still preventing empty values.

ALTER TABLE outbox_event
    DROP CONSTRAINT IF EXISTS check_event_type;

ALTER TABLE outbox_event
    ADD CONSTRAINT check_event_type
    CHECK (event_type ~ '^[A-Za-z][A-Za-z0-9._$]+$' AND length(event_type) <= 255);

COMMENT ON CONSTRAINT check_event_type ON outbox_event IS
'Validates that event_type is a non-empty Java class name (simple or fully qualified).
Examples of valid values:
  - "UserRegisteredEvent" (simple name)
  - "com.scopeflow.core.domain.user.event.UserRegisteredEvent" (FQCN)
  - "com.scopeflow.NonExistentEvent" (valid format, handled gracefully by publisher)
OutboxService.persist() stores FQCN via event.getClass().getName().';
