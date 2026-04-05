# ScopeFlow AI — B2B Service Discovery Platform

AI-powered SaaS platform that helps small B2B service providers (freelancers, microagencies) streamline their sales process. Transform confusing commercial conversations into clear, approved, and raise-ready scopes.

---

## Key Features

- **AI-Assisted Discovery:** Guide clients through structured questions, detect gaps, auto-generate follow-ups
- **Workspace Management:** Multi-tenant architecture with role-based access (Owner, Admin, Member)
- **Briefing Flow:** Sequential questions → Gap detection → Completion (80%+ score required)
- **Public Client Access:** Clients answer via public token (no auth required)
- **Immutable Audit Trail:** All answers and AI generations recorded for compliance
- **Scope Generation:** Completed briefings → Proposal context (async via RabbitMQ)

---

## Tech Stack

| Layer | Technology | Status |
|-------|-----------|--------|
| **Frontend** | Next.js 15 + React 19 + TypeScript | ✅ Created |
| **Backend** | Spring Boot 3.2 + Java 21 | ✅ Created |
| **Database** | PostgreSQL 16-Alpine + Flyway | ✅ Created |
| **Storage** | AWS S3 | 🔄 Planned |
| **Queue** | RabbitMQ 3.13-Alpine | ✅ Docker |
| **Authentication** | Spring Security 6.x + JWT | 🔄 In Progress |
| **PDF Generation** | iText 8.0.1 + Virtual Threads | 🔄 Planned |
| **LLM Integration** | OpenAI SDK for Java 0.18.0 | ✅ Dependency |
| **Observability** | Logback + SLF4J + Prometheus | ✅ Configured |
| **Build Tool** | Maven 3.8+ | ✅ Created |
| **Testing** | JUnit 5 + AssertJ + Testcontainers | ✅ Dependencies |
| **CI/CD** | GitHub Actions | ✅ Created |
| **Containerization** | Docker multi-stage | ✅ Created |
| **Infrastructure** | Docker Compose v3.9 | ✅ Created |

---

## Architecture

**Pattern:** Hexagonal (Ports & Adapters) with Domain-Driven Design (DDD)

**Core Domains:**
1. **User & Workspace** (Terminal 1) — ✅ Completed
2. **Briefing** (Terminal 2) — ✅ API Design Completed (Step 4/7)
3. **Proposal** (Terminal 3) — 🔄 Planned
4. **Client** (Terminal 4) — 🔄 Planned

**Key Principles:**
- **Sealed Classes:** Type-safe domain entities (Java 21)
- **Records:** Immutable DTOs and value objects
- **Virtual Threads:** Async I/O without thread pool limits
- **Outbox Pattern:** Event-driven communication (RabbitMQ via AMQP)
- **Testcontainers:** Real database for integration tests

---

## Briefing API Endpoints

The Briefing domain exposes **11 REST endpoints** for AI-assisted discovery flows:

### Authenticated Endpoints (Workspace owners/members)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/briefings` | Create new briefing session |
| `GET` | `/api/v1/briefings` | List briefings (paginated) |
| `GET` | `/api/v1/briefings/{id}` | Get briefing details |
| `GET` | `/api/v1/briefings/{id}/progress` | Get progress metrics (cached 30s) |
| `GET` | `/api/v1/briefings/{id}/next-question` | Get next sequential question |
| `POST` | `/api/v1/briefings/{id}/answers` | Submit answer |
| `POST` | `/api/v1/briefings/{id}/complete` | Mark briefing as completed |
| `POST` | `/api/v1/briefings/{id}/abandon` | Abandon briefing session |

**Authentication:** JWT Bearer token (100 req/min rate limit)

### Public Endpoints (Clients, no auth)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/public/briefings/{publicToken}` | Get public briefing by token |
| `GET` | `/public/briefings/{publicToken}/next-question` | Get next question (no auth) |
| `POST` | `/public/briefings/{publicToken}/answers` | Submit answer (no auth) |

**Rate Limit:** 10 req/min per IP

**Full API Documentation:** [`docs/api/BRIEFING-API-GUIDE.md`](docs/api/BRIEFING-API-GUIDE.md)
**OpenAPI Spec:** [`docs/api/briefing-api.yaml`](docs/api/briefing-api.yaml)

---

## Getting Started

### Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.8+** (or use embedded Maven wrapper)
- **Node.js LTS** (for frontend only)
- **PostgreSQL 14+** locally (or use Docker Compose)
- **Docker & Docker Compose v2**
- **OpenAI API key** (for AI integration)

### First-Time Setup

```bash
# Clone repository
git clone <repo>
cd projeto-service-b2b

# Setup environment
cp .env.example .env
# Update .env: DATABASE_URL, OPENAI_API_KEY, JWT_SECRET, etc.

# Start infrastructure (PostgreSQL + RabbitMQ)
docker compose up -d

# Backend: Run migrations
cd backend
./mvnw flyway:migrate

# Backend: Start dev server
./mvnw spring-boot:run  # http://localhost:8080/api/v1

# Frontend: Start dev server (separate terminal)
cd frontend
npm install
npm run dev  # http://localhost:3000
```

### Docker Compose (Infrastructure + Services)

#### Starting Services

```bash
# Start entire stack (PostgreSQL + RabbitMQ + Redis + Traefik + monolith + user-service)
docker compose up -d

# Start only infrastructure (useful for local Spring Boot dev)
docker compose up postgres rabbitmq redis traefik -d

# Start specific service
docker compose up user-service -d

# Check service health
docker compose ps
docker compose logs user-service -f

# Stop all services
docker compose down

# Stop and remove volumes (CAUTION: deletes all data)
docker compose down -v
```

#### User Service (Strangler Fig Extraction)

```bash
# Build user-service image
docker compose build user-service

# Start user-service (requires postgres)
docker compose up user-service -d

# Verify health (direct access)
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness

# Verify health (via Traefik routing)
curl http://localhost/api/v1/auth/health    # routed to user-service
curl http://localhost/api/v1/workspaces      # routed to monolith

# Check Traefik dashboard
open http://localhost:8888/dashboard/
# Or: curl http://localhost:8888/api/http/routers | jq

# View logs
docker compose logs user-service -f
docker compose logs traefik -f

# Restart service (e.g., after code changes)
docker compose restart user-service

# Rebuild and restart (after Dockerfile/pom.xml changes)
docker compose up user-service --build -d
```

#### Troubleshooting Docker Compose

| Issue | Diagnostic | Solution |
|-------|-----------|----------|
| **User-service won't start** | `docker compose logs user-service` | Check `DATABASE_URL` in `.env`, verify postgres is healthy (`docker compose ps`) |
| **Traefik routing not working** | `docker inspect scopeflow-user-service \| grep -A5 Labels` | Verify Traefik labels are applied, check priority (user-service=100, monolith=50) |
| **DB connection refused** | `docker compose exec postgres pg_isready` | Ensure postgres container is running and healthy |
| **JWT token rejected** | Compare `JWT_SECRET` in monolith vs user-service logs | Both services MUST use identical `JWT_SECRET` from `.env` |
| **Traefik dashboard 404** | `curl http://localhost:8888/api/http/routers` | Verify Traefik is running: `docker compose ps traefik` |
| **Port conflict (8080/8081)** | `lsof -i :8080` or `netstat -tulpn \| grep 8080` | Stop conflicting process or change `USER_SERVICE_PORT` in `.env` |
| **Container keeps restarting** | `docker compose logs user-service --tail=100` | Check for OOM, missing env vars, or health check failures |

#### Traefik Routing Verification

```bash
# List all routers (should show user-service priority=100, monolith priority=50)
curl -s http://localhost:8888/api/http/routers | jq '.[] | {name: .name, rule: .rule, priority: .priority, status: .status}'

# Expected output:
# {
#   "name": "user-service@docker",
#   "rule": "PathPrefix(`/api/v1/auth`)",
#   "priority": 100,
#   "status": "enabled"
# }
# {
#   "name": "monolith@docker",
#   "rule": "PathPrefix(`/api/`)",
#   "priority": 50,
#   "status": "enabled"
# }

# Test routing (should hit user-service)
curl -v http://localhost/api/v1/auth/health 2>&1 | grep -i "x-powered-by\|server"

# Test routing (should hit monolith)
curl -v http://localhost/api/v1/briefings 2>&1 | grep -i "x-powered-by\|server"
```

### Development Commands

#### Backend (Spring Boot 3.2 + Java 21)

```bash
cd backend

# Development
./mvnw spring-boot:run                     # Start dev server

# Build
./mvnw clean package                       # Create uber JAR

# Database Migrations
./mvnw flyway:migrate                      # Run pending migrations
./mvnw flyway:repair                       # Fix failed migrations

# Testing
./mvnw test                                # Unit tests
./mvnw verify                              # Unit + integration tests
./mvnw test -Dtest=BriefingServiceTest     # Single test class

# Quality & Coverage
./mvnw checkstyle:check                    # Code quality
./mvnw package jacoco:report               # Coverage report
```

#### Frontend (Next.js 15 + React 19)

```bash
cd frontend

# Development
npm run dev                                # Start dev server
npm run build                              # Production build
npm run lint                               # ESLint
npm run type-check                         # TypeScript strict mode
npm run test                               # Vitest unit/component tests
npm run test:e2e                           # Playwright E2E tests
```

---

## Error Handling

All APIs follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457).

**Example Error Response:**
```json
{
  "type": "https://api.scopeflow.com/errors/briefing-not-found",
  "title": "Briefing Not Found",
  "status": 404,
  "detail": "Briefing session with ID 7c9e6679... was not found",
  "instance": "/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "errorCode": "BRIEFING-001",
  "errorId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-22T14:30:00Z"
}
```

**Error Codes:**
- `BRIEFING-001`: Session not found (404)
- `BRIEFING-002`: Already completed (409)
- `BRIEFING-003`: Invalid answer (400)
- `BRIEFING-004`: Max follow-up exceeded (422)
- `BRIEFING-005`: Incomplete briefing (422)
- `BRIEFING-006`: Already in progress (409)
- `BRIEFING-007`: Invalid state (409)

---

## Testing

### E2E Smoke Tests (Authentication Flow)

**Quick start:**
```bash
# Run all E2E tests (both monolith and user-service modes)
./run-e2e-tests.sh

# Run only monolith mode
./run-e2e-tests.sh --monolith

# Run only user-service mode
./run-e2e-tests.sh --user-service
```

**Test coverage:**
- ✅ User registration → JWT generation
- ✅ Login with valid/invalid credentials
- ✅ Protected endpoint access (/auth/me, /workspaces)
- ✅ JWT validation (format, claims, expiration)
- ✅ Cross-service JWT compatibility (user-service → monolith)
- ✅ RFC 9457 error responses

**CI/CD:** Tests run automatically on GitHub Actions for all pushes/PRs. Deployment is blocked if any test fails.

**Documentation:** Full E2E test guide available in [`tests/e2e/README.md`](tests/e2e/README.md)

---

### Integration Tests (Testcontainers)

```bash
./mvnw verify  # Run all integration tests with real PostgreSQL
```

---

### Swagger UI (OpenAPI)

```bash
./mvnw spring-boot:run
# Open: http://localhost:8080/swagger-ui.html
```

---

### Manual API Testing

```bash
# Get JWT token first (from /api/v1/auth/login)
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Create briefing
curl -X POST http://localhost:8080/api/v1/briefings \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"550e8400-e29b-41d4-a716-446655440000","serviceType":"SOCIAL_MEDIA"}'
```

---

## Project Structure

```
projeto-service-b2b/
├── backend/                         # Spring Boot 3.2 + Java 21
│   ├── src/main/java/com/scopeflow/
│   │   ├── core/                    # Domain layer (no Spring)
│   │   │   └── domain/
│   │   │       ├── briefing/        # Briefing domain (sealed classes, records)
│   │   │       ├── workspace/       # Workspace domain
│   │   │       └── user/            # User domain
│   │   ├── adapter/                 # Adapter layer (Spring Boot)
│   │   │   ├── in/
│   │   │   │   └── web/             # REST controllers
│   │   │   │       ├── briefing/    # BriefingControllerV1, PublicBriefingControllerV1
│   │   │   │       │   ├── dto/     # Request/Response DTOs (records)
│   │   │   │       │   └── mapper/  # Domain ↔ DTO mappers
│   │   │   │       └── GlobalExceptionHandler.java
│   │   │   └── out/
│   │   │       ├── persistence/     # JPA entities + repositories
│   │   │       └── messaging/       # RabbitMQ producers/consumers (AMQP)
│   │   └── config/                  # Spring configuration
│   └── src/main/resources/
│       └── db/migration/            # Flyway migrations
│           ├── V1__initial_schema.sql
│           ├── V2__user_workspace_schema.sql
│           └── V3__briefing_domain_schema.sql
├── frontend/                        # Next.js 15 + React 19 + TypeScript
├── docs/
│   ├── api/
│   │   ├── briefing-api.yaml        # OpenAPI 3.1 spec (Step 4 deliverable)
│   │   └── BRIEFING-API-GUIDE.md    # Full API documentation
│   └── architecture/
│       └── adr/
│           ├── ADR-001-user-workspace-service.md
│           └── ADR-002-briefing-domain.md
├── docker-compose.yml               # PostgreSQL + RabbitMQ + Redis
└── README.md                        # This file
```

---

## Contributing

**Branching Strategy:**
- **main**: production-ready, always deployable
- **develop**: staging, feature integration
- **feature/**: `feature/briefing-ai`, `feature/approval-flow`
- **bugfix/**: `bugfix/proposal-rendering`

**Commit Message Format:**
```
feat(briefing): adiciona aprofundamento automático de respostas vagas

Implementa lógica de IA para detectar respostas incompletas
e gerar perguntas complementares. Reduz ambiguidade no briefing.

Closes #42
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `chore`, `ci`

---

## License

Proprietary — ScopeFlow AI

---

## Contact

**Product:** scopeflow@example.com
**API Support:** api@scopeflow.com
**Documentation:** https://docs.scopeflow.com
