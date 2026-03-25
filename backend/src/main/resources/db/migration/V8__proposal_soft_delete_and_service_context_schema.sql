-- V8__proposal_soft_delete_and_service_context_schema.sql
-- Sprint 6: ProposalList + BriefingSession flow — schema changes
-- Date: 2026-03-25
--
-- WHAT THIS MIGRATION DOES:
--   1. Adds `deleted_at` to proposals (soft delete support)
--   2. Creates service_context_profiles (service templates per workspace)
--   3. Creates service_context_questions (question templates per profile)
--
-- WHAT WAS INTENTIONALLY OMITTED AND WHY:
--   - `version` column on proposals: already exists in V4 (line 43)
--   - New briefing_sessions table: already exists in V3 (richer schema)
--   - New briefing_answers table: already exists in V3 (richer schema)
--   - New briefing_questions (per-session): already exists in V3
--     The new service_context_questions below is a DIFFERENT entity:
--     question TEMPLATES reused across sessions, not per-session AI-generated questions.

-- ============================================================================
-- 1. SOFT DELETE: proposals.deleted_at
-- ============================================================================
-- Used by Hibernate @SQLRestriction("deleted_at IS NULL") to filter soft-deleted
-- proposals transparently on all queries. Hard deletes are never issued.
ALTER TABLE proposals
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE NULL DEFAULT NULL;

-- Partial index: list view always filters WHERE deleted_at IS NULL.
-- Partial indexes exclude deleted rows, keeping index small and fast.
CREATE INDEX idx_proposals_not_deleted
    ON proposals(workspace_id, updated_at DESC)
    WHERE deleted_at IS NULL;

-- Partial index: deleted proposals (for admin/audit queries)
CREATE INDEX idx_proposals_deleted
    ON proposals(workspace_id, deleted_at DESC)
    WHERE deleted_at IS NOT NULL;

COMMENT ON COLUMN proposals.deleted_at IS
'Soft delete timestamp. NULL = active. Non-null = logically deleted.
Hibernate @SQLRestriction("deleted_at IS NULL") filters these transparently.
Never physically deleted; retained for audit trail.';

-- ============================================================================
-- 2. SERVICE_CONTEXT_PROFILES TABLE
-- ============================================================================
-- Configures AI behavior per service type per workspace.
-- Each profile defines the question strategy, tone, deliverables, and exclusions
-- that the AI uses when generating a briefing session for that service.
--
-- Ownership: Workspace context (created/managed by workspace owner/admin).
-- One profile per service_type per workspace (UNIQUE constraint enforced below).
CREATE TABLE service_context_profiles (
    id                  UUID PRIMARY KEY,
    workspace_id        UUID          NOT NULL,
    service_type        VARCHAR(50)   NOT NULL,
    profile_name        VARCHAR(255)  NOT NULL,
    tone_override       VARCHAR(50),
    default_entitlements JSONB,
    default_exclusions  JSONB,
    suggested_timeline  VARCHAR(255),
    pricing_structure   JSONB,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_service_context_profiles_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,

    -- One active profile per service type per workspace
    CONSTRAINT ck_service_context_profiles_service_type
        CHECK (service_type IN (
            'SOCIAL_MEDIA', 'LANDING_PAGE', 'WEB_DESIGN',
            'BRANDING', 'VIDEO_PRODUCTION', 'CONSULTING'
        )),
    CONSTRAINT ck_service_context_profiles_tone
        CHECK (tone_override IS NULL OR tone_override IN (
            'FORMAL', 'CASUAL', 'TECHNICAL', 'FRIENDLY'
        ))
);

-- Primary access pattern: fetch profiles for a workspace
CREATE INDEX idx_service_context_profiles_workspace_id
    ON service_context_profiles(workspace_id);

-- Filter active profiles per workspace
CREATE INDEX idx_service_context_profiles_workspace_active
    ON service_context_profiles(workspace_id, service_type)
    WHERE is_active = TRUE;

-- Enforce: one active profile per service type per workspace
CREATE UNIQUE INDEX idx_service_context_profiles_unique_active
    ON service_context_profiles(workspace_id, service_type)
    WHERE is_active = TRUE;

-- Auto-update updated_at (reuses function created in V3)
CREATE TRIGGER service_context_profiles_update_updated_at
    BEFORE UPDATE ON service_context_profiles
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE service_context_profiles IS
'Service configuration profile per workspace. Defines AI behavior, tone, deliverables,
and exclusions for a specific service type. Used as input context when generating
briefing questions for a new session.';
COMMENT ON COLUMN service_context_profiles.service_type IS
'Service type: must match briefing_sessions.service_type domain (same CHECK values).';
COMMENT ON COLUMN service_context_profiles.default_entitlements IS
'JSONB array of strings: deliverables included by default (e.g. ["logo", "brand guide"]).';
COMMENT ON COLUMN service_context_profiles.default_exclusions IS
'JSONB array of strings: what is explicitly NOT included (e.g. ["trademark research"]).';
COMMENT ON COLUMN service_context_profiles.pricing_structure IS
'JSONB: pricing tiers, hourly rate, fixed price ranges. Used as AI context.';
COMMENT ON COLUMN service_context_profiles.is_active IS
'Only one active profile per service_type per workspace (enforced by partial unique index).';

-- ============================================================================
-- 3. SERVICE_CONTEXT_QUESTIONS TABLE
-- ============================================================================
-- Question TEMPLATES associated with a service context profile.
-- These are reusable, ordered questions that seed the AI briefing flow for a
-- given service type. Distinct from briefing_questions (which are per-session,
-- AI-generated during the live discovery flow — created in V3).
--
-- Design note: `service_context_questions` are the static template layer.
-- At runtime, the AI uses these as starting context to generate
-- session-specific `briefing_questions` (V3 table) with dynamic follow-ups.
CREATE TABLE service_context_questions (
    id                          UUID          PRIMARY KEY,
    service_context_profile_id  UUID          NOT NULL,
    question_text               VARCHAR(1000) NOT NULL,
    question_type               VARCHAR(50)   NOT NULL,
    order_index                 INT           NOT NULL,
    is_required                 BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_service_context_questions_profile
        FOREIGN KEY (service_context_profile_id)
        REFERENCES service_context_profiles(id) ON DELETE CASCADE,

    CONSTRAINT ck_service_context_questions_type
        CHECK (question_type IN (
            'OPEN_ENDED', 'MULTIPLE_CHOICE', 'SCALE', 'YES_NO', 'TEXT', 'TEXTAREA'
        )),
    CONSTRAINT ck_service_context_questions_order
        CHECK (order_index > 0)
);

-- Primary access pattern: fetch all questions for a profile, ordered
CREATE INDEX idx_service_context_questions_profile_order
    ON service_context_questions(service_context_profile_id, order_index);

-- Enforce: one question per order_index per profile (no gaps, no duplicates)
CREATE UNIQUE INDEX idx_service_context_questions_unique_order
    ON service_context_questions(service_context_profile_id, order_index);

COMMENT ON TABLE service_context_questions IS
'Question templates for a service context profile. Static seed layer used by AI
to generate session-specific briefing_questions (V3). Ordered via order_index.
Distinct from briefing_questions which are per-session and AI-generated dynamically.';
COMMENT ON COLUMN service_context_questions.question_text IS
'Template question text (max 1000 chars). AI may rephrase for specific clients.';
COMMENT ON COLUMN service_context_questions.question_type IS
'OPEN_ENDED (free text), MULTIPLE_CHOICE (options JSON), SCALE (1-10),
YES_NO (boolean choice), TEXT (single line), TEXTAREA (multiline).';
COMMENT ON COLUMN service_context_questions.order_index IS
'Presentation order (1-based, no gaps enforced by UNIQUE constraint).';
COMMENT ON COLUMN service_context_questions.is_required IS
'Whether this question must be answered for briefing completion scoring.';

-- ============================================================================
-- END OF V8 MIGRATION
-- ============================================================================
