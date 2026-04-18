# Stack Validation Report — 2026-04-18

## Sumário Executivo

**Status Geral:** ❌ FAILED — User-service em crash loop devido a configuração incorreta no `.env`

**Serviços Operacionais:** 6/7
- ✅ postgres (scopeflow DB)
- ✅ user-db (scopeflow_users DB)
- ✅ rabbitmq
- ✅ redis
- ✅ traefik
- ✅ app (monólito)
- ❌ user-service (restart loop — exit code 1)

---

## Root Cause Analysis

### Problema Identificado

User-service conecta em `localhost:5432/scopeflow` em vez de `user-db:5432/scopeflow_users`.

**Evidência:**
```
Caused by: org.postgresql.util.PSQLException: Connection to localhost:5432 refused.
```

### Causa Raiz

**Arquivo `.env` contém configuração OBSOLETA (dual-write pré-cutover):**
```bash
# .env (ATUAL — ERRADO)
USER_SERVICE_DATABASE_URL=jdbc:postgresql://localhost:5432/scopeflow
```

**Esperado (DB-per-service consolidado):**
```bash
USER_SERVICE_DATABASE_URL=jdbc:postgresql://user-db:5432/scopeflow_users
```

### Por Que Aconteceu

1. `.env` tem precedência sobre `docker-compose.yml`
2. Variável `USER_SERVICE_DATABASE_URL` no `.env` sobrescreve default do compose
3. Valor antigo refletia fase de dual-write (antes do cut-over)
4. DB-per-service foi consolidado, mas `.env` não foi atualizado

---

## Validações Executadas

### ✅ Infraestrutura Base

| Componente | Status | Detalhes |
|------------|--------|---------|
| postgres (5432) | ✅ Healthy | DB `scopeflow` criado, tabelas presentes |
| user-db (5433) | ✅ Healthy | DB `scopeflow_users` criado |
| rabbitmq | ✅ Healthy | Management UI acessível (15672) |
| redis | ✅ Healthy | `PING` → `PONG` |
| traefik | ✅ Running | Dashboard (8888), rotas carregadas |

### ✅ Monólito (app)

- **Status:** Healthy (7 min uptime)
- **Conexão DB:** `postgres:5432/scopeflow` ✅
- **Health endpoint:** `/api/v1/actuator/health/readiness` → 200 OK
- **Logs:** Sem erros, FilterChain processando requests

### ❌ User Service

- **Status:** Restarting (exit 1)
- **Causa:** HikariPool falha ao conectar em `localhost:5432`
- **DB correto:** `user-db:5432/scopeflow_users` existe e está healthy
- **Flyway migrations:** V1 presente no serviço (confirmado em logs anteriores)

### ✅ Traefik Routing (Strangler Fig)

**Arquivo `traefik/dynamic.yml` — configuração CORRETA:**
```yaml
http:
  routers:
    user-service:
      rule: "PathPrefix(`/api/v1/auth`) || PathPrefix(`/api/v1/users`)"
      priority: 100  # ✅ Maior precedência
      service: user-service (http://user-service:8081)

    monolith:
      rule: "PathPrefix(`/api/`)"
      priority: 50   # ✅ Menor precedência (catch-all)
      service: monolith (http://app:8080)
```

**Validação:** Traefik carregou rotas sem erros. Routing só será testável após user-service subir.

---

## Fix Proposto

### 1. Corrigir `.env` (Ação Obrigatória)

```bash
# Substituir linha antiga:
USER_SERVICE_DATABASE_URL=jdbc:postgresql://localhost:5432/scopeflow

# Por:
USER_SERVICE_DATABASE_URL=jdbc:postgresql://user-db:5432/scopeflow_users
```

**Justificativa:** Consolida DB-per-service. User-service deve usar banco dedicado.

### 2. Remover Comentário Obsoleto

```bash
# Remover linhas (pré-cutover):
# Durante dual-write (pré-cutover): USER_SERVICE_DATABASE_URL aponta para o
# USER_SERVICE_DATABASE_URL=jdbc:postgresql://localhost:5433/scopeflow_users
```

### 3. Rebuild + Restart

```bash
docker compose down user-service
docker compose build --no-cache user-service
docker compose up -d user-service
```

**Nota:** Rebuild em andamento (task `bxdvxu1z4`). Após fix no `.env`, repetir rebuild.

---

## Smoke Tests Pendentes

Após user-service subir:

### 1. Health Endpoints
```bash
# User-service direto
curl http://localhost:8081/api/v1/actuator/health

# User-service via Traefik (Strangler Fig)
curl http://localhost/api/v1/auth/health

# Monólito
curl http://localhost:8080/api/v1/actuator/health
curl http://localhost/api/health  # via Traefik
```

### 2. Tabelas no DB Correto
```bash
# Confirmar que user-service usa user-db
docker exec scopeflow-user-db psql -U postgres -d scopeflow_users -c '\dt'
# Deve listar: users, flyway_schema_history

# Confirmar que monólito usa postgres
docker exec scopeflow-postgres psql -U postgres -d scopeflow -c '\dt'
# Deve listar: workspaces, clients, briefings, proposals, etc.
```

### 3. JWT Cross-Compatibility
```bash
# Login via user-service (Traefik)
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Extrair token e validar no monólito
curl http://localhost/api/v1/workspaces \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Próximos Passos Recomendados

### Imediato (Blocker)
1. ✅ **Atualizar `.env`** — fix na linha `USER_SERVICE_DATABASE_URL`
2. ✅ **Rebuild user-service** — `docker compose build --no-cache user-service && docker compose up -d`
3. ⏳ **Validar logs** — confirmar conexão em `user-db:5432/scopeflow_users`
4. ⏳ **Executar smoke tests** — health + DB + routing

### Após Stack Healthy
5. **Smoke test completo** — fluxo de registro + login + JWT validation cross-service
6. **Documentar mudança** — atualizar `docs/migration/DB-MIGRATION-USER-SERVICE.md` com lição aprendida (`.env` deve ser sincronizado com compose em cut-overs)
7. **Commitar fix** — `git commit -am "fix(config): corrige USER_SERVICE_DATABASE_URL no .env para DB-per-service"`

### Backlog (Post-Validation)
8. **CI check** — adicionar validação no pipeline que compara variáveis `.env` vs `.env.example` (evitar drifts futuros)
9. **Staging test** — validar `docker-compose.staging.yml` override funciona corretamente
10. **Monitoramento** — configurar alertas Prometheus para connection pool failures (detectar issues similares em prod)

---

## Arquivos Relevantes

| Arquivo | Status | Ação Necessária |
|---------|--------|-----------------|
| `.env` | ❌ Obsoleto | Atualizar `USER_SERVICE_DATABASE_URL` |
| `docker-compose.yml` | ✅ Correto | Nenhuma |
| `traefik/dynamic.yml` | ✅ Correto | Nenhuma |
| `user-service/Dockerfile` | ✅ Correto | Nenhuma (rebuild após fix .env) |
| `docs/migration/DB-MIGRATION-USER-SERVICE.md` | ⚠️ Desatualizado | Adicionar seção sobre sincronização de `.env` |

---

## Lições Aprendidas

1. **`.env` tem precedência** sobre defaults do `docker-compose.yml` — sempre validar após mudanças arquiteturais
2. **Comentários inline podem enganar** — `.env` tinha comentário "Durante dual-write" mas valor não refletia fase atual
3. **Rebuild é obrigatório** após mudanças em env vars que afetam build-time (Spring Boot resolve properties no startup)
4. **DB-per-service exige coordenação** — cut-over completo requer atualização sincronizada de `.env`, compose overrides, e docs

---

## Comandos de Diagnóstico Utilizados

```bash
# Services status
docker compose ps

# Logs específicos
docker logs scopeflow-user-service --tail 60
docker logs scopeflow-api --tail 30

# Env vars runtime
docker inspect scopeflow-user-service --format '{{range .Config.Env}}{{println .}}{{end}}'

# DB validation
docker exec scopeflow-user-db psql -U postgres -c '\l'
docker exec scopeflow-postgres psql -U postgres -c '\l'

# Traefik config
cat traefik/dynamic.yml

# .env check
cat .env | grep -E "(DATASOURCE|DATABASE)"

# Rebuild
docker compose down user-service
docker compose build --no-cache user-service
docker compose up -d user-service
```
