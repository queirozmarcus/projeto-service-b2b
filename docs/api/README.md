# Briefing API

**OpenAPI Spec:** [`briefing-api.yaml`](./briefing-api.yaml)

AI-assisted discovery API for B2B service providers. Transforms client conversations into structured, approved briefings.

---

## Quick Start

```bash
# Start backend
cd backend && ./mvnw spring-boot:run

# Open Swagger UI
http://localhost:8080/swagger-ui.html
```

**Example: Create briefing**
```bash
curl -X POST http://localhost:8080/api/v1/briefings \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"550e8400-...","serviceType":"SOCIAL_MEDIA"}'
```

---

## Authentication

| Endpoint | Auth | Rate Limit |
|----------|------|-----------|
| `/api/v1/briefings/*` | JWT Bearer token | 100 req/min |
| `/public/briefings/{token}/*` | None | 10 req/min per IP |

---

## Endpoints

### Authenticated (Workspace Owners)

| Method | Endpoint | Description |
|--------|---------|-------------|
| POST | `/api/v1/briefings` | Create briefing |
| GET | `/api/v1/briefings` | List briefings (paginated) |
| GET | `/api/v1/briefings/{id}` | Get details |
| GET | `/api/v1/briefings/{id}/progress` | Get progress |
| GET | `/api/v1/briefings/{id}/next-question` | Get next question |
| POST | `/api/v1/briefings/{id}/answers` | Submit answer |
| POST | `/api/v1/briefings/{id}/complete` | Complete briefing |
| POST | `/api/v1/briefings/{id}/abandon` | Abandon briefing |

### Public (Clients)

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | `/public/briefings/{token}` | Get briefing |
| GET | `/public/briefings/{token}/next-question` | Get next question |
| POST | `/public/briefings/{token}/answers` | Submit answer |

---

## Error Handling

All errors follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) with `errorCode` for automation.

**Key Error Codes:**
- `BRIEFING-001` (404) — Briefing not found
- `BRIEFING-002` (409) — Already completed (locked)
- `BRIEFING-003` (400) — Invalid answer
- `BRIEFING-006` (409) — Duplicate active briefing
- `AUTH-401` (401) — Unauthorized
- `RATE-429` (429) — Rate limit exceeded

See [`briefing-api.yaml`](./briefing-api.yaml) for complete error catalog.

---

## Features

- Sequential questions (no skip)
- AI gap detection
- Auto follow-up (max 1 per question)
- Public access for clients (no login)
- Workspace-scoped multi-tenancy

---

## Testing

```bash
# Integration tests
cd backend
./mvnw test -Dtest=BriefingControllerV1IntegrationTest

# Manual testing
http://localhost:8080/swagger-ui.html
```

---

**Architecture:** See [`../../CLAUDE.md`](../../CLAUDE.md)  
**Support:** api@scopeflow.com
