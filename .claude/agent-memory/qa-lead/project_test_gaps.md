---
name: gaps-de-teste-scopeflow
description: Gaps de cobertura identificados na auditoria de abril/2026 — domínios sem teste, bugs confirmados e prioridades
type: project
---

Auditoria executada em 2026-04-19 mapeando 47 classes de teste no backend e 7 no user-service.

Gaps críticos identificados:
1. Domínio `client` — zero testes (unit, integração ou contrato). Alto risco de regressão silenciosa.
2. Bug confirmado: Email VO inválido retorna 500 em vez de 400 no `UserControllerIntegrationTest`. Documentado em `user-controller-integration-tests.md`.
3. AI generation sempre mockada — fluxo principal do produto sem cobertura realista.
4. Mutation testing (PIT) nunca executado — cobertura de linha pode ser enganosa.
5. Contract tests de Workspace (Fase 2 Strangler Fig) ainda não iniciados.

**Why:** Auditoria solicitada para atualizar documentação QA ao estado real do Sprint 10.

**How to apply:** Ao gerar testes ou revisar PRs do domínio client, sinalizar ausência de cobertura. Ao revisar UserController, verificar se bug do Email VO foi corrigido.
