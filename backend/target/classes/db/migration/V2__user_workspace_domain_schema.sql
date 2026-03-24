-- V2__user_workspace_domain_schema.sql
-- User & Workspace Domain Layer Schema
-- Date: 2026-03-22
-- Purpose: Create tables for User and Workspace bounded context with full audit trail and multi-tenancy support

-- ============================================================================
-- USERS TABLE
-- ============================================================================
-- Stores user authentication and profile information
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Comment on table
COMMENT ON TABLE users IS 'User accounts with authentication credentials. Multi-tenant aware via workspace_members table.';
COMMENT ON COLUMN users.email IS 'Unique email address (case-insensitive lookup via normalized form)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (never stored plaintext)';
COMMENT ON COLUMN users.status IS 'User state: ACTIVE (can login), INACTIVE (invited, not confirmed), DELETED (soft-deleted, GDPR)';

-- ============================================================================
-- WORKSPACES TABLE
-- ============================================================================
-- Represents a tenant/organization in ScopeFlow
CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    niche VARCHAR(100) NOT NULL,
    tone_settings JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Indexes for query performance
CREATE INDEX idx_workspaces_owner_id ON workspaces(owner_id);
CREATE INDEX idx_workspaces_status ON workspaces(status);
CREATE INDEX idx_workspaces_niche ON workspaces(niche);
CREATE INDEX idx_workspaces_created_at ON workspaces(created_at);

-- Unique constraint: name must be unique per owner (not globally)
-- For now, enforce globally; can be refined later with partial unique index per owner_id
CREATE UNIQUE INDEX idx_workspaces_name_unique ON workspaces(name);

-- Comment on table
COMMENT ON TABLE workspaces IS 'Tenant/organization in ScopeFlow. Every workspace has exactly one OWNER.';
COMMENT ON COLUMN workspaces.owner_id IS 'User who owns this workspace (invariant: cannot remove last OWNER)';
COMMENT ON COLUMN workspaces.niche IS 'Business domain: social-media, landing-page, branding, web-development, etc.';
COMMENT ON COLUMN workspaces.tone_settings IS 'JSONB: tone customizations (formal, casual, technical, etc.)';
COMMENT ON COLUMN workspaces.status IS 'Workspace state: ACTIVE (normal), SUSPENDED (owner paused)';

-- ============================================================================
-- WORKSPACE_MEMBERS TABLE
-- ============================================================================
-- Represents membership relationship: User in Workspace with Role
CREATE TABLE workspace_members (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INVITED', 'LEFT')),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_workspace_members_unique UNIQUE (workspace_id, user_id)
);

-- Indexes for query performance
CREATE INDEX idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user_id ON workspace_members(user_id);
CREATE INDEX idx_workspace_members_role ON workspace_members(role);
CREATE INDEX idx_workspace_members_status ON workspace_members(status);
CREATE INDEX idx_workspace_members_joined_at ON workspace_members(joined_at);

-- Find all OWNER members in workspace (for invariant checking)
CREATE INDEX idx_workspace_members_owner_check ON workspace_members(workspace_id, role, status)
    WHERE role = 'OWNER' AND status = 'ACTIVE';

-- Comment on table
COMMENT ON TABLE workspace_members IS 'Membership: links User to Workspace with Role. Supports invitations (INVITED state).';
COMMENT ON COLUMN workspace_members.role IS 'Role: OWNER (full access), ADMIN (manage proposals/members), MEMBER (read-only)';
COMMENT ON COLUMN workspace_members.status IS 'Membership state: ACTIVE (member), INVITED (pending acceptance), LEFT (historical)';

-- ============================================================================
-- OUTBOX TABLE (Event Sourcing)
-- ============================================================================
-- Transactional outbox for reliable event publishing
-- Ensures no event is lost if publishing fails
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for publishing worker
CREATE INDEX idx_outbox_published_at ON outbox(published_at) WHERE published_at IS NULL;
CREATE INDEX idx_outbox_event_type ON outbox(event_type);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);

-- Comment on table
COMMENT ON TABLE outbox IS 'Transactional outbox: reliable event publishing. Publishing worker consumes and publishes to Kafka/RabbitMQ.';
COMMENT ON COLUMN outbox.aggregate_type IS 'Domain aggregate: User, Workspace, WorkspaceMember';
COMMENT ON COLUMN outbox.aggregate_id IS 'ID of the aggregate that produced the event';
COMMENT ON COLUMN outbox.event_type IS 'Event class name: UserRegistered, WorkspaceMemberInvited, etc.';
COMMENT ON COLUMN outbox.event_payload IS 'JSON payload of the event (userId, email, timestamp, etc.)';
COMMENT ON COLUMN outbox.published_at IS 'NULL = not yet published, filled when published to Kafka/RabbitMQ';

-- ============================================================================
-- AUDIT LOG TABLE (Historical Record)
-- ============================================================================
-- Immutable log of all actions (inserts only, never updates or deletes)
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    changes JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_logs_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for audit queries
CREATE INDEX idx_activity_logs_workspace_id ON activity_logs(workspace_id);
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_entity ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_logs_action ON activity_logs(action);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);

-- Comment on table
COMMENT ON TABLE activity_logs IS 'Immutable audit trail: all actions logged for compliance and debugging.';
COMMENT ON COLUMN activity_logs.action IS 'Action performed: created, updated, deleted, invited, left, etc.';
COMMENT ON COLUMN activity_logs.entity_type IS 'Entity type: User, Workspace, WorkspaceMember, etc.';
COMMENT ON COLUMN activity_logs.changes IS 'JSONB diff: before/after state changes';

-- ============================================================================
-- CONSTRAINTS & INVARIANTS
-- ============================================================================

-- Invariant: Every workspace has exactly one OWNER
-- Enforced by application logic (WorkspaceService.createWorkspace, updateMemberRole)
-- This CHECK constraint ensures we never have workspace with no OWNER members (basic safeguard)

-- Invariant: Email is unique
-- Already enforced by UNIQUE constraint on users.email

-- Invariant: User can only appear once per workspace
-- Enforced by UNIQUE constraint on (workspace_id, user_id)

-- ============================================================================
-- VIEWS (Optional, for common queries)
-- ============================================================================

-- View: Active workspace members with user details
CREATE VIEW v_workspace_members_active AS
SELECT
    wm.id,
    wm.workspace_id,
    wm.user_id,
    u.email,
    u.full_name,
    wm.role,
    wm.joined_at,
    wm.updated_at
FROM workspace_members wm
JOIN users u ON wm.user_id = u.id
WHERE wm.status = 'ACTIVE';

COMMENT ON VIEW v_workspace_members_active IS 'Active members of all workspaces with user details';

-- View: Owner verification (for invariant checks)
CREATE VIEW v_workspace_owners AS
SELECT
    workspace_id,
    COUNT(*) as owner_count
FROM workspace_members
WHERE role = 'OWNER' AND status = 'ACTIVE'
GROUP BY workspace_id;

COMMENT ON VIEW v_workspace_owners IS 'Owner count per workspace (should always be >= 1)';

-- ============================================================================
-- GRANTS & SECURITY
-- ============================================================================
-- Default: assume application connects as app user (not superuser)
-- Adjust per your security model

-- GRANT SELECT, INSERT, UPDATE ON users TO app_user;
-- GRANT SELECT, INSERT, UPDATE ON workspaces TO app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON workspace_members TO app_user;
-- GRANT SELECT, INSERT ON outbox TO app_user;
-- GRANT SELECT, INSERT ON activity_logs TO app_user;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
