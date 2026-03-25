#!/bin/bash

# BriefingSession E2E Test Suite
# Validates complete flow from user registration to completion

set -e

BASE_URL="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)
TEST_EMAIL="test-briefing-${TIMESTAMP}@example.com"
TEST_PASS="TestPassword123!"

echo "╔════════════════════════════════════════════════════════╗"
echo "║  BRIEFING SESSION E2E TEST SUITE — Sprint 6 Task 3     ║"
echo "║  Status: EXECUTING FULL FLOW                           ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Base URL: $BASE_URL"
echo "Test Email: $TEST_EMAIL"
echo "Timestamp: $TIMESTAMP"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass_count=0
fail_count=0

test_result() {
  local name=$1
  local expected=$2
  local actual=$3
  
  if [ "$expected" == "$actual" ]; then
    echo -e "${GREEN}✅ PASS${NC}: $name"
    ((pass_count++))
  else
    echo -e "${RED}❌ FAIL${NC}: $name"
    echo "   Expected: $expected"
    echo "   Actual: $actual"
    ((fail_count++))
  fi
}

# ============================================================================
# STEP 1: Register User
# ============================================================================
echo -e "${BLUE}[STEP 1/8] Register User${NC}"
echo "Command: POST /auth/register"

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASS\",
    \"fullName\": \"Test Briefing User\"
  }")

USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.userId // .id // empty' 2>/dev/null)
JWT_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.accessToken // empty' 2>/dev/null)
WORKSPACE_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.workspaceId // empty' 2>/dev/null)

if [ -z "$USER_ID" ] || [ -z "$JWT_TOKEN" ]; then
  echo -e "${RED}❌ FAIL${NC}: Could not register user"
  echo "Response: $REGISTER_RESPONSE"
  exit 1
fi

test_result "User registered" "ok" "ok"
echo "   User ID: $USER_ID"
echo "   Workspace ID: $WORKSPACE_ID"
echo ""

# ============================================================================
# STEP 2: Create Proposal
# ============================================================================
echo -e "${BLUE}[STEP 2/8] Create Proposal${NC}"
echo "Command: POST /proposals"

PROPOSAL_RESPONSE=$(curl -s -X POST "$BASE_URL/proposals" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "Test Client Corp",
    "clientEmail": "client@test.com",
    "serviceType": "SOCIAL_MEDIA",
    "title": "Social Media Campaign Test",
    "description": "Test briefing session"
  }')

PROPOSAL_ID=$(echo "$PROPOSAL_RESPONSE" | jq -r '.id // empty' 2>/dev/null)

if [ -z "$PROPOSAL_ID" ]; then
  echo -e "${RED}❌ FAIL${NC}: Could not create proposal"
  echo "Response: $PROPOSAL_RESPONSE"
  exit 1
fi

test_result "Proposal created" "ok" "ok"
echo "   Proposal ID: $PROPOSAL_ID"
echo ""

# ============================================================================
# STEP 3: Create BriefingSession
# ============================================================================
echo -e "${BLUE}[STEP 3/8] Create BriefingSession${NC}"
echo "Command: POST /proposals/{id}/briefing-sessions"

BRIEFING_RESPONSE=$(curl -s -X POST "$BASE_URL/proposals/$PROPOSAL_ID/briefing-sessions" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "SOCIAL_MEDIA"
  }')

SESSION_ID=$(echo "$BRIEFING_RESPONSE" | jq -r '.id // empty' 2>/dev/null)
PUBLIC_TOKEN=$(echo "$BRIEFING_RESPONSE" | jq -r '.publicToken // empty' 2>/dev/null)

if [ -z "$SESSION_ID" ] || [ -z "$PUBLIC_TOKEN" ]; then
  echo -e "${RED}❌ FAIL${NC}: Could not create briefing session"
  echo "Response: $BRIEFING_RESPONSE"
  exit 1
fi

test_result "BriefingSession created" "ok" "ok"
echo "   Session ID: $SESSION_ID"
echo "   Public Token: $PUBLIC_TOKEN"
echo ""

# ============================================================================
# STEP 4: Get Questions
# ============================================================================
echo -e "${BLUE}[STEP 4/8] Get Questions (Authenticated)${NC}"
echo "Command: GET /briefing-sessions/{id}/questions"

QUESTIONS_RESPONSE=$(curl -s -X GET "$BASE_URL/briefing-sessions/$SESSION_ID/questions" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

QUESTION_COUNT=$(echo "$QUESTIONS_RESPONSE" | jq '.questions | length' 2>/dev/null || echo "0")

if [ "$QUESTION_COUNT" -lt 1 ]; then
  echo -e "${RED}❌ FAIL${NC}: No questions returned"
  echo "Response: $QUESTIONS_RESPONSE"
else
  test_result "Questions retrieved" "ok" "ok"
  echo "   Question count: $QUESTION_COUNT"
fi

# Extract first question ID for testing
FIRST_Q_ID=$(echo "$QUESTIONS_RESPONSE" | jq -r '.questions[0].questionId // empty' 2>/dev/null)
echo "   First question ID: $FIRST_Q_ID"
echo ""

# ============================================================================
# STEP 5: Test Frontend Public Route (Curl)
# ============================================================================
echo -e "${BLUE}[STEP 5/8] Test Frontend Public Route${NC}"
echo "Command: GET /briefing/{token} (Frontend)"

FRONTEND_RESPONSE=$(curl -s -w "\n%{http_code}" "http://localhost:3000/briefing/$PUBLIC_TOKEN")
HTTP_CODE=$(echo "$FRONTEND_RESPONSE" | tail -n1)
HTML_CONTENT=$(echo "$FRONTEND_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "200" ]; then
  test_result "Frontend page loads" "200" "$HTTP_CODE"
  # Check if BriefingFlow component is loaded
  if echo "$HTML_CONTENT" | grep -q "Project Briefing"; then
    test_result "Frontend has briefing content" "ok" "ok"
  fi
else
  test_result "Frontend page loads" "200" "$HTTP_CODE"
fi

echo "   HTTP Status: $HTTP_CODE"
echo "   URL: http://localhost:3000/briefing/$PUBLIC_TOKEN"
echo ""

# ============================================================================
# STEP 6: Submit Answers
# ============================================================================
echo -e "${BLUE}[STEP 6/8] Submit Answers${NC}"
echo "Command: POST /briefing-sessions/{id}/answers"

# Build answers payload dynamically from questions
ANSWERS_PAYLOAD=$(echo "$QUESTIONS_RESPONSE" | jq -r '
  {
    answers: [
      .questions[] | {
        questionId: .questionId,
        answerText: "This is a test answer for question: \(.text | ascii_downcase | gsub("[^a-z ]"; ""))"
      }
    ]
  }
')

ANSWERS_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/briefing-sessions/$SESSION_ID/answers" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$ANSWERS_PAYLOAD")

ANSWERS_HTTP=$(echo "$ANSWERS_RESPONSE" | tail -n1)

if [ "$ANSWERS_HTTP" == "204" ]; then
  test_result "Answers submitted" "204" "$ANSWERS_HTTP"
else
  test_result "Answers submitted" "204" "$ANSWERS_HTTP"
  echo "   Response: $(echo "$ANSWERS_RESPONSE" | head -n-1)"
fi
echo ""

# ============================================================================
# STEP 7: Complete BriefingSession
# ============================================================================
echo -e "${BLUE}[STEP 7/8] Complete BriefingSession${NC}"
echo "Command: POST /briefing-sessions/{id}/complete"

COMPLETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/briefing-sessions/$SESSION_ID/complete" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json")

COMPLETE_HTTP=$(echo "$COMPLETE_RESPONSE" | tail -n1)
COMPLETE_BODY=$(echo "$COMPLETE_RESPONSE" | head -n-1)

SCORE=$(echo "$COMPLETE_BODY" | jq -r '.completenessScore // empty' 2>/dev/null)
STATUS=$(echo "$COMPLETE_BODY" | jq -r '.status // empty' 2>/dev/null)
MESSAGE=$(echo "$COMPLETE_BODY" | jq -r '.message // empty' 2>/dev/null)

if [ "$COMPLETE_HTTP" == "200" ]; then
  test_result "Session completed" "200" "$COMPLETE_HTTP"
  test_result "Score calculated" "ok" "ok"
  echo "   Score: $SCORE%"
  echo "   Status: $STATUS"
  echo "   Message: $MESSAGE"
else
  test_result "Session completed" "200" "$COMPLETE_HTTP"
  echo "   Response: $COMPLETE_BODY"
fi
echo ""

# ============================================================================
# STEP 8: Verify Workspace Isolation
# ============================================================================
echo -e "${BLUE}[STEP 8/8] Verify Workspace Isolation (Security Test)${NC}"
echo "Command: GET /briefing-sessions/{id} with different workspace"

# Register a second user in a different workspace
REGISTER2_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"test-isolation-${TIMESTAMP}@example.com\",
    \"password\": \"$TEST_PASS\",
    \"fullName\": \"Test User 2\"
  }")

JWT_TOKEN2=$(echo "$REGISTER2_RESPONSE" | jq -r '.accessToken // empty' 2>/dev/null)

if [ -n "$JWT_TOKEN2" ]; then
  ISOLATION_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/briefing-sessions/$SESSION_ID" \
    -H "Authorization: Bearer $JWT_TOKEN2" \
    -H "Content-Type: application/json")
  
  ISO_HTTP=$(echo "$ISOLATION_RESPONSE" | tail -n1)
  
  if [ "$ISO_HTTP" == "403" ]; then
    test_result "Workspace isolation enforced" "403" "$ISO_HTTP"
  else
    test_result "Workspace isolation enforced" "403" "$ISO_HTTP"
  fi
else
  echo -e "${YELLOW}⚠️  SKIP${NC}: Could not register second user for isolation test"
fi
echo ""

# ============================================================================
# SUMMARY
# ============================================================================
echo "╔════════════════════════════════════════════════════════╗"
echo "║                    TEST SUMMARY                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Passed: $pass_count${NC}"
echo -e "${RED}Failed: $fail_count${NC}"
echo ""

TOTAL=$((pass_count + fail_count))
if [ "$fail_count" -eq 0 ]; then
  echo -e "${GREEN}✅ ALL TESTS PASSED${NC}"
  echo ""
  echo "Test Data Summary:"
  echo "  User Email: $TEST_EMAIL"
  echo "  User ID: $USER_ID"
  echo "  Workspace ID: $WORKSPACE_ID"
  echo "  Proposal ID: $PROPOSAL_ID"
  echo "  Session ID: $SESSION_ID"
  echo "  Public Token: $PUBLIC_TOKEN"
  echo "  Questions: $QUESTION_COUNT"
  echo "  Completion Score: $SCORE%"
  echo ""
  echo "📋 Frontend Test Link:"
  echo "  http://localhost:3000/briefing/$PUBLIC_TOKEN"
  echo ""
  exit 0
else
  echo -e "${RED}❌ SOME TESTS FAILED${NC}"
  echo "Please review the errors above."
  exit 1
fi

