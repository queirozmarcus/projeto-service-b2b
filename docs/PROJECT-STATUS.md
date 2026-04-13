# ScopeFlow AI — Status do Projeto

**Data:** 2026-04-13
**Branch:** `develop`

---

## Estado Geral

| Camada | Status | Cobertura |
|--------|--------|-----------|
| Backend — Monólito | ✅ Operacional | ~85% |
| User Service | ✅ Staging ativo | 100% extraído |
| Frontend | ✅ Operacional | ~70% |
| Infra / CI/CD | ✅ Operacional | — |
| Produção | 🔄 Pendente | Cut-over user-service aguardando |

---

## ✅ Concluído

### Backend — Monólito (Spring Boot 3.2.0 + Java 21)

| Feature | Detalhes |
|---------|---------|
| Arquitetura hexagonal | Domains: Briefing, Workspace, Proposal, Client, User (decommissioned) |
| 9 Flyway migrations | V1–V9 aplicadas — schema completo |
| Auth (JWT) | Spring Security 6 + access token (15min) + refresh token (7d) |
| Briefing domain | Sealed classes, question flow, gap detection, 80%+ score para completion |
| Proposal domain | CRUD, publish, soft delete, workspace-scoped |
| Outbox pattern | `outbox_events` + `OutboxEventPublisher` — exactly-once delivery |
| Idempotency | `Idempotency-Key` header, cache 24h, dedup de submissões públicas |
| Purge jobs | 3 jobs diários (02:00/02:15/02:30 UTC) — outbox 7d, idempotency 30d, ai_generations 90d |
| Circuit breakers | user-service (CB+Retry) e SES (CB) via Resilience4j |
| GlobalExceptionHandler | RFC 9457 completo — domain, JWT, circuit breaker, rate limit, 503 |
| WorkspaceControllerV2 | Invite via user-service (`findByEmail` + `createInvitedUser`) |
| 59 testes | Unitários + integração com Testcontainers (PostgreSQL real) |

### User Service — Strangler Fig (20/20 sprints ✅)

| Feature | Detalhes |
|---------|---------|
| Estrutura hexagonal | 40 classes Java, domain puro, adapters separados |
| Endpoints Auth | `/register`, `/login`, `/refresh`, `/logout`, `/me` |
| Endpoints User | `/by-email/{email}`, `/users/invited` |
| JWT compartilhado | Mesmo `JWT_SECRET` do monólito — tokens cross-compatible |
| DB-per-service | `scopeflow_users` PostgreSQL dedicado (porta 5433) |
| Flyway V1 | Schema `users` independente do monólito |
| Profile staging | `application-staging.yml` → `user-db:5432/scopeflow_users` |
| Traefik routing | Prioridade 100 captura `/auth/*` e `/users/*` |
| 5 testes | Contract tests + integration tests (Testcontainers) |
| E2E smoke tests | `run-e2e-tests.sh` — auth flow completo monólito + user-service |

### Frontend (Next.js 15 + TypeScript)

| Feature | Detalhes |
|---------|---------|
| Auth flow | Login, register, JWT refresh automático (axios interceptor) |
| Dashboard | Stats reais (proposals), atividade recente, quick actions |
| Proposals | Lista paginada, detalhes, publish, delete (two-step confirm) |
| Briefings | Lista de sessões com status |
| Stores Zustand | `useSession`, `useDashboardStore`, `useBriefingSessionStore` |
| API clients | `proposalApi`, `briefingSessionApi`, `api.ts` (axios + JWT) |
| Toast system | Zustand-based, auto-dismiss 4s/6s, max 3 concurrent |
| 7 suítes Vitest | `proposalApi`, `dashboardStore`, `useToast`, `CompletionSummary`, etc. |

### Infraestrutura

| Feature | Detalhes |
|---------|---------|
| Docker Compose | 7 serviços: postgres, user-db, rabbitmq, redis, traefik, app, user-service |
| docker-compose.staging.yml | Override: `SPRING_PROFILES_ACTIVE=staging` → banco dedicado |
| GitHub Actions | CI/CD para backend e frontend |
| DB_MIGRATION_GUIDE.md | Passos documentados para cut-over e rollback em produção |

---

## 🔄 Backlog

| Item | Prioridade | Referência |
|------|-----------|------------|
| Cut-over user-service em produção | **Alta** | `.claude/plans/backlog/2026-04-12-migration-fase3-cutover-producao.md` |
| Fix `BriefingControllerV1IntegrationTest` | Média | `.questionId()` não existe em `BriefingQuestion` — compile error pré-existente |
| Circuit breaker OpenAI + S3 | Baixa | Phase 4 — adapters não existem. TODOs em `ITextPdfServiceAdapter` |
| PDF real (iText 8) | Baixa | Stub existe, aguarda Phase 4 com S3 integration |
| OpenAI real (sem mock) | Baixa | SDK presente, mock local ativo em dev |
| Extração Workspace context | Backlog | Próximo bounded context do Strangler Fig — após user-service em prod estável |

---

## Histórico de Planos

Ver `.claude/plans/`:
- `concluido/` — 32 artefatos de planejamento e execução concluídos
- `backlog/` — planos aprovados aguardando execução
