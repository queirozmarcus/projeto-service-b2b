# Status: Feature Geração de Scope via IA

**Data Início:** 2026-04-21  
**Modo:** Incremental (aprovação por sprint)  
**Branch:** develop

---

## Progress Tracker

| Sprint | Agent | Status | Data | Output |
|--------|-------|--------|------|--------|
| 1 | architect | ✅ CONCLUÍDO | 2026-04-21 | Design doc com 6 decisões + migration alert |
| 2 | api-designer | ✅ CONCLUÍDO | 2026-04-21 | OpenAPI spec completo com 6 error codes |
| 3 | backend-dev | ✅ CONCLUÍDO | 2026-04-21 | Use case + stub + 3 exceptions + port interface |
| 4 | backend-dev | ✅ CONCLUÍDO | 2026-04-21 | Controller + 3 exception handlers + migration V12 |
| 5 | unit-test-engineer + backend-dev | ✅ CONCLUÍDO | 2026-04-21 | 11 testes unitários (258 backend passando) |
| 6 | integration-test-engineer | ✅ CONCLUÍDO | 2026-04-21 | 8 testes integração implementados |
| 7 | Frontend (direto) | ✅ CONCLUÍDO | 2026-04-21 | Botão UI + API client + loading states |
| 8 | Teste Manual (E2E) | 🔄 EM ANDAMENTO | 2026-04-21 | Endpoint validado, aguardando briefing completo |
| 9 | e2e-test-engineer | ⏳ AGUARDANDO | - | Testes automatizados E2E |
| 10 | code-reviewer | ⏳ AGUARDANDO | - | Review final do código |

---

## Sprint 1: Arquitetura e Design ✅

**Agent:** architect  
**Duração:** ~10 min  
**Aprovado por:** Usuário (2026-04-21)

### Decisões
1. ✅ Interface `ScopeGenerationPort` + Stub reversível
2. ✅ Fluxo síncrono (MVP)
3. ✅ `GenerateScopeAIUseCase` em application/
4. ✅ 5 validações em cadeia
5. ✅ Mock determinístico por serviceType
6. ✅ Error codes PROPOSAL-010 a 014

### Artefatos
- `.claude/plans/2026-04-21-scope-ai-generation-design.md`

### Alertas
⚠️ Migration V10 necessária: adicionar `SCOPE_GENERATION` ao enum `generation_type`

---

## Sprint 2: Especificação OpenAPI ✅

**Agent:** api-designer  
**Duração:** ~8 min  
**Aprovado por:** Aguardando aprovação do usuário

### Entregas
- `docs/api/proposal-api.yaml` (OpenAPI 3.1)
- Endpoint: `POST /api/v1/proposals/{proposalId}/generate-scope-ai`
- 6 error codes: PROPOSAL-001, 010-014
- Schemas reutilizados: ProposalResponse, Scope, ProblemDetail

### Especificação
- ✅ Request: sem body (usa briefing vinculado)
- ✅ Response 200: ProposalResponse com scope gerado
- ✅ Timing: síncrono, 3-10s (documentado)
- ✅ Rate limit: 100 req/min
- ✅ Circuit breaker: 503 + Retry-After: 30
- ✅ RFC 9457 Problem Details em todos os erros
- ✅ Workspace isolation via JWT claim

### Validações
- ✅ HTTP semantics (POST correto)
- ✅ Consistente com briefing-api.yaml
- ✅ Security scheme reutilizado
- ✅ 4 invariants documentados (DRAFT, briefing exists, COMPLETED, 80%+)

---

## Sprint 3: Backend — Domain & Service ✅

**Agent:** backend-dev  
**Duração:** ~12 min  
**Aprovado por:** Aguardando aprovação do usuário

### Entregas (6 arquivos)
1. `ScopeGenerationPort.java` — Interface porta IA (`application/port/out/`)
2. `ScopeGenerationException.java` — Exception 503 PROPOSAL-014
3. `BriefingIncompleteException.java` — Exception 409 PROPOSAL-012
4. `BriefingNotReadyException.java` — Exception 409 PROPOSAL-013
5. `GenerateScopeAIUseCase.java` — Orquestração com 5 validações
6. `StubScopeGenerationAdapter.java` — Mock determinístico + audit trail

### Validações Implementadas
- ✅ Proposta existe (PROPOSAL-001)
- ✅ Workspace isolation (JWT match)
- ✅ Estado DRAFT obrigatório (PROPOSAL-011)
- ✅ Briefing vinculado (PROPOSAL-010)
- ✅ Briefing COMPLETED (PROPOSAL-012)
- ✅ Completeness ≥ 80% (PROPOSAL-013)

### Stub Mock
- **Deliverables:** 3 genéricos (Análise, Desenvolvimento, Entrega)
- **Price:** BRL 5.000,00
- **Timeline:** hoje + 30 dias
- **Exclusions:** 2 itens (hospedagem, conteúdo)
- **Assumptions:** 2 itens (feedback 48h, briefing aprovado)
- **Audit:** `ai_generations` table (type=SCOPE_GENERATION, model=stub-v1, cost=$0)
- **Profile:** `@Profile("!production")` — desabilitado em prod

### Arquitetura
```
application/
├── GenerateScopeAIUseCase (orchestrator)
├── ScopeGenerationException (503)
└── port/out/
    └── ScopeGenerationPort (interface)

adapter/out/ai/
└── StubScopeGenerationAdapter (mock)

core/domain/briefing/
├── BriefingIncompleteException (409)
└── BriefingNotReadyException (409)
```

### Próximos Passos (Sprint 4)
- Endpoint REST no controller
- Exception handlers no GlobalExceptionHandler
- Migration V10 (enum SCOPE_GENERATION)

---

## Sprint 4: Backend — Controller & DTOs ✅

**Agent:** backend-dev  
**Duração:** ~15 min  
**Aprovado por:** Usuário (2026-04-21)

### Entregas (3 arquivos)
1. `ProposalControllerV2.java` — Endpoint `POST /{id}/generate-scope-ai`
2. `GlobalExceptionHandler.java` — 3 exception handlers (012, 013, 014)
3. `V10__add_scope_generation_type.sql` — Migration enum SCOPE_GENERATION

---

## Sprint 5: Backend — Testes Unitários ✅

**Agent:** unit-test-engineer + backend-dev (correções)  
**Duração:** ~55 min (implementação + 4 rodadas de correção)  
**Status:** ✅ CONCLUÍDO — 11/11 testes passando + 258 testes totais do backend ✅

### Entregas
1. `GenerateScopeAIUseCaseTest.java` — 11 testes unitários (1 happy path + 10 validações)

### Correções Aplicadas (4 rodadas)

#### Rodada 1: Imports incorretos
- ❌ `ProposalRepositoryPort` → ✅ `ProposalRepository` (domain)
- ❌ `BriefingRepositoryPort` → ✅ `BriefingSessionRepository` (domain)
- ❌ `AIGenerationRepositoryPort` → ✅ `AIGenerationRepository` (domain)

#### Rodada 2: Mock inexistente removido
- ❌ `@Mock ProposalService` (não existe no projeto) → ✅ Removido

#### Rodada 3: Enum value incorreto
- ❌ `ServiceType.CUSTOM_SOFTWARE` → ✅ `ServiceType.CONSULTING`

#### Rodada 4: Lógica dos testes (unit-test-engineer)
- ✅ Re-adicionado `@Mock ProposalService` (use case real depende dele)
- ✅ Mock duplo `proposalRepository.findById()` retornando draft → updatedDraft
- ✅ Teste "briefing not linked" ajustado (mock para dado corrompido)
- ✅ Adicionado `verify(proposalService.updateScope())` em 3 testes

### Resultado Final
```bash
cd backend && ./mvnw test -Dtest=GenerateScopeAIUseCaseTest
# [INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
# [INFO] Total backend tests: 258 (27 skipped)
```

---

## Sprint 6: Backend — Testes de Integração ✅

**Agent:** integration-test-engineer  
**Duração:** ~11 min  
**Status:** ✅ CONCLUÍDO

### Entregas
1. `GenerateScopeAIIntegrationTest.java` — 8 testes (1 happy path + 7 error paths)

### Cobertura
| Cenário | HTTP | Error Code | Implementado |
|---------|------|-----------|--------------|
| Geração bem-sucedida | 200 | — | ✅ |
| Proposta não encontrada | 404 | PROPOSAL-001 | ✅ |
| Workspace mismatch | 401 | AUTH-002 | ✅ |
| Proposta não DRAFT | 409 | PROPOSAL-011 | ✅ |
| Briefing não vinculado | 409 | PROPOSAL-010 | ✅ |
| Briefing não completado | 409 | PROPOSAL-012 | ✅ |
| Score < 80% | 409 | PROPOSAL-013 | ✅ |
| Falha serviço IA | 503 | PROPOSAL-014 | ✅ |

### Stack Técnico
- `@SpringBootTest` + `@AutoConfigureMockMvc`
- Testcontainers (PostgreSQL)
- JwtTokenUtil inline para geração de tokens
- Setup: workspace → briefing COMPLETED (85%) → proposta DRAFT

---

## Sprint 7: Frontend — Componente UI ✅

**Implementação:** Direta (sem agent)  
**Duração:** ~15 min  
**Status:** ✅ CONCLUÍDO

### Entregas

**1. API Client (`proposalApi.ts`)**
- ✅ Método `generateScopeAI(id)` adicionado
- ✅ Documentação JSDoc completa
- ✅ Tratamento de erros com `ProposalApiError`

**2. Página de Detalhe (`proposals/[id]/page.tsx`)**
- ✅ Estado de loading (`isGeneratingScope`)
- ✅ Estado de erro (`generateScopeError`)
- ✅ Handler `handleGenerateScopeAI()` com atualização de store
- ✅ Botão "Gerar Escopo com IA" (roxo, ícone ⚡)
- ✅ Loading spinner animado
- ✅ Botão só aparece quando `scope === null`
- ✅ Help text atualizado
- ✅ Todos os botões desabilitados durante geração

### UX Flow
```
Usuário na página de proposta DRAFT sem scope
  → Vê botão "Gerar Escopo com IA" + help text
  → Clica → Loading spinner (3-10s)
  → Success: Scope aparece (deliverables, price, timeline)
  → Botão desaparece, "Publicar" fica habilitado
```

### Arquivos Modificados
1. `frontend/src/lib/proposalApi.ts`
2. `frontend/src/app/dashboard/proposals/[id]/page.tsx`

---

## Sprint 8: Validação E2E Manual 🔄

**Status:** EM ANDAMENTO  
**Data:** 2026-04-21

### Validações Realizadas

#### ✅ Endpoint Funcional
```
POST /proposals/30fbfe71-be7a-4eec-9693-6fe247e2ac6b/generate-scope-ai
→ Auth OK (JWT user-service)
→ Secured endpoint OK
→ Validação de negócio OK (briefing incomplete detectado)
```

#### 🔄 Teste Happy Path
**Pendente:** Completar briefing `29da9e9a-7e95-4389-9991-ae42a84b89f8`
- URL: `http://localhost:3000/dashboard/briefings/29da9e9a-...`
- Ação: Responder todas as perguntas + ≥ 80% completeness + marcar como concluído
- Após: Voltar à proposta e clicar "Gerar Escopo com IA"

#### Resultado Esperado (Happy Path)
- HTTP 200
- Scope gerado com:
  - 3 deliverables (stub determinístico)
  - Preço: BRL 5.000,00
  - Timeline: hoje + 30 dias
  - 2 exclusões, 2 assumptions
- Botão "Gerar Escopo com IA" desaparece
- Botão "Publicar Proposta" fica habilitado

---

## Correções de Bugs (Sessão 2026-04-21)

### Bug 1: Flyway Conflito V10 ✅
- **Problema:** Duas migrations com versão V10
- **Solução:** Renomear para V11, depois V12 (V11 já existia)
- **Arquivo:** `V12__add_scope_generation_type.sql`

### Bug 2: Migration V12 — Enum Type ✅
- **Problema:** `ALTER TYPE generation_type` falhou (tipo não existe)
- **Root cause:** Banco usa VARCHAR + CHECK constraint, não ENUM nativo
- **Solução:** DROP constraint antiga + ADD constraint com `SCOPE_GENERATION`
- **Validação:** `./mvnw flyway:migrate` → BUILD SUCCESS

