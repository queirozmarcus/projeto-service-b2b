# E2E Smoke Tests - Validation Checklist

Use this checklist before deploying the Strangler Fig migration to production.

---

## Pre-Deployment Validation

### 1. Local Environment Tests

#### Monolith Mode (Baseline)
```bash
□ Build backend JAR successfully
□ Start services (postgres, redis, rabbitmq, backend)
□ Wait for backend health check (< 120s)
□ Run: AUTH_SERVICE_EXTRACTED=false ./tests/e2e/auth-flow.test.sh
□ All 7 tests pass
□ No errors in backend logs
□ Test execution < 30s
□ Cleanup completes without errors
```

**Expected output:**
```
✓ Test 1: Register user
✓ Test 2: Login
✓ Test 3: Get /me
✓ Test 4: Access workspace
✓ Test 5: Invalid credentials
✓ Test 6: Expired JWT
✓ Test 7: Invalid JWT format

📊 Test Summary: 7 passed, 0 failed
```

---

#### User-Service Mode (Migration)
```bash
□ Build backend + user-service JARs successfully
□ Create .env with AUTH_SERVICE_EXTRACTED=true
□ Start all services (postgres, redis, rabbitmq, user-service, backend, traefik)
□ Wait for user-service health check (< 120s)
□ Wait for backend health check (< 120s)
□ Run: AUTH_SERVICE_EXTRACTED=true ./tests/e2e/auth-flow.test.sh
□ All 7 tests pass
□ No errors in user-service logs
□ No errors in backend logs
□ Traefik routing logs show requests to user-service
□ Test execution < 30s
□ Cleanup completes without errors
```

**Critical validation:**
```bash
□ Test 4 passes (JWT from user-service accepted by monolith)
□ Verify JWT claims: sub, exp, email present
□ Verify JWT expiration > current time
□ No "Invalid JWT signature" errors in monolith logs
```

---

### 2. Traefik Routing Validation

```bash
□ Run: ./tests/e2e/validate-traefik-routing.sh
□ Traefik container is running
□ User-service container is running
□ User-service backend registered in Traefik
□ Direct access to user-service:8081 works
□ Backend reachable through Traefik:8080
□ Routing rule exists in docker-compose.yml
```

**Expected routing:**
```
POST /api/v1/auth/register  → user-service:8081 ✓
POST /api/v1/auth/login     → user-service:8081 ✓
POST /api/v1/auth/refresh   → user-service:8081 ✓
GET  /api/v1/auth/me        → user-service:8081 ✓
POST /api/v1/auth/logout    → user-service:8081 ✓
*    (all other routes)     → backend:8080 ✓
```

---

### 3. CI/CD Pipeline Validation

```bash
□ Push code to feature branch
□ GitHub Actions workflow triggers
□ Job 1 (monolith-mode) completes successfully
□ Job 2 (user-service-mode) completes successfully
□ Job 3 (summary) shows all tests passed
□ No flaky tests (run 3 times, all pass)
□ CI execution time < 5 minutes
```

**GitHub Actions status:**
```
✅ e2e-monolith-mode (2m 30s)
✅ e2e-user-service-mode (3m 15s)
✅ summary (5s)
```

---

### 4. JWT Compatibility

```bash
□ JWT_SECRET is identical in backend and user-service
□ JWT claims match between services (sub, exp, email)
□ JWT expiration time matches (15 minutes)
□ Refresh token expiration matches (7 days)
□ JWT signature algorithm is HS256 (not RS256)
□ No JWT validation errors in monolith logs
```

**Verify JWT contents:**
```bash
# Extract JWT from user-service login
export JWT_TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# Decode payload
echo $JWT_TOKEN | cut -d'.' -f2 | base64 -d | jq .

# Verify claims:
□ "sub" exists (user ID)
□ "exp" exists (expiration timestamp)
□ "email" exists (user email)
□ "iat" exists (issued at timestamp)
□ exp > current time (not expired)
```

---

### 5. Error Handling

```bash
□ Test 5 returns 401 for invalid credentials
□ Test 5 response follows RFC 9457 format
□ Test 6 returns 401 for expired JWT
□ Test 7 returns 401 for malformed JWT
□ All error responses include:
  - type (URL to error docs)
  - title (human-readable summary)
  - status (HTTP status code)
  - detail (specific error message)
  - errorCode (stable code like AUTH-001)
  - errorId (unique trace ID)
  - timestamp (ISO 8601)
```

**Example error response:**
```json
{
  "type": "https://scopeflow.ai/errors/authentication",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid email or password",
  "errorCode": "AUTH-001",
  "errorId": "tr-abc123def456",
  "timestamp": "2024-04-05T14:30:00.123Z"
}
```

---

### 6. Performance Benchmarks

```bash
□ Monolith mode tests complete in < 90s
□ User-service mode tests complete in < 120s
□ No timeout errors (health checks < 120s)
□ Backend startup time < 60s
□ User-service startup time < 60s
□ PostgreSQL startup time < 30s
□ RabbitMQ startup time < 30s
```

---

### 7. Logs & Observability

```bash
□ Backend logs show successful auth requests
□ User-service logs show successful auth requests
□ Traefik logs show routing to user-service
□ No ERROR level logs in any service
□ No WARN level logs (except expected)
□ Trace IDs present in all log entries
□ Logs include userId for authenticated requests
```

**Verify log format:**
```
2024-04-05 14:30:00.123 INFO  [nio-8081-exec-3] c.s.u.a.i.w.a.AuthController : User logged in: userId=f47ac10b, traceId=abc123
```

---

### 8. Database State

```bash
□ Test users are created during tests
□ Test users are deleted after tests
□ No orphaned test data in database
□ Flyway migrations are up to date
□ No pending migrations
□ Database schema matches expected state
```

**Verify database:**
```bash
# Connect to database
docker exec -it scopeflow-postgres psql -U scopeflow -d scopeflow

# Check for orphaned test users
SELECT * FROM users WHERE email LIKE 'smoke-test-%';
-- Expected: 0 rows

# Check migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
-- Expected: All migrations applied
```

---

### 9. Rollback Plan

```bash
□ Rollback procedure documented
□ Rollback tested in staging
□ Rollback time < 5 minutes
□ Rollback impact: zero downtime
□ Rollback requires:
  - Set AUTH_SERVICE_EXTRACTED=false
  - Restart backend: docker compose restart backend
  - Verify: ./tests/e2e/auth-flow.test.sh
```

**Rollback steps:**
```bash
# 1. Set feature flag to false
echo "AUTH_SERVICE_EXTRACTED=false" >> .env

# 2. Restart backend
docker compose restart backend

# 3. Verify rollback
./tests/e2e/auth-flow.test.sh

# Expected: All tests pass in monolith mode
```

---

### 10. Production Readiness

```bash
□ All E2E tests pass in staging environment
□ Load tests pass (100 req/s sustained for 5 minutes)
□ JWT compatibility validated across all environments
□ Monitoring alerts configured (Prometheus/Grafana)
□ On-call rotation assigned
□ Rollback plan communicated to team
□ Deployment window scheduled (off-peak hours)
□ Post-deployment verification plan ready
```

---

## Post-Deployment Validation

### Immediately After Deploy (0-5 minutes)

```bash
□ Run E2E tests on production (with caution!)
□ Check error rate (< 1%)
□ Check latency (p95 < 200ms)
□ Verify JWT validation success rate (> 99.5%)
□ Monitor logs for errors
□ Verify Traefik routing logs show user-service requests
```

---

### Short-term Monitoring (5-60 minutes)

```bash
□ Monitor error rate every 5 minutes
□ Monitor latency every 5 minutes
□ Check JWT validation failure rate
□ Review logs for unexpected errors
□ Verify user registration/login flows work
□ Check database connection pool usage
□ Verify RabbitMQ message processing
```

---

### Long-term Monitoring (1 hour - 24 hours)

```bash
□ Error rate remains < 0.5%
□ Latency p95 remains < 150ms
□ JWT validation failure rate < 0.1%
□ No memory leaks (heap usage stable)
□ No connection pool exhaustion
□ No Traefik routing errors
□ User-service uptime > 99.9%
```

---

## Go/No-Go Criteria

### Go Criteria (Safe to Deploy)

```
✅ All 7 E2E tests pass in monolith mode
✅ All 7 E2E tests pass in user-service mode
✅ Test 4 (JWT compatibility) passes consistently
✅ CI/CD pipeline passes (no flaky tests)
✅ Traefik routing validation passes
✅ JWT secrets synchronized
✅ Load tests pass (100 req/s)
✅ Rollback plan validated
✅ Monitoring alerts configured
✅ On-call engineer assigned
```

---

### No-Go Criteria (Block Deploy)

```
❌ Any E2E test fails
❌ Test 4 (JWT compatibility) fails or flaky
❌ JWT secrets mismatch
❌ Traefik routing issues
❌ CI/CD pipeline fails
❌ Load tests fail (error rate > 1% or latency > 500ms)
❌ Rollback plan not validated
❌ Monitoring not configured
```

---

## Sign-off

```
Validated by:        _________________  Date: ________
Tech Lead:           _________________  Date: ________
Product Owner:       _________________  Date: ________

Deployment approved: [ ] Yes  [ ] No

Notes:
_________________________________________________________
_________________________________________________________
_________________________________________________________
```

---

## Emergency Contacts

| Role | Name | Contact |
|------|------|---------|
| Tech Lead | TBD | Slack: @tech-lead |
| On-call Engineer | TBD | Phone: +1-XXX-XXX-XXXX |
| DevOps Lead | TBD | Slack: @devops-lead |
| Product Owner | TBD | Email: product@scopeflow.ai |

---

## Appendix: Common Issues

### Issue 1: Test 4 fails with "Invalid JWT signature"

**Cause:** JWT_SECRET mismatch

**Fix:**
```bash
# Sync secrets
grep JWT_SECRET .env
grep JWT_SECRET user-service/src/main/resources/application.yml

# If different:
# 1. Update user-service/application.yml
# 2. Rebuild: docker compose build --no-cache user-service
# 3. Restart: docker compose restart user-service backend
# 4. Retest: ./tests/e2e/auth-flow.test.sh
```

---

### Issue 2: Tests timeout waiting for health

**Cause:** Service startup time > 120s

**Fix:**
```bash
# Check logs
docker logs scopeflow-backend -f
docker logs scopeflow-user-service -f

# Common causes:
# - Database migration stuck
# - Connection pool exhaustion
# - Insufficient memory (increase to 4GB+)
```

---

### Issue 3: Traefik not routing to user-service

**Cause:** Missing or incorrect Traefik labels

**Fix:**
```bash
# Verify labels in docker-compose.yml
grep -A 5 "traefik.http.routers.user-service" docker-compose.yml

# Expected:
# traefik.http.routers.user-service.rule=Host(`localhost`) && PathPrefix(`/api/v1/auth`)

# Restart Traefik
docker compose restart traefik
```

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2024-04-05 | Initial checklist | AI Assistant |
