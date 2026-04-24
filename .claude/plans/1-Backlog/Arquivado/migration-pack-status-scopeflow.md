# Status: Migration Pack — ScopeFlow AI
Data: 2026-04-12

## Contexto

O usuário perguntou em que fase do Migration Pack o projeto ScopeFlow AI está.
O objetivo é mapear o estado atual das 4 fases do Migration Pack contra o progresso real,
e identificar claramente o que foi concluído e o que resta fazer.

---

## Estado Atual: User Service (1º Bounded Context Extraído)

### Fase 1 — `/migration-discovery` ✅ CONCLUÍDA

O que foi feito:
- Bounded contexts identificados: User, Workspace, Briefing, Proposal, Client
- Ordem de extração definida: User Service como prova de conceito (domínio mais isolado)
- Event Storming implícito na estrutura hexagonal do monólito

### Fase 2 — `/migration-prepare` ✅ CONCLUÍDA

O que foi feito:
- Seam criado via `AuthControllerV2` no monólito (proxy condicional com feature toggle)
- Feature toggle: `auth.service.use-extracted=true/false` (rollback instantâneo)
- 8 contract tests Spring Cloud Contract como gate de deploy
- Interfaces de boundary definidas: JWT_SECRET compartilhado (HS256), mesmo schema PostgreSQL

### Fase 3 — `/migration-extract` ✅ CONCLUÍDA (staging)

O que foi feito (ADR-008, aceito 2026-04-11):
- `user-service/` criado como microsserviço separado (porta 8081, monólito em 8080)
- Arquitetura hexagonal própria: User sealed classes + value objects + JPA adapter
- Traefik configurado: `/api/v1/auth/*` → user-service (priority 100)
- Cut-over ativado em STAGING: `application-staging.yml` com `use-extracted: true`
- Docker Compose: monólito + user-service + PostgreSQL compartilhado

**Fase 2 do cut-over (produção gradual) ainda pendente:**
- 10% → 50% → 100% com gates SLO (p95 < 200ms, erro < 0.1%)
- Depende de 48h de observação em staging (iniciado em 11 abr 2026)

### Fase 4 — `/migration-decommission` ⏳ NÃO INICIADA

O que falta:
- Aguardar 72h+ de estabilidade em produção após cut-over 100%
- Remover código auth legado do monólito (`AuthControllerV1`, `UserService` do monólito)
- Migrar para database-per-service (planejado como Fase 3 do ADR-002)
- Secrets Manager (Vault) para JWT_SECRET em vez de env var compartilhada

---

## Próximos Bounded Contexts (após User Service estabilizar)

| Bounded Context | Status | Dependência |
|----------------|--------|-------------|
| **User Service** | 🔄 Staging, produção pendente | — |
| **Workspace** | ⏳ Próximo candidato | User Service estável |
| **Briefing** | ⏳ Planejado | Workspace extraído |
| **Proposal** | ⏳ Planejado | Briefing extraído |
| **Client** | ⏳ Planejado | Proposal extraído |

---

## Resposta direta: Em que fase estamos?

**Fase 3 — `/migration-extract` — em finalização.**

O User Service está extraído e com cut-over ativado em staging (11/04).
O próximo passo imediato é o cut-over gradual em produção (Fase 2 do ADR-008).
A Fase 4 (`/migration-decommission`) só começa após 72h de estabilidade em produção.

Não há tarefa de código para executar agora — o trabalho atual é operacional:
monitorar SLOs em staging e, quando aprovado, executar o roll-out gradual em produção.

---

## Ações disponíveis (se o usuário quiser avançar)

1. **Executar cut-over em produção** → `/devops-incident` ou playbook `k8s-deploy-safe.md`
2. **Iniciar decommission do código legado** → `/migration-decommission user`
3. **Iniciar discovery do próximo contexto** → `/migration-discovery` (Workspace)
4. **Separar o banco de dados** → `/data-migrate` (database-per-service para User)
