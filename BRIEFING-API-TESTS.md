# BriefingSession API Tests — Manual E2E

**Status:** Staging deployment operational  
**Date:** 2026-03-25 22:50 UTC  
**Base URL:** http://localhost:8080/api/v1

---

## Test Sequence

### Step 1: Create/Login User (Get JWT Token)

**Endpoint:** `POST /auth/register` (or `/auth/login`)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-briefing@example.com",
    "password": "TestPassword123!",
    "name": "Test User"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "test-briefing@example.com",
  "name": "Test User",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440001",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900000
}
```

**Save for next steps:**
- `JWT_TOKEN` = `accessToken` value
- `WORKSPACE_ID` = `workspaceId` value
- `USER_ID` = `id` value

---

### Step 2: Create a Proposal

**Endpoint:** `POST /proposals`

```bash
curl -X POST http://localhost:8080/api/v1/proposals \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "Acme Corp",
    "clientEmail": "contact@acme.com",
    "serviceType": "SOCIAL_MEDIA",
    "title": "Social Media Campaign",
    "description": "Monthly social media management"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440001",
  "clientName": "Acme Corp",
  "clientEmail": "contact@acme.com",
  "serviceType": "SOCIAL_MEDIA",
  "status": "DRAFT",
  "title": "Social Media Campaign",
  "createdAt": "2026-03-25T22:50:00Z"
}
```

**Save for next steps:**
- `PROPOSAL_ID` = `id` value

---

### Step 3: Create BriefingSession

**Endpoint:** `POST /proposals/{proposalId}/briefing-sessions`

```bash
curl -X POST "http://localhost:8080/api/v1/proposals/{PROPOSAL_ID}/briefing-sessions" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "SOCIAL_MEDIA"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "publicToken": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "proposalId": "550e8400-e29b-41d4-a716-446655440002",
  "workspaceId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "IN_PROGRESS",
  "createdAt": "2026-03-25T22:50:00Z"
}
```

**Save for next steps:**
- `SESSION_ID` = `id` value
- `PUBLIC_TOKEN` = `publicToken` value — **CRITICAL for next step**

---

### Step 4: Get Questions (Authenticated)

**Endpoint:** `GET /briefing-sessions/{sessionId}/questions`

```bash
curl -X GET "http://localhost:8080/api/v1/briefing-sessions/{SESSION_ID}/questions" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "questions": [
    {
      "questionId": "q1",
      "text": "What is your current social media presence like?",
      "type": "TEXTAREA",
      "required": true,
      "orderIndex": 1
    },
    {
      "questionId": "q2",
      "text": "What are your main goals for this campaign?",
      "type": "TEXTAREA",
      "required": true,
      "orderIndex": 2
    },
    {
      "questionId": "q3",
      "text": "What is your budget range?",
      "type": "TEXT",
      "required": false,
      "orderIndex": 3
    }
  ]
}
```

**Verify:**
- ✅ Array of questions
- ✅ Each has: questionId, text, type (TEXT/TEXTAREA), required, orderIndex
- ✅ type values match V8 migration CHECK constraint: OPEN_ENDED, MULTIPLE_CHOICE, SCALE, YES_NO, TEXT, TEXTAREA

---

### Step 5: Test Frontend Public Route

**Navigate to Browser:**
```
http://localhost:3000/briefing/{PUBLIC_TOKEN}
```

Replace `{PUBLIC_TOKEN}` with the `publicToken` from Step 3.

**Expected Behavior:**
- ✅ Page loads (no 404)
- ✅ Header shows "Project Briefing"
- ✅ First question displays
- ✅ BriefingProgress shows "1 of 3" (or however many questions)
- ✅ QuestionCard renders with textarea input
- ✅ Next/Previous buttons visible
- ✅ **NO console errors** (open DevTools → Console tab)

**Browser Console Check:**
```javascript
// Should see no errors in console
// Example of clean console:
// [Next.js info about page load]
// [React Strict Mode warning (OK for dev)]
```

---

### Step 6: Submit Answers (Frontend or API)

#### Option A: Via Frontend (Browser)

1. Click into the first question's textarea
2. Type an answer (any text is fine for testing)
3. Click **"Next"** button
4. Repeat for all questions
5. On the last question, click **"Complete"** instead of "Next"
6. Wait for response (~1 second)

#### Option B: Via API (curl)

**Endpoint:** `POST /briefing-sessions/{sessionId}/answers`

```bash
curl -X POST "http://localhost:8080/api/v1/briefing-sessions/{SESSION_ID}/answers" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "answers": [
      {
        "questionId": "q1",
        "answerText": "We have Instagram and Facebook, but not TikTok. Struggling with consistency."
      },
      {
        "questionId": "q2",
        "answerText": "Increase engagement, reach new audience, drive website traffic"
      },
      {
        "questionId": "q3",
        "answerText": "$500-1000 per month"
      }
    ]
  }'
```

**Expected Response (204 No Content):**
```
HTTP/1.1 204 No Content
```

---

### Step 7: Complete BriefingSession

**Endpoint:** `POST /briefing-sessions/{sessionId}/complete`

#### Option A: Via Frontend

1. All questions answered (required ones must have text)
2. Click **"Complete"** button on last question
3. CompletionSummary appears

#### Option B: Via API

```bash
curl -X POST "http://localhost:8080/api/v1/briefing-sessions/{SESSION_ID}/complete" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "completenessScore": 85,
  "status": "COMPLETED",
  "message": "Excellent briefing! You provided comprehensive details that will help us prepare an accurate proposal."
}
```

**Score Calculation:**
- `score = (answeredRequired / totalRequired) * 100`
- 0-100 range
- >= 80%: "Excellent" (green badge)
- < 80%: "Good" (orange badge)

---

### Step 8: Verify Frontend Completion Screen

**Expected UI:**
- ✅ CompletionSummary component renders
- ✅ Emoji: 🎉 (high score) or 👍 (low score)
- ✅ Title: "Briefing Completed!"
- ✅ Score badge shows: "85%"
- ✅ Message displays
- ✅ If authenticated: "Review Proposal in Dashboard" button appears
- ✅ If not authenticated: Message "Our team will review..."

---

## Validation Checklist

| Step | Test | Expected | Status |
|------|------|----------|--------|
| 1 | Register/Login | JWT token returned | [ ] |
| 2 | Create Proposal | proposalId returned | [ ] |
| 3 | Create BriefingSession | publicToken returned | [ ] |
| 4 | Get Questions (API) | Array of questions | [ ] |
| 5 | Frontend Page Load | No 404, stepper shows | [ ] |
| 6 | Submit Answers | 204 or POST success | [ ] |
| 7 | Complete Session | Score calculated, status COMPLETED | [ ] |
| 8 | Completion Screen | CompletionSummary renders | [ ] |

---

## Common Issues & Fixes

### Issue: 404 on /briefing/{token}

**Symptom:** Browser shows "Invalid or Expired Link"

**Cause:** Token not found or invalid format

**Fix:**
1. Verify publicToken is copied exactly (no spaces, right format: UUID)
2. Check token is from Step 3 response
3. Ensure it's in the route: `/briefing/` + token

---

### Issue: Questions not loading (blank page)

**Symptom:** Page loads but no questions appear

**Cause:** API call failed or returned empty array

**Fix:**
1. Check browser console (DevTools → Console)
2. Check network tab (should see GET /api/v1/briefing-sessions/{id}/questions)
3. Verify backend is responding (curl http://localhost:8080/api/v1/actuator/health)

---

### Issue: "Complete" button disabled

**Symptom:** Can't click "Complete" button, it's grayed out

**Cause:** Not all required questions answered

**Fix:**
1. Go back to each question (use "Previous" button)
2. Ensure all have text (look for red asterisks = required)
3. Return to last question
4. Try "Complete" again

---

### Issue: 409 Conflict on Complete

**Symptom:** Error after clicking "Complete"

**Cause:** Session already completed

**Fix:**
1. Create a new proposal (Step 2)
2. Create a new briefing session (Step 3)
3. Repeat flow

---

## Security Tests (Optional)

### Test: Workspace Isolation

```bash
# 1. Create session as User A, get publicToken
# 2. Login as User B
# 3. Try to access User A's session:

curl -X GET "http://localhost:8080/api/v1/briefing-sessions/{SESSION_ID}" \
  -H "Authorization: Bearer {USER_B_JWT_TOKEN}"

# Expected: 403 Forbidden
# {
#   "error": "BriefingSession does not belong to the authenticated workspace"
# }
```

### Test: Rate Limiting (Public Endpoint)

```bash
# Hit the public endpoint many times rapidly
for i in {1..15}; do
  curl -w "Request $i: %{http_code}\n" -s -o /dev/null \
    "http://localhost:3000/briefing/invalid-token"
  sleep 0.05
done

# Expected:
# Request 1-10: 200 or 404 (OK)
# Request 11+: 429 Too Many Requests (throttled)
```

---

## Success Criteria

✅ **All tests pass if:**
1. BriefingSession created with valid publicToken
2. Questions loaded from backend
3. Frontend renders without errors
4. Answers submitted successfully
5. Session completed with score calculated
6. CompletionSummary displays correctly
7. Workspace isolation enforced (403 on cross-workspace)
8. Rate limiting active on public endpoints

🎉 **If all pass: STAGING VALIDATED, READY FOR PRODUCTION**

