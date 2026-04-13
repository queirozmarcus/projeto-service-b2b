# 🚀 Staging Deployment Guide

## Quick Start

### Prerequisites
- Docker & Docker Compose v2+
- Node.js 20+ (for local frontend dev)
- Maven 3.8+ (for local backend dev)
- PostgreSQL CLI (optional, for debugging)

### Environment Setup

```bash
cd /home/mq/iGitHub/projeto-service-b2b

# Set environment variables (optional)
export OPENAI_API_KEY="your-key-here"  # For AI features
export CORS_ORIGIN_LOCAL="http://localhost:3000"
```

---

## Option 1: Docker Compose (Recommended)

### Start Staging Environment

```bash
# Build and start all services
docker compose -f docker-compose.staging.yml up -d

# Wait for services to be healthy (2-3 minutes)
docker compose -f docker-compose.staging.yml ps

# Expected output:
# STATUS: Up (healthy)
```

### Access Services

- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080/api
- **RabbitMQ Dashboard:** http://localhost:15672 (guest/guest)
- **Database:** postgres://localhost:5432/scopeflow

### Verify Deployment

```bash
# Run smoke tests
bash scripts/smoke-tests.sh

# Expected output:
# ✅ Backend Health Check
# ✅ Frontend is responding
# ✅ User Registration
# ✅ Login
# ✅ Token Refresh Endpoint
# ✅ All smoke tests passed!
```

### Stop Services

```bash
docker compose -f docker-compose.staging.yml down

# To also delete volumes:
docker compose -f docker-compose.staging.yml down -v
```

---

## Option 2: Local Development (without Docker)

### Prerequisites
- PostgreSQL running on localhost:5432
- RabbitMQ running on localhost:5672
- Redis running on localhost:6379

### Backend

```bash
cd backend

# Run migrations
mvn flyway:migrate

# Start Spring Boot app
mvn spring-boot:run

# Or build and run JAR:
mvn clean package
java -jar target/scopeflow-*.jar
```

Backend runs on: http://localhost:8080

### Frontend

```bash
cd frontend

# Install dependencies
npm install --legacy-peer-deps

# Start dev server
npm run dev

# Or build and start production:
npm run build
npm start
```

Frontend runs on: http://localhost:3000

---

## Smoke Tests

### What They Test

1. **Backend Health** — `/api/health/ready` responds 200
2. **Frontend Availability** — Frontend loads
3. **User Registration** — Can create account via API
4. **Login** — Can login with credentials
5. **Token Refresh** — Refresh endpoint responds
6. **Invalid Credentials** — Proper error for wrong password
7. **Database Health** — PostgreSQL is running
8. **RabbitMQ** — Message broker is responsive
9. **Redis** — Cache is running

### Run Tests

```bash
# Make script executable
chmod +x scripts/smoke-tests.sh

# Run tests
bash scripts/smoke-tests.sh

# With custom API URL
API_URL="http://staging.example.com/api" bash scripts/smoke-tests.sh
```

### Expected Results

```
✅ PASS: Backend Health Check
✅ PASS: Frontend is responding
✅ PASS: User Registration
✅ PASS: Login
✅ PASS: Token Refresh Endpoint
✅ PASS: Invalid Credentials Rejected
✅ PASS: Database Health
✅ PASS: RabbitMQ Management Console
✅ PASS: Redis is running

📊 Smoke Test Summary
✅ Passed: 9
❌ Failed: 0
🎉 All smoke tests passed!
```

---

## Testing the Application

### Manual Testing Flow

#### 1. Register New Account
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "workspaceName": "My Test Workspace",
    "fullName": "Test User",
    "email": "test@example.com",
    "password": "TestPass123"
  }'

# Response should include: accessToken, user info
```

#### 2. Access Dashboard
- Navigate to http://localhost:3000
- Should redirect to /login if not authenticated
- Login with credentials registered above
- Should redirect to /dashboard
- Navbar shows user name and logout button

#### 3. Test Token Refresh
- Login
- Open browser DevTools → Application → Cookies
- Verify `refreshToken` cookie exists with:
  - `HttpOnly` flag set
  - `Secure` flag (in production)
  - `SameSite=Strict`

#### 4. Test Logout
- Click "Logout" button in Navbar
- Should redirect to /login
- Session cleared from Zustand store
- Multi-tab: logout in one tab, check other tabs also redirect

---

## E2E Tests

### Prerequisites

```bash
cd frontend

# Install Playwright browsers (one-time)
npx playwright install
```

### Run E2E Tests

```bash
# Run all tests
npm run test:e2e

# Run in UI mode (interactive)
npm run test:e2e:ui

# Run in headed mode (see browser)
npm run test:e2e:headed

# Run specific test file
npm run test:e2e -- e2e/auth.spec.ts

# Run specific test
npm run test:e2e -- -g "should login successfully"
```

### Expected Output

```
Running 6 tests using 1 worker

✓ [chromium] › auth.spec.ts › should login successfully with valid credentials (5s)
✓ [chromium] › auth.spec.ts › should register new account and auto-login (6s)
✓ [chromium] › auth.spec.ts › should refresh token automatically (3s)
✓ [chromium] › auth.spec.ts › should sync logout across tabs (4s)
✓ [chromium] › auth.spec.ts › should show error for invalid credentials (2s)
✓ [chromium] › auth.spec.ts › should redirect to login when accessing protected route (2s)

6 passed (22.5s)
```

---

## Debugging

### Backend Logs

```bash
# View logs from running container
docker compose -f docker-compose.staging.yml logs backend -f

# View logs from stopped container
docker logs scopeflow-api-staging
```

### Frontend Logs

```bash
# View browser console
# Open http://localhost:3000 and press F12

# View Next.js server logs
docker compose -f docker-compose.staging.yml logs frontend -f
```

### Database Debugging

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d scopeflow

# List tables
\dt

# View users
SELECT id, email, full_name FROM "user" LIMIT 10;

# View workspaces
SELECT id, name FROM workspace LIMIT 10;
```

### RabbitMQ Dashboard

```
URL: http://localhost:15672
Username: guest
Password: guest

Check:
- Queues (user.registered, proposal.approved, briefing.completed)
- Messages (should be empty if processing works)
- Dead Letter Queues (if messages failed)
```

---

## Troubleshooting

### "Connection refused" on localhost:3000
- Check frontend container is running: `docker ps | grep frontend`
- Check logs: `docker logs scopeflow-frontend-staging`
- Ensure port 3000 is not in use: `lsof -i :3000`

### "Invalid JWT" or "Token expired"
- Clear browser cookies: DevTools → Application → Cookies → Delete all
- Logout and login again
- Check JWT_SECRET matches between frontend expectations and backend

### "CORS error" in browser console
- Backend CORS config issue
- Check that backend env var `CORS_ORIGIN_LOCAL=http://localhost:3000`
- Restart backend container if changed

### RabbitMQ messages piling up (not processed)
- Check event listeners are running
- Check Zustand store has correct accessToken
- Check logs for listener errors
- May need to manually clear DLQ if listener implementation has bugs

### PostgreSQL connection errors
- Check postgres container is healthy: `docker ps`
- Check volume is mounted: `docker inspect scopeflow-postgres-staging`
- Try accessing directly: `psql -h localhost -U postgres`

---

## Production Deployment Notes

Before deploying to production:

1. **Update Secrets:**
   - Change `JWT_SECRET` to a long random value (min 32 chars)
   - Set `SPRING_PROFILES_ACTIVE=prod`
   - Configure real AWS S3 credentials if needed
   - Set `AWS_S3_ENABLED=true` if using S3 for PDFs

2. **Update CORS:**
   - Change `CORS_ORIGIN_LOCAL` to actual domain
   - Remove localhost origins

3. **HTTPS:**
   - Set `Secure` flag on cookies (automatic in prod with HTTPS)
   - Configure TLS/SSL certificates

4. **Database:**
   - Use managed PostgreSQL (RDS, CloudSQL) instead of container
   - Enable backups and replication
   - Use strong password for postgres user

5. **Monitoring:**
   - Set up Prometheus scraping for metrics
   - Configure Grafana dashboards
   - Set up alerting for critical events
   - Enable Sentry or similar for error tracking

6. **Load Testing:**
   - Run load tests before production
   - Ensure Kubernetes HPA is configured for auto-scaling

---

## Useful Commands

```bash
# View all running containers
docker compose -f docker-compose.staging.yml ps

# Restart a specific service
docker compose -f docker-compose.staging.yml restart backend

# View all logs
docker compose -f docker-compose.staging.yml logs -f

# Clean up (remove containers and volumes)
docker compose -f docker-compose.staging.yml down -v

# Rebuild images
docker compose -f docker-compose.staging.yml build --no-cache

# Enter container shell
docker compose -f docker-compose.staging.yml exec backend /bin/sh
docker compose -f docker-compose.staging.yml exec frontend /bin/sh
```

---

**Last Updated:** 2026-03-25
**Status:** Ready for Staging
