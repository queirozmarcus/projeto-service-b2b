# Extração: Briefing

**Prioridade:** 4 (último a extrair)
**Risco:** ALTO
**Acoplamento:** médio de saída (depende de Workspace via JWT e templates, e de Proposal via validação V2), complexidade interna muito alta

> **Por que último?** Não pela falta de valor — Briefing é o coração do produto. Mas é
> o contexto mais complexo do sistema: 3 controllers, 19 endpoints, state machine com sealed
> classes, integração assíncrona com OpenAI, public token flow sem autenticação JWT, e
> `idempotency_records` de uso exclusivo deste contexto. Extrair por último garante que a
> infra de microsserviços já esteja validada pelos 3 serviços anteriores.

## Dependências de entrada

Quem consome este contexto:
- **Proposal context** — `BriefingSessionId` (6 classes); FK `proposals.briefing_id` (RESTRICT)
  - **Resolvida antes desta extração:** Proposal service já usa UUID opaco + cache local (concluído no card 03)
- **ProposalControllerV2** — `BriefingSessionControllerV2` compartilha path `/api/v1/proposals/{proposalId}/briefing-sessions`
  - Requer decisão de routing: Briefing service assume este endpoint ou redireciona via API Gateway

## Dependências de saída

O que este contexto chama:
- **Workspace context** — `WorkspaceId` importado em 12 classes; FK `briefing_sessions.workspace_id`
  - **WorkspaceServiceClient (novo)** — REST client para carregar templates de perguntas por `serviceType` após ADR-004 (ver seção de Dados). Endpoints consumidos: `GET /workspaces/{id}/service-context-profiles` e `GET /workspaces/{id}/service-context-profiles/{profileId}/questions`
- **Proposal context** — validação de `proposalId` em `BriefingSessionControllerV2.createBriefingSession()`
  - **ProposalServiceClient (novo)** — REST client para verificar que a Proposal existe e pertence ao workspace informado. Quando Briefing for extraído, Proposal já será um serviço separado (extraído no card 03); sem este client, `createBriefingSession` não tem como validar o `proposalId` cross-service
- **OpenAI API** — chamadas assíncronas para geração de perguntas e análise de respostas
  - Sem circuit breaker atualmente (ponto crítico — obrigatório antes do corte)

## Dados

### Tabelas que pertencem a este contexto (5 tabelas — ADR-004)

> **ADR-004:** `service_context_profiles` e `service_context_questions` foram transferidas para o
> **Workspace context**. A decisão reflete que esses templates são configurações administradas e
> possuídas pelo Workspace; o Briefing os consome via REST call ao Workspace service, não os
> persiste localmente. O card anterior (versão pré-ADR-004) listava 7 tabelas e incluía essas
> duas — essa contagem está desatualizada.

| Tabela | Aggregate | Tipo | Observações |
|--------|-----------|------|-------------|
| `briefing_sessions` | BriefingSession (root) | Domain | State machine: `IN_PROGRESS → COMPLETED / ABANDONED` |
| `briefing_questions` | BriefingSession (child) | Domain | Cascade delete com session |
| `briefing_answers` | BriefingSession (child) | Domain | Imutável — trigger bloqueia UPDATE |
| `ai_generations` | BriefingSession (audit) | Domain | Audit trail de chamadas OpenAI |
| `briefing_activity_logs` | BriefingSession (audit) | Domain | Audit trail de ações do usuário |

`service_context_profiles` e `service_context_questions` **não pertencem mais ao Briefing service.**
Elas são possuídas pelo Workspace service (card 02) e expostas via `GET /workspaces/{id}/service-context-profiles/**`.

### Tabelas compartilhadas e estratégia

- `outbox_event` — compartilhada. Filtrar por `aggregate_type = 'BriefingSession'`.
  - Na fase database-per-service: Briefing service terá sua própria tabela `outbox_event`.
- `idempotency_record` — usada **exclusivamente** pelo Briefing (public endpoints).
  - Na fase database-per-service: move com o Briefing service.

### FKs saindo deste contexto

| FK | Para | Ação | Estratégia |
|----|------|------|------------|
| `briefing_sessions.workspace_id` | `workspaces.id` | CASCADE | Manter na fase shared DB |

> `service_context_profiles.workspace_id → workspaces.id` foi removida deste contexto pelo ADR-004.
> Essa FK agora é gerenciada pelo Workspace service.

### FKs apontando PARA briefing_sessions (consumidores)

| FK | De | Estratégia |
|----|----|------------|
| `proposals.briefing_id` | Proposal | **Já resolvida na extração 3** — Proposal usa UUID + cache |

## Pré-requisito de refatoração: BriefingSessionControllerV2 (antes de extrair)

> **G-04.1 — Violação hexagonal a corrigir ANTES da extração.**

`BriefingSessionControllerV2` importa diretamente as entidades JPA `JpaBriefingSession` e
`JpaServiceContextQuestion` do pacote `adapter.out.persistence.briefing`. Um controller
(`adapter.in`) não pode depender de entidades de `adapter.out` — isso viola a separação
hexagonal e cria acoplamento de adapter para adapter, contornando o domínio.

**Evidência no código atual:**

```java
// adapter/in/web/briefing/BriefingSessionControllerV2.java — LINHA 5-6
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;      // VIOLAÇÃO
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestion; // VIOLAÇÃO

// ... controller recebe JpaBriefingSession direto do service:
JpaBriefingSession session = service.createBriefingSession(proposalId, workspaceId);
return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session, proposalId));

// ... e mapeia JpaServiceContextQuestion direto:
List<JpaServiceContextQuestion> questions = service.getQuestions(id, workspaceId);
return ResponseEntity.ok(questions.stream().map(this::toQuestionResponse).toList());
```

**Refatoração necessária:**
1. `BriefingSessionService` deve retornar objetos de domínio (`BriefingSession`) ou DTOs de saída — nunca entidades JPA
2. O controller deve mapear domínio → DTO usando um mapper em `adapter.in.web.briefing.mapper`
3. `JpaServiceContextQuestion` em `getQuestions()` deve ser substituído por um DTO de domínio (ou, após ADR-004, pelo DTO retornado pelo `WorkspaceServiceClient`)
4. Adicionar ArchUnit test para enforçar: nenhuma classe de `adapter.in` importa classes de `adapter.out`

Esta refatoração é um **pré-requisito obrigatório** antes de iniciar a extração — o Briefing service não pode ser gerado a partir de código que viola sua própria arquitetura.

## Estratégia de roteamento

### Fase 1: Proxy transparente (Strangler Fig)

1. Criar Briefing service como aplicação Spring Boot separada
2. API Gateway roteia os paths abaixo para o novo serviço:
   - `/briefings/**`
   - `/public/briefings/**`
   - `/api/v1/briefing-sessions/**`
   - `/api/v1/proposals/{proposalId}/briefing-sessions` (shared path — requer atenção)
3. Monólito mantém os mesmos endpoints como fallback
4. Feature flag controla % de tráfego roteado para o novo serviço

> **Atenção ao path compartilhado:** `POST /api/v1/proposals/{proposalId}/briefing-sessions`
> está em `BriefingSessionControllerV2` mas o path inclui `/proposals/`. Após a extração de
> Proposal, o API Gateway precisa rotear este path para o **Briefing service** (dono da criação
> de briefing sessions), não para o Proposal service.

### Fase 2: Corte completo

1. 100% do tráfego nos paths de briefing vai para o Briefing service
2. Remover os 3 controllers de briefing e classes relacionadas do monólito
3. Briefing service publica `BriefingCompletedEvent` via RabbitMQ (Proposal service já consome)
4. `idempotency_record` move para o DB do Briefing service
5. Monólito vira proxy puro (sem contextos de domínio) — ver seção "Transição de testes"

## Endpoints a migrar

> **Nota sobre contagem:** ADR-001 menciona "11 endpoints" para Briefing. Essa contagem foi
> escrita antes da implementação dos controllers V2 e públicos. A contagem correta e atual
> é **19 endpoints** conforme detalhado abaixo. O ADR-001 será atualizado separadamente para
> refletir o estado real da implementação.

**BriefingControllerV1** (`/briefings`) — autenticado:

| Método | Path | Observações |
|--------|------|-------------|
| POST | /briefings | Criar briefing session |
| GET | /briefings | Listar briefings do workspace (paginado) |
| GET | /briefings/{briefingId} | Buscar briefing por ID |
| GET | /briefings/{briefingId}/progress | Progresso da sessão |
| GET | /briefings/{briefingId}/next-question | Próxima pergunta (lógica de sequência) |
| POST | /briefings/{briefingId}/answers | Submeter resposta |
| POST | /briefings/{briefingId}/complete | Completar sessão (dispara `BriefingCompletedEvent`) |
| POST | /briefings/{briefingId}/abandon | Abandonar sessão |

**PublicBriefingControllerV1** (`/public/briefings`) — **sem autenticação JWT, usa public token**:

| Método | Path | Observações |
|--------|------|-------------|
| GET | /public/briefings/{publicToken} | Dados do briefing pelo token público |
| GET | /public/briefings/{publicToken}/next-question | Próxima pergunta (cliente anônimo) |
| POST | /public/briefings/{publicToken}/answers | Submeter resposta (cliente anônimo) |
| GET | /public/briefings/{publicToken}/questions | Todas as perguntas |
| POST | /public/briefings/{publicToken}/batch-answers | Submeter múltiplas respostas |

**BriefingSessionControllerV2** (`/api/v1`) — autenticado:

| Método | Path | Observações |
|--------|------|-------------|
| POST | /api/v1/proposals/{proposalId}/briefing-sessions | Criar session a partir de proposal — requer `ProposalServiceClient` |
| GET | /api/v1/briefing-sessions/{id} | Buscar session por ID |
| GET | /api/v1/briefing-sessions/{id}/questions | Listar perguntas — templates via `WorkspaceServiceClient` após ADR-004 |
| GET | /api/v1/briefing-sessions/token/{token} | Buscar session por token público |
| POST | /api/v1/briefing-sessions/{id}/answers | Submeter resposta |
| POST | /api/v1/briefing-sessions/{id}/complete | Completar session |

**Total: 19 endpoints** (8 autenticados V1 + 5 públicos + 6 V2)

## Classes a migrar

**Controllers e DTOs:**
- `BriefingControllerV1`, `PublicBriefingControllerV1`, `BriefingSessionControllerV2` + DTOs (`briefing/dto/*`)
- Mappers em `adapter.in.web.briefing.mapper` (criados durante refatoração do G-04.1)

**Domain:**
- `BriefingSession`, `BriefingInProgress`, `BriefingCompleted`, `BriefingAbandoned` (sealed classes)
- `BriefingQuestion`, `BriefingAnswer`, `AiGeneration` (domain)
- `BriefingSessionId`, `PublicToken`, `AnswerText`, `CompletionScore` (value objects)

> `ServiceContextProfile` e `ServiceContextQuestion` **não migram para o Briefing service** —
> pertencem ao Workspace service (ADR-004).

**Application:**
- `BriefingService`, `BriefingSessionService` (application)

**Ports:**
- `BriefingSessionRepository`, `ServiceContextProfileRepository` (ports out — repositórios)
- `WorkspaceServiceClient` (novo port out — REST client para templates via Workspace service)
- `ProposalServiceClient` (novo port out — REST client para validar `proposalId` via Proposal service)

**Adapters:**
- Todos os adapters JPA de briefing (persistence layer) — exceto entidades de `service_context_*`
- `OpenAiClient` + `OpenAiAdapter` (adapter de saída — integração com OpenAI)
- `IdempotencyService`, `IdempotencyRepository` + tabela `idempotency_record` (move com Briefing)
- `RateLimitInterceptor` (duplicar — rate limit em endpoints públicos)

**Infraestrutura (duplicar por serviço):**
- `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `ScopeFlowPrincipal`
- `GlobalExceptionHandler`

## Sobre o BriefingCompletedListener

> **G-04.5 — Clareza sobre ownership do listener.**

O `BriefingCompletedListener` existe hoje no monólito (`application/listener/`) com
responsabilidade de fallback para re-geração de perguntas quando a sessão é completada.
Ele **não é responsável por criar Proposals** — essa responsabilidade pertence ao Proposal
service, que consome o `BriefingCompletedEvent` diretamente.

Na decomposição:
- O listener de **fallback de perguntas** (lógica atual do `BriefingCompletedListener` no monólito)
  pertence ao **Briefing service** — migra neste card
- O consumer de `BriefingCompletedEvent` para **geração de Proposal** foi criado no
  **Proposal service** (card 03) — já estará funcionando quando este card for executado
- Ao extrair o Briefing service, o `BriefingCompletedListener` do monólito é removido; o
  Briefing service passa a publicar o evento via RabbitMQ e o Proposal service já o consome

Isso evita confusão: quando o card 04 for executado, o consumer de Proposal para
`BriefingCompletedEvent` já existe e está estável em produção.

## Complexidades específicas desta extração

1. **Public token flow (sem JWT):** `PublicBriefingControllerV1` usa token opaco em vez de JWT.
   O Briefing service precisa de rota sem `JwtAuthenticationFilter` para esses endpoints.

2. **OpenAI async sem circuit breaker:** Risco de cascading failure se OpenAI estiver lento.
   Implementar `Resilience4j` com timeout + fallback ANTES de extrair. Pré-requisito obrigatório.

3. **State machine com sealed classes:** `BriefingSession` tem transições de estado complexas
   (`IN_PROGRESS → COMPLETED/ABANDONED`). Garantir que todos os testes de estado passam antes
   do corte.

4. **`idempotency_record` exclusivo:** Apenas Briefing usa idempotency (endpoints públicos).
   Na fase shared DB, continua compartilhada. Na fase database-per-service, move com Briefing.

5. **Templates de perguntas via REST (ADR-004):** `GET /api/v1/briefing-sessions/{id}/questions`
   anteriormente lia de `service_context_questions` local. Após ADR-004, o Briefing service
   chama o Workspace service via `WorkspaceServiceClient`. Implementar cache local com TTL
   para evitar latência adicional em cada requisição de pergunta.

6. **Validação cross-service de `proposalId`:** `BriefingSessionControllerV2.createBriefingSession()`
   recebe `proposalId` e precisa confirmar que a Proposal existe e pertence ao workspace.
   Quando Briefing for extraído, Proposal já é um serviço separado — requer `ProposalServiceClient`
   com circuit breaker. Falha na validação deve retornar 404 (Proposal não encontrada) ou 403
   (Proposal de outro workspace).

7. **Path compartilhado `/api/v1/proposals/{proposalId}/briefing-sessions`:** API Gateway precisa
   rotear este path para o Briefing service, não para o Proposal service. Definir routing antes
   do corte.

## Critérios de sucesso

- [ ] Todos os 19 endpoints respondem identicamente (body + status codes)
- [ ] Public token flow funciona sem JWT (clientes anônimos conseguem responder)
- [ ] State machine de `BriefingSession` funciona corretamente (todas as transições)
- [ ] OpenAI integration funciona com circuit breaker ativo
- [ ] `BriefingCompletedEvent` publicado via RabbitMQ e consumido pelo Proposal service
- [ ] Idempotency funciona para endpoints públicos (respostas duplicadas rejeitadas)
- [ ] Rate limiting funciona nos endpoints públicos
- [ ] Templates de perguntas carregados via `WorkspaceServiceClient` (ADR-004)
- [ ] Validação de `proposalId` funciona via `ProposalServiceClient` (retorna 404/403 corretos)
- [ ] Testes de integração do monólito continuam passando durante o período de feature flag (fallback ativo)
- [ ] Latência p99 do `/briefings/**` < 300ms; `/public/**` < 500ms (inclui latência OpenAI)
- [ ] Health check do Briefing service em `/actuator/health`
- [ ] Zero downtime durante a migração (proxy switch)

## Critérios de rollback

**Quando reverter:**
- Latência p99 > 800ms por mais de 5 minutos (OpenAI pode ser lento — threshold maior)
- Taxa de erro > 1% nos endpoints autenticados
- Taxa de erro > 2% nos endpoints públicos (clientes anônimos)
- `BriefingCompletedEvent` não sendo publicado (Proposal service para de receber eventos)
- Falha no idempotency (respostas duplicadas sendo processadas)
- Falha na state machine (briefings completados mas sem evento publicado)
- `WorkspaceServiceClient` retornando erros > 5% (templates de perguntas indisponíveis)
- `ProposalServiceClient` retornando erros que impedem criação de briefing sessions

**Como reverter:**
1. Feature flag: rotear 100% do tráfego de volta para o monólito
2. Monólito já tem os endpoints funcionando (não foram removidos)
3. Tempo de rollback estimado: < 1 minuto (switch de proxy)

## Transição de testes após extração completa

Após o corte do card 04, o monólito não possui mais nenhum contexto de domínio próprio — todos
(User, Workspace, Proposal, Briefing) foram extraídos. O monólito passa a ser um **proxy puro**:
recebe requisições, roteia para o serviço correto via API Gateway, e retorna a resposta.

**Impacto nos testes:**
- Testes de integração do monólito que cobriam o fluxo completo (ex: criar workspace → criar briefing → completar → gerar proposal) **não têm mais código de domínio para testar** — o monólito não executa mais lógica de negócio
- Esses testes migram para **testes E2E inter-service**: exercitam os endpoints reais de cada serviço em sequência, verificando o fluxo completo end-to-end
- Testes unitários e de integração de cada serviço continuam existindo e sendo responsabilidade de cada serviço individualmente
- O pipeline de CI/CD passa a executar: testes de cada serviço isolado + suite E2E inter-service como quality gate de release

**Estratégia de E2E inter-service:**
- Scripts de bash existentes (`RUN-BRIEFING-TESTS.sh`, `TEST-BRIEFING-SESSION.sh`) devem ser adaptados para apontar para os endpoints dos microsserviços diretamente
- Adicionar testes de contrato (Pact) como substituto dos testes de integração cruzada — mais rápidos, executáveis em CI sem infraestrutura completa
- Smoke tests (`scripts/smoke-tests.sh`) devem incluir health checks de todos os 4 serviços

## Pré-requisitos (antes de começar)

**Extração anterior:**
- [ ] User service extraído e estável em produção (extração 1 concluída)
- [ ] Workspace service extraído e estável em produção (extração 2 concluída) — incluindo endpoints de templates `GET /workspaces/{id}/service-context-profiles/**`
- [ ] Proposal service extraído e estável em produção (extração 3 concluída) — incluindo consumer de `BriefingCompletedEvent`

**Refatoração do monólito (G-04.1 — obrigatório antes de qualquer implementação):**
- [ ] **Refatorar `BriefingSessionControllerV2`**: remover imports diretos de `JpaBriefingSession` e `JpaServiceContextQuestion`; `BriefingSessionService` deve retornar objetos de domínio ou DTOs de aplicação
- [ ] Adicionar ArchUnit test enforçando que `adapter.in` não importa `adapter.out`

**Dependências de dados:**
- [ ] FK `proposals.briefing_id` já removida (substituída por UUID + cache no Proposal service)
- [ ] Workspace service expondo `GET /workspaces/{id}/service-context-profiles` e `GET /workspaces/{id}/service-context-profiles/{profileId}/questions` (ADR-004)

**Novos clients de saída:**
- [ ] `WorkspaceServiceClient` implementado com Resilience4j (circuit breaker + retry + timeout + cache local com TTL para templates)
- [ ] `ProposalServiceClient` implementado com Resilience4j (circuit breaker + retry + timeout)

**Qualidade e infraestrutura:**
- [ ] **Circuit breaker implementado no OpenAI client** (Resilience4j — obrigatório antes do corte)
- [ ] Contract tests (Pact) entre Briefing e Proposal service (especialmente `BriefingCompletedEvent`)
- [ ] **Contract tests (Pact) entre Briefing e Workspace service** (templates de perguntas via `WorkspaceServiceClient`)
- [ ] Testes de carga nos endpoints públicos (rate limiting validado)
- [ ] Distributed tracing configurado (OpenTelemetry) — rastrear chamadas OpenAI, `WorkspaceServiceClient` e `ProposalServiceClient`
- [ ] Pipeline de CI/CD para o novo serviço (build, test, deploy)
- [ ] Decisão de routing do path `/api/v1/proposals/{proposalId}/briefing-sessions` no API Gateway (Briefing service, não Proposal service)
- [ ] Health check e monitoring configurados (incluindo métricas de OpenAI — latência, erros)
- [ ] Chaos engineering: validar comportamento quando OpenAI estiver indisponível (fallback), quando Workspace service estiver indisponível (templates — fallback com perguntas default?), quando Proposal service estiver indisponível (criação de briefing session)
