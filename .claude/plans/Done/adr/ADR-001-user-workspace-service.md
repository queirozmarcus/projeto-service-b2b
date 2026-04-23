# ADR-001: User & Workspace Domain Service Architecture

**Date:** 2026-03-22
**Status:** ✅ ACCEPTED
**Decision Maker:** Architect (Agent)
**Affected:** Sprint 1 Domain Layer Implementation

---

## Problem Statement

ScopeFlow MVP requires a foundational **User & Workspace** bounded context that:
- Manages multi-tenancy (workspace isolation, RBAC)
- Handles user authentication & authorization
- Enforces domain invariants (e.g., every workspace has exactly 1 OWNER)
- Communicates with Briefing & Proposal domains without coupling

**Questions answered by this ADR:**
1. What is the single responsibility of this service?
2. How does it communicate with other bounded contexts?
3. Which database tables does it own exclusively?
4. What are the key architectural trade-offs?

---

## Context

### ScopeFlow MVP Architecture
- **Style:** Hexagonal (ports & adapters) with domain-driven design
- **Multi-tenancy:** Every operation scoped to a workspace
- **Type Safety:** Java 21 sealed classes + records (immutable value objects)
- **Async Communication:** Domain events published via Kafka or RabbitMQ
- **Database:** PostgreSQL 16 with JSONB, versioned migrations (Flyway)

### Bounded Contexts (3 in Sprint 1)
1. **User & Workspace** (THIS) — Foundation, no dependencies
2. **Briefing** — Depends on User & Workspace
3. **Proposal** — Depends on Briefing

### Key Constraints
- Domain layer: **zero framework dependencies** (pure Java)
- No JPA annotations in domain entities
- Repository interfaces defined in domain; implementations in adapter layer
- Domain events enable eventual consistency with other contexts

---

## Decision

### 1. Single Responsibility

**The User & Workspace Domain Service owns:**

> *Manage user lifecycle, workspace multi-tenancy, and role-based access control, enabling secure isolation of client data and coordinating downstream discovery & approval workflows.*

**In practice:**
- Register users (email + password + profile)
- Create workspaces (owned by creator)
- Invite members & assign roles (OWNER, ADMIN, MEMBER)
- Validate permissions (who can do what in which workspace)
- Publish domain events for user registration & membership changes

---

### 2. Communication with Other Bounded Contexts

| Bounded Context | Pattern | Trigger | Medium |
|-----------------|---------|---------|--------|
| **Briefing** | Asynchronous | `UserRegistered` event | Kafka/RabbitMQ |
| **Briefing** | Synchronous (query) | "Get briefing owner workspace" | REST API call |
| **Proposal** | Asynchronous | `WorkspaceMemberInvited` event | Kafka/RabbitMQ |
| **Proposal** | Synchronous (query) | "Validate approval role" | REST API call |

**Rationale:**
- **Events for notification:** User & Workspace publishes events; other domains subscribe and react
- **Sync queries for reads:** Other domains can query User & Workspace for validation (e.g., "Is this member OWNER?")
- **No reverse dependency:** Briefing & Proposal never call User & Workspace to mutate data
- **Eventual consistency:** If a member role changes, downstream domains learn via event

---

### 3. Data Ownership

**Exclusively owned tables:**

```
users
├── id (UUID, PK)
├── email (unique, indexed)
├── password_hash (bcrypt)
├── full_name
├── phone
├── status (ACTIVE, INACTIVE, DELETED)
├── created_at, updated_at
└── [No foreign keys to other domains]

workspaces
├── id (UUID, PK)
├── name
├── owner_id (FK → users)
├── niche (e.g., "social-media", "landing-page")
├── tone_settings (JSONB)
├── created_at, updated_at
└── [No FK to briefing/proposal]

workspace_members
├── id (UUID, PK)
├── workspace_id (FK → workspaces)
├── user_id (FK → users)
├── role (enum: OWNER, ADMIN, MEMBER)
├── joined_at
├── [Composite unique: workspace_id + user_id]
```

**Shared via events (not tables):**
- Domain events published: `UserRegistered`, `WorkspaceMemberInvited`, `WorkspaceMemberRoleChanged`
- Other domains consume events to maintain their own read models (e.g., "briefing_owners", "proposal_approvers")

---

### 4. Domain Layer Design

#### Sealed Classes (Type Safety)

```java
// User entity — sealed class with subtypes for state
public sealed class User permits UserActive, UserInactive, UserDeleted {
    // Only these 3 states are possible (compiler verified)
}

public final class UserActive extends User {
    // Can login, change password
}

public final class UserInactive extends User {
    // Invited but not yet confirmed
}

public final class UserDeleted extends User {
    // Soft-deleted (GDPR compliance)
}
```

**Benefits:**
- Exhaustive pattern matching at compile time
- Impossible to create invalid user states
- No null checks (sealed classes force all cases to be handled)

#### Value Objects (Records)

```java
// Email — immutable, validated
public record Email(String value) {
    public Email {
        // Compact constructor enforces validation
        if (!isValid(value)) throw new IllegalArgumentException(...);
    }
}

// UserId — wrapped UUID
public record UserId(UUID value) { }

// PasswordHash — enforces bcrypt format
public record PasswordHash(String value) { }
```

**Benefits:**
- Zero boilerplate (records auto-generate equals, hashCode, toString)
- Immutable by design (no setters)
- Validation in constructor

#### Domain Events

```java
public record UserRegistered(
    UserId userId,
    Email email,
    Instant timestamp
) {}

public record WorkspaceMemberInvited(
    WorkspaceId workspaceId,
    UserId memberId,
    Role role,
    Instant timestamp
) {}
```

**Benefits:**
- Other bounded contexts subscribe and react
- Audit trail (all events stored in event log)
- Enables temporal queries ("What happened to this user?")

#### Domain Services

```java
@Service
public class UserService {
    // Business logic: invariants, workflows

    public UserActive registerUser(Email email, String password, String name) {
        // Enforce invariant: email must be unique
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        // Create user, publish event
        UserActive user = User.createActive(email, password, name);
        eventPublisher.publish(new UserRegistered(...));
        return user;
    }
}
```

---

## Consequences

### Positive
✅ **Clean boundaries:** User & Workspace is independent; other domains depend on it, not vice versa
✅ **Type safety:** Sealed classes + records eliminate entire categories of bugs
✅ **Auditability:** Domain events logged; full history available
✅ **Scalability:** Domain events enable async processing (no blocking dependencies)
✅ **Testing:** Domain layer is pure Java; trivial to unit test (no DB, no Spring)

### Trade-offs
⚠️ **Sealed classes:** Requires Java 17+ (we use 21, so OK)
⚠️ **Event sourcing overhead:** Event publishing adds latency; mitigate with async publishing
⚠️ **Synchronous queries:** Other domains must call REST API to validate permissions (network round-trip)
⚠️ **Eventual consistency:** Member role changes not instantly visible to Proposal domain (acceptable for MVP)

### Risks & Mitigations
| Risk | Mitigation |
|------|-----------|
| Event publishing fails (message lost) | Outbox table in User & Workspace schema; Kafka connector polls and publishes |
| Other domain doesn't receive event | Implement retry + DLQ (Dead Letter Queue) in RabbitMQ |
| Sync API call slow (blocking) | Cache workspace/member info in other domains; refresh on event |

---

## Implementation Roadmap

### Phase 1: Domain Layer (This Sprint)
- [ ] User sealed class + subtypes (UserActive, UserInactive, UserDeleted)
- [ ] Workspace sealed class + subtypes (WorkspaceActive, WorkspaceSuspended)
- [ ] WorkspaceMember sealed class + subtypes
- [ ] Value objects: Email, PasswordHash, UserId, WorkspaceId, Role
- [ ] Domain services: UserService, WorkspaceService
- [ ] Repository interfaces (no JPA here)
- [ ] Domain events: UserRegistered, WorkspaceMemberInvited, WorkspaceMemberRoleChanged
- [ ] 100+ unit tests (no DB, pure Java)

### Phase 2: Adapter Layer (Next Sprint)
- [ ] JPA entity mappings (UserJpaEntity ↔ User domain)
- [ ] Spring Data JPA repository implementations
- [ ] REST controllers (AuthController, WorkspaceController)
- [ ] Event publisher (Spring Integration → Kafka/RabbitMQ)
- [ ] 50+ integration tests (Testcontainers + PostgreSQL)

### Phase 3: Integration (Sprint 3)
- [ ] Use cases (RegisterUserUseCase, CreateWorkspaceUseCase)
- [ ] Global exception handler (Problem Details RFC 9457)
- [ ] OpenAPI documentation
- [ ] Observability (Prometheus metrics, structured logging)

---

## Alternative Approaches Considered

### Alternative 1: Monolithic Domain (All 3 contexts in one service)
- **Pros:** Simpler coordination, fewer network calls
- **Cons:** Harder to scale independently, tight coupling
- **Verdict:** ❌ Rejected (we chose 3 separate bounded contexts)

### Alternative 2: Synchronous RPC (REST/gRPC) for all communication
- **Pros:** Simpler than event sourcing, immediate consistency
- **Cons:** Tighter coupling, cascade failures (if User & Workspace down, Briefing fails)
- **Verdict:** ⚠️ Partial: Use sync for queries (reads), async for events (mutations)

### Alternative 3: Share database (multi-tenant schema)
- **Pros:** ACID transactions across all domains
- **Cons:** Tight coupling, schema drift, hard to scale independently
- **Verdict:** ❌ Rejected (separate schemas, eventual consistency)

---

## Approval & Sign-off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Architect | Marcus (delegate) | 2026-03-22 | ✅ |
| Tech Lead | (User) | — | ⏳ |

---

## Related Decisions

- **ADR-002:** Briefing Domain Architecture (pending)
- **ADR-003:** Proposal Domain Architecture (pending)
- **Sprint 1 Plan:** `.claude/plans/sprint-1-domain-layer-full.md`

---

## References

- Domain-Driven Design (Eric Evans)
- Building Microservices (Sam Newman) — Ch. 4 (Async Communication)
- Java 21 Sealed Classes (JEP 409, 425)
- Event Sourcing (Martin Fowler)

