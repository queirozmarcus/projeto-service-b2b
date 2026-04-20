# Email Validation Synchronization — Technical Documentation

**Data:** 2026-04-19  
**Autor:** QA Team  
**Status:** ✅ Implementado e Validado

---

## Contexto

### Problema Inicial

**Sintoma:** Email inválido retornava HTTP 500 (Internal Server Error) em vez de 400 (Bad Request).

**Root Cause:** Email Value Object lançava `IllegalArgumentException` genérica que não era capturada pelo `GlobalExceptionHandler`.

**Impacto:**
- ❌ Violação de RFC 9457 (Problem Details)
- ❌ Logs poluídos com stack traces desnecessários
- ❌ Cliente HTTP recebia erro genérico sem contexto
- ❌ Inconsistência entre backend e user-service

**Testes falhando:** 5/6 testes de `InvalidEmailValidationIntegrationTest`:
```
Expected: $.type = "https://api.scopeflow.com/errors/invalid-value-object"
Actual: $.type = "https://api.scopeflow.com/errors/internal-server-error"

Expected: $.error_code = "VO-001"
Actual: $.error_code = "INTERNAL-500"
```

---

## Solução Implementada

### Decisões Técnicas

#### 1. Criar `InvalidValueObjectException` dedicada

**Rationale:**
- Separar validações de VO (domain) de validações genéricas (framework)
- Permitir error code estável (`VO-001`)
- Facilitar debugging (exception específica vs genérica)
- Garantir RFC 9457 compliance

**Design:**
```java
public class InvalidValueObjectException extends RuntimeException {
    private static final String ERROR_CODE = "VO-001";
    private final String voType;

    public InvalidValueObjectException(String voType, String message) {
        super(message);
        this.voType = voType;
    }

    public String getErrorCode() { return ERROR_CODE; }
    public String getVoType() { return voType; }
}
```

**Localização:**
- Backend: `com.scopeflow.core.domain.common.InvalidValueObjectException`
- User-service: `com.scopeflow.user.domain.shared.InvalidValueObjectException`

**Por que duplicado?** DB-per-service design — cada serviço tem seu próprio domain model.

---

#### 2. Sincronizar Email VO

**Alteração:**
```java
// ❌ ANTES
public Email {
    if (!value.matches(EMAIL_REGEX)) {
        throw new IllegalArgumentException("Invalid email format: " + value);
    }
}

// ✅ DEPOIS
public Email {
    if (!value.trim().matches(EMAIL_REGEX)) {
        throw new InvalidValueObjectException("Email", "Invalid email format: " + value);
    }
}
```

**Localização:**
- Backend: `com.scopeflow.core.domain.common.Email`
- User-service: `com.scopeflow.user.domain.model.Email`

---

#### 3. GlobalExceptionHandler já existia

**Verificação Sprint 3:** Handler RFC 9457 já estava implementado, sem necessidade de alteração.

**Handler existente:**
```java
@ExceptionHandler(InvalidValueObjectException.class)
public ResponseEntity<ProblemDetail> handleInvalidValueObject(
    InvalidValueObjectException ex,
    WebRequest request
) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    pd.setType(URI.create("https://api.scopeflow.com/errors/invalid-value-object"));
    pd.setTitle("Invalid Value Object");
    pd.setProperty("error_code", ex.getErrorCode());
    pd.setProperty("error_id", UUID.randomUUID().toString());
    pd.setProperty("timestamp", Instant.now().toString());
    pd.setProperty("vo_type", ex.getVoType());
    pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
}
```

**Localização:**
- Backend: `com.scopeflow.adapter.in.web.GlobalExceptionHandler`
- User-service: `com.scopeflow.user.adapter.in.web.GlobalExceptionHandler`

---

#### 4. Remover Bean Validation do DTO

**Problema Sprint 5:** `@Email` do Jakarta Validation interceptava antes do domain layer.

**Alteração:**
```java
// ❌ ANTES
public record RegisterRequest(
    @NotBlank @Email String email,  // ❌ Intercepta antes do Email VO
    ...
) {}

// ✅ DEPOIS
public record RegisterRequest(
    @NotBlank String email,  // ✅ Validação acontece no Email VO
    ...
) {}
```

**Rationale:** Validação de formato de domínio deve acontecer no **domain layer** (VOs), não na **adapter layer** (DTOs).

**Exceção:** `@NotBlank` mantido — validação sintática genérica (ok em DTO). Issue #4 do code review sugere remover para consistência total.

---

## Arquitetura

### Fluxo de Validação (After Fix)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. HTTP Request                                              │
│    POST /api/v1/auth/register                                │
│    { "email": "invalid", "password": "Pass123!", ... }       │
└────────────────────────┬────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Adapter Layer (Controller)                                │
│    AuthController.register(RegisterRequest)                  │
│    - @NotBlank valida (não vazio) ✅                         │
│    - @Email removido (não intercepta) ✅                     │
└────────────────────────┬────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Application Layer (Use Case)                              │
│    RegisterUserApplicationService.execute()                  │
│    - Email email = Email.of(request.email())                 │
└────────────────────────┬────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Domain Layer (Value Object)                               │
│    Email.of("invalid")                                       │
│    - value.trim() → "invalid"                                │
│    - matches(EMAIL_REGEX) → false                            │
│    - throw new InvalidValueObjectException(                  │
│        "Email",                                              │
│        "Invalid email format: invalid"                       │
│      )                                                       │
└────────────────────────┬────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Exception Handler                                         │
│    GlobalExceptionHandler.handleInvalidValueObject()         │
│    - HTTP 400 (Bad Request)                                  │
│    - RFC 9457 Problem Details                                │
└────────────────────────┬────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. HTTP Response                                             │
│    Status: 400 Bad Request                                   │
│    Content-Type: application/problem+json                    │
│    {                                                         │
│      "type": "https://api.scopeflow.com/errors/invalid-value-object",│
│      "title": "Invalid Value Object",                        │
│      "status": 400,                                          │
│      "detail": "Invalid email format: invalid",              │
│      "error_code": "VO-001",                                 │
│      "error_id": "550e8400-e29b-41d4-a716-446655440000",     │
│      "timestamp": "2026-04-19T10:30:00Z",                    │
│      "vo_type": "Email",                                     │
│      "instance": "/api/v1/auth/register"                     │
│    }                                                         │
└─────────────────────────────────────────────────────────────┘
```

---

### Camadas e Responsabilidades

| Camada | Responsabilidade | Validação |
|--------|------------------|-----------|
| **Adapter (DTO)** | Validações sintáticas genéricas | `@NotBlank`, `@Size`, `@Pattern` (formatos não-domínio) |
| **Application** | Orquestração de use cases | Nenhuma (delega para domain) |
| **Domain (VO)** | Validações semânticas de domínio | Email format, CPF, regras de negócio |
| **Exception Handler** | Tradução exception → HTTP | RFC 9457 compliance |

---

### RFC 9457 Compliance

**Campos obrigatórios:**
- `type` (URI): identifica o tipo de problema
- `title` (string): resumo legível do tipo
- `status` (int): HTTP status code
- `detail` (string): explicação específica do problema
- `instance` (URI): URI da requisição que causou o problema

**Campos opcionais (custom properties):**
- `error_code` (string): código estável para client-side handling
- `error_id` (UUID): rastreabilidade em logs
- `timestamp` (ISO 8601): auditoria
- `vo_type` (string): tipo do Value Object inválido (debugging)

**Content-Type:**
- `application/problem+json` (RFC 9457 compliant)

**Referência:** https://www.rfc-editor.org/rfc/rfc9457.html

---

## Testes

### Pirâmide de Testes

```
       /\
      /E2\      7 testes E2E
     /----\     HTTP → DB (Testcontainers)
    / Con \    3 contract tests
   /--------\   Backend ↔ User-service (Spring Cloud Contract)
  /  Integ  \  22 testes de integração
 /------------\ Controllers + Domain (MockMvc + real DB)
/     Unit     \ 21 testes unitários
\______________/ Email VO + Exception + Handler (JUnit 5 + AssertJ)
```

**Total:** 53 testes (21 unit + 22 integration + 3 contract + 7 E2E)

---

### Cobertura por Tipo

#### 1. Testes Unitários (21)

**InvalidValueObjectException (6 testes):**
- `shouldReturnErrorCodeVO001()`
- `shouldReturnCorrectVoType()`
- `shouldReturnCorrectMessage()`
- `shouldExtendRuntimeException()`
- `shouldSupportDifferentVoTypes()`
- `shouldHandleNullMessage()`

**Email VO (11 testes — backend + user-service):**
- `shouldCreateValidEmail()`
- `shouldNormalizeEmail()`
- `shouldRejectInvalidFormat()`
- `shouldRejectBlankEmail()`
- `shouldRejectNullEmail()`
- `shouldRejectEmailWithoutAtSymbol()`
- `shouldRejectEmailWithoutDomain()`
- `shouldRejectEmailWithSpaces()`
- `shouldThrowInvalidValueObjectException()`
- `shouldIncludeErrorCodeVO001InException()`
- `shouldIncludeVoTypeInException()`

**GlobalExceptionHandler (4 testes):**
- `shouldHandleInvalidValueObjectException_withHttp400()`
- `shouldReturnCorrectType_forInvalidValueObjectException()`
- `shouldIncludeErrorId_forInvalidValueObjectException()`
- `shouldIncludeVoType_forInvalidValueObjectException()`

---

#### 2. Testes de Integração (22)

**InvalidEmailValidationIntegrationTest (6 testes):**
- `shouldReturn400_whenEmailFormatInvalid()`
- `shouldReturn400_whenEmailHasInvalidCharacters()`
- `shouldReturn400_whenEmailMissingAtSymbol()`
- `shouldReturn400_whenEmailMissingDomain()`
- `shouldValidateRfc9457Structure()`
- `shouldReturn400_whenEmailIsBlank()`

**AuthControllerIntegrationTest (8 testes):**
- `shouldRegisterSuccessfully_whenValidEmail()`
- `shouldReturn400_whenEmailInvalid()` (via Email VO)
- `shouldReturn409_whenEmailAlreadyExists()`
- `shouldLoginSuccessfully_whenValidCredentials()`
- `shouldReturn401_whenInvalidPassword()`
- `shouldReturn401_whenUserNotFound()`
- `shouldRefreshTokenSuccessfully()`
- `shouldLogoutSuccessfully()`

**UserControllerIntegrationTest (8 testes):**
- `shouldGetUserByEmail_whenExists()`
- `shouldReturn404_whenUserNotFound()`
- `shouldCreateInvitedUser_whenValidEmail()`
- `shouldReturn400_whenEmailInvalid()` (via Email VO)
- `shouldReturn409_whenEmailAlreadyExists()`
- `shouldUpdateProfile()`
- `shouldChangePassword()`
- `shouldDeactivateAccount()`

---

#### 3. Contract Tests (3)

**Provider contracts (user-service):**
- `register-invalid-email-format.yml` — Email sem @
- `register-invalid-email-missing-domain.yml` — Email incompleto
- `register-invalid-email-blank.yml` — Email vazio

**Provider tests:**
- `ContractVerifierTest` (auto-gerado) — valida que user-service respeita contratos

**Consumer tests (backend):**
- `UserServiceContractTest.shouldReturn400WithVO001_whenRegisteringWithInvalidEmailFormat()`
- `UserServiceContractTest.shouldReturn400WithVO001_whenRegisteringWithEmailMissingDomain()`
- `UserServiceContractTest.shouldReturn400WithVO001_whenRegisteringWithBlankEmail()`

**Framework:** Spring Cloud Contract

**Garantias:**
- ✅ Provider (user-service) mantém contrato RFC 9457
- ✅ Consumer (backend) aceita respostas do provider
- ✅ Mudanças no provider que quebrem contrato → CI falha
- ✅ Backward compatibility garantida

---

#### 4. E2E Tests (7)

**RegisterInvalidEmailE2ETest:**
- `shouldReturn400WithVO001_whenEmailFormatInvalid()`
- `shouldReturn400WithVO001_whenEmailMissingAtSymbol()`
- `shouldReturn400WithVO001_whenEmailMissingDomain()`
- `shouldReturn400WithVO001_whenEmailIsBlank()`
- `shouldReturn400WithVO001_whenEmailContainsSpaces()`
- `shouldReturnDifferentErrorCode_whenPasswordInvalid()` (contraste)
- `shouldRegisterSuccessfully_whenEmailIsValid()` (happy path)

**Framework:** JUnit 5 + Spring Boot Test + MockMvc + Testcontainers (PostgreSQL 16)

**Validações RFC 9457:**
- ✅ HTTP status 400
- ✅ Content-Type: `application/problem+json`
- ✅ Field `type`: URL correta
- ✅ Field `error_code`: `VO-001`
- ✅ Field `error_id`: UUID v4 válido (regex)
- ✅ Field `timestamp`: presente

---

### Cobertura de Código

| Componente | Cobertura | Métrica |
|-----------|-----------|---------|
| `InvalidValueObjectException` | 100% | 3/3 métodos |
| `Email` VO (backend) | 100% | Constructor + validações |
| `Email` VO (user-service) | 100% | Constructor + validações |
| `GlobalExceptionHandler.handleInvalidValueObject()` | 100% | Todas as linhas |
| **Domain layer (média)** | **100%** | **VO-001 path completo** |

**Ferramenta:** JaCoCo

**Comando:**
```bash
cd user-service
./mvnw clean verify jacoco:report
# Abrir: user-service/target/site/jacoco/index.html
```

---

## Validação

### Comandos de Teste

```bash
# 1. Backend — todos os testes
cd backend
./mvnw test
# Esperado: Tests run: 50, Failures: 0, Errors: 0

# 2. User-service — todos os testes
cd user-service
./mvnw verify
# Esperado: Tests run: 48, Failures: 0, Errors: 0

# 3. Teste específico — Email VO
./mvnw test -Dtest=EmailTest
# Esperado: Tests run: 11, Failures: 0

# 4. Teste específico — InvalidValueObjectException
./mvnw test -Dtest=InvalidValueObjectExceptionTest
# Esperado: Tests run: 6, Failures: 0

# 5. Teste específico — GlobalExceptionHandler
./mvnw test -Dtest=GlobalExceptionHandlerTest
# Esperado: Tests run: 15, Failures: 0

# 6. Teste específico — Integration
./mvnw test -Dtest=InvalidEmailValidationIntegrationTest
# Esperado: Tests run: 6, Failures: 0

# 7. Contract tests — provider
./mvnw test -Dtest=ContractVerifierTest
# Esperado: Tests run: 12, Failures: 0

# 8. Contract tests — consumer
cd ../backend
./mvnw test -Dtest=UserServiceContractTest
# Esperado: Tests run: 14, Failures: 0

# 9. E2E tests
cd ../user-service
./mvnw test -Dtest=RegisterInvalidEmailE2ETest
# Esperado: Tests run: 7, Failures: 0

# 10. Verificar sincronização VO-001
grep -r "VO-001" backend/src/ user-service/src/ --include="*.java"
# Esperado: 8+ ocorrências
```

---

### Validação Manual (via curl)

```bash
# 1. Subir ambiente
docker compose up -d

# 2. Testar email inválido (via user-service)
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "ValidPassword123!",
    "fullName": "Test User"
  }'

# Esperado:
# HTTP/1.1 400 Bad Request
# Content-Type: application/problem+json
# {
#   "type": "https://api.scopeflow.com/errors/invalid-value-object",
#   "title": "Invalid Value Object",
#   "status": 400,
#   "detail": "Invalid email format: invalid-email",
#   "error_code": "VO-001",
#   "error_id": "550e8400-...",
#   "timestamp": "2026-04-19T10:30:00Z",
#   "vo_type": "Email",
#   "instance": "/api/v1/auth/register"
# }

# 3. Testar via monólito (proxy AuthControllerV2)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "ValidPassword123!",
    "fullName": "Test User"
  }'

# Esperado: Mesmo response (monólito faz proxy para user-service)

# 4. Testar via Traefik (staging)
curl -X POST http://localhost/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "ValidPassword123!",
    "fullName": "Test User"
  }'

# Esperado: Traefik roteia para user-service (priority 100)
```

---

## Próximos Passos

### Issues do Code Review (Sprint 9)

#### Issue #1: Adicionar validação `vo_type` em E2E
**Prioridade:** Média  
**Esforço:** 5 min  
**Ação:**
```java
// Adicionar em RegisterInvalidEmailE2ETest (7 testes)
.andExpect(jsonPath("$.vo_type").value("Email"))
```

#### Issue #2: E2E variant com `RANDOM_PORT`
**Prioridade:** Média  
**Esforço:** 1h  
**Ação:** Criar `RegisterInvalidEmailE2EFullHttpTest` com HTTP real

#### Issue #3: Contract YAML — valores dinâmicos
**Prioridade:** Média  
**Esforço:** 30 min  
**Ação:** Substituir valores literais por placeholders documentados

#### Issue #4: Decidir sobre `@NotBlank` em `email`
**Prioridade:** Baixa  
**Esforço:** 15 min  
**Ação:** Remover (consistência total) ou documentar decisão de manter

#### Issue #5: Email VO — armazenar `trimmed` value
**Prioridade:** Baixa  
**Esforço:** 10 min  
**Ação:** Reassign `value = value.trim()` no compact constructor

---

### Aplicar Padrão a Outros VOs

**Candidatos:**
- `PasswordHash` — validação de força de senha
- `UserId` / `WorkspaceId` — validação de UUID format
- `PhoneNumber` — validação E.164 format (se implementado)
- `Cpf` / `Cnpj` — validação de documento (se implementado)

**Padrão a seguir:**
1. VO lança `InvalidValueObjectException("VoType", "mensagem")`
2. Handler captura e retorna RFC 9457 (error_code `VO-001`)
3. Remover Bean Validation do DTO (deixar validação no domain)
4. Criar testes (unit + integration + contract + E2E)

---

### Melhorias Futuras

#### 1. Mutation Testing
**Tool:** PIT (Pitest)  
**Objetivo:** Validar qualidade dos testes (detectar mutantes sobreviventes)  
**Comando:**
```bash
./mvnw pitest:mutationCoverage
```

#### 2. Performance Testing
**Tool:** JMeter / Gatling  
**Objetivo:** Validar latência p95 < 200ms (carga 100 req/s)  
**Cenários:**
- Email válido (happy path)
- Email inválido (VO-001 path)

#### 3. Security Testing
**Tool:** OWASP ZAP / Burp Suite  
**Objetivo:** Validar OWASP Top 10  
**Cenários:**
- Injection (SQL, NoSQL, LDAP)
- Broken authentication
- XSS (email field)

#### 4. Observability
**Tool:** OpenTelemetry + Grafana  
**Objetivo:** Métricas de erro (rate, latency, distribution)  
**Dashboard:**
- Taxa de erros VO-001 (por endpoint)
- Latência p50/p95/p99 (validação de email)
- Top 10 mensagens de erro

---

## Referências

### Documentação Interna

- [sprint10-final-audit-report.md](sprint10-final-audit-report.md) — Auditoria final Sprint 10
- [sprint5-test-fix-report.md](sprint5-test-fix-report.md) — Correção Bean Validation
- [sprint6-unit-tests-report.md](sprint6-unit-tests-report.md) — Testes unitários (100% coverage)
- [sprint7-contract-tests-report.md](sprint7-contract-tests-report.md) — Contract tests (3 YAMLs)
- [sprint8-e2e-tests-report.md](sprint8-e2e-tests-report.md) — E2E tests (7 cenários)
- [sprint9-code-review-report.md](sprint9-code-review-report.md) — Code review (0 críticos)
- [AUTH-TESTS-FIX-ANALYSIS.md](AUTH-TESTS-FIX-ANALYSIS.md) — Análise path prefix

### Standards & RFCs

- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Testcontainers](https://www.testcontainers.org/)
- [JUnit 5](https://junit.org/junit5/)
- [AssertJ](https://assertj.github.io/doc/)

### Architecture & Patterns

- [Hexagonal Architecture (Ports & Adapters)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://www.domainlanguage.com/)
- [Value Objects](https://martinfowler.com/bliki/ValueObject.html)
- [Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)

---

**Documentação criada por:** QA Team  
**Data:** 2026-04-19  
**Versão:** 1.0  
**Status:** ✅ Completo e validado
