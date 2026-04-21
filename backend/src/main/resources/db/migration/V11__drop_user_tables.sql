-- V11__drop_user_tables.sql
-- Purpose: Decommission User context from monolith (extracted to user-service)
-- Date: 2026-04-21
-- Migration Strategy: Strangler Fig — User Service now owns user data
-- DB-per-service: scopeflow_users (PostgreSQL dedicated, port 5433)
--
-- CRITICAL SAFETY CHECKS BEFORE RUNNING THIS MIGRATION:
-- 1. ✅ User-service is deployed and stable in production
-- 2. ✅ Traefik routes /api/v1/auth/* and /api/v1/users/* to user-service
-- 3. ✅ JWT_SECRET is identical in both services (tokens are cross-compatible)
-- 4. ✅ Full database backup created (pg_dump scopeflow > backup.sql)
-- 5. ✅ Zero active sessions using monolith user endpoints
--
-- ROLLBACK PLAN:
-- If issues arise, restore from backup:
--   psql -U postgres -d scopeflow < backup.sql
--
-- ============================================================================
-- FOREIGN KEY CONSTRAINTS TO DROP
-- ============================================================================
-- These tables have FKs pointing to users table:
--
-- 1. workspaces.owner_id → users.id (ON DELETE RESTRICT)
--    Strategy: Convert owner_id to UUID (no FK) — user-service owns user data
--
-- 2. workspace_members.user_id → users.id (ON DELETE CASCADE)
--    Strategy: Convert user_id to UUID (no FK) — eventual consistency via events
--
-- 3. activity_logs.user_id → users.id (ON DELETE SET NULL)
--    Strategy: Already nullable — safe to remove FK
--
-- 4. proposal_versions.created_by → users.id (NO FK CONSTRAINT)
--    Strategy: No action needed — column is UUID without FK
--
-- NOTE: proposal_versions.created_by does NOT have a FK constraint to users.
-- V4__proposal_domain_schema.sql line 116 documents it as "UserId" but no FK was created.
-- This is intentional — proposal versions are immutable audit records, and user deletions
-- should not cascade to proposal history.
--
-- ============================================================================
-- STEP 1: DROP FOREIGN KEY CONSTRAINTS
-- ============================================================================

-- Drop FK from workspaces table
ALTER TABLE workspaces DROP CONSTRAINT IF EXISTS fk_workspaces_owner;

-- Drop FK from workspace_members table
ALTER TABLE workspace_members DROP CONSTRAINT IF EXISTS fk_workspace_members_user;

-- Drop FK from activity_logs table
ALTER TABLE activity_logs DROP CONSTRAINT IF EXISTS fk_activity_logs_user;

-- ============================================================================
-- STEP 2: DROP VIEWS REFERENCING USERS TABLE
-- ============================================================================

DROP VIEW IF EXISTS v_workspace_members_active CASCADE;
DROP VIEW IF EXISTS v_workspace_owners CASCADE;

-- ============================================================================
-- STEP 3: DROP USERS TABLE
-- ============================================================================
-- CASCADE will drop any remaining dependent objects (indexes, triggers)

DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- STEP 4: ADD COMMENTS TO ORPHANED COLUMNS
-- ============================================================================
-- Document that these columns now reference user-service data

COMMENT ON COLUMN workspaces.owner_id IS 
'UUID of workspace owner. References user-service DB (scopeflow_users.users.id). No FK — cross-service boundary.';

COMMENT ON COLUMN workspace_members.user_id IS 
'UUID of workspace member. References user-service DB (scopeflow_users.users.id). No FK — cross-service boundary.';

COMMENT ON COLUMN activity_logs.user_id IS 
'UUID of user who performed action. References user-service DB (scopeflow_users.users.id). No FK — cross-service boundary. Nullable for system actions.';

COMMENT ON COLUMN proposal_versions.created_by IS 
'UUID of user who created this version. References user-service DB (scopeflow_users.users.id). No FK — immutable audit record.';

-- ============================================================================
-- POST-MIGRATION VALIDATION
-- ============================================================================
-- Run these queries to validate the migration:
--
-- 1. Confirm users table is gone:
--    SELECT table_name FROM information_schema.tables 
--    WHERE table_schema = 'public' AND table_name = 'users';
--    Expected: 0 rows
--
-- 2. Confirm orphaned columns still exist:
--    SELECT column_name, table_name FROM information_schema.columns
--    WHERE table_schema = 'public' AND column_name IN ('owner_id', 'user_id', 'created_by');
--    Expected: workspaces.owner_id, workspace_members.user_id, activity_logs.user_id, proposal_versions.created_by
--
-- 3. Confirm no FK constraints reference users:
--    SELECT constraint_name, table_name FROM information_schema.table_constraints
--    WHERE constraint_type = 'FOREIGN KEY' AND constraint_name LIKE '%user%';
--    Expected: 0 rows (or only constraints NOT pointing to users table)
--
-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
