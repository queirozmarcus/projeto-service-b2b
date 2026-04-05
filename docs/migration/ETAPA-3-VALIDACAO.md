# Etapa 3 — Validação de Infraestrutura Docker Compose

**Objetivo:** Validar que a infraestrutura Docker Compose está provisionada corretamente antes da extração de código (Etapa 4).

---

## Checklist de Validação

### 1. Build de Imagens

```bash
cd /home/mq/iGitHub/projeto-service-b2b

# Build user-service (deve completar sem erros)
docker compose build user-service

# Verificar imagem criada
docker images | grep user-service

# Esperado:
# projeto-service-b2b-user-service   latest   <image-id>   <timestamp>   <size>
```

**Critérios de sucesso:**
- Build completa sem erros
- Imagem criada com tag `latest`
- Tamanho da imagem < 500MB (alpine JRE 21 + app)

---

### 2. Start de Serviços

```bash
# Start apenas user-service (depende de postgres)
docker compose up user-service -d

# Verificar status (deve estar healthy após ~40s)
docker compose ps

# Esperado:
# NAME                        STATUS                        PORTS
# scopeflow-user-service      Up (healthy)                  0.0.0.0:8081->8081/tcp
# scopeflow-postgres          Up (healthy)                  0.0.0.0:5432->5432/tcp
# scopeflow-traefik           Up                            0.0.0.0:80->80/tcp, 0.0.0.0:8888->8080/tcp
```

**Critérios de sucesso:**
- `scopeflow-user-service` status: `Up (healthy)`
- `scopeflow-postgres` status: `Up (healthy)`
- `scopeflow-traefik` status: `Up`
- Nenhum restart loop

---

### 3. Health Checks (Direto)

```bash
# Liveness probe (aplicação viva)
curl -f http://localhost:8081/actuator/health/liveness

# Esperado: HTTP 200
# {
#   "status": "UP"
# }

# Readiness probe (pronta para tráfego)
curl -f http://localhost:8081/actuator/health/readiness

# Esperado: HTTP 200
# {
#   "status": "UP"
# }

# Health geral
curl http://localhost:8081/actuator/health

# Esperado: HTTP 200
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP" },
#     "diskSpace": { "status": "UP" },
#     "livenessState": { "status": "UP" },
#     "ping": { "status": "UP" },
#     "readinessState": { "status": "UP" }
#   }
# }
```

**Critérios de sucesso:**
- Todos os endpoints retornam HTTP 200
- `status: "UP"` em todos os componentes
- `db.status: "UP"` (conexão PostgreSQL OK)

---

### 4. Traefik Routing (Strangler Fig)

```bash
# Verificar routers configurados
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

# Testar routing para user-service (priority 100)
curl -i http://localhost/api/v1/auth/health

# Esperado:
# HTTP/1.1 404 Not Found (ainda não implementado, mas roteado para user-service)
# Logs do user-service devem mostrar: "GET /api/v1/auth/health"

# Verificar logs do user-service
docker compose logs user-service --tail=20 | grep "GET /api/v1/auth/health"

# Testar routing para monólito (priority 50)
curl -i http://localhost/api/v1/workspaces

# Esperado:
# HTTP/1.1 401 Unauthorized (endpoint do monólito, requer JWT)
# Logs do monólito devem mostrar: "GET /api/v1/workspaces"
```

**Critérios de sucesso:**
- Traefik detecta 2 routers (`user-service`, `monolith`)
- `user-service` priority: 100 (maior que monólito)
- `monolith` priority: 50
- Requisições `/api/v1/auth/*` roteadas para user-service
- Requisições `/api/v1/*` (outros) roteadas para monólito

---

### 5. Traefik Dashboard

```bash
# Acessar dashboard
open http://localhost:8888/dashboard/

# OU via curl
curl -s http://localhost:8888/api/http/routers | jq
curl -s http://localhost:8888/api/http/services | jq
```

**Critérios de sucesso:**
- Dashboard acessível em `http://localhost:8888/dashboard/`
- Mostra 2 routers (`user-service@docker`, `monolith@docker`)
- Mostra 2 services (`user-service@docker`, `monolith@docker`)
- Ambos com status `enabled`

---

### 6. JWT Secret Shared

```bash
# Verificar secret no user-service
docker compose exec user-service env | grep JWT_SECRET

# Verificar secret no monólito
docker compose exec app env | grep JWT_SECRET

# DEVEM ser IDÊNTICOS (critical para JWT validation)
```

**Critérios de sucesso:**
- `JWT_SECRET` presente em ambos os containers
- Valores IDÊNTICOS entre user-service e monólito
- Mínimo 32 caracteres

---

### 7. Graceful Shutdown

```bash
# Start user-service
docker compose up user-service -d

# Enviar SIGTERM (simula stop/redeploy)
docker compose stop user-service

# Verificar logs de shutdown
docker compose logs user-service --tail=50 | grep -i "shutting down\|stopped"

# Esperado:
# "Shutting down gracefully"
# "Closing JPA EntityManagerFactory"
# "HikariPool closed"
# Shutdown completa em < 30s
```

**Critérios de sucesso:**
- Shutdown completa em < 30s (suporte a Spot instances)
- Logs mostram `Shutting down gracefully`
- Nenhum erro de `InterruptedException` ou `Connection refused`
- HikariCP fecha conexões antes de terminar

---

### 8. Resource Limits (Docker Compose)

```bash
# Verificar limites (se configurados)
docker stats scopeflow-user-service --no-stream

# Esperado:
# CONTAINER                   CPU %   MEM USAGE / LIMIT
# scopeflow-user-service      0.5%    256MiB / 1GiB
```

**Critérios de sucesso:**
- Memória em uso < 512MB (idle)
- CPU < 5% (idle)
- Nenhum OOM kill

---

### 9. Logs Estruturados

```bash
# Verificar formato de logs
docker compose logs user-service --tail=20

# Esperado (formato ISO 8601 + nível + logger + mensagem):
# 2026-04-05 14:30:00.123 [main] INFO  com.scopeflow.userservice.UserServiceApplication - Starting UserServiceApplication
# 2026-04-05 14:30:02.456 [main] INFO  o.s.b.w.embedded.tomcat.TomcatWebServer - Tomcat started on port 8081
```

**Critérios de sucesso:**
- Logs com timestamp ISO 8601
- Nível de log (INFO, DEBUG, WARN, ERROR) visível
- Logger completo (package + class)
- Sem stacktraces no startup (exceto se erro real)

---

### 10. Database Connectivity (Shared DB)

```bash
# Verificar conexão do user-service ao PostgreSQL
docker compose exec user-service curl -s http://localhost:8081/actuator/health | jq '.components.db'

# Esperado:
# {
#   "status": "UP",
#   "details": {
#     "database": "PostgreSQL",
#     "validationQuery": "isValid()"
#   }
# }

# Verificar conexão do monólito ao PostgreSQL (shared DB)
docker compose exec app wget -qO- http://localhost:8080/api/v1/health/ready | jq '.components.db'
```

**Critérios de sucesso:**
- `db.status: "UP"` em ambos os serviços
- Shared DB (mesma instância PostgreSQL)
- HikariCP pool size: 10 (configurado)

---

## Troubleshooting

### Problema: Build falha com "Cannot resolve dependencies"

**Diagnóstico:**
```bash
docker compose logs user-service | grep -i "dependency\|maven"
```

**Solução:**
- Verificar `pom.xml` (repositórios Maven Central acessíveis)
- Verificar conectividade: `docker compose exec user-service ping -c 3 repo.maven.apache.org`
- Rodar `./mvnw dependency:purge-local-repository` (se cache corrompido)

---

### Problema: Health check failing

**Diagnóstico:**
```bash
docker compose exec user-service curl -v http://localhost:8081/actuator/health/liveness
docker compose logs user-service --tail=100 | grep -i error
```

**Solução:**
- Aumentar `start_period: 60s` em `docker-compose.yml` (se app demora para iniciar)
- Verificar porta correta (8081, não 8080)
- Verificar rota: `/actuator/health/liveness` (não `/health`)

---

### Problema: Traefik não roteia

**Diagnóstico:**
```bash
docker inspect scopeflow-user-service | grep -A10 Labels
curl http://localhost:8888/api/http/routers | jq
```

**Solução:**
- Verificar labels do container (devem incluir `traefik.enable=true`)
- Restart Traefik: `docker compose restart traefik`
- Verificar network: ambos devem estar em `scopeflow-network`

---

### Problema: JWT_SECRET mismatch

**Diagnóstico:**
```bash
docker compose config | grep JWT_SECRET
```

**Solução:**
- Verificar `.env` (deve ter `JWT_SECRET` definido)
- Verificar interpolação: `${JWT_SECRET:-default}` em `docker-compose.yml`
- Restart ambos os serviços: `docker compose restart app user-service`

---

## Resumo de Validação

| Check | Critério de Sucesso | Status |
|-------|---------------------|--------|
| 1. Build imagem | Completa sem erros, tamanho < 500MB | ⏳ |
| 2. Start serviços | `Up (healthy)` após 40s | ⏳ |
| 3. Health checks | HTTP 200, `status: "UP"` | ⏳ |
| 4. Traefik routing | user-service priority 100, monólito 50 | ⏳ |
| 5. Traefik dashboard | 2 routers, 2 services, ambos enabled | ⏳ |
| 6. JWT secret | Idêntico em ambos os serviços | ⏳ |
| 7. Graceful shutdown | < 30s, sem erros | ⏳ |
| 8. Resource limits | < 512MB idle, < 5% CPU | ⏳ |
| 9. Logs estruturados | ISO 8601 + nível + logger | ⏳ |
| 10. DB connectivity | `db.status: "UP"` em ambos | ⏳ |

**Próxima etapa:** Preencher status (✅/❌) após executar cada check.

---

## Próximos Passos

Após validação completa (todos os checks ✅):

1. Commitar infraestrutura:
   ```bash
   git add user-service/ docker-compose.yml .env.example README.md docs/migration/
   git commit -m "feat(infra): provisiona user-service Docker Compose (Strangler Fig)"
   ```

2. Avançar para Etapa 4:
   ```bash
   claude --agent marcus "/migration-extract user-auth"
   ```

3. Implementar controllers REST:
   - `POST /api/v1/auth/login`
   - `POST /api/v1/auth/refresh`
   - `POST /api/v1/auth/register`
   - `POST /api/v1/auth/logout`

4. Migrar testes de integração do monólito

5. Feature flag para rollback (LaunchDarkly/Unleash)

---

**Última atualização:** 2026-04-05
**Responsável:** Marcus Workflow (Etapa 3 — Provisionar Infra)
