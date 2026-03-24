---
name: ScopeFlow schema state after V4
description: Current migration state, table inventory, key constraints and design decisions for ScopeFlow PostgreSQL schema
type: project
---

# Schema State After V4 (2026-03-24)

Applied migrations: V1 (superseded draft), V2 (User/Workspace), V3 (Briefing), V4 (Proposal).

## Active tables by domain

**User/Workspace (V2):** users, workspaces, workspace_members, outbox, activity_logs
**Briefing (V3):** briefing_sessions, briefing_questions, briefing_answers, ai_generations, briefing_activity_logs
**Proposal (V4):** proposals, proposal_versions, approval_workflows, approvals

## Key invariants enforced at DB level

- `workspace_members(workspace_id, user_id)` UNIQUE
- `briefing_sessions(workspace_id, client_id, service_type)` UNIQUE WHERE status='IN_PROGRESS' — prevents duplicate active briefing
- `approval_workflows(proposal_id)` UNIQUE — 1 workflow per proposal
- `approvals(workflow_id, approver_email)` UNIQUE — 1 decision per approver per workflow
- `briefing_answers` immutable via trigger `briefing_answers_immutable_trigger`
- `proposal_versions` immutable via trigger `proposal_versions_immutable_trigger`
- `completion_score >= 80` required when `briefing_sessions.status = 'COMPLETED'`

## Critical design choices

- `client_id` in briefing_sessions and proposals is NOT a FK — cross-domain boundary, intentional
- `briefing_id` in proposals uses ON DELETE RESTRICT — cannot delete briefing while proposal exists
- `proposals.scope_json` is denormalized snapshot for fast reads (also stored in proposal_versions)
- `approvals.status` CHECK is ('PENDING','APPROVED','REJECTED') — no IN_PROGRESS (individual decision is atomic)
- `update_updated_at_column()` function defined in V3, reused in V4

## Redundant indexes to remove in V5

- `idx_users_email` (redundant with UNIQUE users_email_key)
- `idx_approval_workflows_proposal_id` (redundant with UNIQUE uq_approval_workflows_proposal)
- `idx_approvals_workflow_email` (redundant with UNIQUE uq_approvals_workflow_approver)
- `idx_briefing_questions_step` (redundant with UNIQUE idx_briefing_questions_unique_step)

## Tables dropped in V4 (V1 drafts, to be recreated in V5)

kickoff_summaries, project_artifacts, notifications, services, projects, project_services

**Why:** These V1 drafts had incorrect or incomplete design. V5 will recreate with correct constraints when their domain is properly designed.
