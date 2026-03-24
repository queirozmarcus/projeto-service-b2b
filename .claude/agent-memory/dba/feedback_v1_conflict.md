---
name: V1 draft conflict pattern
description: Pattern for handling V1 bootstrap draft tables that conflict with domain-specific migrations
type: feedback
---

# V1 Draft Tables Conflict Pattern

**Rule:** When a new domain migration targets tables that already exist from a V1 bootstrap draft, the migration must DROP the draft tables before recreating them with correct constraints.

**Why:** V1 was created as a quick bootstrap to unblock JPA entity development. It contains incomplete constraints (no CHECK on status enums, no composite indexes, missing triggers, incorrect default values). When backend-dev delivers domain migrations (V2, V3, V4), they define the authoritative schema — V1 is superseded, not extended.

**How to apply:**
1. Compare V1 table DDL against the new domain migration DDL
2. If the domain migration creates the same table name, add DROP TABLE IF EXISTS CASCADE before CREATE TABLE
3. Drop in reverse dependency order (child tables before parent tables)
4. Include comment explaining why DROP is safe (no production data in this sprint)
5. For future sprints (post-go-live), use expand-then-contract instead — DROP is only safe when there is no production data

**Tables V1 created that were superseded:**
- users, workspaces, workspace_members → V2 (authoritative)
- briefing_sessions, briefing_questions, briefing_answers → V3 (authoritative)
- proposals, proposal_versions, approval_workflows, approvals → V4 (authoritative)
- kickoff_summaries, project_artifacts, notifications, services, projects, project_services → dropped in V4, to be recreated in V5
