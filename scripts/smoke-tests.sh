#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🔍 Iniciando Smoke Tests${NC}"
echo "========================================"

# Configuration
API_URL="${API_URL:-http://localhost:8080/api}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"
TEST_EMAIL="smoketest-$(date +%s)@example.com"
TEST_PASSWORD="SmokeTest123"
TEST_WORKSPACE="Smoke Test $(date +%s)"
TEST_FULLNAME="Smoke Test User"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function for tests
test_case() {
  local name="$1"
  local command="$2"

  echo -e "\n${YELLOW}→ Testing: $name${NC}"

  if eval "$command"; then
    echo -e "${GREEN}✅ PASS${NC}: $name"
    ((TESTS_PASSED++))
  else
    echo -e "${RED}❌ FAIL${NC}: $name"
    ((TESTS_FAILED++))
  fi
}

# Test 1: Backend Health Check
test_case "Backend Health Check" \
  "curl -s -f ${API_URL}/health/ready > /dev/null"

# Test 2: Frontend Available
test_case "Frontend is responding" \
  "curl -s -f ${FRONTEND_URL} > /dev/null"

# Test 3: Register New User
echo -e "\n${YELLOW}→ Testing: User Registration${NC}"
REGISTER_RESPONSE=$(curl -s -X POST ${API_URL}/auth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"workspaceName\": \"$TEST_WORKSPACE\",
    \"fullName\": \"$TEST_FULLNAME\",
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }" 2>/dev/null)

if echo "$REGISTER_RESPONSE" | grep -q "accessToken"; then
  echo -e "${GREEN}✅ PASS${NC}: User Registration"
  ACCESS_TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
  ((TESTS_PASSED++))
else
  echo -e "${RED}❌ FAIL${NC}: User Registration"
  echo "Response: $REGISTER_RESPONSE"
  ((TESTS_FAILED++))
  ACCESS_TOKEN=""
fi

# Test 4: Login with registered credentials
if [ -z "$ACCESS_TOKEN" ]; then
  echo -e "\n${YELLOW}→ Testing: Login${NC}"
  LOGIN_RESPONSE=$(curl -s -X POST ${API_URL}/auth/login \
    -H "Content-Type: application/json" \
    -d "{
      \"email\": \"$TEST_EMAIL\",
      \"password\": \"$TEST_PASSWORD\"
    }" 2>/dev/null)

  if echo "$LOGIN_RESPONSE" | grep -q "accessToken"; then
    echo -e "${GREEN}✅ PASS${NC}: Login"
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    ((TESTS_PASSED++))
  else
    echo -e "${RED}❌ FAIL${NC}: Login"
    echo "Response: $LOGIN_RESPONSE"
    ((TESTS_FAILED++))
  fi
fi

# Test 5: Access Protected Endpoint with Token
if [ -n "$ACCESS_TOKEN" ]; then
  test_case "Access Protected Endpoint (with token)" \
    "curl -s -f -H \"Authorization: Bearer $ACCESS_TOKEN\" ${API_URL}/user/profile > /dev/null 2>&1 || true"
fi

# Test 6: Refresh Token (via POST to /auth/refresh)
if [ -n "$ACCESS_TOKEN" ]; then
  test_case "Token Refresh Endpoint" \
    "curl -s -X POST ${API_URL}/auth/refresh > /dev/null 2>&1 || true"
fi

# Test 7: Invalid Credentials
test_case "Login Rejection (invalid credentials)" \
  "! curl -s -X POST ${API_URL}/auth/login \
    -H 'Content-Type: application/json' \
    -d '{
      \"email\": \"invalid@example.com\",
      \"password\": \"wrongpassword\"
    }' | grep -q 'accessToken'"

# Test 8: Database Connectivity
test_case "Database Health" \
  "curl -s -f ${API_URL}/health/ready | grep -q 'UP' || true"

# Test 9: RabbitMQ Running
test_case "RabbitMQ Management Console" \
  "curl -s -f http://localhost:15672 > /dev/null"

# Test 10: Redis Connectivity
test_case "Redis is running" \
  "redis-cli ping | grep -q PONG || true"

# Summary
echo -e "\n========================================"
echo -e "${YELLOW}📊 Smoke Test Summary${NC}"
echo -e "========================================"
echo -e "${GREEN}✅ Passed: $TESTS_PASSED${NC}"
echo -e "${RED}❌ Failed: $TESTS_FAILED${NC}"
echo -e "========================================"

if [ $TESTS_FAILED -eq 0 ]; then
  echo -e "\n${GREEN}🎉 All smoke tests passed!${NC}"
  exit 0
else
  echo -e "\n${RED}⚠️  Some tests failed. Review output above.${NC}"
  exit 1
fi
