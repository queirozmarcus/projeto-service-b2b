# ADR-002: Briefing Domain Architecture

**Date:** 2026-03-22
**Status:** Proposed
**Agent:** architect
**Context:** Sprint 1 Terminal 2 — Briefing Domain (AI-assisted discovery)

---

## Context

Small B2B service providers (freelancers, microagencies) struggle with sales conversations:
- Audio notes scattered across WhatsApp
- Unclear scope, misaligned expectations
- Rework due to ambiguous briefs

**ScopeFlow solves this** with AI-assisted discovery: guide clients through structured questions, detect gaps in their responses, generate follow-up questions, consolidate into a clear, structured briefing ready for scope generation.

**Briefing Domain owns:** The entire discovery flow from session start → gap detection → completion → handoff to Proposal context.

---

## Decision

We adopt a **sealed class hierarchy with value objects** for type-safe, immutable domain modeling. This enables:
1. **Compile-time type safety:** BriefingSession has exactly 3 states (InProgress | Completed | Abandoned)
2. **Immutability:** All domain objects are effectively immutable via records and sealed classes
3. **Event-sourcing ready:** Outbox pattern integrates naturally
4. **Invariant enforcement:** Domain services validate all business rules before state changes

### Architecture Pattern: Hexagonal (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer (No Spring)              │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Sealed Classes (Entities)                        │  │
│  ├─ BriefingSession (sealed)                        │  │
│  │  ├─ BriefingInProgress (can answer questions)    │  │
│  │  ├─ BriefingCompleted (locked, generates scope)  │  │
│  │  └─ BriefingAbandoned (restart allowed)          │  │
│  ├─ BriefingAnswer (sealed)                         │  │
│  │  ├─ AnsweredDirect (single response)             │  │
│  │  └─ AnsweredWithFollowup (auto-generated)        │  │
│  │ BriefingQuestion                                 │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Value Objects (Records)                          │  │
│  ├─ BriefingSessionId(UUID)                         │  │
│  ├─ QuestionId(UUID)                               │  │
│  ├─ AnswerId(UUID)                                 │  │
│  ├─ BriefingProgress(step, total, percentage)      │  │
│  ├─ CompletionScore(score, gaps)                   │  │
│  ├─ AIGeneration(type, prompt_v, latency_ms)       │  │
│  ├─ PublicToken(String)                            │  │
│  └─ [5+ more]                                       │  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Domain Services (Business Logic)                 │  │
│  ├─ BriefingService                                │  │
│  │  ├─ startBriefing(...) → BriefingInProgress     │  │
│  │  ├─ submitAnswer(...) → void                    │  │
│  │  ├─ detectGaps(...) → CompletionScore           │  │
│  │  ├─ generateFollowUp(...) → Optional<Question>  │  │
│  │  └─ completeBriefing(...) → BriefingCompleted   │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Domain Events (Outbox Pattern)                   │  │
│  ├─ BriefingSessionStarted                         │  │
│  ├─ QuestionAsked                                  │  │
│  ├─ AnswerSubmitted                                │  │
│  ├─ FollowupQuestionGenerated                      │  │
│  ├─ BriefingCompleted                              │  │
│  └─ BriefingAbandoned                              │  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Domain Exceptions                                │  │
│  ├─ BriefingNotFoundException (BRIEFING-001)        │  │
│  ├─ BriefingAlreadyCompletedException (BRIEFING-002)  │
│  ├─ InvalidAnswerException (BRIEFING-003)          │  │
│  ├─ MaxFollowupExceededException (BRIEFING-004)    │  │
│  └─ IncompleteGapsException (BRIEFING-005)         │  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Repository Interfaces (Ports)                    │  │
│  ├─ BriefingSessionRepository                      │  │
│  ├─ BriefingQuestionRepository                     │  │
│  ├─ BriefingAnswerRepository                       │  │
│  └─ AIGenerationRepository                         │  │
└─────────────────────────────────────────────────────────┘
                          ↕ (Spring JPA adapters below)
┌─────────────────────────────────────────────────────────┐
│              Adapter Layer (Spring Boot)                │
│  ├─ JPA Entities (JpaBriefingSession, etc.)            │
│  ├─ Spring Data Repository implementations              │
│  ├─ REST Controllers (BriefingController, etc.)         │
│  └─ Event Publisher (OutboxPublisher → Kafka)          │
└─────────────────────────────────────────────────────────┘
```

---

## Sealed Class Hierarchy

### BriefingSession (Sealed Parent)

```java
public sealed class BriefingSession permits BriefingInProgress, BriefingCompleted, BriefingAbandoned {
    private final BriefingSessionId id;
    private final WorkspaceId workspaceId;
    private final ClientId clientId;
    private final ServiceType serviceType;
    private final PublicToken publicToken;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Factory methods for safe creation
    public static BriefingInProgress startNew(
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType
    ) { ... }
}
```

### BriefingInProgress (Permitted Subtype)

```java
public final class BriefingInProgress extends BriefingSession {
    private final BriefingProgress progress; // step, total, percentage
    private final List<BriefingAnswer> answers;

    // State transition: answer a question
    public void submitAnswer(AnswerId answerId, AnswerText text) { ... }

    // State transition: complete (if >= 80% + no critical gaps)
    public BriefingCompleted completeBriefing(CompletionScore score) { ... }

    // State transition: abandon
    public BriefingAbandoned abandon() { ... }
}
```

### BriefingCompleted (Terminal State)

```java
public final class BriefingCompleted extends BriefingSession {
    private final CompletionScore completionScore;
    private final AIAnalysis aiAnalysis;
    private final Instant completedAt;

    // Terminal state: cannot transition further
    // Can only be queried/exported for scope generation
}
```

### BriefingAbandoned (Terminal State)

```java
public final class BriefingAbandoned extends BriefingSession {
    private final String abandonReason;
    private final Instant abandonedAt;

    // Can restart: create new BriefingInProgress with same client/service
}
```

### BriefingAnswer (Sealed Parent)

```java
public sealed class BriefingAnswer permits AnsweredDirect, AnsweredWithFollowup {
    private final AnswerId id;
    private final BriefingSessionId sessionId;
    private final QuestionId questionId;
    private final AnswerText text;
    private final Instant answeredAt;

    // Immutable: no setter
}
```

### AnsweredDirect (No Follow-up)

```java
public final class AnsweredDirect extends BriefingAnswer {
    private final int qualityScore; // 0-100
    // No follow_up_generated = false
}
```

### AnsweredWithFollowup (Auto-generated Follow-up)

```java
public final class AnsweredWithFollowup extends BriefingAnswer {
    private final BriefingQuestion generatedFollowup;
    private final int confidenceScore;
    // follow_up_generated = true
}
```

---

## Value Objects (Records)

All value objects are records with **compact constructor validation**:

```java
public record BriefingSessionId(UUID value) {
    public BriefingSessionId {
        if (value == null) throw new IllegalArgumentException("id required");
    }
    public static BriefingSessionId generate() { return new BriefingSessionId(UUID.randomUUID()); }
}

public record AnswerText(String value) {
    public AnswerText {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("answer required");
        if (value.length() > 5000) throw new IllegalArgumentException("answer too long");
    }
}

public record PublicToken(String value) {
    public PublicToken {
        if (value == null || value.length() < 32) throw new IllegalArgumentException("token invalid");
    }
    public static PublicToken generate() { return new PublicToken(UUID.randomUUID().toString()); }
}

public record CompletionScore(int score, List<String> gapsIdentified) {
    public CompletionScore {
        if (score < 0 || score > 100) throw new IllegalArgumentException("score 0-100");
        if (score < 80) throw new IllegalArgumentException("must be >= 80 to complete");
        if (gapsIdentified == null) gapsIdentified = List.of();
    }
}

public record BriefingProgress(int currentStep, int totalSteps, int completionPercentage) {
    public BriefingProgress {
        if (currentStep < 0 || currentStep > totalSteps) throw new IllegalArgumentException("invalid step");
        int calculatedPercentage = totalSteps == 0 ? 0 : (currentStep * 100) / totalSteps;
        if (completionPercentage != calculatedPercentage) throw new IllegalArgumentException("percentage mismatch");
    }
}

public record AIGeneration(
    GenerationType type,
    String inputJson,
    String outputJson,
    String promptVersion,
    long latencyMs,
    BigDecimal costUsd
) {
    public AIGeneration {
        if (type == null || inputJson == null || outputJson == null) throw new IllegalArgumentException("required");
        if (latencyMs < 0) throw new IllegalArgumentException("latency must be >= 0");
        if (costUsd.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("cost must be >= 0");
    }
}
```

---

## Domain Services

### BriefingService (Enforces All Invariants)

```java
public class BriefingService {
    private final BriefingSessionRepository sessionRepo;
    private final BriefingQuestionRepository questionRepo;
    private final BriefingAnswerRepository answerRepo;
    private final AIGenerationRepository aiRepo;

    // Invariant: Only 1 active briefing per client per service type
    public BriefingInProgress startBriefing(
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType
    ) throws BriefingAlreadyInProgressException {
        Optional<BriefingSession> active = sessionRepo.findActiveByClientAndService(clientId, serviceType);
        if (active.isPresent()) {
            throw new BriefingAlreadyInProgressException(
                "BRIEFING-001",
                "Only 1 active briefing per client per service type"
            );
        }
        BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);
        sessionRepo.save(session);
        // Publish event: BriefingSessionStarted
        return session;
    }

    // Invariant: Sequential questions (no skip)
    public BriefingQuestion getNextQuestion(BriefingSessionId sessionId)
        throws BriefingNotFoundException {
        BriefingSession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new BriefingNotFoundException("BRIEFING-001", "Session not found"));

        if (!(session instanceof BriefingInProgress)) {
            throw new InvalidStateException("BRIEFING-002", "Session not in progress");
        }

        int nextStep = sessionRepo.countAnswers(sessionId) + 1;
        BriefingQuestion question = questionRepo.findBySessionAndStep(sessionId, nextStep)
            .orElseThrow(() -> new InvalidStateException("BRIEFING-003", "No more questions"));

        return question; // Publish event: QuestionAsked
    }

    // Invariant: No empty answers, Max 1 follow-up
    public void submitAnswer(
        BriefingSessionId sessionId,
        QuestionId questionId,
        AnswerText answerText,
        Optional<AIGeneration> aiGeneration
    ) throws InvalidAnswerException {
        if (answerText.value().isBlank()) {
            throw new InvalidAnswerException("BRIEFING-003", "Answer cannot be empty");
        }

        BriefingAnswer answer = new AnsweredDirect(AnswerId.generate(), sessionId, questionId, answerText);
        answerRepo.save(answer);

        // Check for follow-up (max 1 per question)
        long followupCount = answerRepo.countFollowupsByQuestion(questionId);
        if (followupCount >= 1) {
            throw new MaxFollowupExceededException("BRIEFING-004", "Max 1 follow-up per question");
        }

        // Generate follow-up if gaps detected
        if (aiGeneration.isPresent()) {
            BriefingQuestion followup = generateFollowup(sessionId, aiGeneration.get());
            answer = new AnsweredWithFollowup(answer.id(), answer.sessionId(), answer.questionId(), answer.text(), followup);
        }

        // Publish event: AnswerSubmitted
    }

    // Invariant: Completion >= 80% + no critical gaps
    public BriefingCompleted completeBriefing(
        BriefingSessionId sessionId,
        CompletionScore score
    ) throws IncompleteGapsException {
        if (score.score() < 80) {
            throw new IncompleteGapsException("BRIEFING-005", "Completion score < 80%");
        }

        BriefingSession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new BriefingNotFoundException("BRIEFING-001", "Session not found"));

        if (!(session instanceof BriefingInProgress)) {
            throw new BriefingAlreadyCompletedException("BRIEFING-002", "Session already completed");
        }

        BriefingCompleted completed = ((BriefingInProgress) session).completeBriefing(score);
        sessionRepo.save(completed);

        // Publish event: BriefingCompleted → Outbox → Kafka → Proposal context
        return completed;
    }

    // Helpers
    private BriefingQuestion generateFollowup(BriefingSessionId sessionId, AIGeneration aiGen) { ... }

    public CompletionScore detectGaps(BriefingSessionId sessionId) { ... }
}
```

---

## Domain Events (Outbox Pattern)

All events implement a common interface for Outbox publishing:

```java
public sealed interface DomainEvent permits
    BriefingSessionStarted,
    QuestionAsked,
    AnswerSubmitted,
    FollowupQuestionGenerated,
    BriefingCompleted,
    BriefingAbandoned
{
    BriefingSessionId aggregateId();
    Instant occurredAt();
    String eventType();
}

public record BriefingSessionStarted(
    BriefingSessionId sessionId,
    WorkspaceId workspaceId,
    ClientId clientId,
    ServiceType serviceType,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.session.started"; }
}

public record BriefingCompleted(
    BriefingSessionId sessionId,
    CompletionScore score,
    AIAnalysis analysis,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.completed"; }
}

// ... other events follow same pattern
```

**Outbox publishing:**
- Domain service publishes event to domain event collection
- Adapter layer extracts event → writes to Outbox table → worker reads → publishes to Kafka topic `briefing.events.v1`

---

## Repository Interfaces (Ports)

No JPA annotations, no Spring—just pure domain ports:

```java
public interface BriefingSessionRepository {
    Optional<BriefingSession> findById(BriefingSessionId id);
    void save(BriefingSession session);
    Optional<BriefingSession> findActiveByClientAndService(ClientId clientId, ServiceType serviceType);
    List<BriefingSession> findByWorkspaceAndStatus(WorkspaceId workspaceId, BriefingStatus status);
}

public interface BriefingQuestionRepository {
    Optional<BriefingQuestion> findBySessionAndStep(BriefingSessionId sessionId, int step);
    List<BriefingQuestion> findBySession(BriefingSessionId sessionId);
}

public interface BriefingAnswerRepository {
    void save(BriefingAnswer answer);
    List<BriefingAnswer> findBySession(BriefingSessionId sessionId);
    long countFollowupsByQuestion(QuestionId questionId);
}

public interface AIGenerationRepository {
    void save(AIGeneration generation);
    List<AIGeneration> findBySession(BriefingSessionId sessionId);
}
```

---

## Domain Exceptions

All exceptions have **stable error codes** for support/automation:

```java
public abstract class BriefingDomainException extends RuntimeException {
    private final String errorCode;

    protected BriefingDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}

public class BriefingNotFoundException extends BriefingDomainException {
    public BriefingNotFoundException(String errorCode, String message) {
        super("BRIEFING-001", message);
    }
}

public class BriefingAlreadyCompletedException extends BriefingDomainException {
    public BriefingAlreadyCompletedException(String errorCode, String message) {
        super("BRIEFING-002", message);
    }
}

public class InvalidAnswerException extends BriefingDomainException {
    public InvalidAnswerException(String errorCode, String message) {
        super("BRIEFING-003", message);
    }
}

public class MaxFollowupExceededException extends BriefingDomainException {
    public MaxFollowupExceededException(String errorCode, String message) {
        super("BRIEFING-004", message);
    }
}

public class IncompleteGapsException extends BriefingDomainException {
    public IncompleteGapsException(String errorCode, String message) {
        super("BRIEFING-005", message);
    }
}
```

---

## Invariants & Enforcement Strategy

| # | Invariant | Enforcement |
|---|-----------|-------------|
| 1 | **Sequential Questions** | Domain service `getNextQuestion()` computes next step; answer submission goes in order |
| 2 | **No Empty Answers** | `AnswerText` record rejects blank in compact constructor |
| 3 | **Max 1 Follow-up per Question** | Domain service checks `countFollowupsByQuestion()` before generating |
| 4 | **Completion >= 80% + no critical gaps** | `CompletionScore` record rejects < 80%; domain service enforces before `completeBriefing()` |
| 5 | **Immutable Answers** | `BriefingAnswer` is sealed final; no setters; design prevents mutation |
| 6 | **Unique Public Token** | `PublicToken` value object generates UUID-based token; database UNIQUE constraint |
| 7 | **Single Active Briefing per Client per Service** | Domain service `startBriefing()` queries `findActiveByClientAndService()` before allowing new |

---

## Communication with Other Contexts

### Async (Outbox → Kafka)

**Briefing → Proposal Context:**
- Event: `BriefingCompleted`
- Topic: `briefing.events.v1`
- Consumer: proposal-service listens, creates ProposalDraft

**User & Workspace → Briefing:**
- Event: `UserRegistered`
- Consumer: briefing-service caches user name
- Event: `WorkspaceMemberInvited`
- Consumer: briefing-service updates notification preferences

### Sync (REST Calls)

**Query:** "Get workspace for briefing session"
- Briefing service calls user-workspace-service `/api/v1/workspaces/{id}` to fetch owner info

**Query:** "Generate scope from briefing"
- Proposal service calls back to briefing-service to fetch structured briefing data

---

## Consequences

### Positive ✅
- **Type Safety:** Sealed classes guarantee only 3 states per entity; compiler catches invalid transitions
- **Immutability:** Records prevent accidental mutations; thread-safe by design
- **Event-Sourcing Ready:** Sealed interfaces + records fit perfectly with Outbox pattern
- **Java 21 Idioms:** Uses modern Java features (sealed, records, pattern matching)
- **Clear Invariants:** All business rules enforced at domain layer, not database
- **Testability:** Pure domain logic, no Spring dependencies → fast unit tests

### Negative ⚠️
- **Learning Curve:** Sealed classes are Java 21 feature; team must understand permits/sealed
- **More Boilerplate (initially):** Records require explicit value objects, but pays off in clarity
- **State Transitions Complex:** BriefingInProgress has methods that return BriefingCompleted; requires careful API design
- **No SQL-like Queries in Domain:** Repository interfaces are minimal; complex queries belong in adapter layer

---

## Alternatives Considered

### ❌ JPA @Inheritance Hierarchy
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class BriefingSession { ... }
```
**Why rejected:** Loses type safety; Spring couples domain to persistence; harder to version events.

### ❌ Enum-based State Machine
```java
public class BriefingSession {
    private BriefingStatus status; // IN_PROGRESS, COMPLETED, ABANDONED
    public void complete() { this.status = COMPLETED; }
}
```
**Why rejected:** No compile-time guarantee of valid transitions; easier to create invalid states at runtime.

### ❌ Plain POJO Inheritance
```java
public class BriefingSession { ... }
public class BriefingInProgress extends BriefingSession { ... }
```
**Why rejected:** Fragile base class problem; no guarantee of exactly 3 subtypes; can be subclassed elsewhere.

---

## Implementation Notes

### Spring Integration Points

The domain layer is **100% Spring-free**. Spring enters at the adapter layer:

1. **JPA Entities:** Create `JpaBriefingSession`, `JpaBriefingInProgress` with `@Entity` / `@DiscriminatorValue`
2. **Spring Data Repositories:** Create `BriefingSessionJpaRepository extends JpaRepository` implementing domain port
3. **Mappers:** Create `BriefingSessionMapper` (domain ↔ JPA)
4. **Controllers:** REST endpoints receive dtos, convert to domain, call domain service, convert response
5. **Event Publisher:** Spring listener on domain events → writes to Outbox table → worker publishes to Kafka

### Testing Strategy

1. **Unit Tests:** No database, no Spring; test domain entities + services directly
2. **Integration Tests:** With Testcontainers; test JPA mappings + repository queries
3. **E2E Tests:** Via REST controllers; test full briefing flow

### Migration & Versioning

- **Prompt versions:** All prompts stored as files (briefing_questions_v1.md, briefing_questions_v2.md)
- **AI Generation audit trail:** Every IA output records prompt_version for reproducibility
- **Answer immutability:** Once submitted, answers never change (audit trail)
- **Event versioning:** Kafka topic includes version (briefing.events.v1) for future compatibility

---

## Future Enhancements (Post-MVP)

- **Follow-up depth:** Currently max 1 follow-up per question; can be extended
- **Question branching:** Conditional questions based on previous answers
- **AI model selection:** Choose between GPT-4, Claude, open-source models
- **Briefing templates:** Pre-defined question sets by service type
- **Client approval:** Client can mark briefing as "approved" before scope generation

---

## References

- **Terminal 1 ADR:** `docs/architecture/adr/ADR-001-user-workspace-service.md`
- **Specification:** `.claude/plans/TERMINAL2-BRIEFING-HANDOFF.md`
- **Java 21 Features:** sealed classes (JEP 425), records (JEP 395), pattern matching (JEP 432)
- **DDD Reference:** Evans, "Domain-Driven Design"
- **Outbox Pattern:** https://microservices.io/patterns/data/transactional-outbox.html

---

## Approval & Sign-Off

**Architect:** Claude Sonnet (Agent)
**Date:** 2026-03-22
**Status:** ✅ Ready for backend-dev (Step 2)

Next step: backend-dev implements domain classes, repositories, services, 50+ unit tests.
