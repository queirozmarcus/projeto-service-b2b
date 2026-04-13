#!/bin/bash
set -e

BASE_URL="http://localhost:8080"
FRONTEND_URL="http://localhost:3000"

echo "🧪 TESTING BRIEFING SESSION FLOW — Sprint 6 Task 3"
echo "=================================================="
echo ""
echo "Base URL: $BASE_URL"
echo "Frontend: $FRONTEND_URL"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}[1/6] Testing Backend Health${NC}"
HEALTH=$(curl -s http://localhost:8080/api/v1/actuator/health 2>&1 | head -c 50)
echo "Response: $HEALTH"
echo ""

echo -e "${BLUE}[2/6] Testing Frontend Availability${NC}"
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000)
echo "Frontend status code: $FRONTEND_STATUS (expected: 200)"
[ "$FRONTEND_STATUS" -eq 200 ] && echo -e "${GREEN}✅ Frontend responding${NC}" || echo -e "${YELLOW}⚠️ Frontend may have issue${NC}"
echo ""

echo -e "${BLUE}[3/6] Test 1: POST /api/v1/proposals/{id}/briefing-sessions${NC}"
echo "Command:"
echo 'curl -X POST http://localhost:8080/api/v1/proposals/{proposalId}/briefing-sessions \'
echo '  -H "Authorization: Bearer {jwtToken}" \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"serviceType": "SOCIAL_MEDIA"}'"'"
echo ""
echo "Steps:"
echo "  1. Get a valid proposalId and JWT token from your auth flow"
echo "  2. Replace {proposalId} and {jwtToken} in the command above"
echo "  3. Run the command"
echo "  4. Copy the publicToken from the response"
echo ""

echo -e "${BLUE}[4/6] Test 2: GET /api/v1/briefing-sessions/{id}/questions${NC}"
echo "Command (after getting sessionId from step 3):"
echo 'curl -X GET http://localhost:8080/api/v1/briefing-sessions/{sessionId}/questions \'
echo '  -H "Authorization: Bearer {jwtToken}" \'
echo '  -H "Content-Type: application/json"'
echo ""
echo "Expected response: Array of questions with type, text, orderIndex"
echo ""

echo -e "${BLUE}[5/6] Test 3: GET /briefing/{publicToken} (Frontend)${NC}"
echo "Steps:"
echo "  1. Copy the publicToken from step 3 response"
echo "  2. Open in browser: http://localhost:3000/briefing/{publicToken}"
echo "  3. Verify:"
echo "     ✓ Page loads without console errors"
echo "     ✓ First question appears"
echo "     ✓ Stepper shows '1 of N'"
echo "     ✓ Answer input renders (TEXT or TEXTAREA)"
echo ""

echo -e "${BLUE}[6/6] Test 4: Complete Briefing Flow${NC}"
echo "Steps (in browser):"
echo "  1. Answer first question and click 'Next'"
echo "  2. Continue through all questions"
echo "  3. On last question, click 'Complete'"
echo "  4. Verify:"
echo "     ✓ CompletionSummary renders"
echo "     ✓ Score displayed (0-100%)"
echo "     ✓ Message shows"
echo "     ✓ If authenticated: 'Review Proposal' link works"
echo ""

echo "=================================================="
echo -e "${GREEN}✅ SETUP COMPLETE${NC}"
echo ""
echo "Next: Follow the manual tests above"
echo ""
echo "Curl commands for quick testing:"
echo "---"
echo ""
echo "# Test backend health (non-SSL endpoint)"
echo "curl -s http://localhost:8080/api/v1/actuator/health | jq ."
echo ""
echo "# Test frontend"
echo "curl -s http://localhost:3000 | head -5"
echo ""

