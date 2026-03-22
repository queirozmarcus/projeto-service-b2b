# API Designer Output — Step 4: Briefing REST Endpoints

**Date:** 2026-03-22
**Agent:** api-designer (Claude Sonnet)
**Task:** Design and implement REST API for Briefing bounded context
**Status:** ✅ Complete (stubs only, ready for backend implementation)

---

## Summary

Implemented **3 controllers** with **8 REST endpoints** for the Briefing domain:

1. **BriefingControllerV1** (admin endpoints) — 5 endpoints
2. **PublicBriefingControllerV1** (client-facing, token-based) — 3 endpoints
3. **GlobalExceptionHandler** (extended with 7 briefing exception handlers)

All endpoints follow RFC 9457 Problem Details, OpenAPI 3.1 documentation, and multi-tenant workspace scoping.

---

## Files Created

### 1. BriefingControllerV1 (Admin Endpoints)

**File:** `backend/src/main/java/com/scopeflow/adapter/in/web/BriefingControllerV1.java`
**Lines:** ~380
**Purpose:** Admin-facing endpoints for managing briefing sessions

#### Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/workspaces/{workspaceId}/briefing` | Start new briefing session | JWT |
| `GET` | `/api/v1/workspaces/{workspaceId}/briefing` | List briefing sessions (paginated) | JWT |
| `GET` | `/api/v1/workspaces/{workspaceId}/briefing/{sessionId}` | Get briefing details | JWT |
| `POST` | `/api/v1/workspaces/{workspaceId}/briefing/{sessionId}/complete` | Complete briefing | JWT |
| `POST` | `/api/v1/workspaces/{workspaceId}/briefing/{sessionId}/abandon` | Abandon briefing | JWT |

#### DTOs (Immutable Records)

**Request DTOs:**
- `StartBriefingRequest` (client_id, service_type)
- `CompleteBriefingRequest` (force_complete)
- `AbandonBriefingRequest` (reason)

**Response DTOs:**
- `StartBriefingResponse` (session_id, public_token, first_question, progress)
- `BriefingSessionResponse` (session summary for list)
- `BriefingSessionDetailResponse` (full briefing with all answers)
- `BriefingCompletedResponse` (completion summary)
- `BriefingAbandonedResponse` (abandonment confirmation)
- `BriefingListResponse` (paginated list)

**Nested DTOs:**
- `BriefingProgressDto` (current_step, total_steps, completion_percentage)
- `BriefingQuestionDto` (question_id, question_text, question_type, step, total_steps)
- `BriefingAnswerDto` (answer_id, question_id, answer_text, follow_up_generated, ai_analysis, answered_at)

---

### 2. PublicBriefingControllerV1 (Client Endpoints)

**File:** `backend/src/main/java/com/scopeflow/adapter/in/web/PublicBriefingControllerV1.java`
**Lines:** ~280
**Purpose:** Client-facing endpoints using public token authentication (no JWT)

#### Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/briefing/{sessionId}?token={token}` | Get current question | Token |
| `POST` | `/api/v1/briefing/{sessionId}/answers?token={token}` | Submit answer | Token |
| `POST` | `/api/v1/briefing/{sessionId}/complete?token={token}` | Complete briefing (public) | Token |

---

### 3. GlobalExceptionHandler (Extended)

**File:** `backend/src/main/java/com/scopeflow/adapter/in/web/GlobalExceptionHandler.java`
**Lines:** +140 (extended existing file)
**Purpose:** Centralized exception handling for all Briefing domain exceptions

#### Briefing Exception Handlers Added

| Exception | Status Code | Error Code |
|-----------|-------------|------------|
| `BriefingNotFoundException` | 404 NOT_FOUND | BRIEFING-001 |
| `BriefingAlreadyCompletedException` | 409 CONFLICT | BRIEFING-002 |
| `InvalidAnswerException` | 400 BAD_REQUEST | BRIEFING-003 |
| `MaxFollowupExceededException` | 409 CONFLICT | BRIEFING-004 |
| `IncompleteGapsException` | 409 CONFLICT | BRIEFING-005 |
| `BriefingAlreadyInProgressException` | 409 CONFLICT | BRIEFING-006 |
| `InvalidStateException` | 409 CONFLICT | BRIEFING-007 |

---

## API Contracts (Examples)

### Start Briefing (Admin)

```http
POST /api/v1/workspaces/{workspaceId}/briefing
Authorization: Bearer {jwt}

{
  "client_id": "550e8400-e29b-41d4-a716-446655440000",
  "service_type": "SOCIAL_MEDIA"
}

Response: 201 CREATED
{
  "session_id": "a7b8c9d0-e1f2-3456-7890-abcdef123456",
  "public_token": "xxx-xxx-xxx",
  "first_question": { ... },
  "progress": { ... }
}
```

### Submit Answer (Public)

```http
POST /api/v1/briefing/{sessionId}/answers?token={token}

{
  "question_id": "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
  "answer_text": "Marketing professionals aged 25-45"
}

Response: 200 OK
{
  "answer_id": "...",
  "answer_submitted": true,
  "follow_up_question": { ... },
  "next_question": { ... },
  "completion_score": 30,
  "progress": { ... }
}
```

---

## Design Decisions

1. **Controller Separation:** Admin (JWT) vs Public (token) for security
2. **Immutable DTOs:** Java 21 records with validation
3. **Centralized Exceptions:** All domain errors in GlobalExceptionHandler
4. **OpenAPI Inline:** Documentation lives with code
5. **Multi-Tenancy:** Workspace scoping enforced in URLs

---

## Swagger UI

- **Development:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/v3/api-docs

---

## Checklist ✅

- [x] BriefingControllerV1 created (5 endpoints)
- [x] PublicBriefingControllerV1 created (3 endpoints)
- [x] GlobalExceptionHandler extended (7 handlers)
- [x] 20+ DTOs with validation
- [x] OpenAPI annotations complete
- [x] Problem Details (RFC 9457)
- [x] Multi-tenancy enforced
- [x] All endpoints are stubs

---

**Status:** ✅ Complete
**Next:** DevOps Engineer (Step 5)
