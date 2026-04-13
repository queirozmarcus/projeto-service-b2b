# ScopeFlow AI

AI-powered SaaS platform that helps B2B service providers transform client conversations into clear, approved scopes through structured AI-assisted discovery.

**Branch:** `develop` | **Atualizado:** 2026-04-13

---

## Estado do Projeto

| Camada | Status |
|--------|--------|
| Backend — Monólito (Spring Boot 3.4.3 + Java 21) | ✅ ~85% |
| User Service (Strangler Fig — 20/20 sprints) | ✅ Staging ativo |
| Frontend (Next.js 15 + TypeScript) | ✅ ~70% |
| Produção | 🔄 Cut-over user-service pendente |

---

## Documentação

| Documento | Descrição |
|-----------|-----------|
| **[docs/README.md](docs/README.md)** | Índice central de toda a documentação técnica |
| **[docs/PROJECT-STATUS.md](docs/PROJECT-STATUS.md)** | Status detalhado: concluído, backlog, métricas |
| **[CLAUDE.md](CLAUDE.md)** | Guia para Claude Code: arquitetura, padrões, backlog técnico |
| **[docs/architecture/README.md](docs/architecture/README.md)** | Arquitetura hexagonal, ADRs, pontos de atenção |
| **[docs/migration/README.md](docs/migration/README.md)** | Strangler Fig: status das extrações, próximos passos |
| **[docs/api/BRIEFING-API-GUIDE.md](docs/api/BRIEFING-API-GUIDE.md)** | API do Briefing com exemplos |
| **[user-service/README.md](user-service/README.md)** | User Service: setup, endpoints, DB migration |
| **[user-service/docs/DB_MIGRATION_GUIDE.md](user-service/docs/DB_MIGRATION_GUIDE.md)** | Cut-over produção: pg_dump, restore, rollback |
| **[tests/e2e/README.md](tests/e2e/README.md)** | Testes E2E: auth flow, smoke tests |

---

## Quick Start

### Stack completa (Docker Compose)

```bash
cp .env.example .env          # configurar JWT_SECRET (mín. 32 chars)
docker compose up -d           # postgres, user-db, rabbitmq, redis, traefik, app, user-service
docker compose ps              # verificar saúde dos serviços
```

### Com DB-per-service (staging)

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d
```

### Desenvolvimento local

```bash
# Infraestrutura
docker compose up postgres user-db rabbitmq redis -d

# Monólito
cd backend && ./mvnw spring-boot:run      # :8080/api/v1

# User Service
cd user-service && ./mvnw spring-boot:run # :8081/api/v1

# Frontend
cd frontend && npm install && npm run dev  # :3000
```

---

## Serviços e Portas

| Serviço | Porta | Acesso |
|---------|-------|--------|
| Traefik (API gateway) | `:80` | `http://localhost/api/v1/...` |
| Traefik dashboard | `:8888` | `http://localhost:8888/dashboard/` |
| Monólito | `:8080` | direto: `http://localhost:8080/api/v1/...` |
| User Service | `:8081` | direto: `http://localhost:8081/api/v1/...` |
| Frontend | `:3000` | `http://localhost:3000` |
| PostgreSQL (monólito) | `:5432` | db `scopeflow` |
| PostgreSQL (user-service) | `:5433` | db `scopeflow_users` |
| RabbitMQ management | `:15672` | `guest/guest` |

---

## Tech Stack

| Camada | Tecnologia |
|--------|-----------|
| Frontend | Next.js 15 + React 19 + TypeScript + Zustand |
| Backend | Spring Boot 3.4.3 + Java 21 + Virtual Threads |
| User Service | Spring Boot 3.4.3 + Java 21 (microsserviço) |
| Database | PostgreSQL 16 + Flyway |
| Queue | RabbitMQ 3.13 |
| Cache | Redis 7 |
| API Gateway | Traefik v3.0 (Strangler Fig routing) |
| Autenticação | Spring Security 6 + JWT (shared secret) |
| Resiliência | Resilience4j (Circuit Breaker + Retry) |
| Testes | JUnit 5 + Testcontainers + Vitest |
| CI/CD | GitHub Actions |

---

## Testes

```bash
# Backend — monólito
cd backend && ./mvnw test          # unitários (sem Docker)
cd backend && ./mvnw verify        # + integração (Testcontainers)

# User Service
cd user-service && ./mvnw verify

# Frontend
cd frontend && npm run test

# E2E
./run-e2e-tests.sh                 # auth flow completo
./RUN-BRIEFING-TESTS.sh            # briefing flow completo
```

---

## Arquitetura

### Traefik Routing (Strangler Fig)

```
Traefik :80
├── PathPrefix(/api/v1/auth) || PathPrefix(/api/v1/users)  → user-service :8081  [prioridade 100]
└── PathPrefix(/api/)                                       → monólito :8080     [prioridade 50]
```

### Principais Padrões

- **Hexagonal (Ports & Adapters) + DDD** — domain puro, zero Spring/JPA no core
- **Outbox Pattern** — exactly-once delivery via `outbox_events` → RabbitMQ
- **Idempotency Keys** — endpoints públicos sem duplicatas (`Idempotency-Key` header)
- **Circuit Breakers** — user-service (CB+Retry) e SES (CB) via Resilience4j
- **RFC 9457 Problem Details** — todos os erros da API seguem o padrão
- **DB-per-service** — `scopeflow_users` dedicado para o user-service (staging ativo)

### ADRs

Ver [`docs/architecture/README.md`](docs/architecture/README.md) — 8 ADRs documentados (ADR-002 a ADR-009).

---

## Estrutura do Repositório

```
projeto-service-b2b/
├── backend/                    # Monólito Spring Boot 3.4.3
├── user-service/               # Microsserviço extraído (Strangler Fig)
│   └── docs/DB_MIGRATION_GUIDE.md   # Cut-over produção
├── frontend/                   # Next.js 15
├── docs/                       # Documentação técnica
│   ├── README.md               # Índice central
│   ├── PROJECT-STATUS.md       # Status atual + backlog
│   ├── architecture/           # ADRs, bounded contexts, coupling matrix
│   ├── migration/              # Strangler Fig: cards, ADRs, status
│   ├── database/               # Schema, migrations, índices
│   ├── api/                    # OpenAPI specs e guias
│   ├── qa/                     # Contract tests, cobertura
│   └── deployment/             # Guias de deploy
├── tests/e2e/                  # E2E e smoke tests
├── scripts/                    # Scripts utilitários
├── docker-compose.yml          # Stack completa (7 serviços)
├── docker-compose.staging.yml  # Override: DB-per-service
├── CLAUDE.md                   # Guia para Claude Code
└── GEMINI.md                   # Guia para Gemini CLI
```

---

## Erros — RFC 9457

```json
{
  "type": "https://api.scopeflow.com/errors/briefing-not-found",
  "title": "Briefing Not Found",
  "status": 404,
  "errorCode": "BRIEFING-001",
  "errorId": "550e8400-...",
  "timestamp": "2026-04-13T00:00:00Z"
}
```

Circuit breaker aberto → HTTP 503 com `Retry-After: 30`.

---

## Contribuição

- **main** — produção, protegida
- **develop** — staging, branch ativa
- Commits: Conventional Commits em PT-BR (`feat`, `fix`, `docs`, `refactor`, `chore`)

---

## License

Proprietary — ScopeFlow AI
