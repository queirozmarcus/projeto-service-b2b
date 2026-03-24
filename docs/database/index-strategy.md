# ScopeFlow — Index Strategy

**Version:** V4 (post-migration)
**Database:** PostgreSQL 16
**Updated:** 2026-03-24

---

## Index Inventory

### users

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `users_pkey` | `id` | PK | Primary lookup |
| `users_email_key` | `email` | UNIQUE | Login lookup; email must be globally unique |
| `idx_users_email` | `email` | B-Tree | Redundant with unique constraint — exists for compatibility; consider dropping |
| `idx_users_status` | `status` | B-Tree | Filter active/inactive/deleted users in admin queries |
| `idx_users_created_at` | `created_at` | B-Tree | Pagination and time-range audit queries |

**Note:** `idx_users_email` is redundant with the UNIQUE constraint. The UNIQUE constraint already creates an implicit index. The explicit CREATE INDEX can be dropped in V5 without functional change.

---

### workspaces

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `workspaces_pkey` | `id` | PK | Primary lookup |
| `idx_workspaces_name_unique` | `name` | UNIQUE | Business rule: globally unique workspace name (MVP constraint) |
| `idx_workspaces_owner_id` | `owner_id` | B-Tree | Query all workspaces by owner (`findByOwnerId`) |
| `idx_workspaces_status` | `status` | B-Tree | Filter active/suspended workspaces |
| `idx_workspaces_niche` | `niche` | B-Tree | Analytics: group by business niche |
| `idx_workspaces_created_at` | `created_at` | B-Tree | Time-range queries, onboarding analytics |

---

### workspace_members

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `workspace_members_pkey` | `id` | PK | Primary lookup |
| `uk_workspace_members_unique` | `(workspace_id, user_id)` | UNIQUE | Invariant: user appears once per workspace |
| `idx_workspace_members_workspace_id` | `workspace_id` | B-Tree | Find all members of a workspace |
| `idx_workspace_members_user_id` | `user_id` | B-Tree | Find all workspaces a user belongs to |
| `idx_workspace_members_role` | `role` | B-Tree | Filter by role (OWNER, ADMIN, MEMBER) |
| `idx_workspace_members_status` | `status` | B-Tree | Filter active/invited/left members |
| `idx_workspace_members_joined_at` | `joined_at` | B-Tree | Onboarding analytics |
| `idx_workspace_members_owner_check` | `(workspace_id, role, status)` WHERE role='OWNER' AND status='ACTIVE' | Partial B-Tree | Invariant check: find active owners of a workspace. Covers `COUNT(*) WHERE role='OWNER'` efficiently |

---

### outbox

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `outbox_pkey` | `id` | PK | Primary lookup |
| `idx_outbox_published_at` | `published_at` WHERE `published_at IS NULL` | Partial B-Tree | Publishing worker: fetch only unpublished events. Partial index keeps it small as published events accumulate |
| `idx_outbox_event_type` | `event_type` | B-Tree | Debug: find all events of a given type |
| `idx_outbox_created_at` | `created_at` | B-Tree | Ordered processing; dead-letter detection (old unpublished events) |

---

### activity_logs

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `activity_logs_pkey` | `id` | PK | Primary lookup |
| `idx_activity_logs_workspace_id` | `workspace_id` | B-Tree | Tenant-scoped audit log queries |
| `idx_activity_logs_user_id` | `user_id` | B-Tree | Audit: all actions by a specific user |
| `idx_activity_logs_entity` | `(entity_type, entity_id)` | B-Tree | Audit: all events for a specific entity (e.g., all changes to proposal X) |
| `idx_activity_logs_action` | `action` | B-Tree | Filter by action type |
| `idx_activity_logs_created_at` | `created_at` | B-Tree | Time-range audit queries; retention purge |

---

### briefing_sessions

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `briefing_sessions_pkey` | `id` | PK | Primary lookup |
| `briefing_sessions_public_token_key` | `public_token` | UNIQUE | Public link access — must be unique globally |
| `idx_briefing_sessions_workspace_id` | `workspace_id` | B-Tree | Tenant-scoped listing |
| `idx_briefing_sessions_status` | `status` | B-Tree | Filter by status |
| `idx_briefing_sessions_client_id` | `client_id` | B-Tree | Find briefings by client |
| `idx_briefing_sessions_service_type` | `service_type` | B-Tree | Filter by service category |
| `idx_briefing_sessions_public_token` | `public_token` | B-Tree | Public link lookup (redundant with UNIQUE — kept for explicit EXPLAIN readability) |
| `idx_briefing_sessions_created_at` | `created_at` | B-Tree | Pagination |
| `idx_briefing_sessions_active_single` | `(workspace_id, client_id, service_type)` WHERE status='IN_PROGRESS' | Partial UNIQUE | Invariant: only 1 active briefing per client per service type per workspace |
| `idx_briefing_sessions_completed_recent` | `(workspace_id, updated_at)` WHERE status='COMPLETED' | Partial B-Tree | Dashboard: recently completed briefings ready for scope generation |

---

### briefing_questions

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `briefing_questions_pkey` | `id` | PK | Primary lookup |
| `idx_briefing_questions_unique_step` | `(briefing_session_id, step)` | UNIQUE | Invariant: one question per step per session |
| `idx_briefing_questions_session_id` | `briefing_session_id` | B-Tree | Load all questions for a session |
| `idx_briefing_questions_step` | `(briefing_session_id, step)` | B-Tree | Ordered step traversal (redundant with unique — kept) |
| `idx_briefing_questions_type` | `question_type` | B-Tree | Filter by question type |
| `idx_briefing_questions_created_at` | `created_at` | B-Tree | Insertion order |
| `idx_briefing_questions_followup_generated` | `briefing_session_id` WHERE follow_up_generated=TRUE | Partial B-Tree | Find auto-generated follow-ups per session |

---

### briefing_answers

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `briefing_answers_pkey` | `id` | PK | Primary lookup |
| `idx_briefing_answers_unique_per_question` | `(briefing_session_id, question_id)` | UNIQUE | Invariant: one answer per question per session |
| `idx_briefing_answers_session_id` | `briefing_session_id` | B-Tree | Load all answers for a session |
| `idx_briefing_answers_question_id` | `question_id` | B-Tree | Find answer for a specific question |
| `idx_briefing_answers_created_at` | `created_at` | B-Tree | Ordered answer history |

---

### ai_generations

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `ai_generations_pkey` | `id` | PK | Primary lookup |
| `idx_ai_generations_session_id` | `briefing_session_id` | B-Tree | Load all AI calls for a session |
| `idx_ai_generations_type` | `generation_type` | B-Tree | Cost breakdown by type |
| `idx_ai_generations_prompt_version` | `prompt_version` | B-Tree | A/B testing: compare costs and quality per prompt version |
| `idx_ai_generations_created_at` | `created_at` | B-Tree | Time-range cost aggregation (v_ai_generation_costs view) |
| `idx_ai_generations_model_used` | `model_used` | B-Tree | Cost breakdown by model |

---

### proposals

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `proposals_pkey` | `id` | PK | Primary lookup |
| `idx_proposals_workspace_id` | `workspace_id` | B-Tree | Tenant-scoped listing (`findByWorkspaceId`) |
| `idx_proposals_client_id` | `client_id` | B-Tree | Find all proposals for a client (`findByClientIdAndWorkspaceId`) |
| `idx_proposals_briefing_id` | `briefing_id` | B-Tree | Reverse lookup: which proposal came from this briefing |
| `idx_proposals_created_at` | `created_at` | B-Tree | Pagination by creation date |
| **`idx_proposals_workspace_status`** | `(workspace_id, status)` | **Composite B-Tree** | Primary query pattern: `findByWorkspaceIdAndStatus`. Covers workspace filter + status filter in single index scan |
| `idx_proposals_workspace_active` | `(workspace_id, updated_at DESC)` WHERE status IN ('DRAFT','PUBLISHED') | Partial B-Tree | Dashboard: recently active proposals. Partial keeps index small (excludes terminal APPROVED/REJECTED) |

**Key decision:** `idx_proposals_workspace_status` is composite in left-to-right order `(workspace_id, status)` because `workspace_id` is always present in queries (tenant isolation) and `status` is the most common filter. This avoids a Bitmap Index Scan merging two separate indexes.

---

### proposal_versions

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `proposal_versions_pkey` | `id` | PK | Primary lookup |
| `idx_proposal_versions_proposal_id` | `proposal_id` | B-Tree | Covers basic FK lookups |
| **`idx_proposal_versions_proposal_created_at`** | `(proposal_id, created_at DESC)` | **Composite B-Tree** | Covers `findByProposalIdOrderByCreatedAtDesc`. DESC matches the ORDER BY direction — avoids sort step. The single-column `idx_proposal_versions_proposal_id` is redundant and can be dropped when this composite exists |

---

### approval_workflows

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `approval_workflows_pkey` | `id` | PK | Primary lookup |
| `uq_approval_workflows_proposal` | `proposal_id` | UNIQUE | Invariant: 1 workflow per proposal. Also serves as index for `findByProposalId` |
| `idx_approval_workflows_proposal_id` | `proposal_id` | B-Tree | Explicit FK index (coexists with UNIQUE — redundant, kept for JPA schema validation) |
| `idx_approval_workflows_status` | `status` | B-Tree | Filter by workflow status |
| `idx_approval_workflows_open` | `initiated_at` WHERE status IN ('PENDING','IN_PROGRESS') | Partial B-Tree | Background job: find stale open workflows for reminders / expiry |

**Note:** `uq_approval_workflows_proposal` (UNIQUE constraint) already creates an implicit index on `proposal_id`, making `idx_approval_workflows_proposal_id` redundant. Consider dropping the explicit index in V5.

---

### approvals

| Index | Columns | Type | Why |
|-------|---------|------|-----|
| `approvals_pkey` | `id` | PK | Primary lookup |
| `uq_approvals_workflow_approver` | `(workflow_id, approver_email)` | UNIQUE | Invariant: one decision per approver per workflow |
| `idx_approvals_workflow_id` | `workflow_id` | B-Tree | Load all approvers for a workflow (`findByWorkflowId`) |
| `idx_approvals_status` | `status` | B-Tree | Filter pending approvals |
| `idx_approvals_approver_email` | `approver_email` | B-Tree | Find all decisions by email (audit, LGPD data request) |
| **`idx_approvals_workflow_email`** | `(workflow_id, approver_email)` | **Composite B-Tree** | Covers `findByWorkflowIdAndApproverEmail`. Redundant with UNIQUE constraint implicit index — kept for explicit EXPLAIN readability, can be dropped |

**Note:** `uq_approvals_workflow_approver` already creates an implicit composite index on `(workflow_id, approver_email)`. The explicit `idx_approvals_workflow_email` is redundant. Drop in V5.

---

## Indexes to Remove in V5 (Cleanup)

| Index | Table | Reason |
|-------|-------|--------|
| `idx_users_email` | users | Redundant with UNIQUE constraint `users_email_key` |
| `idx_briefing_questions_step` | briefing_questions | Redundant with UNIQUE `idx_briefing_questions_unique_step` |
| `idx_approval_workflows_proposal_id` | approval_workflows | Redundant with UNIQUE `uq_approval_workflows_proposal` |
| `idx_approvals_workflow_email` | approvals | Redundant with UNIQUE `uq_approvals_workflow_approver` |

Removing these saves ~4 B-Tree index structures with no query performance impact.

---

## Index Decision Framework

```
Query pattern                      → Index choice
─────────────────────────────────────────────────
= on single column (PK/UK)         → UNIQUE or PK (implicit)
= on single column (FK, filter)    → Single B-Tree
= on col A AND = on col B          → Composite (A, B) — A with lower cardinality first if both always present
ORDER BY col DESC                  → Include col in composite with DESC
Subset of rows (partial condition) → Partial index WHERE clause
Boolean flag, unpublished state    → Partial index (small, fast)
Immutable lookup by hash/token     → UNIQUE index on that column
```
