# Matriz de Acoplamento — ScopeFlow AI

**Data:** 2026-04-05  
**Versão:** 1.0

## Visão Geral

Matriz consolidada de dependências entre bounded contexts do ScopeFlow AI. Cruzamento de análises de domínio, dados e segurança.

---

## Matriz de Acoplamento (Resumida)

|                | **Briefing** | **Proposal** | **Workspace** | **User** |
|----------------|--------------|--------------|---------------|----------|
| **Briefing**   | —            | —            | data          | —        |
| **Proposal**   | data         | —            | data          | —        |
| **Workspace**  | —            | —            | —             | data     |
| **User**       | —            | —            | —             | —        |

**Legenda:**
- `data`: Dependência via foreign key (value object UUID)
- `event`: Comunicação via domain event (assíncrona)
- `—`: Sem dependência direta

**Total de dependências:** 4 (todas via `data`)

---

## Detalhamento por Relação

### Briefing → Workspace

**Tipo:** `data` (FK via value object)

**Como:**
- `BriefingSession` agrega `WorkspaceId` (value object)
- Tabela: `briefing_sessions.workspace_id` → `workspaces.id` (FK ON DELETE RESTRICT)

**Por quê:**
- Multi-tenancy obrigatório — todo briefing pertence a um workspace
- Autorização: usuário só acessa briefings do próprio workspace

**Risco de acoplamento:** BAIXO (FK esperada em multi-tenancy)

**Estratégia de desacoplamento (se extrair):**
- Briefing Service mantém `workspace_id` como campo, mas sem FK
- Validação de existência via API call síncrona no create
- Event-driven: `WorkspaceDeletedEvent` → soft delete briefings órfãos

---

### Proposal → Briefing

**Tipo:** `data` (FK via value object)

**Como:**
- `Proposal` agrega `BriefingSessionId` (value object)
- Tabela: `proposals.briefing_id` → `briefing_sessions.id` (FK ON DELETE RESTRICT)

**Por quê:**
- Proposta nasce de briefing completado (relação causal forte)
- Rastreabilidade: toda proposta tem origem em um briefing

**Risco de acoplamento:** MÉDIO (relação 1:1 forte)

**Estratégia de desacoplamento (se extrair):**
- Remover FK → Proposal mantém `briefing_id` como campo UUID
- `BriefingCompletedEvent` → Proposal Service cria proposta automaticamente
- Validação de existência: cache Redis com TTL 24h (briefing completado)
- Trade-off: Integridade referencial perde garantia de DB, ganha via event

---

### Proposal → Workspace

**Tipo:** `data` (FK via value object)

**Como:**
- `Proposal` agrega `WorkspaceId` (value object)
- Tabela: `proposals.workspace_id` → `workspaces.id` (FK ON DELETE RESTRICT)

**Por quê:**
- Multi-tenancy obrigatório — toda proposta pertence a um workspace
- Autorização: usuário só acessa propostas do próprio workspace

**Risco de acoplamento:** BAIXO (FK esperada em multi-tenancy)

**Estratégia de desacoplamento (se extrair):**
- Mesma estratégia de Briefing → Workspace

---

### Workspace → User

**Tipo:** `data` (FK via value object)

**Como:**
- `Workspace` agrega `UserId owner` (value object)
- Tabela: `workspaces.owner_id` → `users.id` (FK ON DELETE RESTRICT)

**Por quê:**
- Todo workspace tem um owner (invariante de negócio)
- Ownership usado para autorização (só OWNER pode deletar workspace)

**Risco de acoplamento:** MÉDIO (relação 1:N forte)

**Estratégia de desacoplamento (se extrair):**
- Workspace Service mantém `owner_id` como campo UUID
- Validação de existência via User Service API no create
- Cache Redis de `user_id → user_status` (TTL 1h)
- Trade-off: Latência +50ms na criação de workspace (validação síncrona)

---

## Comunicação Assíncrona (Domain Events)

### Eventos Publicados

| Contexto | Evento | Listener | Ação |
|----------|--------|----------|------|
| Briefing | `BriefingCompletedEvent` | `BriefingCompletedListener` | Gera perguntas de fallback se score < 80% |
| Proposal | `ProposalApprovedEvent` | `ProposalApprovalListener` | Gera PDF + envia email |
| User | `UserRegisteredEvent` | `UserRegistrationListener` | Envia email de boas-vindas |

**Outbox Pattern:** Todos os eventos passam por `outbox_event` antes de RabbitMQ (exactly-once delivery).

**Idempotency:** Listeners usam `IdempotencyService` para garantir at-most-once processing.

---

## Side Effects Globais

### Scheduled Jobs

| Job | Frequência | Contexto | Ação |
|-----|-----------|----------|------|
| `OutboxEventPublisher` | 5 segundos | Infra | Publica eventos de `outbox_event` → RabbitMQ |

### Filters

| Filter | Escopo | Ação |
|--------|--------|------|
| `JwtAuthenticationFilter` | Todos os endpoints | Valida JWT → carrega `SecurityContext` |

---

## Análise de Coesão Interna

### Briefing (Coesão ALTA ✅)
- **1 aggregate root**, 3 estados sealed, transações locais
- **Invariantes respeitados:** Status flow validado, score calculation interno
- **Nenhuma transação cross-aggregate**

### Proposal (Coesão ALTA ✅)
- **1 aggregate root**, 4 estados sealed, transações locais
- **Invariantes respeitados:** Approval workflow validado, versioning imutável
- **Nenhuma transação cross-aggregate**

### Workspace (Coesão MÉDIA ⚠️)
- **2 aggregates** (`Workspace`, `WorkspaceMember`) — risco de transação cruzada
- **Análise:** Criação de workspace + owner como member → **não verificado se é atômica**
- **Recomendação:** Validar se `WorkspaceService.create()` usa `@Transactional` para garantir atomicidade

### User (Coesão ALTA ✅)
- **1 aggregate root**, 3 estados sealed, transações locais
- **Invariantes respeitados:** Email unique, password hashed
- **Nenhuma transação cross-aggregate**

---

## Pontos de Atenção

### 1. Validação de Integridade Referencial Ausente

**Problema:** `Proposal.create()` aceita `BriefingSessionId` sem validar existência/completude do briefing.

**Risco:** Proposta órfã.

**Fix:**
```java
// No ProposalService
Optional<BriefingSession> briefing = briefingRepository.findById(briefingId);
if (briefing.isEmpty() || !briefing.get().isCompleted()) {
    throw new BriefingNotCompletedException(briefingId);
}
```

### 2. Workspace Creation: Transação Cruzada Não Verificada

**Problema:** Não confirmado se criação de workspace + owner como member é atômica.

**Risco:** Workspace sem owner em `workspace_members` → autorização quebrada.

**Fix:** Validar que `WorkspaceService.create()` usa `@Transactional`.

### 3. Event Replay: Sem Idempotência em Todos os Listeners

**Problema:** `BriefingCompletedListener` usa idempotency, mas `UserRegistrationListener` não foi validado.

**Risco:** Email de boas-vindas duplicado.

**Fix:** Adicionar `@IdempotentEventHandler` em todos os listeners ou validar manualmente.

---

## Estratégia de Migração para Microsserviços

### Cenário Recomendado: 4 Microsserviços

```
┌──────────────┐
│ User Service │ (auth, users)
└──────┬───────┘
       │ event: UserRegisteredEvent
       ▼
┌───────────────────┐
│ Workspace Service │ (multi-tenancy)
└─────┬─────────────┘
      │ event: WorkspaceCreatedEvent
      ▼
┌─────────────────┐        event: BriefingCompletedEvent        ┌──────────────────┐
│ Briefing Service├────────────────────────────────────────────▶│ Proposal Service │
└─────────────────┘                                             └──────────────────┘
```

### Ordem de Extração Sugerida

1. **User Service** (menor acoplamento, baixo volume write)
2. **Workspace Service** (depende de User, baixo volume write)
3. **Briefing Service** (alto volume, isolável)
4. **Proposal Service** (depende de Briefing, pode esperar event)

### Gatilhos para Migração

Considerar migração quando **2 ou mais** forem verdadeiros:
- ✅ Escala: 10K+ workspaces OU 100K+ briefing sessions
- ✅ Performance: Briefing impacta latência de Proposal
- ✅ Equipe: 8+ devs (1 squad por serviço)
- ✅ Deploy: Necessidade de deploy independente
- ✅ Resiliência: Failure isolation (Briefing down ≠ Proposal down)

**Status atual:** Monólito modular apropriado até 5K workspaces (~2-3 anos).

---

## Conclusão

**Acoplamento total:** BAIXO

- **4 dependências via data (FKs)** — todas justificadas
- **3 dependências via eventos** — assíncronas, desacopladas
- **Zero chamadas síncronas diretas** entre contextos
- **Coesão interna:** Alta em 3 de 4 contextos (Workspace precisa validação)

**Veredicto:** Arquitetura hexagonal + DDD bem implementada. Projeto pronto para migrar quando escala exigir.
