# Inventário do Schema — ScopeFlow AI

**Data:** 2026-04-05  
**Migrations aplicadas:** V1–V9  
**SGBD:** PostgreSQL 16+  
**ORM:** Hibernate 6.x (JPA 3.1)

---

## Visão Geral

- **Total de tabelas:** 17 (15 de domínio + 2 de infraestrutura)
- **Bounded contexts:** Briefing, Workspace, User, Proposal
- **Multi-tenancy:** Workspace-scoped (todas as tabelas de domínio incluem `workspace_id`)
- **Padrões arquiteturais:** Outbox Pattern, Idempotency, Soft Delete, Optimistic Locking, Immutable Audit Trail

---

## Tabelas por Contexto

### Contexto: **Briefing** (7 tabelas)

O briefing é o agregado central do sistema — fluxo de descoberta AI-assisted que transforma conversas com clientes em escopo aprovado.

#### `briefing_sessions` (Aggregate Root)

- **Owner:** Briefing
- **Aggregate:** BriefingSession (root)
- **Entidade JPA:** `JpaBriefingSession`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `workspace_id` (UUID, FK → workspaces) — Multi-tenancy
  - `client_id` (UUID) — Referência a Client (contexto externo, sem FK)
  - `service_type` (VARCHAR(50)) — Tipo de serviço: SOCIAL_MEDIA, LANDING_PAGE, etc.
  - `status` (VARCHAR(20)) — IN_PROGRESS, COMPLETED, ABANDONED
  - `public_token` (VARCHAR(255), UNIQUE) — Token público para acesso sem autenticação
  - `completion_score` (INT) — Score 0-100, >= 80 para COMPLETED
  - `ai_analysis` (JSONB) — Análise AI final (gaps, recomendações)
  - `abandoned_reason` (VARCHAR(500))
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_briefing_sessions_workspace_id` ON (workspace_id)
  - `idx_briefing_sessions_status` ON (status)
  - `idx_briefing_sessions_client_id` ON (client_id)
  - `idx_briefing_sessions_service_type` ON (service_type)
  - `idx_briefing_sessions_public_token` ON (public_token)
  - `idx_briefing_sessions_created_at` ON (created_at)
  - `idx_briefing_sessions_active_single` UNIQUE ON (workspace_id, client_id, service_type) WHERE status='IN_PROGRESS' — Invariante: 1 briefing ativo por cliente por serviço
  - `idx_briefing_sessions_completed_recent` ON (workspace_id, updated_at) WHERE status='COMPLETED'
  - `ix_briefing_sessions_workspace_client` ON (workspace_id, client_id) — V9
  - `ix_briefing_sessions_workspace_status_created` ON (workspace_id, status, created_at DESC) — V9
  - `ix_briefing_sessions_public_token_unique` UNIQUE ON (public_token) — V9
- **Foreign Keys:**
  - `workspace_id` → `workspaces.id` ON DELETE CASCADE
- **Triggers:**
  - `briefing_sessions_update_updated_at` — Auto-update `updated_at` antes de UPDATE
- **Invariantes:**
  - `status` IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')
  - `service_type` IN ('SOCIAL_MEDIA', 'LANDING_PAGE', 'WEB_DESIGN', 'BRANDING', 'VIDEO_PRODUCTION', 'CONSULTING')
  - Se `status='COMPLETED'`, então `completion_score >= 80`
- **Volume estimado:** 1 sessão por briefing; crescimento médio (1-10K sessões/ano dependendo do workspace)

---

#### `briefing_questions`

- **Owner:** Briefing
- **Aggregate:** BriefingSession (entity filho)
- **Entidade JPA:** `JpaBriefingQuestion`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `briefing_session_id` (UUID, FK → briefing_sessions)
  - `question_text` (TEXT) — Max 5000 chars
  - `step` (INT) — Ordem sequencial (1, 2, 3...)
  - `question_type` (VARCHAR(20)) — OPEN_ENDED, MULTIPLE_CHOICE, SCALE
  - `ai_prompt_version` (VARCHAR(50), default 'v1')
  - `required` (BOOLEAN, default TRUE)
  - `follow_up_generated` (BOOLEAN, default FALSE) — TRUE se auto-gerada pela AI
  - `created_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_briefing_questions_session_id` ON (briefing_session_id)
  - `idx_briefing_questions_step` ON (briefing_session_id, step)
  - `idx_briefing_questions_type` ON (question_type)
  - `idx_briefing_questions_created_at` ON (created_at)
  - `idx_briefing_questions_followup_generated` ON (briefing_session_id) WHERE follow_up_generated=TRUE
  - `idx_briefing_questions_unique_step` UNIQUE ON (briefing_session_id, step) — Invariante: 1 pergunta por step
- **Foreign Keys:**
  - `briefing_session_id` → `briefing_sessions.id` ON DELETE CASCADE
- **Invariantes:**
  - `question_type` IN ('OPEN_ENDED', 'MULTIPLE_CHOICE', 'SCALE')
  - `step > 0`
- **Volume estimado:** 8-12 perguntas por sessão; alto crescimento proporcional a sessões

---

#### `briefing_answers` (Immutable)

- **Owner:** Briefing
- **Aggregate:** BriefingSession (entity filho)
- **Entidade JPA:** `JpaBriefingAnswer`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `briefing_session_id` (UUID, FK → briefing_sessions)
  - `question_id` (UUID, FK → briefing_questions)
  - `answer_text` (TEXT) — Max 5000 chars
  - `answer_json` (JSONB) — Estruturado para múltipla escolha, arrays
  - `quality_score` (INT) — 0-100, computado pela AI
  - `ai_analysis` (JSONB) — Insights AI (gaps, follow-ups sugeridos)
  - `created_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_briefing_answers_session_id` ON (briefing_session_id)
  - `idx_briefing_answers_question_id` ON (question_id)
  - `idx_briefing_answers_created_at` ON (created_at)
  - `idx_briefing_answers_unique_per_question` UNIQUE ON (briefing_session_id, question_id) — Invariante: 1 resposta por questão
- **Foreign Keys:**
  - `briefing_session_id` → `briefing_sessions.id` ON DELETE CASCADE
  - `question_id` → `briefing_questions.id` ON DELETE CASCADE
- **Triggers:**
  - `briefing_answers_immutable_trigger` — BEFORE UPDATE OR DELETE: RAISE EXCEPTION — Respostas são imutáveis
- **Imutabilidade:** INSERT-only; UPDATE/DELETE bloqueados por trigger
- **Volume estimado:** 8-12 respostas por sessão; alto crescimento

---

#### `ai_generations` (Immutable Audit Trail)

- **Owner:** Briefing
- **Aggregate:** BriefingSession (audit filho)
- **Entidade JPA:** `JpaAIGeneration`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `briefing_session_id` (UUID, FK → briefing_sessions)
  - `generation_type` (VARCHAR(50)) — FOLLOW_UP_QUESTION, GAP_ANALYSIS, COMPLETION_SUMMARY
  - `input_json` (JSONB) — Input para LLM
  - `output_json` (JSONB) — Resposta do LLM
  - `prompt_version` (VARCHAR(50), default 'v1')
  - `latency_ms` (BIGINT) — Tempo de round-trip
  - `cost_usd` (NUMERIC(10,6)) — Custo em dólares
  - `model_used` (VARCHAR(100)) — gpt-4, claude-opus, etc.
  - `created_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_ai_generations_session_id` ON (briefing_session_id)
  - `idx_ai_generations_type` ON (generation_type)
  - `idx_ai_generations_prompt_version` ON (prompt_version)
  - `idx_ai_generations_created_at` ON (created_at)
  - `idx_ai_generations_model_used` ON (model_used)
- **Foreign Keys:**
  - `briefing_session_id` → `briefing_sessions.id` ON DELETE CASCADE
- **Invariantes:**
  - `generation_type` IN ('FOLLOW_UP_QUESTION', 'GAP_ANALYSIS', 'COMPLETION_SUMMARY')
- **Imutabilidade:** INSERT-only (sem trigger, mas nunca atualizado/deletado)
- **Retenção:** 30 dias recomendado (audit trail + cost tracking)
- **Volume estimado:** 3-5 gerações AI por sessão; alto crescimento

---

#### `briefing_activity_logs` (Immutable Audit)

- **Owner:** Briefing
- **Aggregate:** BriefingSession (audit filho)
- **Entidade JPA:** `JpaBriefingActivityLog`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `briefing_session_id` (UUID, FK → briefing_sessions)
  - `action` (VARCHAR(100)) — SESSION_STARTED, ANSWER_SUBMITTED, SESSION_COMPLETED, etc.
  - `entity_type` (VARCHAR(50))
  - `entity_id` (UUID)
  - `details` (JSONB) — Contexto adicional (who, where, why)
  - `created_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_briefing_activity_logs_session_id` ON (briefing_session_id)
  - `idx_briefing_activity_logs_action` ON (action)
  - `idx_briefing_activity_logs_entity` ON (entity_type, entity_id)
  - `idx_briefing_activity_logs_created_at` ON (created_at)
- **Foreign Keys:**
  - `briefing_session_id` → `briefing_sessions.id` ON DELETE CASCADE
- **Invariantes:**
  - `action` IN ('SESSION_STARTED', 'SESSION_RESUMED', 'QUESTION_ASKED', 'ANSWER_SUBMITTED', 'FOLLOWUP_GENERATED', 'COMPLETION_REQUESTED', 'SESSION_COMPLETED', 'SESSION_ABANDONED', 'SESSION_RESTARTED', 'PUBLIC_LINK_SHARED', 'PUBLIC_LINK_VIEWED')
- **Imutabilidade:** INSERT-only (sem trigger, mas nunca atualizado/deletado)
- **Volume estimado:** 10-20 eventos por sessão; crescimento proporcional

---

#### `service_context_profiles`

- **Owner:** Briefing (configuração de workspace)
- **Aggregate:** ServiceContextProfile (root)
- **Entidade JPA:** `JpaServiceContextProfile`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `workspace_id` (UUID, FK → workspaces)
  - `service_type` (VARCHAR(50)) — Mesmo domínio de `briefing_sessions.service_type`
  - `profile_name` (VARCHAR(255))
  - `tone_override` (VARCHAR(50)) — FORMAL, CASUAL, TECHNICAL, FRIENDLY
  - `default_entitlements` (JSONB) — Array de deliverables incluídos
  - `default_exclusions` (JSONB) — Array de exclusões explícitas
  - `suggested_timeline` (VARCHAR(255))
  - `pricing_structure` (JSONB) — Tiers, hourly rate, ranges
  - `is_active` (BOOLEAN, default TRUE)
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_service_context_profiles_workspace_id` ON (workspace_id)
  - `idx_service_context_profiles_workspace_active` ON (workspace_id, service_type) WHERE is_active=TRUE
  - `idx_service_context_profiles_unique_active` UNIQUE ON (workspace_id, service_type) WHERE is_active=TRUE — Invariante: 1 perfil ativo por serviço por workspace
- **Foreign Keys:**
  - `workspace_id` → `workspaces.id` ON DELETE CASCADE
- **Triggers:**
  - `service_context_profiles_update_updated_at`
- **Invariantes:**
  - `service_type` IN (mesmos valores de `briefing_sessions.service_type`)
  - `tone_override` IN ('FORMAL', 'CASUAL', 'TECHNICAL', 'FRIENDLY') ou NULL
- **Volume estimado:** 1-6 perfis por workspace (1 por tipo de serviço); baixo crescimento

---

#### `service_context_questions` (Question Templates)

- **Owner:** Briefing (configuração de workspace)
- **Aggregate:** ServiceContextProfile (entity filho)
- **Entidade JPA:** `JpaServiceContextQuestion`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `service_context_profile_id` (UUID, FK → service_context_profiles)
  - `question_text` (VARCHAR(1000))
  - `question_type` (VARCHAR(50)) — OPEN_ENDED, MULTIPLE_CHOICE, SCALE, YES_NO, TEXT, TEXTAREA
  - `order_index` (INT) — Ordem de apresentação (1-based)
  - `is_required` (BOOLEAN, default TRUE)
  - `created_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_service_context_questions_profile_order` ON (service_context_profile_id, order_index)
  - `idx_service_context_questions_unique_order` UNIQUE ON (service_context_profile_id, order_index) — Invariante: 1 questão por ordem por perfil
- **Foreign Keys:**
  - `service_context_profile_id` → `service_context_profiles.id` ON DELETE CASCADE
- **Invariantes:**
  - `question_type` IN ('OPEN_ENDED', 'MULTIPLE_CHOICE', 'SCALE', 'YES_NO', 'TEXT', 'TEXTAREA')
  - `order_index > 0`
- **Volume estimado:** 5-15 perguntas por perfil; baixo crescimento

**Diferença vs. `briefing_questions`:** `service_context_questions` são templates estáticos reutilizáveis; `briefing_questions` são instâncias geradas pela AI durante uma sessão específica, com follow-ups dinâmicos.

---

### Contexto: **Workspace** (2 tabelas)

Workspace é o conceito de tenant/organização no ScopeFlow. Todos os dados de domínio são workspace-scoped.

#### `workspaces` (Aggregate Root)

- **Owner:** Workspace
- **Aggregate:** Workspace (root)
- **Entidade JPA:** `JpaWorkspace`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `owner_id` (UUID, FK → users) — RESTRICT delete: workspace não pode ser deletado se owner não existir
  - `name` (VARCHAR(255), UNIQUE)
  - `niche` (VARCHAR(100)) — Domínio de negócio: social-media, landing-page, branding, etc.
  - `tone_settings` (JSONB) — Configurações de tom (formal, casual, etc.)
  - `status` (VARCHAR(50)) — ACTIVE, SUSPENDED
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_workspaces_owner_id` ON (owner_id)
  - `idx_workspaces_status` ON (status)
  - `idx_workspaces_niche` ON (niche)
  - `idx_workspaces_created_at` ON (created_at)
  - `idx_workspaces_name_unique` UNIQUE ON (name) — Invariante: nome único globalmente
- **Foreign Keys:**
  - `owner_id` → `users.id` ON DELETE RESTRICT
- **Invariantes:**
  - `status` IN ('ACTIVE', 'SUSPENDED')
  - Todo workspace tem exatamente 1 OWNER (enforced no código, não no DB)
- **Volume estimado:** 100-10K workspaces; crescimento médio

---

#### `workspace_members` (Membership)

- **Owner:** Workspace
- **Aggregate:** WorkspaceMember (entity filho ou root — debatível)
- **Entidade JPA:** `JpaWorkspaceMember`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `workspace_id` (UUID, FK → workspaces)
  - `user_id` (UUID, FK → users)
  - `role` (VARCHAR(50)) — OWNER, ADMIN, MEMBER
  - `status` (VARCHAR(50)) — ACTIVE, INVITED, LEFT
  - `joined_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_workspace_members_workspace_id` ON (workspace_id)
  - `idx_workspace_members_user_id` ON (user_id)
  - `idx_workspace_members_role` ON (role)
  - `idx_workspace_members_status` ON (status)
  - `idx_workspace_members_joined_at` ON (joined_at)
  - `idx_workspace_members_owner_check` ON (workspace_id, role, status) WHERE role='OWNER' AND status='ACTIVE' — Para checagem de invariante
  - `uk_workspace_members_unique` UNIQUE ON (workspace_id, user_id) — Invariante: usuário só aparece 1x por workspace
- **Foreign Keys:**
  - `workspace_id` → `workspaces.id` ON DELETE CASCADE
  - `user_id` → `users.id` ON DELETE CASCADE
- **Invariantes:**
  - `role` IN ('OWNER', 'ADMIN', 'MEMBER')
  - `status` IN ('ACTIVE', 'INVITED', 'LEFT')
  - Todo workspace tem >= 1 OWNER ACTIVE (enforced no código)
- **Volume estimado:** 1-50 membros por workspace; crescimento médio

---

### Contexto: **User** (1 tabela)

User é o contexto de identidade e autenticação.

#### `users`

- **Owner:** User
- **Aggregate:** User (root)
- **Entidade JPA:** `JpaUser`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `email` (VARCHAR(255), UNIQUE)
  - `password_hash` (VARCHAR(255)) — BCrypt, nunca plaintext
  - `full_name` (VARCHAR(255))
  - `phone` (VARCHAR(20))
  - `status` (VARCHAR(50)) — ACTIVE, INACTIVE, DELETED
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
  - `version` (BIGINT) — Optimistic locking (adicionado em V4)
- **Índices:**
  - `idx_users_email` ON (email)
  - `idx_users_status` ON (status)
  - `idx_users_created_at` ON (created_at)
- **Foreign Keys:** Nenhuma (root do contexto)
- **Invariantes:**
  - `status` IN ('ACTIVE', 'INACTIVE', 'DELETED')
  - `email` UNIQUE (case-insensitive lookup via normalized form)
- **Volume estimado:** 100-100K usuários; crescimento alto

---

### Contexto: **Proposal** (4 tabelas)

Proposal é o agregado que nasce após um briefing completado — representa a proposta formatada que segue para aprovação do cliente.

#### `proposals` (Aggregate Root)

- **Owner:** Proposal
- **Aggregate:** Proposal (root)
- **Entidade JPA:** `JpaProposal`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `workspace_id` (UUID, FK → workspaces)
  - `client_id` (UUID) — Referência a Client (contexto externo, sem FK)
  - `briefing_id` (UUID, FK → briefing_sessions) — RESTRICT delete
  - `proposal_name` (VARCHAR(500))
  - `status` (VARCHAR(50)) — DRAFT, PUBLISHED, APPROVED, REJECTED
  - `scope_json` (JSONB) — Snapshot atual do escopo (redundante com última proposal_version para reads rápidos)
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
  - `deleted_at` (TIMESTAMP WITH TIME ZONE) — Soft delete (V8)
  - `version` (BIGINT) — Optimistic locking
- **Índices:**
  - `idx_proposals_workspace_id` ON (workspace_id)
  - `idx_proposals_client_id` ON (client_id)
  - `idx_proposals_briefing_id` ON (briefing_id)
  - `idx_proposals_status` ON (status)
  - `idx_proposals_workspace_status` ON (workspace_id, status)
  - `idx_proposals_workspace_active` ON (workspace_id, updated_at DESC) WHERE status IN ('DRAFT', 'PUBLISHED')
  - `idx_proposals_not_deleted` ON (workspace_id, updated_at DESC) WHERE deleted_at IS NULL — V8
  - `idx_proposals_deleted` ON (workspace_id, deleted_at DESC) WHERE deleted_at IS NOT NULL — V8 (audit)
  - `ix_proposals_workspace_client` ON (workspace_id, client_id) WHERE deleted_at IS NULL — V9
  - `ix_proposals_workspace_status_updated` ON (workspace_id, status, updated_at DESC) WHERE deleted_at IS NULL — V9
- **Foreign Keys:**
  - `workspace_id` → `workspaces.id` ON DELETE CASCADE
  - `briefing_id` → `briefing_sessions.id` ON DELETE RESTRICT (briefing não pode ser deletado enquanto proposal referenciar)
- **Triggers:**
  - `proposals_update_updated_at`
- **Invariantes:**
  - `status` IN ('DRAFT', 'PUBLISHED', 'APPROVED', 'REJECTED')
- **Soft Delete:** `@SQLRestriction("deleted_at IS NULL")` no JPA filtra automaticamente
- **Volume estimado:** 1 proposal por briefing completado; crescimento médio

---

#### `proposal_versions` (Immutable History)

- **Owner:** Proposal
- **Aggregate:** Proposal (entity filho)
- **Entidade JPA:** `JpaProposalVersion`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `proposal_id` (UUID, FK → proposals)
  - `scope_json` (JSONB) — Snapshot completo do escopo nesta versão
  - `created_at` (TIMESTAMP WITH TIME ZONE)
  - `created_by` (UUID) — User ID que criou esta versão
- **Índices:**
  - `idx_proposal_versions_proposal_id` ON (proposal_id)
  - `idx_proposal_versions_proposal_created_at` ON (proposal_id, created_at DESC)
  - `ix_proposal_versions_proposal_created_desc` ON (proposal_id, created_at DESC) — V9 (garante covering index)
- **Foreign Keys:**
  - `proposal_id` → `proposals.id` ON DELETE CASCADE
- **Triggers:**
  - `proposal_versions_immutable_trigger` — BEFORE UPDATE OR DELETE: RAISE EXCEPTION
- **Imutabilidade:** INSERT-only; histórico nunca alterado
- **Volume estimado:** 1-5 versões por proposal (revisões); crescimento médio

---

#### `approval_workflows`

- **Owner:** Proposal
- **Aggregate:** ApprovalWorkflow (root ou entity filho)
- **Entidade JPA:** `JpaApprovalWorkflow`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `proposal_id` (UUID, FK → proposals)
  - `status` (VARCHAR(50)) — PENDING, IN_PROGRESS, APPROVED, REJECTED
  - `initiated_at` (TIMESTAMP WITH TIME ZONE)
  - `completed_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_approval_workflows_proposal_id` ON (proposal_id)
  - `idx_approval_workflows_status` ON (status)
  - `idx_approval_workflows_open` ON (initiated_at) WHERE status IN ('PENDING', 'IN_PROGRESS') — Background jobs/reminders
  - `ix_approval_workflows_status_completed` ON (status, completed_at DESC) — V9 (dashboards)
  - `uq_approval_workflows_proposal` UNIQUE ON (proposal_id) — Invariante: 1 workflow por proposal
- **Foreign Keys:**
  - `proposal_id` → `proposals.id` ON DELETE CASCADE
- **Invariantes:**
  - `status` IN ('PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED')
  - Exatamente 1 workflow por proposal (UNIQUE constraint)
- **Volume estimado:** 1 workflow por proposal; crescimento médio

---

#### `approvals`

- **Owner:** Proposal
- **Aggregate:** ApprovalWorkflow (entity filho)
- **Entidade JPA:** `JpaApproval`
- **Colunas principais:**
  - `id` (UUID, PK)
  - `workflow_id` (UUID, FK → approval_workflows)
  - `approver_name` (VARCHAR(255))
  - `approver_email` (VARCHAR(255)) — Não é FK (clientes aprovando não precisam ser users)
  - `status` (VARCHAR(50)) — PENDING, APPROVED, REJECTED
  - `ip_address` (VARCHAR(45)) — Audit trail (LGPD compliance)
  - `user_agent` (TEXT) — Browser fingerprint (forensic)
  - `approved_at` (TIMESTAMP WITH TIME ZONE)
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_approvals_workflow_id` ON (workflow_id)
  - `idx_approvals_status` ON (status)
  - `idx_approvals_approver_email` ON (approver_email)
  - `idx_approvals_workflow_email` ON (workflow_id, approver_email) — Covers exact query pattern
  - `ix_approvals_workflow_status` ON (workflow_id, status) — V9 (hot path de resolução)
  - `uq_approvals_workflow_approver` UNIQUE ON (workflow_id, approver_email) — Invariante: 1 decisão por approver
- **Foreign Keys:**
  - `workflow_id` → `approval_workflows.id` ON DELETE CASCADE
- **Triggers:**
  - `approvals_update_updated_at`
- **Invariantes:**
  - `status` IN ('PENDING', 'APPROVED', 'REJECTED')
- **Volume estimado:** 1-3 aprovadores por workflow; crescimento médio

---

### Tabelas de Infraestrutura (2 tabelas)

Estas tabelas suportam padrões transversais usados por todos os contextos.

#### `outbox_event` (Outbox Pattern)

- **Owner:** Infraestrutura (compartilhado por todos os contextos)
- **Propósito:** Transactional Outbox — garante atomicidade entre TX do banco e publicação de eventos no RabbitMQ
- **Colunas principais:**
  - `id` (UUID, PK)
  - `event_type` (VARCHAR(255)) — Fully qualified class name do evento
  - `aggregate_id` (UUID) — ID do agregado que produziu o evento
  - `aggregate_type` (VARCHAR(100)) — Tipo do agregado (User, Proposal, BriefingSession)
  - `payload` (JSONB) — Serialização JSON do evento
  - `published_at` (TIMESTAMP WITH TIME ZONE) — NULL = não publicado ainda
  - `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_outbox_event_unpublished` ON (created_at) WHERE published_at IS NULL — Worker poll (hot path)
  - `idx_outbox_event_aggregate` ON (aggregate_type, aggregate_id)
  - `idx_outbox_event_type` ON (event_type)
  - `ix_outbox_event_aggregate_id` ON (aggregate_id) — V9 (replay/debug)
- **Foreign Keys:** Nenhuma (desacoplado de domínio)
- **Invariantes:**
  - `event_type` ~ '^[A-Za-z0-9]+Event$' (regex check)
- **Background Job:** `OutboxEventPublisher` — scheduled a cada 5s, publica eventos não publicados
- **Volume estimado:** 1-10 eventos por agregado modificado; alto crescimento; **retenção 7 dias** recomendado após publicação

---

#### `idempotency_record` (Idempotent Consumer Pattern)

- **Owner:** Infraestrutura (compartilhado por todos os contexts)
- **Propósito:** Prevenir side effects duplicados em event listeners (email 2x, PDF 2x, etc.)
- **Colunas principais:**
  - `id` (UUID, PK)
  - `listener_id` (VARCHAR(100)) — ID lógico do listener (ex: "approval-listener")
  - `idempotency_key` (VARCHAR(255)) — Chave única para este processamento (ex: "approval-listener:proposal-id:event-id")
  - `processed_at` (TIMESTAMP WITH TIME ZONE)
  - `result_data` (JSONB) — Resultado do processamento (email ID, PDF URL, etc.)
- **Índices:**
  - `idx_idempotency_key` ON (listener_id, idempotency_key)
  - `idx_idempotency_processed_at` ON (processed_at)
  - `unique_listener_idempotency` UNIQUE ON (listener_id, idempotency_key) — Invariante: 1 registro por (listener, key)
- **Foreign Keys:** Nenhuma
- **Volume estimado:** 1 registro por evento processado com side effect; alto crescimento; **retenção 30 dias** recomendado

---

#### `activity_logs` (Global Audit Trail)

**NOTA:** Esta tabela foi criada em V2 com propósito de audit trail global, mas hoje o Briefing tem sua própria `briefing_activity_logs` mais especializada. `activity_logs` ainda serve para auditoria de User/Workspace, mas há overlap. Recomendação: consolidar em futuro refactor.

- **Owner:** Infraestrutura (compartilhado)
- **Colunas principais:**
  - `id` (UUID, PK)
  - `workspace_id` (UUID, FK → workspaces) — Nullable
  - `user_id` (UUID, FK → users) — Nullable
  - `action` (VARCHAR(100))
  - `entity_type` (VARCHAR(100))
  - `entity_id` (UUID)
  - `changes` (JSONB) — Diff before/after
  - `ip_address` (VARCHAR(45))
  - `user_agent` (TEXT)
  - `created_at` (TIMESTAMP WITH TIME ZONE)
- **Índices:**
  - `idx_activity_logs_workspace_id` ON (workspace_id)
  - `idx_activity_logs_user_id` ON (user_id)
  - `idx_activity_logs_entity` ON (entity_type, entity_id)
  - `idx_activity_logs_action` ON (action)
  - `idx_activity_logs_created_at` ON (created_at)
  - `ix_activity_logs_workspace_created` ON (workspace_id, created_at DESC) — V9
  - `ix_activity_logs_workspace_user` ON (workspace_id, user_id, created_at DESC) — V9
- **Foreign Keys:**
  - `workspace_id` → `workspaces.id` ON DELETE SET NULL
  - `user_id` → `users.id` ON DELETE SET NULL
- **Imutabilidade:** INSERT-only
- **Volume estimado:** Alto; retenção LGPD: 5 anos (compliance) ou conforme política do workspace

---

## Views (3)

### `v_workspace_members_active`

```sql
SELECT wm.id, wm.workspace_id, wm.user_id, u.email, u.full_name, wm.role, wm.joined_at, wm.updated_at
FROM workspace_members wm
JOIN users u ON wm.user_id = u.id
WHERE wm.status = 'ACTIVE';
```

- **Propósito:** Lista membros ativos com detalhes do user
- **Uso:** Dashboard de workspace

---

### `v_workspace_owners`

```sql
SELECT workspace_id, COUNT(*) as owner_count
FROM workspace_members
WHERE role = 'OWNER' AND status = 'ACTIVE'
GROUP BY workspace_id;
```

- **Propósito:** Verificar invariante (owner_count >= 1)
- **Uso:** Health checks, background jobs

---

### `v_briefing_sessions_active`

```sql
SELECT bs.id, bs.workspace_id, bs.client_id, bs.service_type, bs.status, bs.public_token,
       COUNT(bq.id) as total_questions,
       COUNT(ba.id) as answered_questions,
       CASE WHEN COUNT(bq.id) > 0 THEN (COUNT(ba.id) * 100 / COUNT(bq.id)) ELSE 0 END as progress_percentage,
       bs.created_at, bs.updated_at
FROM briefing_sessions bs
LEFT JOIN briefing_questions bq ON bs.id = bq.briefing_session_id
LEFT JOIN briefing_answers ba ON bq.id = ba.question_id
WHERE bs.status = 'IN_PROGRESS'
GROUP BY bs.id, bs.workspace_id, bs.client_id, bs.service_type, bs.status, bs.public_token, bs.created_at, bs.updated_at;
```

- **Propósito:** Dashboard com métricas de progresso
- **Nota:** View materializada pode ser benéfica se uso for intenso

---

### `v_briefing_sessions_completed`

Análoga a `active`, mas filtra `status = 'COMPLETED'` e inclui métricas de qualidade.

---

### `v_ai_generation_costs`

```sql
SELECT DATE_TRUNC('day', ag.created_at)::DATE as generation_date,
       ag.generation_type, ag.model_used,
       COUNT(*) as call_count,
       SUM(ag.latency_ms) as total_latency_ms,
       AVG(ag.latency_ms) as avg_latency_ms,
       SUM(ag.cost_usd) as total_cost_usd,
       AVG(ag.cost_usd) as avg_cost_usd
FROM ai_generations ag
GROUP BY generation_date, ag.generation_type, ag.model_used
ORDER BY generation_date DESC;
```

- **Propósito:** Monitoramento de custo e performance AI
- **Uso:** FinOps, dashboards de custo

---

## Funções & Procedures

### `update_updated_at_column()`

Trigger function reutilizável — auto-update `updated_at` em UPDATE quando `OLD.* IS DISTINCT FROM NEW.*`.

**Aplicado em:**
- `briefing_sessions`
- `proposals`
- `approvals`
- `service_context_profiles`

---

### `get_briefing_progress(p_session_id UUID) RETURNS INT`

Calcula progresso percentual de um briefing: `(answered_questions * 100) / total_questions`.

**Uso:** Pode ser substituído pela view `v_briefing_sessions_active` para evitar chamadas repetidas.

---

## Estimativa de Volume de Dados

| Tabela | Taxa de Crescimento | Retenção | Observações |
|--------|---------------------|----------|-------------|
| `briefing_sessions` | Média | Indefinida | 1 por briefing; 100-10K/ano por workspace ativo |
| `briefing_questions` | Alta | Indefinida | 8-12 por sessão |
| `briefing_answers` | Alta | Indefinida | 8-12 por sessão |
| `ai_generations` | Alta | 30 dias | 3-5 por sessão; **tabela quente** |
| `briefing_activity_logs` | Alta | 90 dias | 10-20 eventos por sessão |
| `service_context_profiles` | Baixa | Indefinida | 1-6 por workspace |
| `service_context_questions` | Baixa | Indefinida | 5-15 por perfil |
| `workspaces` | Média | Indefinida | 100-10K total |
| `workspace_members` | Média | Indefinida | 1-50 por workspace |
| `users` | Alta | Indefinida | 100-100K total |
| `proposals` | Média | Indefinida | 1 por briefing completado |
| `proposal_versions` | Média | Indefinida | 1-5 por proposal |
| `approval_workflows` | Média | Indefinida | 1 por proposal |
| `approvals` | Média | Indefinida | 1-3 por workflow |
| `outbox_event` | Alta | 7 dias | **Tabela quente**; limpar após `published_at` |
| `idempotency_record` | Alta | 30 dias | 1 por evento processado |
| `activity_logs` | Alta | 5 anos | LGPD compliance; considerar particionamento |

---

## Recomendações de Performance & Manutenção

### Particionamento (Futuro)

Tabelas candidatas para particionamento por `created_at` (range partitioning mensal):
- `ai_generations` — cresce rápido, leitura recente
- `briefing_activity_logs` — auditoria por range temporal
- `activity_logs` — auditoria global; particionamento essencial em escala
- `outbox_event` — se retenção aumentar além de 7 dias

---

### Índices Parciais & Covering Indexes

V9 adicionou 10 índices compostos focados em hot paths:
- ✅ `ix_briefing_sessions_workspace_status_created` — cobre paginação ordenada por data
- ✅ `ix_proposals_workspace_status_updated` — idem para proposals
- ✅ `ix_approvals_workflow_status` — resolução de workflow (query crítica)

**Próximos passos:**
- Monitorar `pg_stat_statements` para identificar queries lentas
- Adicionar índices BRIN para tabelas particionadas (futuro)

---

### Vacuum & Analyze

- `ai_generations`, `outbox_event`, `idempotency_record`: **autovacuum agressivo** (high churn)
- Tabelas imutáveis (answers, proposal_versions): autovacuum padrão suficiente

---

### Backup & Retenção

| Categoria | Backup | Retenção em Prod |
|-----------|--------|------------------|
| Domínio crítico (briefing, proposals, users, workspaces) | Daily full + WAL | Indefinida |
| Audit trail (activity_logs, briefing_activity_logs) | Daily full | 5 anos (LGPD) |
| Infraestrutura (outbox, idempotency) | Daily full | 7-30 dias (pode purgar) |
| AI audit (ai_generations) | Weekly | 30 dias (ou mais se FinOps exigir) |

---

## Conclusão

O schema do ScopeFlow AI está **bem estruturado** para suportar crescimento em multi-tenancy. Principais pontos fortes:

1. **Bounded contexts claros** — Cada contexto gerencia suas tabelas sem leaks cross-context
2. **Invariantes garantidos por DB** — UNIQUE constraints, CHECK constraints, triggers de imutabilidade
3. **Auditoria completa** — Immutable audit trails em todos os agregados críticos
4. **Patterns de resiliência** — Outbox para eventos, idempotency para consumers
5. **Multi-tenancy seguro** — Workspace-scoping em todas as tabelas de domínio

**Pontos de atenção para migração futura:**
- `client_id` é UUID sem FK — se Client virar bounded context próprio, será necessário event-driven sync
- `activity_logs` vs `briefing_activity_logs` — overlap a consolidar
- Tabelas quentes (`ai_generations`, `outbox_event`) — monitorar crescimento e planejar particionamento

---

**FIM DO INVENTÁRIO**
