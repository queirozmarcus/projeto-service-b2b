---
name: ScopeFlow Dashboard — API Integration
date: 2026-04-12
status: EM EXECUÇÃO
project: projeto-service-b2b
branch: develop
---

## Contexto

Frontend do ScopeFlow AI usa mock data hardcoded no dashboard.
Auth (login/register/refresh) já integrado com o backend.
Objetivo: conectar proposals e briefings com a API real.

## Decisões do Brainstorm

- `baseURL` do `api.ts`: `http://localhost:8080/api/v1` (context-path: /api/v1 no application.yml)
- Backend `ProposalControllerV2` → `/api/v1/proposals` ✅
- Backend `BriefingSessionControllerV2` → `/api/v1/briefing-sessions` ✅
- **Mismatch corrigido no Sprint 1:** frontend usava `status: 'SENT'`, backend retorna `'PUBLISHED'`
- `POST /proposals` exige `briefingId` — formulário new/page precisará de redesign (Sprint 5)
- `ProposalResponse` não retorna `clientName` nem `serviceType` diretamente — apenas `clientId` e `briefingId`
  → componente `ProposalList` precisará ser adaptado (Sprint 2)

## Stack

- Frontend: Next.js 15, TypeScript, Zustand, Axios (`api.ts` com JWT interceptor)
- Backend: Java 21, Spring Boot 3.4.3, context-path `/api/v1`

## Etapas

1. ✅ `src/lib/proposalApi.ts` + `src/types/proposal.ts` — cliente tipado para proposals (2026-04-12)
2. ✅ `useDashboardStore` refatorado — fetch real, sem mock (2026-04-12)
3. ✅ `src/lib/briefingSessionApi.ts` — endpoints autenticados de briefing (2026-04-12)
4. ✅ `useBriefingSessionStore` + `/dashboard/briefings` real (2026-04-12)
5. ✅ `/dashboard/proposals/new` → POST real (redesign do fluxo briefing→proposta) (2026-04-12)
6. ⏳ `/dashboard/proposals/[id]` → dados reais + publish/delete
7. ⏳ Dashboard stats → calculados dos dados reais
8. ⏳ Recent activity → derivada das proposals reais
9. ⏳ Error handling → toasts, loading skeletons, empty states
10. ⏳ Testes unitários dos API clients + stores

## Riscos

- `POST /proposals` requer `briefingId` (UUID) — formulário atual não coleta isso.
  Decisão de UX pendente no Sprint 5: criar briefing inline ou selecionar existente.
- `ProposalList` usa `clientName` + `serviceType` — backend não retorna esses campos diretamente.
  Sprint 2 adaptará o componente para usar `proposalName` + status.
status: CONCLUÍDO ✅
