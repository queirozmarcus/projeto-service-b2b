-- V3__briefing_domain_schema.sql
-- Briefing Domain Layer Schema
-- Date: 2026-03-22
-- Purpose: Create tables for Briefing bounded context (AI-assisted discovery flow)
-- Architect: Claude Sonnet (Agent)
-- Schema Design: DBA (Claude Sonnet)

-- ============================================================================
-- BRIEFING_SESSIONS TABLE (Aggregate Root)
-- ============================================================================
-- Parent aggregate: represents a discovery/briefing session with a client
-- States: IN_PROGRESS, COMPLETED, ABANDONED
-- Invariant: Only 1 active briefing per client per service type per workspace
CREATE TABLE briefing_sessions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    client_id UUID NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    public_token VARCHAR(255) NOT NULL UNIQUE,
    completion_score INT CHECK (completion_score IS NULL OR (completion_score >= 0 AND completion_score <= 100)),
    ai_analysis JSONB,
    abandoned_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_briefing_sessions_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT ck_briefing_sessions_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    CONSTRAINT ck_briefing_sessions_service_type CHECK (service_type IN ('SOCIAL_MEDIA', 'LANDING_PAGE', 'WEB_DESIGN', 'BRANDING', 'VIDEO_PRODUCTION', 'CONSULTING')),
    CONSTRAINT ck_briefing_sessions_completion_score_required_for_completed CHECK (
        (status = 'COMPLETED' AND completion_score IS NOT NULL AND completion_score >= 80) OR
        (status IN ('IN_PROGRESS', 'ABANDONED'))
    )
);

-- Indexes for query performance
CREATE INDEX idx_briefing_sessions_workspace_id ON briefing_sessions(workspace_id);
CREATE INDEX idx_briefing_sessions_status ON briefing_sessions(status);
CREATE INDEX idx_briefing_sessions_client_id ON briefing_sessions(client_id);
CREATE INDEX idx_briefing_sessions_service_type ON briefing_sessions(service_type);
CREATE INDEX idx_briefing_sessions_public_token ON briefing_sessions(public_token);
CREATE INDEX idx_briefing_sessions_created_at ON briefing_sessions(created_at);

-- Partial index: find active sessions efficiently
CREATE UNIQUE INDEX idx_briefing_sessions_active_single
ON briefing_sessions(workspace_id, client_id, service_type)
WHERE status = 'IN_PROGRESS';

-- Partial index: find recently completed briefings for scope generation
CREATE INDEX idx_briefing_sessions_completed_recent
ON briefing_sessions(workspace_id, updated_at)
WHERE status = 'COMPLETED';

-- Comments
COMMENT ON TABLE briefing_sessions IS 'Briefing session aggregate root. Represents discovery flow from start to completion or abandonment.';
COMMENT ON COLUMN briefing_sessions.workspace_id IS 'Workspace (tenant) owning this briefing.';
COMMENT ON COLUMN briefing_sessions.client_id IS 'Reference to client (in different bounded context). No FK for flexibility across domains.';
COMMENT ON COLUMN briefing_sessions.service_type IS 'Type of service: SOCIAL_MEDIA, LANDING_PAGE, etc.';
COMMENT ON COLUMN briefing_sessions.status IS 'Briefing state: IN_PROGRESS (active), COMPLETED (locked), ABANDONED (can restart).';
COMMENT ON COLUMN briefing_sessions.public_token IS 'Public token for client access link (no authentication required for client-facing approval).';
COMMENT ON COLUMN briefing_sessions.completion_score IS 'Final completion score (0-100). Only set when status=COMPLETED, must be >= 80.';
COMMENT ON COLUMN briefing_sessions.ai_analysis IS 'JSONB: AI summary of gaps, recommendations, confidence levels (populated at completion).';
COMMENT ON COLUMN briefing_sessions.abandoned_reason IS 'Optional reason for abandonment (user input or system-generated).';

-- ============================================================================
-- BRIEFING_QUESTIONS TABLE
-- ============================================================================
-- Represents questions in the discovery flow
-- Questions are sequential: step 1, 2, 3... (enforced at application layer)
CREATE TABLE briefing_questions (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL,
    question_text TEXT NOT NULL,
    step INT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    ai_prompt_version VARCHAR(50) DEFAULT 'v1',
    required BOOLEAN DEFAULT TRUE,
    follow_up_generated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_briefing_questions_session FOREIGN KEY (briefing_session_id) REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    CONSTRAINT ck_briefing_questions_type CHECK (question_type IN ('OPEN_ENDED', 'MULTIPLE_CHOICE', 'SCALE')),
    CONSTRAINT ck_briefing_questions_step CHECK (step > 0)
);

-- Indexes for query performance
CREATE INDEX idx_briefing_questions_session_id ON briefing_questions(briefing_session_id);
CREATE INDEX idx_briefing_questions_step ON briefing_questions(briefing_session_id, step);
CREATE INDEX idx_briefing_questions_type ON briefing_questions(question_type);
CREATE INDEX idx_briefing_questions_created_at ON briefing_questions(created_at);

-- Partial index: find auto-generated follow-up questions
CREATE INDEX idx_briefing_questions_followup_generated
ON briefing_questions(briefing_session_id)
WHERE follow_up_generated = TRUE;

-- Unique constraint: one question per step per session
CREATE UNIQUE INDEX idx_briefing_questions_unique_step
ON briefing_questions(briefing_session_id, step);

-- Comments
COMMENT ON TABLE briefing_questions IS 'Questions in the discovery flow. Immutable once created (except follow_up_generated flag).';
COMMENT ON COLUMN briefing_questions.question_text IS 'The question text (max 5000 chars, trimmed).';
COMMENT ON COLUMN briefing_questions.step IS 'Sequential order (1, 2, 3...). Application enforces no skip logic.';
COMMENT ON COLUMN briefing_questions.question_type IS 'OPEN_ENDED, MULTIPLE_CHOICE, SCALE.';
COMMENT ON COLUMN briefing_questions.ai_prompt_version IS 'Version of prompt that generated this question (for reproducibility).';
COMMENT ON COLUMN briefing_questions.required IS 'Whether question MUST be answered before completion.';
COMMENT ON COLUMN briefing_questions.follow_up_generated IS 'TRUE if this is an auto-generated follow-up (AI detected gap in previous answer).';

-- ============================================================================
-- BRIEFING_ANSWERS TABLE (Immutable Answers)
-- ============================================================================
-- Immutable audit trail of all answers submitted by client
-- No UPDATE or DELETE allowed (enforced by triggers)
CREATE TABLE briefing_answers (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL,
    question_id UUID NOT NULL,
    answer_text TEXT NOT NULL,
    answer_json JSONB,
    quality_score INT CHECK (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 100)),
    ai_analysis JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_briefing_answers_session FOREIGN KEY (briefing_session_id) REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_briefing_answers_question FOREIGN KEY (question_id) REFERENCES briefing_questions(id) ON DELETE CASCADE
);

-- Indexes for query performance
CREATE INDEX idx_briefing_answers_session_id ON briefing_answers(briefing_session_id);
CREATE INDEX idx_briefing_answers_question_id ON briefing_answers(question_id);
CREATE INDEX idx_briefing_answers_created_at ON briefing_answers(created_at);

-- Unique constraint: max 1 answer per question per session
CREATE UNIQUE INDEX idx_briefing_answers_unique_per_question
ON briefing_answers(briefing_session_id, question_id);

-- Comments
COMMENT ON TABLE briefing_answers IS 'Immutable audit trail. Answers cannot be updated or deleted (enforced by database trigger).';
COMMENT ON COLUMN briefing_answers.answer_text IS 'Plain text answer (max 5000 chars).';
COMMENT ON COLUMN briefing_answers.answer_json IS 'Structured JSON (for multiple choice selections, arrays, etc.).';
COMMENT ON COLUMN briefing_answers.quality_score IS 'AI-computed quality score (0-100): how complete/valuable this answer is.';
COMMENT ON COLUMN briefing_answers.ai_analysis IS 'JSONB: AI insights (confidence, gaps detected, suggested follow-up topic).';

-- Trigger: Prevent any modifications to briefing_answers
CREATE OR REPLACE FUNCTION briefing_answers_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Briefing answers are immutable. Cannot % answer (id=%)', TG_OP, OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER briefing_answers_immutable_trigger
BEFORE UPDATE OR DELETE ON briefing_answers
FOR EACH ROW
EXECUTE FUNCTION briefing_answers_immutable();

-- ============================================================================
-- AI_GENERATIONS TABLE (Audit Trail of IA Outputs)
-- ============================================================================
-- Immutable record of all IA/LLM calls: prompts, responses, costs, latency
-- Used for debugging, cost tracking, prompt versioning
CREATE TABLE ai_generations (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL,
    generation_type VARCHAR(50) NOT NULL,
    input_json JSONB NOT NULL,
    output_json JSONB NOT NULL,
    prompt_version VARCHAR(50) NOT NULL DEFAULT 'v1',
    latency_ms BIGINT CHECK (latency_ms >= 0),
    cost_usd NUMERIC(10, 6) CHECK (cost_usd >= 0),
    model_used VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_generations_session FOREIGN KEY (briefing_session_id) REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_generations_type CHECK (generation_type IN ('FOLLOW_UP_QUESTION', 'GAP_ANALYSIS', 'COMPLETION_SUMMARY'))
);

-- Indexes for query performance
CREATE INDEX idx_ai_generations_session_id ON ai_generations(briefing_session_id);
CREATE INDEX idx_ai_generations_type ON ai_generations(generation_type);
CREATE INDEX idx_ai_generations_prompt_version ON ai_generations(prompt_version);
CREATE INDEX idx_ai_generations_created_at ON ai_generations(created_at);
CREATE INDEX idx_ai_generations_model_used ON ai_generations(model_used);

-- Comments
COMMENT ON TABLE ai_generations IS 'Audit trail of all LLM calls. Immutable for cost tracking and reproducibility.';
COMMENT ON COLUMN ai_generations.generation_type IS 'FOLLOW_UP_QUESTION, GAP_ANALYSIS, or COMPLETION_SUMMARY.';
COMMENT ON COLUMN ai_generations.input_json IS 'Input to LLM: questions, answers, context, client profile.';
COMMENT ON COLUMN ai_generations.output_json IS 'LLM response: questions, analysis, recommendations.';
COMMENT ON COLUMN ai_generations.prompt_version IS 'Version of prompt used (v1, v2, etc.) for A/B testing and reproducibility.';
COMMENT ON COLUMN ai_generations.latency_ms IS 'Round-trip time to LLM (milliseconds).';
COMMENT ON COLUMN ai_generations.cost_usd IS 'Token cost in USD (for cost tracking and budgeting).';
COMMENT ON COLUMN ai_generations.model_used IS 'Model identifier (gpt-4, claude-opus, etc.).';

-- ============================================================================
-- BRIEFING_ACTIVITY_LOGS TABLE (Audit Trail for Compliance)
-- ============================================================================
-- Immutable log of all significant actions in briefing lifecycle
-- Used for audit trails, LGPD compliance, debugging
CREATE TABLE briefing_activity_logs (
    id UUID PRIMARY KEY,
    briefing_session_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_briefing_activity_logs_session FOREIGN KEY (briefing_session_id) REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    CONSTRAINT ck_briefing_activity_logs_action CHECK (action IN (
        'SESSION_STARTED',
        'SESSION_RESUMED',
        'QUESTION_ASKED',
        'ANSWER_SUBMITTED',
        'FOLLOWUP_GENERATED',
        'COMPLETION_REQUESTED',
        'SESSION_COMPLETED',
        'SESSION_ABANDONED',
        'SESSION_RESTARTED',
        'PUBLIC_LINK_SHARED',
        'PUBLIC_LINK_VIEWED'
    ))
);

-- Indexes for audit queries
CREATE INDEX idx_briefing_activity_logs_session_id ON briefing_activity_logs(briefing_session_id);
CREATE INDEX idx_briefing_activity_logs_action ON briefing_activity_logs(action);
CREATE INDEX idx_briefing_activity_logs_entity ON briefing_activity_logs(entity_type, entity_id);
CREATE INDEX idx_briefing_activity_logs_created_at ON briefing_activity_logs(created_at);

-- Comments
COMMENT ON TABLE briefing_activity_logs IS 'Immutable audit log of all briefing events. Insert-only for compliance and debugging.';
COMMENT ON COLUMN briefing_activity_logs.action IS 'Type of action: SESSION_STARTED, ANSWER_SUBMITTED, SESSION_COMPLETED, etc.';
COMMENT ON COLUMN briefing_activity_logs.entity_type IS 'Type of entity affected (BRIEFING_SESSION, BRIEFING_ANSWER, AI_GENERATION).';
COMMENT ON COLUMN briefing_activity_logs.entity_id IS 'ID of affected entity (session_id, answer_id, etc.).';
COMMENT ON COLUMN briefing_activity_logs.details IS 'JSONB: context-specific details (who, where, why).';

-- ============================================================================
-- ENSURE OUTBOX TABLE EXISTS (Shared across all domains)
-- ============================================================================
-- Already created in V2, but we reference it here for safety
-- This is the transactional outbox for event publishing to Kafka
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

-- Index for background worker (find unpublished events)
CREATE INDEX IF NOT EXISTS idx_outbox_published_at ON outbox(published_at) WHERE published_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_outbox_event_type ON outbox(event_type);

-- ============================================================================
-- CONSTRAINTS & INVARIANTS (Database Level)
-- ============================================================================

-- Invariant 1: No duplicate questions in same session at same step (enforced by unique index)
-- Already created: idx_briefing_questions_unique_step

-- Invariant 2: No duplicate answers to same question (enforced by unique index)
-- Already created: idx_briefing_answers_unique_per_question

-- Invariant 3: Single active briefing per client per service type (enforced by partial unique index)
-- Already created: idx_briefing_sessions_active_single

-- Invariant 4: Immutable answers (enforced by trigger)
-- Already created: briefing_answers_immutable_trigger

-- Invariant 5: Completion score >= 80 when status=COMPLETED (enforced by CHECK constraint)
-- Already created: ck_briefing_sessions_completion_score_required_for_completed

-- ============================================================================
-- VIEWS FOR COMMON QUERIES
-- ============================================================================

-- View: Active briefing sessions (for dashboard, analytics)
CREATE VIEW v_briefing_sessions_active AS
SELECT
    bs.id,
    bs.workspace_id,
    bs.client_id,
    bs.service_type,
    bs.status,
    bs.public_token,
    COUNT(bq.id) as total_questions,
    COUNT(ba.id) as answered_questions,
    CASE WHEN COUNT(bq.id) > 0 THEN (COUNT(ba.id) * 100 / COUNT(bq.id)) ELSE 0 END as progress_percentage,
    bs.created_at,
    bs.updated_at
FROM briefing_sessions bs
LEFT JOIN briefing_questions bq ON bs.id = bq.briefing_session_id
LEFT JOIN briefing_answers ba ON bq.id = ba.question_id
WHERE bs.status = 'IN_PROGRESS'
GROUP BY bs.id, bs.workspace_id, bs.client_id, bs.service_type, bs.status, bs.public_token, bs.created_at, bs.updated_at;

COMMENT ON VIEW v_briefing_sessions_active IS 'Active briefing sessions with progress metrics. Used for dashboard and monitoring.';

-- View: Completed briefings (for scope generation)
CREATE VIEW v_briefing_sessions_completed AS
SELECT
    bs.id,
    bs.workspace_id,
    bs.client_id,
    bs.service_type,
    bs.completion_score,
    bs.ai_analysis,
    COUNT(ba.id) as total_answers,
    COUNT(CASE WHEN ba.quality_score >= 75 THEN 1 END) as high_quality_answers,
    bs.created_at,
    bs.updated_at
FROM briefing_sessions bs
LEFT JOIN briefing_answers ba ON bs.id = ba.briefing_session_id
WHERE bs.status = 'COMPLETED'
GROUP BY bs.id, bs.workspace_id, bs.client_id, bs.service_type, bs.completion_score, bs.ai_analysis, bs.created_at, bs.updated_at;

COMMENT ON VIEW v_briefing_sessions_completed IS 'Completed briefings ready for scope generation. Includes quality metrics.';

-- View: AI generation cost tracking (for cost optimization)
CREATE VIEW v_ai_generation_costs AS
SELECT
    DATE_TRUNC('day', ag.created_at)::DATE as generation_date,
    ag.generation_type,
    ag.model_used,
    COUNT(*) as call_count,
    SUM(ag.latency_ms) as total_latency_ms,
    AVG(ag.latency_ms) as avg_latency_ms,
    SUM(ag.cost_usd) as total_cost_usd,
    AVG(ag.cost_usd) as avg_cost_usd
FROM ai_generations ag
GROUP BY generation_date, ag.generation_type, ag.model_used
ORDER BY generation_date DESC;

COMMENT ON VIEW v_ai_generation_costs IS 'Daily AI generation costs and performance metrics. Used for cost optimization and monitoring.';

-- ============================================================================
-- FUNCTIONS & PROCEDURES
-- ============================================================================

-- Function: Update updated_at timestamp (reusable for all tables)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Auto-update updated_at on briefing_sessions
CREATE TRIGGER briefing_sessions_update_updated_at
BEFORE UPDATE ON briefing_sessions
FOR EACH ROW
WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION update_updated_at_column();

-- Function: Calculate briefing progress percentage (helper)
CREATE OR REPLACE FUNCTION get_briefing_progress(p_session_id UUID)
RETURNS INT AS $$
DECLARE
    v_total_questions INT;
    v_answered_questions INT;
    v_progress INT;
BEGIN
    SELECT COUNT(*) INTO v_total_questions
    FROM briefing_questions
    WHERE briefing_session_id = p_session_id;

    SELECT COUNT(*) INTO v_answered_questions
    FROM briefing_answers
    WHERE briefing_session_id = p_session_id;

    IF v_total_questions = 0 THEN
        v_progress := 0;
    ELSE
        v_progress := (v_answered_questions * 100) / v_total_questions;
    END IF;

    RETURN v_progress;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION get_briefing_progress(UUID) IS 'Calculate briefing completion progress as percentage (0-100).';

-- ============================================================================
-- SECURITY & GRANTS
-- ============================================================================
-- Default: assume application connects as app user (not superuser)
-- Uncomment and adjust per your security model

-- GRANT SELECT, INSERT, UPDATE ON briefing_sessions TO app_user;
-- GRANT SELECT, INSERT ON briefing_questions TO app_user;
-- GRANT SELECT, INSERT ON briefing_answers TO app_user;
-- GRANT SELECT, INSERT ON ai_generations TO app_user;
-- GRANT SELECT, INSERT ON briefing_activity_logs TO app_user;
-- GRANT SELECT, INSERT, UPDATE ON outbox TO app_user;

-- ============================================================================
-- END OF V3 MIGRATION
-- ============================================================================
