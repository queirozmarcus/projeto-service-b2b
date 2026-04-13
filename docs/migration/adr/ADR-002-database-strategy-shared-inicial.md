# ADR-002: Database strategy — shared database inicial

**Status:** Proposto
**Data:** 2026-04-05
**Contexto:**

Ao extrair bounded contexts em microsservicos, a estrategia de dados e a decisao com
maior impacto de longo prazo. As opcoes sao: shared database (todos os servicos acessam
o mesmo PostgreSQL) ou database-per-service (cada servico com sua propria instancia).

O schema atual tem FKs cross-context:
- `workspaces.owner_id` -> `users.id`
- `workspace_members.user_id` -> `users.id`
- `briefing_sessions.workspace_id` -> `workspaces.id`
- `proposals.workspace_id` -> `workspaces.id`
- `proposals.briefing_id` -> `briefing_sessions.id`

**Decisao:**

Shared database com schema logico separado por contexto. Cada servico acessa apenas suas
proprias tabelas (disciplina de equipe, nao enforcement tecnico). FKs cross-context
permanecem durante a transicao.

Caminho incremental em 3 fases:
1. Shared DB + servicos separados (cada um acessa suas tabelas)
2. Views/sinonimos para leitura cross-context (read-only)
3. Database-per-service com data sync via eventos (somente quando necessario)

**Alternativas consideradas:**

1. *Database-per-service imediato*: Descartado. Requer remover todas as FKs cross-context,
   implementar data sync via eventos, manter consistencia eventual. Complexidade desproporcional
   ao tamanho atual da equipe e do sistema.

2. *Schema-per-service no mesmo PostgreSQL*: Considerado como alternativa leve. Schemas
   separados (`user_schema`, `workspace_schema`) no mesmo PG oferecem isolamento logico
   sem overhead operacional. Pode ser adotado na Fase 2 se a disciplina de equipe falhar.

**Trade-offs:**

| Ganha | Perde |
|-------|-------|
| FKs continuam funcionando (integridade referencial) | Acoplamento de dados nao e resolvido tecnicamente |
| Zero overhead operacional adicional | Risco de servicos acessarem tabelas alheias por acidente |
| Rollback trivial (servicos voltam a ser um monolito) | Nao testa cenarios de eventual consistency |
| Outbox table compartilhada funciona sem mudancas | Database pode virar bottleneck de performance (improvavel no scale atual) |

**Impacto:**

- Nenhuma mudanca no schema durante a extracao do User service
- Outbox table continua compartilhada (publisher filtra por `aggregate_type`)
- FKs serao removidas somente na Fase 3, uma por vez, com testes de regressao
- Activity logs e idempotency records permanecem no shared DB (infraestrutura transversal)
