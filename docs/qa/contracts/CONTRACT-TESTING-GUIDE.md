# Contract Testing Guide — Strangler Fig Migration

## Visão Geral

Contract testing garante compatibilidade entre serviços durante a migração Strangler Fig (monólito → microsserviços).

**Framework:** Spring Cloud Contract 4.1.0

**Princípios:**
- **Consumer-driven:** consumer define expectativas, provider verifica
- **Backward compatibility:** mudanças devem ser retrocompatíveis
- **Fail fast:** falha de contrato bloqueia deploy no CI
- **Shared secret validation:** JWT do serviço extraído aceito pelo monólito

## Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                   Contract Testing Flow                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Provider (user-service):                             │
│     └─ Define contratos (YAML)                           │
│     └─ Run contract tests                                │
│     └─ Publish stubs JAR                                 │
│                                                          │
│  2. Consumer (monólito):                                 │
│     └─ Download stubs JAR                                │
│     └─ Run tests against WireMock                        │
│     └─ Validate JWT compatibility                        │
│                                                          │
│  3. CI Pipeline:                                         │
│     └─ Provider CI → publish stubs artifact              │
│     └─ Consumer CI → download stubs → run tests          │
│     └─ ❌ Fail → block deploy                            │
│     └─ ✅ Pass → deploy allowed                          │
└─────────────────────────────────────────────────────────┘
```

## Contratos Implementados

### User Service (Provider)

| Endpoint | Contratos | Status | Validação |
|----------|-----------|--------|-----------|
| `POST /api/v1/auth/login` | 2 | ✅ | Success (200) + Invalid credentials (401) |
| `GET /api/v1/auth/me` | 2 | ✅ | Success (200) + Unauthorized (401) |
| `GET /api/v1/users/by-email/{email}` | 2 | ✅ | Success (200) + Not found (404) |
| `POST /api/v1/users/invited` | 2 | ✅ | Success (201) + Duplicate email (409) |

**Total:** 8 contratos YAML → 8 testes provider + 10 testes consumer

## Estrutura de Arquivos

```
projeto-service-b2b/
├── user-service/                          # Provider
│   ├── src/test/resources/contracts/      # Contratos YAML
│   │   ├── auth/
│   │   │   ├── login-success.yml
│   │   │   ├── login-invalid-credentials.yml
│   │   │   ├── get-user-me-success.yml
│   │   │   └── get-user-me-unauthorized.yml
│   │   └── users/
│   │       ├── get-user-by-email-success.yml
│   │       ├── get-user-by-email-not-found.yml
│   │       ├── create-invited-user-success.yml
│   │       └── create-invited-user-duplicate.yml
│   ├── src/test/java/com/scopeflow/user/contract/
│   │   ├── ContractVerifierBase.java      # Base class com mocks
│   │   └── ContractVerifierSecurityConfig.java
│   └── pom.xml                            # spring-cloud-contract-maven-plugin
│
├── backend/                               # Consumer
│   ├── src/test/java/com/scopeflow/contract/
│   │   └── UserServiceContractTest.java   # Consumer tests com stubs
│   └── pom.xml                            # spring-cloud-starter-contract-stub-runner
│
├── docs/qa/
│   ├── CONTRACT-TESTS-USER-SERVICE.md     # Docs do provider
│   └── CONTRACT-TESTS-MONOLITH.md        # Docs do consumer
│
├── scripts/
│   └── validate-contracts.sh              # Script de validação CI
│
└── docs/qa/contracts/
    └── CONTRACT-TESTING-GUIDE.md          # Este documento
```

## Formato de Contrato (YAML)

### Exemplo: Login bem-sucedido

```yaml
description: User login with valid credentials
name: shouldLoginSuccessfully
request:
  method: POST
  url: /api/v1/auth/login
  headers:
    Content-Type: application/json
  body:
    email: "test@example.com"
    password: "ValidPassword123!"
  matchers:
    body:
      - path: $.email
        type: by_regex
        value: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    accessToken: "eyJhbGc..."
    userId: "550e8400-e29b-41d4-a716-446655440000"
    email: "test@example.com"
    fullName: "Test User"
  matchers:
    body:
      - path: $.accessToken
        type: by_regex
        value: "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"
      - path: $.userId
        type: by_regex
        value: "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
```

**Matchers:**
- `by_regex`: valida formato (UUID, JWT, ISO date)
- `by_type`: valida tipo (string, number, boolean)
- `by_equality`: valida valor exato

### Exemplo: Erro RFC 9457

```yaml
description: User login with invalid credentials
name: shouldReturn401OnInvalidCredentials
request:
  method: POST
  url: /api/v1/auth/login
  body:
    email: "test@example.com"
    password: "WrongPassword"
response:
  status: 401
  headers:
    Content-Type: application/problem+json
  body:
    type: "https://api.scopeflow.com/errors/invalid-credentials"
    title: "Invalid Credentials"
    status: 401
    detail: "Invalid email or password"
    error_code: "AUTH-401"
    error_id: "550e8400-e29b-41d4-a716-446655440000"
    timestamp: "2025-01-15T10:30:00Z"
  matchers:
    body:
      - path: $.error_code
        type: by_equality
      - path: $.status
        type: by_equality
      - path: $.error_id
        type: by_regex
        value: "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
```

## Executar Localmente

### Opção 1: Script automatizado

```bash
./scripts/validate-contracts.sh
```

**Executa:**
1. Valida JWT secrets match
2. Provider tests (user-service)
3. Publica stubs
4. Consumer tests (monólito)

### Opção 2: Manual

```bash
# 1. Provider: gerar e testar contratos
cd user-service
./mvnw spring-cloud-contract:generateTests test -Dtest=ContractVerifier*

# 2. Publicar stubs
./mvnw clean install -DskipTests

# 3. Consumer: rodar testes contra stubs
cd ../backend
./mvnw test -Dtest=UserServiceContractTest
```

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Contract Tests

on: [push, pull_request]

jobs:
  provider-tests:
    name: Provider Contract Tests (user-service)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java 21
        uses: actions/setup-java@v3
        with:
          java-version: 21

      - name: Run provider contract tests
        run: |
          cd user-service
          ./mvnw spring-cloud-contract:generateTests test -Dtest=ContractVerifier*

      - name: Publish stubs
        run: |
          cd user-service
          ./mvnw clean install -DskipTests

      - name: Upload stubs artifact
        uses: actions/upload-artifact@v3
        with:
          name: user-service-stubs
          path: ~/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/*.jar

  consumer-tests:
    name: Consumer Contract Tests (monolith)
    runs-on: ubuntu-latest
    needs: provider-tests
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java 21
        uses: actions/setup-java@v3
        with:
          java-version: 21

      - name: Download stubs
        uses: actions/download-artifact@v3
        with:
          name: user-service-stubs
          path: ~/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/

      - name: Run consumer contract tests
        run: |
          cd backend
          ./mvnw test -Dtest=UserServiceContractTest

      - name: Fail on contract mismatch
        if: failure()
        run: |
          echo "❌ Contract mismatch! See logs above."
          echo "Action: Update consumer or revert provider change."
          exit 1
```

## Validação de JWT Compartilhado

### Problema

Durante Strangler Fig:
- User faz login no **user-service** → JWT gerado
- User acessa Workspace no **monólito** → JWT enviado
- Monólito **DEVE aceitar** JWT do user-service

### Solução

**Shared secret:** `JWT_SECRET` idêntico em:
- `user-service/src/main/resources/application.yml`
- `backend/src/main/resources/application.yml`

**Validação:**
- Script: `validate-contracts.sh` verifica se secrets match
- Consumer test: `jwtTokenFromUserService_shouldBeAcceptedByMonolith()`

### Em Produção

Adicionar teste E2E real:

```java
@Test
void jwtFromUserService_shouldAccessMonolithWorkspaces() {
    // Login no user-service
    String jwt = loginViaUserService("test@example.com", "password");

    // Acessar Workspace no monólito
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt);

    ResponseEntity<WorkspaceListResponse> response = restTemplate.exchange(
        monolithUrl + "/api/v1/workspaces",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        WorkspaceListResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

## Breaking Changes — Como Lidar

### Cenário: Provider muda contrato

**Exemplo:** user-service muda campo `accessToken` → `token`

```diff
 public record LoginResponse(
-    String accessToken,
+    String token,
     long expiresIn,
     UUID userId
 ) {}
```

**Resultado:**
1. ✅ Provider tests passam (contratos atualizados)
2. ❌ **Consumer tests FALHAM** (stubs antigos)
3. ❌ CI bloqueia deploy

**Opções:**

#### Opção 1: Atualizar consumer (backward compatibility perdida)
```java
// Monólito: atualizar para usar novo campo
String token = response.getBody().token(); // era accessToken
```

#### Opção 2: Versionamento de API (recomendado)
```java
// user-service: manter v1 + criar v2
@PostMapping("/v1/auth/login") // mantém accessToken
@PostMapping("/v2/auth/login") // usa token

// Monólito: migrar gradualmente
// 1. Suportar ambos v1 e v2
// 2. Migrar consumers para v2
// 3. Deprecar v1 após 3 meses
// 4. Remover v1
```

#### Opção 3: Adicionar campo sem remover (backward compatible)
```java
// user-service: manter accessToken + adicionar token
public record LoginResponse(
    @Deprecated String accessToken, // mantém
    String token,                   // novo
    long expiresIn,
    UUID userId
) {
    public LoginResponse(...) {
        this.accessToken = token; // alias
        this.token = token;
    }
}
```

## Schema Evolution Rules

```
✅ BACKWARD COMPATIBLE (seguro):
   - Adicionar campo opcional
   - Adicionar valor em enum (se consumer ignora desconhecidos)
   - Relaxar validação (required → optional)

❌ BREAKING CHANGES (requer nova versão):
   - Remover campo
   - Renomear campo
   - Mudar tipo de campo
   - Tornar campo optional → required
   - Mudar semântica de campo
```

## Troubleshooting

### Provider tests falham: "404 Not Found"
**Causa:** Path do contrato não bate com controller.
**Fix:** Verificar `@RequestMapping` no controller.

### Consumer tests falham: "Stub not found"
**Causa:** Stubs JAR não instalado.
**Fix:** Rodar `./mvnw install` no user-service.

### Consumer tests falham: "JWT signature does not match"
**Causa:** `JWT_SECRET` diferente entre serviços.
**Fix:** Sincronizar secrets em `.env` ou `application.yml`.

### Tests flaky: "WireMock port already in use"
**Causa:** Teste anterior não limpou WireMock.
**Fix:** Rodar `./mvnw clean test`.

## Próximos Passos

### Fase 1: Auth & User (✅ CONCLUÍDO)
- ✅ Contratos REST (login, me, by-email, invited)
- ✅ Provider tests (8 contratos)
- ✅ Consumer tests (10 cenários)
- ✅ JWT validation
- ✅ CI script

### Fase 2: Workspace Extraction (⏳ PRÓXIMO)
- ⏳ Contratos de Workspace endpoints
- ⏳ Consumer tests no monólito
- ⏳ Multi-tenancy validation (workspaceId)

### Fase 3: Event-Driven Communication (⏳ FUTURO)
- ⏳ Kafka contracts (Pact CDC)
- ⏳ Event schema validation
- ⏳ Backward compatibility de eventos

### Fase 4: Pact Broker (⏳ QUANDO > 3 SERVIÇOS)
- ⏳ Migrar para Pact Broker
- ⏳ UI visual de contratos
- ⏳ Can-I-Deploy matrix

## Referências

- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Consumer-Driven Contracts](https://martinfowler.com/articles/consumerDrivenContracts.html)
- [Pact](https://docs.pact.io/) (alternativa para event-driven)

## Contatos

- **QA Lead:** Especialista em contract testing
- **Tech Lead:** Decisões de versionamento de API
- **DevOps:** CI/CD integration
