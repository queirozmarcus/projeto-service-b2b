-- V4__proposal_domain_schema.sql
-- Proposal Domain Layer Schema
-- Date: 2026-03-24
-- Purpose: proposals, proposal_versions, approval_workflows, approvals

-- ============================================================================
-- PROPOSALS TABLE
-- ============================================================================
CREATE TABLE proposals (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    client_id UUID NOT NULL,
    briefing_id UUID NOT NULL,
    proposal_name VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'APPROVED', 'REJECTED')),
    scope_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_proposals_workspace FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_proposals_briefing FOREIGN KEY (briefing_id)
        REFERENCES briefing_sessions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_proposals_workspace_id ON proposals(workspace_id);
CREATE INDEX idx_proposals_client_id ON proposals(client_id);
CREATE INDEX idx_proposals_briefing_id ON proposals(briefing_id);
CREATE INDEX idx_proposals_status ON proposals(status);
CREATE INDEX idx_proposals_created_at ON proposals(created_at);

COMMENT ON TABLE proposals IS 'Proposal aggregate: lifecycle from DRAFT to APPROVED/REJECTED';
COMMENT ON COLUMN proposals.scope_json IS 'JSONB: current scope snapshot (deliverables, exclusions, price, timeline)';
COMMENT ON COLUMN proposals.version IS 'Optimistic locking version counter';

-- ============================================================================
-- PROPOSAL_VERSIONS TABLE (Immutable history)
-- ============================================================================
CREATE TABLE proposal_versions (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL,
    scope_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    CONSTRAINT fk_proposal_versions_proposal FOREIGN KEY (proposal_id)
        REFERENCES proposals(id) ON DELETE CASCADE
);

CREATE INDEX idx_proposal_versions_proposal_id ON proposal_versions(proposal_id);
CREATE INDEX idx_proposal_versions_created_at ON proposal_versions(created_at);

COMMENT ON TABLE proposal_versions IS 'Immutable version snapshots of proposal scope. Insert-only (never update or delete).';
COMMENT ON COLUMN proposal_versions.scope_json IS 'JSONB: complete scope at this version (deliverables, exclusions, price, timeline)';
COMMENT ON COLUMN proposal_versions.created_by IS 'UserId who created this version';

-- ============================================================================
-- APPROVAL_WORKFLOWS TABLE
-- ============================================================================
CREATE TABLE approval_workflows (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED')),
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_approval_workflows_proposal FOREIGN KEY (proposal_id)
        REFERENCES proposals(id) ON DELETE CASCADE
);

CREATE INDEX idx_approval_workflows_proposal_id ON approval_workflows(proposal_id);
CREATE INDEX idx_approval_workflows_status ON approval_workflows(status);

COMMENT ON TABLE approval_workflows IS 'Approval workflow: tracks the approval process for a proposal.';
COMMENT ON COLUMN approval_workflows.completed_at IS 'Set when all approvals resolved (APPROVED or REJECTED)';

-- ============================================================================
-- APPROVALS TABLE
-- ============================================================================
CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    approver_name VARCHAR(255),
    approver_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED')),
    ip_address VARCHAR(45),
    user_agent TEXT,
    approved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_approvals_workflow FOREIGN KEY (workflow_id)
        REFERENCES approval_workflows(id) ON DELETE CASCADE
);

CREATE INDEX idx_approvals_workflow_id ON approvals(workflow_id);
CREATE INDEX idx_approvals_status ON approvals(status);
CREATE INDEX idx_approvals_approver_email ON approvals(approver_email);

COMMENT ON TABLE approvals IS 'Individual approver decision. One record per approver per workflow.';
COMMENT ON COLUMN approvals.ip_address IS 'Client IP captured at approval time (audit trail)';
COMMENT ON COLUMN approvals.user_agent IS 'Browser user-agent captured at approval time (audit trail)';

-- ============================================================================
-- ADD VERSION COLUMN TO USERS (Optimistic lock)
-- ============================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
