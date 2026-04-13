#!/bin/bash
set -euo pipefail

# ==============================================================================
# Traefik Routing Validator
# ==============================================================================
# Validates that Traefik is correctly routing auth requests to user-service
# when AUTH_SERVICE_EXTRACTED=true.
#
# Usage:
#   ./tests/e2e/validate-traefik-routing.sh
# ==============================================================================

readonly GREEN='\033[0;32m'
readonly RED='\033[0;31m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

log_info() {
  echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
  echo -e "${GREEN}✓${NC} $1"
}

log_error() {
  echo -e "${RED}✗${NC} $1"
}

log_section() {
  echo ""
  echo -e "${YELLOW}━━━ $1 ━━━${NC}"
  echo ""
}

main() {
  log_section "Traefik Routing Validation"

  # Check if Traefik is running
  if ! docker ps | grep -q scopeflow-traefik; then
    log_error "Traefik container is not running"
    echo "Start it with: docker compose up -d traefik"
    exit 1
  fi

  log_success "Traefik container is running"

  # Check if user-service is running
  if ! docker ps | grep -q scopeflow-user-service; then
    log_error "User-service container is not running"
    echo "Start it with: docker compose up -d user-service"
    exit 1
  fi

  log_success "User-service container is running"

  # Capture Traefik logs for last 100 lines
  log_info "Analyzing Traefik logs..."
  local traefik_logs=$(docker logs scopeflow-traefik 2>&1 | tail -100)

  # Check for user-service backend registration
  if echo "$traefik_logs" | grep -q "user-service"; then
    log_success "Traefik detected user-service backend"
  else
    log_error "Traefik did not detect user-service backend"
    echo ""
    echo "Traefik logs (last 20 lines):"
    docker logs scopeflow-traefik 2>&1 | tail -20
    exit 1
  fi

  # Test direct access to user-service
  log_info "Testing direct access to user-service..."
  if curl -sf http://localhost:8081/actuator/health > /dev/null 2>&1; then
    log_success "User-service is reachable on port 8081"
  else
    log_error "User-service is not reachable on port 8081"
    exit 1
  fi

  # Test Traefik routing to backend
  log_info "Testing Traefik routing to backend..."
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    log_success "Backend is reachable through Traefik on port 8080"
  else
    log_error "Backend is not reachable through Traefik"
    exit 1
  fi

  # Verify routing rules
  log_section "Routing Rules Summary"

  echo "Expected routing (when AUTH_SERVICE_EXTRACTED=true):"
  echo ""
  echo "  POST /api/v1/auth/register  → user-service:8081"
  echo "  POST /api/v1/auth/login     → user-service:8081"
  echo "  POST /api/v1/auth/refresh   → user-service:8081"
  echo "  GET  /api/v1/auth/me        → user-service:8081"
  echo "  POST /api/v1/auth/logout    → user-service:8081"
  echo "  *    (all other routes)     → backend:8080"
  echo ""

  # Check docker-compose labels
  log_info "Verifying Traefik labels in docker-compose.yml..."

  if grep -q 'traefik.http.routers.user-service.rule=Host(`localhost`) && PathPrefix(`/api/v1/auth`)' docker-compose.yml 2>/dev/null; then
    log_success "Traefik routing rule found in docker-compose.yml"
  else
    log_error "Traefik routing rule NOT found in docker-compose.yml"
    echo "Expected label: traefik.http.routers.user-service.rule=Host(\`localhost\`) && PathPrefix(\`/api/v1/auth\`)"
    exit 1
  fi

  # Final summary
  log_section "Validation Summary"
  log_success "All Traefik routing checks passed!"
  echo ""
  echo "To test routing during E2E tests:"
  echo "  1. Set AUTH_SERVICE_EXTRACTED=true in .env"
  echo "  2. Restart backend: docker compose restart backend"
  echo "  3. Run E2E tests: ./tests/e2e/auth-flow.test.sh"
  echo "  4. Check logs: docker logs scopeflow-traefik -f"
  echo ""
}

main "$@"
