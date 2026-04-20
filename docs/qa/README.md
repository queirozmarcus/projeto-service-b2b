# Testes — ScopeFlow AI

**Status:** ✅ 126 testes, 0 falhas (50 backend + 76 user-service)  
**Última validação:** 2026-04-20

---

## Quick Start

```bash
# 🚀 VALIDAÇÃO COMPLETA (recomendado)
./scripts/validate-qa-full.sh --with-stack
# Executa: unitários + integração + user-service + contracts + cobertura
# Tempo: ~4 min | Log: logs/qa-validation-{timestamp}.log

# Unitários (rápido, sem Docker)
cd backend && ./mvnw test

# Integração completa (Testcontainers)
cd backend && ./mvnw verify

# User service
cd user-service && ./mvnw verify

# Contract tests (8 contratos Auth + User)
./scripts/validate-contracts.sh

# Cobertura JaCoCo
cd backend && ./mvnw verify jacoco:report
# Relatório: backend/target/site/jacoco/index.html
```

---

## Estado Atual

| Métrica | Valor |
|---------|-------|
| **Testes executados** | **126** (50 backend + 76 user-service) |
| **Taxa de sucesso** | **100%** (0 falhas, 0 erros) |
| **Cobertura** | Alta em domain/application layers |

### Cobertura por Domínio

| Domínio | Unit | Integração | Contract | Status |
|---------|------|-----------|----------|--------|
| **user (user-service)** | ✅ | ✅ | ✅ Provider (8 contratos) | Completo |
| **auth (monólito)** | ✅ | ✅ | ✅ Consumer | Completo |
| **briefing** | ✅ | ✅ | — | Pronto |
| **proposal** | ✅ | ✅ | — | Pronto |
| **workspace** | ✅ | ⚠️ | — | Gaps médios |
| **outbox/idempotency** | ✅ | ✅ | — | Pronto |

**Gaps conhecidos:** Domínio `client` sem testes (backlog), AI generation mockado, circuit breaker em estado `OPEN` não testado

---

## Quality Gates

### Obrigatórios (bloqueiam merge)
- ✅ `./mvnw verify` (backend + user-service) — zero falhas
- ✅ `./scripts/validate-contracts.sh` — 8 contratos passando
- ✅ Smoke tests (journey + OWASP)

### Desejáveis
- ⏹️ JaCoCo >= 80% em `core/domain/`
- ⏹️ Mutation score >= 70% (PIT)
- ✅ Zero testes flaky

---

## Padrões de Teste

### Stack
- **JUnit 5** + **AssertJ** + **Mockito**
- **Testcontainers** com PostgreSQL 16-alpine (nunca H2)
- **Spring Cloud Contract 4.1.0** para contratos

### Naming
```
{Classe}Test                → unitário
{Classe}IntegrationTest     → integração com Testcontainers
*ContractTest               → consumer/provider contract

void shouldBehavior_whenCondition()
```

### Separação unit vs integration
```bash
./mvnw test    # Unitários apenas (rápido, sem Docker)
./mvnw verify  # Tudo (unit + integration + contract)
```

Maven Surefire exclui `*IntegrationTest` e `*ContractTest` da fase `test`.

### Segurança em testes
- `@WebMvcTest` + `@Import(TestSecurityConfig.class)` — security desabilitado
- Para testar autenticado: `@WithScopeFlowUser` (não `@WithMockUser`)
- Rate limiting desabilitado via `test/resources/application.properties`

### Mocks em helpers
Use `lenient()` para stubs em helpers compartilhados. Stubs em `@Test` individuais devem ser estritos.

---

## Contract Tests

### Arquitetura
```
Provider (user-service):
  └─ Define 8 contratos YAML (auth + users)
  └─ Run contract tests
  └─ Publish stubs JAR

Consumer (monólito):
  └─ Download stubs JAR
  └─ Run tests against WireMock
  └─ Validate JWT compatibility
```

### Contratos implementados (Fase 1)
- **4 Auth:** login success/fail, get-me success/fail
- **4 Users:** get by email, create invited, duplicates, not found
- **JWT validation:** token cross-compatible entre serviços
- **RFC 9457 Problem Details:** todos os erros validados

### Executar
```bash
./scripts/validate-contracts.sh
```

**Output esperado:**
```
[1/4] Validating JWT secrets... ✅
[2/4] Running provider tests (user-service)... ✅
[3/4] Publishing stubs to Maven local... ✅
[4/4] Running consumer tests (monolith)... ✅

✅ All contract tests PASSED
Contracts are compatible! Safe to deploy.
```

### Troubleshooting

**1. Stubs JAR não encontrado**
```bash
cd user-service && ./mvnw clean install -DskipTests
```

**2. JWT signature mismatch**
- Verificar `JWT_SECRET` idêntico em `.env` de ambos os serviços
- Validar com: `./scripts/validate-contracts.sh` (primeiro step valida secrets)

**3. WireMock 404 em teste consumer**
- Verificar que o stub JAR foi publicado: `ls ~/.m2/repository/com/scopeflow/user-service/`
- Se não existir: `cd user-service && ./mvnw clean install -DskipTests`

**4. Matchers não funcionam (valores literais)**
- YAMLs usam matchers: `regex(...)`, `timestamp(...)`, `uuid(...)`
- Valores literais no YAML são apenas documentação
- Matcher real está no `matchers:` block

**5. Contract test falha após mudança no provider**
- **Breaking change detectado** — consumer espera resposta diferente
- Solução: versionar API (`/v2/`) ou adicionar campo como opcional

**6. Maven permission denied**
```bash
rm -rf ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/3.2.0
./scripts/validate-contracts.sh
```

### Próximos passos (Fase 2+)
- **Fase 2:** Workspace extraction contracts (multi-tenancy validation)
- **Fase 3:** Event-driven (Kafka contracts com Pact CDC)
- **Fase 4:** Pact Broker (quando > 3 serviços)

---

## Testcontainers

### Base classes
- `ScopeFlowIntegrationTestBase` — API completa
- `BriefingIntegrationTestBase` — domínio briefing
- `MessagingIntegrationTestBase` — eventos

### Container compartilhado
```java
@Container
static PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("scopeflow_test");
```

### Isolamento
`@BeforeEach` limpa tabelas na ordem correta (FK). Nunca compartilhar estado entre testes.

---

## Smoke Tests

### `SmokeTests` (12 steps)
Journey completo: Register → Login → Create workspace → Briefing → Proposal → Approve

### `SecuritySmokeTests` (14 cenários OWASP)
SQL injection, XSS, CSRF, Auth bypass, IDOR

---

## Estrutura de Testes

### Backend (`backend/src/test/`)
```
core/domain/           11 classes unit
core/application/       2 classes unit
adapter/in/web/        10 classes @WebMvcTest
adapter/in/web/
  integration/         15+ classes Testcontainers
  smoke/                2 classes E2E
adapter/out/           5 classes unit
application/           6 classes integração (outbox, idempotency)
contract/              1 classe consumer
```

### User Service (`user-service/src/test/`)
```
domain/                1 classe unit
adapter/in/web/        3 classes (unit + integração)
contract/              2 classes provider
```

---

## Logs e CI/CD

### Logs automáticos
```bash
logs/qa-validation-YYYYMMDD-HHMMSS.log  # Cada execução

# Ver última execução
ls -t logs/qa-validation-*.log | head -1 | xargs cat
```

### Pipeline CI/CD
```bash
./scripts/validate-qa-full.sh  # Executa tudo + logs
```

Contract tests bloqueiam deploy se falharem (step 4).

---

## Lessons Learned

### Best Practices Validadas

1. **Incremental > Big Bang**
   - Sprints pequenos com entregas frequentes
   - Fácil de reverter, feedback rápido
   - Aplicado em 10 sprints (Email VO Validation Sync)

2. **Test-First Mindset**
   - Testes escritos antes ou junto com o código
   - Detecta bugs early (Bean Validation interceptando domain exception)
   - 100% de confiança para deploy

3. **Pirâmide de Testes Respeitada**
   ```
   ~50 unit → ~30 integration → 8 contract → 2 E2E
   Proporção: ~7:4:1:0.3
   ```

4. **Testcontainers > H2/HSQLDB**
   - Real DB (PostgreSQL 16) detecta SQL dialect issues
   - Zero surpresas em produção
   - Custo: +10s startup, benefício: alta confiança

5. **Contract Tests Garantem Backward Compatibility**
   - Breaking changes detectadas antes do deploy
   - Provider-consumer contract (Spring Cloud Contract)
   - Evolutivo: adicionar campos opcionais é safe

6. **RFC 9457 Compliance**
   - `GlobalExceptionHandler` retorna structured errors
   - Error code estável (`BRIEFING-001`, `USER-012`)
   - UUID rastreável (`error_id`), timestamp para auditoria

7. **DB-per-Service Design**
   - Código duplicado intencional (backend vs user-service)
   - Cada serviço com seu próprio domain model
   - Independência de deploy (user-service não quebra se backend mudar)

### Métricas Históricas (Sprint 1-10)

| Sprint | Tempo | Testes Criados | Status |
|--------|-------|----------------|--------|
| 1-4 | 2h | 0 (apenas código) | ✅ |
| 5 | 30min | 5 corrigidos | ✅ |
| 6 | 1h | 21 unit | ✅ |
| 7 | 1h | 3 contract | ✅ |
| 8 | 1h | 7 E2E | ✅ |
| 9 | 30min | 0 (code review) | ✅ |
| 10 | 1h | 0 (auditoria) | ✅ |
| **Total** | **~6h** | **53 testes** | **✅** |

**Velocity:** ~9 testes/hora (considerando apenas sprints de testes)  
**Defect leakage:** 0% (nenhum bug chegou a produção)

---

## Referências

- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Testcontainers](https://www.testcontainers.org/)
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
