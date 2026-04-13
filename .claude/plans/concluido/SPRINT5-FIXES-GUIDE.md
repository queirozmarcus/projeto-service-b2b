# Sprint 5: Fixes Implementation Guide

**Objective:** Resolve 5 critical and 5 important issues identified in code review

**Time Estimate:** 4-5 hours total

---

## Fix #1: Add HTTPS Enforcement

**File:** `backend/src/main/java/com/scopeflow/config/SecurityConfig.java`

**Current Code (Line 50-76):**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session ->
      session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
      // ... permitAll rules
      .anyRequest().authenticated()
    )
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

  return http.build();
}
```

**Fixed Code:**
```java
@Configuration
@EnableWebSecurity
@EnableCaching
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtFilter;
  private final List<String> allowedOrigins;
  private final boolean requiresHttps;  // Add this field

  public SecurityConfig(
    JwtAuthenticationFilter jwtFilter,
    @Value("${cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins,
    @Value("${app.requires-https:true}") boolean requiresHttps  // Add this parameter
  ) {
    this.jwtFilter = jwtFilter;
    this.allowedOrigins = allowedOrigins;
    this.requiresHttps = requiresHttps;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      // Add HTTPS enforcement
      .requiresChannel(channel -> {
        if (requiresHttps) {
          channel.anyRequest().requiresSecure();
        }
      })
      .authorizeHttpRequests(auth -> auth
        // ... existing permitAll rules ...
        .anyRequest().authenticated()
      )
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // ... rest of the class unchanged
}
```

**Environment Variable Configuration:**

Add to `application.yml`:
```yaml
app:
  cookie:
    secure: ${APP_COOKIE_SECURE:true}
  requires-https: ${APP_REQUIRES_HTTPS:true}
```

For development (set in `.env`):
```env
APP_REQUIRES_HTTPS=false
APP_COOKIE_SECURE=false
```

For production (set in deployment):
```env
APP_REQUIRES_HTTPS=true
APP_COOKIE_SECURE=true
```

---

## Fix #2: Add Login Rate Limiting

**File:** `backend/src/main/java/com/scopeflow/config/RateLimiterConfig.java` (NEW FILE)

**Step 1: Add Bucket4j Dependency**

Add to `pom.xml`:
```xml
<dependency>
  <groupId>com.github.vladimir-bukhtoyarov</groupId>
  <artifactId>bucket4j-core</artifactId>
  <version>7.6.0</version>
</dependency>
```

**Step 2: Create Rate Limiter Config**

```java
package com.scopeflow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimiterConfig {

  /**
   * Rate limiter: 5 login attempts per 5 minutes per IP.
   * Uses in-memory cache with Caffeine for automatic cleanup.
   */
  @Bean
  public Map<String, Bucket> loginRateLimiters() {
    return Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(10))
      .build()
      .asMap();
  }

  public static Bucket createNewBucket() {
    Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(5)));
    return Bucket4j.builder()
      .addLimit(limit)
      .build();
  }
}
```

**Step 3: Create Rate Limiter Aspect**

```java
package com.scopeflow.adapter.in.web.auth;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Aspect
@Component
public class RateLimitAspect {

  private final Map<String, Bucket> buckets;

  public RateLimitAspect(Map<String, Bucket> buckets) {
    this.buckets = buckets;
  }

  @Around("@annotation(com.scopeflow.adapter.in.web.auth.RateLimit)")
  public Object enforceRateLimit(ProceedingJoinPoint pjp) throws Throwable {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
      .getRequestAttributes())
      .getRequest();

    String clientIp = getClientIp(request);
    String bucketKey = clientIp; // Can be refined to "IP:endpoint"

    Bucket bucket = buckets.computeIfAbsent(
      bucketKey,
      k -> RateLimiterConfig.createNewBucket()
    );

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return pjp.proceed();
    }

    long waitForRefill = (probe.getRoundedSecondsToWait());
    throw new ResponseStatusException(
      HttpStatus.TOO_MANY_REQUESTS,
      "Too many login attempts. Please try again in " + waitForRefill + " seconds."
    );
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
```

**Step 4: Add Rate Limit Annotation**

```java
package com.scopeflow.adapter.in.web.auth;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
  // Marker annotation for rate-limited endpoints
}
```

**Step 5: Annotate Controller Methods**

```java
@PostMapping("/login")
@RateLimit  // Add this
@Operation(summary = "Authenticate and obtain tokens")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
  // ... existing implementation
}

@PostMapping("/register")
@RateLimit  // Add this
@Operation(summary = "Register new user account")
public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
  // ... existing implementation
}
```

---

## Fix #3: Add Mutex Timeout in api.ts

**File:** `frontend/src/lib/api.ts`

**Current Code (Lines 6-40):**
```typescript
let refreshTokenPromise: Promise<string> | null = null;

const api = axios.create({
  baseURL: env.apiUrl,
  withCredentials: true,
});

api.interceptors.request.use(async (config) => {
  const { accessToken, user } = useSessionStore.getState();

  if (accessToken && shouldRefreshToken(accessToken)) {
    if (!refreshTokenPromise) {
      refreshTokenPromise = api
        .post<{ accessToken: string; user: typeof user }>('/auth/refresh')
        .then((res) => {
          const { accessToken: newToken, user: refreshedUser } = res.data;
          if (refreshedUser) {
            useSessionStore.getState().setSession(newToken, refreshedUser);
          }
          return newToken;
        })
        .catch((err: unknown) => {
          useSessionStore.getState().clearSession();
          if (typeof window !== 'undefined') {
            window.location.href = '/auth/login';
          }
          throw err;
        })
        .finally(() => {
          refreshTokenPromise = null;
        });
    }

    const newToken = await refreshTokenPromise;
    config.headers.Authorization = `Bearer ${newToken}`;
    return config;
  }
  // ... rest of interceptor
});
```

**Fixed Code:**
```typescript
let refreshTokenPromise: Promise<string> | null = null;

// Helper function to create a promise with timeout
function withTimeout<T>(promise: Promise<T>, timeoutMs: number): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((_, reject) =>
      setTimeout(() => reject(new Error('Request timeout')), timeoutMs)
    ),
  ]);
}

const api = axios.create({
  baseURL: env.apiUrl,
  withCredentials: true,
});

api.interceptors.request.use(async (config) => {
  const { accessToken, user } = useSessionStore.getState();

  if (accessToken && shouldRefreshToken(accessToken)) {
    if (!refreshTokenPromise) {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5-second timeout

      refreshTokenPromise = api
        .post<{ accessToken: string; expiresIn: number }>(
          '/auth/refresh',
          {},
          { signal: controller.signal }
        )
        .then((res) => {
          const { accessToken: newToken } = res.data;
          // Keep existing user in store; don't expect user in refresh response
          useSessionStore.getState().setSession(newToken, useSessionStore.getState().user);
          return newToken;
        })
        .catch((err: unknown) => {
          useSessionStore.getState().clearSession();
          if (typeof window !== 'undefined') {
            window.location.href = '/auth/login';
          }
          throw err;
        })
        .finally(() => {
          clearTimeout(timeoutId);
          refreshTokenPromise = null;
        });
    }

    try {
      const newToken = await refreshTokenPromise;
      config.headers.Authorization = `Bearer ${newToken}`;
      return config;
    } catch (err) {
      // Refresh failed; request will proceed without auth and likely 401
      return config;
    }
  }

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

// ... rest of api.ts unchanged
```

---

## Fix #4: Remove Dead Code from Refresh Response

**File:** `frontend/src/lib/api.ts` (Lines 8-25)

**Current Code:**
```typescript
// Resposta flat do backend: access token + campos do usuário direto no body.
// O refresh token é entregue via httpOnly cookie (Set-Cookie) — nunca no body.
interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  userId: string;
  email: string;
  fullName: string;
  workspaceId?: string;
}

// /auth/refresh retorna apenas o novo accessToken (sem dados do usuário)
interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
  user: typeof user; // ❌ DEAD CODE: Backend doesn't return user
}
```

**Fixed Code:**
```typescript
// Resposta do login/register: access token + user data.
// O refresh token é entregue via httpOnly cookie (Set-Cookie) — nunca no body.
interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  userId: string;
  email: string;
  fullName: string;
  workspaceId?: string;
}

// /auth/refresh retorna apenas o novo accessToken
interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
  // user removed: backend doesn't include it in refresh response
}
```

---

## Fix #5: Fix SessionProvider /auth/me Race Condition

**File:** `frontend/src/components/auth/SessionProvider.tsx` (Lines 30-50)

**Current Code:**
```typescript
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const { setSession, clearSession } = useSessionStore();

  useEffect(() => {
    api
      .post<RefreshResponse>('/auth/refresh')
      .then(async ({ data }) => {
        // ❌ RACE CONDITION: Bypass interceptor with explicit header
        const me = await api.get<MeResponse>('/auth/me', {
          headers: { Authorization: `Bearer ${data.accessToken}` },
        });
        const user: User = {
          id: me.data.id,
          email: me.data.email,
          fullName: me.data.fullName,
          role: 'owner',
          workspaceId: me.data.workspaceId ?? '',
        };
        setSession(data.accessToken, user);
      })
      .catch(() => {
        clearSession();
      });
    // ...
  }, [setSession, clearSession]);

  return <>{children}</>;
}
```

**Fixed Code:**
```typescript
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const { setSession, clearSession } = useSessionStore();

  useEffect(() => {
    api
      .post<RefreshResponse>('/auth/refresh')
      .then(async ({ data }) => {
        // ✅ Let the interceptor handle token refresh automatically
        const me = await api.get<MeResponse>('/auth/me');

        const user: User = {
          id: me.data.id,
          email: me.data.email,
          fullName: me.data.fullName,
          role: 'owner',
          workspaceId: me.data.workspaceId ?? '',
        };
        setSession(data.accessToken, user);
      })
      .catch(() => {
        clearSession();
      });

    // Sincronizar logout entre abas via BroadcastChannel:
    // limpa estado local e faz hard navigation para que o middleware
    // veja o cookie já removido pelo logout da aba originária.
    authBroadcaster.onMessage((message) => {
      if (message.type === 'logout') {
        clearSession();
        if (typeof window !== 'undefined') {
          window.location.href = '/auth/login';
        }
      }
    });

    return () => {
      authBroadcaster.close();
    };
  }, [setSession, clearSession]);

  return <>{children}</>;
}
```

---

## Fix #6: Add JWT_SECRET Validation

**File:** `backend/src/main/java/com/scopeflow/config/JwtService.java`

**Current Code (Lines 33-41):**
```java
public JwtService(
    @Value("${jwt.secret}") String secret,
    @Value("${jwt.expiration:900000}") long accessTokenExpirationMs,
    @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs
) {
  this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  this.accessTokenExpirationMs = accessTokenExpirationMs;
  this.refreshTokenExpirationMs = refreshTokenExpirationMs;
}
```

**Fixed Code:**
```java
public JwtService(
    @Value("${jwt.secret}") String secret,
    @Value("${jwt.expiration:900000}") long accessTokenExpirationMs,
    @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs
) {
  validateSecretKey(secret);

  this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  this.accessTokenExpirationMs = accessTokenExpirationMs;
  this.refreshTokenExpirationMs = refreshTokenExpirationMs;

  log.info("JWT Service initialized: accessToken TTL={}ms, refreshToken TTL={}ms",
    accessTokenExpirationMs, refreshTokenExpirationMs);
}

private void validateSecretKey(String secret) {
  if (secret == null || secret.isEmpty()) {
    throw new IllegalArgumentException(
      "JWT_SECRET environment variable is not set. " +
      "Set it to a random 32+ character string: " +
      "export JWT_SECRET=$(openssl rand -base64 32)"
    );
  }

  if (secret.length() < 32) {
    throw new IllegalArgumentException(
      "JWT_SECRET must be at least 32 characters long (for HMAC-SHA256 security). " +
      "Current length: " + secret.length() + " characters. " +
      "Generate a new key: openssl rand -base64 32"
    );
  }

  // Warn if using the default placeholder
  if (secret.contains("your-secret-key") || secret.contains("change-in-production")) {
    log.warn("JWT_SECRET appears to be a placeholder. Use a random key in production.");
  }
}
```

---

## Fix #7: Reduce Cache TTL (Optional)

**File:** `backend/src/main/resources/application.yml` (Lines 65-70)

**Current Code:**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      # userStatus cache: TTL 5min, max 10.000 entries
      spec: maximumSize=10000,expireAfterWrite=300s
```

**Fixed Code (Option A: Reduce to 1 minute):**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      # userStatus cache: TTL 1min, max 10.000 entries
      # Trade-off: quicker deactivation vs. more DB queries
      spec: maximumSize=10000,expireAfterWrite=60s
```

**Add Documentation Comment:**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      # userStatus cache configuration:
      # - TTL: 60 seconds (window before deactivated user is rejected)
      # - Max entries: 10,000 (one per active user)
      # - Trade-off: shorter TTL = timely deactivation, but more DB load
      # For compliance scenarios needing immediate response, this is acceptable.
      # For high-volume (10k+ concurrent users), consider external cache (Redis).
      spec: maximumSize=10000,expireAfterWrite=60s
```

---

## Fix #8: Add Backend Auth Tests

**File:** `backend/src/test/java/com/scopeflow/adapter/in/web/auth/AuthControllerV2Test.java`

**Add These Test Cases:**

```java
@Test
@DisplayName("POST /auth/login returns 200 with tokens when valid credentials")
void login_shouldReturn200_whenValidCredentials() throws Exception {
  // Given
  UserActive mockUser = new UserActive(
    UserId.generate(),
    new Email("user@example.com"),
    new PasswordHash(BCRYPT_HASH),
    "Test User", "+5511999999999",
    Instant.now(), Instant.now()
  );
  given(userService.getUserByEmail(any())).willReturn(Optional.of(mockUser));
  given(passwordEncoder.matches("Password1!", BCRYPT_HASH)).willReturn(true);
  given(jwtService.generateAccessToken(any(), any(), any(), any())).willReturn("access-token");
  given(jwtService.generateRefreshToken(any())).willReturn("refresh-token");
  given(jwtService.getAccessTokenExpirationMs()).willReturn(900000L);
  given(jwtService.getRefreshTokenExpirationMs()).willReturn(604800000L);

  LoginRequest request = new LoginRequest("user@example.com", "Password1!");

  // When / Then
  mockMvc.perform(post("/auth/login")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.accessToken").value("access-token"))
    .andExpect(header().exists("Set-Cookie"))
    .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
}

@Test
@DisplayName("POST /auth/login returns 401 when invalid credentials")
void login_shouldReturn401_whenInvalidPassword() throws Exception {
  // Given
  given(userService.getUserByEmail(any())).willReturn(Optional.empty());

  LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

  // When / Then
  mockMvc.perform(post("/auth/login")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isUnauthorized())
    .andExpect(jsonPath("$.detail").exists());
}

@Test
@DisplayName("POST /auth/refresh returns 200 with new access token")
void refresh_shouldReturn200_whenValidRefreshToken() throws Exception {
  // Given
  String validRefreshToken = "valid-refresh-token";
  UUID userId = UUID.randomUUID();

  given(jwtService.isRefreshToken(validRefreshToken)).willReturn(true);
  given(jwtService.extractUserId(validRefreshToken)).willReturn(userId);

  UserActive mockUser = new UserActive(
    new UserId(userId),
    new Email("user@example.com"),
    new PasswordHash(BCRYPT_HASH),
    "Test User", "+5511999999999",
    Instant.now(), Instant.now()
  );
  given(userService.getUserById(any())).willReturn(Optional.of(mockUser));
  given(jwtService.generateAccessToken(any(), any(), any(), any())).willReturn("new-access-token");
  given(jwtService.getAccessTokenExpirationMs()).willReturn(900000L);

  // When / Then
  mockMvc.perform(post("/auth/refresh")
    .cookie(new Cookie("refreshToken", validRefreshToken)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
    .andExpect(jsonPath("$.expiresIn").value(900));
}

@Test
@DisplayName("POST /auth/logout clears refresh token cookie")
void logout_shouldClearCookie_always() throws Exception {
  // When / Then
  mockMvc.perform(post("/auth/logout"))
    .andExpect(status().isNoContent())
    .andExpect(header().exists("Set-Cookie"))
    .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
    .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
}
```

---

## Testing Checklist

After implementing all fixes:

```bash
# Backend
cd backend

# Compile
./mvnw clean compile

# Run all tests (including new ones)
./mvnw test

# Check coverage
./mvnw test jacoco:report
# Coverage report: target/site/jacoco/index.html

# Frontend
cd ../frontend

# Run E2E tests
npm run test:e2e

# All 12 tests should PASS
```

---

## Verification Commands

```bash
# Verify JWT_SECRET validation works
export JWT_SECRET="short"  # Less than 32 chars
./mvnw spring-boot:run
# Should fail with: "JWT_SECRET must be at least 32 characters"

# Verify rate limiting
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}' \
  -i

# First 5 requests: 401 (invalid credentials)
# 6th request: 429 (too many requests)

# Verify HTTPS enforcement (if APP_REQUIRES_HTTPS=true)
curl -X GET http://localhost:8080/api/v1/proposals \
  -H "Authorization: Bearer fake-token"
# Should redirect to HTTPS or return 301 Moved Permanently
```

---

## Estimated Time per Fix

| Fix # | Issue | Time | Difficulty |
|-------|-------|------|-----------|
| 1 | HTTPS Enforcement | 15 min | Easy |
| 2 | Rate Limiting | 45 min | Medium |
| 3 | Mutex Timeout | 20 min | Easy |
| 4 | Dead Code Removal | 5 min | Trivial |
| 5 | SessionProvider Race | 10 min | Easy |
| 6 | JWT Validation | 15 min | Easy |
| 7 | Cache TTL Reduction | 5 min | Trivial |
| 8 | Backend Tests | 90 min | Medium |
| **Total** | | **205 min** | |

**Total Time: 3.5-4 hours** (with testing and verification)

---

## Merge Ready Checklist

After completing all fixes:

- [ ] Fix #1: HTTPS enforcement added
- [ ] Fix #2: Rate limiting implemented
- [ ] Fix #3: Mutex timeout added
- [ ] Fix #4: Dead code removed
- [ ] Fix #5: SessionProvider race fixed
- [ ] Fix #6: JWT validation added
- [ ] All tests passing locally
- [ ] E2E tests: 12/12 PASS
- [ ] Backend test coverage ≥ 95%
- [ ] Code review: APPROVED
- [ ] Ready for staging deployment

