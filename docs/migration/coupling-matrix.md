# Matriz de Acoplamento — ScopeFlow AI

**Data:** 2026-04-05
**Versao:** 1.0

## Legenda

- **Code**: imports de classes/interfaces de outro contexto
- **Data**: foreign keys no schema PostgreSQL
- **Runtime**: injecao direta de services de outro contexto em controllers/use cases
- **Event**: eventos de dominio consumidos por outro contexto (via outbox/RabbitMQ)

## Matriz (linhas dependem de colunas)

| Contexto ↓ depende de → | User | Workspace | Briefing | Proposal |
|--------------------------|------|-----------|----------|----------|
| **User** | — | — | — | — |
| **Workspace** | Code: UserId (8 classes) | — | — | — |
| | Data: FK workspaces.owner_id, workspace_members.user_id | | | |
| | Runtime: WorkspaceControllerV2 injeta UserService | | | |
| **Briefing** | — | Code: WorkspaceId (12 classes) | — | — |
| | | Data: FK briefing_sessions.workspace_id | | |
| **Proposal** | — | Code: WorkspaceId (4 classes) | Code: BriefingSessionId (6 classes) | — |
| | | Data: FK proposals.workspace_id | Data: FK proposals.briefing_id (RESTRICT) | |

## Infraestrutura compartilhada (transversal)

| Componente | Usado por | Estrategia |
|------------|-----------|------------|
| `outbox` table | User, Workspace, Briefing, Proposal | Compartilhada; filtrar por aggregate_type |
| `activity_logs` table | User, Workspace | Compartilhada; FKs com SET NULL |
| `idempotency_records` table | Briefing (public endpoints) | Move com Briefing |
| `JwtService` + `SecurityConfig` | Todos | Duplicar em cada servico (stateless) |
| `PasswordEncoder` | User, Workspace (invite) | Move com User; Workspace usa REST call |
| `RateLimitInterceptor` | Auth endpoints, public briefing | Duplicar por servico |

## Zonas de ambiguidade (requer decisao)

1. **service_context_profiles** + **service_context_questions**: FK para `workspaces`,
   consumidos por Briefing. Ownership recomendado: Workspace (configuracao), exposto
   para Briefing via API ou evento.

2. **WorkspaceControllerV2.inviteMember()**: chama `UserService.getUserByEmail()` e
   `UserService.saveInvitedUser()` diretamente. Precisa ser refatorado para REST call
   ANTES de extrair Workspace.

3. **SecurityUtil / ScopeFlowPrincipal**: extrai userId e workspaceId do JWT.
   Transversal — cada servico tera sua copia. Candidato a shared library.

## Metricas de acoplamento (resumo)

| Contexto | Deps saida (code) | Deps saida (data) | Deps saida (runtime) | Score |
|----------|-------------------|--------------------|----------------------|-------|
| User | 0 | 0 | 0 | **0** (ideal para extracao) |
| Workspace | 1 (User) | 2 FKs (User) | 1 (UserService) | **4** |
| Briefing | 1 (Workspace) | 1 FK (Workspace) | 0 | **2** |
| Proposal | 2 (Workspace + Briefing) | 2 FKs | 0 | **4** |

*Score = deps code + FKs + runtime injections. Menor score = mais facil de extrair.*

**Nota sobre Briefing (score 2 vs Proposal score 4):**
Briefing tem score menor de acoplamento formal, mas complexidade interna muito maior
(11 endpoints, state machine, IA async, public token flow). O score de acoplamento
sozinho nao determina a ordem — risco tecnico e complexidade interna tambem pesam.
