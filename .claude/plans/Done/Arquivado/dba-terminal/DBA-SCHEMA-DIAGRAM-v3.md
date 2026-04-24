# Schema Diagram — V3 Briefing Domain

**Generated:** 2026-03-22
**Database:** PostgreSQL 16
**Flyway Version:** 9.22+

---

## Entity-Relationship Diagram (Conceptual)

```
┌─────────────────────────────────────────────────────────────────────┐
│                      BRIEFING DOMAIN (V3)                          │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ workspaces (FK from V2)                                      │  │
│  │ ├─ id (PK)                                                   │  │
│  │ ├─ name, niche, status                                       │  │
│  │ └─ (tenant scoping for all tables)                           │  │
│  └──────────┬───────────────────────────────────────────────────┘  │
│             │                                                       │
│             │ (1) workspace_id (FK)                                │
│             │                                                       │
│  ┌──────────▼───────────────────────────────────────────────────┐  │
│  │ briefing_sessions (AGGREGATE ROOT)                           │  │
│  │                                                               │  │
│  │ ├─ id (PK) — UUID                                            │  │
│  │ ├─ workspace_id (FK) — tenant scoping                        │  │
│  │ ├─ client_id — reference (no FK to other context)            │  │
│  │ ├─ service_type — SOCIAL_MEDIA, LANDING_PAGE, etc.         │  │
│  │ ├─ status — IN_PROGRESS | COMPLETED | ABANDONED             │  │
│  │ ├─ public_token — UNIQUE, client access link                │  │
│  │ ├─ completion_score — 0-100, set only when COMPLETED        │  │
│  │ ├─ ai_analysis — JSONB (summary at completion)              │  │
│  │ ├─ abandoned_reason — VARCHAR(500)                          │  │
│  │ └─ created_at, updated_at — TIMESTAMPTZ                     │  │
│  │                                                               │  │
│  │ INVARIANTS:                                                   │  │
│  │ • Partial Unique: (workspace_id, client_id, service_type)    │  │
│  │   WHERE status = 'IN_PROGRESS'                               │  │
│  │ • completion_score ≥ 80 only when status = COMPLETED         │  │
│  │                                                               │  │
│  │ INDEXES (8):                                                  │  │
│  │ ├─ workspace_id, status, client_id, service_type             │  │
│  │ ├─ public_token (UNIQUE)                                      │  │
│  │ ├─ created_at (timeline)                                      │  │
│  │ └─ active_single (PARTIAL UNIQUE), completed_recent          │  │
│  └──────────┬──────────────────┬──────────────────────────────┬──┘  │
│             │                  │                              │     │
│      (1)    │ (1..N)           │ (1..N)                  (1..N)│    │
│             │                  │                              │     │
│  ┌──────────▼──────────────┐  ┌─▼─────────────────────┐  ┌──▼────┐│
│  │ briefing_questions      │  │ briefing_answers      │  │ai_gens││
│  │                         │  │                       │  │(see ──┘│
│  │ ├─ id (PK) — UUID       │  │ ├─ id (PK) — UUID    │  │below)  │
│  │ ├─ session_id (FK)      │  │ ├─ session_id (FK)   │  └────────┘
│  │ ├─ question_text        │  │ ├─ question_id (FK)  │
│  │ ├─ step — 1, 2, 3...    │  │ ├─ answer_text       │
│  │ ├─ question_type        │  │ ├─ answer_json       │
│  │ ├─ ai_prompt_version    │  │ ├─ quality_score     │
│  │ ├─ required — BOOLEAN   │  │ ├─ ai_analysis       │
│  │ └─ follow_up_generated  │  │ └─ created_at        │
│  │                         │  │                       │
│  │ INVARIANT:              │  │ INVARIANT: IMMUTABLE  │
│  │ • Unique (session, step)│  │ • Trigger prevents    │
│  │ • Unique question/step  │  │   UPDATE/DELETE       │
│  │                         │  │                       │
│  │ INDEXES (6):            │  │ INDEXES (4):          │
│  │ ├─ session_id           │  │ ├─ session_id         │
│  │ ├─ (session, step)      │  │ ├─ question_id        │
│  │ ├─ type, created_at     │  │ ├─ created_at         │
│  │ └─ followup_generated   │  │ └─ unique_per_q       │
│  │   (PARTIAL)             │  │   (UNIQUE)            │
│  └─────────────────────────┘  └───────────────────────┘
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ ai_generations (Audit Trail)                         │  │
│  │                                                        │  │
│  │ ├─ id (PK) — UUID                                    │  │
│  │ ├─ session_id (FK) — briefing_sessions               │  │
│  │ ├─ generation_type — FOLLOW_UP_QUESTION, GAP_ANALYSIS │  │
│  │ ├─ input_json — JSONB (LLM input)                   │  │
│  │ ├─ output_json — JSONB (LLM response)               │  │
│  │ ├─ prompt_version — v1, v2, etc. (reproducibility) │  │
│  │ ├─ latency_ms — BIGINT (performance tracking)       │  │
│  │ ├─ cost_usd — NUMERIC(10,6) (cost optimization)    │  │
│  │ ├─ model_used — gpt-4, claude-opus, etc.           │  │
│  │ └─ created_at — TIMESTAMPTZ                         │  │
│  │                                                        │  │
│  │ INDEXES (5):                                          │  │
│  │ ├─ session_id, type, prompt_version                 │  │
│  │ ├─ created_at, model_used                           │  │
│  │ └─ (for cost tracking & reproducibility)             │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ briefing_activity_logs (Compliance Audit Trail)      │  │
│  │                                                        │  │
│  │ ├─ id (PK) — UUID                                    │  │
│  │ ├─ session_id (FK) — briefing_sessions               │  │
│  │ ├─ action — SESSION_STARTED, ANSWER_SUBMITTED, etc. │  │
│  │ ├─ entity_type — BRIEFING_SESSION, ANSWER, etc.    │  │
│  │ ├─ entity_id — UUID of entity affected              │  │
│  │ ├─ details — JSONB (context)                        │  │
│  │ └─ created_at — TIMESTAMPTZ (insert-only)           │  │
│  │                                                        │  │
│  │ INDEXES (4):                                          │  │
│  │ ├─ session_id, action                               │  │
│  │ ├─ (entity_type, entity_id)                         │  │
│  │ └─ created_at (timeline)                            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ outbox (Shared Transactional Outbox — from V2)      │  │
│  │                                                        │  │
│  │ ├─ id (PK) — UUID                                    │  │
│  │ ├─ aggregate_type — 'briefing', 'user', 'workspace' │  │
│  │ ├─ aggregate_id — UUID (session_id, etc.)           │  │
│  │ ├─ event_type — 'briefing.session.started', etc.   │  │
│  │ ├─ event_payload — JSONB (event data)               │  │
│  │ ├─ created_at — TIMESTAMPTZ                         │  │
│  │ └─ published_at — TIMESTAMPTZ (when published)      │  │
│  │                                                        │  │
│  │ EVENTS PUBLISHED BY BRIEFING DOMAIN:                │  │
│  │ ├─ briefing.session.started — → Kafka               │  │
│  │ ├─ briefing.session.completed — → proposal service  │  │
│  │ ├─ briefing.session.abandoned                       │  │
│  │ ├─ briefing.question.asked                          │  │
│  │ └─ briefing.answer.submitted                        │  │
│  │                                                        │  │
│  │ INDEXES (2):                                          │  │
│  │ ├─ published_at (PARTIAL, for worker)               │  │
│  │ └─ event_type (by type queries)                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
└─────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────┐
                    │ VIEWS (3)               │
                    ├─────────────────────────┤
                    │ v_briefing_sessions_    │
                    │   active                │
                    │ v_briefing_sessions_    │
                    │   completed             │
                    │ v_ai_generation_costs   │
                    └─────────────────────────┘
```

---

## Table Statistics

| Table | Purpose | Rows/Year (Est.) | Primary Queries | Index Count |
|-------|---------|------------------|-----------------|-------------|
| `briefing_sessions` | Aggregate root | ~10K | By status, workspace, client | 8 |
| `briefing_questions` | Questions | ~100K | By session, step | 6 |
| `briefing_answers` | Immutable answers | ~100K | By session, question | 4 |
| `ai_generations` | LLM audit trail | ~50K | By session, type, model | 5 |
| `briefing_activity_logs` | Compliance log | ~200K | By session, action | 4 |
| **TOTAL** | — | ~460K | — | **27** |

---

## Constraint Matrix

| Table | Primary Key | Unique Constraints | Foreign Keys | Check Constraints |
|-------|-------------|------------------|--------------|------------------|
| `briefing_sessions` | `id` | `public_token`, active_single (partial) | workspace (CASCADE) | status IN (...), service_type IN (...), completion_score 0-100 |
| `briefing_questions` | `id` | (session_id, step) | session (CASCADE) | step > 0, question_type IN (...) |
| `briefing_answers` | `id` | (session_id, question_id) | session, question (CASCADE) | quality_score 0-100, immutable trigger |
| `ai_generations` | `id` | — | session (CASCADE) | generation_type IN (...), latency_ms >= 0, cost_usd >= 0 |
| `briefing_activity_logs` | `id` | — | session (CASCADE) | action IN (...) |

---

## Index Strategy Matrix

| Purpose | Technique | Examples |
|---------|-----------|----------|
| **Tenant scoping (multi-tenancy)** | First column always | `(workspace_id, status)`, `(workspace_id, client_id)` |
| **Invariant enforcement** | UNIQUE INDEX | `(session_id, step)`, `(session_id, question_id)`, active_single |
| **Partial indexes** | `WHERE` condition | `WHERE status = 'IN_PROGRESS'`, `WHERE follow_up_generated = TRUE` |
| **Composite queries** | Composite index | `(session_id, step)` for sequential access |
| **Status queries** | Single column | `status`, `action` for filtering |
| **Timeline queries** | Date column | `created_at`, `updated_at` for sorting |

---

## Domain Event Flow

```
┌─────────────────────────────────────────────────────────┐
│                  BRIEFING DOMAIN EVENTS                 │
└─────────────────────────────────────────────────────────┘

1. Session Lifecycle
   ┌─ START ──→ briefing.session.started
   │            (INSERT session, INSERT questions)
   │
   ├─ QUESTION_ASKED ──→ (informational)
   │
   ├─ ANSWER_SUBMITTED ──→ briefing.answer.submitted
   │  (INSERT answer, INSERT ai_generation if analyzed)
   │
   ├─ FOLLOWUP_GENERATED ──→ (if gap detected by AI)
   │  (INSERT question with follow_up_generated=true)
   │
   ├─ COMPLETED ──→ briefing.completed [EVENT PUBLISHED TO KAFKA]
   │  (UPDATE status=COMPLETED, score=80+)
   │  └─→ Proposal service: CREATE ProposalDraft
   │
   └─ ABANDONED ──→ briefing.abandoned (optional event)
      (UPDATE status=ABANDONED, reason=...)

2. Outbox Pattern (Reliable Publishing)
   ┌─ Domain Service publishes event
   ├─ INSERT INTO outbox (aggregate_type, aggregate_id, event_type, payload)
   ├─ Background Worker reads unpublished events
   │  SELECT * FROM outbox WHERE published_at IS NULL
   ├─ Publish to Kafka: briefing.events.v1 topic
   └─ UPDATE outbox.published_at = NOW()
      (worker marks as published)

3. Cross-Bounded-Context Communication
   Briefing → Outbox → Kafka → Proposal Context

   "briefing.completed" Event triggers:
   ├─ proposal-service: CREATE ProposalDraft
   ├─ user-service: NOTIFY user
   ├─ notification-service: SEND email/SMS to client
   └─ analytics-service: INCREMENT completion metric
```

---

## Performance Profile

### Read-Heavy Queries (Optimized)

| Query | Estimated Time | Index Used |
|-------|-----------------|-----------|
| Get next question for client | **1-5ms** | `idx_briefing_questions_session_id` + `idx_briefing_questions_step` |
| Check active briefing (invariant) | **<1ms** | `idx_briefing_sessions_active_single` (PARTIAL UNIQUE) |
| Get all answers for session | **10-50ms** | `idx_briefing_answers_session_id` |
| Daily cost report | **100-500ms** | View `v_ai_generation_costs` with aggregation |
| Find completed sessions | **5-20ms** | `idx_briefing_sessions_completed_recent` (PARTIAL) |

### Write-Heavy Operations (INSERT only, immutable)

| Operation | Estimated Time | Safety |
|-----------|-----------------|--------|
| Insert answer | **2-10ms** | Trigger enforces immutability |
| Insert question | **1-5ms** | Unique index prevents duplicates |
| Insert AI generation record | **2-10ms** | Indexes created, no lock contention |
| Publish to outbox | **1-5ms** | Partial index on published_at |

### Scaling Notes

- **No DELETE operations:** Audit trail never deleted (compliance)
- **No UPDATE on answers:** Immutable by trigger (safe concurrency)
- **Partial indexes:** Only index active/relevant data (reduced bloat)
- **Partitioning:** Not needed for MVP (~500K rows/year)

---

## Compliance & Audit

### LGPD/Privacy Compliance

1. **Data Collection:**
   - Briefing answers store client input (necessary for service)
   - Activity log captures all significant actions

2. **Data Retention:**
   - Recommend 2-year retention, then archive to cold storage
   - Briefing_activity_logs used for audit trail (never delete)

3. **Data Export/Deletion:**
   - Query all rows for workspace_id → export as JSON
   - Implement soft-delete + archive process for compliance

### Audit Trail

- **Activity logs:** 100% of briefing events logged
- **Answer immutability:** TRIGGER prevents modification
- **AI tracking:** Every LLM call recorded with prompt version
- **Cost tracking:** ai_generations table captures costs for billing/optimization

---

## Migration Safety Checklist

✅ **File naming:** V3__briefing_domain_schema.sql (Flyway convention)
✅ **Idempotent:** Safe to run multiple times (no duplicates)
✅ **No forward references:** All FKs reference V1/V2 tables
✅ **Reversible:** DROP TABLE IF EXISTS (no data destruction)
✅ **Zero-downtime:** No locks > 1 second
✅ **Documented:** Comments on all tables, columns, indexes
✅ **Validated:** SQL syntax correct PostgreSQL 16
✅ **Aligned:** Matches domain design (ADR-002)

---

## Testing Strategy

### Unit Tests (in memory)
- Domain classes already tested (Step 2)
- No database queries required

### Integration Tests (Testcontainers)
- Test query performance on indexed columns
- Verify unique constraints work
- Verify immutability trigger blocks UPDATE/DELETE
- Test partial indexes return correct result sets

### E2E Tests (REST API)
- Full briefing flow: start → answer → complete
- Verify events published to outbox
- Verify activity logs created

---

**End of Schema Diagram**
