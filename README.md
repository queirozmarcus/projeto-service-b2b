# ScopeFlow AI — B2B Service Discovery Platform

AI-powered SaaS platform that helps B2B service providers (freelancers, microagencies) transform client conversations into clear, approved scopes through structured AI-assisted discovery.

**Branch ativa:** `develop` — **Última atualização:** 2026-04-13

---

## Estado Atual do Projeto

| Camada | Status | Detalhes |
|--------|--------|---------|
| **Backend — Monólito** | ✅ ~85% | Todos os domínios implementados, circuit breakers, purge jobs |
| **User Service** | ✅ 100% (staging) | Extraído via Strangler Fig, DB-per-service ativo |
| **Frontend** | ✅ ~70% | Dashboard, proposals e briefings integrados com API real |
| **Infra / CI/CD** | ✅ Operacional | Docker Compose completo, Traefik, GitHub Actions |
| **Produção** | 🔄 Pendente | User service estável em staging; cut-over prod aguardando |

---

## Tech Stack

| Layer | Technology | Status |
|-------|-----------|--------|
| **Frontend** | Next.js 15 + React 19 + TypeScript + Zustand | ✅ Operacional |
| **Backend** | Spring Boot 3.4.3 + Java 21 + Virtual Threads | ✅ Operacional |
| **User Service** | Spring Boot 3.4.3 + Java 21 (microsserviço) | ✅ Operacional |
| **Database (monólito)** | PostgreSQL 16 + Flyway (V1–V9) | ✅ Operacional |
| **Database (user-service)** | PostgreSQL 16 dedicado + Flyway (V1) | ✅ Staging ativo |
| **Queue** | RabbitMQ 3.13 | ✅ Docker |
| **Cache** | Redis 7 | ✅ Docker |
| **API Gateway** | Traefik v3.0 (Strangler Fig routing) | ✅ Operacional |
| **Authentication** | Spring Security 6.x + JWT (shared secret) | ✅ Cross-service |
| **Resilience** | Resilience4j (Circuit Breaker + Retry) | ✅ user-service + SES |
| **PDF Generation** | iText 8 (stub — Phase 4) | 🔄 Planned |
| **LLM Integration** | OpenAI SDK Java 0.18.0 (mock local) | 🔄 Partial |
| **Email** | AWS SES adapter + circuit breaker | ✅ Implementado |
| **Storage** | AWS S3 (stub — Phase 4) | 🔄 Planned |
| **Observability** | Logback + SLF4J + Prometheus (Actuator) | ✅ Configurado |
| **Testing** | JUnit 5 + AssertJ + Testcontainers + Vitest | ✅ Operacional |
| **CI/CD** | GitHub Actions | ✅ Operacional |

---

## Architecture

### Padrão Geral

**Hexagonal (Ports & Adapters) + DDD** em ambos backend e user-service.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Traefik v3.0                              │
│         PathPrefix(/api/v1/auth|/users) → user-service          │
│         PathPrefix(/api/)              → monolith               │
└──────────────┬──────────────────────────┬───────────────────────┘
               │                          │
    ┌──────────▼──────────┐   ┌───────────▼──────────┐
    │  user-service :8081  │   │   monolith :8080      │
    │  Java 21 + SB 3.4    │   │   Java 21 + SB 3.4    │
    │  DB: scopeflow_users │   │   DB: scopeflow       │
    └──────────────────────┘   └──────────────────────┘
               │                          │
    ┌──────────▼──────────┐   ┌───────────▼──────────┐
    │ PostgreSQL :5433     │   │ PostgreSQL :5432      │
    │ scopeflow_users      │   │ scopeflow             │
    └──────────────────────┘   └──────────────────────┘
```

### Domínios do Monólito

| Domínio | Pacote | Estado |
|---------|--------|--------|
| **User** | `core/domain/user/` | ✅ Extraído para user-service (decommissioned no monólito) |
| **Workspace** | `core/domain/workspace/` | ✅ Completo |
| **Briefing** | `core/domain/briefing/` | ✅ Completo (sealed classes, question flow) |
| **Proposal** | `core/domain/proposal/` | ✅ Completo |
| **Client** | `core/domain/client/` | ✅ Completo |

### Strangler Fig — Status de Extração

| Bounded Context | Status | Notas |
|----------------|--------|-------|
| **User (Auth)** | ✅ Extraído — staging ativo | DB-per-service em `scopeflow_users` |
| **Workspace** | 🔄 Próxima extração | Após user-service estável em prod |
| **Briefing** | ⏳ Backlog | Dependência: Workspace extraído |
| **Proposal** | ⏳ Backlog | Dependência: Briefing extraído |

---

## Getting Started

### Prerequisites

- **Java 21** (Eclipse Temurin recomendado)
- **Maven 3.8+** (ou use o wrapper `./mvnw`)
- **Node.js LTS** (para o frontend)
- **Docker Desktop** com WSL2 integration
- **OpenAI API key** (opcional em dev — serviço retorna mock local)

### Setup Local (Docker Compose)

```bash
# Clone
git clone <repo>
cd projeto-service-b2b

# Configurar variáveis de ambiente
cp .env.example .env
# Editar .env: JWT_SECRET (mín. 32 chars), OPENAI_API_KEY (opcional)

# Subir stack completa (postgres, user-db, rabbitmq, redis, traefik, app, user-service)
docker compose up -d

# Verificar saúde dos serviços
docker compose ps
```

### Setup Staging (DB-per-service ativo)

```bash
# Subir stack com profile staging (user-service → banco dedicado scopeflow_users)
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d

# Verificar routing do Traefik
curl -s http://localhost:8888/api/http/routers | jq '.[] | {name: .name, priority: .priority}'
```

### Desenvolvimento Local (Spring Boot direto)

```bash
# Subir apenas infraestrutura
docker compose up postgres user-db rabbitmq redis -d

# Backend — monólito
cd backend
./mvnw spring-boot:run     # http://localhost:8080/api/v1

# User service
cd user-service
./mvnw spring-boot:run     # http://localhost:8081/api/v1

# Frontend
cd frontend
npm install && npm run dev  # http://localhost:3000
```

---

## Comandos de Desenvolvimento

### Backend (Monólito)

```bash
cd backend

./mvnw test                                # Testes unitários
./mvnw verify                              # Testes unitários + integração (Testcontainers)
./mvnw test -Dtest=PurgeJobServiceTest     # Classe específica
./mvnw package jacoco:report               # Relatório de cobertura
./mvnw flyway:migrate                      # Aplicar migrations pendentes
./mvnw flyway:info                         # Status das migrations
```

### User Service

```bash
cd user-service

./mvnw test                                # Testes unitários
./mvnw verify                              # Testes de integração
./mvnw spring-boot:run                     # Iniciar localmente (porta 8081)
```

### Frontend

```bash
cd frontend

npm run dev                                # Dev server
npm run build                              # Build de produção
npm run type-check                         # TypeScript strict mode
npm run test                               # Vitest (unit + component)
npm run test:coverage                      # Cobertura
```

---

## Estrutura do Projeto

```
projeto-service-b2b/
├── backend/                             # Monólito Spring Boot 3.4.3 + Java 21
│   ├── src/main/java/com/scopeflow/
│   │   ├── core/domain/                 # Domínio puro (zero Spring)
│   │   │   ├── briefing/                # Briefing aggregate (sealed classes)
│   │   │   ├── workspace/               # Workspace aggregate
│   │   │   ├── user/                    # User domain (ServiceUnavailableException)
│   │   │   └── client/                  # Client aggregate
│   │   ├── application/                 # Use cases e orquestração
│   │   │   ├── port/out/                # Output ports (interfaces)
│   │   │   ├── idempotency/             # IdempotencyService + Repository
│   │   │   ├── outbox/                  # OutboxService + OutboxEventPublisher
│   │   │   ├── purge/                   # PurgeJobService (3 jobs diários)
│   │   │   └── listener/                # Domain event listeners
│   │   ├── adapter/
│   │   │   ├── in/web/                  # REST controllers
│   │   │   │   ├── auth/                # AuthController
│   │   │   │   ├── briefing/            # BriefingControllerV1 + Public
│   │   │   │   ├── proposal/            # ProposalControllerV2
│   │   │   │   ├── workspace/           # WorkspaceControllerV2 (invite via user-service)
│   │   │   │   ├── user/                # UserController
│   │   │   │   └── GlobalExceptionHandler.java  # RFC 9457 + Circuit Breaker handlers
│   │   │   └── out/
│   │   │       ├── persistence/         # JPA entities + repositories
│   │   │       ├── email/               # AwsSesEmailServiceAdapter (@CircuitBreaker)
│   │   │       ├── pdf/                 # ITextPdfServiceAdapter (stub Phase 4)
│   │   │       └── userservice/         # UserServiceRestAdapter (@CircuitBreaker + @Retry)
│   │   └── config/                      # Spring, Security, Resilience4j configs
│   └── src/main/resources/
│       ├── application.yml              # Config base + Resilience4j + Purge
│       ├── application-local.yml        # Dev local
│       ├── application-staging.yml      # Staging
│       └── db/migration/                # V1–V9 (Flyway)
│
├── user-service/                        # Microsserviço extraído (Strangler Fig)
│   ├── src/main/java/com/scopeflow/user/
│   │   ├── domain/                      # User aggregate (Email, PasswordHash, UserId)
│   │   ├── application/                 # RegisterUser, AuthenticateUser, GetProfile
│   │   ├── adapter/
│   │   │   ├── in/web/                  # AuthController, UserController
│   │   │   └── out/persistence/         # JpaUserRepository
│   │   └── config/                      # Security, JWT, CORS
│   └── src/main/resources/
│       ├── application.yml              # Base (Flyway habilitado, baseline-on-migrate)
│       ├── application-staging.yml      # Staging → user-db:5432/scopeflow_users
│       ├── application-production.yml   # Prod (flyway.enabled: false até cut-over)
│       └── db/migration/V1__create_users_table.sql
│
├── frontend/                            # Next.js 15 + React 19 + TypeScript
│   └── src/
│       ├── app/
│       │   ├── (auth)/                  # login, register
│       │   └── dashboard/               # Dashboard, proposals, briefings
│       ├── stores/                      # Zustand: useSession, useDashboard, useBriefing
│       ├── lib/                         # proposalApi, briefingApi, api.ts (axios+JWT)
│       ├── hooks/                       # useToast
│       └── components/
│           ├── dashboard/               # StatsGrid, ProposalList, RecentActivity, etc.
│           └── ui/                      # ToastContainer
│
├── docker-compose.yml                   # Stack completa (7 serviços)
├── docker-compose.staging.yml           # Override: user-service → DB dedicado
├── docs/
│   ├── api/                             # OpenAPI specs + guias
│   └── architecture/adr/               # ADRs
└── .claude/plans/
    ├── concluido/                       # 32 artefatos de planejamento e execução
    └── backlog/                         # Cut-over prod + backlog técnico
```

---

## Flyway Migrations — Monólito

| Versão | Arquivo | Descrição |
|--------|---------|-----------|
| V1 | `V1__initial_schema.sql` | Schema inicial |
| V2 | `V2__user_workspace_domain_schema.sql` | User + Workspace domain |
| V3 | `V3__briefing_domain_schema.sql` | Briefing domain |
| V4 | `V4__proposal_domain_schema.sql` | Proposal domain |
| V5 | `V5__outbox_event_schema.sql` | Outbox pattern table |
| V6 | `V6__idempotency_record_schema.sql` | Idempotency table |
| V7 | `V7__fix_outbox_event_type_constraint.sql` | Fix constraint |
| V8 | `V8__proposal_soft_delete_and_service_context_schema.sql` | Soft delete + service context |
| V9 | `V9__add_missing_indexes.sql` | Índices de performance |

---

## API Endpoints

### Monólito (via Traefik :80 ou direto :8080)

**Auth** — `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`

**Workspace** — `POST /api/v1/workspaces`, `GET /api/v1/workspaces/mine`, `POST /api/v1/workspaces/{id}/members/invite`

**Briefing** — `POST /api/v1/briefing-sessions`, `GET /api/v1/briefing-sessions`, `GET /api/v1/briefing-sessions/{id}`, `POST /api/v1/briefing-sessions/{id}/answers`, `POST /api/v1/briefing-sessions/{id}/complete`

**Proposals** — `POST /api/v1/proposals`, `GET /api/v1/proposals`, `GET /api/v1/proposals/{id}`, `DELETE /api/v1/proposals/{id}`, `POST /api/v1/proposals/{id}/publish`

**Public (sem auth)** — `GET /public/briefings/{token}`, `POST /public/briefings/{token}/answers`

### User Service (via Traefik :80 ou direto :8081)

`POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /api/v1/users/me`, `GET /api/v1/users/by-email/{email}`, `POST /api/v1/users/invited`

**Health:** `GET /api/v1/actuator/health/readiness` (ambos os serviços)

---

## Error Handling — RFC 9457

```json
{
  "type": "https://api.scopeflow.com/errors/briefing-not-found",
  "title": "Briefing Not Found",
  "status": 404,
  "detail": "Briefing session 7c9e6679 was not found",
  "errorCode": "BRIEFING-001",
  "errorId": "550e8400-...",
  "timestamp": "2026-04-13T00:00:00Z"
}
```

Códigos de erro documentados: `BRIEFING-001..007`, `WORKSPACE-001..005`, `USER-001..013`, `PROPOSAL-001..005`

Circuit breaker aberto → HTTP 503 com `Retry-After: 30`

---

## Resilience4j — Circuit Breakers

| Instância | Serviço protegido | Sliding window | Failure threshold | Wait |
|-----------|------------------|---------------|-------------------|------|
| `user-service` | `UserServiceRestAdapter` | 10 calls | 50% | 30s |
| `ses` | `AwsSesEmailServiceAdapter` | 5 calls | 60% | 30s |

`@Retry(name = "user-service")` aplicado em operações de leitura/escrita ao user-service (2 tentativas, 200ms de espera).

---

## Purge Jobs (Scheduled)

| Job | Horário | Tabela | Retenção padrão |
|-----|---------|--------|----------------|
| Outbox cleanup | 02:00 UTC | `outbox_events` | 7 dias (somente `publishedAt IS NOT NULL`) |
| Idempotency cleanup | 02:15 UTC | `idempotency_record` | 30 dias |
| AI generations cleanup | 02:30 UTC | `ai_generations` | 90 dias |

Controlados via `app.purge.enabled` (env var). Todos os valores de retenção configuráveis.

---

## Traefik Routing (Strangler Fig)

| Router | Rule | Priority | Destino |
|--------|------|----------|---------|
| `user-service` | `PathPrefix(/api/v1/auth) \|\| PathPrefix(/api/v1/users)` | **100** | `:8081` |
| `monolith` | `PathPrefix(/api/)` | 50 | `:8080` |

```bash
# Verificar routers ativos
curl -s http://localhost:8888/api/http/routers | jq '.[] | {name: .name, priority: .priority, status: .status}'
```

---

## Testes

### Monólito (59 testes)

```bash
cd backend
./mvnw test         # Unitários (sem Docker)
./mvnw verify       # + Integração (Testcontainers — requer Docker)
```

### User Service (5 testes)

```bash
cd user-service
./mvnw verify       # Unitários + contract tests + integração
```

### Frontend (7 suítes Vitest)

```bash
cd frontend
npm run test        # Vitest — proposalApi, dashboardStore, useToast, etc.
```

### E2E (smoke tests)

```bash
./run-e2e-tests.sh              # Auth flow completo
./run-e2e-tests.sh --monolith   # Monólito direto
./run-e2e-tests.sh --user-service  # Via user-service
```

---

## Docker Compose — Referência Rápida

```bash
# Stack dev padrão
docker compose up -d

# Stack staging (user-service → DB dedicado)
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d

# Rebuild serviço específico (após alteração em código)
docker compose build user-service && docker compose up -d user-service

# Logs em tempo real
docker compose logs user-service -f
docker compose logs app -f

# Saúde dos serviços
docker compose ps

# Reset completo (CUIDADO: apaga todos os dados)
docker compose down -v
```

### Serviços e Portas

| Serviço | Porta | Acesso |
|---------|-------|--------|
| Traefik (API gateway) | `:80` | `http://localhost/api/v1/...` |
| Traefik dashboard | `:8888` | `http://localhost:8888/dashboard/` |
| Monólito (direto) | `:8080` | `http://localhost:8080/api/v1/...` |
| User service (direto) | `:8081` | `http://localhost:8081/api/v1/...` |
| PostgreSQL (monólito) | `:5432` | `postgres/postgres` — db `scopeflow` |
| PostgreSQL (user-service) | `:5433` | `postgres/postgres` — db `scopeflow_users` |
| RabbitMQ | `:5672` / `:15672` | Management: `guest/guest` |
| Redis | `:6379` | — |

---

## Troubleshooting

| Problema | Diagnóstico | Solução |
|---------|-------------|---------|
| `user-service` crash loop | `docker logs scopeflow-user-service --tail=100` | Verificar `SPRING_DATASOURCE_URL`, rebuild se migration mudou |
| Tabela `users` não existe | `docker exec scopeflow-user-db psql -U postgres -d scopeflow_users -c "\dt"` | Rebuild da imagem user-service para incluir V1 migration |
| Traefik não roteia para user-service | `curl -s http://localhost:8888/api/http/routers` | Verificar labels no docker-compose, container healthcheck |
| JWT rejeitado cross-service | `docker inspect scopeflow-api` e `scopeflow-user-service` — comparar `JWT_SECRET` | Ambos devem usar o mesmo `JWT_SECRET` do `.env` |
| Flyway checksum mismatch | `./mvnw flyway:info` | Nunca modificar migration aplicada — criar nova versão |
| Testcontainers falha | `docker ps` | Docker deve estar rodando; memória mínima 4GB |

---

## Branching Strategy

- `main` — produção, protegida
- `develop` — staging, integração de features ← **branch ativa**
- `feature/` — novas features
- `bugfix/` — correções

---

## License

Proprietary — ScopeFlow AI
