# Status: Feature Geração de Scope via IA

**Data Início:** 2026-04-21  
**Modo:** Incremental (aprovação por sprint)  
**Branch:** develop

---

## Progress Tracker

| Sprint | Agent | Status | Data | Output |
|--------|-------|--------|------|--------|
| 1 | architect | ✅ CONCLUÍDO | 2026-04-21 | Design doc com 6 decisões + migration alert |
| 2 | api-designer | ✅ APROVADO | 2026-04-21 | OpenAPI spec completo com 6 error codes |
| 3 | backend-dev | 🔄 EM ANDAMENTO | 2026-04-21 | - |
| 4 | backend-dev | ⏳ AGUARDANDO | - | - |
| 5 | unit-test-engineer | ⏳ AGUARDANDO | - | - |
| 6 | integration-test-engineer | ⏳ AGUARDANDO | - | - |
| 7 | Frontend (direto) | ⏳ AGUARDANDO | - | - |
| 8 | Frontend (direto) | ⏳ AGUARDANDO | - | - |
| 9 | Frontend (direto) | ⏳ AGUARDANDO | - | - |
| 10 | e2e-test-engineer + code-reviewer | ⏳ AGUARDANDO | - | - |

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

## Sprint 3: Backend — Domain & Service ⏳

**Agent:** backend-dev  
**Status:** Aguardando aprovação do Sprint 2...

