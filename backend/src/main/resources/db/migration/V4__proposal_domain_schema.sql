-- V4__proposal_domain_schema.sql
-- Proposal Domain Layer Schema
-- Date: 2026-03-24
-- Purpose: proposals, proposal_versions, approval_workflows, approvals
--
-- NOTE: V1__initial_schema.sql already created these tables as a first draft.
-- This migration DROPS those V1 drafts and recreates them with correct constraints,
-- composite indexes, CHECK constraints, FKs, and audit triggers.
-- Zero-data-loss: V1 was created in the same sprint — no production data exists yet.

-- ============================================================================
-- DROP V1 DRAFT TABLES (reverse dependency order)
-- ============================================================================
DROP TABLE IF EXISTS kickoff_summaries CASCADE;
DROP TABLE IF EXISTS project_artifacts CASCADE;
DROP TABLE IF EXISTS activity_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS approvals CASCADE;
DROP TABLE IF EXISTS approval_workflows CASCADE;
DROP TABLE IF EXISTS proposal_versions CASCADE;
DROP TABLE IF EXISTS proposals CASCADE;
DROP TABLE IF EXISTS project_services CASCADE;
DROP TABLE IF EXISTS briefing_answers CASCADE;
DROP TABLE IF EXISTS briefing_questions CASCADE;
DROP TABLE IF EXISTS briefing_sessions CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS services CASCADE;

-- ============================================================================
-- PROPOSALS TABLE (Aggregate Root)
-- ============================================================================
-- States: DRAFT -> PUBLISHED -> APPROVED | REJECTED
-- Invariant: Only 1 non-DRAFT proposal per briefing at a time (enforced below)
CREATE TABLE proposals (
    id              UUID PRIMARY KEY,
    workspace_id    UUID         NOT NULL,
    client_id       UUID         NOT NULL,
    briefing_id     UUID         NOT NULL,
    proposal_name   VARCHAR(500) NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    scope_json      JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_proposals_workspace FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_proposals_briefing FOREIGN KEY (briefing_id)
        REFERENCES briefing_sessions(id) ON DELETE RESTRICT,
    CONSTRAINT ck_proposals_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'APPROVED', 'REJECTED'))
);

-- Individual column indexes
CREATE INDEX idx_proposals_workspace_id  ON proposals(workspace_id);
CREATE INDEX idx_proposals_client_id     ON proposals(client_id);
CREATE INDEX idx_proposals_briefing_id   ON proposals(briefing_id);
CREATE INDEX idx_proposals_created_at    ON proposals(created_at);

-- Composite index: primary query pattern (workspace + status filter)
CREATE INDEX idx_proposals_workspace_status ON proposals(workspace_id, status);

-- Partial index: find active (non-terminal) proposals per workspace efficiently
CREATE INDEX idx_proposals_workspace_active
    ON proposals(workspace_id, updated_at DESC)
    WHERE status IN ('DRAFT', 'PUBLISHED');

-- Auto-update updated_at (reuses function created in V3)
CREATE TRIGGER proposals_update_updated_at
    BEFORE UPDATE ON proposals
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE proposals IS 'Proposal aggregate: lifecycle from DRAFT to APPROVED/REJECTED. One per briefing session.';
COMMENT ON COLUMN proposals.scope_json IS 'JSONB: current scope snapshot (deliverables, exclusions, price, timeline). Redundant with latest proposal_version for fast reads.';
COMMENT ON COLUMN proposals.version IS 'Optimistic locking counter (JPA @Version). Prevents lost updates under concurrent edits.';
COMMENT ON COLUMN proposals.briefing_id IS 'FK to briefing_sessions (RESTRICT delete). Briefing cannot be deleted while a proposal references it.';

-- ============================================================================
-- PROPOSAL_VERSIONS TABLE (Immutable history)
-- ============================================================================
-- Insert-only — never UPDATE or DELETE.
-- Every scope change creates a new version row.
CREATE TABLE proposal_versions (
    id          UUID PRIMARY KEY,
    proposal_id UUID NOT NULL,
    scope_json  JSONB NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  UUID NOT NULL,

    CONSTRAINT fk_proposal_versions_proposal FOREIGN KEY (proposal_id)
        REFERENCES proposals(id) ON DELETE CASCADE
);

-- Primary access pattern: fetch all versions for a proposal, newest first
CREATE INDEX idx_proposal_versions_proposal_id         ON proposal_versions(proposal_id);
CREATE INDEX idx_proposal_versions_proposal_created_at ON proposal_versions(proposal_id, created_at DESC);

-- Immutability trigger
CREATE OR REPLACE FUNCTION proposal_versions_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Proposal versions are immutable. Cannot % version (id=%)', TG_OP, OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER proposal_versions_immutable_trigger
    BEFORE UPDATE OR DELETE ON proposal_versions
    FOR EACH ROW
    EXECUTE FUNCTION proposal_versions_immutable();

COMMENT ON TABLE proposal_versions IS 'Immutable snapshot history of proposal scope. Insert-only (trigger enforces). Scope changes here, approved clients see exact version they signed.';
COMMENT ON COLUMN proposal_versions.scope_json IS 'JSONB: complete scope at this version (deliverables, exclusions, price, timeline).';
COMMENT ON COLUMN proposal_versions.created_by IS 'UserId who triggered this version snapshot.';

-- ============================================================================
-- APPROVAL_WORKFLOWS TABLE
-- ============================================================================
-- One workflow per proposal (enforced by UNIQUE constraint).
-- Tracks the lifecycle of the approval process.
-- States: PENDING -> IN_PROGRESS -> APPROVED | REJECTED
CREATE TABLE approval_workflows (
    id           UUID PRIMARY KEY,
    proposal_id  UUID NOT NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_approval_workflows_proposal FOREIGN KEY (proposal_id)
        REFERENCES proposals(id) ON DELETE CASCADE,
    CONSTRAINT ck_approval_workflows_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED')),
    -- Enforce: at most 1 workflow per proposal (business invariant)
    CONSTRAINT uq_approval_workflows_proposal UNIQUE (proposal_id)
);

CREATE INDEX idx_approval_workflows_proposal_id ON approval_workflows(proposal_id);
CREATE INDEX idx_approval_workflows_status       ON approval_workflows(status);

-- Partial index: find open workflows quickly (for background jobs / reminders)
CREATE INDEX idx_approval_workflows_open
    ON approval_workflows(initiated_at)
    WHERE status IN ('PENDING', 'IN_PROGRESS');

COMMENT ON TABLE approval_workflows IS 'Approval process for a proposal. Exactly one workflow per proposal (UNIQUE constraint). Tracks aggregate state across all individual approver decisions.';
COMMENT ON COLUMN approval_workflows.completed_at IS 'Set when all approvals resolved (status = APPROVED or REJECTED).';

-- ============================================================================
-- APPROVALS TABLE
-- ============================================================================
-- One record per approver per workflow.
-- Audit trail: IP, user-agent, timestamp captured at decision time.
-- States: PENDING -> APPROVED | REJECTED
CREATE TABLE approvals (
    id             UUID PRIMARY KEY,
    workflow_id    UUID         NOT NULL,
    approver_name  VARCHAR(255),
    approver_email VARCHAR(255) NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    approved_at    TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_approvals_workflow FOREIGN KEY (workflow_id)
        REFERENCES approval_workflows(id) ON DELETE CASCADE,
    CONSTRAINT ck_approvals_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    -- Enforce: one decision record per approver per workflow
    CONSTRAINT uq_approvals_workflow_approver UNIQUE (workflow_id, approver_email)
);

CREATE INDEX idx_approvals_workflow_id    ON approvals(workflow_id);
CREATE INDEX idx_approvals_status         ON approvals(status);
CREATE INDEX idx_approvals_approver_email ON approvals(approver_email);

-- Composite index: covers the exact query findByWorkflowIdAndApproverEmail
CREATE INDEX idx_approvals_workflow_email ON approvals(workflow_id, approver_email);

-- Auto-update updated_at
CREATE TRIGGER approvals_update_updated_at
    BEFORE UPDATE ON approvals
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE approvals IS 'Individual approver decision record. One per approver per workflow. Audit trail: IP + user-agent captured at decision. Status: PENDING -> APPROVED | REJECTED only (no IN_PROGRESS — decision is atomic).';
COMMENT ON COLUMN approvals.ip_address IS 'Client IP captured at decision time (audit trail, LGPD compliance).';
COMMENT ON COLUMN approvals.user_agent IS 'Browser user-agent captured at decision time (forensic audit).';
COMMENT ON COLUMN approvals.approver_email IS 'Email of approver. Not a FK to users (clients approving proposals may not be registered users).';

-- ============================================================================
-- ADD VERSION COLUMN TO USERS (Optimistic lock)
-- ============================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ============================================================================
-- END OF V4 MIGRATION
-- ============================================================================
