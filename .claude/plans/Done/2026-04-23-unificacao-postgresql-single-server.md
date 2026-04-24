# Plano: Unificação PostgreSQL — Single Server, Dual Database
Data: 2026-04-23
Status: ✅ CONCLUÍDO (2026-04-23)

## Contexto
Consolidar dois containers PostgreSQL (postgres:5432 + user-db:5433) em um único
servidor PostgreSQL hospedando ambos os databases (scopeflow + scopeflow_users).
Isolamento DB-per-service preservado — apenas o servidor é unificado.

## Decisão
Opção A: Single PostgreSQL instance, databases separadas.
Motivação: simplificar Docker stack, reduzir overhead operacional, manter isolamento lógico.

## Etapas

| Sprint | Entregável | Agent | Status |
|--------|-----------|-------|--------|
| 1 | Diagnóstico — inventário conexões, env vars, configs, volumes | data-engineer | ✅ CONCLUÍDO |
| 2 | Init script — cria scopeflow_users no container principal | dba | ✅ CONCLUÍDO |
| 3 | Script migração de dados (user-db → postgres) | data-engineer | ✅ CONCLUÍDO |
| 4 | Atualizar user-service datasource URL + env vars | backend-engineer | ✅ CONCLUÍDO |
| 5 | Atualizar docker-compose.yml | devops-engineer | ✅ CONCLUÍDO |
| 6 | Atualizar docker-compose.staging.yml + .env.example | devops-engineer | ✅ CONCLUÍDO |
| 7 | Atualizar Testcontainers (testes integração user-service) | integration-test-engineer | ✅ CONCLUÍDO |
| 8 | Validação QA completa | qa-engineer | ✅ CONCLUÍDO |
| 9 | ADR-011 — decisão unificação PostgreSQL (010 já em uso) | architect | ✅ CONCLUÍDO |
| 10 | Cleanup final — remover user-db refs, atualizar docs | backend-engineer + devops-engineer | ✅ CONCLUÍDO |

## Gates
- S3 depende de S2
- S4 depende de S3
- S5 depende de S4
- S7-S8 dependem de S5-S6
- S10 só após S8 ✅

## Paralelos
- S5 e S6 podem rodar em paralelo
- S7 e S9 podem rodar em paralelo

## Riscos
- Dados em produção no user-db exigem janela de manutenção (S3)
- Em dev: trivial
