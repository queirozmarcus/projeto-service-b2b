-- V9__add_missing_indexes.sql
-- ScopeFlow AI — Auditoria de Índices (Tasks 26/28)
-- Date: 2026-04-04
--
-- CONTEXTO:
--   Auditoria das migrations V1-V8 identificou FKs sem índice e hot paths
--   sem cobertura adequada. Esta migration adiciona os índices críticos usando
--   CONCURRENTLY (zero downtime) e IF NOT EXISTS (idempotente).
--
-- IMPACTO ESPERADO:
--   - Elimina Seq Scans em listagens por workspace + client_id
--   - Cobre ORDER BY updated_at DESC nos filtros paginados de proposals
--   - Cobre queries de lookup de proposals por briefing_id (join briefing → proposal)
--   - Cobre lookup de public_token com índice UNIQUE real (não apenas constraint)
--   - Cobre FK de outbox_event.aggregate_id sem índice individual
--   - Reduz custo de queries de approval por workflow e status combinados

-- ============================================================================
-- 1. briefing_sessions — composite workspace + client_id
-- ============================================================================
-- Problema: idx_briefing_sessions_workspace_id e idx_briefing_sessions_client_id
-- existem separados, mas a query findByWorkspaceAndStatus usa workspace_id + status.
-- Um índice composto (workspace_id, client_id) cobre buscas de sessões ativas de um
-- cliente dentro de um workspace (painel do operador).
-- Impacto: listagens de sessões por cliente dentro de workspace passam de Seq Scan
-- para Index Scan em tabelas grandes.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_briefing_sessions_workspace_client
    ON briefing_sessions(workspace_id, client_id);

-- Composite workspace_id + status + created_at para suportar ORDER BY na query
-- findByWorkspaceAndStatus: ORDER BY created_at DESC.
-- O índice existente idx_briefing_sessions_workspace_id não cobre o ORDER BY,
-- forçando sort em memória para workspaces com muitas sessões.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_briefing_sessions_workspace_status_created
    ON briefing_sessions(workspace_id, status, created_at DESC);

-- ============================================================================
-- 2. briefing_sessions — public_token como UNIQUE INDEX explícito
-- ============================================================================
-- Problema: existe UNIQUE constraint em public_token (garante unicidade), mas
-- não existe índice hash separado para lookup por token. O índice criado pela
-- constraint é B-tree padrão e já serve, mas o nome idx_briefing_sessions_public_token
-- criado em V3 não é UNIQUE. A constraint uk_briefing_sessions_public_token é implícita.
-- Garantindo que o índice de lookup seja UNIQUE para que o planner saiba
-- que é uma busca de no máximo 1 linha (impacta cardinality estimate).
-- NOTA: O índice abaixo pode conflitar com o existente se já for unique.
-- O IF NOT EXISTS protege a idempotência.
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS ix_briefing_sessions_public_token_unique
    ON briefing_sessions(public_token);

-- ============================================================================
-- 3. proposals — composite workspace_id + client_id
-- ============================================================================
-- Problema: findByClientIdAndWorkspaceId faz Seq Scan usando dois índices separados
-- (bitmap AND). Um índice composto é mais eficiente para este pattern.
-- Hot path: listagem de proposals de um cliente específico em um workspace.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_proposals_workspace_client
    ON proposals(workspace_id, client_id)
    WHERE deleted_at IS NULL;

-- Composite workspace_id + status + updated_at DESC para cobrir o padrão
-- findByWorkspaceIdAndStatus com paginação ordenada por updated_at.
-- O índice idx_proposals_workspace_status (V4) existe mas não inclui updated_at,
-- causando sort stage adicional em queries paginadas.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_proposals_workspace_status_updated
    ON proposals(workspace_id, status, updated_at DESC)
    WHERE deleted_at IS NULL;

-- ============================================================================
-- 4. proposals — briefing_id lookup (FK sem cobertura de ORDER BY)
-- ============================================================================
-- Problema: idx_proposals_briefing_id (V4) cobre a FK, mas queries que fazem
-- JOIN briefing_sessions → proposals precisam de suporte a filtros adicionais.
-- O índice existente já é adequado para lookup simples; mantido para documentação.
-- Nenhum índice adicional necessário aqui — registrado para clareza.

-- ============================================================================
-- 5. proposal_versions — composite proposal_id + created_at DESC
-- ============================================================================
-- Problema: O índice idx_proposal_versions_proposal_id (V4) existe mas
-- idx_proposal_versions_proposal_created_at foi criado com (proposal_id, created_at DESC).
-- A entity JpaProposalVersion define apenas idx_proposal_versions_created_at
-- (sem proposal_id como prefixo), o que força um Index Scan + Sort para buscar
-- a versão mais recente de uma proposal.
-- O índice idx_proposal_versions_proposal_created_at da migration V4 já cobre isso;
-- garantindo que a entity reflita o mesmo índice composto correto.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_proposal_versions_proposal_created_desc
    ON proposal_versions(proposal_id, created_at DESC);

-- ============================================================================
-- 6. approval_workflows — composite status + initiated_at para background jobs
-- ============================================================================
-- Problema: O índice parcial idx_approval_workflows_open (V4) indexa apenas
-- initiated_at WHERE status IN ('PENDING', 'IN_PROGRESS'). Para o job de lembretes
-- que ordena por iniciação mais antiga, este índice já é adequado. Nenhum adicional.
-- Para dashboards que listam workflows por status, cobrindo status + completed_at:
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_approval_workflows_status_completed
    ON approval_workflows(status, completed_at DESC);

-- ============================================================================
-- 7. approvals — composite workflow_id + status (hot path de resolução)
-- ============================================================================
-- Problema: idx_approvals_workflow_id e idx_approvals_status existem separados (V4).
-- A query que verifica se todos os approvers de um workflow decidiram
-- (SELECT * FROM approvals WHERE workflow_id = ? AND status = 'PENDING')
-- usa bitmap AND entre dois índices. Um índice composto elimina isso.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_approvals_workflow_status
    ON approvals(workflow_id, status);

-- ============================================================================
-- 8. outbox_event — aggregate_id individual (FK sem índice isolado)
-- ============================================================================
-- Problema: idx_outbox_event_aggregate (V5) indexa (aggregate_type, aggregate_id).
-- Queries de replay/debug que filtram apenas por aggregate_id (sem type) fazem
-- Seq Scan. Um índice em aggregate_id isolado cobre esse caso.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_outbox_event_aggregate_id
    ON outbox_event(aggregate_id);

-- ============================================================================
-- 9. service_context_profiles — workspace_id + is_active (sem partial geral)
-- ============================================================================
-- Já coberto pelo partial unique index idx_service_context_profiles_unique_active (V8).
-- O índice idx_service_context_profiles_workspace_active (V8) também existe.
-- Nenhum índice adicional necessário aqui.

-- ============================================================================
-- 10. activity_logs — composite workspace_id + created_at DESC para audit queries
-- ============================================================================
-- Problema: idx_activity_logs_workspace_id e idx_activity_logs_created_at existem
-- separados (V2). Queries de auditoria filtram por workspace e ordenam por data;
-- o planner usa bitmap AND ou ignora um dos índices.
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_activity_logs_workspace_created
    ON activity_logs(workspace_id, created_at DESC);

-- Composite para queries por user + workspace (quem fez o quê em qual workspace):
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_activity_logs_workspace_user
    ON activity_logs(workspace_id, user_id, created_at DESC);

-- ============================================================================
-- FIM DA MIGRATION V9
-- ============================================================================
