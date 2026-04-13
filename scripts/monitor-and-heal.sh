#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Monitor and Auto-Healing Script for ScopeFlow AI
# ==============================================================================
# Monitors critical containers and runs smoke tests with auto-retry logic.
# On failure: captures last 100 lines of logs from each container and creates
# a consolidated incident report for post-mortem analysis.
#
# Usage:
#   ./scripts/monitor-and-heal.sh
#
# Exit codes:
#   0 - All tests passed
#   1 - Tests failed after retry OR safety checks failed
# ==============================================================================

# ==============================================================================
# Configuration
# ==============================================================================

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
readonly LOGS_DIR="$PROJECT_ROOT/logs"
readonly TEST_SCRIPT="$PROJECT_ROOT/tests/e2e/auth-flow.test.sh"
readonly LOCKFILE="$PROJECT_ROOT/.monitor-and-heal.lock"

readonly CONTAINERS=(
  "user-service"
  "monolith"
  "postgresql"
)

readonly RETRY_DELAY=5
readonly TEST_TIMEOUT=30

# Colors
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[0;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Global state
LOG_CAPTURE_PID=""
INCIDENT_TIMESTAMP=""

# ==============================================================================
# Helper Functions
# ==============================================================================

log_info() {
  echo -e "${BLUE}🔍${NC} $1"
}

log_success() {
  echo -e "${GREEN}✅${NC} $1"
}

log_warning() {
  echo -e "${YELLOW}⚠️ ${NC} $1"
}

log_error() {
  echo -e "${RED}❌${NC} $1"
}

# Cleanup function (called on EXIT)
cleanup() {
  if [ -n "$LOG_CAPTURE_PID" ] && kill -0 "$LOG_CAPTURE_PID" 2>/dev/null; then
    log_info "Stopping log capture process (PID: $LOG_CAPTURE_PID)..."
    kill "$LOG_CAPTURE_PID" 2>/dev/null || true
    wait "$LOG_CAPTURE_PID" 2>/dev/null || true
  fi

  # Remove lockfile
  [ -f "$LOCKFILE" ] && rm -f "$LOCKFILE"
}

trap cleanup EXIT

# ==============================================================================
# Safety Checks
# ==============================================================================

check_lockfile() {
  if [ -f "$LOCKFILE" ]; then
    local lock_pid
    lock_pid=$(cat "$LOCKFILE" 2>/dev/null || echo "")

    if [ -n "$lock_pid" ] && kill -0 "$lock_pid" 2>/dev/null; then
      log_error "Another instance is already running (PID: $lock_pid)"
      log_info "If this is incorrect, remove: $LOCKFILE"
      exit 1
    else
      # Stale lockfile
      rm -f "$LOCKFILE"
    fi
  fi

  # Create lockfile with current PID
  echo $$ > "$LOCKFILE"
}

check_docker_running() {
  log_info "Checking Docker Compose status..."

  if ! docker compose ps &>/dev/null; then
    log_error "Docker Compose is not available."
    log_info "Make sure Docker is running."
    exit 1
  fi

  local running_containers
  running_containers=$(docker compose ps --filter "status=running" --format "{{.Service}}" 2>/dev/null | wc -l)

  if [ "$running_containers" -lt 3 ]; then
    log_error "Not all required containers are running."
    log_info "Expected: user-service, monolith (app), postgresql"
    log_info "Current running containers:"
    docker compose ps
    log_info ""
    log_info "Start all containers with: docker compose up -d"
    exit 1
  fi

  log_success "Docker Compose is running (${running_containers} containers up)"
}

check_test_script() {
  if [ ! -f "$TEST_SCRIPT" ]; then
    log_error "Test script not found: $TEST_SCRIPT"
    exit 1
  fi

  if [ ! -x "$TEST_SCRIPT" ]; then
    log_warning "Test script is not executable, fixing permissions..."
    chmod +x "$TEST_SCRIPT"
  fi

  log_success "Test script found: $TEST_SCRIPT"
}

check_disk_space() {
  local available_mb
  available_mb=$(df -m "$PROJECT_ROOT" | awk 'NR==2 {print $4}')

  if [ "$available_mb" -lt 100 ]; then
    log_error "Insufficient disk space: ${available_mb}MB available (minimum 100MB required)"
    exit 1
  fi

  log_success "Disk space check passed (${available_mb}MB available)"
}

create_logs_directory() {
  if [ ! -d "$LOGS_DIR" ]; then
    mkdir -p "$LOGS_DIR"
    log_info "Created logs directory: $LOGS_DIR"
  fi
}

# ==============================================================================
# Log Capture
# ==============================================================================

start_log_capture() {
  log_info "Starting background log capture..."

  # Capture logs in background and redirect to /dev/null
  # (logs will be retrieved on-demand if tests fail)
  docker compose logs -f -t user-service app postgres > /dev/null 2>&1 &
  LOG_CAPTURE_PID=$!

  # Verify process started successfully
  if ! kill -0 "$LOG_CAPTURE_PID" 2>/dev/null; then
    log_error "Failed to start log capture process"
    exit 1
  fi

  log_success "Log capture started (PID: $LOG_CAPTURE_PID)"
}

# ==============================================================================
# Test Execution
# ==============================================================================

run_tests() {
  local attempt=$1

  if [ "$attempt" -eq 1 ]; then
    log_info "Executing smoke tests: $TEST_SCRIPT"
  else
    log_info "Retry attempt $attempt: executing tests..."
  fi

  # Run tests with timeout
  if timeout "${TEST_TIMEOUT}s" "$TEST_SCRIPT"; then
    return 0
  else
    return 1
  fi
}

# ==============================================================================
# Incident Handling
# ==============================================================================

capture_incident_logs() {
  INCIDENT_TIMESTAMP=$(date +%s)

  log_error "Test failed after retry."
  log_info "Capturing incident logs..."

  # Capture logs from each container (last 100 lines)
  for container in "${CONTAINERS[@]}"; do
    local service_name="$container"

    # Map container name to docker-compose service name
    case "$container" in
      "monolith")
        service_name="app"
        ;;
      "postgresql")
        service_name="postgres"
        ;;
    esac

    local log_file="$LOGS_DIR/incident-${INCIDENT_TIMESTAMP}-${container}.log"

    if docker compose logs --tail=100 "$service_name" > "$log_file" 2>&1; then
      log_success "Captured logs: $container → $(basename "$log_file")"
    else
      log_warning "Failed to capture logs for: $container"
      echo "ERROR: Could not retrieve logs for $service_name" > "$log_file"
    fi
  done

  # Create consolidated incident report
  create_incident_report
}

create_incident_report() {
  local report_file="$LOGS_DIR/incident-${INCIDENT_TIMESTAMP}.log"

  cat > "$report_file" <<EOF
========================================
INCIDENT REPORT
========================================
Timestamp: $(date -Iseconds -d @"$INCIDENT_TIMESTAMP")
Test Failed: $TEST_SCRIPT
Exit Code: 1
Working Directory: $PROJECT_ROOT
Docker Compose Status:

$(docker compose ps)

========================================
EOF

  # Append logs from each container
  for container in "${CONTAINERS[@]}"; do
    local container_log="$LOGS_DIR/incident-${INCIDENT_TIMESTAMP}-${container}.log"

    if [ -f "$container_log" ]; then
      cat >> "$report_file" <<EOF

========================================
${container^^} LOGS (last 100 lines)
========================================
$(cat "$container_log")

========================================
EOF
    fi
  done

  log_error "Incident report saved: $report_file"
  echo ""
  log_info "📋 To diagnose this incident, execute:"
  echo -e "${BLUE}   claude --agent marcus${NC}"
  echo -e "${BLUE}   > diagnosticar incidente $report_file${NC}"
  echo ""
}

# ==============================================================================
# Main Execution
# ==============================================================================

main() {
  echo ""
  echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║${NC}  ${YELLOW}🏥 ScopeFlow AI - Monitor & Auto-Healing${NC}              ${BLUE}║${NC}"
  echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""

  # Safety checks
  check_lockfile
  check_docker_running
  check_test_script
  check_disk_space
  create_logs_directory

  echo ""

  # Start log capture in background
  start_log_capture

  echo ""
  log_info "Monitored containers:"
  for container in "${CONTAINERS[@]}"; do
    echo "  - $container"
  done
  echo ""

  # First attempt
  if run_tests 1; then
    echo ""
    log_success "Tests passed! System is healthy."
    log_info "Containers monitored: ${CONTAINERS[*]}"
    log_info "Incidents detected: 0"
    echo ""
    exit 0
  fi

  # First failure - retry after delay
  echo ""
  log_warning "Test failed. Retrying in ${RETRY_DELAY} seconds..."
  sleep "$RETRY_DELAY"
  echo ""

  # Second attempt
  if run_tests 2; then
    echo ""
    log_success "Tests passed after retry! System recovered."
    log_info "Containers monitored: ${CONTAINERS[*]}"
    log_info "Incidents detected: 0 (1 transient failure)"
    echo ""
    exit 0
  fi

  # Both attempts failed - capture incident logs
  echo ""
  capture_incident_logs
  exit 1
}

main "$@"
