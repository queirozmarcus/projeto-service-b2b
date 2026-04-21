# V11 Migration: DROP User Tables from Monolith

**Status:** READY FOR EXECUTION  
**Created:** 2026-04-21  
**Context:** User Service extraction via Strangler Fig (DB-per-service completed)

---

## Executive Summary

Esta migration remove as tabelas do contexto User do banco de dados do monólito após a extração bem-sucedida do user-service. O user-service agora possui seu próprio banco dedicado (`scopeflow_users`, porta 5433).

**Tabelas a remover:**
- `users` (tabela principal)
- `v_workspace_members_active` (view)
- `v_workspace_owners` (view)

**Foreign Keys afetadas:**
- `workspaces.owner_id` → `users.id` (ON DELETE RESTRICT)
- `workspace_members.user_id` → `users.id` (ON DELETE CASCADE)
- `activity_logs.user_id` → `users.id` (ON DELETE SET NULL)

---

## Pre-Flight Checklist

Validar **TODOS** os itens antes de executar:

### 1. User-Service Stability
```bash
# Verificar health do user-service
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP"}

# Verificar últimos logs (sem erros)
docker logs scopeflow-user-service --tail 50
```

### 2. Routing Verification
```bash
# Traefik deve rotear /auth e /users para user-service
curl -I http://localhost/api/v1/auth/health
# Expected: HTTP/1.1 200 (via Traefik → user-service)

# Verificar regras Traefik
docker exec scopeflow-traefik cat /etc/traefik/traefik.yml
# Confirm priority 100 for user-service > 50 for api (monolith)
```

### 3. JWT Secret Alignment
```bash
# Ambos devem ter o mesmo JWT_SECRET
docker inspect scopeflow-api | grep JWT_SECRET
docker inspect scopeflow-user-service | grep JWT_SECRET
# Expected: valores idênticos
```

### 4. Database Backup (CRÍTICO)
```bash
# Backup completo do banco monólito
docker exec scopeflow-postgres pg_dump -U postgres scopeflow > /tmp/scopeflow-backup-$(date +%Y%m%d-%H%M%S).sql

# Validar backup (deve ter >1MB e conter CREATE TABLE users)
ls -lh /tmp/scopeflow-backup-*.sql
grep "CREATE TABLE users" /tmp/scopeflow-backup-*.sql
```

### 5. Zero Active Sessions
```bash
# Verificar endpoints monólito não estão sendo usados
docker logs scopeflow-api --tail 100 | grep -E "POST /api/v1/auth|GET /api/v1/users"
# Expected: zero hits (Traefik está interceptando)
```

---

## Execution Steps

### Step 1: Stop Monolith (Maintenance Window)
```bash
docker stop scopeflow-api
# User-service continua rodando — auth não é interrompido
```

### Step 2: Apply Migration
```bash
cd /home/mq/iGitHub/projeto-service-b2b/backend

# Dry-run: Verificar migrations pendentes
./mvnw flyway:info

# Apply V11
./mvnw flyway:migrate

# Expected output:
# Successfully applied 1 migration to schema "public", now at version v11
```

### Step 3: Post-Migration Validation
```sql
-- Connect to monolith DB
docker exec -it scopeflow-postgres psql -U postgres -d scopeflow

-- 1. Confirm users table is gone
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name = 'users';
-- Expected: 0 rows

-- 2. Confirm orphaned columns still exist (UUID, no FK)
SELECT column_name, table_name FROM information_schema.columns
WHERE table_schema = 'public' AND column_name IN ('owner_id', 'user_id', 'created_by');
-- Expected: 4 rows (workspaces.owner_id, workspace_members.user_id, 
--                   activity_logs.user_id, proposal_versions.created_by)

-- 3. Confirm no FK constraints reference users
SELECT constraint_name, table_name FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY' AND constraint_name LIKE '%user%';
-- Expected: 0 rows (or only fk_workspace_members_workspace, not fk_*_user)

-- 4. Verify Flyway applied V11
SELECT version, description, installed_on FROM flyway_schema_history 
ORDER BY installed_rank DESC LIMIT 1;
-- Expected: version=11, description="drop user tables"
```

### Step 4: Restart Monolith
```bash
docker start scopeflow-api

# Wait for healthy
docker logs scopeflow-api -f
# Expected: "Started ScopeFlowApplication in X seconds"
```

### Step 5: Integration Tests
```bash
# Auth via user-service (deve continuar funcionando)
curl -X POST http://localhost/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","fullName":"Test User"}'
# Expected: 201 Created

# Workspace operations no monólito (owner_id agora é UUID sem FK)
curl -X POST http://localhost/api/v2/workspaces \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Workspace","niche":"web-development"}'
# Expected: 201 Created (owner_id persiste normalmente)
```

---

## Rollback Plan

Se qualquer validação falhar:

### Option A: Restore from Backup (Full Rollback)
```bash
# Stop monolith
docker stop scopeflow-api

# Restore backup
docker exec -i scopeflow-postgres psql -U postgres -d scopeflow < /tmp/scopeflow-backup-YYYYMMDD-HHMMSS.sql

# Restart
docker start scopeflow-api
```

### Option B: Revert Migration (Flyway)
```bash
# Create V12__restore_user_tables.sql (reverse of V11)
# Copy table definitions from V2__user_workspace_domain_schema.sql

cd backend
./mvnw flyway:migrate
```

**Note:** Option A é preferível — restaura estado exato pré-migration.

---

## Data Ownership After Migration

| Table | Owner | Cross-Context References |
|-------|-------|--------------------------|
| `users` | **user-service** (scopeflow_users DB) | — |
| `workspaces` | monolith | `owner_id` → user-service (no FK) |
| `workspace_members` | monolith | `user_id` → user-service (no FK) |
| `activity_logs` | monolith | `user_id` → user-service (no FK, nullable) |
| `proposal_versions` | monolith | `created_by` → user-service (no FK) |

**Eventual Consistency Strategy:**
- User deletions no user-service devem publicar evento `UserDeleted`
- Monólito consome evento e atualiza `activity_logs.user_id = NULL` (audit trail preservado)
- Workspaces/proposals mantêm UUIDs órfãos — acceptable (user já foi validado no momento da criação)

---

## Post-Deployment Monitoring

### Week 1: Watch for Anomalies
```bash
# Monitor errors relacionados a user_id
docker logs scopeflow-api -f | grep -i "user_id\|owner_id\|foreign key"

# Monitor latência de workspace operations
curl http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/v2/workspaces
```

### Week 2: Performance Baseline
- Compare query performance (workspaces, proposals) — sem FK, joins via application
- Validar que zero queries falham por "missing FK constraint"

---

## Success Criteria

- ✅ Migration V11 aplicada sem erros
- ✅ Tabela `users` removida do banco monólito
- ✅ User-service continua operacional (auth, profile endpoints)
- ✅ Monólito continua operacional (workspace, proposals endpoints)
- ✅ Zero erros de FK constraint em logs
- ✅ Backup validado e armazenado (mínimo 30 dias)

---

## References

- Migration file: `backend/src/main/resources/db/migration/V11__drop_user_tables.sql`
- User-service migration: `user-service/src/main/resources/db/migration/V1__create_users_table.sql`
- Strangler Fig ADR: `docs/architecture/adr/001-strangler-fig-user-service.md`
- Cut-over plan: `docs/migration/DB-MIGRATION-USER-SERVICE.md`
