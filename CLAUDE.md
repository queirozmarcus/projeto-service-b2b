# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ScopeFlow AI** — AI-powered SaaS platform for B2B service providers (freelancers, microagencies) to transform client conversations into clear, approved scopes through structured AI-assisted discovery.

**Current Status:** Core briefing flow implemented and tested. User/Workspace domains complete. API design finalized but not all controllers implemented yet.

See [`README.md`](README.md) for tech stack, setup instructions, and API documentation.

---

## Architecture: Hexagonal (Ports & Adapters) + DDD

### Package Structure

```
backend/src/main/java/com/scopeflow/
├── core/domain/              # Pure domain logic (zero Spring dependencies)
│   ├── briefing/             # Briefing aggregate with sealed classes
│   ├── workspace/            # Workspace aggregate
│   └── user/                 # User aggregate
├── application/              # Application services (orchestration)
│   ├── port/out/             # Output ports (interfaces)
│   ├── idempotency/          # Idempotency service
│   ├── outbox/               # Outbox pattern for events
│   └── listener/             # Domain event listeners
├── adapter/
│   ├── in/web/               # REST controllers (Spring MVC)
│   │   └── briefing/
│   │       ├── dto/          # Request/Response records
│   │       └── mapper/       # Domain ↔ DTO mapping
│   └── out/
│       ├── persistence/      # JPA entities + repositories
│       ├── pdf/              # PDF generation adapter
│       └── email/            # Email service adapter
└── config/                   # Spring configuration
```

**Key Principle:** Domain layer (`core/domain/`) has **zero dependencies** on Spring, JPA, or external frameworks. All persistence, messaging, and external integrations happen through adapters.

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

3. **Aggregate Roots**: `BriefingSession`, `Workspace`, `User` — always modified through their public methods, never directly

4. **Domain Events**: Published via `@DomainEvents` + `@AfterDomainEventPublication`
   ```java
   public class BriefingSession {
       @DomainEvents
       Collection<Object> domainEvents() { return events; }
       
       @AfterDomainEventPublication
       void clearEvents() { events.clear(); }
   }
   ```

---

## Key Architectural Decisions

### 1. Why Java 21 over Java 8/11/17?

- **Virtual Threads**: Handle 1000+ concurrent HTTP requests without thread pool tuning
- **Sealed Classes**: Type-safe state machines (e.g., `BriefingSession` states)
- **Records**: Zero-boilerplate immutable DTOs
- **Pattern Matching**: Cleaner domain logic (`switch` on sealed types)
- **LTS Support**: Until September 2031

### 2. Outbox Pattern for Events

All domain events go through the **outbox table** before RabbitMQ to guarantee exactly-once delivery.

**Flow:**
1. Domain event saved in `outbox_events` table (same transaction as business data)
2. Background job polls outbox → publishes to RabbitMQ
3. On success: delete from outbox
4. On failure: retry with exponential backoff

**Implementation:** See `OutboxService`, `OutboxEventPublisher`, and migrations `V5__outbox_event_schema.sql`

### 3. Idempotency for Public Endpoints

Public endpoints (client-facing, no auth) use **idempotency keys** to prevent duplicate submissions.

**How it works:**
- Client sends `Idempotency-Key` header (UUID recommended)
- First request: process + cache response for 24h
- Duplicate requests: return cached response (409 Conflict if processing)

**Implementation:** See `IdempotencyService`, `IdempotencyRepository`

### 4. Multi-Tenancy via Workspace Scoping

All data is **workspace-scoped**. Every query must filter by `workspace_id`.

**Enforcement:**
- Spring Security: JWT contains `workspaceId` claim
- Repository methods: always include `workspaceId` parameter
- Database indexes: compound indexes on `(workspace_id, id)`

**Example:**
```java
// ❌ WRONG: Missing workspace filter
List<Proposal> findAll();

// ✅ CORRECT: Workspace-scoped
List<Proposal> findByWorkspaceId(UUID workspaceId);
```

### 5. RFC 9457 Problem Details for All Errors

All API errors return **Problem Details** JSON with:
- `type`: URL to error documentation
- `title`: Human-readable error summary
- `status`: HTTP status code
- `detail`: Specific error message
- `errorCode`: Stable error code (e.g., `BRIEFING-001`)
- `errorId`: Unique trace ID for debugging
- `timestamp`: ISO 8601 timestamp

**Implementation:** See `GlobalExceptionHandler` and README error codes section.

---

## Development Workflow

### Running Tests

```bash
# Unit tests only (fast, no database)
./mvnw test

# Integration tests (with Testcontainers — starts real PostgreSQL)
./mvnw verify

# Single test class
./mvnw test -Dtest=WorkspaceTest

# E2E flow tests (requires running app)
./RUN-BRIEFING-TESTS.sh          # Full briefing flow
./TEST-BRIEFING-SESSION.sh       # Specific session test
./scripts/smoke-tests.sh         # Health checks

# Coverage report
./mvnw package jacoco:report
# Open: target/site/jacoco/index.html
```

### Database Migrations

**Never modify applied migrations** — Flyway will fail. Always create a new migration.

```bash
# Run pending migrations
./mvnw flyway:migrate

# Check migration status
./mvnw flyway:info

# Repair failed migration (only if safe)
./mvnw flyway:repair

# Create new migration
# 1. Add file: backend/src/main/resources/db/migration/V10__your_description.sql
# 2. Run: ./mvnw flyway:migrate
```

**Naming:** `V{n}__{description}.sql` — double underscore after version!

### Testing Strategy

1. **Unit Tests** (`*Test.java`): Domain logic, no Spring, no database
   - Use AssertJ for fluent assertions
   - Given-When-Then structure
   - Example: `WorkspaceTest`, `BriefingSessionTest`

2. **Integration Tests** (`*IntegrationTest.java`): With Testcontainers
   - Real PostgreSQL via Docker
   - `@SpringBootTest` + `@Testcontainers`
   - Example: `BriefingControllerV1IntegrationTest`

3. **E2E Tests** (Bash scripts): Full flow from API
   - `RUN-BRIEFING-TESTS.sh`: User registration → briefing creation → answers → completion
   - Validates HTTP status codes, response bodies, state transitions

**Coverage Targets:**
- Domain logic: 100% (critical business rules)
- Services: 90%+
- Controllers: 80%+ (integration tests)
- Adapters: 70%+

### Working with Domain Aggregates

**Golden Rule:** Always modify aggregates through their **public methods**, never by directly setting fields.

**Example: BriefingSession**

```java
// ❌ WRONG: Bypassing domain logic
briefingSession.status = BriefingStatus.COMPLETED;
briefingSession.completedAt = Instant.now();

// ✅ CORRECT: Using aggregate method
briefingSession.complete();  // Validates 80%+ score, sets timestamp, publishes event
```

**Why?** Aggregate methods enforce invariants, publish domain events, and ensure consistency.

### Adding a New Domain Entity

1. **Create domain class** in `core/domain/{aggregate}/`
   - Use sealed classes for state variants
   - Use records for value objects
   - Zero Spring/JPA dependencies

2. **Define repository interface** in `core/domain/{aggregate}/`
   ```java
   public interface BriefingSessionRepository {
       void save(BriefingSession session);
       Optional<BriefingSession> findById(BriefingSessionId id);
   }
   ```

3. **Create JPA entity** in `adapter/out/persistence/{aggregate}/`
   - Implement the repository interface
   - Map domain → JPA (constructor mapping)

4. **Create mapper** if complex
   - Domain ↔ JPA conversion logic
   - Keep mappers in adapter layer

5. **Write tests**
   - Unit test: domain logic
   - Integration test: repository adapter with Testcontainers

### Adding a New API Endpoint

1. **Design OpenAPI spec first** in `docs/api/{domain}-api.yaml`
   - Define request/response schemas
   - Document error responses (RFC 9457)

2. **Create DTOs** in `adapter/in/web/{domain}/dto/`
   - Use records for immutability
   - Add Jakarta validation annotations

3. **Create controller** in `adapter/in/web/{domain}/`
   - Inject application service
   - Map DTOs ↔ domain objects
   - Return `ResponseEntity<?>` with proper status codes

4. **Write integration tests**
   - `@SpringBootTest` + `@AutoConfigureMockMvc`
   - Test happy path + error cases
   - Verify RFC 9457 error format

5. **Update Swagger UI**
   - Restart app: `./mvnw spring-boot:run`
   - Open: http://localhost:8080/swagger-ui.html

---

## Code Style & Conventions

### Naming

| Type | Convention | Example |
|------|-----------|---------|
| Domain entities | PascalCase, descriptive | `BriefingSession`, `Workspace` |
| Value objects | PascalCase + type suffix | `AnswerText`, `PublicToken` |
| DTOs | PascalCase + Request/Response | `CreateBriefingRequest`, `BriefingResponse` |
| Services | PascalCase + Service | `BriefingService`, `WorkspaceService` |
| Repositories | PascalCase + Repository | `BriefingSessionRepository` |
| Exceptions | PascalCase + Exception | `BriefingNotFoundException` |
| Error codes | DOMAIN-NNN | `BRIEFING-001`, `WORKSPACE-005` |

### Domain Exceptions

All domain exceptions must:
1. Extend `RuntimeException` (or custom domain exception base)
2. Include stable error code: `DOMAIN-NNN`
3. Have clear message for users

**Example:**
```java
public class BriefingAlreadyCompletedException extends BriefingDomainException {
    private static final String ERROR_CODE = "BRIEFING-002";
    
    public BriefingAlreadyCompletedException(BriefingSessionId id) {
        super(ERROR_CODE, "Briefing %s is already completed".formatted(id.value()));
    }
}
```

### Testing Conventions

**Test method naming:**
```java
// Pattern: should{ExpectedBehavior}_when{Condition}
@Test
void shouldThrowException_whenAnswerIsBlank() { ... }

@Test
void shouldCalculateProgress_whenMultipleAnswersExist() { ... }
```

**Structure:**
```java
@Test
void testName() {
    // Given (arrange)
    var session = new BriefingSession(...);
    
    // When (act)
    session.complete();
    
    // Then (assert)
    assertThat(session.isCompleted()).isTrue();
}
```

---

## Common Tasks & Patterns

### Task: Add a New Briefing Question Type

1. Update `ServiceType` enum in `core/domain/briefing/ServiceType.java`
2. Add migration: `V{n}__add_{service}_questions.sql`
3. Insert seed data for questions in migration
4. Update `ServiceContextQuestion` mapping if needed
5. Test with E2E script: `./TEST-BRIEFING-SESSION.sh`

### Task: Add a New Domain Event

1. Create event record in `core/domain/{aggregate}/`
   ```java
   public record BriefingCompletedEvent(
       BriefingSessionId sessionId,
       UUID workspaceId,
       Instant completedAt
   ) {}
   ```

2. Publish from aggregate:
   ```java
   public void complete() {
       // ... validation
       this.events.add(new BriefingCompletedEvent(id, workspaceId, Instant.now()));
   }
   ```

3. Create listener in `application/listener/`
   ```java
   @Component
   public class BriefingCompletedListener {
       @TransactionalEventListener
       public void onBriefingCompleted(BriefingCompletedEvent event) {
           // Handle event (save to outbox, trigger external call, etc.)
       }
   }
   ```

4. Test with integration test: verify event is published and handled

### Task: Debug Outbox Events Not Publishing

1. Check `outbox_events` table: `SELECT * FROM outbox_events WHERE published_at IS NULL;`
2. Check logs: search for `OutboxEventPublisher`
3. Verify RabbitMQ is running: `docker ps | grep rabbitmq`
4. Check RabbitMQ management UI: http://localhost:15672 (guest/guest)
5. Manually trigger publish: restart app or call republish endpoint

### Task: Test with Real OpenAI API

1. Set `OPENAI_API_KEY` in `.env`
2. Ensure `application-local.yml` doesn't mock OpenAI client
3. Run briefing flow: `./RUN-BRIEFING-TESTS.sh`
4. Check `ai_generations` table for prompt/response audit trail

---

## Troubleshooting

### Issue: Flyway migration fails with checksum mismatch

**Cause:** Migration file was modified after being applied.

**Fix:**
```bash
# 1. Revert the file to original content
git checkout HEAD~1 -- backend/src/main/resources/db/migration/V{n}__file.sql

# 2. Create a new migration with your changes
# backend/src/main/resources/db/migration/V{n+1}__your_fix.sql

# 3. Run migrations
./mvnw flyway:migrate
```

### Issue: Tests fail with "Container startup failed"

**Cause:** Docker not running or insufficient resources.

**Fix:**
1. Verify Docker is running: `docker ps`
2. Increase Docker memory: Docker Desktop → Settings → Resources → Memory (minimum 4GB)
3. Clean up containers: `docker system prune -a`

### Issue: JWT token expired during testing

**Cause:** Token TTL is 15 minutes by default.

**Fix:**
```bash
# Get fresh token before each test run
export JWT_TOKEN=$(./scripts/get-test-token.sh)
```

Or increase TTL in `.env` (development only):
```
JWT_EXPIRATION_MS=3600000  # 1 hour
```

### Issue: Integration tests fail with "Port 5432 already in use"

**Cause:** Testcontainers tries to use mapped port but PostgreSQL is already running locally.

**Fix:** Testcontainers uses random ports automatically. This usually means a leftover container is running.

```bash
docker ps -a | grep testcontainers
docker rm -f $(docker ps -aq --filter "label=org.testcontainers")
```

---

## Key Files & Documentation

| File | Purpose |
|------|---------|
| [`README.md`](README.md) | Setup instructions, tech stack, API endpoints |
| [`docs/api/BRIEFING-API-GUIDE.md`](docs/api/BRIEFING-API-GUIDE.md) | Detailed API documentation with examples |
| [`docs/api/briefing-api.yaml`](docs/api/briefing-api.yaml) | OpenAPI 3.1 specification |
| [`docs/architecture/adr/`](docs/architecture/adr/) | Architecture Decision Records (ADRs) |
| [`RUN-BRIEFING-TESTS.sh`](RUN-BRIEFING-TESTS.sh) | E2E test suite for complete briefing flow |
| [`backend/src/main/resources/db/migration/`](backend/src/main/resources/db/migration/) | Flyway migrations (V1–V9 applied) |
| [`docker-compose.yml`](docker-compose.yml) | PostgreSQL + RabbitMQ + Redis services |

---

## Environment Variables

See [`.env.example`](.env.example) for all required variables.

**Critical for local development:**
- `DATABASE_URL` — PostgreSQL connection string
- `JWT_SECRET` — Minimum 32 characters (generate: `openssl rand -hex 32`)
- `OPENAI_API_KEY` — For AI integration (optional for local dev if mocked)

**Optional:**
- `SPRING_PROFILES_ACTIVE` — `dev`, `local`, `test`, `prod`
- `RABBITMQ_HOST` — Defaults to `localhost`

---

## Product Context

For deep product understanding, see [`scopeflow_ai_documento_master_completo.md`](scopeflow_ai_documento_master_completo.md) (2045 lines) — covers:
- Full product spec
- User personas
- Wireframes
- Pricing strategy
- Roadmap (MVP → validation → expansion)

**Note:** That document is the product vision. This CLAUDE.md focuses on the **current implementation**.
