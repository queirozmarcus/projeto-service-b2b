# Plano: Fix — Workspace Registration Flow + Proposals Error

**Data:** 2026-04-24  
**Status:** BACKLOG  
**Prioridade:** Alta — bloqueia uso do dashboard para usuários novos

---

## Contexto

Usuários que se registram via `/auth/register` chegam ao dashboard com a mensagem
`'Erro ao carregar propostas.'`. A causa raiz é que o JWT é emitido com `workspace_id = null`
e todos os endpoints multi-tenant do monólito retornam HTTP 412.

### Chain completa do bug

```
RegisterForm coleta workspaceName
    ↓
useAuth.register() DESCARTA workspaceName (void _ws — intencional mas incompleto)
    ↓
user-service cria usuário com workspaceId = null
    ↓
JWT emitido com workspace_id = null
    ↓
GET /proposals → SecurityUtil.getWorkspaceId() → WorkspaceRequiredException → HTTP 412
    ↓
normalizeProposalError(412) → ProposalApiError { kind: 'server_error' }  ← plain object
    ↓
store catch: err instanceof Error = false → fallback: 'Erro ao carregar propostas.'
```

### Bugs identificados

| # | Camada | Bug |
|---|--------|-----|
| 1 | Frontend | `useAuth.register()` descarta `workspaceName` — workspace nunca criado |
| 2 | Monólito | `WorkspaceControllerV2.create()` não notifica user-service após criar workspace |
| 3 | Monólito | `UserServiceClient` não tem `updateUserWorkspace()` |
| 4 | user-service | Não tem `PATCH /users/{id}/workspace` endpoint |
| 5 | Frontend | Store catch: `ProposalApiError` é plain object, `instanceof Error = false` — perde mensagem real |

---

## Decisões Arquiteturais

- Workspace é criado no monólito (mantém arquitetura atual)
- Após criação, monólito notifica user-service via `UserServiceClient` → `PATCH /users/{id}/workspace`
- Frontend chama `/auth/refresh` após workspace criado para obter JWT atualizado
- Fluxo de registro: register → create workspace → refresh → dashboard

---

## Camada 1 — user-service (5 sprints)

### Sprint 1 — Domain: UpdateUserWorkspace
**Agent:** `backend-dev`  
**Entregável:** Use case `UpdateUserWorkspaceUseCase` + método `updateWorkspace()` no domain `User`

```
- User.java: adicionar método updateWorkspace(UUID workspaceId)
- UpdateUserWorkspaceUseCase.java: orquestra a atualização
  - Valida que userId existe
  - Valida que workspaceId não é null
  - Chama userRepository.save(user.updateWorkspace(workspaceId))
- UserRepository.java: verificar se save() já suporta update
```

### Sprint 2 — Persistence: JpaUser workspaceId update
**Agent:** `dba` + `backend-dev`  
**Entregável:** Migration + JPA update para workspaceId

```
- Verificar se coluna workspace_id já existe em JpaUser (user-service DB)
- Se não: migration V2__add_workspace_id_to_users.sql
- JpaUser.java: garantir que workspace_id é mapeado e atualizável
- JpaUserRepository.java: confirmar que save() faz UPDATE (não INSERT)
```

### Sprint 3 — Adapter: PATCH /users/{id}/workspace endpoint
**Agent:** `backend-dev`  
**Entregável:** Endpoint REST + DTO

```
- UpdateUserWorkspaceRequest.java: record { UUID workspaceId }
- UserController.java: PATCH /users/{id}/workspace
  - Requer JWT válido (autenticado)
  - Apenas SYSTEM/internal calls ou o próprio owner
  - Chama UpdateUserWorkspaceUseCase
  - Retorna 204 No Content
- SecurityConfig.java: liberar o endpoint para chamadas autenticadas
```

### Sprint 4 — Segurança: validação de chamadas internas
**Agent:** `security-engineer`  
**Entregável:** Proteção do endpoint de update workspace

```
- Definir estratégia: JWT do owner OU service-to-service header
- Implementar validação: apenas o próprio userId ou header interno X-Internal-Call
- Adicionar ao GlobalExceptionHandler: erro para workspace_id inválido
- Testes: tentativa de update por outro usuário deve retornar 403
```

### Sprint 5 — Testes: UpdateUserWorkspace
**Agent:** `unit-test-engineer` + `integration-test-engineer`  
**Entregável:** Cobertura completa do fluxo

```
- UpdateUserWorkspaceUseCaseTest.java: unit tests (happy path + user not found + null workspaceId)
- UserControllerWorkspaceTest.java: @WebMvcTest PATCH /users/{id}/workspace
- UserWorkspaceIntegrationTest.java: Testcontainers — persistence + controller
- Contrato: workspace_id persistido corretamente e refletido no próximo issueAccessToken
```

---

## Camada 2 — Monólito (5 sprints)

### Sprint 1 — Port: UserServiceClient.updateUserWorkspace()
**Agent:** `backend-dev`  
**Entregável:** Novo método no port de saída

```
- UserServiceClient.java: adicionar
    void updateUserWorkspace(UUID userId, UUID workspaceId, String bearerToken);
- Javadoc: explica quando chamar, exceções esperadas (404, 503)
- ServiceUnavailableException propagada se user-service indisponível
```

### Sprint 2 — Adapter: UserServiceRestAdapter implementação
**Agent:** `backend-dev`  
**Entregável:** Implementação REST do novo método

```
- UserServiceRestAdapter.java: implementar updateUserWorkspace()
  - PATCH {userServiceUrl}/users/{userId}/workspace
  - Body: { "workspaceId": "..." }
  - @CircuitBreaker(name = "user-service") + @Retry(name = "user-service")
  - 404 → UserNotFoundException; 503 → ServiceUnavailableException
```

### Sprint 3 — Controller: WorkspaceControllerV2 notifica user-service
**Agent:** `backend-dev`  
**Entregável:** WorkspaceControllerV2.create() completo

```
- WorkspaceControllerV2.java: após workspaceService.createWorkspace():
    userServiceClient.updateUserWorkspace(
        ownerId.value(),
        workspace.getId().value(),
        extractBearerToken(request)   // HttpServletRequest já injetado
    );
- Rollback strategy: se userServiceClient falhar → workspace criado mas usuário não atualizado
  → logar o erro, retornar 503 com mensagem explicativa
- Extrair token do header Authorization via método privado
```

### Sprint 4 — Resilience: tratamento de falha no notifyUserService
**Agent:** `backend-dev` + `architect`  
**Entregável:** Estratégia de compensação para falha parcial

```
- Decisão: compensação imediata vs. outbox (registrar e retentar)
- Implementar: Outbox entry para "workspace-owner-assignment" se userService retornar 503
- OutboxEventPublisher já existente — criar novo tipo de evento WorkspaceOwnerAssigned
- Fallback: resposta 201 com header X-Workspace-Pending: true se outbox ativado
```

### Sprint 5 — Testes: integração completa monólito
**Agent:** `integration-test-engineer`  
**Entregável:** Testes de integração do fluxo workspace + user-service

```
- WorkspaceCreationIntegrationTest.java: Testcontainers
  - Mock do user-service via WireMock
  - Happy path: workspace criado + user-service notificado
  - Falha do user-service: workspace criado, outbox entry gerado
- UserServiceRestAdapterTest.java: unit test do PATCH
- Contract test: monólito consumer × user-service provider
```

---

## Camada 3 — Frontend (5 sprints)

### Sprint 1 — useAuth: register flow com workspace
**Agent:** `backend-dev` (frontend)  
**Entregável:** `useAuth.register()` cria workspace após registro

```
- useAuth.ts: após /auth/register bem-sucedido:
    1. Chamar POST /workspaces com { name: data.workspaceName, niche: '', toneSettings: '{}' }
    2. Aguardar response (workspaceId no body)
    3. Chamar /auth/refresh para obter JWT com workspace_id atualizado
    4. Atualizar session com novo accessToken
- Tratar erros em cada step: se workspace falhar, não bloquear login
  mas marcar estado needsWorkspace = true
```

### Sprint 2 — useSession: estado needsWorkspace
**Agent:** `backend-dev` (frontend)  
**Entregável:** Store com estado de onboarding

```
- useSession.ts: adicionar campo needsWorkspace: boolean
- Detectar no login: se workspaceId vazio/null → needsWorkspace = true
- Detectar após register: se workspace creation falhar → needsWorkspace = true
- Persistir via sessionStorage (não localStorage — segurança)
```

### Sprint 3 — Dashboard: banner de onboarding quando sem workspace
**Agent:** `backend-dev` (frontend)  
**Entregável:** UX para usuário sem workspace

```
- dashboard/page.tsx: se needsWorkspace → mostrar banner/modal de criação
- WorkspaceSetupBanner.tsx: componente simples
  - Input para workspaceName (pré-preenchido se disponível)
  - Botão "Criar Workspace"
  - Chama POST /workspaces → /auth/refresh → reload dashboard
- Substituir mensagem genérica 'Erro ao carregar propostas.' por orientação útil
```

### Sprint 4 — Store: corrigir ProposalApiError catch
**Agent:** `backend-dev` (frontend)  
**Entregável:** Mensagens de erro corretas no dashboard

```
- useDashboardStore.ts: corrigir catch block:
  // Antes:
  const message = err instanceof Error ? err.message : 'Erro ao carregar propostas.';
  
  // Depois:
  const message = isProposalApiError(err)
    ? err.message
    : err instanceof Error
    ? err.message
    : 'Erro ao carregar propostas.';

- Adicionar type guard isProposalApiError() em types/proposal.ts
- Tratar kind === 'forbidden' com mensagem específica para workspace não configurado
  (412 cai em server_error → message: 'Erro no servidor (412)...' → mostrar corretamente)
```

### Sprint 5 — Testes: frontend registration flow
**Agent:** `unit-test-engineer` + `integration-test-engineer`  
**Entregável:** Cobertura do novo fluxo

```
- useAuth.test.ts: mock de /auth/register + POST /workspaces + /auth/refresh
  - Happy path: 3 calls em sequência, session atualizada com workspaceId
  - Workspace falha: needsWorkspace = true, usuário não bloqueado
- useDashboardStore.test.ts: atualizar teste do fetchProposals
  - ProposalApiError agora mostra mensagem real, não fallback genérico
- WorkspaceSetupBanner.test.tsx: render + submit
- RegisterForm.test.tsx: submit completo com workspaceName
```

---

## Sequência de Execução Recomendada

```
Camada 1 (user-service) — Sprints 1-5
    ↓ [dependência]
Camada 2 (monólito) — Sprints 1-5  [depende de Camada 1 Sprint 3]
    ↓ [dependência]
Camada 3 (frontend) — Sprints 1-4  [paralelo a Camada 2 Sprint 5]
Camada 3 Sprint 5 (testes) — após Camada 2 concluída
```

**Camada 3 Sprints 1-4 podem iniciar em paralelo com Camada 2** usando mocks do user-service.

---

## Riscos

| Risco | Probabilidade | Mitigação |
|-------|--------------|-----------|
| Falha parcial: workspace criado mas user-service não atualizado | Média | Outbox Pattern (Camada 2 Sprint 4) |
| Race condition: refresh antes do user-service persistir | Baixa | Retry com backoff no frontend (300ms delay) |
| Usuários existentes com workspace_id=null | Alta | Script de migração de dados (fora do escopo — backlog separado) |
| Regressão nos testes existentes de workspace | Média | Contract tests (Camada 2 Sprint 5) |

---

## Definition of Done

- [ ] Camada 1: `PATCH /users/{id}/workspace` funcionando com Testcontainers
- [ ] Camada 2: `WorkspaceControllerV2.create()` notifica user-service + outbox fallback
- [ ] Camada 3: Registro → workspace → refresh → dashboard sem erro
- [ ] Camada 3: Store exibe mensagem real do `ProposalApiError`
- [ ] Todos os testes passando (`./scripts/validate-qa-full.sh` verde)
- [ ] Usuário novo consegue criar workspace e ver propostas no dashboard
