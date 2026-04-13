# ScopeFlow — Flyway Migration Changelog

**Updated:** 2026-03-24

---

## Migration History

| Version | File | Description | Tables Added | Tables Modified | Status |
|---------|------|-------------|-------------|----------------|--------|
| V1 | `V1__initial_schema.sql` | Initial schema (full draft) | users, workspaces, workspace_members, services, projects, project_services, briefing_sessions, briefing_questions, briefing_answers, proposals, proposal_versions, approval_workflows, approvals, kickoff_summaries, project_artifacts, activity_logs, notifications | — | Superseded by V2–V4 |
| V2 | `V2__user_workspace_domain_schema.sql` | User & Workspace domain (authoritative) | users (redef), workspaces (redef), workspace_members (redef), outbox, activity_logs (redef) | — | Applied |
| V3 | `V3__briefing_domain_schema.sql` | Briefing domain (authoritative) | briefing_sessions, briefing_questions, briefing_answers, ai_generations, briefing_activity_logs | outbox (CREATE IF NOT EXISTS) | Applied |
| V4 | `V4__proposal_domain_schema.sql` | Proposal domain (authoritative) | proposals, proposal_versions, approval_workflows, approvals | users (ADD version) | Applied |

---

## V1 — Initial Schema

**File:** `V1__initial_schema.sql`
**Date:** 2026-03-22
**Author:** backend-dev (Sprint 1 bootstrap)

**Purpose:** First-pass schema for all domains — created during bootstrap to enable JPA entity development.

**Status:** Superseded. V1 tables for User/Workspace domain were redefined in V2 with stricter constraints and correct CHECK constraints. V1 proposal tables were replaced by V4 with corrected design. V4 drops all V1 draft tables before recreating.

**Notes:**
- V1 did not include CHECK constraints on VARCHAR status columns
- V1 `workspaces` had `slug` instead of `niche` — design changed in V2
- V1 `approvals` had incorrect `IN_PROGRESS` status value for individual decisions
- V1 `approval_workflows` lacked UNIQUE(proposal_id) business invariant
- V1 did not have `updated_at` triggers

---

## V2 — User & Workspace Domain

**File:** `V2__user_workspace_domain_schema.sql`
**Date:** 2026-03-22
**Author:** backend-dev (Sprint 2 — Adapter Layer)

**What changed from V1:**
- Added `CHECK` constraints on `users.status`, `workspaces.status`, `workspace_members.role`
- Replaced `workspaces.slug` with `workspaces.niche` (product pivot)
- Added `tone_settings JSONB` to workspaces
- Added `outbox` table for transactional event publishing
- Added partial index `idx_workspace_members_owner_check` for OWNER invariant enforcement
- Added views: `v_workspace_members_active`, `v_workspace_owners`
- `workspace_members` uses inline CONSTRAINT syntax instead of post-table ALTER

**Key invariants enforced:**
- `users.email` UNIQUE (login lookup)
- `workspace_members(workspace_id, user_id)` UNIQUE (no duplicate membership)
- `workspace_members.role IN ('OWNER','ADMIN','MEMBER')` CHECK
- Workspace `ON DELETE RESTRICT` for owner (cannot delete user who owns workspace)

---

## V3 — Briefing Domain

**File:** `V3__briefing_domain_schema.sql`
**Date:** 2026-03-22
**Author:** dba (Sprint 1b — Schema design)

**Tables created:**
- `briefing_sessions` — Discovery session aggregate root
- `briefing_questions` — Questions in the session (immutable step sequence)
- `briefing_answers` — Client answers (immutable via trigger)
- `ai_generations` — LLM call audit trail
- `briefing_activity_logs` — Session lifecycle audit

**Key design decisions:**
- `briefing_sessions.client_id` is NOT a FK — clients are in a different bounded context
- `briefing_answers` immutability enforced by database trigger `briefing_answers_immutable_trigger`
- Partial UNIQUE index `idx_briefing_sessions_active_single` enforces "only 1 active briefing per client per service type per workspace"
- `completion_score >= 80` required when `status = 'COMPLETED'` — enforced by CHECK constraint
- `update_updated_at_column()` function created here — reused by V4 tables

**Views created:**
- `v_briefing_sessions_active` — Active sessions with progress metrics
- `v_briefing_sessions_completed` — Completed sessions ready for scope generation
- `v_ai_generation_costs` — Daily AI cost aggregation

---

## V4 — Proposal Domain

**File:** `V4__proposal_domain_schema.sql`
**Date:** 2026-03-24
**Author:** dba (Sprint 2 — Schema validation)

**Migration strategy:** DROP and recreate. Rationale: V1 created draft versions of these tables with incorrect constraints. Since V4 runs in the same sprint with no production data, expand-then-contract is unnecessary — clean replacement is safe.

**Tables replaced (V1 → V4):**

| Table | V1 issues fixed in V4 |
|-------|----------------------|
| `proposals` | Added `ck_proposals_status` CHECK; added composite `idx_proposals_workspace_status`; added updated_at trigger; added partial index for active proposals |
| `proposal_versions` | Added immutability trigger `proposal_versions_immutable_trigger` |
| `approval_workflows` | Added `uq_approval_workflows_proposal` UNIQUE constraint (1 workflow per proposal); fixed default status from invalid `IN_PROGRESS` to correct `PENDING` |
| `approvals` | Removed `IN_PROGRESS` from CHECK (individual decision is atomic); added `UNIQUE(workflow_id, approver_email)`; added `created_at` and `updated_at` columns; added updated_at trigger |

**Tables dropped (V1 draft, not recreated in V4 — out of scope):**
- `kickoff_summaries` — Planned for V5
- `project_artifacts` — Planned for V5
- `notifications` — Planned for V5
- `services`, `projects`, `project_services` — Domain design pending review

**Columns added to existing tables:**
- `users.version BIGINT DEFAULT 0` — JPA `@Version` optimistic locking

**Fixes applied:**
1. `approval_workflows` default status was `'IN_PROGRESS'` — corrected to `'PENDING'` (workflow starts pending, not in-progress)
2. `approvals.status` CHECK included `'IN_PROGRESS'` — removed (individual approval is PENDING → APPROVED/REJECTED, no intermediate state)
3. Missing UNIQUE constraint on `approval_workflows(proposal_id)` — added
4. Missing composite index `(workspace_id, status)` on proposals — added
5. Missing `updated_at` triggers on proposals and approvals — added
6. Missing `created_at`/`updated_at` on approvals — added (audit trail requirement)

---

## Upcoming Migrations

| Version | Planned | Description |
|---------|---------|-------------|
| V5 | Sprint 3 | Kickoff summaries, project artifacts, notifications |
| V6 | Sprint 3 | Remove redundant indexes (idx_users_email, idx_approval_workflows_proposal_id, idx_approvals_workflow_email) |
| V7 | Post-MVP | Add composite `(workspace_id, status)` to `briefing_sessions` |
| V8 | Post-MVP | Add GIN index on `proposals.scope_json` for JSONB search |
| V9 | Post-MVP | Table partitioning on `activity_logs` by `created_at` (monthly) |

---

## Zero-Downtime Rules

All future migrations MUST follow expand-then-contract:

```
SAFE (no lock):
  ADD COLUMN nullable
  ADD INDEX CONCURRENTLY
  ADD CONSTRAINT CHECK (with NOT VALID, then VALIDATE)
  CREATE TABLE

UNSAFE (acquires AccessExclusiveLock):
  DROP COLUMN
  DROP TABLE
  ALTER COLUMN type
  ADD NOT NULL to existing column (without DEFAULT)
  ADD FOREIGN KEY (without NOT VALID)

Zero-downtime pattern for adding NOT NULL:
  V_n:   ADD COLUMN new_col NULLABLE       ← deploy code that populates it
  V_n+1: UPDATE ... SET new_col = default  ← backfill (batch if large table)
  V_n+2: ALTER COLUMN SET NOT NULL         ← only after all rows populated
```

---

## Flyway Verification Commands

```bash
# Run all migrations from scratch (clean + migrate)
cd backend
./mvnw flyway:clean flyway:migrate -DskipTests

# Check migration status
./mvnw flyway:info

# Validate checksums (detect accidental edits to applied migrations)
./mvnw flyway:validate

# Repair (fix failed migration state — use with caution)
./mvnw flyway:repair
```

Expected output after V4:
```
+-----------+-----------------------------+------+--------------+---------+
| Version   | Description                 | Type | Installed On | State   |
+-----------+-----------------------------+------+--------------+---------+
| 1         | initial schema              | SQL  | ...          | Success |
| 2         | user workspace domain schema| SQL  | ...          | Success |
| 3         | briefing domain schema      | SQL  | ...          | Success |
| 4         | proposal domain schema      | SQL  | ...          | Success |
+-----------+-----------------------------+------+--------------+---------+
```

---

## Rollback Strategy

Flyway Community Edition does not support automatic rollback. Rollback procedure:

```bash
# 1. Identify the migration version to roll back
./mvnw flyway:info

# 2. Execute the inverse SQL manually
# (Each migration must have a documented inverse in this changelog)
psql -U scopeflow -d scopeflow_dev -f scripts/rollback/V4_rollback.sql

# 3. Delete the migration record from flyway_schema_history
DELETE FROM flyway_schema_history WHERE version = '4';

# 4. Redeploy previous application version

# 5. Verify
./mvnw flyway:info  -- Should show V4 as missing/not applied
```

V4 inverse (conceptual — drop tables added in V4, readd V1 drafts):
```sql
-- V4 rollback: drop V4 tables (re-creation of V1 drafts is handled by flyway:clean + re-migrate to V1)
DROP TABLE IF EXISTS approvals CASCADE;
DROP TABLE IF EXISTS approval_workflows CASCADE;
DROP TABLE IF EXISTS proposal_versions CASCADE;
DROP TABLE IF EXISTS proposals CASCADE;
ALTER TABLE users DROP COLUMN IF EXISTS version;
```
