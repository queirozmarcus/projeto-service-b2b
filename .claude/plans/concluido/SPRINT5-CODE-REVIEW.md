# Sprint 5 Code Review — Frontend Authentication

**Reviewer:** Senior Code Reviewer (Claude Haiku 4.5)
**Date:** March 25, 2026
**Branch:** feature/sprint-3-application-services
**Commits:** 11 commits (a4ba77d → 03bfbe1)
**Status:** 12/12 E2E Tests PASSING ✅

---

## Executive Summary

Sprint 5 delivers a production-grade authentication system with strong architectural decisions (D11-D15) and comprehensive E2E test coverage. The implementation demonstrates excellent separation of concerns between frontend (Zustand + Next.js middleware) and backend (Spring Security 6.x + JWT). However, **5 critical and 5 important issues must be addressed before merge to main**.

**Recommendation:** Ready for staging deployment after fixes.

---

## Quick Assessment

| Category | Score | Status |
|----------|-------|--------|
| Architecture (D11-D15) | 9/10 | ✅ Excellent |
| Security | 7/10 | ⚠️ Needs hardening |
| Code Quality | 8/10 | ✅ Good |
| Test Coverage | 6/10 | ⚠️ Gaps in backend |
| DevOps | 8/10 | ✅ Good |
| **Overall** | **7.6/10** | 🚀 Ready (with fixes) |

---

## Critical Issues — Must Fix Before Merge

### 1. Missing HTTPS Enforcement in SecurityConfig.java

**Location:** `backend/src/main/java/com/scopeflow/config/SecurityConfig.java`

**Issue:**
- No `.requiresChannel("https")` in SecurityFilterChain
- Secure cookie flag ignored if app runs on HTTP
- Production risk: XSS attacks can steal refresh token from non-HTTPS connections

**Fix:**
```java
.requiresChannel(channel -> channel
    .anyRequest()
    .requiresSecure()
)
```

**For Development:** Use environment variable to disable:
```yaml
app:
  requires-https: ${APP_REQUIRES_HTTPS:true}
```

---

### 2. No Login Rate Limiting

**Location:** `backend/src/main/java/com/scopeflow/adapter/in/web/auth/AuthControllerV2.java`

**Issue:**
- `/auth/login` and `/auth/register` endpoints unprotected against brute-force attacks
- Attacker can attempt unlimited login attempts
- OWASP Top 10: Broken Authentication

**Fix Options:**
- **Option A:** Spring AOP + `@RateLimiter` annotation
- **Option B:** Bucket4j integration (token bucket algorithm)
- **Option C:** Simple in-memory cache + exponential backoff

**Recommended:** Bucket4j (1 attempt per 5 seconds per IP)

---

### 3. Mutex Timeout Missing in api.ts

**Location:** `frontend/src/lib/api.ts` (lines 6-40)

**Issue:**
```typescript
let refreshTokenPromise: Promise<string> | null = null;

if (!refreshTokenPromise) {
  refreshTokenPromise = api.post('/auth/refresh')
    // ... no timeout!
    .finally(() => { refreshTokenPromise = null; });
}
const newToken = await refreshTokenPromise;
```

- If `/auth/refresh` hangs, all subsequent requests hang indefinitely
- Browser becomes unresponsive
- No user feedback on hang

**Fix:**
```typescript
if (!refreshTokenPromise) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 5000); // 5-second timeout

  refreshTokenPromise = api
    .post<{ accessToken: string; expiresIn: number }>('/auth/refresh', {}, { signal: controller.signal })
    .then((res) => {
      const { accessToken: newToken } = res.data;
      useSessionStore.getState().setSession(newToken, useSessionStore.getState().user);
      return newToken;
    })
    .catch((err) => {
      useSessionStore.getState().clearSession();
      if (typeof window !== 'undefined') {
        window.location.href = '/auth/login';
      }
      throw err;
    })
    .finally(() => {
      clearTimeout(timeout);
      refreshTokenPromise = null;
    });
}
```

---

### 4. Dead Code: User Destructuring from /auth/refresh Response

**Location:** `frontend/src/lib/api.ts` (lines 20-22)

**Issue:**
```typescript
interface RefreshResponse {
  accessToken: string;
  user: typeof user;  // ❌ Backend doesn't return user!
  expiresIn: number;
}

// Backend actually returns:
// { accessToken: string; expiresIn: number }
```

Backend `/auth/refresh` returns **only** `accessToken` and `expiresIn`, but the interface expects `user` (which doesn't exist).

**Fix:**
```typescript
interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
  // user removed — not returned by backend
}

// In interceptor:
.then((res) => {
  const { accessToken: newToken } = res.data;
  // User already in store; don't try to destructure it
  useSessionStore.getState().setSession(newToken, useSessionStore.getState().user);
  return newToken;
})
```

---

### 5. SessionProvider Race Condition: Direct Header Passing to /auth/me

**Location:** `frontend/src/components/auth/SessionProvider.tsx` (lines 39-41)

**Issue:**
```typescript
// ❌ This bypasses the request interceptor!
const me = await api.get<MeResponse>('/auth/me', {
  headers: { Authorization: `Bearer ${data.accessToken}` },
});
```

- Direct Authorization header bypasses request interceptor's proactive refresh
- If access token is about to expire, it won't be refreshed before /auth/me call
- Race condition: /auth/me returns 401, SessionProvider catches and clears session
- User briefly sees blank page before redirect to login

**Fix:**
```typescript
// ✅ Let the interceptor handle refresh
const me = await api.get<MeResponse>('/auth/me');
```

The request interceptor will proactively refresh the token if needed.

---

## Important Issues — Should Fix Before Production

### 6. JWT_SECRET Weak Validation

**Location:** `backend/src/main/java/com/scopeflow/config/JwtService.java`

**Issue:**
- Default: `"your-secret-key-change-in-production-minimum-32-characters-long"`
- If `JWT_SECRET` env var not set, weak default is used
- No validation of secret length in code

**Fix:**
```java
public JwtService(
    @Value("${jwt.secret}") String secret,
    @Value("${jwt.expiration:900000}") long accessTokenExpirationMs,
    @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs
) {
  if (secret == null || secret.length() < 32) {
    throw new IllegalArgumentException(
      "JWT_SECRET must be at least 32 characters (current: " +
      (secret != null ? secret.length() : 0) + ")"
    );
  }
  this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  this.accessTokenExpirationMs = accessTokenExpirationMs;
  this.refreshTokenExpirationMs = refreshTokenExpirationMs;
}
```

---

### 7. User Status Cache Stale Window (5 Minutes)

**Location:** `backend/src/main/java/com/scopeflow/config/UserStatusCacheService.java`

**Issue:**
- Cache TTL: 5 minutes
- User disabled at 09:00 can still use token until 09:05
- Problematic for compliance/security audits

**Trade-offs:**
| Option | Pro | Con |
|--------|-----|-----|
| Keep 5 min | Low DB load | Long stale window |
| Reduce to 1 min | Timely deactivation | 5x more DB queries |
| Reduce to 30s | Immediate response | 10x more DB queries |

**Recommendation:** Document the trade-off or reduce to 1-2 minutes if compliance is critical.

---

### 8. No Password Reset Endpoint

**Issue:**
- Users who forget password have no recovery
- No `/auth/reset-password` endpoint
- UX blocker for production

**Fix:** Implement before deployment:
```java
POST /auth/reset-password
{
  "email": "user@example.com"
}
// Returns: confirmation message
// Backend sends email with reset token (30-min TTL)

POST /auth/reset-password/{token}
{
  "newPassword": "..."
}
// Validates token, updates password
```

---

### 9. Backend Auth Test Gaps

**Location:** `backend/src/test/java/com/scopeflow/adapter/in/web/auth/AuthControllerV2Test.java`

**Current Tests:**
- ✅ Register returns 201 with tokens
- ✅ Register with weak password returns 400

**Missing Tests:**
- ❌ Login with valid credentials
- ❌ Login with invalid credentials
- ❌ Refresh token endpoint
- ❌ Logout endpoint
- ❌ /auth/me endpoint
- ❌ Duplicate email registration
- ❌ Token invalidation on logout

**Coverage Target:** 100% for auth paths (security-critical)

**Fix:** Add integration tests for complete flow:
```java
@Test
@DisplayName("POST /auth/login returns 200 with tokens when valid credentials")
void login_shouldReturn200_whenValidCredentials() throws Exception { ... }

@Test
@DisplayName("POST /auth/refresh returns 200 with new access token")
void refresh_shouldReturn200_whenValidRefreshToken() throws Exception { ... }

@Test
@DisplayName("POST /auth/logout clears refresh token cookie")
void logout_shouldClearCookie_always() throws Exception { ... }
```

---

### 10. Middleware Cookie Validation Gap

**Location:** `frontend/src/middleware.ts`

**Issue:**
```typescript
const isProtected = PROTECTED_PREFIXES.some(p => pathname.startsWith(p));
if (isProtected && !refreshToken) {
  return NextResponse.redirect(new URL('/auth/login', request.url));
}
```

- Middleware only checks cookie **existence**, not **validity**
- Expired token still passes middleware
- SessionProvider detects expiry and redirects → flash of unprotected content

**Design Trade-off:** This is acceptable if dashboard layout shows loading state during SessionProvider bootstrap.

**Optional Fix:** Add explicit layout guard in RootLayout:
```typescript
export default function RootLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useSessionStore();

  if (!isAuthenticated) {
    return <LoadingShimmer />;
  }

  return children;
}
```

---

## Approved Architectural Decisions

### D11: JWT with httpOnly Cookies ✅

- Access token: 15-min TTL, in-memory storage, response body
- Refresh token: 7-day TTL, httpOnly + Secure + SameSite=Lax, Set-Cookie
- **Strength:** XSS-proof refresh token, CSRF-protected

### D12: Zustand Store for Auth State ✅

- Minimal state: `accessToken`, `user`, `isAuthenticated`, `isLoading`, `error`
- No localStorage (token leakage risk)
- **Strength:** Simple, testable, secure

### D13: Mutex for Race Condition Prevention ✅

- `refreshTokenPromise` deduplicates concurrent refresh calls
- **Caveat:** Needs timeout (see Critical Issue #3)

### D14: Next.js Middleware + Route Protection ✅

- Server-side protection before layout hydration
- Prevents flash of unprotected content
- Bidirectional redirects (auth ↔ protected)

### D15: BroadcastChannel Multi-Tab Logout Sync ✅

- Cross-tab state sync without backend
- Hard navigation ensures middleware re-evaluation
- Graceful degradation in unsupported browsers

---

## Testing Assessment

### E2E Tests (12/12 PASSING) ✅

**Coverage:**
1. ✅ Login with valid credentials → /dashboard
2. ✅ Register and auto-login
3. ✅ Silent refresh on cold start
4. ✅ Multi-tab logout sync via BroadcastChannel
5. ✅ Invalid credentials → error message
6. ✅ Protected route redirect when unauthenticated

**Strengths:**
- Comprehensive happy-path and error-path scenarios
- Multi-tab testing with BrowserContext
- Mocks backend to reduce dependencies

**Suggestions:**
- Add explicit test for access token expiry + proactive refresh
- Add explicit test for refresh token expiry → redirect to login
- Add CORS preflight OPTIONS test

### Backend Tests (⚠️ GAPS)

**Status:** Only 2 out of 7 auth endpoints tested
- Register endpoint: ✅ 2 tests
- Login endpoint: ❌ 0 tests
- Refresh endpoint: ❌ 0 tests
- Logout endpoint: ❌ 0 tests
- /auth/me endpoint: ❌ 0 tests
- JwtAuthenticationFilter: ❌ 0 tests
- SecurityConfig: ❌ 0 tests

**Target:** 100% coverage for auth paths (security-critical)

---

## Security Hardening Summary

### Strong Points

✅ Token separation (memory + httpOnly cookie)
✅ CORS with credentials + explicit origins
✅ Stateless JWT with user status cache
✅ Password validation (uppercase + digit + special)
✅ Phone validation (E.164 format)
✅ SameSite=Lax CSRF protection

### Gaps

❌ No HTTPS enforcement
❌ No login rate limiting
❌ No password reset
❌ No audit logging
❌ Cache 5-min stale window
❌ No IP pinning for refresh tokens

---

## Merge Checklist

### Before Merge to Main

- [ ] Fix #1: Add HTTPS enforcement (SecurityConfig)
- [ ] Fix #2: Add login rate limiting (Spring AOP or Bucket4j)
- [ ] Fix #3: Add mutex timeout in api.ts (AbortController)
- [ ] Fix #4: Remove dead code from refresh response
- [ ] Fix #5: Fix SessionProvider /auth/me race condition
- [ ] Fix #6: Add JWT_SECRET validation (JwtService)
- [ ] Fix #9: Add backend auth tests (100% coverage target)
- [ ] Optional: Implement password reset flow
- [ ] Optional: Add audit logging

### Before Production Deployment

- [ ] All Critical and Important issues resolved
- [ ] Backend test coverage ≥ 95%
- [ ] Security audit (OWASP Top 10 scan)
- [ ] Load testing (JWT validation + cache performance)
- [ ] Staging deployment with E2E smoke tests
- [ ] Document rate limiting, token expiry, cache behavior

---

## Files Reviewed

### Backend (Java)

| File | Lines | Status |
|------|-------|--------|
| AuthControllerV2.java | 240 | ✅ Good |
| SecurityConfig.java | 114 | ⚠️ HTTPS missing |
| JwtAuthenticationFilter.java | 102 | ✅ Good |
| JwtService.java | 127 | ⚠️ No validation |
| SecurityUtil.java | 62 | ✅ Good |
| UserStatusCacheService.java | 52 | ✅ Good |
| AuthControllerV2Test.java | 100+ | ⚠️ Gaps |

### Frontend (TypeScript/React)

| File | Lines | Status |
|------|-------|--------|
| api.ts | 73 | ⚠️ Timeout missing |
| SessionProvider.tsx | 75 | ⚠️ Race condition |
| useAuth.ts | 98 | ✅ Good |
| middleware.ts | 27 | ✅ Good |
| broadcast.ts | 35 | ✅ Good |
| useSession.ts | 22 | ✅ Good |
| auth.spec.ts | 356 | ✅ Good |

---

## Recommendations for Sprint 6

1. **Add Rate Limiting** → 1 attempt per 5 seconds per IP
2. **Add Password Reset** → Email-based token (30-min TTL)
3. **Add Audit Logging** → Login/logout/refresh with IP & User-Agent
4. **Add Device Management** → List active sessions, revoke per device
5. **Add 2FA (Optional)** → TOTP-based second factor

---

## Final Verdict

**Overall Score: 7.6 / 10**

**Status: ✅ APPROVED WITH MANDATORY FIXES**

This sprint demonstrates strong architectural understanding of JWT token management, server-side protection, and cross-browser synchronization. The E2E test coverage is exemplary. However, 5 critical and 5 important issues must be resolved before merge.

**Timeline:**
- Fixes: 2-3 hours
- Testing: 1 hour
- Review: 1 hour
- **Total: 4-5 hours to production-ready**

**Next Step:** Assign fixes to engineering and schedule follow-up review before merge.

---

## Sign-Off

**Reviewer:** Claude Haiku 4.5 (Senior Code Reviewer)
**Review Date:** March 25, 2026
**Reviewed Files:** 13 (7 Java, 6 TypeScript)
**Total Lines Analyzed:** ~1,250

**Approved Paths:**
- ✅ Architecture decisions (D11-D15)
- ✅ E2E test coverage
- ✅ Frontend state management
- ✅ JWT token model
- ⚠️ Security hardening (needs fixes)
- ⚠️ Backend test coverage (needs expansion)

