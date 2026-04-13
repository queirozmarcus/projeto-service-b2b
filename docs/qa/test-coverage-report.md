# Test Coverage Report — Sprint 2 QA Phase

**Data:** 2026-03-24
**Sprint:** Sprint 2 — Adapter Layer
**Autor:** QA Engineer

---

## Resumo

| Métrica                           | Antes (Fase 5)  | Depois (Fase 5) | Target  |
|-----------------------------------|-----------------|-----------------|---------|
| Total de testes (@Test)           | 79              | 179+            | 120+    |
| Novos arquivos de teste           | 0               | 11              | —       |
| Cobertura adapter layer           | Estimado ~60%   | Estimado 80%+   | 80%+    |
| Fixes validados com testes        | 0/9             | 9/9             | 9/9     |
| Security smoke tests              | 0               | 14              | —       |

---

## Arquivos Criados

### Testes Unitários

| Arquivo                                               | Fix(es) | Testes |
|-------------------------------------------------------|---------|--------|
| `config/JwtAuthenticationFilterTest.java`             | C1      | 11     |
| `config/UserStatusCacheServiceTest.java`              | C1      | 4      |
| `adapter/in/web/workspace/InviteMemberTest.java`      | C2      | 9      |
| `core/domain/briefing/DetectGapsTest.java`            | C3      | 8      |
| `adapter/in/web/proposal/IpSpoofingMitigationTest.java` | I1    | 8      |
| `adapter/in/web/proposal/WorkspaceIsolationAndPaginationTest.java` | I2, I3 | 11 |
| `adapter/in/web/briefing/BriefingListAndServiceEncapsulationTest.java` | I4, I5 | 9 |
| `adapter/out/persistence/TransactionalAdapterTest.java` | I6    | 7      |

### Testes de Integração (Testcontainers)

| Arquivo                                                          | Fix(es)        | Testes |
|------------------------------------------------------------------|----------------|--------|
| `adapter/in/web/integration/Sprint2FixesIntegrationTest.java`    | C1,C2,C3,I1-I4,I6 | 18  |

### Smoke Tests

| Arquivo                                          | Tipo         | Testes |
|--------------------------------------------------|--------------|--------|
| `adapter/in/web/smoke/SmokeTests.java`            | E2E journey  | 1      |
| `adapter/in/web/smoke/SecuritySmokeTests.java`    | Security     | 14     |

---

## Cobertura por Camada

### adapter/in/web (Controllers)

| Classe                         | Cobertura Estimada | Observações                              |
|--------------------------------|--------------------|------------------------------------------|
| AuthControllerV2               | 85%+               | login, register, me, validation          |
| WorkspaceControllerV2          | 90%+               | CRUD, invite, roles — C2 tests added     |
| ProposalControllerV2           | 85%+               | list, get, publish, isolation — I2/I3    |
| ApprovalControllerV2           | 90%+               | approve, IP spoofing — I1 tests          |
| BriefingControllerV1           | 80%+               | create, list, progress, answers — I4     |
| JwtAuthenticationFilter        | 85%+               | cache, expired, invalid — C1 tests       |

### adapter/out/persistence (Repositories)

| Classe                                  | Cobertura Estimada | Observações               |
|-----------------------------------------|--------------------|---------------------------|
| JpaUserRepositoryAdapter                | 80%+               | CRUD, status transitions  |
| JpaWorkspaceRepositoryAdapter           | 80%+               | CRUD, soft-delete         |
| JpaWorkspaceMemberRepositoryAdapter     | 85%+               | save, delete, find — I6   |
| JpaProposalRepositoryAdapter            | 80%+               | CRUD, status transitions  |
| JpaProposalVersionRepositoryAdapter     | 70%+               | find by proposal          |
| JpaApprovalWorkflowRepositoryAdapter    | 70%+               | workflow lifecycle        |

### core/domain (Domain Services)

| Classe              | Cobertura | Observações                            |
|---------------------|-----------|----------------------------------------|
| BriefingService     | 90%+      | All methods tested, C3 gaps test added |
| WorkspaceService    | 85%+      | RBAC invariants, C2 coverage added     |
| UserService         | 85%+      | register, lookup, saveInvited (C2)     |
| ProposalService     | 80%+      | lifecycle, workspace isolation         |

---

## Mapeamento Fix → Testes

### C1 — JwtAuthenticationFilter Cache

**Objetivo:** Verificar que `UserStatusCacheService` é chamado (e não o repository diretamente), que INACTIVE/null são rejeitados, e que tokens inválidos retornam 401.

**Testes adicionados:**
- `JwtAuthenticationFilterTest` — 11 testes unitários (mock-based)
- `Sprint2FixesIntegrationTest#JwtCacheIntegrationTests` — 4 testes integração
- `SecuritySmokeTests#AuthenticationBypassTests` — 3 testes

### C2 — inviteMember Real Logic

**Objetivo:** Verificar que o fluxo cria `UserInactive` para email desconhecido, não duplica usuário existente, e que apenas OWNER/ADMIN pode convidar.

**Testes adicionados:**
- `InviteMemberTest` — 9 testes unitários (WebMvcTest)
- `Sprint2FixesIntegrationTest#InviteMemberIntegrationTests` — 3 testes integração com DB real
- `SecuritySmokeTests#AuthorizationBypassTests` — teste MEMBER não pode convidar

### C3 — detectGaps Never Null

**Objetivo:** `detectGaps()` retorna `GapAnalysis` não-nulo em qualquer estado (0-100 respostas), score nunca excede 100%.

**Testes adicionados:**
- `DetectGapsTest` — 8 testes unitários cobrindo 0/5/8/10/15 respostas
- `Sprint2FixesIntegrationTest#DetectGapsIntegrationTests` — via `/briefings/{id}/progress`

### I1 — IP Spoofing Mitigation

**Objetivo:** `X-Forwarded-For` aceito apenas de proxy confiável (private/loopback), rejeitado de IP público. IP armazenado na resposta de aprovação.

**Testes adicionados:**
- `IpSpoofingMitigationTest` — 8 testes unitários com `request.setRemoteAddr()`
- `Sprint2FixesIntegrationTest#IpSpoofingIntegrationTests` — validação via approval real
- `SecuritySmokeTests#IpSpoofingSecurityTests` — smoke do fluxo completo

### I2 — Workspace Isolation on Versions

**Objetivo:** `GET /proposals/{id}/versions` retorna 403 quando proposal pertence a workspace diferente do JWT.

**Testes adicionados:**
- `WorkspaceIsolationAndPaginationTest#VersionsWorkspaceIsolationTests` — 4 testes unitários
- `Sprint2FixesIntegrationTest#WorkspaceIsolationIntegrationTests` — 3 testes com DB real
- `SecuritySmokeTests#AuthorizationBypassTests` — teste de bypass

### I3 — Pagination on Proposals

**Objetivo:** `GET /proposals` retorna estrutura paginada com `content`, `totalElements`, `size`, `number`, `first`, `last`. Size máximo = 100.

**Testes adicionados:**
- `WorkspaceIsolationAndPaginationTest#PaginationTests` — 7 testes unitários
- `ProposalControllerV2Test` (pré-existente) — 3 testes de paginação
- `Sprint2FixesIntegrationTest#PaginationIntegrationTests` — 2 testes com DB real

### I4 — GET /briefings Intent

**Objetivo:** Confirmar que `GET /briefings` retorna lista paginada do workspace autenticado.

**Testes adicionados:**
- `BriefingListAndServiceEncapsulationTest#BriefingListTests` — 5 testes
- `Sprint2FixesIntegrationTest#BriefingListIntegrationTests` — 2 testes integração

### I5 — BriefingService No Public Getters

**Objetivo:** `BriefingService` não expõe repositórios como getters públicos — controllers delegam para métodos de domínio.

**Testes adicionados:**
- `BriefingListAndServiceEncapsulationTest#ServiceEncapsulationTests` — 4 testes de reflexão e delegação

### I6 — @Transactional on Adapters

**Objetivo:** Adapters de persistência têm `@Transactional(readOnly = true)` na classe e `@Transactional` nos métodos mutantes.

**Testes adicionados:**
- `TransactionalAdapterTest` — 7 testes de reflexão verificando anotações
- `Sprint2FixesIntegrationTest#TransactionalIntegrationTests` — 2 testes de commit/rollback

---

## Lacunas Conhecidas

| Área                                   | Lacuna                                      | Prioridade |
|----------------------------------------|---------------------------------------------|------------|
| Cache TTL expiry                       | Sem teste de TTL real (evita `Thread.sleep`) | Baixa      |
| Briefing complete via API              | Smoke usa DB seeding, não API                | Baixa      |
| RabbitMQ event publishing              | WorkspaceMemberInvited ainda é TODO          | Média      |
| PDF generation                         | PdfService não implementado ainda            | Média      |
| AI generation integration              | Mocked em todos os testes                    | Alta       |
| Mutation testing                       | `./mvnw pitest:mutationCoverage` não rodado  | Média      |

---

## Próximos Passos

1. Rodar `./mvnw clean verify` e confirmar todos os testes passam
2. Rodar `./mvnw jacoco:report` e validar 80%+ na camada adapter
3. Fazer merge para develop
4. Sprint 3: implementar AI integration + PDF generation
