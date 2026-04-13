### # Contract Tests — Monolith (Consumer)

## Visão Geral

Consumer contract tests validam que o monólito pode interagir com o `user-service` usando os contratos publicados (stubs).

**Framework:** Spring Cloud Contract Stub Runner 4.1.0

## Estrutura

```
backend/
└── src/test/java/com/scopeflow/contract/
    └── UserServiceContractTest.java    # Consumer tests com WireMock stubs
```

## Como Funciona

1. **Provider (user-service)** define contratos (YAML) e gera stubs JAR
2. **Consumer (monólito)** baixa stubs e roda testes contra WireMock
3. **WireMock** simula user-service usando contratos do JAR de stubs
4. **Testes validam:**
   - Formato de response (JWT, UserResponse, Problem Details)
   - Status codes (200, 201, 401, 404, 409)
   - JWT token do user-service é aceito pelo monólito

## Casos de Teste

### Autenticação

| Teste | Validação |
|-------|-----------|
| `shouldLoginSuccessfully_andReceiveValidJwtToken()` | Login válido → JWT formato correto + claims |
| `shouldReturn401_whenLoginWithInvalidCredentials()` | Login inválido → Problem Details RFC 9457 |
| `shouldGetCurrentUser_whenAuthenticatedWithValidJwt()` | `/auth/me` → UserResponse |
| `shouldReturn401_whenAccessingProtectedEndpointWithoutJwt()` | Sem JWT → 401 |

### Gestão de Usuários

| Teste | Validação |
|-------|-----------|
| `shouldGetUserByEmail_whenUserExists()` | `/users/by-email/{email}` → UserResponse |
| `shouldReturn404_whenUserByEmailNotFound()` | Email inexistente → Problem Details USER-010 |
| `shouldCreateInvitedUser_whenValidRequest()` | `/users/invited` → 201 + status=INACTIVE |
| `shouldReturn409_whenCreatingInvitedUserWithDuplicateEmail()` | Email duplicado → Problem Details USER-011 |

### JWT Validation (CRÍTICO)

| Teste | Validação |
|-------|-----------|
| `jwtTokenFromUserService_shouldBeAcceptedByMonolith()` | JWT do user-service → aceito pelo monólito |

**Por que é crítico?** Durante Strangler Fig, JWT gerado no user-service DEVE ser aceito pelo monólito para acessar endpoints de Workspace/Proposal.

## Como Executar

### Pré-requisitos

1. **Stubs do user-service instalados localmente:**
```bash
cd user-service
./mvnw clean install -DskipTests
```

Isso publica stubs em `~/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/user-service-1.0.0-SNAPSHOT-stubs.jar`

### Executar consumer tests

```bash
cd backend
./mvnw test -Dtest=UserServiceContractTest
```

**Output esperado:** 10 testes passando

### Stub Runner Configuration

```java
@AutoConfigureStubRunner(
    ids = "com.scopeflow:user-service:+:stubs:8090",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
```

**Explicação:**
- `com.scopeflow:user-service` → groupId + artifactId do provider
- `+` → qualquer versão (latest snapshot)
- `:stubs` → classifier do JAR
- `:8090` → porta onde WireMock sobe (stub server)
- `LOCAL` → busca stubs no Maven local (`~/.m2/repository`)

## Validação de JWT Compartilhado

### Problema

Durante migração Strangler Fig:
1. User faz login no **user-service** → recebe JWT
2. User chama endpoint de Workspace no **monólito** → envia JWT
3. Monólito **DEVE aceitar** o JWT gerado pelo user-service

### Solução

**Shared secret:** `JWT_SECRET` deve ser idêntico em:
- `user-service/src/main/resources/application.yml`
- `backend/src/main/resources/application.yml`

**Validação:** Consumer test `jwtTokenFromUserService_shouldBeAcceptedByMonolith()` valida isso.

### Em Produção

Em ambiente real, adicionar teste chamando endpoint real do monólito:

```java
@Test
void jwtFromUserService_shouldAccessMonolithWorkspaces() {
    // Given: JWT do user-service
    String jwtToken = loginViaUserService();

    // When: chamar endpoint Workspace no monólito
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);

    ResponseEntity<WorkspaceListResponse> response = restTemplate.exchange(
        "http://localhost:" + monolithPort + "/api/v1/workspaces",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        WorkspaceListResponse.class
    );

    // Then: JWT aceito
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

## CI Integration

### Pipeline do Monólito (Consumer)

```yaml
name: Contract Tests (Consumer)

on: [push, pull_request]

jobs:
  contract-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout monólito
        uses: actions/checkout@v3

      - name: Setup Java 21
        uses: actions/setup-java@v3
        with:
          java-version: 21

      - name: Download user-service stubs
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
          echo "❌ Contract tests failed! user-service contract changed."
          echo "Action required: Update monólito to match new contract."
          exit 1
```

### Fluxo Completo (Provider → Consumer)

```
1. user-service CI:
   - Run provider contract tests
   - Publish stubs JAR as artifact

2. monólito CI:
   - Download stubs from user-service
   - Run consumer contract tests
   - ✅ PASS → deploy monólito
   - ❌ FAIL → block deploy, notify team
```

## Quebra de Contrato — Exemplo Real

### Cenário: user-service muda response de login

**Commit no user-service:**
```diff
 public record LoginResponse(
-    String accessToken,
+    String token,
     long expiresIn,
     UUID userId,
     String email,
     String fullName
 ) {}
```

**Resultado:**
1. ✅ Provider tests passam (contrato atualizado)
2. ❌ **Consumer tests falham** no monólito:
   ```
   java.lang.AssertionError: expected: accessToken but was: token
   ```
3. ❌ CI bloqueia deploy do monólito
4. 👨💍💻 **Ação necessária:**
   - Atualizar monólito para usar `token` em vez de `accessToken`
   - OU reverter mudança no user-service
   - OU criar v2 do endpoint (`/api/v2/auth/login`)

## Troubleshooting

### Erro: "Stub not found: com.scopeflow:user-service:+:stubs"
**Causa:** Stubs JAR não instalado no Maven local.
**Fix:**
```bash
cd user-service
./mvnw clean install -DskipTests
```

### Erro: "Connection refused: localhost:8090"
**Causa:** WireMock não subiu (StubRunner falhou).
**Fix:** Verificar logs do teste — provável JAR de stubs corrompido.

### Erro: "JWT signature does not match"
**Causa:** `JWT_SECRET` diferente entre user-service e monólito.
**Fix:**
1. Verificar `.env` em ambos os projetos
2. Garantir que `JWT_SECRET` é idêntico
3. Recompilar e testar

### Erro: "Test fails with 404 on stub endpoint"
**Causa:** Path no contrato não bate com URL no teste.
**Fix:** Verificar contratos YAML no user-service.

## Próximos Passos

1. ✅ Contratos implementados para auth + user endpoints
2. ⏳ **TODO:** Adicionar contratos para:
   - Workspace endpoints (quando extraídos)
   - Proposal endpoints (quando extraídos)
   - Event-driven communication (Kafka contracts via Pact CDC)

3. ⏳ **TODO:** Migrar para Pact Broker (quando > 3 serviços):
   - UI visual de contratos
   - Versionamento avançado
   - Compatibility matrix (can-i-deploy checks)

## Referências

- [Spring Cloud Contract Docs](https://spring.io/projects/spring-cloud-contract)
- [Stub Runner Configuration](https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#features-stub-runner)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Provider Contracts](../user-service/CONTRACT-TESTS.md)
