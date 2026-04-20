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
| **Script de validação** | ✅ `./scripts/validate-qa-full.sh` |
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

**Gaps conhecidos:**
- Domínio `client` sem testes (backlog)
- AI generation mockado (aguarda implementação real)
- Circuit breaker em estado `OPEN` não testado

---

## Quality Gates

### Obrigatórios (bloqueiam merge)
- ✅ `./mvnw verify` (backend + user-service) — zero falhas
- ✅ `./scripts/validate-contracts.sh` — 8 contratos passando
- ✅ Smoke tests (journey + OWASP)
- ⏹️ Sonar: zero critical/blocker
- ⏹️ Segurança: zero critical/high

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
Provider (user-service) → Define 8 contratos YAML → Publish stubs JAR
Consumer (monólito)     → Download stubs        → Run against WireMock
```

### Contratos implementados
- **4 Auth:** login success/fail, get-me success/fail
- **4 Users:** get by email, create invited, duplicates, not found
- **JWT validation:** token cross-compatible entre serviços
- **RFC 9457 Problem Details:** todos os erros validados

### Executar
```bash
./scripts/validate-contracts.sh
```

### Troubleshooting básico
```bash
# Stubs JAR não encontrado
cd user-service && ./mvnw clean install -DskipTests

# JWT signature mismatch
# Verificar JWT_SECRET idêntico em .env de ambos os serviços
```

**Detalhes avançados:** [`archive/contract-tests-detailed.md`](archive/contract-tests-detailed.md)

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
    new PostgreSQLContainer<>("postgres:16-alpine");
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
# OU manualmente:
# 1. ./mvnw test
# 2. ./mvnw verify
# 3. cd user-service && ./mvnw verify
# 4. ./scripts/validate-contracts.sh
# 5. jacoco:report
```

Contract tests bloqueiam deploy se falharem.

---

## Documentação Adicional

### Archive (`archive/`)
- **`contract-tests-detailed.md`** — Troubleshooting avançado, WireMock debugging (341 linhas)
- **`SPRINT-RETROSPECTIVE.md`** — Lessons learned de 10 sprints (504 linhas)
- **`README.md`** — Índice navegável

### Referências
- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Testcontainers](https://www.testcontainers.org/)
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
