# Fork 1: Architect Context — Briefing Domain ADR-002

**Agent:** architect (sonnet/opus)
**Task:** Design Briefing Domain architecture
**Input:** TERMINAL2-BRIEFING-HANDOFF.md (full spec above)
**Output:** ADR-002-briefing-domain.md
**Timeline:** ~1 day
**Branch:** feature/sprint-1b-briefing-domain

---

## Your Responsibility

Design the **Briefing bounded context** with:

1. **Sealed class hierarchy** (type safety, state pattern)
   - BriefingSession (BriefingInProgress | BriefingCompleted | BriefingAbandoned)
   - BriefingAnswer (AnsweredDirect | AnsweredWithFollowup)
   - Immutability and safe transitions between states

2. **Value objects** (records, compact constructors)
   - BriefingSessionId, QuestionId, AnswerId, BriefingProgress, CompletionScore, AIGeneration
   - Validation in compact constructors

3. **Domain services** (business logic, invariants enforcement)
   - BriefingService: startBriefing, getNextQuestion, submitAnswer, detectGaps, generateFollowUp, completeBriefing, abandonBriefing
   - Services enforce all 7 invariants

4. **Domain events** (async communication)
   - BriefingSessionStarted, QuestionAsked, AnswerSubmitted, FollowupQuestionGenerated, BriefingCompleted, BriefingAbandoned
   - Enable Outbox pattern → Kafka/RabbitMQ

5. **Repository interfaces** (ports, no JPA)
   - BriefingSessionRepository, BriefingQuestionRepository, BriefingAnswerRepository, AIGenerationRepository

6. **Domain exceptions** (stable error codes)
   - BriefingNotFoundException, BriefingAlreadyCompletedException, InvalidAnswerException, MaxFollowupExceededException, IncompleteGapsException
   - Error codes: BRIEFING-001 through BRIEFING-005

7. **Invariants** (must be enforceable)
   - Sequential questions (no skip)
   - No empty answers
   - Max 1 follow-up per question
   - Completion >= 80% + no critical gaps
   - Immutable answers (no edit)
   - Unique public_token per session
   - Single active briefing per client per service type

8. **Communication patterns**
   - Async IN: UserRegistered (cache user name), WorkspaceMemberInvited (notify)
   - Async OUT: BriefingCompleted → Proposal context
   - Sync queries: Get workspace, Generate scope

---

## Deliverable: ADR-002

Create `docs/architecture/adr/ADR-002-briefing-domain.md` with:
- Decision: Why sealed classes, why records, why Outbox pattern
- Consequences: Trade-offs, what becomes harder/easier
- Rationale: Domain-driven design, Java 21 features, event sourcing
- Alternatives considered: JPA @Inheritance, plain classes, etc.
- Implementation notes: Repository design, Spring integration points

---

## Context From Terminal 1 (Reference)

You have access to ADR-001 (User & Workspace) as a template:
- Path: `docs/architecture/adr/ADR-001-user-workspace-service.md`
- Pattern: Same structure, but for Briefing domain
- Tech: Same stack (Java 21, Spring Boot 3.2, sealed classes, records)

---

## Git Setup

- New branch: `feature/sprint-1b-briefing-domain` (off main)
- Directory: `src/main/java/com/scopeflow/core/domain/briefing/`
- Commit: "feat(architect): adr-002-briefing-domain-architecture"
- Push: Ready for backend-dev to pull

---

## Output Template

When done, update: `.claude/plans/ARCHITECT-OUTPUT-Step1-Briefing.md`

With:
- [ ] ADR-002 created and committed
- [ ] Sealed class hierarchy finalized
- [ ] Domain services and invariants documented
- [ ] Communication patterns defined
- [ ] Ready for backend-dev to implement

---

**Next:** Backend-Dev fork receives ADR-002 + starts implementation
