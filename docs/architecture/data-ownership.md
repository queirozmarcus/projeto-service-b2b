# Data Ownership — ScopeFlow AI

**Data:** 2026-04-05  
**Princípio:** Cada bounded context é **dono exclusivo** de suas tabelas. Nenhum contexto deve acessar tabelas de outro diretamente (via JOIN ou query cross-context).

---

## Princípio de Ownership

No ScopeFlow AI, seguimos a regra de **context isolation** para dados:

1. **Single-owner tables:** Cada tabela pertence a exatamente 1 bounded context
2. **No cross-context JOINs:** Contextos se comunicam via eventos ou APIs, nunca via JOIN SQL
3. **Foreign Keys cross-context:** Permitidas apenas para multi-tenancy (`workspace_id`) e relações 1:1 explícitas (ex: `proposals.briefing_id`)
4. **Referências sem FK:** IDs de outros contextos armazenados como UUID simples, sem constraint (ex: `briefing_sessions.client_id`)

---

## Ownership Map

| Bounded Context | Tabelas Owned | Tipo |
|----------------|---------------|------|
| **Briefing** | `briefing_sessions`, `briefing_questions`, `briefing_answers`, `ai_generations`, `briefing_activity_logs`, `service_context_profiles`, `service_context_questions` | Domain |
| **Workspace** | `workspaces`, `workspace_members` | Domain |
| **User** | `users` | Domain |
| **Proposal** | `proposals`, `proposal_versions`, `approval_workflows`, `approvals` | Domain |
| **Infrastructure** | `outbox_event`, `idempotency_record`, `activity_logs` | Shared |

**Total:** 4 bounded contexts de domínio + 1 infraestrutura compartilhada.

---

## Análise Detalhada por Contexto

### Briefing Context (7 tabelas)

**Owner:** Briefing  
**Responsabilidade:** Fluxo de descoberta AI-assisted — perguntas, respostas, análise AI, templates de serviço

| Tabela | Aggregate | Tipo | Observações |
|--------|-----------|------|-------------|
| `briefing_sessions` | BriefingSession (root) | Single-owner | ✅ Nenhum contexto externo acessa diretamente |
| `briefing_questions` | BriefingSession (child) | Single-owner | ✅ Cascade delete com session |
| `briefing_answers` | BriefingSession (child) | Single-owner | ✅ Imutável (trigger) |
| `ai_generations` | BriefingSession (audit) | Single-owner | ✅ Audit trail AI |
| `briefing_activity_logs` | BriefingSession (audit) | Single-owner | ✅ Audit trail de ações |
| `service_context_profiles` | ServiceContextProfile (root) | Single-owner | ✅ Configuração por workspace |
| `service_context_questions` | ServiceContextProfile (child) | Single-owner | ✅ Templates de perguntas |

**Conformidade:** ✅ **100%** — Nenhuma violação de ownership detectada.

**Referências externas:**
- `briefing_sessions.workspace_id` → `workspaces.id` (FK) — **Multi-tenancy esperada**
- `briefing_sessions.client_id` (UUID) — **Sem FK** — Client pode ser de outro contexto (futuro)

---

### Workspace Context (2 tabelas)

**Owner:** Workspace  
**Responsabilidade:** Tenant/organização — membros, configurações, multi-tenancy

| Tabela | Aggregate | Tipo | Observações |
|--------|-----------|------|-------------|
| `workspaces` | Workspace (root) | Single-owner | ✅ Acessado por todos os contextos via FK (`workspace_id`) |
| `workspace_members` | WorkspaceMember | Single-owner | ✅ Membership: User ↔ Workspace |

**Conformidade:** ✅ **100%**

**Referências externas:**
- `workspaces.owner_id` → `users.id` (FK) — **Relação 1:1 legítima** (owner é um User)
- `workspace_members.workspace_id` → `workspaces.id` (FK)
- `workspace_members.user_id` → `users.id` (FK) — **Relação M:N legítima**

**Nota crítica:** `workspaces` é **compartilhado por referência** (outros contextos armazenam `workspace_id`), mas o **ownership pertence ao Workspace context**. Nenhum outro contexto modifica `workspaces` diretamente.

---

### User Context (1 tabela)

**Owner:** User  
**Responsabilidade:** Identidade, autenticação, perfil

| Tabela | Aggregate | Tipo | Observações |
|--------|-----------|------|-------------|
| `users` | User (root) | Single-owner | ✅ Referenciado por `workspaces.owner_id` e `workspace_members.user_id` |

**Conformidade:** ✅ **100%**

**Referências externas:**
- Nenhuma FK saindo de `users` — é root do contexto.
- FKs entrando:
  - `workspaces.owner_id` → `users.id` (RESTRICT delete)
  - `workspace_members.user_id` → `users.id` (CASCADE delete)

---

### Proposal Context (4 tabelas)

**Owner:** Proposal  
**Responsabilidade:** Proposta formatada pós-briefing — versões, workflow de aprovação

| Tabela | Aggregate | Tipo | Observações |
|--------|-----------|------|-------------|
| `proposals` | Proposal (root) | Single-owner | ✅ Nenhuma violação |
| `proposal_versions` | Proposal (child) | Single-owner | ✅ Imutável (trigger) |
| `approval_workflows` | ApprovalWorkflow (root) | Single-owner | ✅ 1:1 com proposal |
| `approvals` | ApprovalWorkflow (child) | Single-owner | ✅ Decisões de aprovadores |

**Conformidade:** ✅ **100%**

**Referências externas:**
- `proposals.workspace_id` → `workspaces.id` (FK) — **Multi-tenancy**
- `proposals.briefing_id` → `briefing_sessions.id` (FK, RESTRICT delete) — **Cross-context FK legítima**
- `proposals.client_id` (UUID) — **Sem FK** (Client pode ser de contexto futuro)

**Nota:** `proposals.briefing_id` é uma FK cross-context **intencional**. Isso cria uma dependência Proposal → Briefing, mas é aceitável porque:
1. Proposal **nasce** de um Briefing completado (relação causal forte)
2. RESTRICT delete garante que Briefing não é deletado enquanto Proposal existir
3. Alternativa (event-driven) seria over-engineering para este caso

---

## Dependências de Dados (Foreign Keys)

### Multi-Tenancy (Esperadas)

Todas as tabelas de domínio têm `workspace_id` → `workspaces.id`:

| Contexto | Tabela | FK para Workspace | Ação ON DELETE |
|----------|--------|-------------------|----------------|
| Briefing | `briefing_sessions` | `workspace_id` | CASCADE |
| Briefing | `service_context_profiles` | `workspace_id` | CASCADE |
| Proposal | `proposals` | `workspace_id` | CASCADE |
| Workspace | `workspace_members` | `workspace_id` | CASCADE |

**Risco de acoplamento:** ✅ **Nulo** — Multi-tenancy é cross-cutting concern esperado.

---

### Cross-Context (Intencional)

| De (Contexto) | Para (Contexto) | FK | Ação ON DELETE | Justificativa |
|---------------|-----------------|-----|----------------|---------------|
| Workspace | User | `workspaces.owner_id` → `users.id` | RESTRICT | Workspace tem exatamente 1 owner (invariante) |
| Workspace | User | `workspace_members.user_id` → `users.id` | CASCADE | Membership M:N legítima |
| Proposal | Briefing | `proposals.briefing_id` → `briefing_sessions.id` | RESTRICT | Proposal nasce de Briefing completado |

**Risco de acoplamento:**
- **Baixo:** FKs são intencionais e representam relações causais fortes
- **RESTRICT** em `owner_id` e `briefing_id` garante integridade referencial sem permitir deleções em cascata perigosas

---

### Referências sem FK (Loose Coupling)

| Tabela | Campo | Referência | Por quê sem FK? |
|--------|-------|------------|-----------------|
| `briefing_sessions` | `client_id` | Client (contexto futuro?) | Client pode não estar no mesmo DB; event-driven sync planejado |
| `proposals` | `client_id` | Client (contexto futuro?) | Idem |
| `proposal_versions` | `created_by` | `users.id` | Audit trail — user pode ser deletado, mas versão persiste |

**Recomendação:** Quando Client virar bounded context próprio:
1. Manter `client_id` como UUID sem FK
2. Sincronizar via evento `ClientCreated` → cache local no Briefing/Proposal (denormalization intencional)
3. Ou API call síncrona se read-heavy

---

## Queries Cross-Context Detectadas

### Status: ✅ **Nenhuma violação detectada**

**Metodologia:** Grep em todos os repositories JPA por `JOIN`, `@Query` com native SQL, `@ManyToOne` cross-context.

**Resultado:**
- ✅ Nenhum JOIN entre tabelas de contextos diferentes detectado
- ✅ Todos os `@ManyToOne`/`@OneToMany` são intra-contexto (dentro do mesmo aggregate)
- ✅ Views (`v_workspace_members_active`, `v_briefing_sessions_active`) fazem JOINs **dentro do mesmo contexto**

**Exemplo de JOIN legítimo (intra-context):**
```sql
-- View: v_briefing_sessions_active (contexto Briefing)
SELECT bs.*, COUNT(bq.id) as total_questions, COUNT(ba.id) as answered_questions
FROM briefing_sessions bs
LEFT JOIN briefing_questions bq ON bs.id = bq.briefing_session_id
LEFT JOIN briefing_answers ba ON bq.id = ba.question_id
WHERE bs.status = 'IN_PROGRESS'
GROUP BY bs.id, ...;
```

**Por quê legítimo?** Todas as 3 tabelas (`briefing_sessions`, `briefing_questions`, `briefing_answers`) pertencem ao mesmo agregado (BriefingSession) dentro do contexto Briefing.

---

### Potencial Query Cross-Context (Futura)

**Cenário hipotético:** Se um desenvolvedor tentasse fazer:

```sql
-- ❌ VIOLAÇÃO: Query cross-context
SELECT b.*, p.proposal_name, p.status
FROM briefing_sessions b
JOIN proposals p ON p.briefing_id = b.id
WHERE b.workspace_id = ?;
```

**Por quê é violação?**
- `briefing_sessions` pertence ao contexto **Briefing**
- `proposals` pertence ao contexto **Proposal**
- JOIN cross-context cria acoplamento forte (mudança de schema em Briefing afeta Proposal)

**Solução correta:**
1. **Event-driven:** Proposal consome evento `BriefingCompletedEvent` e mantém cache local de `briefing_id` + `briefing_status`
2. **API call:** ProposalService chama `BriefingService.getBriefingById(briefingId)` quando precisar de dados do briefing
3. **Denormalização:** Proposal duplica campos críticos do Briefing (ex: `service_type`, `client_id`) no momento da criação

**Proteção atual:** Arquitetura hexagonal + repositories isolados por contexto impedem naturalmente JOINs cross-context.

---

## Conformidade com Bounded Contexts

### ✅ **Conformidade Total: 100%**

| Critério | Status | Observações |
|----------|--------|-------------|
| Cada tabela tem owner único | ✅ PASS | 17/17 tabelas classificadas |
| Nenhum JOIN cross-context | ✅ PASS | 0 violações detectadas |
| FKs cross-context apenas multi-tenancy | ✅ PASS | `workspace_id` em todas as tabelas de domínio |
| FKs cross-context intencionais documentadas | ✅ PASS | `owner_id`, `briefing_id` justificadas |
| Repositories isolados por contexto | ✅ PASS | 4 packages: `briefing`, `workspace`, `user`, `proposal` |

---

## Estratégia de Split para Microsserviços (Futuro)

Quando evoluir de monólito modular para microsserviços:

### Cenário A: 4 Microsserviços (1 por contexto)

```
┌─────────────────────────────────────────────────────┐
│ Briefing Service                                     │
│ - briefing_sessions, questions, answers, ai_gens    │
│ - service_context_profiles, questions               │
│ - DB: briefing_db                                    │
└─────────────────────────────────────────────────────┘
                     ↓ event: BriefingCompletedEvent
┌─────────────────────────────────────────────────────┐
│ Proposal Service                                     │
│ - proposals, proposal_versions                       │
│ - approval_workflows, approvals                      │
│ - DB: proposal_db                                    │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Workspace Service (Multi-tenancy)                    │
│ - workspaces, workspace_members                      │
│ - DB: workspace_db                                   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ User Service (Identity)                              │
│ - users                                              │
│ - DB: user_db (ou reaproveitado de auth provider)   │
└─────────────────────────────────────────────────────┘
```

**Dependências a resolver:**

1. **`proposals.briefing_id` → `briefing_sessions.id`**
   - **Solução:** Remover FK; Proposal armazena `briefing_id` como UUID simples
   - **Sincronização:** Evento `BriefingCompletedEvent` → Proposal cria cache local com `{briefing_id, service_type, client_id, completion_score}`
   - **Validação:** Proposal valida `briefing_id` via API call síncrona antes de criar proposal

2. **`workspaces.owner_id` → `users.id`**
   - **Solução:** Remover FK; Workspace armazena `owner_id` como UUID
   - **Sincronização:** Evento `UserRegisteredEvent` → Workspace cache local de `{user_id, email, full_name}`
   - **Validação:** Workspace valida `owner_id` via API call a User Service ao criar workspace

3. **`workspace_members.user_id` → `users.id`**
   - **Solução:** Idem acima; cache local de users no Workspace Service

4. **Multi-tenancy (`workspace_id` em todas as tabelas)**
   - **Solução:** JWT contém `workspaceId` claim; cada serviço valida workspace via API call a Workspace Service ou cache Redis
   - **Cache:** `GET /workspaces/{id}` → cache 1h no Redis

---

### Cenário B: 2 Microsserviços (Core + Support)

```
┌─────────────────────────────────────────────────────┐
│ Core Service (Briefing + Proposal)                   │
│ - Todos os agregados de Briefing e Proposal          │
│ - DB: core_db                                        │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Platform Service (Workspace + User)                  │
│ - workspaces, workspace_members, users               │
│ - DB: platform_db                                    │
└─────────────────────────────────────────────────────┘
```

**Vantagem:** Menos split overhead; FKs `proposals.briefing_id` permanecem intra-service.

**Desvantagem:** Core Service fica grande; menos autonomia.

---

### Recomendação: Cenário A (4 Microsserviços)

**Por quê?**
- Bounded contexts já estão bem definidos
- Ownership de dados é 100% claro
- FKs cross-context são mínimas e todas documentadas
- Event-driven já implementado (Outbox Pattern pronto)

**Quando fazer split?**
- Quando carga de Briefing e Proposal justificar scaling independente
- Quando equipes crescerem (1 equipe por serviço)
- Após atingir 10K+ workspaces ativos (problema de escala real)

---

## Checklist de Migração para Microsserviços

Quando decidir fazer split:

### 1. Pré-requisitos
- [ ] Event-driven já funcional (Outbox + RabbitMQ)
- [ ] Idempotency em todos os listeners
- [ ] Testes de contrato (Pact) entre contextos
- [ ] API Gateway configurado (routing por contexto)

### 2. Split de Dados
- [ ] Criar `briefing_db`, `proposal_db`, `workspace_db`, `user_db`
- [ ] Migrar schema (Flyway) de cada contexto para seu DB
- [ ] Remover FKs cross-context (`proposals.briefing_id`, `workspaces.owner_id`, `workspace_members.user_id`)
- [ ] Adicionar cache local em cada serviço para dados de referência

### 3. Sincronização
- [ ] Implementar eventos de sincronização:
  - `BriefingCompletedEvent` → Proposal Service
  - `UserRegisteredEvent` → Workspace Service
  - `WorkspaceCreatedEvent` → Todos os serviços (multi-tenancy)
- [ ] Implementar cache Redis para `workspaces` (1h TTL)

### 4. Validação
- [ ] Testes E2E multi-service (Testcontainers + Docker Compose)
- [ ] Chaos engineering: derrubar 1 serviço, verificar degradação graceful
- [ ] Validação de integridade: comparar contagens antes/depois do split

### 5. Cutover
- [ ] Blue-green deployment: rodar ambos (monólito + microsserviços) em paralelo por 1 semana
- [ ] Comparar respostas (shadow traffic)
- [ ] Cutover gradual: 10% → 50% → 100% tráfego para microsserviços

---

## Princípios de Ownership — Resumo

| Princípio | Status | Observações |
|-----------|--------|-------------|
| **Single-owner tables** | ✅ 100% | 17/17 tabelas classificadas |
| **No cross-context JOINs** | ✅ 100% | 0 violações detectadas |
| **FKs apenas multi-tenancy ou causais** | ✅ 100% | 3 FKs cross-context justificadas |
| **Event-driven para sync** | ✅ Pronto | Outbox Pattern implementado |
| **Cache local para referências** | ⚠️ Futuro | Implementar quando split ocorrer |

---

## Conclusão

O ScopeFlow AI está **pronto para migração para microsserviços** quando necessário:

1. **Ownership 100% claro** — Nenhuma tabela compartilhada entre contextos
2. **Zero queries cross-context** — Arquitetura hexagonal + repositories isolados
3. **Event-driven já implementado** — Outbox Pattern funcional
4. **FKs cross-context mínimas** — Apenas 3, todas justificadas e documentadas

**Próximos passos recomendados:**

1. **Curto prazo (3-6 meses):**
   - Monitorar `pg_stat_statements` — identificar queries lentas
   - Adicionar testes de contrato (Pact) entre Briefing ↔ Proposal
   - Implementar cache Redis para `workspaces` (preparação para split)

2. **Médio prazo (6-12 meses):**
   - Quando atingir 5K workspaces ativos ou 100K briefing sessions, reavaliar necessidade de split
   - Implementar API Gateway + service mesh (Istio)
   - Planejar split Briefing → Briefing Service (primeiro candidato por ser o mais isolado)

3. **Longo prazo (12+ meses):**
   - Split completo para 4 microsserviços conforme Cenário A
   - Migração de `outbox_event` para Kafka (maior escala)
   - Implementar CQRS/Event Sourcing se necessário (análise de briefings históricos)

---

**FIM DO DOCUMENTO**
