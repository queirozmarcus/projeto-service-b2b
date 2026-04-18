# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ScopeFlow AI** — AI-powered SaaS platform for B2B service providers (freelancers, microagencies) to transform client conversations into clear, approved scopes through structured AI-assisted discovery.

**Current Status (2026-04-17):**
- Backend monolith: ~85% — todos os domínios implementados, circuit breakers ativos, purge jobs
- User Service: 100% extraído via Strangler Fig — DB-per-service consolidado e validado ✅
- Frontend: ~70% — dashboard, proposals e briefings integrados com API real
- Tests: ✅ Sprint 10 — 50 testes unitários criados, integration tests refatorados
- Docker Stack: ✅ 7/7 serviços operacionais — validação completa executada
- Ambiente: ✅ Pronto para desenvolvimento (DB-per-service isolado, Traefik routing OK)

See [`README.md`](README.md) for tech stack, setup instructions, and full API documentation.

---

## Architecture: Hexagonal (Ports & Adapters) + DDD

### Monolith Package Structure

```
backend/src/main/java/com/scopeflow/
├── core/domain/              # Pure domain logic (zero Spring dependencies)
│   ├── briefing/             # Briefing aggregate (sealed classes, question flow)
│   ├── workspace/            # Workspace aggregate
│   ├── user/                 # User domain (ServiceUnavailableException USER-012)
│   └── client/               # Client aggregate
├── application/              # Application services (orchestration)
│   ├── port/out/             # Output ports (interfaces)
│   ├── idempotency/          # IdempotencyService + Repository
│   ├── outbox/               # OutboxService + OutboxEventPublisher
│   ├── purge/                # PurgeJobService (3 scheduled jobs)
│   └── listener/             # Domain event listeners
├── adapter/
│   ├── in/web/               # REST controllers (Spring MVC)
│   │   ├── auth/             # AuthControllerV2 — proxy para user-service (fallback sem Traefik)
│   │   ├── briefing/         # BriefingControllerV1, BriefingSessionControllerV2, PublicBriefingControllerV1
│   │   ├── proposal/         # ProposalControllerV2
│   │   ├── workspace/        # WorkspaceControllerV2 (invite via user-service)
│   │   ├── user/             # UserController
│   │   └── GlobalExceptionHandler.java
│   └── out/
│       ├── persistence/      # JPA entities + repositories
│       ├── email/            # AwsSesEmailServiceAdapter (@CircuitBreaker)
│       ├── pdf/              # ITextPdfServiceAdapter (stub — Phase 4)
│       └── userservice/      # UserServiceRestAdapter (@CircuitBreaker + @Retry)
└── config/                   # Spring, Security, Resilience4j configs
```

**Key Principle:** Domain layer (`core/domain/`) has **zero dependencies** on Spring, JPA, or external frameworks.

### User Service Package Structure

```
user-service/src/main/java/com/scopeflow/user/
├── domain/          # User aggregate (Email, PasswordHash, UserId records)
├── application/     # RegisterUser, AuthenticateUser, GetProfile, etc.
├── adapter/
│   ├── in/web/      # AuthController, UserController
│   └── out/         # JpaUserRepository
└── config/          # SecurityConfig, JwtService, CORS
```

### Domain Model Design Patterns

1. **Sealed Classes** (Java 21): Type-safe state modeling
   ```java
   sealed interface BriefingSession
       permits BriefingInProgress, BriefingCompleted, BriefingAbandoned {}
   ```

2. **Records**: Immutable value objects and DTOs
   ```java
   record AnswerText(String value) {
       public AnswerText {
           if (value == null || value.isBlank())
               throw new IllegalArgumentException("Answer cannot be blank");
       }
   }
   ```

3. **Aggregate Roots**: `BriefingSession`, `Workspace`, `User` — always modified through their public methods

4. **Domain Events**: Published via `@DomainEvents` + `@AfterDomainEventPublication`

---

## Key Architectural Decisions

### 1. Strangler Fig — User Service Extracted

User Service foi extraído do monólito via Strangler Fig. Traefik roteia `/api/v1/auth/*` e `/api/v1/users/*` para o user-service (prioridade 100), resto para o monólito (prioridade 50).

- **JWT secret compartilhado**: ambos os serviços usam o mesmo `JWT_SECRET` — tokens são cross-compatible
- **DB-per-service**: `scopeflow_users` (PostgreSQL dedicado, porta 5433) — ativo em staging
- **Módulo User no monólito**: decommissioned — `ServiceUnavailableException` protege chamadas residuais
- **AuthControllerV2 (proxy)**: o monólito mantém `AuthControllerV2` que faz proxy das requisições `/auth/*` para o user-service via `RestTemplate`. Funciona como fallback quando Traefik não está presente (dev local sem docker compose). Em staging/produção, Traefik intercepta antes do monólito (prioridade 100 > 50).

### 2. Circuit Breakers (Resilience4j)

Dois circuit breakers protegem chamadas a serviços externos:

| Instância | Adapter protegido | Retry? |
|-----------|------------------|--------|
| `user-service` | `UserServiceRestAdapter` | Sim (2x, 200ms) |
| `ses` | `AwsSesEmailServiceAdapter` | Não (falhas de quota/auth não são transientes) |

`CallNotPermittedException` → HTTP 503 com `Retry-After: 30`. Handler usa `ex.getCausingCircuitBreakerName()` (genérico — cobre futuros CBs automaticamente).

### 3. Outbox Pattern for Events

All domain events go through the **outbox table** before RabbitMQ to guarantee exactly-once delivery.

**Flow:**
1. Domain event saved in `outbox_events` (same transaction as business data)
2. `OutboxEventPublisher` scheduler polls → publishes to RabbitMQ
3. Success: mark `published_at`; Failure: retry with exponential backoff
4. `PurgeJobService` limpa eventos publicados após 7 dias (02:00 UTC)

### 4. Idempotency for Public Endpoints

Public endpoints use `Idempotency-Key` header to prevent duplicate submissions. Cache de 24h por chave. Registros limpos após 30 dias pelo `PurgeJobService` (02:15 UTC).

### 5. Multi-Tenancy via Workspace Scoping

All data is **workspace-scoped**. Every query must filter by `workspace_id`.

```java
// ❌ WRONG
List<Proposal> findAll();

// ✅ CORRECT
List<Proposal> findByWorkspaceId(UUID workspaceId);
```

### 6. RFC 9457 Problem Details for All Errors

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

**Implementation:** `GlobalExceptionHandler` — cobre domain exceptions, circuit breaker, rate limit, JWT, ServiceUnavailable.

### 7. Purge Jobs

`PurgeJobService` (@Scheduled, 3 jobs):
- `02:00 UTC` — outbox (7 dias, somente `publishedAt IS NOT NULL`)
- `02:15 UTC` — idempotency (30 dias)
- `02:30 UTC` — ai_generations (90 dias)

Controlados via `app.purge.enabled` (default: true). Todos os valores de retenção configuráveis via env vars.

---

## IMPORTANTE: Regra de Comandos

**Nunca execute builds ou testes automaticamente.** Sempre forneça o comando para o usuário rodar manualmente:

```
# ✅ Correto: fornecer o comando
"Para validar, rode: ./mvnw test"

# ❌ Errado: executar diretamente
Bash("./mvnw test")
```

---

## Development Workflow

### Running Tests

```bash
# Backend — unit tests only (fast, no Docker)
cd backend && ./mvnw test

# Backend — unit + integration (Testcontainers — requires Docker)
cd backend && ./mvnw verify

# Single test class
./mvnw test -Dtest=PurgeJobServiceTest

# User service tests
cd user-service && ./mvnw verify

# Frontend
cd frontend && npm run test

# Coverage report
cd backend && ./mvnw package jacoco:report
# Open: backend/target/site/jacoco/index.html
```

### Database Migrations

**Never modify applied migrations** — Flyway will fail. Always create a new file.

```bash
# Next migration: V10__your_description.sql
# Run: cd backend && ./mvnw flyway:migrate
# Check: cd backend && ./mvnw flyway:info
```

### Adding a New Domain Entity

1. Create domain class in `core/domain/{aggregate}/` (zero framework dependencies)
2. Define repository interface in `core/domain/{aggregate}/`
3. Create JPA entity in `adapter/out/persistence/{aggregate}/`
4. Write unit test (domain logic) + integration test (Testcontainers)

### Adding a New API Endpoint

1. Design OpenAPI spec in `docs/api/{domain}-api.yaml`
2. Create DTOs (records) in `adapter/in/web/{domain}/dto/`
3. Create/update controller in `adapter/in/web/{domain}/`
4. Write integration test (`@SpringBootTest + @AutoConfigureMockMvc`)
5. Handle errors via `GlobalExceptionHandler` (RFC 9457)

---

## Known Issues / Backlog

| Item | Severidade | Contexto |
|------|-----------|---------|
| Circuit breaker OpenAI / S3 | Baixa | `ITextPdfServiceAdapter` tem TODOs completos com config sugerida; aguarda Phase 4 (adapters não existem) |
| User service cut-over em produção | Alta | Plano em `.claude/plans/backlog/2026-04-12-migration-fase3-cutover-producao.md` |
| Extração Workspace context | Backlog | Próximo bounded context — após user-service estável em prod |

---

## Code Style & Conventions

### Naming

| Type | Convention | Example |
|------|-----------|---------|
| Domain entities | PascalCase | `BriefingSession`, `Workspace` |
| Value objects | PascalCase + type suffix | `AnswerText`, `PublicToken` |
| DTOs | PascalCase + Request/Response | `CreateBriefingRequest`, `BriefingResponse` |
| Services | PascalCase + Service | `BriefingService`, `WorkspaceService` |
| Exceptions | PascalCase + Exception | `BriefingNotFoundException` |
| Error codes | DOMAIN-NNN | `BRIEFING-001`, `USER-012` |

### Domain Exceptions

```java
public class BriefingAlreadyCompletedException extends BriefingDomainException {
    private static final String ERROR_CODE = "BRIEFING-002";

    public BriefingAlreadyCompletedException(BriefingSessionId id) {
        super(ERROR_CODE, "Briefing %s is already completed".formatted(id.value()));
    }
}
```

### Testing Conventions

```java
// Naming: should{ExpectedBehavior}_when{Condition}
@Test
void shouldThrowException_whenAnswerIsBlank() {
    // Given
    var session = new BriefingSession(...);
    // When / Then
    assertThatThrownBy(() -> session.addAnswer(blank))
        .isInstanceOf(InvalidAnswerException.class);
}
```

---

## Common Tasks & Patterns

### Adicionar Circuit Breaker a um novo Adapter

```java
@CircuitBreaker(name = "my-service", fallbackMethod = "myMethodFallback")
@Retry(name = "my-service")  // apenas para erros transientes (rede, timeout)
public Result myMethod(Input input) { ... }

private Result myMethodFallback(Input input, Throwable ex) {
    throw new ServiceUnavailableException("my-service", ex);
}
```

Adicionar config em `application.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      my-service:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

### Debug Outbox Events Not Publishing

```sql
SELECT * FROM outbox_events WHERE published_at IS NULL;
```

Verificar logs: `OutboxEventPublisher`. RabbitMQ management: http://localhost:15672 (guest/guest).

### Trabalhar com o User Service

```bash
# Rebuild após alterações em código/migrations
docker compose build user-service
docker compose up -d user-service

# Verificar migration aplicada
docker exec scopeflow-user-db psql -U postgres -d scopeflow_users -c "SELECT * FROM flyway_schema_history;"

# Logs em tempo real
docker logs scopeflow-user-service -f
```

---

## Troubleshooting

| Issue | Diagnóstico | Solução |
|-------|-------------|---------|
| `user-service` crash: `missing table [users]` | Migration V1 não está na imagem | Rebuild: `docker compose build user-service` |
| JWT rejeitado cross-service | `docker inspect scopeflow-api` vs `scopeflow-user-service` — comparar `JWT_SECRET` | Ambos devem ter o mesmo secret |
| Flyway checksum mismatch | `./mvnw flyway:info` | Nunca modificar migration aplicada — criar V{n+1} |
| Testcontainers falha | `docker ps` | Docker rodando + mínimo 4GB RAM |
| Circuit breaker aberto (503) | Logs do serviço + `Retry-After` header | Aguardar 30s ou reiniciar serviço dependente |

---

## Key Files Reference

| File | Propósito |
|------|-----------|
| `README.md` | Setup, stack, API endpoints, Docker commands |
| `docker-compose.yml` | Stack completa (7 serviços) |
| `docker-compose.staging.yml` | Override: user-service → DB dedicado |
| `backend/src/main/resources/application.yml` | Config base + Resilience4j + Purge |
| `docs/migration/DB-MIGRATION-USER-SERVICE.md` | Passos de cut-over e rollback para produção |
| `backend/src/main/resources/db/migration/` | Flyway V1–V9 (monólito) |
| `user-service/src/main/resources/db/migration/` | Flyway V1 (user-service) |
| `.claude/plans/backlog/` | Planos aprovados aguardando execução |
| `.claude/plans/concluido/` | Histórico de planos executados |
| `docs/architecture/adr/` | Architecture Decision Records |

---

## Product Context

For deep product understanding, see [`docs/scopeflow_ai_documento_master_completo.md`](docs/scopeflow_ai_documento_master_completo.md) (2045 lines) — product spec, personas, wireframes, pricing, roadmap.

**Note:** That document is the product vision. This CLAUDE.md reflects the **current implementation state**.
