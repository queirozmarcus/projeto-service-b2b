# ScopeFlow AI — Status do Projeto

**Data:** 2026-04-17
**Branch:** `develop`

---

## Estado Geral

| Camada | Status | Cobertura |
|--------|--------|-----------|
| Backend — Monólito | ✅ Operacional | ~85% |
| User Service | ✅ DB-per-service consolidado | 100% extraído e validado |
| Frontend | ✅ Operacional | ~70% |
| Docker Stack | ✅ 7/7 serviços healthy | Validação completa 2026-04-17 |
| Infra / CI/CD | ✅ Operacional | — |

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
| Sprint 10 ✅ | 50 testes unitários criados, integration tests refatorados com JWT generation |
| 100+ testes | Unitários + integração com Testcontainers (PostgreSQL real) |

### User Service — Strangler Fig (20/20 sprints ✅)

| Feature | Detalhes |
|---------|---------|
| Estrutura hexagonal | 40 classes Java, domain puro, adapters separados |
| Endpoints Auth | `/register`, `/login`, `/refresh`, `/logout`, `/me` |
| Endpoints User | `/by-email/{email}`, `/users/invited` |
| JWT compartilhado | Mesmo `JWT_SECRET` do monólito — tokens cross-compatible |
| DB-per-service ✅ | `scopeflow_users` PostgreSQL dedicado (porta 5433) — consolidado e validado |
| Flyway V1 | Schema `users` independente do monólito |
| Traefik routing | Prioridade 100 captura `/auth/*` e `/users/*` — validado 2026-04-17 |
| 5 testes | Contract tests + integration tests (Testcontainers) |
| E2E smoke tests | `run-e2e-tests.sh` — auth flow completo monólito + user-service |
| Validação ambiente ✅ | 7/7 serviços healthy, DB isolado confirmado (.claude/validation-report-2026-04-18.md) |

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
| Stack validada ✅ | 7/7 serviços healthy (validação 2026-04-17) — DB-per-service isolado |
| Traefik Strangler Fig | Routing OK: user-service prioridade 100, monólito prioridade 50 |
| docker-compose.staging.yml | Override: `SPRING_PROFILES_ACTIVE=staging` → banco dedicado |
| GitHub Actions | CI/CD para backend e frontend |
| Validação Report | `.claude/validation-report-2026-04-18.md` + `.claude/validation-summary-final.md` |

---

## 🔄 Backlog

| Item | Prioridade | Referência |
|------|-----------|------------|
| Workspace extraction (Strangler Fig Round 2) | **Alta** | Próximo bounded context — ADR-001 |
| Dashboard Sprints 6-10 | Média | Frontend 70% → 90% (API integration completa) |
| Circuit breaker OpenAI + S3 | Baixa | Phase 4 — adapters não existem. TODOs em `ITextPdfServiceAdapter` |
| PDF real (iText 8) | Baixa | Stub existe, aguarda Phase 4 com S3 integration |
| OpenAI real (sem mock) | Baixa | SDK presente, mock local ativo em dev |
| Extração Workspace context | Backlog | Próximo bounded context do Strangler Fig — após user-service em prod estável |

---

## Histórico de Planos

Ver `.claude/plans/`:
- `concluido/` — 41 artefatos de planejamento e execução concluídos
- `backlog/` — planos aprovados aguardando execução
