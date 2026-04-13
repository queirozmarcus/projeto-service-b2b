---
name: ScopeFlow schema state after V9
description: Current migration state (V1-V9), table inventory, key design decisions, and documentation gaps identified in docs/database/
type: project
---

# Schema State After V9 (2026-04-04+)

Applied migrations: V1 (superseded draft), V2 (User/Workspace), V3 (Briefing), V4 (Proposal),
V5 (outbox_event table), V6 (idempotency_record table), V7 (fix check_event_type constraint),
V8 (soft delete on proposals + service_context_profiles + service_context_questions),
V9 (10+ composite indexes audit).

## Active tables by domain

**User/Workspace (V2):** users, workspaces, workspace_members, outbox, activity_logs
**Briefing (V3):** briefing_sessions, briefing_questions, briefing_answers, ai_generations, briefing_activity_logs
**Proposal (V4):** proposals, proposal_versions, approval_workflows, approvals
**Outbox/Events (V5):** outbox_event (table actually used by OutboxService — distinct from `outbox` in V2)
**Idempotency (V6):** idempotency_record (listener-scoped idempotency, not HTTP header-based)
**Service Context (V8):** service_context_profiles, service_context_questions

## IMPORTANT: Two outbox tables coexist in the monolith DB

- `outbox` (V2): original design, columns: aggregate_type, aggregate_id, event_type, event_payload, published_at
- `outbox_event` (V5): table actually used by OutboxService, columns: event_type, aggregate_id, aggregate_type, payload, published_at, updated_at + check constraint on event_type (FQCN format, fixed in V7)
Both exist in public schema. This is a known inconsistency — `outbox` (V2) may be a dead table.

## Soft delete on proposals

V8 added `deleted_at TIMESTAMPTZ NULL` to proposals. Hibernate uses @SQLRestriction("deleted_at IS NULL").
All queries on proposals must use indexes with WHERE deleted_at IS NULL (partial indexes added in V8 and V9).

## Key invariants enforced at DB level

- `workspace_members(workspace_id, user_id)` UNIQUE
- `briefing_sessions(workspace_id, client_id, service_type)` UNIQUE WHERE status='IN_PROGRESS'
- `approval_workflows(proposal_id)` UNIQUE — 1 workflow per proposal
- `approvals(workflow_id, approver_email)` UNIQUE — 1 decision per approver per workflow
- `briefing_answers` immutable via trigger `briefing_answers_immutable_trigger`
- `proposal_versions` immutable via trigger `proposal_versions_immutable_trigger`
- `completion_score >= 80` required when `briefing_sessions.status = 'COMPLETED'`
- `service_context_profiles(workspace_id, service_type)` UNIQUE WHERE is_active=TRUE — one active profile per service type per workspace
- `service_context_questions(service_context_profile_id, order_index)` UNIQUE

## Key composite indexes added in V9

- ix_briefing_sessions_workspace_client ON briefing_sessions(workspace_id, client_id)
- ix_briefing_sessions_workspace_status_created ON briefing_sessions(workspace_id, status, created_at DESC)
- ix_briefing_sessions_public_token_unique UNIQUE ON briefing_sessions(public_token)
- ix_proposals_workspace_client ON proposals(workspace_id, client_id) WHERE deleted_at IS NULL
- ix_proposals_workspace_status_updated ON proposals(workspace_id, status, updated_at DESC) WHERE deleted_at IS NULL
- ix_proposal_versions_proposal_created_desc ON proposal_versions(proposal_id, created_at DESC)
- ix_approval_workflows_status_completed ON approval_workflows(status, completed_at DESC)
- ix_approvals_workflow_status ON approvals(workflow_id, status)
- ix_outbox_event_aggregate_id ON outbox_event(aggregate_id)
- ix_activity_logs_workspace_created ON activity_logs(workspace_id, created_at DESC)
- ix_activity_logs_workspace_user ON activity_logs(workspace_id, user_id, created_at DESC)

## User Service DB (scopeflow_users, porta 5433)

Separate DB with single migration V1__create_users_table.sql. Schema mirrors monolith users table
(V2 + V4 columns including version BIGINT). Applied 2026-04-12.

## Documentation gap (identified 2026-04-13)

docs/database/ is frozen at V4 state. All four documents (schema-diagram.md, flyway-changelog.md,
index-strategy.md, query-performance-baseline.md) need updates to reflect V5-V9.
Critical: flyway-changelog.md "Upcoming Migrations" V5-V9 describe features that were never
implemented as planned — the actual V5-V9 are completely different from what was projected.

## Redundant indexes to review

- `idx_users_email` — redundant with UNIQUE users_email_key (V2)
- `idx_approval_workflows_proposal_id` — redundant with UNIQUE uq_approval_workflows_proposal (V4)
- `idx_approvals_workflow_email` — redundant with UNIQUE uq_approvals_workflow_approver (V4)
- `idx_briefing_questions_step` — redundant with UNIQUE idx_briefing_questions_unique_step (V3)
- `idx_proposal_versions_proposal_id` — may be redundant with ix_proposal_versions_proposal_created_desc (V9)
- `idx_briefing_sessions_public_token` — B-Tree, coexists with UNIQUE constraint implicit index and V9 ix_briefing_sessions_public_token_unique

## Tables dropped in V4 (V1 drafts)

kickoff_summaries, project_artifacts, notifications, services, projects, project_services
Status: still not recreated as of V9. Noted in changelog as "planned for V5" but V5 implemented outbox_event instead.
