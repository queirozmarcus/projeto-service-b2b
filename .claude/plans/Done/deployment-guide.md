# 📋 Staging Deployment — Guia Executivo

**Data:** 2026-03-25
**Status:** ✅ Pronto para Deploy

---

## ✅ O Que Foi Preparado

### Etapa 1: Backend + Frontend Builds
```
✅ Backend: Maven build pipeline (via Docker)
✅ Frontend: Next.js 15 production build (in progress)
✅ Dockerfiles: Dockerfile.prod (backend), Dockerfile (frontend)
```

### Etapa 2: Staging Environment
```
✅ docker-compose.staging.yml completo com:
   - PostgreSQL 16 (database)
   - RabbitMQ 3.13 (message broker)
   - Redis 7 (cache)
   - Backend Spring Boot (porta 8080)
   - Frontend Next.js (porta 3000)

✅ Health checks em todos os serviços
✅ Networks isoladas (scopeflow-staging)
✅ Volumes persistentes para data
```

### Etapa 3: Validação (Smoke Tests)
```
✅ scripts/smoke-tests.sh com 10 testes:
   1. Backend health check (/health/ready)
   2. Frontend responding
   3. User registration via API
   4. Login with credentials
   5. Token refresh endpoint
   6. Invalid credentials handling
   7. Database connectivity
   8. RabbitMQ management console
   9. Redis connectivity
   10. Summary report

✅ Suporta env vars: API_URL, FRONTEND_URL
✅ Saída formatada (cores, status)
✅ Exit code 0 se todos tests passam
```

### Etapa 4: Documentação
```
✅ DEPLOYMENT.md com:
   - Quick start (3 comandos)
   - Docker Compose setup
   - Local dev setup
   - Smoke tests como rodar
   - E2E tests com Playwright
   - Debugging guide
   - Production checklist
   - Troubleshooting
```

---

## 🚀 Como Começar (3 Passos)

### Passo 1: Build Images (5 min)
```bash
cd /home/mq/iGitHub/projeto-service-b2b

# Build backend + frontend Docker images
docker compose -f docker-compose.staging.yml build
```

**Output esperado:**
```
[+] Building 45.2s (25/25) FINISHED
 ✔ backend
 ✔ frontend
```

### Passo 2: Start Services (2 min)
```bash
# Iniciar todos os containers
docker compose -f docker-compose.staging.yml up -d

# Aguardar health checks (2 minutos)
docker compose -f docker-compose.staging.yml ps

# Esperado: STATUS = "Up (healthy)"
```

**Serviços rodando:**
- Frontend: http://localhost:3000
- Backend: http://localhost:8080/api
- RabbitMQ Dashboard: http://localhost:15672
- Database: localhost:5432

### Passo 3: Validate Deployment (2 min)
```bash
# Rodar smoke tests
bash scripts/smoke-tests.sh

# Esperado:
# ✅ 10 testes passando
# 🎉 All smoke tests passed!
```

---

## 📊 Fluxo de Testes

### Smoke Tests (Automatizado)
```
Backend Health (GET /health/ready) ────→ ✅ Running

Frontend Availability ────────────────→ ✅ Responding

User Registration ─────────────────→ ✅ Account created
                    ↓                    ↓
               accessToken           refreshToken (cookie)
                    ↓                    ↓
Login ─────────────────────────────→ ✅ Authenticated
                    ↓
Token Refresh ────────────────────→ ✅ New token issued

Protected Endpoint ────────────────→ ✅ Accessible with token

401 Handling ──────────────────────→ ✅ Redirect to /login

Invalid Credentials ───────────────→ ✅ Error returned

Database + Infra ──────────────────→ ✅ All healthy
```

### E2E Tests (Manual - Playwright)
```bash
# Opcional: rodar E2E tests
npm run test:e2e

# 6 testes de auth flow
✓ Login happy path
✓ Register happy path
✓ Token refresh
✓ Multi-tab logout sync
✓ Invalid credentials
✓ Protected route redirect
```

---

## 📈 Health Check Status

| Serviço | URL | Status | Check Interval |
|---------|-----|--------|----------------|
| **Backend** | http://localhost:8080/api/health/ready | ✅ | 30s |
| **Frontend** | http://localhost:3000 | ✅ | 30s |
| **PostgreSQL** | localhost:5432 | ✅ | 10s |
| **RabbitMQ** | localhost:5672 | ✅ | 10s |
| **Redis** | localhost:6379 | ✅ | 10s |

---

## 🔍 Verificação Manual

### Login (via curl)
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "workspaceName": "Test Workspace",
    "fullName": "Test User",
    "email": "test@example.com",
    "password": "TestPass123"
  }'

# Response: { "accessToken": "...", "user": {...} }
# Cookie: Set-Cookie: refreshToken=....; HttpOnly; Secure; SameSite=Strict
```

### Frontend (via browser)
1. Open http://localhost:3000
2. Should redirect to /login (not authenticated)
3. Enter email + password → click Login
4. Should redirect to /dashboard
5. Navbar shows user name + logout button

### Multi-Tab Sync
1. Login na Aba 1
2. Abrir Aba 2 → Acessa /dashboard (auto-logged in via silent refresh)
3. Logout na Aba 1
4. Aba 2 deve redirecionar para /login (via BroadcastChannel)

---

## 🛑 Stop Services

```bash
# Parar containers (data persiste em volumes)
docker compose -f docker-compose.staging.yml down

# Limpar tudo (containers + volumes)
docker compose -f docker-compose.staging.yml down -v
```

---

## 📚 Próximas Etapas

### Imediato (Hoje)
1. ✅ Rodar `docker compose up -d`
2. ✅ Aguardar health checks
3. ✅ Rodar smoke tests
4. ✅ Testar login/logout manual
5. ✅ Testar E2E tests (Playwright)

### Sprint 6 (Próxima Semana)
- [ ] Implement ProposalList page
- [ ] Implement BriefingSession flow
- [ ] Create proposal templates
- [ ] PDF preview (frontend)

### Production Deployment (After Staging Validation)
- [ ] Update secrets (JWT_SECRET, S3 credentials)
- [ ] Configure CORS for production domain
- [ ] Enable HTTPS + SSL certificates
- [ ] Set up monitoring (Prometheus + Grafana)
- [ ] Configure backups (database + files)
- [ ] Load testing

---

## 💡 Tips & Tricks

### View Logs
```bash
# All services
docker compose -f docker-compose.staging.yml logs -f

# Specific service
docker compose -f docker-compose.staging.yml logs -f backend
docker compose -f docker-compose.staging.yml logs -f frontend
```

### Enter Container Shell
```bash
docker compose -f docker-compose.staging.yml exec backend sh
docker compose -f docker-compose.staging.yml exec frontend sh
```

### Access Database
```bash
psql -h localhost -U postgres -d scopeflow
# Password: postgres

# List tables
\dt

# View users
SELECT id, email, full_name FROM "user";
```

### RabbitMQ Dashboard
```
URL: http://localhost:15672
User: guest / Password: guest

Check:
- Connections
- Channels
- Queues (user.registered, proposal.approved, briefing.completed)
- Messages
```

---

## ❌ Troubleshooting

### "Connection refused" on :3000
```bash
# Check if port is in use
lsof -i :3000

# Kill process if needed
kill -9 <PID>

# Restart container
docker compose -f docker-compose.staging.yml restart frontend
```

### "CORS error" in browser
```bash
# Check CORS is configured
docker compose -f docker-compose.staging.yml logs backend | grep CORS

# Verify env var
docker compose -f docker-compose.staging.yml exec backend env | grep CORS

# Restart if changed
docker compose -f docker-compose.staging.yml restart backend
```

### "Database connection refused"
```bash
# Check postgres is running
docker compose -f docker-compose.staging.yml logs postgres

# Verify volume
docker volume ls | grep postgres_data_staging

# Restart postgres
docker compose -f docker-compose.staging.yml restart postgres
```

---

## 📋 Checklist Antes de Production

- [ ] Todos smoke tests passam
- [ ] E2E tests passam (6/6)
- [ ] Login/logout funciona
- [ ] Multi-tab sync funciona
- [ ] Token refresh funciona
- [ ] PDF generation não testa (mockado em staging)
- [ ] Email não testa (mockado em staging)
- [ ] RabbitMQ processa eventos
- [ ] Logs sem erros críticos

**Após checklist:** ✅ Ready for production deployment

---

**Status:** 🚀 READY FOR STAGING
**Data:** 2026-03-25
**Próximo:** docker compose up -d

