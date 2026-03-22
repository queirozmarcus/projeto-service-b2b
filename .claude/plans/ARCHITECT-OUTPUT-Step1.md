# Step 1: Architect Output — User & Workspace Domain

**Date:** 2026-03-22
**Agent:** architect (opus)
**Status:** ✅ COMPLETED
**Next:** Delegate to backend-dev (Step 2)

---

## 1. Single Responsibility

**User & Workspace Domain Service owns:**
> *Manage user lifecycle, workspace multi-tenancy, and role-based access control, enabling secure isolation of client data and coordinating downstream discovery & approval workflows.*

---

## 2. Communication with Other Bounded Contexts

### Briefing Domain
- **Async:** `UserRegistered` event → triggers briefing template cache
- **Sync:** Query "Get briefing owner workspace" (REST call)

### Proposal Domain
- **Async:** `WorkspaceMemberInvited` event → triggers proposal initialization
- **Sync:** Query "Validate approval role" (REST call for permission check)

### Pattern
- **Mutations (writes):** Asynchronous via domain events → eventual consistency
- **Queries (reads):** Synchronous REST calls (faster, simpler for reads)
- **No reverse dependency:** Briefing & Proposal never call User & Workspace to mutate

---

## 3. Data Ownership

**Exclusively owned by User & Workspace:**

```
users (PK: id)
  ├── email (unique, indexed)
  ├── password_hash (bcrypt)
  ├── full_name, phone
  ├── status (ACTIVE, INACTIVE, DELETED)
  └── created_at, updated_at

workspaces (PK: id)
  ├── owner_id (FK → users)
  ├── name, niche
  ├── tone_settings (JSONB)
  └── created_at, updated_at

workspace_members (PK: id)
  ├── workspace_id (FK → workspaces)
  ├── user_id (FK → users)
  ├── role (OWNER, ADMIN, MEMBER)
  └── joined_at
```

**Shared via events (not owned):**
- Domain events published; other domains maintain read models

---

## 4. Architectural Decisions (ADR-001)

**File:** `.claude/plans/adr/ADR-001-user-workspace-service.md`

**Key decisions:**
- ✅ Sealed classes (UserActive, UserInactive, UserDeleted) for type safety
- ✅ Records for value objects (Email, UserId, PasswordHash, etc.)
- ✅ Domain events for async communication
- ✅ Domain layer: zero framework dependencies
- ✅ Repository interfaces in domain; implementations in adapter
- ✅ Multi-tenancy: workspace-scoped queries enforced at service layer

---

## Domain Model (High-Level)

```
User (sealed class)
├── UserActive (can login, change password)
├── UserInactive (invited, not confirmed)
└── UserDeleted (soft-deleted, GDPR)

Workspace (sealed class)
├── WorkspaceActive (normal)
└── WorkspaceSuspended (owner paused)

WorkspaceMember (sealed class)
├── MemberActive (currently a member)
├── MemberInvited (invited, waiting acceptance)
└── MemberLeft (removed, historical)

Value Objects:
├── Email (validated, case-insensitive)
├── PasswordHash (bcrypt-formatted)
├── UserId (wrapped UUID)
├── WorkspaceId (wrapped UUID)
└── Role (enum: OWNER, ADMIN, MEMBER)

Domain Events:
├── UserRegistered
├── WorkspaceMemberInvited
└── WorkspaceMemberRoleChanged

Domain Services:
├── UserService (registerUser, authenticateUser)
└── WorkspaceService (createWorkspace, inviteMember, updateRole)
```

---

## Constraints for Implementation

1. **Domain layer (com.scopeflow.core.domain):**
   - No @Entity, @Service, @Repository annotations
   - No JPA, no Spring dependencies
   - Pure Java: sealed classes, records, interfaces
   - Tests: JUnit 5 + AssertJ + Mockito (no DB)

2. **Adapter layer (com.scopeflow.adapter):**
   - @Entity classes map domain to JPA
   - @Repository implementations use Spring Data JPA
   - @Controller exposes REST endpoints

3. **Type safety (Java 21):**
   - Sealed classes enforce exhaustive pattern matching
   - Records ensure immutability
   - No null checks needed (sealed classes handle all cases)

4. **Invariants (enforced in domain):**
   - Every workspace has exactly 1 OWNER
   - Cannot remove last OWNER (invariant violation)
   - Email must be unique (invariant)
   - Password must be hashed (invariant)

---

## Estimated Deliverables (This Terminal 1)

### Code
- 9+ sealed classes (User, Workspace, WorkspaceMember, and subtypes)
- 6+ value objects (Email, PasswordHash, UserId, WorkspaceId, Role)
- 2+ domain services (UserService, WorkspaceService)
- 3+ repository interfaces (UserRepository, WorkspaceRepository, WorkspaceMemberRepository)
- 5+ domain events (UserRegistered, WorkspaceMemberInvited, WorkspaceMemberRoleChanged, etc.)
- Exception hierarchy (domain-specific exceptions)

### Tests
- 40+ unit tests (100% domain coverage, no DB)
- 15+ integration tests (Testcontainers + PostgreSQL)

### Documentation
- ADR-001 (this file)
- Domain model diagram (Mermaid)
- Service contract documentation

---

## Timeline

**Step 1 (Architect):** ✅ COMPLETED (now)
**Step 2 (Backend-Dev):** ⏳ Next — Create hexagonal structure + configs
**Step 3 (DBA):** ⏳ Schema Flyway migration
**Step 4 (API-Designer):** ⏳ Health + OpenAPI endpoints
**Step 5 (DevOps-Engineer):** ⏳ Docker + Helm + CI/CD
**Step 6 (Marcus):** ⏳ Consolidation + QA integration

**Total Terminal 1:** ~5-7 days (with parallel subagents)

---

## Next Action

✅ **Architecture approved and documented**

**Passing baton to:** `backend-dev` (Step 2)
- Task: Create hexagonal structure, config files, placeholder use case
- Input: This ADR + architectural design
- Output: Project structure + application.yml + GlobalExceptionHandler + placeholder test

