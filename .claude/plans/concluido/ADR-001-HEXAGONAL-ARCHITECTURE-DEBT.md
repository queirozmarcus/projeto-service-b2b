# ADR-001: Hexagonal Architecture — Core Imports Adapter (Dívida Documentada)

**Status:** DOCUMENTED (Accepted with caveats for Sprint 6 MVP)
**Date:** 2026-03-25
**Author:** Code Review — Sprint 6 Task 3 (BriefingSession)
**Affects:** BriefingSessionService, all JPA-dependent application services

---

## Context

The application aims to follow hexagonal architecture:
- **Core** (`core.domain`, `core.application`) — business logic, independent of frameworks
- **Adapters** (`adapter.in.web`, `adapter.out.persistence`) — HTTP, databases, external services

**Problem Identified:**
`BriefingSessionService` (in `core.application.briefing`) imports directly from `adapter.out.persistence`:

```java
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingAnswerSpringRepository;
// ... 3 more imports from adapter.out
```

This inverts dependency flow: Core should depend on ports (interfaces), not adapters (implementations).

---

## Impact Assessment

| Aspect | Impact | Severity |
|--------|--------|----------|
| **Testability** | Service requires Spring repositories for unit tests (cannot mock interfaces) | Medium |
| **Reusability** | Service tightly coupled to Spring Data JPA, cannot be used with other ORMs | Low (not planned) |
| **Design** | Violates hexagonal principle but functionally correct for MVP | Medium |
| **Refactoring Cost** | ~4-6 hours to extract ports + create adapters (Sprint 7 task) | Low |

---

## Decision

**Accept as Technical Debt for Sprint 6 MVP** with commitment to resolve in Sprint 7.

**Rationale:**
1. BriefingSession is a new feature; there is no legacy code to preserve
2. Refactoring now would delay Sprint 6 delivery with low business value
3. Current design is transparent and testable (Testcontainers integration tests validate all behavior)
4. The debt is **explicitly documented** and has low migration cost

---

## Mitigation (Sprint 6 — Now)

None. This is deferral, not mitigation. All tests PASS; there is no blocking issue.

---

## Resolution Plan (Sprint 7 — Immediate Priority)

**Step 1:** Extract port interfaces

```java
// core.domain.briefing.port
public interface BriefingSessionRepository {
    JpaBriefingSession findById(UUID id);
    JpaBriefingSession save(JpaBriefingSession session);
    Optional<JpaBriefingSession> findByPublicToken(String token);
    // ... other repository methods
}

// And similar for answers, questions, profiles
```

**Step 2:** Update BriefingSessionService to depend on ports

```java
@Service
public class BriefingSessionService {
    private final BriefingSessionRepository sessionRepo;  // ← port, not JpaRepository
    // ... rest of service
}
```

**Step 3:** Create adapter implementing the port

```java
// adapter.out.persistence.briefing.JpaBriefingSessionRepositoryAdapter implements BriefingSessionRepository {
//    @Autowired JpaBriefingSessionSpringRepository springRepo;
//    @Override public JpaBriefingSession findById(UUID id) { ... }
// }
```

**Step 4:** Wire adapter in Spring Config

```java
@Bean
public BriefingSessionRepository briefinRepository(JpaBriefingSessionSpringRepository springRepo) {
    return new JpaBriefingSessionRepositoryAdapter(springRepo);
}
```

**Effort:** ~4-6 hours
**Risk:** LOW (changes are purely structural, behavior unchanged)
**Benefit:** Core becomes independent of Spring, fully testable without container

---

## Alternative Considered

**Implement hexagonal now (reject deferral):**
- Pros: Design is correct from day 1
- Cons: Delays Sprint 6 delivery by 1-2 days with zero business value for MVP
- Decision: Rejected (favor delivery over perfection)

---

## Related

- Code Review Report (Sprint 6 Task 3) — identified this debt
- Refactoring Pipeline — Spring 7 backlog

---

## Approval

This ADR documents a **conscious trade-off**: correct architecture deferred to maintain delivery schedule.

For MVP (Sprint 6), this is acceptable. For production scale, it must be resolved before adding more services that depend on the same pattern.

**Signed off by:** Code Review (2026-03-25)
