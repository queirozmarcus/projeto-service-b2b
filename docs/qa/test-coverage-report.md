# Relatório de Cobertura de Testes — ScopeFlow AI

**Data:** 2026-04-19
**Sprint atual:** Sprint 10
**Autor:** QA Lead
**Atualizado por:** QA Lead (revisão completa)

---

## Resumo Executivo

| Métrica | Valor atual |
|---------|-------------|
| Total de classes de teste (backend) | 47 |
| Total de classes de teste (user-service) | 7 |
| Testes unitários estimados | 250+ |
| Testes de integração (Testcontainers) | 15+ classes |
| Contract tests (Spring Cloud Contract) | 8 contratos / 10 cenários consumer |
| Smoke tests | 2 classes (journey + security) |
| Domínios com cobertura de integração | briefing, proposal, workspace, auth, user |
| Domínios sem cobertura de integração | client, outbox (parcial), purge (unit only) |

---

## Estrutura Real de Testes

### Backend — `backend/src/test/java/com/scopeflow/`

#### Testes de Domínio (unitários)

| Arquivo | Domínio | Tipo |
|---------|---------|------|
| `core/domain/briefing/BriefingSessionTest.java` | briefing | Unit |
| `core/domain/briefing/BriefingAnswerTest.java` | briefing | Unit |
| `core/domain/briefing/BriefingServiceTest.java` | briefing | Unit |
| `core/domain/briefing/DetectGapsTest.java` | briefing | Unit |
| `core/domain/briefing/ValueObjectTests.java` | briefing | Unit |
| `core/domain/proposal/ProposalServiceTest.java` | proposal | Unit |
| `core/domain/user/UserTest.java` | user | Unit |
| `core/domain/workspace/WorkspaceTest.java` | workspace | Unit |
| `core/domain/workspace/WorkspaceMemberTest.java` | workspace | Unit |
| `core/domain/workspace/WorkspaceServiceTest.java` | workspace | Unit |
| `core/application/briefing/BriefingSessionServiceTest.java` | briefing | Unit |

#### Testes de Controller (unitários — `@WebMvcTest`)

| Arquivo | Domínio |
|---------|---------|
| `adapter/in/web/auth/AuthControllerV2Test.java` | auth |
| `adapter/in/web/briefing/BriefingListAndServiceEncapsulationTest.java` | briefing |
| `adapter/in/web/briefing/mapper/BriefingMapperTest.java` | briefing |
| `adapter/in/web/proposal/ApprovalControllerV2Test.java` | proposal |
| `adapter/in/web/proposal/ProposalControllerV2Test.java` | proposal |
| `adapter/in/web/proposal/ProposalCrudControllerTest.java` | proposal |
| `adapter/in/web/proposal/IpSpoofingMitigationTest.java` | proposal/security |
| `adapter/in/web/proposal/WorkspaceIsolationAndPaginationTest.java` | proposal |
| `adapter/in/web/user/UserControllerTest.java` | user |
| `adapter/in/web/workspace/InviteMemberTest.java` | workspace |
| `adapter/in/web/workspace/WorkspaceControllerV2Test.java` | workspace |

#### Testes de Persistência (unitários/integração)

| Arquivo | Tipo |
|---------|------|
| `adapter/out/persistence/JpaProposalRepositoryAdapterTest.java` | Unit/Reflection |
| `adapter/out/persistence/JpaUserRepositoryAdapterTest.java` | Unit/Reflection |
| `adapter/out/persistence/JpaWorkspaceMemberRepositoryAdapterTest.java` | Unit/Reflection |
| `adapter/out/persistence/TransactionalAdapterTest.java` | Unit/Reflection |
| `adapter/out/userservice/UserServiceRestAdapterTest.java` | Unit |

#### Testes de Integração (Testcontainers — PostgreSQL real)

| Arquivo | Domínios cobertos |
|---------|-------------------|
| `adapter/in/web/integration/ScopeFlowIntegrationTestBase.java` | base class |
| `adapter/in/web/integration/AuthIntegrationTest.java` | auth |
| `adapter/in/web/integration/WorkspaceIntegrationTest.java` | workspace |
| `adapter/in/web/integration/ProposalIntegrationTest.java` | proposal |
| `adapter/in/web/integration/ApprovalIntegrationTest.java` | proposal/approval |
| `adapter/in/web/integration/Sprint2FixesIntegrationTest.java` | auth, workspace, briefing, proposal |
| `adapter/in/web/briefing/integration/BriefingIntegrationTestBase.java` | base class |
| `adapter/in/web/briefing/integration/BriefingControllerV1IntegrationTest.java` | briefing |
| `adapter/in/web/briefing/integration/BriefingSessionControllerV2IntegrationTest.java` | briefing |
| `adapter/in/web/briefing/integration/PublicBriefingControllerV1IntegrationTest.java` | briefing (public) |
| `adapter/in/web/briefing/integration/BriefingControllerCompletionFlowTest.java` | briefing |
| `adapter/in/web/briefing/integration/BriefingControllerErrorHandlingTest.java` | briefing |
| `adapter/in/web/briefing/integration/BriefingControllerRateLimitTest.java` | briefing/rate-limit |
| `adapter/in/web/briefing/integration/BriefingControllerSecurityTest.java` | briefing/security |
| `adapter/in/web/user/UserControllerIntegrationTest.java` | user |
| `application/idempotency/IdempotencyServiceIntegrationTest.java` | idempotency |
| `application/outbox/OutboxEventPublisherIntegrationTest.java` | outbox |
| `application/listener/BriefingCompletedListenerIntegrationTest.java` | eventos |
| `application/listener/ProposalApprovalListenerIntegrationTest.java` | eventos |
| `application/listener/UserRegistrationListenerIntegrationTest.java` | eventos |

#### Testes de Aplicação (unitários)

| Arquivo | Domínio |
|---------|---------|
| `application/purge/PurgeJobServiceTest.java` | purge jobs |

#### Contract Tests

| Arquivo | Papel |
|---------|-------|
| `contract/UserServiceContractTest.java` | Consumer (monólito → user-service) |

#### Smoke Tests

| Arquivo | Tipo |
|---------|------|
| `adapter/in/web/smoke/SmokeTests.java` | Full journey (12 steps) |
| `adapter/in/web/smoke/SecuritySmokeTests.java` | OWASP Top 10 (14 testes) |

#### Infraestrutura de Testes

| Arquivo | Propósito |
|---------|-----------|
| `config/TestSecurityConfig.java` | `@MockBean(JwtService.class)` + `@MockBean(UserStatusCacheService.class)` — desabilita auth real nos testes de controller |
| `config/UserStatusCacheServiceTest.java` | Testa o serviço de cache de status |
| `config/JwtAuthenticationFilterTest.java` | Testa o filtro JWT |
| `config/WithScopeFlowUser.java` | Anotação customizada para injetar usuário autenticado |
| `config/WithScopeFlowUserSecurityContextFactory.java` | Factory da anotação acima |

---

### User Service — `user-service/src/test/`

| Arquivo | Tipo |
|---------|------|
| `domain/UserTest.java` | Unit |
| `adapter/in/web/auth/AuthControllerTest.java` | Unit (`@WebMvcTest`) |
| `adapter/in/web/auth/AuthControllerIntegrationTest.java` | Integração (Testcontainers) |
| `adapter/in/web/user/UserControllerIntegrationTest.java` | Integração (Testcontainers) |
| `config/TestSecurityConfig.java` | Config de segurança para testes |
| `contract/ContractVerifierBase.java` | Provider base class (Spring Cloud Contract) |
| `contract/ContractVerifierSecurityConfig.java` | Security config para verificação de contratos |

---

## Cobertura por Domínio

| Domínio | Unit | Integração | Contract | Gaps críticos |
|---------|------|-----------|---------|---------------|
| **briefing** | Alta (5 classes) | Alta (6 classes) | Nenhum | AI generation mockado; completion via DB seed |
| **proposal** | Alta (3 classes) | Alta (3 classes) | Nenhum | PDF generation não implementado |
| **workspace** | Alta (3 classes) | Média (1 classe) | Nenhum | Nenhum crítico |
| **auth (monólito)** | Média (1 classe) | Alta (1 classe) | Consumer | — |
| **user (monólito)** | Alta (1 classe) | Alta (1 classe) | Consumer | Email VO invalido retorna 500 em vez de 400 |
| **user (user-service)** | Alta (1 classe) | Alta (2 classes) | Provider (8 contratos) | — |
| **client** | Nenhum | Nenhum | Nenhum | **SEM NENHUM TESTE** |
| **idempotency** | Nenhum | Alta (1 classe) | Nenhum | — |
| **outbox** | Nenhum | Alta (1 classe + 3 listeners) | Nenhum | — |
| **purge** | Alta (1 classe) | Nenhum | Nenhum | Jobs agendados não testados com scheduler real |

---

## Padrões de Teste Adotados

### Stack obrigatória
- **JUnit 5** + **AssertJ** + **Mockito**
- **Testcontainers** com PostgreSQL 16-alpine — nunca H2
- **Spring Cloud Contract 4.1.0** para contratos entre serviços

### Naming
```
{Classe}Test           → unitário
{Classe}IntegrationTest → integração com Testcontainers
{Classe}ContractTest   → consumer/provider contract test
```

### Nomenclatura de métodos
```java
void shouldBehavior_whenCondition()
```

### Segurança em testes de controller
Todos os testes `@WebMvcTest` usam `TestSecurityConfig`:
```java
@MockBean(JwtService.class)
@MockBean(UserStatusCacheService.class)
```

### Rate limiting em testes
Desabilitado via `src/test/resources/application.properties`:
```properties
auth.rate-limit.enabled=false
```
A classe `BriefingControllerRateLimitTest` testa o rate limiting com a propriedade habilitada explicitamente.

### Fixtures e helpers
- `BriefingSessionTestFixtures`, `BriefingTestData`, `BriefingTestFixtures` — dados de teste para briefing
- `MessagingEventFixtures`, `MessagingIntegrationTestBase` — base para testes de messaging
- `TestAwsConfig` — mock da configuração AWS para testes
- `WithScopeFlowUser` — anotação para injetar contexto autenticado em testes de integração

---

## Gaps de Cobertura por Prioridade

### Prioridade Alta
| Gap | Domínio | Justificativa |
|-----|---------|---------------|
| Domínio `client` sem nenhum teste | client | Dado existente no produto — risco de regressão silenciosa |
| Email VO inválido retorna 500 | user | Bug confirmado no `user-controller-integration-tests.md` — deveria retornar 400 |
| AI generation sempre mockado | briefing | Fluxo principal do produto não testado com stub realista |

### Prioridade Média
| Gap | Domínio | Justificativa |
|-----|---------|---------------|
| Mutation testing não executado | todos | `./mvnw pitest:mutationCoverage` nunca rodado — cobertura de linha pode ser enganosa |
| Briefing completion via API | briefing | `SmokeTests` usa DB seeding para simular conclusão |
| Purge jobs com scheduler real | purge | Testes unitários verificam lógica mas não o disparo agendado |
| RabbitMQ publishing de WorkspaceMemberInvited | workspace | Evento ainda é TODO no controller |

### Prioridade Baixa
| Gap | Domínio | Justificativa |
|-----|---------|---------------|
| Cache TTL expiry real | auth | Requer `Thread.sleep` — excluído intencionalmente |
| PDF generation | proposal | `ITextPdfServiceAdapter` ainda é stub (Phase 4) |
| Circuit breaker OpenAI | ai | Adapter não existe ainda |

---

## Quality Gates para Release

- [ ] `./mvnw clean verify` — zero falhas no backend
- [ ] `./mvnw clean verify` no user-service — zero falhas
- [ ] JaCoCo: cobertura de linha >= 80% em `core/domain/` e `adapter/`
- [ ] `./scripts/validate-contracts.sh` — 8 contratos passando
- [ ] SmokeTests (12 steps de journey) — todos PASS
- [ ] SecuritySmokeTests (14 testes OWASP) — todos PASS
- [ ] Sonar: zero critical/blocker
- [ ] Segurança: zero critical/high

---

## Como Executar

```bash
# Unitários apenas (rápido, sem Docker)
cd backend && ./mvnw test

# Testes completos com Testcontainers (requer Docker)
cd backend && ./mvnw verify

# User service
cd user-service && ./mvnw verify

# Contract tests
./scripts/validate-contracts.sh

# Cobertura JaCoCo
cd backend && ./mvnw verify jacoco:report
# Abrir: backend/target/site/jacoco/index.html

# Classe específica
./mvnw test -Dtest=BriefingSessionTest
./mvnw test -Dtest=UserControllerIntegrationTest
```
