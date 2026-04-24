# Sprint 1 Terminal 2: Briefing Domain Parallel Orchestration

**Date:** 2026-03-22
**Mode:** Full Orchestration Parallel (Terminal 1 + Terminal 2 simultaneous)
**Timeline:** ~2 weeks (parallel with Terminal 1)
**Status:** 🟢 READY TO DELEGATE

---

## Context

Terminal 1 (User & Workspace Domain) is **100% COMPLETE** and ready for consolidation.

Terminal 2 (Briefing Domain) now starts **in parallel** with full orchestration:
- Does NOT depend on Terminal 1 completion
- Can begin immediately with context isolation
- Will follow same 6-step process: Architect → Backend-Dev → DBA → API-Designer → DevOps → Consolidation

---

## 🎯 Bounded Context: Briefing

### Responsibility

> *Manage AI-assisted discovery flow where clients answer structured questions, with system detecting gaps and generating follow-ups, producing structured briefing for scope generation.*

### Data Ownership

**Exclusively owned tables:**

```
briefing_sessions (parent aggregate)
├── id (UUID, PK)
├── workspace_id (FK → workspaces)
├── client_id (FK → clients)
├── service_type (FK → service_catalog)
├── status (IN_PROGRESS, COMPLETED, ABANDONED)
├── public_token (for client access)
├── completion_score (0-100, gaps identified)
├── ai_analysis (JSONB: summary of gaps, recommendations)
├── created_at, updated_at

briefing_questions (questions for this service type)
├── id (UUID, PK)
├── briefing_session_id (FK → briefing_sessions)
├── question_text (string)
├── step (order in sequence)
├── question_type (OPEN, MULTIPLE_CHOICE, etc.)
├── ai_prompt_version (v1, v2, etc.)
├── required (boolean)
├── created_at

briefing_answers (responses from client)
├── id (UUID, PK)
├── briefing_session_id (FK → briefing_sessions)
├── question_id (FK → briefing_questions)
├── answer_text (string)
├── answer_json (JSONB: structured response)
├── follow_up_generated (boolean)
├── ai_analysis (JSONB: quality score, confidence, gaps)
├── created_at

ai_generations (audit trail of IA outputs)
├── id (UUID, PK)
├── briefing_session_id (FK → briefing_sessions)
├── generation_type (FOLLOW_UP_QUESTION, GAP_ANALYSIS, COMPLETION_SUMMARY)
├── input_json (question + previous answers)
├── output_json (AI response)
├── prompt_version (v1, v2, etc. for reproducibility)
├── latency_ms (performance tracking)
├── cost_usd (token cost from OpenAI)
├── created_at
```

---

## Domain Model

### Sealed Classes (Type Safety)

```java
BriefingSession (sealed)
├── BriefingInProgress (can add answers, ask questions)
├── BriefingCompleted (locked, can generate scope)
└── BriefingAbandoned (user gave up, can restart)

BriefingAnswer (sealed)
├── AnsweredDirect (single response, no follow-up)
└── AnsweredWithFollowup (response + generated follow-up question)
```

### Value Objects (Records)

```java
BriefingSessionId (wrapped UUID)
QuestionId (wrapped UUID)
AnswerId (wrapped UUID)

BriefingProgress (current_step: int, total_steps: int, completion_percentage: int)
AIGeneration (type, input_json, output_json, prompt_version, latency_ms)
CompletionScore (score: 0-100, gaps_identified: List<String>)
```

### Domain Events

```java
BriefingSessionStarted (when client begins)
QuestionAsked (when system asks next question)
AnswerSubmitted (when client responds)
FollowupQuestionGenerated (when AI identifies gap)
BriefingCompleted (when all questions answered)
BriefingAbandoned (when user gives up)
```

### Domain Services

```java
BriefingService
├── startBriefing(workspaceId, clientId, serviceType) → BriefingInProgress
├── getNextQuestion(briefingSessionId) → BriefingQuestion
├── submitAnswer(briefingSessionId, answerId, answerText) → void
├── detectGaps(briefingSessionId) → CompletionScore
├── generateFollowUp(briefingSessionId) → Optional<BriefingQuestion>
├── completeBriefing(briefingSessionId) → BriefingCompleted
└── abandonBriefing(briefingSessionId) → BriefingAbandoned
```

---

## Invariants

1. **Sequential Questions:** Cannot skip questions (must answer in order)
2. **No Empty Answers:** Blank responses not allowed
3. **Max 1 Follow-up per Question:** System generates at most 1 follow-up per response
4. **Completion Score:** Cannot complete if completion < 80% or critical gaps remain
5. **Immutable Answers:** Once submitted, answers cannot be edited (audit trail)
6. **Public Token:** Each briefing has unique public_token (for client access)
7. **Single Active Briefing:** Only 1 active briefing per client per service type

---

## Communication with Other Contexts

### User & Workspace → Briefing

**Async (events):**
- `UserRegistered` event → cache user name in Briefing context
- `WorkspaceMemberInvited` event → update notification preferences

**Sync (query):**
- "Get workspace for briefing session" (lookup owner info)

### Briefing → Proposal (Next Terminal)

**Async (events):**
- `BriefingCompleted` → Proposal context consumes and suggests scope generation

**Sync (query):**
- "Generate scope from briefing" (proposal context calls back)

---

## API Endpoints (Planned)

```
# Start briefing session
POST /api/v1/workspaces/{id}/briefing
  Body: { client_id, service_type }
  Response: { session_id, public_token, first_question }

# Public: client accesses via token
GET /api/v1/briefing/{session_id}?token={public_token}
  Response: { current_question, progress, answers_so_far }

# Submit answer
POST /api/v1/briefing/{session_id}/answers?token={public_token}
  Body: { question_id, answer_text }
  Response: { follow_up_question (if any), next_question, completion_score }

# Complete briefing
POST /api/v1/briefing/{session_id}/complete?token={public_token}
  Response: { completion_score, gaps, ready_for_scope_generation }

# List briefings (admin)
GET /api/v1/workspaces/{id}/briefing?status=COMPLETED
  Response: { briefing_sessions[] }
```

---

## Execution Plan (Same as Terminal 1)

### Step 1: Architect
- Define Briefing responsibility + communication patterns
- Create ADR-002: Briefing Domain Architecture
- Input: This handoff document
- Output: Design documentation + architecture decisions

### Step 2: Backend-Dev
- Create domain entities (sealed classes)
- Create value objects (records)
- Create domain services
- Create repository interfaces
- Create unit test skeleton
- Deliverable: 7+ sealed classes, 5+ value objects, 2+ services, 50+ tests

### Step 3: DBA
- Create Flyway V3 migration (briefing tables)
- Add indexes (session_id, status, created_at, completion_score)
- Outbox table integration for events
- Deliverable: 4 core tables, 15+ indexes, audit trail

### Step 4: API-Designer
- Create BriefingController (REST endpoints)
- Create PublicBriefingController (token-based access for clients)
- Create OpenAPI documentation
- Error handling (Briefing-specific exceptions)
- Deliverable: 2 controllers, 8+ endpoints, OpenAPI

### Step 5: DevOps-Engineer
- Docker image (multi-stage)
- Helm chart values for Briefing service
- CI/CD (GitHub Actions)
- Deliverable: Production-ready deployment

### Step 6: Consolidation
- Merge branches, run full test suite
- Code review (dev-review)
- Tag release (if consolidating all 3 terminals)

---

## Estimated Deliverables

| Item | Target | Expected |
|------|--------|----------|
| Sealed Classes | 3+ | 3-4 |
| Value Objects (Records) | 5+ | 5-6 |
| Domain Services | 2+ | 2-3 |
| Repository Interfaces | 3+ | 3 |
| Domain Events | 5+ | 5-6 |
| Domain Exceptions | 4+ | 4-5 |
| Unit Tests | 50+ | 50-60 |
| Integration Tests | 15+ | 15-20 |
| REST Endpoints | 8+ | 8-10 |
| Flyway Tables | 4 | 4 |
| Indexes | 15+ | 15+ |

---

## Timeline

**Start:** Now (parallel with Terminal 1 consolidation)
**Step 1 (Architect):** 1 day
**Step 2 (Backend-Dev):** 2-3 days
**Step 3 (DBA):** 1 day
**Step 4 (API-Designer):** 1-2 days
**Step 5 (DevOps):** 1 day
**Step 6 (Consolidation):** 1-2 days
**Total:** ~1 week (parallel execution faster than sequential)

---

## Context Isolation

Terminal 2 works independently:
- Separate git branch: `feature/sprint-1b-briefing-domain`
- Separate directory structure: `src/main/java/com/scopeflow/core/domain/briefing/`
- No conflicts with Terminal 1 code
- Merges into main branch after completion

---

## Next Actions

Authorize `/dev-bootstrap briefing-domain` to start Terminal 2 with full orchestration.

All 5 agents will work in parallel:
- architect → backend-dev → dba → api-designer → devops-engineer

Expected completion: ~1 week

Then Terminal 3 (Proposal Domain) begins when Terminal 2 ~80% complete.

