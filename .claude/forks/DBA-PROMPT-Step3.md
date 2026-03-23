# DBA Delegation — Step 3: Briefing Domain Schema

**To:** dba (Claude Sonnet)
**From:** Marcus (Orchestrator)
**Date:** 2026-03-22
**Mode:** Fork isolated — design and implement Flyway migration
**Task ID:** TERMINAL2-STEP3-DBA
**Dependency:** ADR-002 + Backend-Dev implementation (Step 2)

---

## Mission

Design and implement the **Flyway V3 migration** for the Briefing bounded context. Create 4 core tables, 15+ indexes, Outbox pattern integration, and audit trails.

**Input:**
- ADR-002: `docs/architecture/adr/ADR-002-briefing-domain.md`
- Backend-Dev output: Domain entities + value objects (after Step 2)
- Reference: `backend/src/main/resources/db/migration/V2__user_workspace_domain_schema.sql` (Terminal 1 example)
- Project CLAUDE.md: `./CLAUDE.md` (database conventions)

**Output:**
- `backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql` (~600-800 lines)
- Documented in: `.claude/plans/DBA-OUTPUT-Step3-Briefing.md`

**Constraints:**
- PostgreSQL 16 syntax
- Flyway convention: `V{n}__{description}.sql`
- Never alter migrations already applied
- Indexes on all foreign keys + frequently queried fields
- Partial indexes for invariant enforcement
- Outbox table for event publishing

**Timeline:** ~1 day

---

## Core Tables to Create

### 1. briefing_sessions (parent aggregate)

```sql
CREATE TABLE briefing_sessions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,  -- FK to clients (different bounded context)
    service_type VARCHAR(50) NOT NULL,  -- social_media, landing_page, etc.
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    public_token VARCHAR(255) NOT NULL UNIQUE,
    completion_score INT CHECK (completion_score >= 0 AND completion_score <= 100),
    ai_analysis JSONB,  -- summary of gaps, recommendations
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Indexes:**
- `idx_briefing_sessions_workspace_id` (workspace scoping)
- `idx_briefing_sessions_status` (frequent query)
- `idx_briefing_sessions_client_id` (find active by client)
- `idx_briefing_sessions_service_type` (find by service)
- `idx_briefing_sessions_public_token` (client access via token)
- `idx_briefing_sessions_created_at` (timeline queries)
- Partial index: `idx_briefing_sessions_active WHERE status = 'IN_PROGRESS'` (single active per client/service)

### 2. briefing_questions (questions for service type)

```sql
CREATE TABLE briefing_questions (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    step INT NOT NULL,  -- order in sequence (1, 2, 3...)
    question_type VARCHAR(20) NOT NULL CHECK (question_type IN ('OPEN', 'MULTIPLE_CHOICE', 'YES_NO')),
    ai_prompt_version VARCHAR(50) DEFAULT 'v1',  -- for reproducibility
    required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Indexes:**
- `idx_briefing_questions_session_id` (find questions by session)
- `idx_briefing_questions_step` (sequential access)
- `idx_briefing_questions_type` (filter by type)

### 3. briefing_answers (client responses)

```sql
CREATE TABLE briefing_answers (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES briefing_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    answer_json JSONB,  -- structured response (if multiple choice, selected options)
    follow_up_generated BOOLEAN DEFAULT FALSE,
    ai_analysis JSONB,  -- quality score, confidence, detected gaps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- NO UPDATE OR DELETE ALLOWED (immutable audit trail)
);
```

**Indexes:**
- `idx_briefing_answers_session_id` (find answers by session)
- `idx_briefing_answers_question_id` (find answers to specific question)
- `idx_briefing_answers_follow_up_generated` (find questions with follow-ups)
- Partial index: `idx_briefing_answers_with_followup WHERE follow_up_generated = TRUE` (find auto-generated questions)

### 4. ai_generations (audit trail of IA outputs)

```sql
CREATE TABLE ai_generations (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    generation_type VARCHAR(50) NOT NULL CHECK (generation_type IN ('FOLLOW_UP_QUESTION', 'GAP_ANALYSIS', 'COMPLETION_SUMMARY')),
    input_json JSONB NOT NULL,  -- question + previous answers
    output_json JSONB NOT NULL,  -- AI response
    prompt_version VARCHAR(50) NOT NULL,  -- v1, v2, etc. for reproducibility
    latency_ms BIGINT,  -- performance tracking
    cost_usd DECIMAL(10, 6),  -- token cost from OpenAI
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Indexes:**
- `idx_ai_generations_session_id` (find IA outputs by session)
- `idx_ai_generations_type` (find by generation type)
- `idx_ai_generations_prompt_version` (find outputs by prompt version)
- `idx_ai_generations_created_at` (timeline queries)

---

## Constraints & Invariants (Database Level)

### Invariant 1: Single Active Briefing per Client per Service Type

```sql
-- Partial index (helps enforce at app level, but app must check)
CREATE UNIQUE INDEX idx_briefing_sessions_active_single
ON briefing_sessions(client_id, service_type)
WHERE status = 'IN_PROGRESS';
```

### Invariant 2: Immutable Answers (no updates/deletes)

```sql
-- Triggers to prevent answer modifications
CREATE OR REPLACE FUNCTION briefing_answers_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Briefing answers are immutable. Cannot %', TG_OP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER briefing_answers_immutable_trigger
BEFORE UPDATE OR DELETE ON briefing_answers
FOR EACH ROW
EXECUTE FUNCTION briefing_answers_immutable();
```

### Invariant 3: No Duplicate Answers to Same Question

```sql
CREATE UNIQUE INDEX idx_briefing_answers_unique_per_question
ON briefing_answers(briefing_session_id, question_id);
```

---

## Outbox Table (Event Publishing)

The Outbox table is **shared** across all bounded contexts for reliable async publishing:

```sql
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,  -- 'briefing', 'user', 'workspace'
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,  -- 'briefing.session.started', etc.
    event_payload JSONB NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for background worker to find unpublished events
CREATE INDEX idx_outbox_published_at ON outbox(published_at, created_at);
```

---

## Activity Log (Audit Trail)

Optional but recommended for compliance/LGPD:

```sql
CREATE TABLE briefing_activity_logs (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,  -- 'SESSION_STARTED', 'ANSWER_SUBMITTED', 'FOLLOWUP_GENERATED', 'COMPLETED', 'ABANDONED'
    entity_type VARCHAR(50),  -- 'BRIEFING_SESSION', 'BRIEFING_ANSWER', 'AI_GENERATION'
    entity_id UUID,
    changes JSONB,  -- what changed (if applicable)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_briefing_activity_logs_session_id ON briefing_activity_logs(briefing_session_id);
CREATE INDEX idx_briefing_activity_logs_action ON briefing_activity_logs(action);
```

---

## Deliverables Checklist

When done, Step 3 is complete if:

- [ ] File created: `backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql`
- [ ] All 4 core tables created (briefing_sessions, briefing_questions, briefing_answers, ai_generations)
- [ ] All 15+ indexes created (workspace_id, status, client_id, service_type, question_id, etc.)
- [ ] Partial indexes for active briefing + with_followup
- [ ] Immutability triggers on briefing_answers
- [ ] Unique constraint on (client_id, service_type, status='IN_PROGRESS')
- [ ] Outbox table created (or confirmed exists from Terminal 1)
- [ ] Activity log table created (optional)
- [ ] Migration tested locally: `./mvnw flyway:migrate`
- [ ] Migration compiles without SQL errors
- [ ] Committed: `feat(dba): v3-briefing-domain-schema`
- [ ] Output summary: `.claude/plans/DBA-OUTPUT-Step3-Briefing.md`

---

## Reference Materials

- **Terminal 1 Schema:** `backend/src/main/resources/db/migration/V2__user_workspace_domain_schema.sql`
- **ADR-002:** `docs/architecture/adr/ADR-002-briefing-domain.md` (table requirements)
- **PostgreSQL Docs:** https://www.postgresql.org/docs/16/
- **Flyway Convention:** Never alter applied migrations; new changes = new V{n} file

---

## Git Workflow

1. Branch: `feature/sprint-1b-briefing-domain` (already created)
2. Create: `backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql`
3. Test locally: `./mvnw flyway:migrate`
4. Verify tables created: `psql scopeflow_dev -c "\dt"` (show tables)
5. Commit: `feat(dba): v3-briefing-domain-schema`
6. Push to origin

---

## Timeline

**Start:** After backend-dev Step 2 ✅ (when Java files exist)
**Duration:** ~1 day
**Next:** API-Designer (Step 4)

---

**Ready. Design the Flyway migration for Briefing domain.** 🗄️
