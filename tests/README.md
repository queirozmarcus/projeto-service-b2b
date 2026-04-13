# Tests Documentation

This directory contains all test suites for the ScopeFlow AI project.

---

## Directory Structure

```
tests/
└── e2e/                              # End-to-end smoke tests
    ├── auth-flow.test.sh             # Core E2E test script (7 scenarios)
    ├── validate-traefik-routing.sh   # Traefik routing validator
    ├── README.md                     # Full E2E test guide
    ├── EXAMPLE-OUTPUT.md             # Example test outputs
    ├── TESTING-STRATEGY.md           # Strangler Fig migration strategy
    └── VALIDATION-CHECKLIST.md       # Pre/post-deployment checklist
```

---

## Test Suites

### 1. E2E Smoke Tests (`e2e/`)

**Purpose:** Validate authentication flow in both monolith and user-service modes (Strangler Fig migration).

**Coverage:**
- 4 happy path scenarios (register, login, profile, cross-service JWT)
- 3 error path scenarios (invalid credentials, expired JWT, malformed JWT)

**Run tests:**
```bash
# All tests (both modes)
./run-e2e-tests.sh

# Monolith only
./run-e2e-tests.sh --monolith

# User-service only
./run-e2e-tests.sh --user-service
```

**Documentation:** [`e2e/README.md`](e2e/README.md)

---

### 2. Unit Tests (`backend/src/test/`)

**Purpose:** Test business logic in isolation (no Spring, no database).

**Coverage:**
- Domain entities (User, Workspace, BriefingSession)
- Value objects (Email, PasswordHash, PublicToken)
- Business rules validation

**Run tests:**
```bash
cd backend
./mvnw test
```

---

### 3. Integration Tests (`backend/src/test/`)

**Purpose:** Test with real database (Testcontainers) and Spring context.

**Coverage:**
- Repository adapters (JPA)
- REST controllers (MockMvc)
- Database migrations (Flyway)

**Run tests:**
```bash
cd backend
./mvnw verify
```

---

### 4. Contract Tests (`user-service/src/test/contract/`)

**Purpose:** Validate API contracts between user-service and monolith.

**Coverage:**
- 8 contract scenarios
- JWT compatibility validation
- Schema validation (OpenAPI)

**Run tests:**
```bash
cd user-service
./mvnw test -Dtest="*ContractTest"
```

---

## Quick Start

### Local Development

```bash
# 1. Start infrastructure
docker compose up -d postgres redis rabbitmq

# 2. Run all test suites
./run-all-tests.sh  # (create this script if needed)

# Or run individually:
cd backend && ./mvnw verify           # Unit + Integration
./run-e2e-tests.sh                    # E2E Smoke Tests
```

---

### CI/CD

Tests run automatically on GitHub Actions:

```yaml
# .github/workflows/
├── backend-tests.yml          # Unit + Integration tests
└── e2e-smoke-tests.yml        # E2E tests (both modes)
```

**Status badges:**
[![Backend Tests](https://github.com/scopeflow/projeto-service-b2b/actions/workflows/backend-tests.yml/badge.svg)](https://github.com/scopeflow/projeto-service-b2b/actions/workflows/backend-tests.yml)
[![E2E Tests](https://github.com/scopeflow/projeto-service-b2b/actions/workflows/e2e-smoke-tests.yml/badge.svg)](https://github.com/scopeflow/projeto-service-b2b/actions/workflows/e2e-smoke-tests.yml)

---

## Test Pyramid

```
         /\
        /  \       E2E (7 scenarios)
       /────\      ← Smoke tests for critical flows
      /      \     Integration Tests (Testcontainers)
     /────────\    ← Database + REST API
    /          \   Unit Tests (JUnit 5)
   /____________\  ← Business logic + domain model
```

**Guidelines:**
- **Unit tests:** Fast, isolated, 100% coverage for business logic
- **Integration tests:** Real database, 80%+ coverage for adapters
- **E2E tests:** Slow, expensive, cover only critical user flows

---

## Coverage Targets

| Layer | Target | Tool |
|-------|--------|------|
| Domain logic | 100% | JaCoCo |
| Application services | 90%+ | JaCoCo |
| Adapters | 80%+ | JaCoCo |
| E2E flows | Critical flows only | Manual smoke tests |

**View coverage report:**
```bash
cd backend
./mvnw package jacoco:report
# Open: target/site/jacoco/index.html
```

---

## Adding New Tests

### Unit Test

```java
// backend/src/test/java/com/scopeflow/core/domain/user/UserTest.java
@Test
void shouldCreateActiveUser() {
    // Given
    Email email = new Email("test@example.com");
    PasswordHash hash = new PasswordHash("hashed");

    // When
    UserActive user = new UserActive(email, hash, "Test User", null);

    // Then
    assertThat(user.canLogin()).isTrue();
}
```

---

### Integration Test

```java
// backend/src/test/java/com/scopeflow/adapter/in/web/UserControllerIntegrationTest.java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {
    @Test
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",...}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").exists());
    }
}
```

---

### E2E Test

Add new scenario to `tests/e2e/auth-flow.test.sh`:

```bash
test_new_feature() {
  log_test 8 "Test new feature"

  http_request POST "$BASE_URL/api/v1/new-endpoint" "$payload" "Authorization: Bearer $JWT_TOKEN"

  local status=$(get_status)
  if [ "$status" != "200" ]; then
    log_error "Test failed"
    return 1
  fi

  log_success "New feature test passed"
}
```

Update `TESTS_TOTAL=8` and call `test_new_feature || true` in `main()`.

---

## Troubleshooting

### Tests fail with "Container startup failed"

**Cause:** Docker not running or insufficient resources.

**Fix:**
```bash
# Verify Docker
docker ps

# Increase memory (Docker Desktop → Settings → Resources)
# Minimum: 4GB RAM, 2 CPUs
```

---

### Tests fail with "Database migration error"

**Cause:** Flyway migration conflict.

**Fix:**
```bash
cd backend
./mvnw flyway:info    # Check migration status
./mvnw flyway:repair  # Fix checksum mismatches
./mvnw flyway:migrate # Apply pending migrations
```

---

### E2E tests fail with "JWT validation failed"

**Cause:** JWT_SECRET mismatch between services.

**Fix:**
```bash
# Sync secrets
grep JWT_SECRET .env
grep JWT_SECRET user-service/src/main/resources/application.yml

# If different, update and rebuild
docker compose build --no-cache
docker compose up -d
```

---

## References

- [E2E Testing Guide](e2e/README.md)
- [Testing Strategy](e2e/TESTING-STRATEGY.md)
- [Validation Checklist](e2e/VALIDATION-CHECKLIST.md)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [AssertJ Documentation](https://assertj.github.io/doc/)

---

## Contributing

When adding new features:

1. **Write tests first** (TDD)
2. **Maintain coverage targets** (check with `mvn jacoco:report`)
3. **Update documentation** (this README + test-specific docs)
4. **Run all tests locally** before pushing
5. **Ensure CI passes** on GitHub Actions

---

## Contact

Questions or issues?
1. Check troubleshooting sections above
2. Review test-specific documentation in each directory
3. Open GitHub issue with error logs
4. Slack: #scopeflow-engineering
