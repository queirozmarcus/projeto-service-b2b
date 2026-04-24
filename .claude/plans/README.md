# Índice de Planos — ScopeFlow AI

**Projeto:** ScopeFlow AI (B2B)  
**Última atualização:** 2026-04-23  
**Branch ativa:** `main`  
**Status geral:** Migration Strangler Fig em andamento — User Service extraído, Workspace próximo

---

## Estrutura (Kanban)

```
.claude/plans/
├── README.md          # Este arquivo
├── 1-Backlog/         # Planos pendentes / próximos ciclos
├── 2-Refinando/       # Planos sendo detalhados
├── 3-ToDo/            # Planos aprovados, prontos para execução
├── 4-Validando/        # Planos em validação
└── Done/         # Planos executados e arquivados

Novo - 1-Backlog/
Refinando - 2-Refinando/
Aprovado - 3-ToDo/
Em execução - 3-ToDo/ (status atualizado no arquivo)
Validando - 4-Validando/
Sprint concluída - Done/
Plano principal - Done/ (só quando todas as sprints fecharem)

```

---

## Status da Migração

### User Service (1º Bounded Context)
- ✅ **Fase 1** — Discovery: bounded contexts identificados
- ✅ **Fase 2** — Prepare: seams + feature toggle + contract tests
- ✅ **Fase 3** — Extract: microsserviço criado, staging ativo (12+ dias)
- ⏳ **Fase 4** — Decommission: aguarda cut-over produção

### Próximos Contextos
1. **Workspace** — próximo candidato após User Service estabilizar
2. **Briefing** — depende de Workspace
3. **Proposal** — depende de Briefing
4. **Client** — depende de Proposal
