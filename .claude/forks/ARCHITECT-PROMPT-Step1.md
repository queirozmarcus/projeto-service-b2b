# Architect Delegation — Step 1: Briefing Domain ADR

**To:** architect (Claude Sonnet)
**From:** Marcus (Orchestrator)
**Date:** 2026-03-22
**Mode:** Fork isolated — your context window is clean, you own this output
**Task ID:** TERMINAL2-STEP1-ARCHITECT

---

## Mission

You are the **sole architect** for this bounded context. Your job is to design a production-grade domain model for the **Briefing Domain** following Domain-Driven Design, Java 21 sealed classes, and Spring Boot 3.2.

**Input:** Full briefing specification in `TERMINAL2-BRIEFING-HANDOFF.md`
**Output:** ADR-002-briefing-domain.md (decision record) + domain class skeleton
**Constraints:**
- Use sealed classes for entities (type safety)
- Use records for value objects (immutability)
- All invariants must be enforceable at domain layer
- No Spring/JPA annotations in domain layer
- Event-sourcing ready (Outbox pattern)

---

## The Briefing Domain at a Glance

**Responsibility:** Manage AI-assisted discovery flow where clients answer structured questions, system detects gaps and generates follow-ups, producing structured briefing for scope generation.

**Key Entities:**
- BriefingSession (sealed: InProgress | Completed | Abandoned)
- BriefingAnswer (sealed: AnsweredDirect | AnsweredWithFollowup)
- BriefingQuestion
- AIGeneration (audit trail)

**Key Domain Events:**
- BriefingSessionStarted
- QuestionAsked
- AnswerSubmitted
- FollowupQuestionGenerated
- BriefingCompleted
- BriefingAbandoned

**Domain Services:**
- BriefingService (7 methods: startBriefing, getNextQuestion, submitAnswer, detectGaps, generateFollowUp, completeBriefing, abandonBriefing)

**Invariants (MUST be enforced):**
1. Sequential questions (no skip)
2. No empty answers
3. Max 1 follow-up per question
4. Completion >= 80% + no critical gaps to complete
5. Immutable answers (no edit after submit)
6. Unique public_token per session
7. Only 1 active briefing per client per service type

**Communication:**
- Async IN: UserRegistered (cache name), WorkspaceMemberInvited (notify)
- Async OUT: BriefingCompleted → Proposal context (via Outbox + Kafka)
- Sync: Get workspace, Generate scope from briefing

---

## Your Deliverables

### 1. ADR-002: Briefing Domain Architecture

**File:** `docs/architecture/adr/ADR-002-briefing-domain.md`

**Template (use ADR-001 as reference):**

```markdown
# ADR-002: Briefing Domain Architecture

**Status:** Proposed

## Context

[Explain the briefing problem: AI-assisted discovery, gap detection, follow-up generation]

## Decision

[Your design choices: sealed classes, records, Outbox pattern, why?]

### Sealed Class Hierarchy

[Show the hierarchy and rationale]

### Value Objects

[List all records with validation logic]

### Domain Services

[BriefingService responsibilities, how invariants are enforced]

### Domain Events

[Events for Outbox pattern]

### Repository Interfaces

[What the adapter layer must implement]

## Consequences

### Positive
- Type safety via sealed classes
- Immutability via records
- Event sourcing ready
- [Others...]

### Negative
- [Trade-offs...]
- [What becomes harder...]

## Alternatives Considered

- Plain OOP inheritance (fragile, hard to evolve)
- Enum-based state (less type-safe)
- [Others...]

## Implementation Notes

- [Spring integration points]
- [Testing strategy]
- [Migration/versioning]
```

### 2. Domain Class Skeleton

Create the directory structure and class signatures (no full implementation, just skeleton):

```
src/main/java/com/scopeflow/core/domain/briefing/
├── BriefingSession.java (sealed)
├── BriefingInProgress.java (permits)
├── BriefingCompleted.java (permits)
├── BriefingAbandoned.java (permits)
├── BriefingAnswer.java (sealed)
├── AnsweredDirect.java (permits)
├── AnsweredWithFollowup.java (permits)
├── BriefingQuestion.java (regular class)
├── AIGeneration.java (record)
├── (5+ other value objects as records)
├── BriefingService.java (domain service)
├── (Domain exceptions)
├── (Domain events)
├── (Repository interfaces)
└── package-info.java
```

### 3. Decision Summary

Create: `.claude/plans/ARCHITECT-OUTPUT-Step1-Briefing.md`

With:
- [x] ADR-002 created
- [x] Sealed class hierarchy finalized
- [x] Domain services and invariants documented
- [x] Communication patterns defined
- [x] Ready for backend-dev

---

## Important Notes

### On Java 21 Features
- **Sealed classes:** Use `sealed` + `permits` to define exact subtypes (BriefingSession permits BriefingInProgress, etc.)
- **Records:** All value objects as records with validation in compact constructors
- **Pattern matching:** You can use in domain services for safe casting if needed

### On Type Safety
- BriefingSession is sealed → only 3 subtypes exist
- BriefingInProgress can transition to BriefingCompleted or BriefingAbandoned
- BriefingCompleted cannot transition (terminal state)
- This prevents invalid state transitions at compile-time

### On Invariants
- **Sequential questions:** Domain service checks `current_step` before allowing answer
- **No empty answers:** Value object AnswerText has non-empty validation
- **Max 1 follow-up:** Domain service checks follow_up_generated flag
- **Completion >= 80%:** CompletionScore value object rejects < 80%
- **Immutable answers:** Once created, Answer cannot be modified
- **Unique token:** Value object ensures token generation is unique
- **Single active:** Repository interface defines query method, domain service enforces

### On Communication
- Events are published via **Outbox pattern**: domain service publishes to outbox table → worker reads + publishes to Kafka
- You don't implement the worker; you design the event structure so adapter layer can handle it
- Sync queries are request/response; document the interface expectations

### On Spring Integration Points
- Domain layer: **ZERO Spring dependencies**
- Spring only enters at adapter layer (JPA entities, repositories, controllers)
- Document which Spring annotations the adapter layer will add

---

## Reference: ADR-001 (User & Workspace)

Review `docs/architecture/adr/ADR-001-user-workspace-service.md` for:
- ADR format and style
- How sealed classes are documented
- How invariants are listed
- How domain services are structured

---

## Git Workflow

1. You're on branch: `feature/sprint-1b-briefing-domain`
2. Create the directory: `src/main/java/com/scopeflow/core/domain/briefing/`
3. Create ADR-002 in: `docs/architecture/adr/ADR-002-briefing-domain.md`
4. Create class skeletons with Javadoc
5. Commit: `feat(architect): adr-002-briefing-domain-architecture`
6. Push to origin (don't merge yet)
7. Create summary: `.claude/plans/ARCHITECT-OUTPUT-Step1-Briefing.md`

---

## Success Criteria

When you're done, Step 1 is complete if:
- [x] ADR-002 is well-reasoned and references Java 21 features
- [x] All sealed classes and permits are defined
- [x] All value objects (records) are defined with compact constructor validation
- [x] All 7 invariants are documented with enforcement strategy
- [x] Domain services have clear method signatures
- [x] Communication patterns (async + sync) are documented
- [x] No Spring/JPA annotations in domain layer
- [x] Repository interfaces are clean (no queries, just ports)

---

## Timeline

**Start:** Now
**Duration:** ~1 day
**Next:** backend-dev receives ADR-002 and starts Step 2

---

## Context Resources

- Handoff spec: `.claude/plans/TERMINAL2-BRIEFING-HANDOFF.md` (full)
- Reference ADR: `docs/architecture/adr/ADR-001-user-workspace-service.md`
- Project CLAUDE.md: `./CLAUDE.md` (code style, Java 21 features, testing approach)
- Git status: `feature/sprint-1b-briefing-domain` (clean branch, ready to work)

---

**Questions?** Marcus (orchestrator) is available. But you're the architect — make decisions and document them!

🏗️ Ready. Go design.
