# Testes — ScopeFlow AI

**Última atualização:** 2026-04-19 (Sprint 10)  
**Última validação completa:** 2026-04-19 — ✅ 426 testes, 0 falhas

## Quick Start

```bash
# 🚀 VALIDAÇÃO COMPLETA (recomendado antes de commit/deploy)
./scripts/validate-qa-full.sh
# Executa: unitários + integração + user-service + contracts + cobertura
# Tempo: ~8-10 min | Log: logs/qa-validation-{timestamp}.log

# Unitários (rápido, sem Docker)
cd backend && ./mvnw test

# Integração completa (Testcontainers — requer Docker)
cd backend && ./mvnw verify

# User service
cd user-service && ./mvnw verify

# Contract tests (8 contratos Auth + User)
./scripts/validate-contracts.sh

# Smoke tests (12 steps + 14 security)
cd backend && ./mvnw test -Dtest=SmokeTests,SecuritySmokeTests

# Classe específica
./mvnw test -Dtest=BriefingSessionTest
./mvnw test -Dtest=UserControllerIntegrationTest

# Cobertura JaCoCo
cd backend && ./mvnw verify jacoco:report
# Abrir: backend/target/site/jacoco/index.html
```

---

## Estado Atual

| Métrica | Valor |
|---------|-------|
| **Testes executados (última run)** | **426** (380 backend + 46 user-service) |
| **Taxa de sucesso** | **100%** (0 falhas, 0 erros) |
| Classes de teste (backend) | 47 |
| Classes de teste (user-service) | 7 |
| Testes unitários estimados | 250+ |
| Testes de integração (Testcontainers) | 15+ classes |
| Contract tests (Spring Cloud Contract) | 8 contratos / 10 cenários consumer |
| Smoke tests | 2 classes (12 journey + 14 OWASP) |
| Script de validação completa | ✅ `./scripts/validate-qa-full.sh` |

### Cobertura por Domínio

| Domínio | Unit | Integração | Contract | Gaps |
|---------|------|-----------|----------|------|
| **briefing** | ✅ Alta (5 classes) | ✅ Alta (6 classes) | — | AI generation mockado |
| **proposal** | ✅ Alta (3 classes) | ✅ Alta (3 classes) | — | PDF não implementado |
| **workspace** | ✅ Alta (3 classes) | ⚠️ Média (1 classe) | — | — |
| **auth (monólito)** | ⚠️ Média (1 classe) | ✅ Alta (1 classe) | ✅ Consumer | — |
| **user (monólito)** | ✅ Alta (2 classes) | ✅ Alta (2 classes) | ✅ Consumer | — |
| **user (user-service)** | ✅ Alta (2 classes) | ✅ Alta (3 classes) | ✅ Provider (8 contratos) | — |
| **client** | ❌ Nenhum | ❌ Nenhum | — | **SEM NENHUM TESTE** |
| **idempotency** | — | ✅ Alta (1 classe) | — | — |
| **outbox** | — | ✅ Alta (1 classe + 3 listeners) | — | — |
| **purge** | ✅ Alta (1 classe) | — | — | Scheduler não testado |

---

## Gaps Críticos

### Prioridade Alta
1. **Domínio `client` sem nenhum teste** — risco de regressão silenciosa
2. **AI generation sempre mockado** — fluxo principal não testado com stub realista

### Prioridade Média
- Mutation testing não executado (cobertura de linha pode ser enganosa)
- Briefing completion via API (smoke test usa DB seed)
- Purge jobs com scheduler real (só unit test da lógica)
- Circuit breaker em estado `OPEN` não testado

---

## Quality Gates para Release

### Obrigatórios (bloqueiam merge)
- [ ] `./mvnw clean verify` (backend) — zero falhas
- [ ] `./mvnw clean verify` (user-service) — zero falhas
- [ ] `./scripts/validate-contracts.sh` — 8 contratos passando
- [ ] SmokeTests + SecuritySmokeTests — todos PASS
- [ ] Sonar: zero critical/blocker
- [ ] Segurança: zero critical/high

### Desejáveis (não bloqueiam)
- [ ] JaCoCo >= 80% em `core/domain/` e `adapter/`
- [ ] Mutation score >= 70% em `core/domain/` (PIT)
- [ ] Zero testes flaky no histórico

---

## Padrões de Teste

### Stack obrigatória
- **JUnit 5** + **AssertJ** + **Mockito**
- **Testcontainers** com PostgreSQL 16-alpine — nunca H2
- **Spring Cloud Contract 4.1.0** para contratos entre serviços

### Naming
```
{Classe}Test                → unitário
{Classe}IntegrationTest     → integração com Testcontainers
*ContractTest               → consumer/provider contract
```

```java
// Métodos
void shouldBehavior_whenCondition()
```

### Separação unit vs integration
`maven-surefire-plugin` exclui `*IntegrationTest` e `*ContractTest` da fase `test`:
- `./mvnw test` → unitários apenas, sem Docker, feedback rápido
- `./mvnw verify` → tudo (unitários + integração + contract)

### Segurança em testes de controller
Todos os testes `@WebMvcTest` usam `TestSecurityConfig`:
```java
@Import(TestSecurityConfig.class)
// Injeta @MockBean JwtService + @MockBean UserStatusCacheService
// Security com permitAll() — requisições sem auth chegam ao controller
```

**Importante:**
- `TestSecurityConfig` usa `permitAll()` — sem auth real **não retorna 401**, retorna **500** (controller falha em `SecurityUtil.currentPrincipal()`)
- Para testar autenticado: use `@WithScopeFlowUser` (não `@WithMockUser`)
- Controllers que dependem de `UserServiceClient`: use `@MockBean UserServiceClient` (não `@MockBean RestTemplate`)

### Rate limiting
Desabilitado via `src/test/resources/application.properties`:
```properties
auth.rate-limit.enabled=false
```
Testar rate limiting apenas em `BriefingControllerRateLimitTest`.

### Stubs em helpers compartilhados
Use `lenient()` quando um helper cria mocks usados por múltiplos testes (nem todos precisam de todos os stubs):
```java
private Proposal mockProposal() {
    var proposal = mock(Proposal.class);
    lenient().when(proposal.getId()).thenReturn(...);  // defensivo
    lenient().when(proposal.getStatus()).thenReturn(...);
    return proposal;
}
```
Stubs em métodos `@Test` individuais devem permanecer estritos (sem `lenient()`).

---

## Contract Tests — Spring Cloud Contract

### Arquitetura
```
Provider (user-service):
  └─ Define contratos (8 YAML)
  └─ Run contract tests
  └─ Publish stubs JAR

Consumer (monólito):
  └─ Download stubs JAR
  └─ Run tests against WireMock
  └─ Validate JWT compatibility
```

### Contratos implementados (Fase 1 — Auth & User)
- 4 Auth: login success/fail, get-me success/fail
- 4 Users: get by email, create invited, duplicates, not found
- JWT validation: token do user-service aceito pelo monólito
- RFC 9457 Problem Details: todos os erros validados

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

**Stubs JAR não encontrado:**
```bash
cd user-service && ./mvnw clean install -DskipTests
```

**JWT signature mismatch:**
Verificar `JWT_SECRET` idêntico em `.env` de ambos os serviços.

**Maven permission denied:**
```bash
rm -rf ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/3.2.0
./scripts/validate-contracts.sh
```

### Próximos passos
- **Fase 2:** Workspace extraction contracts (multi-tenancy validation)
- **Fase 3:** Event-driven (Kafka contracts com Pact CDC)
- **Fase 4:** Pact Broker (quando > 3 serviços)

---

## Testes de Integração — Testcontainers

### Base classes
- `ScopeFlowIntegrationTestBase` — base para testes de API completa
- `BriefingIntegrationTestBase` — base para domínio briefing
- `MessagingIntegrationTestBase` — base para testes de eventos (`@Tag("integration")`)

### Container compartilhado
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("scopeflow_test");
```

### Isolamento de banco
`@BeforeEach` limpa tabelas na ordem correta (FK). Nunca compartilhar estado entre testes.

### 15+ classes de integração
- 6 briefing (controller + completion + errors + rate-limit + security)
- 3 proposal (CRUD + approval + integration)
- 1 workspace
- 1 auth
- 1 user
- 1 idempotency
- 1 outbox publisher
- 3 event listeners

---

## Smoke Tests

### `SmokeTests` (12 steps — full journey)
1. Register user
2. Login
3. Create workspace
4. Create briefing
5. Answer questions (6 steps)
6. Complete briefing
7. Create proposal from briefing
8. Approve proposal
9. Verify final state

### `SecuritySmokeTests` (14 testes OWASP Top 10)
- SQL injection (4 cenários)
- XSS (3 cenários)
- CSRF (2 cenários)
- Auth bypass (2 cenários)
- IDOR (3 cenários)

---

## Estrutura de Testes

### Backend — `backend/src/test/java/com/scopeflow/`
```
├── core/domain/                 # 11 classes unit
│   ├── briefing/
│   ├── proposal/
│   ├── user/
│   └── workspace/
├── core/application/            # 2 classes unit
│   ├── briefing/
│   └── purge/
├── adapter/in/web/              # 10 classes @WebMvcTest
│   ├── auth/
│   ├── briefing/
│   ├── proposal/
│   ├── user/
│   └── workspace/
├── adapter/in/web/integration/  # 15+ classes Testcontainers
│   ├── briefing/
│   └── [outros domínios]
├── adapter/in/web/smoke/        # 2 classes E2E
├── adapter/out/persistence/     # 4 classes unit/reflection
├── adapter/out/userservice/     # 1 classe unit
├── application/                 # 6 classes integração
│   ├── idempotency/
│   ├── outbox/
│   └── listener/
├── contract/                    # 1 classe consumer
└── config/                      # 3 classes infraestrutura
```

### User Service — `user-service/src/test/`
```
├── domain/                      # 1 classe unit
├── adapter/in/web/              # 3 classes (1 unit + 2 integração)
├── contract/                    # 2 classes provider
└── config/                      # 1 classe test config
```

---

## Fixtures e Helpers

- `BriefingSessionTestFixtures`, `BriefingTestData`, `BriefingTestFixtures` — dados de teste
- `MessagingEventFixtures` — fixtures para eventos
- `TestAwsConfig` — mock da configuração AWS
- `WithScopeFlowUser` — anotação para injetar contexto autenticado em testes
- `TestSecurityConfig` — security config sem autenticação real

---

## Logs e Rastreabilidade

Todas as execuções do `validate-qa-full.sh` salvam logs timestamped em `logs/`:

```bash
logs/
├── qa-validation-20260419-175542.log  # Última execução
├── qa-validation-YYYYMMDD-HHMMSS.log  # Pattern
└── incident-*.log                      # Logs de incidentes (histórico)
```

**Ver última execução:**
```bash
ls -t logs/qa-validation-*.log | head -1 | xargs cat
```

---

## CI/CD Pipeline

```yaml
# Sequência recomendada (automatizada via validate-qa-full.sh)
1. ./mvnw test                       # Unitários (sem Docker) — feedback rápido
2. ./mvnw verify                     # Integração (Testcontainers)
3. cd user-service && ./mvnw verify
4. ./scripts/validate-contracts.sh  # Contract tests
5. jacoco:report                     # Cobertura

# Ou usar o script completo:
./scripts/validate-qa-full.sh       # Executa 1-5 + logs
```

Contract tests bloqueiam deploy se falharem (step 4 falha o pipeline).

---

## Referências

- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Testcontainers](https://www.testcontainers.org/)
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)

---

## Documentação Arquivada

Este README contém toda a informação necessária para uso diário. Documentação histórica e detalhada está em **[`archive/`](archive/)** (16 documentos):

### Quando Consultar o Archive

- **Troubleshooting avançado** → `archive/contract-tests-detailed.md` (341 linhas, debugging profundo)
- **Planejamento de novos testes** → `archive/test-coverage-detailed.md` (321 linhas, mapa completo de gaps)
- **Contexto histórico** → `archive/sprint{5-10}-*-report.md` (relatórios de execução das 10 sprints)
- **Decisões técnicas** → `archive/SPRINT-RETROSPECTIVE.md` (retrospectiva das sprints)

**Ver índice completo:** [`archive/README.md`](archive/README.md)
