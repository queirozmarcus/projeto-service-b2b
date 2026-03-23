# API-Designer Delegation — Step 4: Briefing REST Endpoints

**To:** api-designer (Claude Sonnet)
**From:** Marcus (Orchestrator)
**Date:** 2026-03-22
**Mode:** Fork isolated — design REST controllers + OpenAPI
**Task ID:** TERMINAL2-STEP4-API-DESIGNER
**Dependency:** Backend-Dev (Step 2) + DBA (Step 3)

---

## Mission

Design and implement the **REST API** for the Briefing bounded context. Create 2 controllers (admin + public token-based), 8+ endpoints, OpenAPI 3.1 documentation, and consistent error handling.

**Input:**
- ADR-002: `docs/architecture/adr/ADR-002-briefing-domain.md`
- Backend-Dev output: Domain classes + services (Step 2)
- DBA output: Database schema (Step 3)
- Reference: `backend/src/main/java/com/scopeflow/adapter/in/web/WorkspaceControllerV1.java` (Terminal 1 example)
- Project CLAUDE.md: `./CLAUDE.md` (API design, error handling)

**Output:**
- `backend/src/main/java/com/scopeflow/adapter/in/web/BriefingControllerV1.java` (~300 lines, admin endpoints)
- `backend/src/main/java/com/scopeflow/adapter/in/web/PublicBriefingControllerV1.java` (~250 lines, public client endpoints)
- `backend/src/main/java/com/scopeflow/adapter/in/web/BriefingExceptionHandler.java` (~150 lines, error handling)
- DTOs: Request/Response records (immutable)
- OpenAPI annotations (@Operation, @Schema, etc.)
- Documented in: `.claude/plans/API-DESIGNER-OUTPUT-Step4-Briefing.md`

**Constraints:**
- REST with OpenAPI 3.1
- Problem Details (RFC 9457) for all errors
- JWT Bearer authentication (except public endpoints with token)
- Workspace scoping (multi-tenancy)
- All DTOs as records (immutable)
- No business logic in controller (just orchestration)
- Stubs only (throw UnsupportedOperationException for implementation)

**Timeline:** ~1-2 days

---

## Endpoints to Implement

### BriefingControllerV1 (Admin — requires JWT auth)

#### 1. Start Briefing Session

```java
@PostMapping("/workspaces/{workspaceId}/briefing")
@Operation(summary = "Start AI-assisted discovery briefing")
public ResponseEntity<StartBriefingResponse> startBriefing(
    @PathVariable UUID workspaceId,
    @RequestBody StartBriefingRequest request,
    @AuthenticationPrincipal JwtAuthentication auth
) {
    // Verify workspace access (auth.workspace_id() == workspaceId)
    // Call: briefingService.startBriefing(workspaceId, clientId, serviceType)
    // Return: 201 CREATED + Location header
}
```

**Request:**
```json
{
  "client_id": "uuid",
  "service_type": "social_media"  // or landing_page, etc.
}
```

**Response (201 CREATED):**
```json
{
  "session_id": "uuid",
  "public_token": "xxx-xxx-xxx",
  "first_question": {
    "question_id": "uuid",
    "question_text": "What's your target audience?",
    "question_type": "OPEN",
    "step": 1,
    "total_steps": 10
  },
  "progress": {
    "current_step": 0,
    "total_steps": 10,
    "completion_percentage": 0
  }
}
```

**Errors:**
- `409 CONFLICT`: Only 1 active briefing per client/service (BRIEFING-001)
- `404 NOT_FOUND`: Workspace not found
- `403 FORBIDDEN`: No access to workspace

---

#### 2. List Briefings (Admin)

```java
@GetMapping("/workspaces/{workspaceId}/briefing")
@Operation(summary = "List all briefings in workspace")
public ResponseEntity<Page<BriefingSessionResponse>> listBriefings(
    @PathVariable UUID workspaceId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String status,  // IN_PROGRESS, COMPLETED, ABANDONED
    @AuthenticationPrincipal JwtAuthentication auth
) {
    // Verify workspace access
    // Query: briefingService.findByWorkspaceAndStatus(workspaceId, status)
    // Return paginated list
}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "session_id": "uuid",
      "client_id": "uuid",
      "service_type": "social_media",
      "status": "IN_PROGRESS",
      "completion_score": 45,
      "progress": { "current_step": 5, "total_steps": 10, "completion_percentage": 50 },
      "created_at": "2026-03-22T10:00:00Z",
      "updated_at": "2026-03-22T11:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total_elements": 5,
  "total_pages": 1
}
```

---

#### 3. Get Briefing Details (Admin)

```java
@GetMapping("/workspaces/{workspaceId}/briefing/{sessionId}")
@Operation(summary = "Get briefing session details")
public ResponseEntity<BriefingSessionDetailResponse> getBriefing(
    @PathVariable UUID workspaceId,
    @PathVariable UUID sessionId,
    @AuthenticationPrincipal JwtAuthentication auth
) {
    // Verify workspace access
    // Fetch: briefingService.getSession(sessionId)
    // Return: full briefing with all answers
}
```

**Response (200 OK):**
```json
{
  "session_id": "uuid",
  "client_id": "uuid",
  "service_type": "social_media",
  "status": "IN_PROGRESS",
  "completion_score": 60,
  "progress": { "current_step": 6, "total_steps": 10, "completion_percentage": 60 },
  "answers": [
    {
      "answer_id": "uuid",
      "question_id": "uuid",
      "question_text": "What's your target audience?",
      "answer_text": "Marketing professionals aged 25-45",
      "follow_up_generated": true,
      "ai_analysis": { "confidence": 0.85, "quality_score": 8 },
      "answered_at": "2026-03-22T10:05:00Z"
    }
  ],
  "ai_analysis": { "gaps": ["Need timeline details"], "recommendations": ["..." ] },
  "created_at": "2026-03-22T10:00:00Z",
  "updated_at": "2026-03-22T11:30:00Z"
}
```

---

#### 4. Complete Briefing (Admin)

```java
@PostMapping("/workspaces/{workspaceId}/briefing/{sessionId}/complete")
@Operation(summary = "Complete briefing (if completion >= 80%)")
public ResponseEntity<BriefingCompletedResponse> completeBriefing(
    @PathVariable UUID workspaceId,
    @PathVariable UUID sessionId,
    @RequestBody CompleteBriefingRequest request,  // optional: force_complete flag
    @AuthenticationPrincipal JwtAuthentication auth
) {
    // Verify workspace access
    // Call: briefingService.completeBriefing(sessionId, completionScore)
    // Return: 200 OK
}
```

**Response (200 OK):**
```json
{
  "session_id": "uuid",
  "status": "COMPLETED",
  "completion_score": 85,
  "gaps_identified": [],
  "ready_for_scope_generation": true,
  "completed_at": "2026-03-22T12:00:00Z"
}
```

**Errors:**
- `409 CONFLICT`: Completion score < 80% (BRIEFING-005)
- `404 NOT_FOUND`: Session not found (BRIEFING-001)

---

#### 5. Abandon Briefing (Admin)

```java
@PostMapping("/workspaces/{workspaceId}/briefing/{sessionId}/abandon")
@Operation(summary = "Abandon briefing session (can restart)")
public ResponseEntity<BriefingAbandonedResponse> abandonBriefing(
    @PathVariable UUID workspaceId,
    @PathVariable UUID sessionId,
    @RequestBody AbandonBriefingRequest request,  // reason: String
    @AuthenticationPrincipal JwtAuthentication auth
) {
    // Verify workspace access
    // Call: briefingService.abandonBriefing(sessionId)
    // Return: 200 OK
}
```

**Response (200 OK):**
```json
{
  "session_id": "uuid",
  "status": "ABANDONED",
  "reason": "Client decided to postpone",
  "abandoned_at": "2026-03-22T12:30:00Z"
}
```

---

### PublicBriefingControllerV1 (Public — token-based access)

#### 6. Get Current Question (Public)

```java
@GetMapping("/briefing/{sessionId}")
@Operation(summary = "Get current question (public, requires token)")
public ResponseEntity<CurrentQuestionResponse> getCurrentQuestion(
    @PathVariable UUID sessionId,
    @RequestParam String token,  // public_token
    @RequestParam(required = false) boolean includeHistory
) {
    // Verify token matches session
    // Get current question
    // Return: question + progress + optionally previous answers (if includeHistory=true)
}
```

**Response (200 OK):**
```json
{
  "session_id": "uuid",
  "current_question": {
    "question_id": "uuid",
    "question_text": "What's your target audience?",
    "question_type": "OPEN",
    "step": 1,
    "total_steps": 10
  },
  "progress": { "current_step": 1, "total_steps": 10, "completion_percentage": 10 },
  "previous_answers": [
    {
      "answer_text": "Marketing professionals aged 25-45",
      "follow_up_generated": true
    }
  ]
}
```

---

#### 7. Submit Answer (Public)

```java
@PostMapping("/briefing/{sessionId}/answers")
@Operation(summary = "Submit answer to current question (public)")
public ResponseEntity<SubmitAnswerResponse> submitAnswer(
    @PathVariable UUID sessionId,
    @RequestParam String token,
    @RequestBody SubmitAnswerRequest request  // question_id, answer_text
) {
    // Verify token matches session
    // Validate answer (not empty)
    // Call: briefingService.submitAnswer(sessionId, questionId, answerText)
    // Detect gaps, generate follow-up if needed
    // Return: next question (or follow-up if generated)
}
```

**Request:**
```json
{
  "question_id": "uuid",
  "answer_text": "Marketing professionals aged 25-45, focused on B2B"
}
```

**Response (200 OK):**
```json
{
  "answer_id": "uuid",
  "answer_submitted": true,
  "follow_up_question": {
    "question_id": "uuid",
    "question_text": "Can you be more specific about budget range?",
    "question_type": "MULTIPLE_CHOICE",
    "step": 2
  },
  "next_question": {
    "question_id": "uuid",
    "question_text": "What's your timeline?",
    "step": 3
  },
  "completion_score": 30,
  "progress": { "current_step": 3, "total_steps": 10, "completion_percentage": 30 }
}
```

**Errors:**
- `400 BAD_REQUEST`: Empty answer (BRIEFING-003)
- `404 NOT_FOUND`: Session/question not found (BRIEFING-001)
- `401 UNAUTHORIZED`: Invalid token

---

#### 8. Complete Briefing (Public)

```java
@PostMapping("/briefing/{sessionId}/complete")
@Operation(summary = "Complete briefing (public)")
public ResponseEntity<BriefingCompletedResponse> completeBriefingPublic(
    @PathVariable UUID sessionId,
    @RequestParam String token
) {
    // Verify token matches session
    // Detect gaps, calculate completion score
    // Call: briefingService.completeBriefing(sessionId, score)
    // Return: 200 OK or 409 if incomplete
}
```

---

## Error Handling (BriefingExceptionHandler)

All exceptions return **Problem Details (RFC 9457)**:

```java
@RestControllerAdvice
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class BriefingExceptionHandler {

    @ExceptionHandler(BriefingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBriefingNotFound(
        BriefingNotFoundException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.scopeflow.com/errors/briefing-not-found"));
        problem.setTitle("Briefing Not Found");
        problem.setProperty("error_code", "BRIEFING-001");
        problem.setProperty("error_id", UUID.randomUUID().toString());
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(BriefingAlreadyCompletedException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyCompleted(
        BriefingAlreadyCompletedException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.scopeflow.com/errors/briefing-already-completed"));
        problem.setTitle("Briefing Already Completed");
        problem.setProperty("error_code", "BRIEFING-002");
        problem.setProperty("error_id", UUID.randomUUID().toString());
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    // Similar handlers for other exceptions (BRIEFING-003, 004, 005)
}
```

---

## DTOs (Immutable Records)

```java
// Request DTOs
public record StartBriefingRequest(
    @NotNull UUID client_id,
    @NotNull String service_type
) {}

public record SubmitAnswerRequest(
    @NotNull UUID question_id,
    @NotBlank String answer_text
) {}

public record AbandonBriefingRequest(
    String reason
) {}

public record CompleteBriefingRequest(
    boolean force_complete
) {}

// Response DTOs
public record BriefingSessionResponse(
    UUID session_id,
    UUID client_id,
    String service_type,
    String status,
    int completion_score,
    BriefingProgressDto progress,
    Instant created_at,
    Instant updated_at
) {}

public record CurrentQuestionResponse(
    UUID session_id,
    BriefingQuestionDto current_question,
    BriefingProgressDto progress,
    List<BriefingAnswerDto> previous_answers
) {}

public record SubmitAnswerResponse(
    UUID answer_id,
    boolean answer_submitted,
    BriefingQuestionDto follow_up_question,
    BriefingQuestionDto next_question,
    int completion_score,
    BriefingProgressDto progress
) {}

public record BriefingCompletedResponse(
    UUID session_id,
    String status,
    int completion_score,
    List<String> gaps_identified,
    boolean ready_for_scope_generation,
    Instant completed_at
) {}

// Nested DTOs
public record BriefingProgressDto(
    int current_step,
    int total_steps,
    int completion_percentage
) {}

public record BriefingQuestionDto(
    UUID question_id,
    String question_text,
    String question_type,
    int step,
    int total_steps
) {}

public record BriefingAnswerDto(
    UUID answer_id,
    UUID question_id,
    String question_text,
    String answer_text,
    boolean follow_up_generated,
    Map<String, Object> ai_analysis,
    Instant answered_at
) {}
```

---

## OpenAPI Configuration

Add to `OpenApiConfig.java`:

```java
@Bean
public OpenAPI briefingOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("ScopeFlow Briefing API")
            .version("1.0.0")
            .description("AI-assisted discovery flow for service providers")
        )
        .addServersItem(new Server()
            .url("http://localhost:8080")
            .description("Development")
        )
        .addServersItem(new Server()
            .url("https://api-staging.scopeflow.com")
            .description("Staging")
        )
        .addServersItem(new Server()
            .url("https://api.scopeflow.com")
            .description("Production")
        );
}
```

---

## Deliverables Checklist

When done, Step 4 is complete if:

- [ ] File created: `BriefingControllerV1.java` (~300 lines, 5 admin endpoints)
- [ ] File created: `PublicBriefingControllerV1.java` (~250 lines, 3 public endpoints)
- [ ] File created: `BriefingExceptionHandler.java` (~150 lines, 5 exception handlers)
- [ ] All DTOs created (Request/Response records)
- [ ] All endpoints documented with @Operation, @Schema, @Parameter
- [ ] All responses use Problem Details (RFC 9457)
- [ ] Public endpoints use token-based access (no JWT)
- [ ] Admin endpoints protected with JWT auth
- [ ] Multi-tenancy enforced (workspace_id scoping)
- [ ] All endpoints are stubs (throw UnsupportedOperationException)
- [ ] OpenAPI annotations complete
- [ ] Code compiles: `./mvnw compile`
- [ ] Swagger UI accessible at: http://localhost:8080/swagger-ui.html
- [ ] Committed: `feat(api-designer): briefing-rest-controllers-openapi`
- [ ] Output summary: `.claude/plans/API-DESIGNER-OUTPUT-Step4-Briefing.md`

---

## Reference Materials

- **Terminal 1 API:** `backend/src/main/java/com/scopeflow/adapter/in/web/WorkspaceControllerV1.java`
- **ADR-002:** `docs/architecture/adr/ADR-002-briefing-domain.md`
- **OpenAPI 3.1:** https://spec.openapis.org/oas/v3.1.0
- **Problem Details RFC 9457:** https://www.rfc-editor.org/rfc/rfc9457
- **Project CLAUDE.md:** `./CLAUDE.md` (API design conventions)

---

## Git Workflow

1. Branch: `feature/sprint-1b-briefing-domain`
2. Create: `BriefingControllerV1.java`, `PublicBriefingControllerV1.java`, `BriefingExceptionHandler.java`
3. Compile: `./mvnw compile`
4. Verify Swagger UI: http://localhost:8080/swagger-ui.html
5. Commit: `feat(api-designer): briefing-rest-controllers-openapi`
6. Push to origin

---

## Timeline

**Start:** After DBA Step 3 ✅
**Duration:** ~1-2 days
**Next:** DevOps-Engineer (Step 5)

---

**Ready. Design the REST API for Briefing domain.** 🌐
