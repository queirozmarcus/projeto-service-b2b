#!/bin/bash
set -euo pipefail

# ==============================================================================
# E2E Smoke Tests - Authentication Flow
# ==============================================================================
# Tests complete auth flow in both modes:
# 1. FLAG=false (monolith handles auth)
# 2. FLAG=true (user-service handles auth)
#
# Test scenarios:
# - 4 happy path: register → login → /me → workspace access
# - 3 error paths: invalid credentials, expired JWT, invalid JWT
#
# Usage:
#   ./tests/e2e/auth-flow.test.sh
#   AUTH_SERVICE_EXTRACTED=true ./tests/e2e/auth-flow.test.sh
# ==============================================================================

# Colors
readonly GREEN='\033[0;32m'
readonly RED='\033[0;31m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
readonly BASE_URL="${BASE_URL:-http://localhost:8080}"
readonly AUTH_SERVICE_EXTRACTED="${AUTH_SERVICE_EXTRACTED:-false}"
readonly TEST_EMAIL="smoke-test-$(date +%s)@example.com"
readonly TEST_PASSWORD="SmokeTest123!"
readonly TEST_FULLNAME="Smoke Test User"
readonly TEST_PHONE="+5511999999999"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=7

# Temp file for response storage
readonly RESPONSE_FILE=$(mktemp)
trap "rm -f $RESPONSE_FILE" EXIT

# Global variables for test context
JWT_TOKEN=""
USER_ID=""
REFRESH_TOKEN_COOKIE=""

# ==============================================================================
# Helper Functions
# ==============================================================================

log_info() {
  echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
  echo -e "${GREEN}✓${NC} $1"
  ((TESTS_PASSED++))
}

log_error() {
  echo -e "${RED}✗${NC} $1"
  ((TESTS_FAILED++))
}

log_test() {
  echo ""
  echo -e "${YELLOW}━━━ Test $1/$TESTS_TOTAL: $2 ━━━${NC}"
}

# Make HTTP request and capture response + status code
# Usage: http_request METHOD URL [BODY] [HEADERS...]
http_request() {
  local method=$1
  local url=$2
  local body=${3:-}
  shift 2
  [ -n "$body" ] && shift

  local -a curl_args=(
    -s -w "\nHTTP_STATUS:%{http_code}"
    -X "$method"
  )

  # Add headers from remaining args
  for header in "$@"; do
    curl_args+=(-H "$header")
  done

  # Add body if provided
  if [ -n "$body" ]; then
    curl_args+=(-d "$body")
  fi

  # Include cookies in request
  if [ -n "$REFRESH_TOKEN_COOKIE" ]; then
    curl_args+=(-H "Cookie: refreshToken=$REFRESH_TOKEN_COOKIE")
  fi

  # Capture response + status code
  curl "${curl_args[@]}" "$url" > "$RESPONSE_FILE" 2>/dev/null || true
}

# Extract HTTP status from response file
get_status() {
  grep -o 'HTTP_STATUS:[0-9]*' "$RESPONSE_FILE" | cut -d':' -f2
}

# Extract JSON body (without status line)
get_body() {
  sed '/HTTP_STATUS:/d' "$RESPONSE_FILE"
}

# Extract JSON field using grep/sed (avoids jq dependency)
json_field() {
  local field=$1
  get_body | grep -o "\"$field\":\"[^\"]*" | cut -d'"' -f4
}

# Decode JWT payload (Base64 URL decode)
decode_jwt_payload() {
  local jwt=$1
  local payload=$(echo "$jwt" | cut -d'.' -f2)
  # Add padding if needed
  local pad=$((4 - ${#payload} % 4))
  [ $pad -lt 4 ] && payload="${payload}$(printf '%*s' $pad | tr ' ' '=')"
  # Base64 decode (URL-safe)
  echo "$payload" | tr '_-' '/+' | base64 -d 2>/dev/null || echo "{}"
}

# Validate JWT structure and claims
validate_jwt() {
  local jwt=$1
  local expected_sub=$2

  # Check format: 3 parts separated by dots
  local parts=$(echo "$jwt" | tr -cd '.' | wc -c)
  if [ "$parts" -ne 2 ]; then
    echo "Invalid JWT format (expected 3 parts, got $((parts + 1)))"
    return 1
  fi

  # Decode payload
  local payload=$(decode_jwt_payload "$jwt")

  # Extract claims
  local sub=$(echo "$payload" | grep -o '"sub":"[^"]*' | cut -d'"' -f4)
  local exp=$(echo "$payload" | grep -o '"exp":[0-9]*' | cut -d':' -f2)

  # Validate sub claim
  if [ "$sub" != "$expected_sub" ]; then
    echo "JWT sub mismatch: expected=$expected_sub, got=$sub"
    return 1
  fi

  # Validate expiration (must be in future)
  local now=$(date +%s)
  if [ -z "$exp" ] || [ "$exp" -le "$now" ]; then
    echo "JWT expired or invalid exp claim"
    return 1
  fi

  return 0
}

# ==============================================================================
# Test Cases
# ==============================================================================

# Test 1: Register new user
test_register() {
  log_test 1 "Register new user"

  local payload=$(cat <<EOF
{
  "email": "$TEST_EMAIL",
  "password": "$TEST_PASSWORD",
  "fullName": "$TEST_FULLNAME",
  "phone": "$TEST_PHONE"
}
EOF
)

  http_request POST "$BASE_URL/api/v1/auth/register" "$payload" "Content-Type: application/json"

  local status=$(get_status)
  local body=$(get_body)

  if [ "$status" != "201" ]; then
    log_error "Registration failed: expected 201, got $status"
    echo "Response body: $body"
    return 1
  fi

  # Extract tokens
  JWT_TOKEN=$(json_field "accessToken")
  USER_ID=$(json_field "userId")

  if [ -z "$JWT_TOKEN" ] || [ -z "$USER_ID" ]; then
    log_error "Missing accessToken or userId in response"
    echo "Response body: $body"
    return 1
  fi

  # Validate JWT
  if ! validate_jwt "$JWT_TOKEN" "$USER_ID"; then
    log_error "JWT validation failed"
    return 1
  fi

  log_success "User registered successfully (userId=$USER_ID)"
  log_info "JWT payload: $(decode_jwt_payload "$JWT_TOKEN")"
}

# Test 2: Login with valid credentials
test_login() {
  log_test 2 "Login with valid credentials"

  local payload=$(cat <<EOF
{
  "email": "$TEST_EMAIL",
  "password": "$TEST_PASSWORD"
}
EOF
)

  http_request POST "$BASE_URL/api/v1/auth/login" "$payload" "Content-Type: application/json"

  local status=$(get_status)
  local body=$(get_body)

  if [ "$status" != "200" ]; then
    log_error "Login failed: expected 200, got $status"
    echo "Response body: $body"
    return 1
  fi

  # Extract new token
  JWT_TOKEN=$(json_field "accessToken")
  local returned_user_id=$(json_field "userId")

  if [ -z "$JWT_TOKEN" ]; then
    log_error "Missing accessToken in response"
    echo "Response body: $body"
    return 1
  fi

  if [ "$returned_user_id" != "$USER_ID" ]; then
    log_error "userId mismatch: expected $USER_ID, got $returned_user_id"
    return 1
  fi

  # Validate JWT
  if ! validate_jwt "$JWT_TOKEN" "$USER_ID"; then
    log_error "JWT validation failed"
    return 1
  fi

  log_success "Login successful (userId=$USER_ID)"
}

# Test 3: Get authenticated user profile
test_get_me() {
  log_test 3 "Get authenticated user profile (/auth/me)"

  if [ -z "$JWT_TOKEN" ]; then
    log_error "Cannot test /me: no JWT token from previous tests"
    return 1
  fi

  http_request GET "$BASE_URL/api/v1/auth/me" "" "Authorization: Bearer $JWT_TOKEN"

  local status=$(get_status)
  local body=$(get_body)

  if [ "$status" != "200" ]; then
    log_error "GET /auth/me failed: expected 200, got $status"
    echo "Response body: $body"
    return 1
  fi

  # Validate response contains user data
  local email=$(json_field "email")
  if [ "$email" != "$TEST_EMAIL" ]; then
    log_error "Email mismatch in /me response: expected $TEST_EMAIL, got $email"
    return 1
  fi

  log_success "User profile retrieved successfully"
}

# Test 4: Access protected workspace endpoint (validates JWT cross-service)
test_workspace_access() {
  log_test 4 "Access protected workspace endpoint (JWT compatibility)"

  if [ -z "$JWT_TOKEN" ]; then
    log_error "Cannot test workspace access: no JWT token from previous tests"
    return 1
  fi

  http_request GET "$BASE_URL/api/v1/workspaces" "" "Authorization: Bearer $JWT_TOKEN"

  local status=$(get_status)
  local body=$(get_body)

  # 200 OK or 404 (no workspaces) are both acceptable — proves JWT is valid
  if [ "$status" != "200" ] && [ "$status" != "404" ]; then
    log_error "Workspace access failed: expected 200/404, got $status"
    echo "Response body: $body"
    return 1
  fi

  log_success "JWT accepted by monolith (workspace endpoint accessible)"
}

# Test 5: Login with invalid credentials
test_invalid_credentials() {
  log_test 5 "Login rejection with invalid credentials"

  local payload=$(cat <<EOF
{
  "email": "$TEST_EMAIL",
  "password": "wrongpassword123"
}
EOF
)

  http_request POST "$BASE_URL/api/v1/auth/login" "$payload" "Content-Type: application/json"

  local status=$(get_status)
  local body=$(get_body)

  if [ "$status" != "401" ]; then
    log_error "Expected 401 for invalid credentials, got $status"
    echo "Response body: $body"
    return 1
  fi

  # Validate RFC 9457 Problem Details
  local type=$(json_field "type")
  if [[ ! "$type" =~ "problem" ]] && [[ ! "$body" =~ "detail" ]]; then
    log_error "Response does not follow RFC 9457 Problem Details format"
    echo "Response body: $body"
    return 1
  fi

  log_success "Invalid credentials rejected correctly (401)"
}

# Test 6: Access with expired JWT (simulate by using malformed token)
test_expired_jwt() {
  log_test 6 "Access rejection with expired/invalid JWT"

  # Use a malformed JWT (simulates expired)
  local invalid_jwt="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiZXhwIjoxfQ.invalid"

  http_request GET "$BASE_URL/api/v1/auth/me" "" "Authorization: Bearer $invalid_jwt"

  local status=$(get_status)

  if [ "$status" != "401" ]; then
    log_error "Expected 401 for expired JWT, got $status"
    echo "Response body: $(get_body)"
    return 1
  fi

  log_success "Expired JWT rejected correctly (401)"
}

# Test 7: Access with invalid JWT format
test_invalid_jwt() {
  log_test 7 "Access rejection with invalid JWT format"

  local invalid_jwt="invalid.jwt.token"

  http_request GET "$BASE_URL/api/v1/auth/me" "" "Authorization: Bearer $invalid_jwt"

  local status=$(get_status)

  if [ "$status" != "401" ]; then
    log_error "Expected 401 for invalid JWT, got $status"
    echo "Response body: $(get_body)"
    return 1
  fi

  log_success "Invalid JWT rejected correctly (401)"
}

# ==============================================================================
# Cleanup
# ==============================================================================

cleanup() {
  log_info "Cleaning up test data..."

  if [ -n "$USER_ID" ] && [ -n "$JWT_TOKEN" ]; then
    # Attempt to delete test user (optional — may not have delete endpoint)
    http_request DELETE "$BASE_URL/api/v1/users/$USER_ID" "" "Authorization: Bearer $JWT_TOKEN" 2>/dev/null || true
  fi

  log_info "Cleanup complete"
}

# ==============================================================================
# Main Execution
# ==============================================================================

main() {
  echo ""
  echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║${NC}  ${YELLOW}🧪 E2E Smoke Tests - Authentication Flow${NC}              ${BLUE}║${NC}"
  echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "${BLUE}Configuration:${NC}"
  echo -e "  Base URL:              $BASE_URL"
  echo -e "  Feature flag:          AUTH_SERVICE_EXTRACTED=${YELLOW}$AUTH_SERVICE_EXTRACTED${NC}"
  echo -e "  Expected routing:      $([ "$AUTH_SERVICE_EXTRACTED" = "true" ] && echo "${YELLOW}user-service (port 8081)${NC}" || echo "${YELLOW}monolith (port 8080)${NC}")"
  echo -e "  Test email:            $TEST_EMAIL"
  echo ""

  # Check service health
  log_info "Checking service health..."
  if ! curl -sf "$BASE_URL/actuator/health/readiness" > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Service is not ready at $BASE_URL${NC}"
    echo "Make sure the application is running: docker compose up -d"
    exit 1
  fi
  log_success "Service is ready"

  # Run tests
  test_register || true
  test_login || true
  test_get_me || true
  test_workspace_access || true
  test_invalid_credentials || true
  test_expired_jwt || true
  test_invalid_jwt || true

  # Cleanup
  cleanup

  # Summary
  echo ""
  echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║${NC}  ${YELLOW}📊 Test Summary${NC}                                        ${BLUE}║${NC}"
  echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "  Total tests:     $TESTS_TOTAL"
  echo -e "  ${GREEN}Passed:          $TESTS_PASSED${NC}"
  echo -e "  ${RED}Failed:          $TESTS_FAILED${NC}"
  echo ""

  if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All smoke tests passed!${NC}"
    echo ""
    exit 0
  else
    echo -e "${RED}⚠️  Some tests failed. Review output above.${NC}"
    echo ""
    exit 1
  fi
}

main "$@"
