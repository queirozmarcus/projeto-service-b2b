# Contract Tests — User Service (Provider)

## Visão Geral

Contract tests garantem compatibilidade entre `user-service` (provider) e `monólito` (consumer) durante migração Strangler Fig.

**Framework:** Spring Cloud Contract 4.1.0

## Estrutura

```
user-service/
├── src/test/resources/contracts/
│   ├── auth/                          # Contratos de autenticação
│   │   ├── login-success.yml
│   │   ├── login-invalid-credentials.yml
│   │   ├── get-user-me-success.yml
│   │   └── get-user-me-unauthorized.yml
│   └── users/                         # Contratos de gerenciamento de usuários
│       ├── get-user-by-email-success.yml
│       ├── get-user-by-email-not-found.yml
│       ├── create-invited-user-success.yml
│       └── create-invited-user-duplicate.yml
└── src/test/java/com/scopeflow/user/contract/
    ├── ContractVerifierBase.java       # Base class com mocks
    └── ContractVerifierSecurityConfig.java  # Security config para testes
```

## Contratos Implementados

### Auth Endpoints

| Contrato | Endpoint | Status | Validação |
|----------|----------|--------|-----------|
| `login-success.yml` | `POST /api/v1/auth/login` | 200 | JWT token + userId + email + fullName |
| `login-invalid-credentials.yml` | `POST /api/v1/auth/login` | 401 | Problem Details (error_code=AUTH-401) |
| `get-user-me-success.yml` | `GET /api/v1/auth/me` | 200 | UserResponse com id, email, status, createdAt |
| `get-user-me-unauthorized.yml` | `GET /api/v1/auth/me` | 401 | Problem Details (AUTH-401) |

### User Endpoints

| Contrato | Endpoint | Status | Validação |
|----------|----------|--------|-----------|
| `get-user-by-email-success.yml` | `GET /api/v1/users/by-email/{email}` | 200 | UserResponse completo |
| `get-user-by-email-not-found.yml` | `GET /api/v1/users/by-email/{email}` | 404 | Problem Details (USER-010) |
| `create-invited-user-success.yml` | `POST /api/v1/users/invited` | 201 | UserResponse com status=INACTIVE |
| `create-invited-user-duplicate.yml` | `POST /api/v1/users/invited` | 409 | Problem Details (USER-011) |

## Validações Obrigatórias

### JWT Token (contratos de login)
```yaml
matchers:
  body:
    - path: $.accessToken
      type: by_regex
      value: "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"
    - path: $.userId
      type: by_regex
      value: "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
```

### Problem Details RFC 9457 (contratos de erro)
```yaml
matchers:
  body:
    - path: $.error_code
      type: by_equality
    - path: $.status
      type: by_equality
    - path: $.error_id
      type: by_regex
      value: "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    - path: $.timestamp
      type: by_regex
      value: "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})"
```

## Como Executar

### 1. Gerar contratos e testes
```bash
cd user-service
./mvnw spring-cloud-contract:convert
./mvnw spring-cloud-contract:generateTests
```

**Output esperado:**
- Testes gerados em `target/generated-test-sources/contracts/`
- Stubs gerados em `target/stubs/`

### 2. Executar testes de contrato
```bash
./mvnw test -Dtest=ContractVerifier*
```

**Resultado esperado:** 8 testes passando (1 por contrato YAML)

### 3. Publicar stubs (para consumidores)
```bash
./mvnw clean install
```

**Output:** JAR de stubs em `~/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/user-service-1.0.0-SNAPSHOT-stubs.jar`

## Base Class de Testes

`ContractVerifierBase.java` configura o ambiente de teste:

- **Mocks:** `UserService`, `JwtService`, `PasswordEncoder`
- **Test data:**
  - `TEST_USER_ID = 550e8400-e29b-41d4-a716-446655440000`
  - `TEST_EMAIL = test@example.com`
  - `TEST_PASSWORD = ValidPassword123!`
- **Security:** Desabilitada para testes (via `ContractVerifierSecurityConfig`)

## Validação de JWT no Consumer (Monólito)

Os contratos garantem que o JWT gerado pelo user-service:

1. **Formato válido:** `header.payload.signature` (regex validado)
2. **Claims obrigatórios:** `sub` (userId), `email`, `exp`, `iat`
3. **Aceito pelo monólito:** shared secret entre user-service e monólito

## CI Integration

### Pipeline do Provider (user-service)

```yaml
steps:
  - name: Run contract tests
    run: ./mvnw test -Dtest=ContractVerifier*

  - name: Publish stubs
    run: ./mvnw install -DskipTests

  - name: Upload stubs artifact
    uses: actions/upload-artifact@v3
    with:
      name: user-service-stubs
      path: ~/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/*.jar
```

### Pipeline do Consumer (monólito)

```yaml
steps:
  - name: Download stubs
    uses: actions/download-artifact@v3
    with:
      name: user-service-stubs
      path: ~/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/

  - name: Run consumer tests
    run: ./mvnw test -Dtest=UserServiceContract*
```

## Quebra de Contrato

**Exemplo:** Se o user-service mudar o response de login:

```diff
-  "userId": "550e8400-e29b-41d4-a716-446655440000"
+  "user_id": "550e8400-e29b-41d4-a716-446655440000"
```

**Resultado:**
- ❌ Contract tests falham no provider
- ❌ CI bloqueia merge/deploy
- 👨💍💻 Time deve atualizar contratos + consumidor juntos

## Versionamento de Contratos

Para mudanças não-retrocompatíveis:

1. Criar nova versão do endpoint: `/api/v2/auth/login`
2. Manter v1 até todos os consumidores migrarem
3. Deprecar v1 com 3 meses de aviso
4. Remover v1 após migração completa

## Troubleshooting

### Teste falha: "404 Not Found"
**Causa:** Endpoint não existe ou path errado no contrato.
**Fix:** Verificar `url` no YAML bate com `@RequestMapping` do controller.

### Teste falha: "401 Unauthorized"
**Causa:** `ContractVerifierSecurityConfig` não está sendo carregado.
**Fix:** Verificar `@Import` na base class.

### Teste falha: "Schema mismatch"
**Causa:** Response real difere do contrato YAML.
**Fix:** Atualizar contrato ou corrigir controller.

## Referências

- [Spring Cloud Contract Docs](https://spring.io/projects/spring-cloud-contract)
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [JWT Claims](https://datatracker.ietf.org/doc/html/rfc7519#section-4)
- [Monólito Consumer Tests](../backend/src/test/java/com/scopeflow/contract/UserServiceContractTest.java)
