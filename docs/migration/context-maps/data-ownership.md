# Data Ownership Map — ScopeFlow AI

**Last Updated:** 2026-04-21  
**Status:** User Service extracted (DB-per-service completed)

---

## Database Topology

| Database | Port | Owner Service | Schema Version |
|----------|------|---------------|----------------|
| `scopeflow` | 5432 | Monolith (backend) | Flyway V10 (soon V11) |
| `scopeflow_users` | 5433 | user-service | Flyway V1 |

---

## Table Ownership Matrix

| Table | Owner (Context) | Readers | Writers | Volume (est.) | External FKs | Status |
|-------|-----------------|---------|---------|---------------|--------------|--------|
| **users** | User (user-service) | Auth, Workspace | User Service | ~500-5K rows | — | ✅ Extracted |
| **workspaces** | Workspace (monolith) | Proposal, Briefing | Workspace | ~200-2K rows | `owner_id` → users (NO FK post-V11) | 🟡 Active |
| **workspace_members** | Workspace (monolith) | Auth (roles) | Workspace | ~500-10K rows | `user_id` → users (NO FK post-V11) | 🟡 Active |
| **activity_logs** | Audit (monolith) | Admin UI | All contexts | ~50K-500K rows | `user_id` → users (NO FK post-V11, nullable) | 🟡 Active |
| **briefing_sessions** | Briefing (monolith) | Proposal | Briefing | ~1K-10K rows | `workspace_id` → workspaces | 🟡 Active |
| **briefing_questions** | Briefing (monolith) | Briefing | Briefing | ~10-50 rows | `briefing_id` → briefing_sessions | 🟡 Active |
| **briefing_answers** | Briefing (monolith) | Proposal | Briefing | ~5K-50K rows | `session_id` → briefing_sessions | 🟡 Active |
| **proposals** | Proposal (monolith) | Approval | Proposal | ~500-5K rows | `briefing_id` → briefing_sessions, `workspace_id` → workspaces | 🟡 Active |
| **proposal_versions** | Proposal (monolith) | Approval | Proposal | ~1K-10K rows | `proposal_id` → proposals, `created_by` → users (NO FK) | 🟡 Active |
| **approval_workflows** | Proposal (monolith) | Approval | Proposal | ~500-5K rows | `proposal_id` → proposals | 🟡 Active |
| **approvals** | Proposal (monolith) | Approval | Proposal | ~1K-10K rows | `workflow_id` → approval_workflows | 🟡 Active |
| **outbox_events** | Infra (monolith) | Event Publisher | All contexts | ~10K-100K rows | — | 🟡 Active |
| **idempotency_records** | Infra (monolith) | Public endpoints | All contexts | ~5K-50K rows | — | 🟡 Active |

**Legend:**
- ✅ Extracted — owned by dedicated service
- 🟡 Active — still in monolith
- 🔴 Deprecated — scheduled for removal

---

## Cross-Context Foreign Keys (Post-V11)

| Source Table | Column | Target Table | Target Service | Constraint | Eventual Consistency Strategy |
|--------------|--------|--------------|----------------|------------|-------------------------------|
| `workspaces` | `owner_id` | `users.id` | user-service | **NO FK** (UUID only) | User deletion → `UserDeleted` event → workspace keeps UUID (acceptable orphan) |
| `workspace_members` | `user_id` | `users.id` | user-service | **NO FK** (UUID only) | User deletion → `UserDeleted` event → remove membership row |
| `activity_logs` | `user_id` | `users.id` | user-service | **NO FK** (UUID, nullable) | User deletion → `UserDeleted` event → SET NULL (audit trail preserved) |
| `proposal_versions` | `created_by` | `users.id` | user-service | **NO FK** (UUID only) | Immutable record — never updated (orphan acceptable) |

**Rationale for NO FK:**
- User Service owns `users` table in separate database (`scopeflow_users`)
- PostgreSQL foreign keys cannot span databases
- Eventual consistency via domain events (RabbitMQ)
- Application-level validation ensures referential integrity at write time

---

## Migration Strategy: Strangler Fig

### Phase 1: Extract (✅ COMPLETED)
1. Create user-service with dedicated DB (`scopeflow_users`)
2. Migrate schema (V1__create_users_table.sql in user-service)
3. Copy data from monolith → user-service (one-time ETL)
4. Deploy user-service + Traefik routing

### Phase 2: Dual-Read (✅ COMPLETED)
1. Traefik routes `/api/v1/auth/*` and `/api/v1/users/*` to user-service (priority 100)
2. Monolith keeps `AuthControllerV2` as fallback proxy (local dev without Traefik)
3. JWT tokens are cross-compatible (same `JWT_SECRET`)
4. Monitor for 2 weeks — zero errors

### Phase 3: Cutover (⏳ IN PROGRESS — V11 Migration)
1. Drop foreign keys from monolith → users table
2. Drop views (`v_workspace_members_active`, `v_workspace_owners`)
3. **DROP TABLE users CASCADE** (V11 migration)
4. Document orphaned UUID columns (comments in migration)

### Phase 4: Decommission (📋 BACKLOG)
1. Remove `AuthControllerV2` proxy from monolith (no longer needed)
2. Remove JPA entities: `UserEntity`, `UserRepository` from monolith
3. Remove domain model: `User` aggregate from `core/domain/user/` in monolith
4. Archive user-related tests in monolith (moved to user-service)

---

## References

- Migration V11: `backend/src/main/resources/db/migration/V11__drop_user_tables.sql`
- Procedure: `docs/migration/V11-DROP-USER-TABLES-PROCEDURE.md`
- User-service schema: `user-service/src/main/resources/db/migration/V1__create_users_table.sql`
