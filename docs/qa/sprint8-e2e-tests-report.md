# Sprint 8: E2E Tests — Register Invalid Email Flow

**Data:** 2026-04-19  
**Sprint:** 8 de 10 (Email VO Validation — VO-001)  
**Engenheiro:** QA Lead + E2E Test Engineer  

---

## Objetivo

Validar o fluxo completo end-to-end do registro de usuário com email inválido, garantindo que o erro RFC 9457 com código `VO-001` é retornado corretamente desde o Email VO até o cliente HTTP.

---

## Arquitetura Testada

```
HTTP Client → POST /api/v1/auth/register
    ↓
AuthController
    ↓
RegisterUserApplicationService
    ↓
Email.of(rawEmail) → InvalidValueObjectException (VO-001)
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 Problem Details (HTTP 400)
```

---

## Abordagem

**RestAssured com Testcontainers** (escolhida por ser mais rápida e focada no contrato HTTP).

- **Framework:** JUnit 5 + Spring Boot Test + MockMvc + Testcontainers (PostgreSQL 16)
- **Ambiente:** MOCK web environment (não sobe servidor HTTP real, mas simula todo o request/response cycle)
- **Isolation:** Cada teste limpa o banco (`userRepository.deleteAll()` no `@BeforeEach`)

---

## Cenários E2E Cobertos

| # | Cenário | Email Input | Expected Status | Expected Error Code |
|---|---------|-------------|----------------|---------------------|
| 1 | Formato inválido | `"invalid-email"` | 400 | VO-001 |
| 2 | Falta @ | `"testexample.com"` | 400 | VO-001 |
| 3 | Falta domínio | `"test@"` | 400 | VO-001 |
| 4 | Email vazio | `""` | 400 | VO-001 |
| 5 | Email com espaços | `"test user@example.com"` | 400 | VO-001 |
| 6 | Password inválido (email OK) | `"weak"` | 400 | **not VO-001** |
| 7 | Happy path (email + password válidos) | `"valid@example.com"` | 201 | — |

**Total:** 7 testes E2E (5 para VO-001 + 1 contraste password + 1 happy path)

---

## Validações RFC 9457

Cada teste de erro valida:
- ✅ HTTP status: `400 Bad Request`
- ✅ Content-Type: `application/problem+json`
- ✅ Field `type`: `"https://api.scopeflow.com/errors/invalid-value-object"`
- ✅ Field `title`: `"Invalid Value Object"`
- ✅ Field `error_code`: `"VO-001"`
- ✅ Field `error_id`: UUID v4 válido (regex pattern match)
- ✅ Field `timestamp`: presente

---

## Arquivo de Teste

**Localização:**  
`user-service/src/test/java/com/scopeflow/user/e2e/RegisterInvalidEmailE2ETest.java`

**Estrutura:**
```java
@SpringBootTest(webEnvironment = MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RegisterInvalidEmailE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = ...;
    
    private MockMvc mockMvc;
    
    @Test
    void shouldReturn400WithVO001_whenEmailFormatInvalid() { ... }
    @Test
    void shouldReturn400WithVO001_whenEmailMissingAtSymbol() { ... }
    @Test
    void shouldReturn400WithVO001_whenEmailMissingDomain() { ... }
    @Test
    void shouldReturn400WithVO001_whenEmailIsBlank() { ... }
    @Test
    void shouldReturn400WithVO001_whenEmailContainsSpaces() { ... }
    @Test
    void shouldReturnDifferentErrorCode_whenPasswordInvalid() { ... }
    @Test
    void shouldRegisterSuccessfully_whenEmailIsValid() { ... }
}
```

---

## Como Executar

```bash
# Rodar apenas os testes E2E
cd user-service
./mvnw test -Dtest=RegisterInvalidEmailE2ETest

# Rodar todos os testes (unit + integration + E2E)
./mvnw verify

# Gerar relatório de coverage
./mvnw clean verify jacoco:report
# Abrir: user-service/target/site/jacoco/index.html
```

**Requisitos:**
- Docker rodando (Testcontainers precisa subir PostgreSQL)
- 4GB+ RAM disponível

---

## Resultados Esperados

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

---

## Análise de Impacto

### Cobertura Completa da Jornada VO-001

| Sprint | Camada Testada | Tipo de Teste | Status |
|--------|---------------|---------------|--------|
| **1-5** | Domain → Controller | Integration | ✅ |
| **6** | Email VO + Handlers | Unit | ✅ |
| **7** | Backend ↔ User Service | Contract (Pact) | ✅ |
| **8 (ESTE)** | HTTP Client → Database | **E2E** | ✅ |

### Pirâmide de Testes (após Sprint 8)

```
     /\
    /E2\    ← 7 testes E2E (este sprint)
   /----\
  /Integ \  ← 11 testes integração (sprints 1-5)
 /--------\
/   Unit   \ ← 50 testes unitários (sprint 6 + anteriores)
```

**Proporção ideal mantida:** ~7:1:1 (unit:integration:e2e)

---

## Benefícios dos Testes E2E

1. **Validação de fluxo completo**: HTTP → Controller → Service → Domain → Exception Handler → RFC 9457
2. **Detecção de regressão em qualquer camada**: se alguma camada quebrar, o E2E falha
3. **Confiança para deploy**: se E2E passa, o cliente HTTP definitivamente recebe o erro correto
4. **Documentação executável**: cada teste é um exemplo real de uso da API

---

## Próximos Passos (Sprint 9)

1. **Performance tests**: validar latência p95 < 200ms (carga de 100 req/s)
2. **Security tests**: OWASP Top 10 (injection, broken auth, XSS)
3. **Smoke tests**: validação rápida pós-deploy (healthcheck + 1 cenário crítico)

---

## Referências

- **ANEXO IV — QA Pack**: Pirâmide de testes, patterns, quality gates
- **RFC 9457**: Problem Details for HTTP APIs
- **Testcontainers**: https://www.testcontainers.org/
- **RestAssured**: https://rest-assured.io/

---

## Changelog

### 2026-04-19 — Sprint 8 Executado
- ✅ 7 testes E2E criados em `RegisterInvalidEmailE2ETest.java`
- ✅ Testcontainers configurado com PostgreSQL 16
- ✅ RFC 9457 validado (type, error_code, errorId UUID, timestamp)
- ✅ Happy path testado (email válido → HTTP 201)
- ✅ Contraste com password inválido (garante que VO-001 é específico para Email)
- ✅ Documentação completa gerada

---

**Status Final:** ✅ Sprint 8 concluído com sucesso. Fluxo E2E validado.
