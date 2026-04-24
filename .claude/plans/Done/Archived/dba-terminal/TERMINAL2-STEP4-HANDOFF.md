# Terminal 2 — Step 4 Handoff: API Designer

**Date:** 2026-03-22
**From:** DBA (Claude Sonnet) — Step 3 Complete ✅
**To:** API Designer (API-Designer Agent)
**Status:** Ready to Start
**Blocker:** None

---

## Context

**Briefing Domain** is now at **3/7 steps complete** in Terminal 2 orchestration.

### Completed
- ✅ Step 1: Architect — ADR-002 (sealed classes, domain events)
- ✅ Step 2: Backend-Dev — Domain classes + 50+ unit tests
- ✅ Step 3: DBA — **Flyway V3 migration (just completed)**

### Current
- ⏳ **Step 4: API Designer** — Your task (REST endpoints + OpenAPI)

### Upcoming
- ⏳ Step 5: Backend-Dev — JPA + repositories + integration tests
- ⏳ Step 6: QA — Test suites (unit, integration, E2E)
- ⏳ Step 7: DevOps — Deployment automation

---

## What Step 3 Delivered

### Flyway V3 Migration (401 lines, production-ready)
**File:** `backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql`
**Committed:** `ffbb8846a192485ca463924239c75354146fab35`
**PR:** https://github.com/queirozmarcus/projeto-service-b2b/pull/1

### Schema: 5 Tables + Audit Trail

| Table | Purpose | Rows | Indexes |
|-------|---------|------|---------|
| `briefing_sessions` | Aggregate root | ~10K/year | 8 |
| `briefing_questions` | Questions | ~100K/year | 6 |
| `briefing_answers` | Immutable answers | ~100K/year | 4 |
| `ai_generations` | LLM audit trail | ~50K/year | 5 |
| `briefing_activity_logs` | Compliance log | ~200K/year | 4 |

**Total: ~460K rows/year, 30+ indexes, 10+ constraints**

### Key Features

✅ Database-level invariant enforcement (no app logic can bypass)
✅ Immutable answers (TRIGGER prevents UPDATE/DELETE)
✅ Outbox pattern for reliable event publishing
✅ Event types: started, completed, abandoned, question, answer
✅ Query views: active sessions, completed, cost tracking

---

## Your Task: Design REST API (Step 4)

### Input: What You Have

1. **ADR-002** (Step 1 artifact)
   - Sealed class hierarchy
   - Domain events
   - Repository interfaces
   - Location: `docs/architecture/adr/ADR-002-briefing-domain.md`

2. **Domain Classes** (Step 2 artifact)
   - `com.scopeflow.core.domain.briefing.*`
   - Sealed classes: BriefingSession, BriefingAnswer
   - Value objects: BriefingSessionId, AnswerText, PublicToken, etc.
   - Domain service: BriefingService
   - Location: `backend/src/main/java/com/scopeflow/core/domain/briefing/`

3. **Database Schema** (Step 3 artifact — this)
   - 5 tables + 30+ indexes
   - Flyway migration ready
   - Location: `backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql`

4. **Existing API Controllers** (partial, from prior work)
   - `BriefingControllerV1.java` (may need review/completion)
   - `PublicBriefingControllerV1.java` (may need review/completion)
   - Location: `backend/src/main/java/com/scopeflow/adapter/in/web/`

### Output: What You Need to Produce

#### 1. REST Endpoints (OpenAPI 3.1)

**Base Path:** `/api/v1/briefings`

**Endpoints to Design:**

```
POST   /api/v1/briefings
       Create new briefing session
       Input: workspace_id, client_id, service_type
       Output: BriefingResponse { id, status, public_token, created_at }

GET    /api/v1/briefings/{id}
       Retrieve session details
       Query: ?include=questions,answers,analysis
       Output: BriefingDetailResponse { session, questions, answers, progress }

GET    /api/v1/briefings/{id}/progress
       Get session progress
       Output: ProgressResponse { current_step, total_steps, percentage, answered_questions }

GET    /api/v1/briefings/{id}/next-question
       Get next unanswered question
       Output: QuestionResponse { id, text, type, step, required }

POST   /api/v1/briefings/{id}/answers
       Submit answer to question
       Input: AnswerRequest { question_id, answer_text, answer_json }
       Output: AnswerResponse { id, created_at, ai_analysis, quality_score }

POST   /api/v1/briefings/{id}/complete
       Mark briefing as complete (score ≥ 80)
       Input: CompletionRequest { completion_score, gaps_identified }
       Output: CompletionResponse { id, status, completed_at, ai_analysis }

POST   /api/v1/briefings/{id}/abandon
       Abandon briefing session
       Input: AbandonRequest { reason }
       Output: AbandonResponse { id, status, abandoned_at }

GET    /api/v1/briefings
       List sessions (workspace-scoped)
       Query: ?status=IN_PROGRESS&limit=20&offset=0
       Output: PageResponse<BriefingSummary>

# Public Endpoints (Client-Facing, No Auth)
GET    /api/public/briefings/{id}/approve
       Public approval link (displays briefing)
       Query: ?token={public_token}
       Output: PublicBriefingResponse { session, questions_answered, scope }

POST   /api/public/briefings/{id}/approve
       Client submits approval
       Query: ?token={public_token}
       Input: ApprovalRequest { client_name, client_email }
       Output: ApprovalResponse { approved_at, next_steps }
```

#### 2. Request/Response DTOs (Records)

**Request DTOs:**
```java
record CreateBriefingRequest(
    UUID workspaceId,
    UUID clientId,
    ServiceType serviceType
) {}

record SubmitAnswerRequest(
    UUID questionId,
    String answerText,
    JsonNode answerJson  // optional, for structured responses
) {}

record CompleteBriefingRequest(
    int completionScore,  // 0-100, must be >= 80
    List<String> gapsIdentified
) {}

record AbandonBriefingRequest(
    String reason
) {}
```

**Response DTOs:**
```java
record BriefingResponse(
    UUID id,
    String status,
    String publicToken,
    Instant createdAt
) {}

record BriefingDetailResponse(
    BriefingResponse session,
    List<QuestionResponse> questions,
    List<AnswerResponse> answers,
    ProgressResponse progress
) {}

record ProgressResponse(
    int currentStep,
    int totalSteps,
    int progressPercentage,
    int answeredQuestions
) {}

record QuestionResponse(
    UUID id,
    String text,
    String questionType,
    int step,
    boolean required
) {}

record AnswerResponse(
    UUID id,
    UUID questionId,
    String answerText,
    Integer qualityScore,
    JsonNode aiAnalysis,
    Instant createdAt
) {}
```

#### 3. OpenAPI 3.1 Specification

Document all endpoints, schemas, error responses.

**Key sections:**
- Info: title, version, description
- Servers: dev/staging/prod URLs
- Paths: all endpoints with operation details
- Components: schemas (requests/responses), security schemes
- Tags: organizing endpoints by domain

**Example structure:**
```yaml
openapi: 3.1.0
info:
  title: ScopeFlow Briefing API
  version: 1.0.0
  description: AI-assisted briefing discovery flow

servers:
  - url: http://localhost:8080
    description: Development
  - url: https://staging.scopeflow.com
    description: Staging
  - url: https://api.scopeflow.com
    description: Production

paths:
  /api/v1/briefings:
    post:
      tags: [Briefings]
      summary: Create new briefing session
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateBriefingRequest'
      responses:
        201:
          description: Session created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BriefingResponse'
        400:
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'

components:
  schemas:
    BriefingResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        status:
          type: string
          enum: [IN_PROGRESS, COMPLETED, ABANDONED]
        ...
```

---

## Design Decisions You Need to Make

### 1. Error Handling (Problem Details RFC 9457)

**Choose format for error responses:**

```json
// Option A: Structured error
{
  "type": "https://api.scopeflow.com/errors/briefing-not-found",
  "title": "Briefing Not Found",
  "status": 404,
  "detail": "Session a1b2c3d4 not found in workspace e5f6g7h8",
  "instance": "/api/v1/briefings/a1b2c3d4"
}

// This is RFC 9457 standard, implemented by Spring as ProblemDetail
```

**Error codes to map:**
- BRIEFING-001: BriefingNotFoundException → 404
- BRIEFING-002: BriefingAlreadyCompletedException → 409
- BRIEFING-003: InvalidAnswerException → 400
- BRIEFING-004: MaxFollowupExceededException → 422
- BRIEFING-005: IncompleteGapsException → 422

### 2. Pagination

**List endpoints (e.g., GET /api/v1/briefings):**
- Query params: ?limit=20&offset=0
- Response: PageResponse with metadata

```java
record PageResponse<T>(
    List<T> content,
    int totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {}
```

### 3. Authorization & Scoping

**Authenticated endpoints:**
- Extract workspace_id from JWT token (already in SecurityContext)
- Filter all queries by workspace_id (multi-tenancy)

**Public endpoints (approval link):**
- No authentication required
- Validate public_token from URL query param
- Return only approved fields (no sensitive data)

### 4. Rate Limiting

**Recommended:**
- Public endpoints: 10 req/min per IP
- Authenticated endpoints: 100 req/min per user
- Use Spring RateLimiter or custom Servlet Filter

### 5. Versioning

**API version in path:** `/api/v1/` (already chosen)
**Future versions:** `/api/v2/`, `/api/v3/`, etc.

---

## Integration Points

### Upstream (Step 2: Domain Classes)

Your DTOs should map to domain classes:
```java
// Domain class
public sealed class BriefingSession permits BriefingInProgress, BriefingCompleted, BriefingAbandoned {
    public BriefingSessionId getId();
    public WorkspaceId getWorkspaceId();
    // ...
}

// DTO
record BriefingResponse(UUID id, String status, ...) {}

// Mapper
BriefingResponse toDTOResponseFrom(BriefingSession session) {
    return new BriefingResponse(
        session.getId().value(),
        session.status(),
        ...
    );
}
```

### Downstream (Step 5: Backend-Dev — JPA)

Your endpoints will be implemented in Step 5:
- Controller layer: convert DTOs → domain → service
- Service layer: already exists (domain service)
- Repository layer: to be implemented (JPA adapters)

Step 5 will:
1. Create JPA entities (JpaBriefingSession, etc.)
2. Create repository implementations (Spring Data JPA)
3. Implement controller methods
4. Add integration tests (Testcontainers)

---

## Related Documentation

- **ADR-002:** `docs/architecture/adr/ADR-002-briefing-domain.md`
  - Sealed class hierarchy, domain events, repository ports

- **V2 User & Workspace API:** `docs/api/user-workspace-api.md` (if exists)
  - Reference existing API design pattern

- **Project CLAUDE.md:** `./CLAUDE.md`
  - API style guide, error handling, code style

---

## Deliverables Checklist (for Step 4)

- [ ] OpenAPI 3.1 specification (YAML or JSON)
  - All endpoints documented
  - All schemas defined
  - Error responses specified

- [ ] Request/Response DTOs (records)
  - All 8+ request types
  - All 8+ response types
  - Validation annotations (e.g., @NotNull, @Size)

- [ ] REST Controllers (interface/skeleton)
  - @RestController annotations
  - @RequestMapping paths
  - Method signatures
  - Javadoc comments
  - No implementation yet (Step 5 does that)

- [ ] Exception Mapping
  - Domain exceptions → HTTP status codes
  - GlobalExceptionHandler updated
  - Problem Detail responses

- [ ] API Documentation
  - README section: "API Endpoints"
  - Postman collection (optional)
  - curl examples for critical endpoints

---

## Testing Strategy (Preview for Step 6)

Once your API spec is done, Step 6 (QA) will implement:

1. **Unit Tests**
   - DTO serialization/deserialization
   - Mapper logic (domain → DTO)

2. **Integration Tests**
   - Full HTTP requests (MockMvc)
   - Status codes, headers, response bodies
   - Error cases

3. **E2E Tests**
   - Real HTTP requests (Testcontainers)
   - Full briefing flow: create → answer → complete
   - Public approval flow

---

## Timeline Estimate

- **Design phase:** 2-4 hours
  - Endpoint design review
  - DTO structure finalization
  - OpenAPI spec writing

- **Documentation:** 2-3 hours
  - Javadoc comments
  - curl examples
  - Integration guide for Step 5

- **Total:** 4-7 hours

---

## Next Steps (After Step 4)

### Step 5: Backend-Dev (Implementation)
**Input:** Your OpenAPI spec + DTOs + controller skeleton
**Output:**
- JPA entities + repository implementations
- Controller implementations
- 50+ integration tests (Testcontainers)

### Step 6: QA (Testing)
**Input:** Complete backend implementation
**Output:**
- Unit tests
- Integration tests
- E2E tests
- Coverage report (80%+)

### Step 7: DevOps (Deployment)
**Input:** Complete tested implementation
**Output:**
- Docker container
- Kubernetes manifests
- CI/CD pipeline

---

## Questions for API Designer

1. **Rate limiting:** Implement? If yes, per endpoint or global?
2. **Caching:** Cache GET briefing progress? How long?
3. **Pagination defaults:** Limit=20, offset=0? Adjust?
4. **Sort options:** On list endpoints, allow sort by created_at, status, etc.?
5. **Filter options:** List endpoint, filter by status/service_type/created_date?

---

## Success Criteria

✅ OpenAPI 3.1 spec complete and validated
✅ All DTOs defined and documented
✅ Error handling mapped to HTTP codes
✅ Controller skeleton with method signatures
✅ No implementation bugs in Step 5 (good API design prevents this)
✅ Ready for Step 5 implementation

---

## Sign-Off

**Step 3 Complete:** ✅ DBA (Claude Sonnet)
**Status:** Production-ready database schema
**Blocker for Step 4:** None
**Approver:** Marcus (Agent-Marcus)

---

**Ready for Step 4.** No blockers. Database schema is solid.

Proceed with API design. 🚀

---

Generated by: DBA (Claude Sonnet) | 2026-03-22
For: API Designer (Step 4)
Terminal 2: 3/7 steps complete
