# Backend-Dev Delegation — Step 2: Briefing Domain Implementation

**To:** backend-dev (Claude Sonnet)
**From:** Marcus (Orchestrator)
**Date:** 2026-03-22
**Mode:** Fork isolated — implement 26+ Java files, 50+ tests
**Task ID:** TERMINAL2-STEP2-BACKEND-DEV
**Dependency:** ADR-002 (just completed by architect)

---

## Mission

Implement the **complete Briefing domain** from the ADR using Java 21 sealed classes, records, and clean architecture.

**Input:**
- ADR-002: `docs/architecture/adr/ADR-002-briefing-domain.md` (full design)
- Architecture reference: Terminal 1 backend output (for pattern examples)
- Project CLAUDE.md: `./ CLAUDE.md` (code style, testing approach)

**Output:**
- 26+ Java files in `src/main/java/com/scopeflow/core/domain/briefing/`
- 50+ unit tests in `src/test/java/com/scopeflow/core/domain/briefing/`
- All files compile without errors
- Zero Spring/JPA annotations in domain layer

**Constraints:**
- **No Spring:** Domain layer is pure Java
- **No JPA:** Repositories are interfaces only
- **Immutability:** Sealed classes + records, no setters
- **Type Safety:** All Java 21 sealed class features
- **Testing:** @Nested structure, AssertJ, no DB, 50+ tests

**Timeline:** ~2-3 days

---

## Your Deliverables

### 1. Sealed Classes & Subtypes (7 files)

#### BriefingSession.java (sealed parent)

```java
package com.scopeflow.core.domain.briefing;

public sealed class BriefingSession
    permits BriefingInProgress, BriefingCompleted, BriefingAbandoned {

    private final BriefingSessionId id;
    private final WorkspaceId workspaceId;
    private final ClientId clientId;
    private final ServiceType serviceType;
    private final PublicToken publicToken;
    private final Instant createdAt;
    private final Instant updatedAt;

    protected BriefingSession(
        BriefingSessionId id,
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType,
        PublicToken publicToken,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.workspaceId = Objects.requireNonNull(workspaceId);
        this.clientId = Objects.requireNonNull(clientId);
        this.serviceType = Objects.requireNonNull(serviceType);
        this.publicToken = Objects.requireNonNull(publicToken);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static BriefingInProgress startNew(
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType
    ) {
        BriefingSessionId id = BriefingSessionId.generate();
        PublicToken token = PublicToken.generate();
        Instant now = Instant.now();
        return new BriefingInProgress(id, workspaceId, clientId, serviceType, token, now, now, List.of());
    }

    // Getters (immutable)
    public BriefingSessionId id() { return id; }
    public WorkspaceId workspaceId() { return workspaceId; }
    public ClientId clientId() { return clientId; }
    public ServiceType serviceType() { return serviceType; }
    public PublicToken publicToken() { return publicToken; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    @Override
    public String toString() { return "BriefingSession[" + id + "]"; }
}
```

#### BriefingInProgress.java (permits subtype)

```java
public final class BriefingInProgress extends BriefingSession {
    private final List<BriefingAnswer> answers;
    private final int currentStep;
    private final int totalSteps;

    public BriefingInProgress(
        BriefingSessionId id,
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType,
        PublicToken publicToken,
        Instant createdAt,
        Instant updatedAt,
        List<BriefingAnswer> answers,
        int currentStep,
        int totalSteps
    ) {
        super(id, workspaceId, clientId, serviceType, publicToken, createdAt, updatedAt);
        this.answers = Objects.requireNonNull(answers);
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        if (currentStep < 0 || currentStep > totalSteps) {
            throw new IllegalArgumentException("Invalid step");
        }
    }

    public void submitAnswer(BriefingAnswer answer) {
        if (answer == null) throw new IllegalArgumentException("Answer required");
        // Add to answers (immutably in real implementation)
    }

    public BriefingCompleted completeBriefing(CompletionScore score) {
        if (score == null) throw new IllegalArgumentException("Score required");
        return new BriefingCompleted(
            this.id(),
            this.workspaceId(),
            this.clientId(),
            this.serviceType(),
            this.publicToken(),
            this.createdAt(),
            Instant.now(),
            score,
            Instant.now()
        );
    }

    public BriefingAbandoned abandon() {
        return new BriefingAbandoned(
            this.id(),
            this.workspaceId(),
            this.clientId(),
            this.serviceType(),
            this.publicToken(),
            this.createdAt(),
            Instant.now(),
            "User abandoned",
            Instant.now()
        );
    }

    // Getters
    public List<BriefingAnswer> answers() { return List.copyOf(answers); }
    public int currentStep() { return currentStep; }
    public int totalSteps() { return totalSteps; }
    public int completionPercentage() { return totalSteps == 0 ? 0 : (currentStep * 100) / totalSteps; }
}
```

#### BriefingCompleted.java (terminal state)

```java
public final class BriefingCompleted extends BriefingSession {
    private final CompletionScore completionScore;
    private final Instant completedAt;

    public BriefingCompleted(
        BriefingSessionId id,
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType,
        PublicToken publicToken,
        Instant createdAt,
        Instant updatedAt,
        CompletionScore completionScore,
        Instant completedAt
    ) {
        super(id, workspaceId, clientId, serviceType, publicToken, createdAt, updatedAt);
        this.completionScore = Objects.requireNonNull(completionScore);
        this.completedAt = Objects.requireNonNull(completedAt);
    }

    public CompletionScore completionScore() { return completionScore; }
    public Instant completedAt() { return completedAt; }
    public boolean isReady() { return completionScore.score() >= 80; }
}
```

#### BriefingAbandoned.java (can restart)

```java
public final class BriefingAbandoned extends BriefingSession {
    private final String abandonReason;
    private final Instant abandonedAt;

    public BriefingAbandoned(
        BriefingSessionId id,
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType,
        PublicToken publicToken,
        Instant createdAt,
        Instant updatedAt,
        String abandonReason,
        Instant abandonedAt
    ) {
        super(id, workspaceId, clientId, serviceType, publicToken, createdAt, updatedAt);
        this.abandonReason = abandonReason;
        this.abandonedAt = Objects.requireNonNull(abandonedAt);
    }

    public String abandonReason() { return abandonReason; }
    public Instant abandonedAt() { return abandonedAt; }
}
```

#### BriefingAnswer.java (sealed parent)

```java
public sealed class BriefingAnswer
    permits AnsweredDirect, AnsweredWithFollowup {

    private final AnswerId id;
    private final BriefingSessionId sessionId;
    private final QuestionId questionId;
    private final AnswerText text;
    private final Instant answeredAt;

    protected BriefingAnswer(
        AnswerId id,
        BriefingSessionId sessionId,
        QuestionId questionId,
        AnswerText text,
        Instant answeredAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.sessionId = Objects.requireNonNull(sessionId);
        this.questionId = Objects.requireNonNull(questionId);
        this.text = Objects.requireNonNull(text);
        this.answeredAt = Objects.requireNonNull(answeredAt);
    }

    public AnswerId id() { return id; }
    public BriefingSessionId sessionId() { return sessionId; }
    public QuestionId questionId() { return questionId; }
    public AnswerText text() { return text; }
    public Instant answeredAt() { return answeredAt; }
}
```

#### AnsweredDirect.java

```java
public final class AnsweredDirect extends BriefingAnswer {
    private final int qualityScore;

    public AnsweredDirect(
        AnswerId id,
        BriefingSessionId sessionId,
        QuestionId questionId,
        AnswerText text,
        Instant answeredAt,
        int qualityScore
    ) {
        super(id, sessionId, questionId, text, answeredAt);
        if (qualityScore < 0 || qualityScore > 100) {
            throw new IllegalArgumentException("Quality score 0-100");
        }
        this.qualityScore = qualityScore;
    }

    public int qualityScore() { return qualityScore; }
    public boolean hasFollowup() { return false; }
}
```

#### AnsweredWithFollowup.java

```java
public final class AnsweredWithFollowup extends BriefingAnswer {
    private final BriefingQuestion generatedFollowup;
    private final int confidenceScore;

    public AnsweredWithFollowup(
        AnswerId id,
        BriefingSessionId sessionId,
        QuestionId questionId,
        AnswerText text,
        Instant answeredAt,
        BriefingQuestion generatedFollowup,
        int confidenceScore
    ) {
        super(id, sessionId, questionId, text, answeredAt);
        if (confidenceScore < 0 || confidenceScore > 100) {
            throw new IllegalArgumentException("Confidence score 0-100");
        }
        this.generatedFollowup = Objects.requireNonNull(generatedFollowup);
        this.confidenceScore = confidenceScore;
    }

    public BriefingQuestion generatedFollowup() { return generatedFollowup; }
    public int confidenceScore() { return confidenceScore; }
    public boolean hasFollowup() { return true; }
}
```

### 2. Value Objects (8+ records)

#### BriefingSessionId.java

```java
public record BriefingSessionId(UUID value) {
    public BriefingSessionId {
        if (value == null) throw new IllegalArgumentException("id required");
    }
    public static BriefingSessionId generate() { return new BriefingSessionId(UUID.randomUUID()); }
    @Override
    public String toString() { return value.toString(); }
}
```

#### QuestionId.java

```java
public record QuestionId(UUID value) {
    public QuestionId {
        if (value == null) throw new IllegalArgumentException("id required");
    }
    public static QuestionId generate() { return new QuestionId(UUID.randomUUID()); }
}
```

#### AnswerId.java

```java
public record AnswerId(UUID value) {
    public AnswerId {
        if (value == null) throw new IllegalArgumentException("id required");
    }
    public static AnswerId generate() { return new AnswerId(UUID.randomUUID()); }
}
```

#### AnswerText.java

```java
public record AnswerText(String value) {
    public AnswerText {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("answer cannot be empty");
        }
        if (value.length() > 5000) {
            throw new IllegalArgumentException("answer max 5000 chars");
        }
    }
}
```

#### PublicToken.java

```java
public record PublicToken(String value) {
    public PublicToken {
        if (value == null || value.length() < 32) {
            throw new IllegalArgumentException("invalid public token");
        }
    }
    public static PublicToken generate() {
        return new PublicToken(UUID.randomUUID().toString() + UUID.randomUUID().toString());
    }
}
```

#### CompletionScore.java

```java
public record CompletionScore(int score, List<String> gapsIdentified) {
    public CompletionScore {
        if (score < 0 || score > 100) throw new IllegalArgumentException("score 0-100");
        if (score < 80) throw new IllegalArgumentException("must be >= 80 to complete");
        if (gapsIdentified == null) gapsIdentified = List.of();
        gapsIdentified = List.copyOf(gapsIdentified);
    }
}
```

#### BriefingProgress.java

```java
public record BriefingProgress(int currentStep, int totalSteps, int completionPercentage) {
    public BriefingProgress {
        if (currentStep < 0 || currentStep > totalSteps) throw new IllegalArgumentException("invalid step");
        int calculated = totalSteps == 0 ? 0 : (currentStep * 100) / totalSteps;
        if (completionPercentage != calculated) throw new IllegalArgumentException("percentage mismatch");
    }
}
```

#### AIGeneration.java

```java
public record AIGeneration(
    GenerationType type,
    String inputJson,
    String outputJson,
    String promptVersion,
    long latencyMs,
    BigDecimal costUsd
) {
    public AIGeneration {
        if (type == null || inputJson == null || outputJson == null) {
            throw new IllegalArgumentException("required fields");
        }
        if (latencyMs < 0) throw new IllegalArgumentException("latency >= 0");
        if (costUsd.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("cost >= 0");
    }
}
```

### 3. Domain Service (1 file)

#### BriefingService.java

```java
public class BriefingService {
    private final BriefingSessionRepository sessionRepository;
    private final BriefingQuestionRepository questionRepository;
    private final BriefingAnswerRepository answerRepository;

    public BriefingService(
        BriefingSessionRepository sessionRepository,
        BriefingQuestionRepository questionRepository,
        BriefingAnswerRepository answerRepository
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.questionRepository = Objects.requireNonNull(questionRepository);
        this.answerRepository = Objects.requireNonNull(answerRepository);
    }

    public BriefingInProgress startBriefing(
        WorkspaceId workspaceId,
        ClientId clientId,
        ServiceType serviceType
    ) throws BriefingAlreadyInProgressException {
        Optional<BriefingSession> active = sessionRepository.findActiveByClientAndService(clientId, serviceType);
        if (active.isPresent()) {
            throw new BriefingAlreadyInProgressException(
                "Only 1 active briefing per client per service type"
            );
        }
        BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);
        sessionRepository.save(session);
        return session;
    }

    public BriefingQuestion getNextQuestion(BriefingSessionId sessionId)
        throws BriefingNotFoundException {
        BriefingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BriefingNotFoundException("Session not found"));

        if (!(session instanceof BriefingInProgress inProgress)) {
            throw new InvalidStateException("Session not in progress");
        }

        int nextStep = inProgress.currentStep() + 1;
        return questionRepository.findBySessionAndStep(sessionId, nextStep)
            .orElseThrow(() -> new InvalidStateException("No more questions"));
    }

    public void submitAnswer(
        BriefingSessionId sessionId,
        QuestionId questionId,
        AnswerText answerText
    ) throws InvalidAnswerException {
        if (answerText.value().isBlank()) {
            throw new InvalidAnswerException("Answer cannot be empty");
        }

        AnswerId answerId = AnswerId.generate();
        AnsweredDirect answer = new AnsweredDirect(
            answerId,
            sessionId,
            questionId,
            answerText,
            Instant.now(),
            75 // placeholder quality score
        );
        answerRepository.save(answer);
    }

    public void submitAnswerWithFollowup(
        BriefingSessionId sessionId,
        QuestionId questionId,
        AnswerText answerText,
        BriefingQuestion generatedFollowup
    ) throws MaxFollowupExceededException {
        long followupCount = answerRepository.countFollowupsByQuestion(questionId);
        if (followupCount >= 1) {
            throw new MaxFollowupExceededException("Max 1 follow-up per question");
        }

        AnswerId answerId = AnswerId.generate();
        AnsweredWithFollowup answer = new AnsweredWithFollowup(
            answerId,
            sessionId,
            questionId,
            answerText,
            Instant.now(),
            generatedFollowup,
            85 // placeholder confidence
        );
        answerRepository.save(answer);
    }

    public BriefingCompleted completeBriefing(
        BriefingSessionId sessionId,
        CompletionScore score
    ) throws IncompleteGapsException, BriefingNotFoundException {
        if (score.score() < 80) {
            throw new IncompleteGapsException("Completion score < 80%");
        }

        BriefingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BriefingNotFoundException("Session not found"));

        if (!(session instanceof BriefingInProgress inProgress)) {
            throw new BriefingAlreadyCompletedException("Session already completed");
        }

        BriefingCompleted completed = inProgress.completeBriefing(score);
        sessionRepository.save(completed);
        return completed;
    }

    public BriefingAbandoned abandonBriefing(BriefingSessionId sessionId)
        throws BriefingNotFoundException {
        BriefingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BriefingNotFoundException("Session not found"));

        if (!(session instanceof BriefingInProgress inProgress)) {
            throw new InvalidStateException("Session not in progress");
        }

        BriefingAbandoned abandoned = inProgress.abandon();
        sessionRepository.save(abandoned);
        return abandoned;
    }

    public CompletionScore detectGaps(BriefingSessionId sessionId)
        throws BriefingNotFoundException {
        BriefingSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new BriefingNotFoundException("Session not found"));

        List<BriefingAnswer> answers = answerRepository.findBySession(sessionId);
        int score = Math.min(100, answers.size() * 10); // placeholder calculation
        List<String> gaps = score < 80 ? List.of("Need more details on timeline") : List.of();
        return new CompletionScore(score, gaps);
    }
}
```

### 4. Repository Interfaces (4 files)

```java
// BriefingSessionRepository.java
public interface BriefingSessionRepository {
    Optional<BriefingSession> findById(BriefingSessionId id);
    void save(BriefingSession session);
    Optional<BriefingSession> findActiveByClientAndService(ClientId clientId, ServiceType serviceType);
    List<BriefingSession> findByWorkspaceAndStatus(WorkspaceId workspaceId, String status);
}

// BriefingQuestionRepository.java
public interface BriefingQuestionRepository {
    Optional<BriefingQuestion> findBySessionAndStep(BriefingSessionId sessionId, int step);
    List<BriefingQuestion> findBySession(BriefingSessionId sessionId);
}

// BriefingAnswerRepository.java
public interface BriefingAnswerRepository {
    void save(BriefingAnswer answer);
    List<BriefingAnswer> findBySession(BriefingSessionId sessionId);
    long countFollowupsByQuestion(QuestionId questionId);
}

// AIGenerationRepository.java
public interface AIGenerationRepository {
    void save(AIGeneration generation);
    List<AIGeneration> findBySession(BriefingSessionId sessionId);
}
```

### 5. Domain Exceptions (5 files)

```java
// BriefingDomainException.java (base)
public abstract class BriefingDomainException extends RuntimeException {
    private final String errorCode;

    protected BriefingDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}

// BriefingNotFoundException.java
public class BriefingNotFoundException extends BriefingDomainException {
    public BriefingNotFoundException(String message) {
        super("BRIEFING-001", message);
    }
}

// BriefingAlreadyCompletedException.java
public class BriefingAlreadyCompletedException extends BriefingDomainException {
    public BriefingAlreadyCompletedException(String message) {
        super("BRIEFING-002", message);
    }
}

// InvalidAnswerException.java
public class InvalidAnswerException extends BriefingDomainException {
    public InvalidAnswerException(String message) {
        super("BRIEFING-003", message);
    }
}

// MaxFollowupExceededException.java
public class MaxFollowupExceededException extends BriefingDomainException {
    public MaxFollowupExceededException(String message) {
        super("BRIEFING-004", message);
    }
}

// IncompleteGapsException.java
public class IncompleteGapsException extends BriefingDomainException {
    public IncompleteGapsException(String message) {
        super("BRIEFING-005", message);
    }
}
```

### 6. Domain Events (6+ files)

```java
// DomainEvent.java (sealed interface)
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

// BriefingSessionStarted.java
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

// QuestionAsked.java
public record QuestionAsked(
    BriefingSessionId sessionId,
    QuestionId questionId,
    int step,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.question.asked"; }
}

// AnswerSubmitted.java
public record AnswerSubmitted(
    BriefingSessionId sessionId,
    AnswerId answerId,
    QuestionId questionId,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.answer.submitted"; }
}

// FollowupQuestionGenerated.java
public record FollowupQuestionGenerated(
    BriefingSessionId sessionId,
    QuestionId followupQuestionId,
    QuestionId parentQuestionId,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.followup.generated"; }
}

// BriefingCompletedEvent.java
public record BriefingCompletedEvent(
    BriefingSessionId sessionId,
    CompletionScore score,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.completed"; }
}

// BriefingAbandonedEvent.java
public record BriefingAbandonedEvent(
    BriefingSessionId sessionId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public BriefingSessionId aggregateId() { return sessionId; }
    @Override
    public String eventType() { return "briefing.abandoned"; }
}
```

### 7. Additional Classes

- **BriefingQuestion.java** (regular class, contains question metadata)
- **GenerationType.java** (enum: FOLLOW_UP_QUESTION, GAP_ANALYSIS, COMPLETION_SUMMARY)
- **ServiceType.java** (enum or value object from Terminal 1)
- **ClientId.java** (value object record)
- **WorkspaceId.java** (imported from Terminal 1)
- **package-info.java** (documentation)

### 8. Unit Tests (50+ tests)

#### BriefingSessionTest.java

```java
@Nested
class BriefingSessionTest {
    @Nested
    class StartNew {
        @Test
        void should_create_new_session() {
            BriefingInProgress session = BriefingSession.startNew(
                new WorkspaceId(UUID.randomUUID()),
                new ClientId(UUID.randomUUID()),
                ServiceType.SOCIAL_MEDIA
            );
            assertThat(session).isNotNull();
            assertThat(session.currentStep()).isZero();
            assertThat(session.publicToken()).isNotNull();
        }

        @Test
        void should_set_current_timestamp() {
            Instant before = Instant.now();
            BriefingInProgress session = BriefingSession.startNew(...);
            Instant after = Instant.now();
            assertThat(session.createdAt()).isBetween(before, after);
        }
    }

    @Nested
    class StateTransitions {
        @Test
        void should_transition_to_completed() {
            BriefingInProgress session = BriefingSession.startNew(...);
            CompletionScore score = new CompletionScore(85, List.of());
            BriefingCompleted completed = session.completeBriefing(score);
            assertThat(completed.completionScore().score()).isEqualTo(85);
        }

        @Test
        void should_transition_to_abandoned() {
            BriefingInProgress session = BriefingSession.startNew(...);
            BriefingAbandoned abandoned = session.abandon();
            assertThat(abandoned.abandonedAt()).isNotNull();
        }
    }

    @Nested
    class Invariants {
        @Test
        void should_reject_invalid_step() {
            assertThatThrownBy(() ->
                new BriefingInProgress(..., currentStep: 10, totalSteps: 5)
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

#### ValueObjectTests (for each record)

```java
@Nested
class AnswerTextTest {
    @Test
    void should_reject_blank_answer() {
        assertThatThrownBy(() -> new AnswerText(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_null_answer() {
        assertThatThrownBy(() -> new AnswerText(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_too_long_answer() {
        String longText = "x".repeat(5001);
        assertThatThrownBy(() -> new AnswerText(longText))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_accept_valid_answer() {
        AnswerText answer = new AnswerText("This is a valid answer");
        assertThat(answer.value()).isEqualTo("This is a valid answer");
    }
}

@Nested
class CompletionScoreTest {
    @Test
    void should_reject_score_below_80() {
        assertThatThrownBy(() -> new CompletionScore(79, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be >= 80");
    }

    @Test
    void should_accept_score_80_or_above() {
        CompletionScore score = new CompletionScore(85, List.of("gap1"));
        assertThat(score.score()).isEqualTo(85);
    }
}
```

#### BriefingServiceTest.java

```java
@Nested
class BriefingServiceTest {
    private BriefingService service;
    private BriefingSessionRepository sessionRepo;
    private BriefingQuestionRepository questionRepo;
    private BriefingAnswerRepository answerRepo;

    @BeforeEach
    void setup() {
        sessionRepo = mock(BriefingSessionRepository.class);
        questionRepo = mock(BriefingQuestionRepository.class);
        answerRepo = mock(BriefingAnswerRepository.class);
        service = new BriefingService(sessionRepo, questionRepo, answerRepo);
    }

    @Nested
    class StartBriefing {
        @Test
        void should_create_new_session() {
            service.startBriefing(workspaceId, clientId, serviceType);
            verify(sessionRepo).save(any(BriefingInProgress.class));
        }

        @Test
        void should_reject_duplicate_active_briefing() {
            when(sessionRepo.findActiveByClientAndService(clientId, serviceType))
                .thenReturn(Optional.of(mock(BriefingSession.class)));

            assertThatThrownBy(() -> service.startBriefing(workspaceId, clientId, serviceType))
                .isInstanceOf(BriefingAlreadyInProgressException.class)
                .hasMessage("Only 1 active briefing per client per service type");
        }
    }

    @Nested
    class SubmitAnswer {
        @Test
        void should_save_answer() {
            service.submitAnswer(sessionId, questionId, new AnswerText("Good answer"));
            verify(answerRepo).save(any(AnsweredDirect.class));
        }

        @Test
        void should_reject_empty_answer() {
            assertThatThrownBy(() -> service.submitAnswer(sessionId, questionId, new AnswerText("")))
                .isInstanceOf(InvalidAnswerException.class);
        }
    }

    @Nested
    class CompleteBriefing {
        @Test
        void should_complete_if_score_80_or_above() {
            BriefingInProgress inProgress = mock(BriefingInProgress.class);
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(inProgress));

            service.completeBriefing(sessionId, new CompletionScore(85, List.of()));
            verify(sessionRepo).save(any(BriefingCompleted.class));
        }

        @Test
        void should_reject_if_score_below_80() {
            assertThatThrownBy(() -> service.completeBriefing(sessionId, new CompletionScore(79, List.of())))
                .isInstanceOf(IncompleteGapsException.class);
        }
    }

    @Nested
    class AbandonBriefing {
        @Test
        void should_transition_to_abandoned() {
            BriefingInProgress inProgress = mock(BriefingInProgress.class);
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(inProgress));

            service.abandonBriefing(sessionId);
            verify(sessionRepo).save(any(BriefingAbandoned.class));
        }
    }
}
```

---

## File Checklist

- [ ] BriefingSession.java (sealed)
- [ ] BriefingInProgress.java
- [ ] BriefingCompleted.java
- [ ] BriefingAbandoned.java
- [ ] BriefingAnswer.java (sealed)
- [ ] AnsweredDirect.java
- [ ] AnsweredWithFollowup.java
- [ ] BriefingQuestion.java
- [ ] BriefingSessionId.java (record)
- [ ] QuestionId.java (record)
- [ ] AnswerId.java (record)
- [ ] AnswerText.java (record)
- [ ] PublicToken.java (record)
- [ ] CompletionScore.java (record)
- [ ] BriefingProgress.java (record)
- [ ] AIGeneration.java (record)
- [ ] ClientId.java (record, or import from shared)
- [ ] GenerationType.java (enum)
- [ ] ServiceType.java (enum or import)
- [ ] BriefingService.java
- [ ] BriefingSessionRepository.java
- [ ] BriefingQuestionRepository.java
- [ ] BriefingAnswerRepository.java
- [ ] AIGenerationRepository.java
- [ ] BriefingDomainException.java
- [ ] BriefingNotFoundException.java
- [ ] BriefingAlreadyCompletedException.java
- [ ] InvalidAnswerException.java
- [ ] MaxFollowupExceededException.java
- [ ] IncompleteGapsException.java
- [ ] DomainEvent.java (sealed interface)
- [ ] BriefingSessionStarted.java
- [ ] QuestionAsked.java
- [ ] AnswerSubmitted.java
- [ ] FollowupQuestionGenerated.java
- [ ] BriefingCompletedEvent.java
- [ ] BriefingAbandonedEvent.java
- [ ] package-info.java

**Test Files:**
- [ ] BriefingSessionTest.java (10+ nested tests)
- [ ] BriefingAnswerTest.java (5+ tests)
- [ ] AnswerTextTest.java (4+ tests)
- [ ] PublicTokenTest.java (4+ tests)
- [ ] CompletionScoreTest.java (4+ tests)
- [ ] BriefingProgressTest.java (4+ tests)
- [ ] BriefingServiceTest.java (15+ nested tests)
- [ ] ValueObjectTests (8+ test classes)

---

## Success Criteria

When done, Step 2 is complete if:
- [x] All 26+ Java files compile without errors
- [x] No Spring/JPA annotations in domain layer
- [x] All 50+ unit tests pass with `./mvnw test`
- [x] All sealed classes use proper `permits`
- [x] All records use compact constructor validation
- [x] All invariants from ADR enforced in domain
- [x] All domain services follow ADR specification
- [x] All exceptions have stable error codes (BRIEFING-001..005)
- [x] Repository interfaces have no Spring annotations
- [x] Code compiles: `./mvnw compile`

---

## Git Workflow

1. You're on branch: `feature/sprint-1b-briefing-domain`
2. Create directory: `src/main/java/com/scopeflow/core/domain/briefing/`
3. Create directory: `src/test/java/com/scopeflow/core/domain/briefing/`
4. Implement all 26+ Java files
5. Implement all 8+ test files (50+ tests)
6. Run tests: `./mvnw test`
7. Commit: `feat(backend-dev): implement-briefing-domain-sealed-classes-records-services`
8. Push to origin

---

## Timeline

**Start:** After architect Step 1 ✅ (just completed)
**Duration:** ~2-3 days
**Dependency:** ADR-002 (read thoroughly first)
**Next Step:** DBA (Step 3) — create V3 migration

---

## Reference Materials

- **ADR-002:** `docs/architecture/adr/ADR-002-briefing-domain.md`
- **Terminal 1 Example:** `src/main/java/com/scopeflow/core/domain/user/` (reference patterns)
- **Project CLAUDE.md:** `./ CLAUDE.md` (code style, Java 21 features, testing)
- **Java 21 Sealed Classes:** https://openjdk.java.net/jeps/425
- **Java 21 Records:** https://openjdk.java.net/jeps/395

---

**Ready. Implement the Briefing domain.** 🛠️
