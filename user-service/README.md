# User Service — Strangler Fig Extraction

**Status:** ✅ **Extraído e operacional em staging** | Cut-over produção pendente

Ver: [DB_MIGRATION_GUIDE.md](docs/DB_MIGRATION_GUIDE.md) para passos do cut-over em produção.

---

## Visão Geral

User Service é o **primeiro microsserviço extraído** do monólito ScopeFlow via **Strangler Fig pattern** — 20/20 sprints concluídos.

**Responsabilidades:**
- Autenticação JWT (login, refresh token, logout)
- Registro de usuários
- Gerenciamento de perfil (`/users/me`, `/users/by-email`, `/users/invited`)

**Stack:**
- Java 21 (virtual threads, sealed classes, records)
- Spring Boot 3.4.3
- PostgreSQL 16 dedicado (`scopeflow_users`, porta 5433) — DB-per-service ativo em staging
- Traefik API Gateway (routing `/api/v1/auth/*` e `/api/v1/users/*`, prioridade 100)

---

## Arquitetura de Transição

### Fase Atual (Shared Database)

```
                    ┌─────────────┐
                    │   Traefik   │
                    │  (port 80)  │
                    └─────┬───────┘
                          │
                ┌─────────┴─────────┐
                │                   │
         /api/v1/auth/*      /api/v1/* (outros)
                │                   │
        ┌───────▼────────┐  ┌──────▼─────────┐
        │ User Service   │  │   Monólito     │
        │  (port 8081)   │  │  (port 8080)   │
        └───────┬────────┘  └──────┬─────────┘
                │                   │
                └─────────┬─────────┘
                          │
                    ┌─────▼──────┐
                    │ PostgreSQL │
                    │  (shared)  │
                    └────────────┘
```

**Características:**
- Routing por prefixo de rota (Traefik priority)
- Shared database (zero migração de dados)
- JWT secret compartilhado (transição sem downtime)
- Graceful shutdown < 30s (suporte a Spot instances)

### Fase Futura (Database per Service)

```
        ┌───────────────┐          ┌──────────────┐
        │ User Service  │          │   Monólito   │
        │  (port 8081)  │          │ (port 8080)  │
        └───────┬───────┘          └──────┬───────┘
                │                         │
        ┌───────▼───────┐         ┌──────▼───────┐
        │  User DB      │         │  Monolith DB │
        │ (dedicated)   │         │ (dedicated)  │
        └───────────────┘         └──────────────┘
```

**Migração planejada:**
- CDC (Change Data Capture) com Debezium
- Dual writes durante transição
- Feature flag para rollback

---

## Código Temporário (Etapa 3)

⚠️ **ATENÇÃO:** O código atual (`UserServiceApplication.java`, `application.yml`) é **temporário** para validar a infraestrutura Docker Compose.

**O que está implementado:**
- Dockerfile multi-stage (production-ready)
- docker-compose.yml (Traefik + user-service + shared DB)
- Health checks (liveness + readiness)
- Graceful shutdown (30s timeout)
- Traefik routing com prioridade

**O que NÃO está implementado (será feito na Etapa 4):**
- Controllers REST (`/api/v1/auth/login`, `/api/v1/auth/refresh`, etc.)
- Domain model (User, Role, Permission)
- JWT generation/validation
- Password hashing (BCrypt)
- Rate limiting (10 req/min public endpoints)

**Próxima etapa:** `/migration-extract user-auth` — extrai código do monólito para o user-service.

---

## Contract Tests

Contract tests validate the API contracts between user-service (provider) and consumers (monolith).
Uses Spring Cloud Contract (provider-side verification).

**Run contract tests:**
```bash
./mvnw test -Dtest="com.scopeflow.user.contract.*" --no-transfer-progress
```

**Run all tests including contracts:**
```bash
./mvnw verify -pl user-service
```

**Contracts location:** `src/test/resources/contracts/`
- `auth/` (6 contracts): register, login, refresh, me, logout
- `users/` (5 contracts): by-email lookup, invited user creation

**Quality gate:** Contract tests must pass before any deployment.
A failing contract = a broken API guarantee for consumers.

---

## Como Executar

### Build Local

```bash
cd user-service

# Baixar dependências
./mvnw dependency:go-offline

# Compilar
./mvnw clean package -DskipTests

# Rodar (requer PostgreSQL)
./mvnw spring-boot:run
```

### Docker Compose

```bash
# Build image
docker compose build user-service

# Start service
docker compose up user-service -d

# Verificar health
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness

# Verificar routing Traefik
curl http://localhost/api/v1/auth/health    # user-service
curl http://localhost/api/v1/workspaces      # monólito

# Logs
docker compose logs user-service -f

# Stop
docker compose down
```

### Traefik Dashboard

```bash
# Abrir dashboard
open http://localhost:8888/dashboard/

# Verificar routers (via API)
curl -s http://localhost:8888/api/http/routers | jq '.[] | {name: .name, rule: .rule, priority: .priority, status: .status}'

# Esperado:
# {
#   "name": "user-service@docker",
#   "rule": "PathPrefix(`/api/v1/auth`)",
#   "priority": 100,
#   "status": "enabled"
# }
# {
#   "name": "monolith@docker",
#   "rule": "PathPrefix(`/api/`)",
#   "priority": 50,
#   "status": "enabled"
# }
```

---

## Troubleshooting

### Service não sobe

```bash
# Verificar logs
docker compose logs user-service

# Verificar variáveis de ambiente
docker inspect scopeflow-user-service | grep -A20 Env

# Verificar se PostgreSQL está healthy
docker compose ps postgres
docker compose exec postgres pg_isready
```

### Traefik não roteia

```bash
# Verificar labels do container
docker inspect scopeflow-user-service | grep -A10 Labels

# Verificar se Traefik detectou o service
curl http://localhost:8888/api/http/services | jq '.[] | select(.name | contains("user"))'

# Restart Traefik
docker compose restart traefik
```

### JWT_SECRET mismatch

```bash
# Verificar secret no user-service
docker compose exec user-service env | grep JWT_SECRET

# Verificar secret no monólito
docker compose exec app env | grep JWT_SECRET

# DEVEM ser IDÊNTICOS
```

### Health check failing

```bash
# Verificar se aplicação está de pé
docker compose exec user-service curl -f http://localhost:8081/actuator/health/liveness

# Verificar logs de erro
docker compose logs user-service --tail=50 | grep -i error

# Aumentar start_period se aplicação demora para iniciar
# Editar docker-compose.yml: healthcheck.start_period: 60s
```

---

## Constraints de Produção

### Non-Root User

- UID 1001, GID 1001
- Sem acesso root no container
- Conforme CIS Docker Benchmark

### Graceful Shutdown

- SIGTERM → Spring Boot graceful shutdown
- Timeout: 30s (suporte a Spot instances)
- Configurado via `server.shutdown=graceful`

### Health Checks

- **Liveness:** `/actuator/health/liveness` (aplicação viva)
- **Readiness:** `/actuator/health/readiness` (pronta para tráfego)
- **Interval:** 30s, timeout: 3s, start_period: 40s

### Resource Limits (Kubernetes)

```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

---

## ADRs Relacionadas

- **ADR-002:** Shared Database Strategy (fase inicial do Strangler Fig)
- **ADR-003:** Traefik API Gateway (routing por prioridade de rota)
- **ADR-004:** JWT Shared Secret (transição sem downtime)

---

## Próximos Passos

1. ✅ Provisionar infraestrutura Docker Compose (CONCLUÍDO — Etapa 3)
2. ⏳ Extrair código de autenticação do monólito (Etapa 4 — `/migration-extract`)
3. ⏳ Implementar controllers REST (`/api/v1/auth/login`, `/api/v1/auth/refresh`)
4. ⏳ Migrar testes de integração
5. ⏳ Feature flag para rollback (LaunchDarkly ou Unleash)
6. ⏳ CDC setup (Debezium) para migração de database
7. ⏳ Split database (User DB dedicado)

---

## Referências

- [Strangler Fig Pattern — Martin Fowler](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Traefik Docker Provider Docs](https://doc.traefik.io/traefik/providers/docker/)
- [Spring Boot Graceful Shutdown](https://spring.io/blog/2020/03/27/spring-boot-2-3-0-available-now#graceful-shutdown)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
