# V11 Migration Summary: DROP User Tables

**Date:** 2026-04-21  
**Context:** Strangler Fig Phase 3 — Decommission user tables from monolith  
**Risk Level:** MEDIUM (requires backup, reversible via restore)

---

## What Was Created

### 1. Migration V11 (5.2 KB)
**File:** `backend/src/main/resources/db/migration/V11__drop_user_tables.sql`

**Actions:**
- DROP 3 foreign key constraints (`fk_workspaces_owner`, `fk_workspace_members_user`, `fk_activity_logs_user`)
- DROP 2 views (`v_workspace_members_active`, `v_workspace_owners`)
- DROP TABLE `users CASCADE`
- ADD COMMENT to 4 orphaned UUID columns documenting cross-service references

**Impact:**
- `users` table removed from monolith DB (`scopeflow`)
- Columns `owner_id`, `user_id`, `created_by` remain as plain UUIDs (no FK constraint)
- User data continues to live in user-service DB (`scopeflow_users`)

### 2. Execution Procedure (7.1 KB)
**File:** `docs/migration/V11-DROP-USER-TABLES-PROCEDURE.md`

**Contents:**
- Pre-flight checklist (5 validation steps)
- Step-by-step execution (stop → migrate → validate → restart)
- Post-migration SQL validation queries
- Rollback plan (restore from backup)
- Integration test commands
- Success criteria checklist

### 3. Data Ownership Map (5.1 KB)
**File:** `docs/migration/context-maps/data-ownership.md`

**Contents:**
- Database topology (2 databases, ports 5432/5433)
- Table ownership matrix (13 tables classified by context)
- Cross-context FK inventory (4 orphaned references post-V11)
- Strangler Fig 4-phase plan (Phase 3 in progress)
- Eventual consistency strategies
- Backup schedule and recovery targets

---

## Foreign Keys Inventory

### Before V11 (3 FKs to users table):
1. `workspaces.owner_id` → `users.id` (ON DELETE RESTRICT)
2. `workspace_members.user_id` → `users.id` (ON DELETE CASCADE)
3. `activity_logs.user_id` → `users.id` (ON DELETE SET NULL)

### After V11 (0 FKs, 4 orphaned UUIDs):
1. `workspaces.owner_id` — plain UUID (no FK)
2. `workspace_members.user_id` — plain UUID (no FK)
3. `activity_logs.user_id` — plain UUID, nullable (no FK)
4. `proposal_versions.created_by` — plain UUID (never had FK)

**Validation:** Application enforces referential integrity at write time; eventual consistency via `UserDeleted` events.

---

## Critical Safety Measures

### Pre-Execution Checklist
- [ ] User-service healthy and stable (http://localhost:8081/actuator/health)
- [ ] Traefik routing verified (priority 100 > 50)
- [ ] JWT_SECRET identical in both services
- [ ] **Full DB backup created** (`pg_dump scopeflow > backup.sql`)
- [ ] Zero active sessions on monolith user endpoints

### Validation Queries (Post-Migration)
```sql
-- 1. Confirm users table is gone
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name = 'users';
-- Expected: 0 rows

-- 2. Confirm orphaned columns still exist
SELECT column_name, table_name FROM information_schema.columns
WHERE table_schema = 'public' AND column_name IN ('owner_id', 'user_id', 'created_by');
-- Expected: 4 rows

-- 3. Confirm no FK constraints reference users
SELECT constraint_name FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY' AND constraint_name LIKE '%user%';
-- Expected: 0 rows (or only fk_workspace_members_workspace)
```

---

## Execution Command

```bash
# DO NOT RUN without completing pre-flight checklist first!
cd /home/mq/iGitHub/projeto-service-b2b/backend
./mvnw flyway:migrate

# Expected output:
# Successfully applied 1 migration to schema "public", now at version v11
```

---

## Rollback Strategy

If any validation fails:

```bash
# Stop monolith
docker stop scopeflow-api

# Restore backup
docker exec -i scopeflow-postgres psql -U postgres -d scopeflow < /tmp/scopeflow-backup-YYYYMMDD-HHMMSS.sql

# Restart
docker start scopeflow-api
```

**Recovery Time:** ~5-10 minutes (depends on DB size)

---

## Next Steps After V11

### Immediate (Week 1)
- Monitor logs for FK-related errors: `docker logs scopeflow-api -f | grep -i "user_id\|foreign key"`
- Validate workspace/proposal operations continue normally
- Keep backup for minimum 30 days

### Short-Term (Week 2-4)
- Implement `UserDeleted` event consumer in monolith
- Update `activity_logs.user_id = NULL` on user deletion (audit trail)
- Performance baseline: compare query latency (workspace operations)

### Medium-Term (Phase 4 — Backlog)
- Remove `AuthControllerV2` proxy from monolith (Traefik handles all routing)
- Remove `UserEntity`, `UserRepository` JPA classes from monolith
- Remove `User` domain model from `core/domain/user/`
- Archive monolith user tests (migrated to user-service)

---

## Success Criteria

- [x] Migration V11 created and documented
- [ ] Pre-flight checklist completed (5/5 items)
- [ ] Migration applied without errors
- [ ] Post-migration validation passed (3/3 SQL queries)
- [ ] User-service operational (auth endpoints responding)
- [ ] Monolith operational (workspace/proposal endpoints responding)
- [ ] Zero FK constraint errors in logs (first 24 hours)
- [ ] Backup validated and stored (minimum 30 days retention)

---

## Files Created

| File | Size | Purpose |
|------|------|---------|
| `backend/src/main/resources/db/migration/V11__drop_user_tables.sql` | 5.2 KB | Flyway migration: DROP users table + FKs |
| `docs/migration/V11-DROP-USER-TABLES-PROCEDURE.md` | 7.1 KB | Step-by-step execution guide |
| `docs/migration/context-maps/data-ownership.md` | 5.1 KB | Complete data ownership map |
| `docs/migration/V11-MIGRATION-SUMMARY.md` | This file | Executive summary |

---

## Approval Required

**Before executing V11 in production:**
- [ ] Tech Lead review and approval
- [ ] DBA review (backup strategy, rollback plan)
- [ ] Product Owner notification (maintenance window required)
- [ ] Staging environment dry-run successful

**Estimated Downtime:** 2-5 minutes (stop monolith → migrate → restart)

---

## Questions or Issues?

Contact: Data Engineer / Tech Lead  
Reference: Strangler Fig Phase 3 (User Service extraction)
