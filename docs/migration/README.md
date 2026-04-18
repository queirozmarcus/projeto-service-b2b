# Documentação de Migração — ScopeFlow AI

**Data:** 2026-04-17 (atualizado)
**Status:** User Service ✅ Extraído — staging ativo com DB-per-service | Sprint 10 ✅ — testes refatorados

---

## Índice de Documentos

Este diretório (`docs/migration/`) contém toda a documentação de análise, decisões e plano de execução para a migração do monólito ScopeFlow AI para microsserviços via Strangler Fig.

### Decisões Arquiteturais (ADRs)

| ADR | Título | Status |
|-----|--------|--------|
| [ADR-001](adr/ADR-001-ordem-de-extracao-bounded-contexts.md) | Ordem de extração dos bounded contexts | ✅ Aplicado |
| [ADR-002](adr/ADR-002-database-strategy-shared-inicial.md) | Database strategy — shared inicial → DB-per-service em staging | ✅ Aplicado |
| [ADR-003](adr/ADR-003-comunicacao-entre-servicos.md) | Comunicação entre serviços — REST síncrono via UserServiceRestAdapter | ✅ Aplicado |
| [ADR-004](adr/ADR-004-ownership-service-context.md) | Ownership de `service_context_profiles` / `service_context_questions` | ✅ Documentado |

### Análise de Acoplamento

- **[`../architecture/coupling-matrix.md`](../architecture/coupling-matrix.md)**
  - Matriz de dependências entre os 4 bounded contexts
  - Score de acoplamento por contexto (chamadas diretas, dados compartilhados, eventos)
  - Zonas de ambiguidade e recomendações de desacoplamento

### Extraction Cards (Plano de Execução)

Sequência de extração definida pelo ADR-001. Cada card descreve pré-requisitos, steps, rollback e critérios de aceite para um contexto.

| Prioridade | Contexto | Tabelas | Risco | Status | Notas |
|-----------|----------|---------|-------|--------|-------|
| 1 | [User (Auth)](extraction-cards/01-user-auth.md) | 1 | Baixo | ✅ **Extraído — staging ativo** | DB-per-service em `scopeflow_users`; cut-over produção pendente |
| 2 | [Workspace](extraction-cards/02-workspace.md) | 2 | Médio | 🔄 Próxima extração | Aguarda user-service estável em produção |
| 3 | [Proposal](extraction-cards/03-proposal.md) | 4 | Médio | ⏳ Backlog | Depende: Workspace extraído |
| 4 | [Briefing](extraction-cards/04-briefing.md) | 7 | Alto | ⏳ Backlog | Depende: Proposal extraído; circuit breaker OpenAI pendente (Phase 4) |

### Análise de Dados

1. **[`../architecture/schema-inventory.md`](../architecture/schema-inventory.md)** (31 KB)
   - Inventário completo do schema PostgreSQL (17 tabelas)
   - Detalhamento de cada tabela: colunas, índices, FKs, invariantes, volumes
   - Views, funções, procedures
   - Estimativas de crescimento e recomendações de performance

2. **[`../architecture/data-ownership.md`](../architecture/data-ownership.md)** (24 KB)
   - Mapeamento de ownership por bounded context (Briefing, Workspace, User, Proposal)
   - Análise de dependências (FKs cross-context)
   - Conformidade: 100% — Zero violações de ownership detectadas
   - Estratégia de split para microsserviços (Cenário A: 4 microsserviços)
   - Checklist de migração com 5 fases

---

## Contextos de Migração

Este diretório segue o **ANEXO VII — Migration Pack** do ecossistema Claude Code, implementando o processo **Strangler Fig** para migração de monólito modular para microsserviços.

### Contextos Inventoriados

| Contexto | Tabelas | Tipo de Dados | Crescimento | Pronto para Split? |
|----------|---------|---------------|-------------|-------------------|
| **Briefing** | 7 | Transacional (sessões, perguntas, respostas, AI) | Alto | ✅ Sim |
| **Workspace** | 2 | Multi-tenancy (workspaces, membros) | Médio | ✅ Sim |
| **User** | 1 | Identidade (autenticação) | Alto | ✅ Sim |
| **Proposal** | 4 | Transacional (propostas, versões, aprovações) | Médio | ✅ Sim |
| **Infrastructure** | 3 | Outbox, idempotency, audit trail | Alto (churn) | Compartilhado |

**Total:** 17 tabelas de domínio e infraestrutura.

---

## Resumo Executivo

### Quantidades

- **Total de tabelas:** 17 (15 de domínio + 2 de infraestrutura)
- **Bounded contexts:** 4 (Briefing, Workspace, User, Proposal)
- **Foreign Keys cross-context:** 3 (todas justificadas e documentadas)
- **Queries cross-context detectadas:** 0 (zero violações)
- **Conformidade de ownership:** 100%

### Distribuição por Contexto

```
Briefing:       7 tabelas (41%)  ███████████████
Proposal:       4 tabelas (24%)  ████████
Workspace:      2 tabelas (12%)  ████
User:           1 tabela  ( 6%)  ██
Infrastructure: 3 tabelas (18%)  ██████
```

### Tabelas Compartilhadas

**Nenhuma.** Todas as tabelas de domínio têm owner único. As 3 tabelas de infraestrutura (`outbox_event`, `idempotency_record`, `activity_logs`) são compartilhadas por design (cross-cutting concerns).

### Foreign Keys Cross-Context

| De → Para | FK | Tipo | Justificativa |
|-----------|-----|------|---------------|
| Workspace → User | `workspaces.owner_id` | RESTRICT | Workspace tem 1 owner (invariante de domínio) |
| Workspace → User | `workspace_members.user_id` | CASCADE | Membership M:N legítima |
| Proposal → Briefing | `proposals.briefing_id` | RESTRICT | Proposal nasce de Briefing completado (relação causal forte) |

**Risco de acoplamento:** ✅ **Baixo** — FKs são intencionais e representam relações causais documentadas. RESTRICT em owner e briefing impede deleções em cascata perigosas.

### Pontos de Atenção de Ownership

1. ✅ **Conformidade total** — Nenhuma violação detectada
2. ✅ **Repositories isolados** — Cada contexto tem package próprio (`briefing`, `proposal`, `workspace`, `user`)
3. ✅ **Event-driven pronto** — Outbox Pattern implementado; todos os agregados publicam eventos
4. ⚠️ **Referências sem FK** — `briefing_sessions.client_id` e `proposals.client_id` são UUIDs sem FK (Client pode virar bounded context no futuro)
5. ⚠️ **Overlap de audit** — `activity_logs` (global) vs `briefing_activity_logs` (específica) — considerar consolidação futura

---

## Caminho de Leitura Recomendado

Para quem chega nesta pasta pela primeira vez:

- **Entender o contexto geral** → este README
- **Entender as dependências entre contextos** → [`../architecture/coupling-matrix.md`](../architecture/coupling-matrix.md)
- **Entender as decisões arquiteturais** → [`adr/ADR-001`](adr/ADR-001-ordem-de-extracao-bounded-contexts.md), [`ADR-002`](adr/ADR-002-database-strategy-shared-inicial.md), [`ADR-003`](adr/ADR-003-comunicacao-entre-servicos.md)
- **Executar a migração** → [`extraction-cards/`](extraction-cards/) na ordem 01 → 02 → 03 → 04
- **Entender o schema e ownership de dados** → [`../architecture/schema-inventory.md`](../architecture/schema-inventory.md) + [`../architecture/data-ownership.md`](../architecture/data-ownership.md)

---

## Quando Fazer Split para Microsserviços?

### Gatilhos Recomendados

Considere migração quando **2+ critérios** forem verdadeiros:

1. **Escala:** 10K+ workspaces ativos OU 100K+ briefing sessions
2. **Performance:** Queries de Briefing impactam latência de Proposal (contenção de recursos)
3. **Equipe:** Time cresce para 8+ devs (1 squad por serviço)
4. **Deploy:** Necessidade de deploy independente (Briefing muda 2x/semana, Proposal 1x/mês)
5. **Resiliência:** Failure de Briefing não pode derrubar Proposal (isolation)

**Status atual:** Monólito modular é apropriado até atingir 5K workspaces (~2-3 anos de crescimento).

---

## Estratégia de Split Recomendada

### Cenário A: 4 Microsserviços (1 por contexto)

```
Briefing Service ─┐
                  ├─→ RabbitMQ (eventos) ←─→ Proposal Service
Workspace Service ─┤
User Service ─────┘
```

**Ordem de split sugerida:**

1. **Briefing Service** (primeiro)
   - Mais isolado (0 FKs saindo, 1 FK entrando de Proposal)
   - Alto volume de writes (ai_generations, activity_logs)
   - Benefício imediato: scaling independente

2. **Proposal Service** (segundo)
   - Remove FK `briefing_id` → evento `BriefingCompletedEvent`
   - Cache local de briefings referenciados

3. **User + Workspace Service** (juntos ou separados)
   - Low-churn (reads > writes)
   - Cache Redis para multi-tenancy checks

### Cenário B: 2 Microsserviços (Core + Platform)

```
Core Service (Briefing + Proposal) ←─→ Platform Service (Workspace + User)
```

**Vantagem:** Menos overhead de split; FK `proposals.briefing_id` permanece intra-service.  
**Desvantagem:** Core Service fica grande; menor autonomia.

**Recomendação:** Cenário A quando atingir critérios acima.

---

## Próximos Passos

### Concluídos ✅

- [x] Inventário de schema completo
- [x] Mapeamento de ownership e coupling matrix
- [x] Extraction cards criados (01–04)
- [x] ADRs 001–004 documentados e aplicados
- [x] **Card 01 (User Auth):** extraído — 20/20 sprints. DB-per-service em staging. Traefik routing ativo.
- [x] `GET /users/by-email/{email}` e `POST /users/invited` implementados no monólito
- [x] `UserServiceClient` (port) + `UserServiceRestAdapter` com `@CircuitBreaker` + `@Retry`
- [x] `WorkspaceControllerV2` refatorado para usar `UserServiceClient`

### Próximos Passos

- [ ] **Cut-over produção (card 01):** ver `.claude/plans/backlog/2026-04-12-migration-fase3-cutover-producao.md`
- [ ] **Card 02 (Workspace):** iniciar após user-service estável em produção (~48h pós-cut-over)
- [ ] **Card 03 (Proposal):** dependência: Workspace extraído
- [ ] **Card 04 (Briefing):** dependência: Proposal + circuit breaker OpenAI (Phase 4)

### Curto prazo (3-6 meses)

- [ ] Monitorar `pg_stat_statements` — identificar queries lentas
- [ ] Configurar API Gateway (Kong ou similar)
- [ ] Implementar distributed tracing (Jaeger/Zipkin)

### Médio prazo (6-12 meses)

- [ ] Se atingir 5K workspaces: planejar split de Briefing Service
- [ ] Implementar service mesh (Istio) para resilience
- [ ] Migrar de RabbitMQ para Kafka (maior throughput)

### Longo prazo (12+ meses)

- [ ] Split completo para 4 microsserviços (Cenário A)
- [ ] Implementar CQRS/Event Sourcing para análises históricas
- [ ] Considerar sharding de briefing_sessions por workspace_id

---

## Comandos Úteis

### Análise de Schema

```bash
# Listar todas as tabelas e seus sizes
psql -d scopeflow -c "\dt+ briefing_sessions proposals workspaces users"

# Verificar FKs de uma tabela
psql -d scopeflow -c "\d+ proposals"

# Contar registros por tabela
psql -d scopeflow -c "SELECT 'briefing_sessions' as table, COUNT(*) FROM briefing_sessions UNION ALL SELECT 'proposals', COUNT(*) FROM proposals;"
```

### Validação de Ownership

```bash
# Grep por JOINs cross-context (esperado: 0 resultados)
grep -rn "JOIN.*briefing_sessions.*proposals" backend/src/main/java/com/scopeflow/adapter/out/persistence

# Listar todas as FKs cross-context
psql -d scopeflow -c "
SELECT 
    tc.table_name AS from_table,
    kcu.column_name AS from_column,
    ccu.table_name AS to_table,
    ccu.column_name AS to_column
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
  AND tc.table_name IN ('proposals', 'workspaces', 'workspace_members', 'briefing_sessions');
"
```

### Análise de Volume (Estimativas)

```bash
# Tabelas hot (alto churn)
psql -d scopeflow -c "
SELECT schemaname, tablename, n_tup_ins, n_tup_upd, n_tup_del 
FROM pg_stat_user_tables 
WHERE tablename IN ('ai_generations', 'outbox_event', 'idempotency_record')
ORDER BY n_tup_ins DESC;
"
```

---

## Referências

- **ANEXO VII — Migration Pack:** `~/.claude/ANEXO-VII-migration-pack.md`
- **ADRs de migração:** `docs/migration/adr/`
- **Schema Migrations:** `backend/src/main/resources/db/migration/V*.sql`

---

**Última atualização:** 2026-04-05 (V9 migrations aplicadas)
