# Estratégia de Testes — ScopeFlow AI

**Data:** 2026-04-19
**Autor:** QA Lead
**Versão:** 1.0

---

## Contexto

ScopeFlow AI é uma plataforma SaaS B2B com dois serviços em produção:

- **Monólito** (`backend/`): Java 21, Spring Boot 3, arquitetura hexagonal. Domínios: briefing, proposal, workspace, client, user (residual), auth (proxy).
- **User Service** (`user-service/`): Java 21, Spring Boot 3, hexagonal. Extraído via Strangler Fig. Domínios: user, auth.

Criticidade: plataforma transacional com aprovação de proposta vinculada a assinatura digital (token público) — falhas em auth, aprovação e workspace isolation têm impacto financeiro direto.

---

## Pirâmide de Testes — Estado Atual

```
        [E2E / Smoke]
        SmokeTests (12 steps)
        SecuritySmokeTests (14 testes OWASP)

      [Integração — Testcontainers]
      15+ classes — briefing, proposal, workspace,
      auth, user, idempotency, outbox, eventos

    [Contrato — Spring Cloud Contract]
    8 contratos YAML / 10 cenários consumer (Auth + User)

  [Unitários]
  47 classes — domain, controllers (@WebMvcTest),
  persistence (reflection), application services
```

Proporção estimada: 60% unit / 35% integração / 5% E2E+contract.
Meta: manter proporção saudável — não aumentar E2E sem antes cobrir domínios com unit.

---

## Análise de Risco por Componente

| Componente | Risco | Justificativa | Cobertura atual |
|------------|-------|---------------|-----------------|
| `ApprovalWorkflow` (domain) | Alto | Aprovação vinculada a assinatura pública — erro = contrato inválido | Media — sem teste de domínio isolado |
| `BriefingSession` (domain) | Alto | Fluxo principal do produto | Alta |
| `ProposalService` (domain) | Alto | Ciclo de vida de proposta com estados | Alta |
| `WorkspaceService` (domain) | Alto | Multi-tenancy — isolamento incorreto = vazamento de dados | Alta |
| `JwtAuthenticationFilter` | Alto | Qualquer bypass = acesso não autorizado | Alta |
| `UserServiceRestAdapter` | Alto | Circuit breaker — falha silencia convites | Média |
| `OutboxEventPublisher` | Médio | Falha = eventos perdidos (garantia de entrega) | Alta (integração) |
| `IdempotencyService` | Médio | Duplicação de submissões públicas | Alta (integração) |
| `PurgeJobService` | Médio | Vazamento de dados por retenção incorreta | Média (unit only) |
| `AuthControllerV2` (proxy) | Médio | Proxy para user-service — fallback dev local | Alta |
| `Client` (domain) | Alto | Aggregate sem nenhum teste | **ZERO — gap crítico** |
| `AiGenerationService` | Alto | Core do produto — mockado em todos os testes | **ZERO real — gap crítico** |

---

## Padrões Obrigatórios

### Stack
- JUnit 5 + AssertJ + Mockito
- Testcontainers com PostgreSQL 16-alpine — H2 proibido
- Spring Cloud Contract 4.1.0 para contratos entre serviços

### Naming de classes
```
{Classe}Test                → unitário
{Classe}IntegrationTest     → integração com Testcontainers
ContractVerifier*           → provider (user-service)
*ContractTest               → consumer (monólito)
SmokeTests / SecuritySmokeTests → E2E smoke
```

### Naming de métodos
```java
void shouldBehavior_whenCondition()
```

### Segurança em testes de controller
Todos os testes `@WebMvcTest` devem usar `TestSecurityConfig`:
```java
@Import(TestSecurityConfig.class)
// TestSecurityConfig injeta:
//   @MockBean JwtService
//   @MockBean UserStatusCacheService
//   SecurityFilterChain sem autenticação real
```

### Rate limiting
Desabilitado em todos os testes via `src/test/resources/application.properties`:
```properties
auth.rate-limit.enabled=false
```
Testar rate limiting apenas na classe dedicada `BriefingControllerRateLimitTest`.

### Isolamento de banco
- `@BeforeEach` limpa tabelas relevantes na ordem correta (FK)
- Container compartilhado por classe (`static @Container`)
- Nunca compartilhar estado entre testes

---

## Plano de Ação — Gaps Críticos

| Ação | Tipo | Prioridade | Esforço estimado |
|------|------|-----------|-----------------|
| Criar `ClientTest.java` — domínio client sem nenhum teste | Unit | Alta | P |
| Criar `ClientIntegrationTest.java` | Integração | Alta | M |
| Corrigir bug Email VO → 500 em vez de 400 | Fix + Teste | Alta | P |
| Contract tests para Workspace (Fase 2 Strangler Fig) | Contract | Alta | G |
| Mutation testing com PIT (`pitest:mutationCoverage`) | Qualidade | Média | M |
| Testes de AI generation com WireMock ou stub realista | Integração | Média | G |
| Teste de purge jobs com `@SpringBootTest` e scheduler habilitado | Integração | Média | M |
| Teste de circuit breaker aberto (`UserServiceRestAdapter`) | Integração | Média | M |

Legenda: P = Pequeno (< 1 dia) / M = Médio (1-2 dias) / G = Grande (3+ dias)

---

## Quality Gates para Release

### Obrigatórios (bloqueiam merge para main)
- [ ] `./mvnw clean verify` no backend — zero falhas
- [ ] `./mvnw clean verify` no user-service — zero falhas
- [ ] `./scripts/validate-contracts.sh` — 8 contratos passando
- [ ] SmokeTests (12 steps) e SecuritySmokeTests (14 testes) — todos PASS
- [ ] Sonar: zero issues critical ou blocker
- [ ] Segurança: zero vulnerabilidades critical/high

### Desejáveis (não bloqueiam, mas são rastreados)
- [ ] JaCoCo cobertura de linha >= 80% em `core/domain/` e `adapter/`
- [ ] Mutation score >= 70% em `core/domain/` (via PIT)
- [ ] Zero testes flaky no histórico dos últimos 5 runs

---

## Configuração de CI/CD

```yaml
# Sequência recomendada no pipeline
1. ./mvnw test                    # Unitários (sem Docker) — feedback rápido
2. ./mvnw verify                  # Integração (Testcontainers)
3. cd user-service && ./mvnw verify
4. ./scripts/validate-contracts.sh  # Contract tests
5. jacoco:report                  # Cobertura
```

Contract tests bloqueiam deploy se falharem (step 4 falha o pipeline).

---

## Métricas de Qualidade (referência abril/2026)

| Métrica | Valor atual | Meta |
|---------|-------------|------|
| Classes de teste (backend) | 47 | — |
| Classes de teste (user-service) | 7 | — |
| Domínios com cobertura unit | 5 de 6 (client = 0) | 6 de 6 |
| Domínios com cobertura integração | 5 de 6 (client = 0) | 6 de 6 |
| Contratos ativos | 8 | 8+ (Workspace = próximo) |
| Mutation score | Não medido | >= 70% em domain |
| Testes flaky conhecidos | 0 identificados | 0 |
| Bugs de teste encontrados | 1 (Email VO → 500) | 0 |
