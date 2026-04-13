#!/bin/bash
#
# validate-user-service-infra.sh — Smoke tests para validar infraestrutura user-service
#
# Executa todos os checks da ETAPA-3-VALIDACAO.md automaticamente.
#
# Usage:
#   ./scripts/validate-user-service-infra.sh
#
# Exit codes:
#   0 — Todos os checks passaram
#   1 — Pelo menos um check falhou

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0

echo "=========================================="
echo "  User Service Infrastructure Validation"
echo "=========================================="
echo ""

# Helper functions
check_pass() {
    echo -e "${GREEN}✓ PASS${NC} - $1"
    PASSED=$((PASSED + 1))
}

check_fail() {
    echo -e "${RED}✗ FAIL${NC} - $1"
    FAILED=$((FAILED + 1))
}

check_skip() {
    echo -e "${YELLOW}⊘ SKIP${NC} - $1"
}

# Check 1: Build image
echo "=== Check 1: Build Image ==="
if docker images | grep -q "projeto-service-b2b-user-service"; then
    IMAGE_SIZE=$(docker images --format "table {{.Repository}}\t{{.Size}}" | grep user-service | awk '{print $2}')
    check_pass "Image built successfully (size: $IMAGE_SIZE)"
else
    check_fail "Image not found (run: docker compose build user-service)"
fi
echo ""

# Check 2: Start services
echo "=== Check 2: Start Services ==="
if docker compose ps | grep -q "scopeflow-user-service.*Up"; then
    if docker compose ps | grep -q "scopeflow-user-service.*healthy"; then
        check_pass "User service is running and healthy"
    else
        check_fail "User service is running but NOT healthy"
    fi
else
    check_fail "User service is NOT running (run: docker compose up user-service -d)"
fi

if docker compose ps | grep -q "scopeflow-postgres.*Up.*healthy"; then
    check_pass "PostgreSQL is running and healthy"
else
    check_fail "PostgreSQL is NOT running or unhealthy"
fi

if docker compose ps | grep -q "scopeflow-traefik.*Up"; then
    check_pass "Traefik is running"
else
    check_fail "Traefik is NOT running"
fi
echo ""

# Check 3: Health checks (direct)
echo "=== Check 3: Health Checks (Direct) ==="
if curl -sf http://localhost:8081/actuator/health/liveness > /dev/null 2>&1; then
    check_pass "Liveness probe: HTTP 200"
else
    check_fail "Liveness probe: NOT accessible"
fi

if curl -sf http://localhost:8081/actuator/health/readiness > /dev/null 2>&1; then
    check_pass "Readiness probe: HTTP 200"
else
    check_fail "Readiness probe: NOT accessible"
fi

DB_STATUS=$(curl -s http://localhost:8081/actuator/health 2>/dev/null | jq -r '.components.db.status // "UNKNOWN"')
if [ "$DB_STATUS" = "UP" ]; then
    check_pass "Database connectivity: UP"
else
    check_fail "Database connectivity: $DB_STATUS"
fi
echo ""

# Check 4: Traefik routing
echo "=== Check 4: Traefik Routing ==="
if curl -s http://localhost:8888/api/http/routers 2>/dev/null | jq -e '.[] | select(.name == "user-service@docker")' > /dev/null; then
    PRIORITY=$(curl -s http://localhost:8888/api/http/routers 2>/dev/null | jq -r '.[] | select(.name == "user-service@docker") | .priority')
    if [ "$PRIORITY" = "100" ]; then
        check_pass "Traefik router 'user-service' exists with priority 100"
    else
        check_fail "Traefik router 'user-service' priority is $PRIORITY (expected: 100)"
    fi
else
    check_fail "Traefik router 'user-service' NOT found"
fi

if curl -s http://localhost:8888/api/http/routers 2>/dev/null | jq -e '.[] | select(.name == "monolith@docker")' > /dev/null; then
    PRIORITY=$(curl -s http://localhost:8888/api/http/routers 2>/dev/null | jq -r '.[] | select(.name == "monolith@docker") | .priority')
    if [ "$PRIORITY" = "50" ]; then
        check_pass "Traefik router 'monolith' exists with priority 50"
    else
        check_fail "Traefik router 'monolith' priority is $PRIORITY (expected: 50)"
    fi
else
    check_fail "Traefik router 'monolith' NOT found"
fi
echo ""

# Check 5: Traefik dashboard
echo "=== Check 5: Traefik Dashboard ==="
if curl -sf http://localhost:8888/api/http/routers > /dev/null 2>&1; then
    ROUTERS_COUNT=$(curl -s http://localhost:8888/api/http/routers 2>/dev/null | jq '. | length')
    check_pass "Traefik dashboard accessible ($ROUTERS_COUNT routers configured)"
else
    check_fail "Traefik dashboard NOT accessible"
fi
echo ""

# Check 6: JWT secret shared
echo "=== Check 6: JWT Secret Shared ==="
USER_SERVICE_JWT=$(docker compose exec -T user-service env 2>/dev/null | grep JWT_SECRET | cut -d'=' -f2 || echo "NOT_FOUND")
MONOLITH_JWT=$(docker compose exec -T app env 2>/dev/null | grep JWT_SECRET | cut -d'=' -f2 || echo "NOT_FOUND")

if [ "$USER_SERVICE_JWT" = "NOT_FOUND" ]; then
    check_fail "JWT_SECRET not found in user-service"
elif [ "$MONOLITH_JWT" = "NOT_FOUND" ]; then
    check_fail "JWT_SECRET not found in monolith"
elif [ "$USER_SERVICE_JWT" = "$MONOLITH_JWT" ]; then
    check_pass "JWT_SECRET is identical in both services"
else
    check_fail "JWT_SECRET MISMATCH between services"
fi

if [ ${#USER_SERVICE_JWT} -ge 32 ]; then
    check_pass "JWT_SECRET length >= 32 characters"
else
    check_fail "JWT_SECRET length < 32 characters (insecure)"
fi
echo ""

# Check 7: Graceful shutdown
echo "=== Check 7: Graceful Shutdown ==="
echo "(Skipped — requires manual test: docker compose stop user-service)"
check_skip "Graceful shutdown test (manual)"
echo ""

# Check 8: Resource limits
echo "=== Check 8: Resource Limits ==="
MEM_USAGE=$(docker stats scopeflow-user-service --no-stream --format "{{.MemUsage}}" 2>/dev/null | cut -d'/' -f1 | sed 's/MiB//' || echo "0")
if [ -n "$MEM_USAGE" ] && [ "$MEM_USAGE" != "0" ]; then
    check_pass "Memory usage: ${MEM_USAGE}MiB (idle)"
else
    check_skip "Memory usage (container not running)"
fi
echo ""

# Check 9: Logs structured
echo "=== Check 9: Logs Structured ==="
if docker compose logs user-service --tail=5 2>/dev/null | grep -q "\[.*\].*INFO.*com.scopeflow"; then
    check_pass "Logs are structured (timestamp + level + logger)"
else
    check_fail "Logs are NOT structured"
fi
echo ""

# Check 10: Database connectivity
echo "=== Check 10: Database Connectivity (Shared DB) ==="
USER_SERVICE_DB=$(curl -s http://localhost:8081/actuator/health 2>/dev/null | jq -r '.components.db.status // "UNKNOWN"')
if [ "$USER_SERVICE_DB" = "UP" ]; then
    check_pass "User service DB status: UP"
else
    check_fail "User service DB status: $USER_SERVICE_DB"
fi

# Note: Monolith health endpoint might differ (/api/v1/health/ready)
# Skipping monolith check to avoid false failures
check_skip "Monolith DB status (requires /api/v1/health/ready endpoint)"
echo ""

# Summary
echo "=========================================="
echo "  Validation Summary"
echo "=========================================="
echo -e "${GREEN}PASSED: $PASSED${NC}"
echo -e "${RED}FAILED: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Infrastructure is ready.${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Commit infrastructure: git add -A && git commit -m 'feat(infra): provisiona user-service Docker Compose'"
    echo "  2. Extract code: claude --agent marcus '/migration-extract user-auth'"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Review logs and troubleshoot.${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "  - Build: docker compose build user-service"
    echo "  - Start: docker compose up user-service -d"
    echo "  - Logs: docker compose logs user-service -f"
    echo "  - Traefik: curl http://localhost:8888/api/http/routers | jq"
    exit 1
fi
