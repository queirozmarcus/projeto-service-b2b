# ScopeFlow — Query Performance Baseline

**Version:** V4 (post-migration)
**Database:** PostgreSQL 16
**Updated:** 2026-03-24

> This document records expected EXPLAIN ANALYZE behavior for critical queries.
> Actual execution plans must be validated against a database populated with
> representative data volumes using Testcontainers integration tests.
> Update this document after each significant data volume change (100k+ rows).

---

## How to Run Baselines

```sql
-- Enable timing and buffers for all EXPLAIN runs
SET enable_seqscan = OFF;  -- Use during index validation only; remove for realistic plans

EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
<query here>;
```

```bash
# Via psql in Docker (local dev)
docker compose exec postgres psql -U scopeflow -d scopeflow_dev -c "EXPLAIN (ANALYZE, BUFFERS) ..."

# Update statistics before running baselines
ANALYZE proposals;
ANALYZE proposal_versions;
ANALYZE approval_workflows;
ANALYZE approvals;
```

---

## Critical Query Baselines

### Q1 — Find proposals by workspace and status

**Repository method:** `findByWorkspaceIdAndStatus(workspaceId, status)`

**Generated SQL:**
```sql
SELECT *
FROM proposals
WHERE workspace_id = '550e8400-e29b-41d4-a716-446655440000'
  AND status = 'DRAFT';
```

**Expected plan:**
```
Index Scan using idx_proposals_workspace_status on proposals
  Index Cond: ((workspace_id = '...') AND (status = 'DRAFT'))
  Rows Removed by Filter: 0
```

**Covering index:** `idx_proposals_workspace_status ON proposals(workspace_id, status)`

**Why it works:** Composite index with `workspace_id` as leading column — all queries are tenant-scoped, so `workspace_id` will always be present. `status` as second column eliminates the filter step.

**Red flag:** If plan shows `Seq Scan` or `Bitmap Heap Scan` with high rows-removed ratio, the index is not being used. Check `pg_stat_user_indexes` for `idx_scan = 0`.

---

### Q2 — Find all proposals by workspace (no status filter)

**Repository method:** `findByWorkspaceId(workspaceId)`

**Generated SQL:**
```sql
SELECT *
FROM proposals
WHERE workspace_id = '550e8400-e29b-41d4-a716-446655440000';
```

**Expected plan:**
```
Index Scan using idx_proposals_workspace_id on proposals
  Index Cond: (workspace_id = '...')
```

**Why it works:** Single-column index `idx_proposals_workspace_id`. The composite `idx_proposals_workspace_status` would also satisfy this query (prefix scan on workspace_id), but PostgreSQL may prefer the single-column index for full workspace scans.

---

### Q3 — Find proposals by client and workspace

**Repository method:** `findByClientIdAndWorkspaceId(clientId, workspaceId)`

**Generated SQL:**
```sql
SELECT *
FROM proposals
WHERE client_id = '...'
  AND workspace_id = '...';
```

**Expected plan:**
```
Bitmap Heap Scan on proposals
  Recheck Cond: (client_id = '...')
  Filter: (workspace_id = '...')
  -> Bitmap Index Scan on idx_proposals_client_id
```

**Optimization opportunity (post-MVP):** If this query is frequent, add composite index `(workspace_id, client_id)` to eliminate the Bitmap filter step. Not added in V4 to avoid over-indexing at MVP stage.

---

### Q4 — Find proposal versions ordered by date (newest first)

**Repository method:** `findByProposalIdOrderByCreatedAtDesc(proposalId)`

**Generated SQL:**
```sql
SELECT *
FROM proposal_versions
WHERE proposal_id = '...'
ORDER BY created_at DESC;
```

**Expected plan:**
```
Index Scan Backward using idx_proposal_versions_proposal_created_at on proposal_versions
  Index Cond: (proposal_id = '...')
```

**Key detail:** The index is defined as `(proposal_id, created_at DESC)`. PostgreSQL uses "Index Scan Backward" which is a forward scan on a DESC-ordered index — no sort step. If the index were `(proposal_id, created_at ASC)`, the plan would show `Sort` node with disk spill risk for large version histories.

**Red flag:** A `Sort` node in the plan means the index direction does not match ORDER BY. Verify the index DDL has `DESC`.

---

### Q5 — Find approval workflow by proposal

**Repository method:** `findByProposalId(proposalId)`

**Generated SQL:**
```sql
SELECT *
FROM approval_workflows
WHERE proposal_id = '...';
```

**Expected plan:**
```
Index Scan using uq_approval_workflows_proposal on approval_workflows
  Index Cond: (proposal_id = '...')
```

**Why it works:** The UNIQUE constraint on `proposal_id` creates an implicit B-Tree index. The query returns at most 1 row (guaranteed by the constraint), making this an ultra-fast point lookup.

---

### Q6 — Find approvals by workflow and approver email

**Repository method:** `findByWorkflowIdAndApproverEmail(workflowId, approverEmail)`

**Generated SQL:**
```sql
SELECT *
FROM approvals
WHERE workflow_id = '...'
  AND approver_email = 'client@example.com';
```

**Expected plan:**
```
Index Scan using uq_approvals_workflow_approver on approvals
  Index Cond: ((workflow_id = '...') AND (approver_email = 'client@example.com'))
```

**Why it works:** The UNIQUE constraint on `(workflow_id, approver_email)` exactly matches the query predicate. Returns at most 1 row.

---

### Q7 — Find all approvals for a workflow

**Repository method:** `findByWorkflowId(workflowId)`

**Generated SQL:**
```sql
SELECT *
FROM approvals
WHERE workflow_id = '...';
```

**Expected plan:**
```
Index Scan using idx_approvals_workflow_id on approvals
  Index Cond: (workflow_id = '...')
```

---

### Q8 — Find active briefing sessions by workspace

**Generated SQL:**
```sql
SELECT *
FROM briefing_sessions
WHERE workspace_id = '...'
  AND status = 'IN_PROGRESS';
```

**Expected plan:**
```
Index Scan using idx_briefing_sessions_workspace_id on briefing_sessions
  Index Cond: (workspace_id = '...')
  Filter: (status = 'IN_PROGRESS')
```

**Optimization opportunity (post-MVP):** Add composite `(workspace_id, status)` to eliminate the Filter step, mirroring the `idx_proposals_workspace_status` pattern.

---

### Q9 — Find users by email (login)

**Generated SQL:**
```sql
SELECT *
FROM users
WHERE email = 'user@example.com';
```

**Expected plan:**
```
Index Scan using users_email_key on users
  Index Cond: (email = 'user@example.com')
```

**Why it works:** `email` has a UNIQUE constraint — implicit index. Returns exactly 1 row. This is the hottest read path (every authenticated request touches it).

---

### Q10 — Find workspace members by workspace

**Generated SQL:**
```sql
SELECT *
FROM workspace_members
WHERE workspace_id = '...';
```

**Expected plan:**
```
Index Scan using idx_workspace_members_workspace_id on workspace_members
  Index Cond: (workspace_id = '...')
```

---

## Queries to Avoid (Anti-patterns)

### Anti-pattern 1: Unfiltered listing without workspace_id

```sql
-- Never do this in application code:
SELECT * FROM proposals WHERE status = 'DRAFT';
-- Hits idx_proposals_status (single-column), returns rows from ALL workspaces
-- Violates tenant isolation and will Seq Scan at scale
```

**Fix:** Always include `workspace_id` in WHERE clause.

### Anti-pattern 2: LIKE with leading wildcard

```sql
-- Forces Seq Scan — no index usable:
SELECT * FROM proposals WHERE proposal_name LIKE '%discovery%';
```

**Fix:** Use PostgreSQL full-text search (`to_tsvector` + GIN index) or move search to Elasticsearch for free-text queries.

### Anti-pattern 3: Fetching proposal + versions without pagination

```sql
-- Can return thousands of rows for old proposals:
SELECT * FROM proposal_versions WHERE proposal_id = '...';
```

**Fix:** Always paginate: `ORDER BY created_at DESC LIMIT 10 OFFSET 0`. The `idx_proposal_versions_proposal_created_at` index supports this efficiently.

---

## Diagnostic Queries

```sql
-- 1. Find tables with sequential scans (potential missing indexes)
SELECT relname, seq_scan, idx_scan,
       n_live_tup,
       round(seq_scan::numeric / NULLIF(seq_scan + idx_scan, 0) * 100, 1) AS seq_scan_pct
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_scan DESC;

-- 2. Find unused indexes (candidates for removal)
SELECT schemaname, tablename, indexname, idx_scan,
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelid NOT IN (
      SELECT indexrelid FROM pg_index WHERE indisprimary OR indisunique
  )
ORDER BY pg_relation_size(indexrelid) DESC;

-- 3. Find slow queries (requires pg_stat_statements extension)
SELECT query,
       calls,
       round(mean_exec_time::numeric, 2) AS mean_ms,
       round(total_exec_time::numeric, 2) AS total_ms,
       round(stddev_exec_time::numeric, 2) AS stddev_ms
FROM pg_stat_statements
WHERE mean_exec_time > 10  -- queries averaging > 10ms
ORDER BY mean_exec_time DESC
LIMIT 20;

-- 4. Table sizes (monitor growth)
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)
           - pg_relation_size(schemaname||'.'||tablename)) AS index_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 5. Find missing FK indexes
SELECT conrelid::regclass AS table_name,
       conname AS constraint_name,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE contype = 'f'
  AND NOT EXISTS (
      SELECT 1 FROM pg_index
      WHERE indrelid = conrelid
        AND indkey[0] = conkey[1]
  )
ORDER BY conrelid::regclass::text;
```

---

## Performance Budget (MVP targets)

| Query | Expected p50 | Expected p99 | Max acceptable |
|-------|-------------|-------------|----------------|
| Login (users by email) | < 1ms | < 5ms | 20ms |
| List workspace proposals | < 5ms | < 20ms | 100ms |
| Fetch proposal versions | < 5ms | < 20ms | 100ms |
| Find approval workflow | < 1ms | < 5ms | 20ms |
| Submit approval decision | < 10ms | < 50ms | 200ms |
| Briefing session by token | < 1ms | < 5ms | 20ms |

Baselines collected with: Testcontainers PostgreSQL 16-Alpine, 10k proposals per workspace, 3 versions per proposal, 1 workflow per proposal.
