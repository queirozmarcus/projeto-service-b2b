# Contract Testing — Documentation Hub

## Quick Links

| Document | Purpose |
|----------|---------|
| [**CONTRACT-TESTING-GUIDE.md**](CONTRACT-TESTING-GUIDE.md) | Main guide: architecture, how-to, troubleshooting |
| [**user-service/CONTRACT-TESTS.md**](../../../user-service/CONTRACT-TESTS.md) | Provider contracts (8 YAML files) |
| [**backend/CONTRACT-TESTS.md**](../../../backend/CONTRACT-TESTS.md) | Consumer tests (10 scenarios) |
| [**validate-contracts.sh**](../../../scripts/validate-contracts.sh) | CI validation script |

## TL;DR — Quick Start

### Run All Contract Tests

```bash
./scripts/validate-contracts.sh
```

**Expected output:**
```
✅ JWT secrets validated
✅ Provider tests passed (8 contracts)
✅ Stubs published to Maven local
✅ Consumer tests passed (10 scenarios)

Contracts are compatible! Safe to deploy.
```

### Manual Execution

```bash
# Provider (user-service)
cd user-service
./mvnw spring-cloud-contract:generateTests test -Dtest=ContractVerifier*
./mvnw clean install -DskipTests

# Consumer (monolith)
cd backend
./mvnw test -Dtest=UserServiceContractTest
```

## Implemented Contracts

### Auth Endpoints

| Endpoint | Success | Error | Status |
|----------|---------|-------|--------|
| `POST /api/v1/auth/login` | ✅ 200 | ✅ 401 (invalid credentials) | DONE |
| `GET /api/v1/auth/me` | ✅ 200 | ✅ 401 (unauthorized) | DONE |

### User Endpoints

| Endpoint | Success | Error | Status |
|----------|---------|-------|--------|
| `GET /api/v1/users/by-email/{email}` | ✅ 200 | ✅ 404 (not found) | DONE |
| `POST /api/v1/users/invited` | ✅ 201 | ✅ 409 (duplicate email) | DONE |

**Total:** 8 contracts = 4 endpoints × 2 scenarios (happy + error)

## Key Features

- ✅ **Provider contracts**: YAML format, auto-generated tests
- ✅ **Consumer tests**: WireMock stubs, no external dependencies
- ✅ **JWT validation**: Shared secret compatibility verified
- ✅ **RFC 9457**: Problem Details format validated
- ✅ **CI ready**: Script for automated validation
- ✅ **Backward compatibility**: Breaking changes blocked

## CI Integration

### GitHub Actions Workflow

```yaml
name: Contract Tests
on: [push, pull_request]

jobs:
  validate-contracts:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: 21
      - run: ./scripts/validate-contracts.sh
```

### Fail-Fast Behavior

```
Provider tests fail → ❌ block merge
Consumer tests fail → ❌ block deploy
JWT secret mismatch → ❌ block deploy
```

## Troubleshooting Quick Reference

| Error | Cause | Fix |
|-------|-------|-----|
| `Stub not found` | Stubs JAR missing | Run `./mvnw install` in user-service |
| `404 Not Found` | Path mismatch | Check controller `@RequestMapping` |
| `JWT signature error` | Secret mismatch | Sync `JWT_SECRET` in both services |
| `WireMock port in use` | Leftover process | `./mvnw clean test` |

## Roadmap

- ✅ **Phase 1:** Auth & User contracts (DONE)
- ⏳ **Phase 2:** Workspace extraction contracts
- ⏳ **Phase 3:** Kafka event contracts (Pact CDC)
- ⏳ **Phase 4:** Pact Broker migration (when > 3 services)

## Support

- **Questions:** See [CONTRACT-TESTING-GUIDE.md](CONTRACT-TESTING-GUIDE.md)
- **Provider docs:** [user-service/CONTRACT-TESTS.md](../../../user-service/CONTRACT-TESTS.md)
- **Consumer docs:** [backend/CONTRACT-TESTS.md](../../../backend/CONTRACT-TESTS.md)
