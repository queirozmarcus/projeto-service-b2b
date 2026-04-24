---
name: ScopeFlow — Fix BriefingControllerV1IntegrationTest
date: 2026-04-17
status: EM EXECUÇÃO
project: projeto-service-b2b
branch: develop
---

## Contexto

`BriefingControllerV1IntegrationTest` tem compile errors e falhas de runtime que bloqueiam
`./mvnw verify`. Objetivo: desbloquear o pipeline de testes completo.

## Decisões do Brainstorm

- **Stack:** Java 21, Spring Boot 3.4.3, JUnit 5, Testcontainers (PostgreSQL 16), MockMvc
- **Base:** `BriefingIntegrationTestBase` (Testcontainers + SpringBootTest + MockMvc)
- **JWT mock:** `generateTestJwtToken()` retorna string fake — precisa de JWT real via `JwtService`
- **Auth:** `AuthProxyAdapter` foi introduzido em commit anterior (167cd46) — base precisa alinhamento
- **Erros conhecidos:** `.questionId()` inexistente em `BriefingQuestion` (campo correto: `getId()`)
- **Abordagem:** bottom-up — domain → JPA → DTOs → base → controller tests → verify

## Etapas

1. ✅ **Diagnóstico** — sem erros de compile! Erros são de runtime (2026-04-17)
   - RT-1: JWT fake → 401 em todos os testes autenticados
   - RT-2: `BriefingService` sem bean Spring → ApplicationContext falha
   - RT-3: `ddl-auto=validate` pode falhar com schema Testcontainers
   - RT-4: `createCompletedBriefing()` viola PK ao salvar mesmo id duas vezes
2. ✅ **Fix RT-2** — `BriefingService` removido do test base (não era usado) (2026-04-17)
3. ✅ **Fix RT-1** — JWT real via `JwtService` (claims: workspace_id, email, role OWNER) (2026-04-17)
4. ✅ **Fix RT-4** — `createCompletedBriefing()` corrigido com deleteById + save (2026-04-17)
5. ✅ **Fix RT-3** — `V10__add_updated_at_to_outbox_event.sql` criado (2026-04-17)
6. ✅ **Fix testes CREATE/LIST** — 7 testes; 1 correção: serviceType param removido, assertion == 2 (2026-04-17)
7. ✅ **Fix testes GET/PROGRESS** — 4 testes; 1 correção: currentStep/completionPercentage >= 0 (2026-04-17)
8. ✅ **Fix testes QUESTION/ANSWER** — 4 testes; todos corretos, nenhuma alteração necessária (2026-04-17)
9. ⏳ **Fix testes COMPLETE/ABANDON** — completar os testes de `POST /complete` e `POST /abandon` se ausentes
10. ⏳ **./mvnw verify verde** — rodar verify completo, corrigir falhas remanescentes, cleanup de TODOs

## Riscos

- JWT fake em `generateTestJwtToken()` pode causar 401 em todos os testes autenticados
- `BriefingService` injetado no test base pode ter dependências não satisfeitas sem mocks
- `JpaBriefingSession.status()` pode ser string vs enum — verificar no Sprint 3
- `createCompletedBriefing()` faz cast `(BriefingInProgress)` — pode falhar se sealed class mudou

## Comandos de Referência

```bash
# Sprint 1 — diagnóstico
cd ~/iGitHub/projeto-service-b2b && ./mvnw test-compile -pl backend 2>&1 | grep "ERROR"

# Sprint 10 — verify completo
cd ~/iGitHub/projeto-service-b2b && ./mvnw verify -pl backend
```
