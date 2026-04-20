# Testes — ScopeFlow AI

**Status:** ✅ 126 testes passando (50 backend + 76 user-service)

---

## Como Executar

### Validação Completa (Recomendado)
```bash
./scripts/validate-qa-full.sh --with-stack
# Executa: unitários + integração + user-service + contracts + cobertura
# Tempo: ~4 min | Log: logs/qa-validation-{timestamp}.log
```

### Por Tipo de Teste
```bash
# Unitários (rápido, sem Docker)
cd backend && ./mvnw test

# Integração (Testcontainers + PostgreSQL)
cd backend && ./mvnw verify

# User service (todos os testes)
cd user-service && ./mvnw verify

# Contract tests (8 contratos)
./scripts/validate-contracts.sh

# Smoke tests (journey + security)
cd backend && ./mvnw test -Dtest=SmokeTests,SecuritySmokeTests

# Classe específica
./mvnw test -Dtest=BriefingSessionTest

# Cobertura JaCoCo
cd backend && ./mvnw verify jacoco:report
# Relatório: backend/target/site/jacoco/index.html
```

---

## Como Escrever Testes

### Naming Convention
```java
// Classe
{Classe}Test                → unitário
{Classe}IntegrationTest     → integração com Testcontainers
*ContractTest               → consumer/provider contract

// Método
void shouldBehavior_whenCondition()
```

### Stack Obrigatória
- **JUnit 5** + **AssertJ** + **Mockito**
- **Testcontainers** com PostgreSQL 16-alpine (nunca H2)
- **Spring Cloud Contract 4.1.0** para contratos

### Testes Unitários
```java
@Test
void shouldThrowException_whenEmailIsInvalid() {
    // Given
    var invalidEmail = "not-an-email";
    
    // When / Then
    assertThatThrownBy(() -> new Email(invalidEmail))
        .isInstanceOf(InvalidValueObjectException.class)
        .hasMessageContaining("Invalid email format");
}
```

### Testes de Integração
```java
@SpringBootTest
@Testcontainers
class BriefingIntegrationTest extends ScopeFlowIntegrationTestBase {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Test
    void shouldCreateBriefing() {
        // Given
        var request = new CreateBriefingRequest(...);
        
        // When
        var response = restTemplate.postForEntity(
            "/api/v1/briefings", request, BriefingResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

### Testes de Controller (@WebMvcTest)
```java
@WebMvcTest(BriefingController.class)
@Import(TestSecurityConfig.class)  // Security desabilitado
class BriefingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private BriefingService briefingService;
    
    @Test
    @WithScopeFlowUser  // Injeta usuário autenticado
    void shouldReturnBriefing_whenExists() throws Exception {
        // Given
        when(briefingService.findById(any())).thenReturn(briefing);
        
        // When / Then
        mockMvc.perform(get("/api/v1/briefings/{id}", briefingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(briefingId.toString()));
    }
}
```

### Separação Unit vs Integration
Maven Surefire exclui `*IntegrationTest` e `*ContractTest` da fase `test`:
```bash
./mvnw test    # Apenas unitários (segundos)
./mvnw verify  # Tudo: unit + integration + contract (minutos)
```

---

## Contract Tests

### Como Funcionam
```
Provider (user-service):
  1. Define contratos YAML (auth + users)
  2. Roda testes e publica stubs JAR

Consumer (monólito):
  3. Baixa stubs JAR
  4. Roda testes contra WireMock
  5. Valida JWT cross-compatible
```

### Executar
```bash
./scripts/validate-contracts.sh
```

**Output esperado:**
```
[1/4] Validating JWT secrets... ✅
[2/4] Running provider tests... ✅
[3/4] Publishing stubs... ✅
[4/4] Running consumer tests... ✅

✅ All contract tests PASSED
```

### Troubleshooting

**Stubs JAR não encontrado:**
```bash
cd user-service && ./mvnw clean install -DskipTests
```

**JWT signature mismatch:**
```bash
# Verificar que JWT_SECRET é idêntico em ambos .env
grep JWT_SECRET backend/.env
grep JWT_SECRET user-service/.env
```

**WireMock 404:**
```bash
# Republicar stubs
cd user-service && ./mvnw clean install -DskipTests
./scripts/validate-contracts.sh
```

**Contract falha após mudança no provider:**
- Breaking change detectado
- Solução: versionar API (`/v2/`) ou adicionar campo como opcional

---

## Quality Gates (CI/CD)

### Obrigatórios (bloqueiam merge)
```bash
./mvnw clean verify                 # Backend: zero falhas
cd user-service && ./mvnw verify    # User service: zero falhas
./scripts/validate-contracts.sh     # 8 contratos passando
# Smoke tests incluídos no verify
```

### Desejáveis
- JaCoCo >= 80% em `core/domain/`
- Mutation score >= 70% (PIT)
- Zero testes flaky

---

## Estrutura

### Backend (`backend/src/test/`)
```
core/domain/           Domain logic (11 classes unit)
core/application/      Use cases (2 classes unit)
adapter/in/web/        Controllers (10 classes @WebMvcTest)
adapter/in/web/
  integration/         API completa (15+ Testcontainers)
  smoke/               E2E journey + OWASP
adapter/out/           Persistence/clients (5 classes)
application/           Outbox, idempotency (6 classes)
contract/              Consumer tests (1 classe)
```

### User Service (`user-service/src/test/`)
```
domain/                Domain logic (1 classe)
adapter/in/web/        Controllers (3 classes)
contract/              Provider tests (2 classes)
```

---

## Cobertura Atual

| Domínio | Unit | Integração | Contract | Status |
|---------|------|-----------|----------|--------|
| **user (user-service)** | ✅ | ✅ | ✅ 8 contratos | Completo |
| **auth (monólito)** | ✅ | ✅ | ✅ Consumer | Completo |
| **briefing** | ✅ | ✅ | — | Pronto |
| **proposal** | ✅ | ✅ | — | Pronto |
| **workspace** | ✅ | ⚠️ | — | Gaps médios |
| **outbox/idempotency** | ✅ | ✅ | — | Pronto |
| **client** | ❌ | ❌ | — | Sem testes |

---

## Logs

Validações automáticas salvam logs timestamped:
```bash
logs/qa-validation-YYYYMMDD-HHMMSS.log

# Ver última execução
ls -t logs/qa-validation-*.log | head -1 | xargs cat
```

---

## Referências

- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Testcontainers](https://www.testcontainers.org/)
- [AssertJ](https://assertj.github.io/doc/)
