# Extração: Proposal

**Prioridade:** 3 (terceiro a extrair)
**Risco:** médio
**Acoplamento:** médio (depende de Workspace e Briefing; é folha — nenhum contexto depende de Proposal)

---

## Dependências de entrada

Quem consome este contexto:
- **Nenhum contexto de domínio depende de Proposal.** É o contexto folha da árvore.
- Consumidores externos (clientes): frontend, integrações webhook de aprovação.

## Dependências de saída

O que este contexto chama:
- **Workspace context** — `WorkspaceId` importado em 4 classes; FK `proposals.workspace_id`
- **Briefing context** — `BriefingSessionId` importado em 6 classes; FK `proposals.briefing_id` (RESTRICT)
- **AWS S3** — upload de PDF gerado após aprovação
- **AWS SES** — envio de e-mail de aprovação

> ⚠️ **Desafio principal:** A FK `proposals.briefing_id → briefing_sessions.id` é cross-context
> e deve ser resolvida em fases. Ver estratégia e responsabilidades abaixo.

---

## Dados

### Tabelas que pertencem a este contexto

| Tabela | Aggregate | Observações |
|--------|-----------|-------------|
| `proposals` | Proposal (root) | Contém `briefing_id` como referência cross-context |
| `proposal_versions` | Proposal (child) | Imutável — trigger bloqueia UPDATE; campo `created_by` sem FK (ver seção abaixo) |
| `approval_workflows` | ApprovalWorkflow (root) | 1:1 com proposal |
| `approvals` | ApprovalWorkflow (child) | Decisões dos aprovadores (APPROVED/REJECTED) |

### Tabelas compartilhadas e estratégia

- `outbox_event` — compartilhada. Filtrar por `aggregate_type IN ('Proposal', 'ApprovalWorkflow')`.
- `activity_logs` — mantida no shared DB (cross-cutting concern).

### FKs saindo deste contexto (dependências de dados)

| FK | Para | Ação | Estratégia |
|----|------|------|------------|
| `proposals.workspace_id` | `workspaces.id` | CASCADE | Manter na fase shared DB |
| `proposals.briefing_id` | `briefing_sessions.id` | RESTRICT | Manter na fase shared DB do card 03; remover como pré-passo do card 04 (ver seção abaixo) |

### FKs apontando PARA proposals (consumidores)

**Nenhuma.** Proposal é folha — nenhuma tabela de domínio tem FK apontando para `proposals`.

### Campo `proposal_versions.created_by` — sem FK para `users.id`

`proposal_versions.created_by` armazena um UUID que referencia `users.id`, mas **sem constraint de FK** (conforme `data-ownership.md` — audit trail deve persistir mesmo se o usuário for deletado).

**Estratégia para resolver o UUID em nome de usuário:**

Denormalização no momento da escrita. Ao criar uma nova versão de proposal, o Proposal service copia o `full_name` do usuário autenticado diretamente do claim JWT, sem chamada adicional ao User service. Se o JWT não contiver `full_name` (token gerado antes da claim ser adicionada), realizar uma chamada REST pontual ao User service com circuit breaker.

```java
// No use case de criação de versão
String createdByName = jwtPrincipal.getFullName()
    .orElseGet(() -> userServiceClient.getUser(jwtPrincipal.getUserId()).fullName());

proposalVersionRepository.save(new ProposalVersion(
    proposalId,
    content,
    jwtPrincipal.getUserId(),  // UUID sem FK
    createdByName,             // denormalizado — não depende de JOIN com users
    Instant.now()
));
```

**Não implementar um campo `created_by_name` retroativamente** em versões já existentes — deixar como `null` para registros históricos, exibir "Usuário removido" no frontend.

### Estratégia para `proposals.briefing_id` — sequência e responsabilidades

A remoção da FK cross-context entre Proposal e Briefing segue fases alinhadas com ADR-002:

**Fase shared DB (card 03):**

Manter a FK `proposals.briefing_id → briefing_sessions.id` intacta. O Proposal service, mesmo sendo uma aplicação separada, acessa o mesmo banco de dados PostgreSQL. A FK continua garantindo integridade referencial durante a transição.

```sql
-- Fase shared DB: FK permanece
-- proposals.briefing_id UUID NOT NULL REFERENCES briefing_sessions(id) ON DELETE RESTRICT
```

**Pré-passo do card 04 (responsabilidade do Proposal service):**

Antes de iniciar a extração do Briefing service, o **Proposal service executa uma migration Flyway** para remover a FK e substituir por UUID opaco. Isso acontece enquanto ambos ainda acessam o shared DB, reduzindo o risco do corte.

```sql
-- Flyway migration no Proposal service (executada antes do card 04)
-- V{n}__remove_briefing_id_fk.sql
ALTER TABLE proposals DROP CONSTRAINT IF EXISTS fk_proposals_briefing_id;
-- proposals.briefing_id permanece como coluna UUID NOT NULL, sem FK
-- Cache local via BriefingCompletedEvent já operacional neste ponto
```

**Validação após remoção da FK:**

O Proposal service já deve ter o `BriefingCompletedListener` consumindo `BriefingCompletedEvent` e populando cache local antes da migration ser executada. A sequência correta é:

1. Implantar Proposal service com `BriefingCompletedListener` operacional
2. Aguardar catchup do consumer (sem lag no RabbitMQ)
3. Executar migration de remoção da FK
4. Validar que criação de Proposal continua funcionando via cache/API call
5. Sinalizar card 04 como desbloqueado

**Rollback da remoção da FK:**

Se a migration já foi executada e for necessário reverter, o rollback de schema é obrigatório — risco maior que o rollback de tráfego. Executar:

```sql
-- Flyway repair + migration reversa (se necessário)
ALTER TABLE proposals ADD CONSTRAINT fk_proposals_briefing_id
    FOREIGN KEY (briefing_id) REFERENCES briefing_sessions(id) ON DELETE RESTRICT;
```

---

## Endpoint público sem JWT — `ApprovalControllerV2`

`GET /proposals/{id}/approve` e `POST /proposals/{id}/approve` são **endpoints públicos** — clientes externos acessam via link de aprovação enviado por e-mail, sem autenticação JWT. Isso implica três requisitos obrigatórios que devem ser tratados como pré-requisitos desta extração:

### 1. Exclusão do `JwtAuthenticationFilter`

O Proposal service deve configurar esses dois endpoints como rotas abertas no `SecurityConfig`:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET,  "/proposals/*/approve").permitAll()
            .requestMatchers(HttpMethod.POST, "/proposals/*/approve").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### 2. Rate limiting nos endpoints públicos

Sem autenticação JWT, esses endpoints ficam expostos a abuso. Implementar rate limiting via `RateLimitInterceptor` (duplicar do monólito, adaptando para o contexto):

- `GET /proposals/{id}/approve`: 30 req/min por IP
- `POST /proposals/{id}/approve`: 5 req/min por IP (ação destrutiva)

### 3. Idempotência para `POST /proposals/{id}/approve`

Cliente pode clicar duas vezes no botão de aprovação ou o browser pode fazer retry. Sem idempotência, a mesma aprovação pode ser processada em duplicata.

Implementar com o mesmo pattern já usado no Briefing context:

```java
@PostMapping("/proposals/{id}/approve")
public ResponseEntity<?> approve(
        @PathVariable UUID id,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody ApprovalDecisionRequest request) {

    if (idempotencyKey != null) {
        return idempotencyService.executeIdempotent(
            idempotencyKey,
            () -> approvalUseCase.approve(id, request)
        );
    }
    return approvalUseCase.approve(id, request);
}
```

O `Idempotency-Key` é opcional para não quebrar clientes que não o enviam — mas o frontend deve sempre enviar para evitar duplicatas.

---

## Path compartilhado — risco de routing antecipado (R-01)

`POST /api/v1/proposals/{proposalId}/briefing-sessions` está implementado em `BriefingSessionControllerV2` no monólito, mas o path começa com `/proposals/`.

Quando o card 03 for executado, o API Gateway vai rotear `/proposals/**` para o Proposal service. **Este endpoint não pertence ao Proposal service** — pertence ao Briefing context (criação de briefing session). Rotear errado causará 404 ou 500 no Proposal service.

**Decisão obrigatória antes de iniciar o card 03:**

Configurar exceção explícita no API Gateway:

```
/proposals/{id}/briefing-sessions  →  monólito  (exceção, não Proposal service)
/proposals/**                      →  Proposal service
```

Após o card 04 (Briefing extraído), alterar o routing:

```
/proposals/{id}/briefing-sessions  →  Briefing service
/proposals/**                      →  Proposal service
```

Esta configuração de exceção é **pré-requisito bloqueador** para iniciar o card 03.

---

## Adapters AWS — PDF e e-mail

O `ProposalApprovalListener` (em `application/listener/`) consome `ProposalApprovedEvent` e chama:

- `ITextPdfServiceAdapter` — gera PDF da proposta aprovada e faz upload para S3
- `AwsSesEmailServiceAdapter` — envia e-mail com link do PDF para o cliente

**Estratégia:** esses adapters movem com o Proposal service. Não criar um serviço de notificação separado — over-engineering para o momento atual (ADR-003: side effects ficam no serviço que possui o aggregate).

**Credenciais AWS (pré-requisito):**

O Proposal service precisa de IAM role ou credenciais AWS configuradas para:
- `s3:PutObject` no bucket de PDFs
- `ses:SendEmail` na region configurada

Credenciais injetadas via variáveis de ambiente — nunca hardcoded:

```yaml
# application-prod.yml
aws:
  region: ${AWS_REGION}
  s3:
    bucket: ${AWS_S3_BUCKET_PROPOSALS}
  ses:
    from-address: ${AWS_SES_FROM_ADDRESS}
```

Secrets gerenciados via AWS Secrets Manager ou variáveis de ambiente do ambiente de deploy — **não incluir no repositório**.

---

## Estratégia de roteamento

### Fase 1: Proxy transparente (Strangler Fig)

1. Criar Proposal service como aplicação Spring Boot separada
2. API Gateway roteia `/proposals/**` para o novo serviço, **com exceção**: `POST /proposals/{id}/briefing-sessions` → monólito
3. Monólito mantém os mesmos endpoints como fallback
4. Feature flag controla % de tráfego roteado para o novo serviço

### Fase 2: Corte completo

1. 100% do tráfego em `/proposals/**` vai para Proposal service (exceção do briefing-sessions permanece)
2. Remover `ProposalControllerV2`, `ApprovalControllerV2` e classes relacionadas do monólito
3. Proposal service consome `BriefingCompletedEvent` via RabbitMQ (listener já operacional)
4. `proposals.briefing_id` tratado como UUID opaco (FK já removida neste ponto)

---

## Endpoints a migrar

**ProposalControllerV2** (`/proposals`) — autenticado via JWT:

| Método | Path | Observações |
|--------|------|-------------|
| POST | /proposals | Criar proposal a partir de briefing |
| GET | /proposals/{id} | Buscar proposal por ID |
| GET | /proposals | Listar proposals do workspace (paginado) |
| GET | /proposals/{id}/versions | Listar versões da proposal |
| PUT | /proposals/{id} | Atualizar proposal (gera nova versão) |
| DELETE | /proposals/{id} | Deletar proposal (soft delete) |
| POST | /proposals/{id}/update-scope | Atualizar escopo via IA |
| POST | /proposals/{id}/publish | Publicar proposta para cliente |
| POST | /proposals/{id}/initiate-approval | Iniciar workflow de aprovação |

**ApprovalControllerV2** (`/proposals`) — **público, sem JWT**:

| Método | Path | Observações |
|--------|------|-------------|
| GET | /proposals/{id}/approve | Página de aprovação para o cliente (token-based) |
| POST | /proposals/{id}/approve | Submeter decisão de aprovação — **idempotência obrigatória** |

**Total: 11 endpoints** (9 autenticados + 2 públicos)

---

## Classes a migrar

### Controllers e DTOs
- `ProposalControllerV2`, `ApprovalControllerV2` + DTOs (`proposal/dto/*`)

### Domain
- `Proposal`, `ProposalDraft`, `ProposalPublished`, `ProposalApproved`, `ProposalRejected` (sealed classes)
- `ApprovalWorkflow`, `Approval`
- `ProposalId`, `ProposalVersion` (value objects)

### Application
- `ProposalService`
- `ProposalApprovalListener` — consome `ProposalApprovedEvent`; chama PDF adapter e e-mail adapter
- `BriefingCompletedListener` — consome `BriefingCompletedEvent`; cria/atualiza cache local de briefings referenciados

### Ports (interfaces)
- `ProposalRepository`, `ApprovalWorkflowRepository`
- `BriefingCacheRepository` (novo — cache local de briefings referenciados)
- `PdfGenerationPort`, `EmailServicePort` (portas de saída para PDF e e-mail)

### Adapters de persistência
- `JpaProposalRepositoryAdapter`, `JpaApprovalWorkflowRepositoryAdapter` + entidades JPA

### Adapters de saída — novos ou movidos
- `BriefingServiceClient` (novo — REST client com Resilience4j para validar `briefing_id` quando cache miss)
- `ITextPdfServiceAdapter` (mover do monólito — gera PDF e faz upload S3)
- `AwsSesEmailServiceAdapter` (mover do monólito — envia e-mail via SES)

### Infraestrutura duplicada (cada serviço tem a sua cópia)
- `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `ScopeFlowPrincipal`
- `GlobalExceptionHandler`
- `IdempotencyService`, `IdempotencyRepository` (implementação local para `POST /proposals/{id}/approve`)
- `RateLimitInterceptor` (rate limiting nos endpoints públicos)

---

## Testes durante a transição

Após o corte do Proposal service, testes de integração do monólito que cobrem fluxos completos (briefing → proposal) devem simular o Proposal service via WireMock:

```java
// Teste de integração no monólito após o corte
@WireMockTest
class BriefingToProposalFlowIntegrationTest {

    @BeforeEach
    void stubProposalService(WireMockRuntimeInfo wm) {
        stubFor(post(urlPathMatching("/proposals"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"id": "00000000-0000-0000-0000-000000000001", "status": "DRAFT"}
                """))
        );
    }

    @Test
    void shouldPublishBriefingCompletedEvent_whenBriefingIsCompleted() {
        // ...
    }
}
```

Testes de integração do Proposal service devem usar Testcontainers com PostgreSQL e RabbitMQ reais — nunca H2 ou mocks de broker.

---

## Critérios de sucesso

- [ ] Todos os 11 endpoints de `/proposals/**` respondem identicamente ao monólito (body + status codes)
- [ ] `BriefingCompletedEvent` é consumido pelo `BriefingCompletedListener` do Proposal service (via RabbitMQ)
- [ ] Criação de Proposal valida `briefing_id` via cache local; API call síncrona ao Briefing service somente em cache miss
- [ ] Workflow de aprovação (`ApprovalControllerV2`) funciona end-to-end
- [ ] PDF gerado e upload S3 funcional após `ProposalApprovedEvent`
- [ ] E-mail enviado via SES com link do PDF após aprovação
- [ ] Endpoints públicos (`GET/POST /proposals/{id}/approve`) acessíveis sem JWT
- [ ] Rate limiting ativo nos endpoints públicos (30/5 req por min por IP)
- [ ] Idempotência funcional para `POST /proposals/{id}/approve` (segundo clique retorna mesma resposta)
- [ ] Exception de routing configurada no API Gateway: `/proposals/{id}/briefing-sessions` → monólito
- [ ] Testes de integração do monólito continuam passando com WireMock stub do Proposal service
- [ ] Latência p99 dos endpoints de proposal < 300ms (baseline do monólito)
- [ ] Health check do Proposal service em `/actuator/health`
- [ ] Zero downtime durante a migração (proxy switch)

---

## Critérios de rollback

**Quando reverter:**
- Latência p99 > 600ms por mais de 5 minutos
- Taxa de erro > 1% em `/proposals/**`
- `BriefingCompletedEvent` não sendo consumido (consumer com lag ou parado)
- Falha na geração de PDF ou envio de e-mail de aprovação
- Falha de idempotência em `POST /proposals/{id}/approve` (aprovações duplicadas)

**Como reverter:**
1. Feature flag: rotear 100% do tráfego de volta para o monólito
2. Monólito já tem os endpoints funcionando (não foram removidos)
3. Tempo de rollback estimado: < 1 minuto (switch de proxy)
4. Se a FK `proposals.briefing_id` já foi removida (migration executada como pré-passo do card 04): rollback de schema necessário — risco significativamente maior; avaliar antes de prosseguir

---

## Pré-requisitos (antes de começar)

**Contextuais:**
- [ ] User service extraído e estável em produção (extração 1 concluída)
- [ ] Workspace service extraído e estável em produção (extração 2 concluída)

**API Gateway — bloqueador crítico:**
- [ ] Exceção de routing configurada: `POST /api/v1/proposals/{proposalId}/briefing-sessions` → monólito (não Proposal service)
- [ ] Routing `/proposals/**` → Proposal service configurado e testado em staging

**Segurança e endpoints públicos:**
- [ ] `SecurityConfig` com rotas públicas mapeadas para `GET/POST /proposals/{id}/approve`
- [ ] Rate limiting implementado nos endpoints públicos (30 req/min GET, 5 req/min POST por IP)
- [ ] Idempotência implementada para `POST /proposals/{id}/approve`

**Infraestrutura AWS:**
- [ ] IAM role ou credenciais AWS configuradas para S3 (`s3:PutObject`) e SES (`ses:SendEmail`)
- [ ] Secrets injetados via variáveis de ambiente — sem hardcode

**Integração com Briefing:**
- [ ] `BriefingServiceClient` implementado com Resilience4j (circuit breaker + retry + timeout)
- [ ] `BriefingCompletedListener` implementado e idempotente (consumer do `BriefingCompletedEvent`)
- [ ] Cache local de briefings funcional (Redis ou in-memory com TTL configurável)
- [ ] Consumer testado com catchup: processar eventos históricos sem duplicatas

**Qualidade e observabilidade:**
- [ ] Contract tests (Pact) entre Proposal e Briefing service (especialmente `BriefingCompletedEvent`)
- [ ] WireMock stubs do Proposal service nos testes de integração do monólito
- [ ] Distributed tracing configurado (OpenTelemetry) — obrigatório a partir do 2º serviço
- [ ] Pipeline CI/CD para o novo serviço (build, test, deploy)
- [ ] Health check e monitoring configurados (`/actuator/health`, métricas de consumer lag, S3, SES)
