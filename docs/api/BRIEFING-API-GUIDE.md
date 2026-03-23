# Briefing API Guide

**Version:** 1.0.0
**Base URL:** `http://localhost:8080` (dev) | `https://api.scopeflow.com` (prod)
**OpenAPI Spec:** `docs/api/briefing-api.yaml`

---

## Overview

The Briefing API enables AI-assisted discovery flows for B2B service providers. It transforms scattered WhatsApp conversations into structured, approved briefings ready for scope generation.

**Key Features:**
- Sequential question flow (no skip)
- AI-powered gap detection
- Auto-generated follow-up questions (max 1 per question)
- Immutable answer audit trail
- Public access for clients (no auth required)
- Workspace-scoped for service providers

---

## Authentication

### Authenticated Endpoints (Workspace owners/members)

All `/api/v1/briefings` endpoints require JWT authentication.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**JWT Claims:**
- `workspace_id`: UUID (extracted by backend)
- `user_id`: UUID
- `role`: OWNER | ADMIN | MEMBER

**Rate Limit:** 100 req/min per user

### Public Endpoints (Clients)

All `/public/briefings/{publicToken}` endpoints are **public** (no auth required).

**Rate Limit:** 10 req/min per IP

---

## Error Handling (RFC 9457)

All errors follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457).

**Error Response Structure:**
```json
{
  "type": "https://api.scopeflow.com/errors/briefing-not-found",
  "title": "Briefing Not Found",
  "status": 404,
  "detail": "Briefing session with ID 7c9e6679-7425-40de-944b-e07fc1f90ae7 was not found",
  "instance": "/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "errorCode": "BRIEFING-001",
  "errorId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-22T14:30:00Z"
}
```

### Error Codes

| Code | HTTP | Meaning | Example |
|------|------|---------|---------|
| `BRIEFING-001` | 404 | Briefing session not found | Invalid briefing ID or unauthorized access |
| `BRIEFING-002` | 409 | Briefing already completed | Cannot modify locked briefing |
| `BRIEFING-003` | 400 | Invalid answer | Empty or too long answer text |
| `BRIEFING-004` | 422 | Max follow-up exceeded | Already 1 follow-up for this question |
| `BRIEFING-005` | 422 | Incomplete briefing | Completion score < 80% |
| `BRIEFING-006` | 409 | Briefing already in progress | Duplicate active session for client+service |
| `BRIEFING-007` | 409 | Invalid state | Session not in progress (e.g., abandoned) |
| `VALIDATION-400` | 400 | Request validation failed | Missing required field or invalid format |
| `AUTH-401` | 401 | Unauthorized | Missing or invalid JWT token |
| `RATE-429` | 429 | Rate limit exceeded | Too many requests in time window |
| `INTERNAL-500` | 500 | Internal server error | Unexpected error (contact support) |

---

## Endpoints

### 1. Create Briefing Session

**POST** `/api/v1/briefings`

Creates a new discovery flow for a client and service type.

**Invariant:** Only 1 active briefing per client per service type per workspace.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/briefings \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "550e8400-e29b-41d4-a716-446655440000",
    "serviceType": "SOCIAL_MEDIA"
  }'
```

**Response (201 Created):**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "serviceType": "SOCIAL_MEDIA",
  "status": "IN_PROGRESS",
  "publicToken": "9f8c7d6e-5b4a-3210-9876-543210fedcba",
  "completionScore": null,
  "createdAt": "2026-03-22T10:00:00Z",
  "updatedAt": "2026-03-22T10:00:00Z"
}
```

**Headers:**
```
Location: /api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7
X-Rate-Limit-Remaining: 95
```

**Errors:**
- `400`: Validation failed (missing clientId or serviceType)
- `401`: Unauthorized (missing JWT)
- `409`: Duplicate active briefing (BRIEFING-006)

---

### 2. List Briefings (Paginated)

**GET** `/api/v1/briefings`

Returns paginated list of briefings for workspace.

**Query Parameters:**
- `status`: Filter by status (`IN_PROGRESS`, `COMPLETED`, `ABANDONED`)
- `serviceType`: Filter by service type (`SOCIAL_MEDIA`, `LANDING_PAGE`, etc.)
- `createdAfter`: Filter by creation date (ISO 8601)
- `page`: Page number (zero-based, default 0)
- `size`: Page size (default 20, max 100)
- `sort`: Sort field and direction (default `createdAt,desc`)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/briefings?status=IN_PROGRESS&page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer <jwt_token>"
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
      "clientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "serviceType": "SOCIAL_MEDIA",
      "status": "IN_PROGRESS",
      "publicToken": "9f8c7d6e-5b4a-3210-9876-543210fedcba",
      "completionScore": null,
      "createdAt": "2026-03-22T10:00:00Z",
      "updatedAt": "2026-03-22T14:30:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

---

### 3. Get Briefing Details

**GET** `/api/v1/briefings/{briefingId}`

Returns full details of a briefing session (progress, questions, answers).

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7 \
  -H "Authorization: Bearer <jwt_token>"
```

**Response (200 OK):**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "serviceType": "SOCIAL_MEDIA",
  "status": "IN_PROGRESS",
  "publicToken": "9f8c7d6e-5b4a-3210-9876-543210fedcba",
  "completionScore": null,
  "createdAt": "2026-03-22T10:00:00Z",
  "updatedAt": "2026-03-22T14:30:00Z",
  "progress": {
    "currentStep": 7,
    "totalSteps": 10,
    "completionPercentage": 70,
    "gapsIdentified": ["Need more details on target audience"]
  },
  "questions": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "text": "What are your main goals for this social media campaign?",
      "step": 1,
      "questionType": "OPEN_ENDED",
      "required": true,
      "followUpGenerated": false
    }
  ],
  "answers": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "questionId": "550e8400-e29b-41d4-a716-446655440001",
      "answerText": "We want to increase brand awareness and generate leads.",
      "qualityScore": 85,
      "createdAt": "2026-03-22T11:15:00Z"
    }
  ]
}
```

**Errors:**
- `401`: Unauthorized
- `404`: Briefing not found (BRIEFING-001)

---

### 4. Get Progress Metrics

**GET** `/api/v1/briefings/{briefingId}/progress`

Returns completion progress (step count, percentage, gaps).

**Cache:** 30 seconds

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7/progress \
  -H "Authorization: Bearer <jwt_token>"
```

**Response (200 OK):**
```json
{
  "currentStep": 7,
  "totalSteps": 10,
  "completionPercentage": 70,
  "gapsIdentified": [
    "Need more details on target audience",
    "Budget range missing"
  ]
}
```

**Headers:**
```
Cache-Control: max-age=30, must-revalidate
```

---

### 5. Get Next Question

**GET** `/api/v1/briefings/{briefingId}/next-question`

Returns the next sequential question to answer.

**Invariant:** Questions are sequential (no skip).

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7/next-question \
  -H "Authorization: Bearer <jwt_token>"
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "text": "Who is your target audience?",
  "step": 8,
  "questionType": "OPEN_ENDED",
  "required": true,
  "followUpGenerated": false
}
```

**Errors:**
- `404`: Briefing not found
- `409`: Session not in progress or no more questions (BRIEFING-007)

---

### 6. Submit Answer

**POST** `/api/v1/briefings/{briefingId}/answers`

Submits a client answer to a briefing question.

**Invariants:**
- Answer text cannot be empty
- Max 1 follow-up per question

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7/answers \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": "550e8400-e29b-41d4-a716-446655440003",
    "answerText": "Our target audience is wellness enthusiasts aged 25-45 in urban areas."
  }'
```

**Response (204 No Content)**

**Errors:**
- `400`: Validation failed (empty answerText) (BRIEFING-003)
- `404`: Briefing or question not found
- `409`: Session not in progress (BRIEFING-002)
- `422`: Max follow-up exceeded (BRIEFING-004)

---

### 7. Complete Briefing

**POST** `/api/v1/briefings/{briefingId}/complete`

Marks briefing as completed (locks it for scope generation).

**Invariant:** Completion score must be >= 80% and no critical gaps.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7/complete \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "completionScore": 95,
    "gapsIdentified": []
  }'
```

**Response (200 OK):**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "serviceType": "SOCIAL_MEDIA",
  "status": "COMPLETED",
  "publicToken": "9f8c7d6e-5b4a-3210-9876-543210fedcba",
  "completionScore": 95,
  "createdAt": "2026-03-22T10:00:00Z",
  "updatedAt": "2026-03-22T15:00:00Z"
}
```

**Errors:**
- `404`: Briefing not found
- `409`: Briefing already completed (BRIEFING-002)
- `422`: Completion score < 80% (BRIEFING-005)

---

### 8. Abandon Briefing

**POST** `/api/v1/briefings/{briefingId}/abandon`

Abandons an in-progress briefing. Client can start a new one later.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/briefings/7c9e6679-7425-40de-944b-e07fc1f90ae7/abandon \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Client decided to postpone project"
  }'
```

**Response (204 No Content)**

**Errors:**
- `404`: Briefing not found
- `409`: Cannot abandon completed briefing (BRIEFING-002)

---

## Public Endpoints (Client-Facing)

### 9. Get Public Briefing

**GET** `/public/briefings/{publicToken}`

Client-facing endpoint. Returns briefing details via public token (no sensitive data).

**No authentication required.**

**Request:**
```bash
curl -X GET http://localhost:8080/public/briefings/9f8c7d6e-5b4a-3210-9876-543210fedcba
```

**Response (200 OK):**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "serviceType": "SOCIAL_MEDIA",
  "status": "IN_PROGRESS",
  "progress": {
    "currentStep": 7,
    "totalSteps": 10,
    "completionPercentage": 70,
    "gapsIdentified": []
  },
  "createdAt": "2026-03-22T10:00:00Z"
}
```

**Headers:**
```
X-Rate-Limit-Remaining: 8
```

**Errors:**
- `404`: Briefing not found or token invalid
- `429`: Rate limit exceeded (10 req/min per IP)

---

### 10. Get Public Next Question

**GET** `/public/briefings/{publicToken}/next-question`

Client-facing endpoint. Returns next question in the flow.

**No authentication required.**

**Request:**
```bash
curl -X GET http://localhost:8080/public/briefings/9f8c7d6e-5b4a-3210-9876-543210fedcba/next-question
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "text": "Who is your target audience?",
  "step": 8,
  "questionType": "OPEN_ENDED",
  "required": true,
  "followUpGenerated": false
}
```

**Errors:**
- `404`: Briefing not found or token invalid
- `409`: Session not in progress or no more questions

---

### 11. Submit Public Answer

**POST** `/public/briefings/{publicToken}/answers`

Client-facing endpoint. Submits an answer to a question.

**No authentication required.**

**Request:**
```bash
curl -X POST http://localhost:8080/public/briefings/9f8c7d6e-5b4a-3210-9876-543210fedcba/answers \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": "550e8400-e29b-41d4-a716-446655440003",
    "answerText": "Our target audience is wellness enthusiasts aged 25-45 in urban areas."
  }'
```

**Response (204 No Content)**

**Errors:**
- `400`: Validation failed (empty answerText)
- `404`: Briefing not found or token invalid
- `422`: Max follow-up exceeded

---

## Critical Flow Example

### Complete Briefing Flow (Workspace Owner + Client)

**Step 1: Workspace owner creates briefing**
```bash
# POST /api/v1/briefings (authenticated)
{
  "clientId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceType": "SOCIAL_MEDIA"
}
# Response: publicToken = 9f8c7d6e-5b4a-3210-9876-543210fedcba
```

**Step 2: Workspace owner shares public link with client**
```
https://app.scopeflow.com/briefing/9f8c7d6e-5b4a-3210-9876-543210fedcba
```

**Step 3: Client accesses briefing (no auth)**
```bash
# GET /public/briefings/{publicToken}
```

**Step 4: Client gets first question (no auth)**
```bash
# GET /public/briefings/{publicToken}/next-question
```

**Step 5: Client submits answer (no auth)**
```bash
# POST /public/briefings/{publicToken}/answers
{
  "questionId": "...",
  "answerText": "We want to increase brand awareness..."
}
```

**Step 6: Repeat steps 4-5 until all questions answered**

**Step 7: Workspace owner checks progress (authenticated)**
```bash
# GET /api/v1/briefings/{briefingId}/progress
# Response: completionPercentage = 100, gapsIdentified = []
```

**Step 8: Workspace owner completes briefing (authenticated)**
```bash
# POST /api/v1/briefings/{briefingId}/complete
{
  "completionScore": 95,
  "gapsIdentified": []
}
# Response: status = "COMPLETED"
```

**Step 9: Briefing is locked → ready for scope generation**

---

## Rate Limits

| Endpoint Type | Limit | Scope |
|--------------|-------|-------|
| **Authenticated** (`/api/v1/briefings`) | 100 req/min | Per user (JWT) |
| **Public** (`/public/briefings`) | 10 req/min | Per IP address |

**Rate Limit Headers:**
```
X-Rate-Limit-Remaining: 95
```

**Rate Limit Exceeded (429):**
```json
{
  "type": "https://api.scopeflow.com/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "Rate limit of 10 requests/minute exceeded. Try again in 45 seconds.",
  "instance": "/public/briefings/9f8c7d6e-5b4a-3210-9876-543210fedcba",
  "errorCode": "RATE-429",
  "errorId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-22T14:30:00Z"
}
```

---

## Integration with Step 5 (Backend-Dev)

**Controllers:**
- `BriefingControllerV1.java` — 8 authenticated endpoints (skeleton ready)
- `PublicBriefingControllerV1.java` — 3 public endpoints (skeleton ready)

**DTOs:**
- `CreateBriefingRequest`, `SubmitAnswerRequest`, `CompleteBriefingRequest`, `AbandonBriefingRequest`
- `BriefingResponse`, `BriefingDetailResponse`, `PublicBriefingResponse`, `ProgressResponse`, `QuestionResponse`, `AnswerResponse`
- `PageResponse<T>` — Generic pagination DTO

**Mapper:**
- `BriefingMapper` interface — defines conversion contracts

**Exception Mapping:**
- `GlobalExceptionHandler` — already has all 7 briefing exception handlers

**Step 5 TODO:**
1. Implement controller methods (use domain service + mapper)
2. Create JPA entities + repositories
3. Implement `BriefingMapper` (convert domain ↔ DTO)
4. Add rate limiting annotations (`@RateLimit`)
5. Add caching annotations (`@Cacheable` on progress endpoint)
6. Write 50+ integration tests (Testcontainers)

---

## Testing

**Import OpenAPI spec into Swagger UI:**
```bash
# Start backend
./mvnw spring-boot:run

# Open Swagger UI
http://localhost:8080/swagger-ui.html
```

**Manual API testing with curl:**
```bash
# Get JWT token first (from auth endpoint)
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Create briefing
curl -X POST http://localhost:8080/api/v1/briefings \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"550e8400-e29b-41d4-a716-446655440000","serviceType":"SOCIAL_MEDIA"}'
```

**Integration tests (Step 5):**
```bash
./mvnw test -Dtest=BriefingControllerV1IntegrationTest
```

---

## Support

**Issues:** Create ticket with `error_id` from error response
**OpenAPI Spec:** `docs/api/briefing-api.yaml`
**Contact:** api@scopeflow.com
