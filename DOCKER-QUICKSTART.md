# 🚀 Docker Compose Quick Start

**TL;DR:** Um comando para rodar tudo (backend + frontend + database + cache + queue)

---

## ⚡ Super Rápido (1 minuto)

```bash
# Clone + start
git clone https://github.com/queirozmarcus/projeto-service-b2b.git
cd projeto-service-b2b

# Start everything
docker compose up -d

# Wait 40 seconds for services to stabilize...

# Check if everything is running
docker compose ps

# Access the app
open http://localhost:3000              # Frontend
open http://localhost:8080/swagger-ui   # Backend API Docs
```

**That's it!** ✅

---

## 📋 What Gets Started

| Service | Port | URL | Credentials |
|---------|------|-----|-------------|
| **Frontend** | 3000 | http://localhost:3000 | — |
| **Backend API** | 8080 | http://localhost:8080/api/v1 | JWT (dev) |
| **OpenAPI Docs** | 8080 | http://localhost:8080/swagger-ui.html | — |
| **PostgreSQL** | 5432 | localhost:5432 | postgres/postgres |
| **RabbitMQ UI** | 15672 | http://localhost:15672 | guest/guest |
| **Redis** | 6379 | localhost:6379 | — |

---

## 🔍 Verify Everything Works

```bash
# Run automated tests
./docker-test.sh

# Or manually check each service
docker compose ps                        # Show all containers
docker compose logs -f app              # Stream backend logs
docker compose logs -f frontend         # Stream frontend logs
docker compose logs -f                  # All logs
```

---

## 🛠️ Common Commands

```bash
# Stop everything (but keep data)
docker compose down

# Stop + remove all data (fresh restart)
docker compose down -v

# Rebuild images (after code changes)
docker compose up -d --build

# View specific service logs
docker compose logs -f app              # Backend
docker compose logs -f frontend         # Frontend
docker compose logs postgres            # PostgreSQL

# Connect to database
docker compose exec postgres psql -U postgres -d scopeflow

# Check RabbitMQ queues
docker compose exec rabbitmq rabbitmqctl list_queues

# View Redis data
docker compose exec redis redis-cli
```

---

## 🚀 Full Restart (if stuck)

```bash
docker compose down -v && docker compose up -d --build && sleep 40 && ./docker-test.sh
```

---

## 📝 Environment Variables

All env vars for local development are **hardcoded** in `docker-compose.yml`:
- Database: `postgres:postgres` @ `postgresql://postgres:5432/scopeflow`
- JWT Secret: `dev-secret-key-change-in-production-min-32-chars` (dev only!)
- RabbitMQ: `guest:guest` @ `rabbitmq:5672`
- Redis: `redis:6379`

For production, use `.env` file with secrets.

---

## 🔌 Test the API

```bash
# Get a JWT token (from auth endpoint, or use dev token)
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Create a briefing
curl -X POST http://localhost:8080/api/v1/briefings \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"550e8400-e29b-41d4-a716-446655440000","serviceType":"SOCIAL_MEDIA"}'

# Or just browse the Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 🐛 Troubleshooting

### Port already in use?
```bash
# Find which process is using the port
lsof -i :8080    # Backend
lsof -i :3000    # Frontend
lsof -i :5432    # PostgreSQL

# Either kill the process or change port in docker-compose.yml
```

### Services won't start?
```bash
# Check if Docker daemon is running
docker ps

# Clean up and restart
docker compose down -v
docker compose up -d --build
sleep 40
docker compose ps
```

### Backend health check failing?
```bash
# Wait longer and check logs
docker compose logs app | tail -50

# The app needs ~40s to start with migrations
```

### Frontend can't connect to backend?
```bash
# Verify CORS is working
curl -X OPTIONS http://localhost:8080/api/v1/health \
  -H "Origin: http://localhost:3000"

# Check if backend is actually running
curl http://localhost:8080/actuator/health
```

### Out of memory?
```bash
# Allocate more RAM to Docker Desktop
# Preferences → Resources → Memory: 4GB+
```

---

## 🎁 What's Included

✅ Spring Boot 3.2 + Java 21 backend (114 Java files)
✅ Next.js 15 + React 19 frontend
✅ PostgreSQL 16 with Flyway migrations (V1-V3)
✅ RabbitMQ for async tasks
✅ Redis for caching
✅ OpenAPI 3.1 documentation
✅ 52 integration tests (Testcontainers)
✅ Multi-stage Docker builds (optimized images)
✅ Kubernetes-ready Helm charts
✅ CI/CD pipelines (GitHub Actions)

---

## 📚 Documentation

- **README.md** — Full project overview
- **TELESTORE.md** — Executive summary (what's been built)
- **BOOTSTRAP_SUMMARY.md** — Technical bootstrap details
- **docs/api/BRIEFING-API-GUIDE.md** — API endpoint details
- **docs/api/briefing-api.yaml** — OpenAPI specification

---

## ✨ Next Steps

1. **Frontend:** Open http://localhost:3000
2. **Test API:** Go to http://localhost:8080/swagger-ui.html
3. **RabbitMQ:** Check http://localhost:15672 (guest/guest)
4. **Database:** Connect via `localhost:5432` (postgres/postgres, db: scopeflow)
5. **Logs:** `docker compose logs -f` to watch everything

---

## 🆘 Need Help?

```bash
# Show this quick start
cat DOCKER-QUICKSTART.md

# Run health checks
./docker-test.sh

# Check full documentation
cat TELESTORE.md
```

---

**Happy coding! 🚀**
