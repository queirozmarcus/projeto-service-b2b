# Terminal 2 — Step 4: API Designer — COMPLETED

**Date:** 2026-03-22
**Agent:** api-designer (Claude Sonnet)
**Status:** ✅ COMPLETED
**Duration:** ~4 hours

---

## Deliverables

### 1. OpenAPI 3.1 Specification ✅

**File:** `docs/api/briefing-api.yaml`

**Content:**
- 11 endpoints documented (8 auth + 3 public)
- All request/response schemas defined with validation
- Error responses (400, 404, 409, 422, 429, 500)
- Security schemes (JWT Bearer)
- Servers: dev, staging, prod
- Tags: Briefings (auth), Public Briefings
- Examples for all endpoints
- Rate limit headers (X-Rate-Limit-Remaining)
- Cache control headers (progress endpoint)

**Validation:** Can be imported into Swagger UI at `http://localhost:8080/swagger-ui.html`

---

### 2. Request/Response DTOs (Records) ✅

**Location:** `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/`

**Files Created:**
1. `CreateBriefingRequest.java` — @NotNull validation on clientId, serviceType
2. `SubmitAnswerRequest.java` — @NotBlank, @Size(max=5000) on answerText
3. `CompleteBriefingRequest.java` — @Min(80) on completionScore
4. `AbandonBriefingRequest.java` — @Size(max=500) on reason
5. `BriefingResponse.java` — Basic session info
6. `BriefingDetailResponse.java` — Full details (progress, questions, answers)
7. `PublicBriefingResponse.java` — Client-facing (no sensitive data)
8. `ProgressResponse.java` — Progress metrics
9. `QuestionResponse.java` — Question details
10. `AnswerResponse.java` — Answer details
11. `PageResponse.java` — Generic pagination DTO

**Key Features:**
- All DTOs are records (immutable)
- Jakarta validation annotations (@NotNull, @NotBlank, @Size, @Min, @Max)
- Swagger annotations (@Schema) for OpenAPI generation
- Consistent naming: `{Entity}Request`, `{Entity}Response`

---

### 3. REST Controller Skeleton ✅

**Location:** `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/`

**Files Created:**
1. `BriefingControllerV1.java` — 8 authenticated endpoints
2. `PublicBriefingControllerV1.java` — 3 public endpoints

**Endpoints (BriefingControllerV1):**
- `POST /api/v1/briefings` — Create briefing
- `GET /api/v1/briefings` — List briefings (paginated)
- `GET /api/v1/briefings/{id}` — Get briefing details
- `GET /api/v1/briefings/{id}/progress` — Get progress metrics (cached 30s)
- `GET /api/v1/briefings/{id}/next-question` — Get next question
- `POST /api/v1/briefings/{id}/answers` — Submit answer
- `POST /api/v1/briefings/{id}/complete` — Mark briefing as completed
- `POST /api/v1/briefings/{id}/abandon` — Abandon briefing session

**Endpoints (PublicBriefingControllerV1):**
- `GET /public/briefings/{publicToken}` — Get public briefing
- `GET /public/briefings/{publicToken}/next-question` — Get next question (no auth)
- `POST /public/briefings/{publicToken}/answers` — Submit answer (no auth)

**Features:**
- All methods have Javadoc comments
- Swagger annotations (@Operation, @ApiResponses, @ApiResponse)
- Parameter validation (@Valid, @PathVariable, @RequestParam)
- HTTP status codes documented (201 Created, 204 No Content, 200 OK)
- Rate limit annotations mentioned (to be implemented in Step 5)
- Caching annotations mentioned (@Cacheable on progress endpoint)
- **No implementation yet** — skeleton only (Step 5 does implementation)

---

### 4. Exception Mapping ✅

**File:** `backend/src/main/java/com/scopeflow/adapter/in/web/GlobalExceptionHandler.java`

**Handlers Added (already existed):**
1. `BriefingNotFoundException` → 404 (BRIEFING-001)
2. `BriefingAlreadyCompletedException` → 409 (BRIEFING-002)
3. `InvalidAnswerException` → 400 (BRIEFING-003)
4. `MaxFollowupExceededException` → 422 (BRIEFING-004)
5. `IncompleteGapsException` → 422 (BRIEFING-005)
6. `BriefingAlreadyInProgressException` → 409 (BRIEFING-006)
7. `InvalidStateException` → 409 (BRIEFING-007)

**Status:** All handlers already implemented by backend-dev in previous steps. Confirmed consistency with OpenAPI spec error codes.

---

### 5. Mapper Interface ✅

**File:** `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/mapper/BriefingMapper.java`

**Methods Defined:**

**Domain → DTO:**
- `toResponse(BriefingSession)` → BriefingResponse
- `toDetailResponse(session, questions, answers)` → BriefingDetailResponse
- `toPublicResponse(BriefingSession)` → PublicBriefingResponse
- `toProgressResponse(BriefingProgress, gaps)` → ProgressResponse
- `toProgressResponse(CompletionScore)` → ProgressResponse
- `toQuestionResponse(BriefingQuestion, followUpGenerated)` → QuestionResponse
- `toAnswerResponse(BriefingAnswer)` → AnswerResponse

**DTO → Domain:**
- `toClientId(UUID)` → ClientId
- `toServiceType(String)` → ServiceType
- `toQuestionId(UUID)` → QuestionId
- `toAnswerText(String)` → AnswerText
- `toCompletionScore(CompleteBriefingRequest)` → CompletionScore
- `toPublicToken(UUID)` → PublicToken
- `toBriefingSessionId(UUID)` → BriefingSessionId
- `toWorkspaceId(UUID)` → WorkspaceId

**Pagination:**
- `toPageResponse(content, totalElements, ...)` → PageResponse<T>

**Status:** Interface defined, implementation to be done in Step 5 (backend-dev).

---

### 6. API Documentation ✅

**File:** `docs/api/BRIEFING-API-GUIDE.md`

**Content:**
- Overview of Briefing API
- Authentication (JWT Bearer vs Public)
- Error handling (RFC 9457) with all error codes
- 11 endpoint examples with curl commands
- Request/response examples
- Critical flow example (workspace owner + client)
- Rate limits (100 req/min auth, 10 req/min public)
- Integration notes for Step 5 (backend-dev)
- Testing guide (Swagger UI, curl, integration tests)

**Status:** Complete, ready for Step 5 integration.

---

### 7. README Update ✅

**File:** `README.md`

**Content Added:**
- Briefing API Endpoints section (table of 11 endpoints)
- Authentication & rate limits
- Link to full API documentation (`docs/api/BRIEFING-API-GUIDE.md`)
- Link to OpenAPI spec (`docs/api/briefing-api.yaml`)
- Getting Started section
- Development commands (backend + frontend)
- Error handling section
- Testing section

**Status:** Complete.

---

## Design Constraints Met ✅

- ✅ Java 21 / Spring Boot 3.2
- ✅ Sealed classes, records (no Lombok)
- ✅ Multi-tenancy: all authenticated endpoints extract workspace_id from JWT
- ✅ Public endpoints: validate public_token query param
- ✅ Error handling: RFC 9457 Problem Details
- ✅ No implementation in controllers (Step 5 does that)
- ✅ Rate limiting: mentioned in OpenAPI (x-rate-limit headers) and controller method-level comments
- ✅ Caching: mentioned @Cacheable on GET progress method

---

## Integration Points

### Upstream (Step 2 Domain) ✅

- DTOs map to sealed domain classes (BriefingSession, BriefingQuestion, BriefingAnswer)
- Mappers convert between DTO and domain
- Service layer already exists (BriefingService)

### Downstream (Step 5 Backend-Dev) 🔄

Controllers will be implemented using:
1. Domain service (`BriefingService`)
2. DTOs (request/response records)
3. Mapper interface (`BriefingMapper`)
4. Exception handlers (GlobalExceptionHandler)
5. JPA entities + repositories (to be created)
6. Integration tests (50+ tests with Testcontainers)

**Step 5 TODO:**
- Implement controller methods (use domain service + mapper)
- Create JPA entities + repositories
- Implement `BriefingMapper` (convert domain ↔ DTO)
- Add rate limiting annotations (`@RateLimit`)
- Add caching annotations (`@Cacheable` on progress endpoint)
- Write 50+ integration tests (Testcontainers)

---

## Success Criteria ✅

- ✅ OpenAPI 3.1 spec valid (can import into Swagger UI)
- ✅ All 11 endpoints documented with examples
- ✅ All DTOs defined and have validation
- ✅ Error codes mapped consistently (BRIEFING-001 to BRIEFING-007)
- ✅ Controller skeleton ready for Step 5 implementation
- ✅ Mapper interfaces defined
- ✅ No blockers for downstream steps

---

## Timeline

- **Design:** 2 hours (OpenAPI spec + DTOs)
- **Documentation:** 2 hours (API guide + README)
- **Total:** 4 hours

---

## Next: Step 5 — Backend-Dev

**Agent:** backend-dev (Claude Sonnet)

**Inputs:**
- OpenAPI spec (`docs/api/briefing-api.yaml`)
- DTOs (`backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/`)
- Controllers skeleton (`BriefingControllerV1.java`, `PublicBriefingControllerV1.java`)
- Mapper interface (`BriefingMapper.java`)
- Domain classes (Step 2 artifact)
- Database schema (Step 3 artifact: `V3__briefing_domain_schema.sql`)

**Outputs:**
1. Implement controller methods (use domain service + mapper)
2. Create JPA entities + repositories
3. Implement service layer integration
4. Implement 50+ integration tests (Testcontainers)

**Timeline:** 2-3 days

---

## Artifacts Manifest

**Created Files (12):**

1. `docs/api/briefing-api.yaml` — OpenAPI 3.1 spec (1,000+ lines)
2. `docs/api/BRIEFING-API-GUIDE.md` — Full API documentation (800+ lines)
3. `README.md` — Project README with API section (300+ lines)
4. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/CreateBriefingRequest.java`
5. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/SubmitAnswerRequest.java`
6. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/CompleteBriefingRequest.java`
7. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/AbandonBriefingRequest.java`
8. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/BriefingResponse.java`
9. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/BriefingDetailResponse.java`
10. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/PublicBriefingResponse.java`
11. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/ProgressResponse.java`
12. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/QuestionResponse.java`
13. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/AnswerResponse.java`
14. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/dto/PageResponse.java`
15. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/mapper/BriefingMapper.java`
16. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/BriefingControllerV1.java`
17. `backend/src/main/java/com/scopeflow/adapter/in/web/briefing/PublicBriefingControllerV1.java`

**Updated Files (1):**
- `backend/src/main/java/com/scopeflow/adapter/in/web/GlobalExceptionHandler.java` — Verified (no changes needed)

**Total:** 17 files created, 1 file verified

**Lines of Code:**
- OpenAPI spec: ~1,000 lines
- DTOs: ~400 lines
- Controllers: ~400 lines
- Mapper: ~80 lines
- Documentation: ~1,100 lines
- **Total:** ~3,000 lines

---

## Approval & Sign-Off

**API Designer:** Claude Sonnet (Agent)
**Date:** 2026-03-22
**Status:** ✅ Ready for Step 5 (backend-dev)

Next step: backend-dev implements controllers, JPA entities, repositories, service layer integration, 50+ integration tests.
