# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ScopeFlow AI** is an AI-powered SaaS platform that helps small B2B service providers (freelancers, microagencies) streamline their sales process. The core mission: transform a confusing commercial conversation into a clear, approved, and raiseready scope using structured AI-assisted discovery, briefing consolidation, and proposal generation.

### Key Problem Solved
Small service providers (social media, design, landing pages, web) typically:
- Conduct sales on WhatsApp with scattered audio notes
- Use generic PDFs without clear scope
- Have misaligned expectations → rework
- Lack trackable approvals

ScopeFlow fixes this by guiding both service provider and client through a structured flow where IA adapts questions, consolidates briefing, suggests scope, and enables clear approval.

### Target Audience
- Freelancers and microagencies (marketing, design, social media, landing pages, web)
- Expansion: consultants, small dev shops, video producers
- Geographic: initially Brazil, then Latin America

---

## Architecture & Tech Stack

### Architectural Principles
- **Monolith modular approach** for MVP (no microservices yet)
- **IA as capability**, not separate system — integrated into product workflows
- **Strong versioning** of all artifacts (briefing, scope, proposals)
- **Human review obligatory** — IA assists but never auto-approves
- **Low operational cost** initially — S3 for storage, Redis for async, PostgreSQL for data

### Implemented Stack (MVP) — Spring Boot 3.2 + Java 21 ✅
| Layer | Technology | Status | Notes |
|-------|-----------|--------|-------|
| **Frontend** | Next.js 15 + React 19 + TypeScript | ✅ Created | Tailwind v4, Radix UI, SWC minification |
| **Backend** | Spring Boot 3.2 + Java 21 | ✅ Created | Virtual threads, sealed classes, records, LTS until 2031 |
| **Database** | PostgreSQL 16-Alpine + Flyway | ✅ Created | JSONB support, 14 tables, audit trails, migrations |
| **Storage** | AWS S3 (AWS SDK for Java) | 🔄 Planned | PDFs, logos, presigned URLs |
| **Queue** | RabbitMQ 3.13-Alpine | ✅ Docker | Spring Integration for async: PDF, email, IA calls |
| **Authentication** | Spring Security 6.x + JWT | 🔄 In AuthController | Workspace-scoped, RBAC, stateless |
| **PDF Generation** | iText 8.0.1 + Virtual Threads | 🔄 Planned | Async processing, no blocking |
| **LLM Integration** | OpenAI SDK for Java 0.18.0 | ✅ Dependency | Prompts versioned, structured JSON outputs |
| **Observability** | Logback + SLF4J + Prometheus | ✅ Configured | Management endpoints on /actuator |
| **Build Tool** | Maven 3.8+ | ✅ Created | Spring Boot parent 3.2.0, multi-module ready |
| **Testing** | JUnit 5 + AssertJ + Testcontainers | ✅ Dependencies | Unit, integration, E2E via REST |
| **CI/CD** | GitHub Actions | ✅ Created | Backend CI, Frontend CI, Deploy workflows |
| **Containerization** | Docker multi-stage | ✅ Created | JRE Alpine backend, Node Alpine frontend |
| **Infrastructure** | Docker Compose v3.9 | ✅ Created | PostgreSQL, RabbitMQ, Redis services |

### Why Spring Boot 3.2 + Java 21?
- **Virtual Threads:** Handle 1000+ async requests without thread pool limits; better than Node.js for I/O-heavy workloads
- **Sealed Classes:** Type-safe domain entities (`Proposal permits ProposalDraft, ProposalPublished`)
- **Records:** Immutable DTOs without Lombok boilerplate (e.g., `record BriefingAnswer(String id, String text)`)
- **Structured Concurrency:** Parallel tasks with proper timeout/shutdown (Java 21 preview API)
- **Spring Security:** Enterprise-grade authentication & authorization (vs custom JWT)
- **Hibernate 6.x:** Full JSONB support, strong ORM maturity, proven at scale
- **Java 21 LTS:** 8 years of support until September 2031

---

## Core Domain & Key Concepts

### Main Entities & Relationships
```
User → Workspace (1-to-many)
  ├─ Workspace has Members (with roles: owner, admin, member)
  ├─ Clients (CRUD per workspace)
  ├─ Services (service catalog: social media, landing page, etc.)
  ├─ ServiceContextProfiles (tone, entitlements, exclusions per service)
  ├─ ProposalTemplates (by service)
  └─ Proposals
       ├─ ProposalVersions (immutable history)
       ├─ BriefingSessions (discovery flow)
       │   └─ BriefingAnswers (client responses, structured)
       ├─ AIGenerations (versioned IA outputs with prompts)
       └─ Approvals (trackable: name, email, date, IP, version)
```

### Critical Data Flows
1. **Discovery**: Service selected → IA generates questions → Client answers → System measures completeness → IA appraises gaps
2. **Briefing Consolidation**: Answers → IA structures into objectives, context, risks, deliverables → Stored as JSONB
3. **Scope Generation**: Briefing + ServiceContext → IA suggests scope, exclusions, assumptions → User edits → Version saved
4. **Proposal**: Scope + Template → Rendered HTML/PDF → Shared via public link with token
5. **Approval**: Client clicks "Approve" → System captures name, email, IP, timestamp, version hash → Event logged → Kickoff summary generated

---

## Database Schema Highlights

### Key Tables
- **users**: auth, workspace membership references
- **workspaces**: tenant isolation, primary niche, tone settings
- **workspace_members**: roles (owner, admin, member), permissions
- **clients**: CRUD for each workspace
- **service_catalog**: domain services (social media, landing page, etc.)
- **service_context_profiles**: per-service templates, entitlements, exclusions, tone overrides
- **proposal_templates**: HTML templates with Handlebars or similar placeholders
- **briefing_sessions**: discovery flow instance; contains `public_token` for client
- **briefing_answers**: structured responses (JSON) keyed by question_id, answerable_type
- **ai_generations**: audit trail of all IA outputs (type, input JSON, output JSON, prompt version)
- **proposals**: active proposal state, linked to client, service, briefing, workspace
- **proposal_versions**: immutable snapshots (scope, entitlements, exclusions, price, timeline)
- **approvals**: approval record (name, email, IP, approved_at, version_id reference)
- **proposal_events**: audit log (created, viewed, approved, etc.)
- **files**: S3 references (logo, attachment, proposal PDF, kickoff PDF)

All timestamps are UTC. Sensitive fields are segregated (no PII in JSONB unless necessary). Indexes on workspace_id, proposal_id, client_id for query performance.

---

## Development Workflow & Commands

### Prerequisites
- **Java 21** (Eclipse Temurin recommended; GraalVM optional for native-image)
- **Maven 3.8+** (or use embedded Maven wrapper)
- **Node.js LTS** (for frontend development only)
- **PostgreSQL 14+** locally (Docker Compose provided)
- **RabbitMQ or Redis** (for async queue; Docker Compose provided)
- **Docker & Docker Compose v2**
- **OpenAI / Anthropic API key** (for IA integration)

### First-Time Setup
```bash
# Clone and install
git clone <repo>
cd projeto-service-b2b

# Setup environment
cp .env.example .env
# Update .env: DATABASE_URL, OPENAI_API_KEY, JWT_SECRET, S3_BUCKET, RABBITMQ_URL, etc.

# Start infrastructure
docker compose up -d  # PostgreSQL + RabbitMQ (or Redis) + (optional Redis cache)

# Backend: Run migrations (Liquibase or Flyway)
cd backend
./mvnw flyway:migrate

# Backend: Seed database (optional: sample niche templates)
./mvnw exec:java@seed-data

# Start dev servers (separate terminals)
# Terminal 1 - Backend
cd backend
./mvnw spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm install
npm run dev
```

### Development Commands

#### Backend (Spring Boot 3.2 + Java 21)
```bash
# Prerequisites: Java 21, Maven 3.8+, PostgreSQL running
cd backend

# Start in dev mode
./mvnw spring-boot:run                     # http://localhost:8080/api/v1

# Build (creates uber JAR with all dependencies)
./mvnw clean package                       # target/scopeflow-api-1.0.0-SNAPSHOT.jar

# Database Migrations (Flyway)
./mvnw flyway:migrate                      # Run all pending migrations from V1__initial_schema.sql
./mvnw flyway:repair                       # Fix failed migrations

# Quality & Testing
./mvnw test                                # All unit tests (JUnit 5 + AssertJ + Mockito)
./mvnw verify                              # Unit + integration tests (with Testcontainers)
./mvnw test -Dtest=ProposalServiceTest     # Single test class
./mvnw checkstyle:check                    # Code quality (Checkstyle)
./mvnw package jacoco:report               # Coverage report → target/site/jacoco/index.html

# Java 21 Features
# Virtual threads automatically used for async tasks
# Sealed classes in domain layer (com.scopeflow.core.domain)
# Records for DTOs (immutable, boilerplate-free)

# Troubleshooting
./mvnw help:describe -Dplugin=spring-boot  # Spring Boot plugin info
./mvnw -DskipTests clean package           # Build without tests
./mvnw dependency:tree                     # Show dependency tree
```

#### Frontend (Next.js 15 + React 19)
```bash
cd frontend

# Development
npm run dev                                # http://localhost:3000
npm run build                              # Production build
npm run lint                               # ESLint
npm run type-check                         # TypeScript strict mode
npm run test                               # Jest tests
npm run test:e2e                           # Playwright E2E tests

# Running specific tests
npm test -- ProposalForm.test.tsx
npm run test:e2e -- --grep "briefing flow"
```

#### All-in-One
```bash
# Terminal 1: Start backend + PostgreSQL
docker compose up -d && ./mvnw spring-boot:run

# Terminal 2: Start frontend
cd frontend && npm run dev

# Or use Make/scripts if provided in repo
make dev                                   # Concurrent backend + frontend
```

---

## Code Style & Conventions

### Naming & Structure
| Aspect | Convention | Example |
|--------|-----------|---------|
| **Packages (Backend Java)** | `src/main/java/com/scopeflow/{layer}/{domain}` | `adapter/in/web/AuthController.java` |
| **Service Classes** | `{Entity}Service` with business logic | `BriefingService.java`, `ProposalService.java` |
| **Controllers** | `{Entity}Controller` with DTOs as records | `AuthController.java` (with nested records) |
| **Repositories** | `{Entity}Repository extends JpaRepository` | `ProposalRepository.java` (Spring Data JPA) |
| **DTOs** | Records or immutable classes | `record ProposalResponse(UUID id, String status)` |
| **Domain Entities** | Sealed classes for type safety | `sealed class Proposal permits ProposalDraft, ProposalPublished` |
| **Components (Frontend)** | PascalCase, `src/components/{feature}` | `src/components/briefing/BriefingForm.tsx` |
| **Hooks** | `src/hooks/use{Feature}.ts` | `src/hooks/useBriefing.ts` |
| **Database** | Flyway migrations, snake_case tables | `V1__initial_schema.sql`, `users`, `briefing_sessions` |
| **Packages** | `com.scopeflow.{layer}.{domain}` | `com.scopeflow.adapter.in.web`, `com.scopeflow.core.domain` |

### Error Handling
- Backend: throw domain-specific exceptions (`ProposalNotFoundException`, `BriefingIncompleteError`)
- Frontend: catch, log, show user-friendly toast/modal
- All errors logged with context: user_id, workspace_id, proposal_id where relevant
- No sensitive data in error messages sent to client

### Testing Strategy
- **Unit**: services, repositories, utilities (no DB, mock external calls)
- **Integration**: with real DB (Testcontainers in Node.js or test PostgreSQL)
- **E2E**: full flow from UI to DB (Playwright preferred for Next.js)
- **Naming**: `describe('ProposalService', () => { it('should reject incomplete briefing', () => {...}))`
- **Coverage targets**: 100% for auth/payment logic, 80%+ for domain, 60%+ for controllers

### Formatting
- **Indentation**: 2 spaces
- **Quotes**: Single quotes (`'`) for strings
- **Semicolons**: Required
- **Line length**: 100 chars soft, 120 hard
- **Auto-format**: Prettier (`.prettierrc` configured)
- **Linting**: ESLint with `@typescript-eslint` for type safety

---

## Key Workflows & Decision Points

### 1. Adding a New Service Type (e.g., "Branding")
1. Add entry to `service_catalog` seed
2. Create `ServiceContextProfile` with:
   - Questions template (JSON list)
   - Default entitlements (e.g., "logo", "brand guide")
   - Default exclusions (e.g., "trademark research")
   - Suggested timeline, pricing structure
3. Optionally create `ProposalTemplate` if HTML rendering differs
4. Test with BriefingSession flow: verify IA loads correct context
5. Validate ProposalVersion generation includes new exclusions

### 2. Modifying IA Prompts
1. All prompts stored in `prompts/` folder with version tags (e.g., `briefing_questions_v1.md`)
2. In `ai_generations` table, `prompt_version` field captures which version generated output
3. New prompt → new version file → update backend to reference new version
4. Never modify old prompt versions in-place (breaks auditability)
5. Log prompt_version in all IA generation records for debugging

### 3. Handling Approval Workflow
- Client receives link: `/proposals/{proposal_id}/approve?token={public_token}`
- Page displays:
  - Proposal version HTML (read-only)
  - Friendly summary (what's included, exclusions, timeline, price)
  - Approval form (name, email)
- On submit:
  - Validate token, check expiry
  - Save `Approval` record with IP, User-Agent
  - Create `ProposalEvent` with type="approved"
  - Trigger async job: generate kickoff PDF, send confirmation email
  - Return success page with download links

### 4. Handling PDF Generation
- Backend service: `PdfService` (via puppeteer or pdfkit)
- Receives: proposal template, scope JSON, client name, logo URL
- Outputs: S3 file with key `{workspace_id}/{proposal_id}/{version_id}-proposal.pdf`
- Store file reference in `files` table
- If S3 fails: retry via queue (BullMQ), log failure
- Never block approval on PDF failure (async, best-effort)

---

## Testing & Quality Gates

### Before Committing
```bash
npm run lint              # Fix ESLint/Prettier issues
npm run type-check        # Catch TypeScript errors
npm run test              # Unit tests pass
```

### Before PR/Merge
```bash
npm run test:integration  # DB logic correct
npm run test:e2e          # Critical flows work end-to-end
npm run coverage          # Verify coverage targets met
```

### CI/CD (GitHub Actions)
1. Lint + type-check on PR
2. Run tests (unit, integration, E2E)
3. Build (Next.js, NestJS)
4. Deploy to staging on merge to `develop`
5. Manual approval to production

---

## Security Practices

### Authentication & Authorization
- Users authenticate via email + password (bcrypt hash)
- JWT tokens: access (15 min), refresh (7 days)
- Workspace segregation: all queries filtered by workspace_id
- Roles: owner (all), admin (all except members), member (read proposals only)

### Data Privacy
- Minimal collection: name, email, workspace context, briefing/proposal content
- LGPD compliance: user can request data export, deletion
- Briefing/proposal data: no marketing tracking, no third-party sharing
- PDFs and files: S3 with private ACL, signed URLs for temporary access

### API Security
- Rate limiting on public endpoints (approval links, briefing submission)
- CORS restricted to app domain
- HTTPS enforced (TLS 1.2+)
- Secrets never logged or cached in plain text (.env gitignored)

---

## Observability & Logging

### Critical Paths to Log
- Authentication (login, token refresh, logout)
- Briefing completion (questions asked, answers submitted, consolidation)
- IA generation (calls, latency, success/failure)
- PDF generation (request, S3 upload, errors)
- Proposal approval (client info, IP, timestamp, version)

### Log Format
```json
{
  "timestamp": "2025-01-15T10:30:45Z",
  "level": "info",
  "service": "proposal-service",
  "action": "proposal_approved",
  "user_id": "uuid",
  "workspace_id": "uuid",
  "proposal_id": "uuid",
  "duration_ms": 234,
  "status": "success"
}
```

### Metrics to Track
- Briefing completion rate (%)
- Approval rate (%)
- Time to approval (minutes)
- IA generation latency (seconds)
- PDF generation success rate (%)
- Proposal edits before approval (count)

---

## Git Workflow

### Branching Strategy
- **main**: production-ready, always deployable
- **develop**: staging, feature integration
- **feature/**: `feature/briefing-ai`, `feature/approval-flow`
- **bugfix/**: `bugfix/proposal-rendering`

### Commit Message Format
**Conventional Commits in Portuguese:**
```
feat(briefing): adiciona aprofundamento automático de respostas vagas

Implementa lógica de IA para detectar respostas incompletas
e gerar perguntas complementares. Reduz ambiguidade no briefing.

Closes #42
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `chore`, `ci`

### Before Pushing
- Run tests locally: `npm run test`
- Check lint: `npm run lint`
- Review changes: `git diff` before staging
- No secrets or large binaries in commits

---

## Deployment & Operations

### Environment Variables
| Variable | Purpose | Example |
|----------|---------|---------|
| `DATABASE_URL` | PostgreSQL connection | `postgresql://user:pass@localhost:5432/scopeflow` |
| `REDIS_URL` | BullMQ queue | `redis://localhost:6379` |
| `OPENAI_API_KEY` | LLM calls | `sk-...` |
| `JWT_SECRET` | Token signing | `<random-32-chars>` |
| `S3_BUCKET` | AWS S3 bucket | `scopeflow-prod` |
| `S3_REGION` | AWS region | `sa-east-1` |
| `CORS_ORIGIN` | Frontend URL | `https://app.scopeflow.com` |
| `NODE_ENV` | Environment | `development`, `staging`, `production` |

### Scaling Considerations (Post-MVP)
- **Read replicas** for analytics queries
- **Connection pooling** (PgBouncer) if connection count spikes
- **Caching**: Redis for user profiles, service contexts
- **IA async queue**: scale workers based on generation latency
- **CDN**: CloudFront for PDFs, logos, static assets

---

## Known Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| IA generates generic/irrelevant questions | Low engagement, low perceived value | Niche + service context well-defined; review outputs; A/B test prompts |
| High IA API cost | Profitability risk | Use structured JSON, cache context, set generation limits per plan |
| Proposal PDF fails | User frustration, support burden | Async generation, retry queue, graceful degradation (HTML fallback) |
| Approval token exposed | Unauthorized approval | Short TTL (24h), rate limit, IP logging, secure token gen |
| Low retention post-MVP | Revenue risk | Strong templates per niche, encourage reuse, measure NPS |

---

## Quick Reference: Slash Commands

When using Claude Code, these commands streamline development:

```bash
# Check project structure
/dev-review src/proposals/          # Code review on proposal module

# Generate tests
/qa-generate ProposalService        # Unit + integration tests

# Audit dependencies & security
/qa-security                        # OWASP check, dependency scan

# Plan refactoring
/dev-refactor BriefingSession       # Safe refactoring strategy

# Full bootstrap alternative (if extending with microservices)
/full-bootstrap user-service aws    # Create new service: scaffold + tests + IaC
```

---

## FAQ & Troubleshooting

### Q: How do I reset the database in development?
```bash
docker compose down -v
docker compose up -d
npm run migrate
npm run seed
```

### Q: Why is IA generation slow?
Check:
1. OpenAI API quota/throttling
2. Prompt size (large briefing context can slow inference)
3. Network latency (add timing logs)
4. Redis queue backed up (check `npm run redis-cli` → `LLEN queue:ai_generation`)

### Q: How do I test the approval flow locally?
1. Create proposal via API or UI
2. Get `public_token` from `briefing_sessions` table
3. Visit: `http://localhost:3000/proposals/{proposal_id}/approve?token={token}`
4. Submit approval form
5. Check `approvals` and `proposal_events` tables for records

### Q: How do I add a new field to ProposalVersion?
1. Update `schema.prisma` (add field to model)
2. `npx prisma migrate dev --name add_field_to_proposal_version`
3. Update `ProposalVersionResponse` DTO
4. Update test mocks
5. Commit migration file

---

## Additional Resources

- **Product spec**: `scopeflow_ai_documento_master_completo.md` (2045 lines, full context)
- **API contracts**: Same doc, section 31
- **Database schema**: Same doc, section 30
- **Wireframes**: Same doc, section 29

For questions on product direction, refer to:
- Roadmap: phase 1 (MVP), 2 (validation), 3 (expansion)
- Success metrics: briefing completion rate, approval rate, time-to-approval
- Initial market focus: microagencies + freelancers in marketing/design/social media/landing pages
