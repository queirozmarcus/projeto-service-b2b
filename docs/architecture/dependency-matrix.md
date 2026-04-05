# Matriz de Dependências — ScopeFlow AI

**Versão:** 1.0  
**Data:** 2026-04-05  
**Autor:** Domain Analyst (Claude Code Agent)

## Matriz de Dependências Entre Contextos

|                | **Briefing** | **Proposal** | **User** | **Workspace** |
|----------------|--------------|--------------|----------|---------------|
| **Briefing**   | —            | —            | —        | data          |
| **Proposal**   | data         | —            | —        | data          |
| **User**       | —            | —            | —        | —             |
| **Workspace**  | —            | —            | data     | —             |

**Legenda:**
- `sync`: Chamada direta (use case → use case) entre contextos
- `event`: Comunicação via domain event assíncrono
- `data`: Dependência via foreign key ou referência a value object de outro contexto
- `—`: Sem dependência

---

## Detalhamento das Dependências

### Briefing → Workspace
- **Tipo:** `data` (foreign key via value object)
- **Como:** `BriefingSession` guarda `WorkspaceId` como campo obrigatório
- **Por quê:** Multi-tenancy — todo briefing pertence a um workspace. Queries sempre filtram por `workspaceId`.
- **Direção:** Unidirecional (Briefing conhece Workspace, não o contrário)
- **Acoplamento:** Baixo — apenas referência a value object `WorkspaceId`, sem chamar serviços do Workspace

**Código:**
```java
// core/domain/briefing/BriefingSession.java
private final WorkspaceId workspaceId;

// BriefingService valida workspace no método findByIdAndWorkspace
if (!session.getWorkspaceId().equals(workspaceId)) {
    throw new AccessDeniedException("Briefing does not belong to authenticated workspace");
}
```

---

### Proposal → Briefing
- **Tipo:** `data` (foreign key via value object)
- **Como:** `Proposal` guarda `BriefingSessionId` como origem da proposta
- **Por quê:** Proposta é criada a partir de um briefing completado. Rastreabilidade: qual briefing originou esta proposta.
- **Direção:** Unidirecional (Proposal conhece Briefing, não o contrário)
- **Acoplamento:** Baixo — apenas referência a `BriefingSessionId`, sem chamadas diretas

**Código:**
```java
// core/domain/proposal/Proposal.java
private final BriefingSessionId briefingId;

// ProposalService.createProposal()
ProposalDraft draft = Proposal.create(workspaceId, clientId, briefingId, proposalName);
```

**Nota:** Não há validação de que o briefing existe/está completado no momento da criação da proposta. Isso seria uma possível melhoria (validar via port de consulta ou event listener).

---

### Proposal → Workspace
- **Tipo:** `data` (foreign key via value object)
- **Como:** `Proposal` guarda `WorkspaceId` para multi-tenancy
- **Por quê:** Todo proposal pertence ao workspace que criou o briefing original
- **Direção:** Unidirecional (Proposal conhece Workspace, não o contrário)
- **Acoplamento:** Baixo — apenas referência a `WorkspaceId`

**Código:**
```java
// core/domain/proposal/Proposal.java
private final WorkspaceId workspaceId;

// ProposalRepository.findByIdAndWorkspaceId() valida ownership
public Optional<Proposal> findByIdAndWorkspaceId(ProposalId id, WorkspaceId workspaceId);
```

---

### Workspace → User
- **Tipo:** `data` (foreign key via value object)
- **Como:** 
  - `Workspace` guarda `UserId` do owner (1:1 obrigatório)
  - `WorkspaceMember` guarda `UserId` para relacionamento N:N
- **Por quê:** 
  - Todo workspace tem um owner (usuário criador)
  - Membros do workspace são usuários com roles específicas (OWNER, ADMIN, MEMBER, VIEWER)
- **Direção:** Unidirecional (Workspace conhece User, não o contrário)
- **Acoplamento:** Baixo — apenas referências a `UserId`

**Código:**
```java
// core/domain/workspace/Workspace.java
private final UserId ownerId;

// core/domain/workspace/WorkspaceMember.java
private final UserId userId;
private final WorkspaceId workspaceId;
private final Role role;
```

---

## Comunicação Assíncrona via Domain Events

### Briefing → (Listeners)
**Evento:** `BriefingCompletedEvent`  
**Publicado por:** `BriefingInProgress.completeBriefing()` (via Outbox)  
**Consumido por:** `BriefingCompletedListener`

**Responsabilidade do Listener:**
- Fallback: gerar perguntas adicionais via IA caso não tenham sido geradas durante a sessão
- Futura integração: disparar criação automática de proposta draft

**Fluxo:**
1. `BriefingService.completeBriefing()` → salva `BriefingCompleted` + publica evento
2. Evento salvo em `outbox_events` (mesma transação)
3. `OutboxEventPublisher` (scheduled @5s) publica para RabbitMQ
4. `BriefingCompletedListener` consome da fila `briefing.completed`
5. Idempotency check: já processado? Sim → skip. Não → processar.
6. Marca como processado em `idempotency_events`

**Código:**
```java
// application/listener/BriefingCompletedListener.java
@RabbitListener(queues = "briefing.completed")
@Transactional
public void onBriefingCompleted(BriefingCompletedEvent event) {
    String idempotencyKey = "briefing-" + event.sessionId();
    if (idempotencyService.isProcessed(LISTENER_ID, idempotencyKey)) {
        return; // Já processado
    }
    // TODO: chamar IA para gerar perguntas (fallback)
    idempotencyService.markAsProcessed(LISTENER_ID, idempotencyKey);
}
```

---

### Proposal → (Listeners)
**Evento:** `ProposalApprovedEvent`  
**Publicado por:** `ProposalPublished.approve()` (via Outbox)  
**Consumido por:** `ProposalApprovalListener`

**Responsabilidade do Listener:**
1. Gerar PDF da proposta (iText)
2. Upload do PDF para S3
3. Enviar email de aprovação ao cliente com link do PDF

**Fluxo:**
1. Cliente aprova proposta via link público
2. `ProposalService.recordApproval()` → transiciona para `ProposalApproved` + publica evento
3. Evento salvo em `outbox_events`
4. `OutboxEventPublisher` publica para RabbitMQ
5. `ProposalApprovalListener` consome da fila `proposal.approved`
6. Idempotency check: já processado? Sim → skip. Não → gerar PDF + enviar email
7. Marca como processado com `resultData` (pdfUrl + emailSent)

**Código:**
```java
// application/listener/ProposalApprovalListener.java
@RabbitListener(queues = "proposal.approved")
@Transactional
public void onProposalApproved(ProposalApprovedEvent event) {
    String idempotencyKey = "proposal-" + event.proposalId() + ":" + event.occurredAt().toEpochMilli();
    if (idempotencyService.isProcessed(LISTENER_ID, idempotencyKey)) {
        return; // Já processado
    }
    String pdfUrl = pdfService.generateProposalPdf(event.proposalId(), GenerationContext.APPROVAL);
    emailService.sendProposalApprovedEmail(event.clientEmail(), pdfUrl, event.proposalId());
    idempotencyService.markAsProcessed(LISTENER_ID, idempotencyKey, "{\"pdfUrl\":\"" + pdfUrl + "\"}");
}
```

---

### User → (Listeners)
**Evento:** `UserRegisteredEvent`  
**Publicado por:** `UserService.register()` (via Outbox)  
**Consumido por:** `UserRegistrationListener`

**Responsabilidade do Listener:**
- Enviar welcome email ao novo usuário

**Fluxo:**
1. Novo usuário completa registro
2. `UserService.register()` → cria `UserActive` + publica evento
3. Evento salvo em `outbox_events`
4. `OutboxEventPublisher` publica para RabbitMQ
5. `UserRegistrationListener` consome da fila `user.registered`
6. Idempotency check: já processado? Sim → skip. Não → enviar email
7. Marca como processado

**Código:**
```java
// application/listener/UserRegistrationListener.java
@RabbitListener(queues = "user.registered")
@Transactional
public void onUserRegistered(UserRegisteredEvent event) {
    String idempotencyKey = "user-" + event.userId();
    if (idempotencyService.isProcessed(LISTENER_ID, idempotencyKey)) {
        return; // Já processado
    }
    emailService.sendWelcomeEmail(event.email(), event.fullName(), event.workspaceId());
    idempotencyService.markAsProcessed(LISTENER_ID, idempotencyKey);
}
```

---

## Side Effects (Jobs Agendados)

### OutboxEventPublisher (Scheduled Job)
- **Frequência:** A cada 5 segundos (fixedDelay = 5000ms, initialDelay = 1000ms)
- **Responsabilidade:** 
  - Buscar eventos não publicados da tabela `outbox_events`
  - Desserializar JSON → domain event object
  - Publicar para RabbitMQ via `ApplicationEventPublisher`
  - Marcar como publicado (`published_at = now()`)
- **Transacional:** Sim (@Transactional)
- **Error Handling:** Falhas não marcam evento como publicado → retry no próximo ciclo

**Código:**
```java
// application/outbox/OutboxEventPublisher.java
@Scheduled(fixedDelay = 5000, initialDelay = 1000)
@Transactional
public void publishPendingEvents() {
    List<OutboxEvent> events = outboxRepository.findUnpublished();
    for (OutboxEvent outboxEvent : events) {
        Object domainEvent = objectMapper.readValue(outboxEvent.getPayload(), Class.forName(outboxEvent.getEventType()));
        applicationEventPublisher.publishEvent(domainEvent);
        outboxEvent.markAsPublished();
        outboxRepository.save(outboxEvent);
    }
}
```

**Impacto:**
- Cross-cutting concern: afeta todos os contextos que publicam eventos
- Não bloqueia transação principal (eventos publicados de forma assíncrona)
- Latência: até 5s entre evento salvo e publicação no RabbitMQ

---

## Transações Cruzadas

### Análise: Existem SAGAs Implícitas?

**Cenário 1: Registro de Usuário + Criação de Workspace**
- **Atualmente:** Não há evidência de transação atômica entre User e Workspace
- **Provável implementação:** 
  1. Criar `UserActive`
  2. Criar `WorkspaceActive` com `ownerId = userId`
  3. Se falhar criar workspace → rollback user? **Não verificado no código analisado.**
- **Recomendação:** Implementar SAGA ou transação local (se ambos na mesma DB) para garantir consistência

**Cenário 2: Aprovação de Proposta → PDF + Email**
- **Padrão:** Async via Event Listener (não é transação cruzada, é compensação assíncrona)
- **Falhas tratadas por:** 
  - RabbitMQ retry (3 tentativas)
  - DLQ (Dead Letter Queue) para erros persistentes
  - Idempotency garante que não haverá duplicatas

**Cenário 3: Completar Briefing → Gerar Proposta**
- **Atualmente:** Não há criação automática de proposta ao completar briefing
- **Se implementado:** Seria via Event Listener (`BriefingCompletedEvent` → criar `ProposalDraft`)
- **Não é SAGA:** Operações são independentes, falha na criação de proposta não invalida briefing

**Conclusão:** Não há SAGAs explícitas implementadas. Dependências são via foreign keys (data) ou eventos assíncronos com compensação via listeners. Transações locais por agregado são suficientes para o modelo atual.

---

## Padrões de Integração

### Outbox Pattern (Exactly-Once Delivery)
**Implementação:**
1. Domain event salvo na tabela `outbox_events` (mesma transação do agregado)
2. Commit da transação → evento garantidamente persistido
3. `OutboxEventPublisher` scheduled job publica para RabbitMQ
4. Marca evento como `published_at = now()`
5. Listeners consomem com idempotency check

**Vantagens:**
- Garantia de entrega mesmo com falha do RabbitMQ durante transação
- Não bloqueia transação principal (publicação assíncrona)
- Audit trail completo de eventos publicados

**Desvantagens:**
- Latência de até 5s entre persistência e publicação
- Tabela `outbox_events` cresce indefinidamente (precisa de purge)

### Idempotency Pattern (At-Most-Once Processing)
**Implementação:**
1. Listener recebe evento do RabbitMQ
2. Calcula `idempotencyKey = "{listener-id}:{entity-id}"`
3. Verifica se já processado em `idempotency_events`
4. Se sim → skip (retorna 200 para ACK da mensagem)
5. Se não → processa + marca como processado

**Vantagens:**
- Proteção contra redelivery do RabbitMQ (rede instável, timeout)
- Proteção contra retry manual de mensagens
- Permite reprocessamento seguro de DLQ

**Desvantagens:**
- Tabela `idempotency_events` cresce indefinidamente (precisa de TTL/purge)
- Chave mal calculada pode causar falsos positivos

---

## Pontos de Atenção Arquiteturais

### 1. Ausência de Validação de Integridade Referencial
**Problema:** `Proposal.create()` aceita `BriefingSessionId` sem validar se o briefing existe ou está completado.

**Risco:** Proposta órfã se briefing for deletado ou se `briefingId` for UUID inválido.

**Solução sugerida:**
- Validar via repository port: `briefingRepository.exists(briefingId)` antes de criar proposta
- OU via event listener: `BriefingCompletedEvent` → criar proposta automaticamente (garante briefing completado)

### 2. Crescimento Ilimitado de Tabelas de Auditoria
**Tabelas afetadas:**
- `outbox_events` (todos os eventos publicados)
- `idempotency_events` (todos os eventos processados)
- `ai_generations` (todas as chamadas à OpenAI)

**Risco:** Performance degradada, storage crescente

**Solução sugerida:**
- Implementar purge job (ex: deletar eventos publicados há > 30 dias)
- TTL em `idempotency_events` (ex: 7 dias — redelivery tardio é edge case)
- Archive `ai_generations` para S3 (data lake) após 90 dias

### 3. Falta de Circuit Breaker em Integrações Externas
**Serviços externos:**
- OpenAI API (geração de perguntas/follow-ups)
- AWS S3 (upload de PDFs)
- AWS SES (envio de emails)

**Risco:** Falha em cascata se serviço externo ficar lento/down

**Solução sugerida:**
- Implementar Resilience4j circuit breaker nos adapters (`PdfService`, `EmailService`, `AIAssistantPort`)
- Fallback: retry com backoff exponencial (já implementado via RabbitMQ)
- Timeout configurável (atualmente 30s hardcoded em listeners)

### 4. Multi-Tenancy: Risco de Data Leak
**Mitigação atual:**
- Compound indexes: `(workspace_id, id)` em todas as tabelas
- Queries sempre filtram por `workspaceId` (validado em `findByIdAndWorkspace`)
- JWT contém `workspaceId` claim → extraído por `SecurityUtil`

**Risco residual:**
- Se query esquecer filtro de workspace → leak de dados entre tenants
- Se JWT for forjado com `workspaceId` de outro tenant → acesso não autorizado

**Solução sugerida:**
- Database-level RLS (Row-Level Security) no PostgreSQL como camada adicional
- Audit log de acessos cross-workspace (detectar anomalias)
- Testes de integração validando isolamento (ex: tentativa de acessar briefing de outro workspace)

---

## Resumo Executivo

**Arquitetura atual:**
- **4 bounded contexts** bem definidos com responsabilidades claras
- **Acoplamento baixo**: dependências via value objects, sem chamadas diretas entre contextos
- **Comunicação assíncrona** via Domain Events (Outbox + RabbitMQ)
- **Padrões robustos**: Outbox (exactly-once), Idempotency (at-most-once), Multi-tenancy (workspace filtering)

**Tipo de acoplamento dominante:** `data` (foreign keys via value objects)  
**Comunicação entre contextos:** Predominantemente event-driven (3 listeners ativos)

**Principais riscos identificados:**
1. Validação de integridade referencial ausente (Proposal → Briefing)
2. Crescimento ilimitado de tabelas de auditoria
3. Falta de circuit breakers em integrações externas
4. Multi-tenancy sem camada adicional de proteção (RLS)

**Recomendações prioritárias:**
1. Implementar validação de `BriefingSessionId` ao criar proposta
2. Adicionar purge job para `outbox_events` e `idempotency_events`
3. Implementar circuit breaker com Resilience4j
4. Adicionar testes de integração para isolamento multi-tenant

