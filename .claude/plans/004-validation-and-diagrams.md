# Validação: Fluxos Visuais & Checklists Finais

**Data:** 2026-03-22
**Status:** VALIDAÇÃO VISUAL

---

## 1. Fluxo End-to-End: Do Login à Aprovação

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESTADOR                                    │
│                      (Agência/Freelancer)                            │
└─────────────────────────────────────────────────────────────────────┘

    1. REGISTER / LOGIN
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /auth/register                     │
    │ Input: email, password, workspace_name  │
    │ Output: JWT access + refresh tokens     │
    │ DB: Insert user, workspace, member      │
    └─────────────────────────────────────────┘
       ↓
    2. DASHBOARD (Vazio)
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /dashboard                         │
    │ Shows: 0 propostas, 0% approval rate    │
    │ Ações: "Novo Cliente", "Novo Projeto"   │
    └─────────────────────────────────────────┘
       ↓
    3. CRIAR CLIENTE
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /clients/new                       │
    │ Form: name, email, phone                │
    │ Output: Cliente criado                  │
    │ DB: Insert clients row                  │
    └─────────────────────────────────────────┘
       ↓
    4. CRIAR PROPOSTA + ESCOLHER SERVIÇO
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /proposals/new?clientId=X           │
    │ Form: Choose service (Social Media / etc)|
    │ Backend: Carrega ServiceContextProfile  │
    │ Output: Proposal criado (draft status)   │
    │ DB: Insert proposals row                │
    └─────────────────────────────────────────┘
       ↓
    5. GERAR LINK DE BRIEFING
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /proposals/{id}/share               │
    │ Click: "Gerar Link de Briefing"         │
    │ API: POST /briefing/{proposalId}/start  │
    │   → IA gera 5 perguntas                 │
    │   → Cria BriefingSession + publicToken  │
    │   → Stores in ai_generations            │
    │ Output: Link público gerado             │
    │ URL: app.com/public/briefing/...?token  │
    └─────────────────────────────────────────┘
       ↓
    6. COMPARTILHA COM CLIENTE (WhatsApp, email)
       ↓

┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENTE                                     │
│                    (Pequeno Negócio)                                 │
└─────────────────────────────────────────────────────────────────────┘

    7. ACESSA LINK + RESPONDE PERGUNTAS
       ↓
    ┌─────────────────────────────────────────┐
    │ Page: /public/briefing/{sessionId}      │
    │ Mostra: 5 perguntas sequenciais         │
    │ Cliente: Responde no form (open text)   │
    │ Frontend: POST /public/briefing/answers │
    │ DB: Insert briefing_answers rows        │
    └─────────────────────────────────────────┘
       ↓
    8. BACKEND DETECTA RESPOSTA VAGA?
       ↓
       ┌─ SIM (< 20 chars ou genérica)
       │   ↓
       │ ┌─────────────────────────────────┐
       │ │ IA gera pergunta complementar   │
       │ │ Armazena em ai_generations      │
       │ │ Cliente vê e responde novamente │
       │ └─────────────────────────────────┘
       │   ↓
       │ Resposta completa? SIM → próxima
       │
       └─ NÃO (resposta boa)
           ↓
    9. TODAS 5 RESPOSTAS COMPLETAS
       ↓
    ┌─────────────────────────────────────────┐
    │ Backend: POST /briefing/{id}/consolidate│
    │ IA consolida em briefing estruturado:   │
    │   - objective                           │
    │   - target_audience                     │
    │   - pains                               │
    │   - expectations                        │
    │   - constraints                         │
    │ Retorna confidence_score                │
    │ Stores in briefing_sessions             │
    │ Frontend: "Briefing concluído!"         │
    └─────────────────────────────────────────┘
       ↓

┌─────────────────────────────────────────────────────────────────────┐
│                    PRESTADOR (volta)                                 │
└─────────────────────────────────────────────────────────────────────┘

    10. VÊ RESUMO DE BRIEFING
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /proposals/{id}                    │
    │ Mostra: Resumo consolidado da IA       │
    │ Ações: "Revisar", "Gerar Escopo"       │
    │ Se confidence < 70%: "Revisar manualmente" │
    └─────────────────────────────────────────┘
       ↓
    11. GERAR ESCOPO COM IA
       ↓
    ┌─────────────────────────────────────────┐
    │ Click: "Gerar Escopo"                   │
    │ API: POST /proposals/{id}/generate-scope│
    │ Job enqueue (202 Accepted)              │
    │   → IA gera scope com:                  │
    │     - deliverables                      │
    │     - exclusions                        │
    │     - assumptions                       │
    │     - dependencies                      │
    │   → Salva em ai_generations             │
    │ Frontend: Polling até status 200 OK     │
    │ Output: Scope sugerido                  │
    └─────────────────────────────────────────┘
       ↓
    12. EDITA ESCOPO SE NECESSÁRIO
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /proposals/{id}/scope              │
    │ Pode editar:                            │
    │   - deliverables (add/remove)           │
    │   - exclusions                          │
    │   - pricing                             │
    │   - timeline                            │
    │ Click: "Confirmar" = nova version v2    │
    │ DB: Insert proposal_versions row        │
    │ (v1 = IA, v2 = editada)                │
    └─────────────────────────────────────────┘
       ↓
    13. GERAR PROPOSTA
       ↓
    ┌─────────────────────────────────────────┐
    │ Click: "Gerar Proposta"                 │
    │ API: POST /proposals/{id}/render        │
    │   → Carrega ProposalTemplate (HTML)     │
    │   → Popula com Handlebars:              │
    │     {{deliverables}}                    │
    │     {{exclusions}}                      │
    │     {{timeline}}                        │
    │     {{pricing}}                         │
    │   → Async job: gera PDF                 │
    │   → Salva em S3                         │
    │ Output: HTML preview + PDF link         │
    └─────────────────────────────────────────┘
       ↓
    14. REVISAR PROPOSTA
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /proposals/{id}/preview            │
    │ Mostra: Proposta renderizada            │
    │ Ações: "Editar", "Publicar"             │
    └─────────────────────────────────────────┘
       ↓
    15. PUBLICAR LINK DE APROVAÇÃO
       ↓
    ┌─────────────────────────────────────────┐
    │ Click: "Publicar"                       │
    │ Backend:                                │
    │   - Gera public_token (exp 7 dias)      │
    │   - Status = ready_for_approval         │
    │ Output: Link público                    │
    │ URL: app.com/public/proposals/{id}?token│
    └─────────────────────────────────────────┘
       ↓
    16. COMPARTILHA COM CLIENTE NOVAMENTE
       ↓

┌─────────────────────────────────────────────────────────────────────┐
│                       CLIENTE (aprovação)                            │
└─────────────────────────────────────────────────────────────────────┘

    17. ACESSA LINK DE PROPOSTA
       ↓
    ┌─────────────────────────────────────────┐
    │ Page: /public/proposals/{id}            │
    │ Mostra:                                 │
    │   - Proposta em HTML (read-only)        │
    │   - Resumo amigável (IA simplificado)   │
    │   - Form: Nome, Email                   │
    │ Ação: "Aprovo" button                   │
    └─────────────────────────────────────────┘
       ↓
    18. APROVA
       ↓
    ┌─────────────────────────────────────────┐
    │ Click: "Aprovo"                         │
    │ Form: "João Silva", "joao@empresa.com"  │
    │ API: POST /public/proposals/{id}/approve│
    │ Backend:                                │
    │   - Valida token                        │
    │   - Cria Approval record:               │
    │     * approver_name                     │
    │     * approver_email                    │
    │     * ip_address (rastreado)            │
    │     * user_agent (rastreado)            │
    │     * approved_at (timestamp)           │
    │   - Cria ProposalEvent (type: approved) │
    │   - Enqueue: PDF export, email          │
    │ Output: "Proposta aprovada!"            │
    └─────────────────────────────────────────┘
       ↓

┌─────────────────────────────────────────────────────────────────────┐
│                    PRESTADOR (pós-aprovação)                         │
└─────────────────────────────────────────────────────────────────────┘

    19. RECEBE NOTIFICAÇÃO
       ↓
    ┌─────────────────────────────────────────┐
    │ Email: "Proposta aprovada!"             │
    │ Dashboard: Status = "APPROVED"          │
    │ Mostra: Cliente, data, hora, IP         │
    └─────────────────────────────────────────┘
       ↓
    20. VÊ RESUMO DE KICKOFF
       ↓
    ┌─────────────────────────────────────────┐
    │ App: /proposals/{id}/kickoff            │
    │ Mostra:                                 │
    │   - Resumo executivo (IA gerado)        │
    │   - Checklist de kick-off               │
    │   - Responsabilidades cliente           │
    │   - Risk flags                          │
    │ Download: Proposta PDF + Kickoff PDF    │
    └─────────────────────────────────────────┘
       ↓
    21. PRONTO PRA COMEÇAR O PROJETO!
       ↓
```

---

## 2. Fluxo de Dados — Detalhado

### BriefingSession Lifecycle

```
Status: "incomplete" (início)
├─ Pergunta 1 respondida? SIM → check
├─ Pergunta 2: resposta vaga? SIM → gera follow-up
├─ Pergunta 2 respondida? SIM → check
├─ Pergunta 3 respondida? SIM → check
├─ Pergunta 4 respondida? SIM → check
├─ Pergunta 5 respondida? SIM → check
│
└─ Todas respostas OK? SIM → Status: "complete"
    └─ IA consolida
        └─ BriefingSession.consolidatedBrief = JSON
            └─ confidence_score > 80%? SIM → "ready"
```

### ProposalVersion History

```
v1 (IA generated)
├─ deliverables: [IA suggestions]
├─ exclusions: [IA suggestions]
├─ status: "draft"
│
v2 (Prestador editada)
├─ deliverables: [editadas]
├─ exclusions: [editadas]
├─ status: "draft"
│
v3 (Renderizada)
├─ htmlContent: "<html>..."
├─ status: "published"
│
(Approval linkado a v3)
```

### AiGeneration Audit Trail

```
ai_gen_1: type=briefing_questions
├─ input: {service_id, workspace_id}
├─ output: [q1, q2, q3, q4, q5]
├─ prompt_version: v1
├─ status: success
│
ai_gen_2: type=deep_question
├─ input: {q2, answer, service_id}
├─ output: {follow_up_question}
├─ prompt_version: v1
├─ status: success
│
ai_gen_3: type=briefing_consolidation
├─ input: {all_answers}
├─ output: {objective, audience, pains, ...}
├─ prompt_version: v1
├─ confidence_score: 0.82
├─ status: success
│
ai_gen_4: type=scope_generation
├─ input: {consolidated_brief, service_context}
├─ output: {deliverables, exclusions, ...}
├─ prompt_version: v1
├─ status: success
│
ai_gen_5: type=approval_summary
├─ input: {proposal_scope, tone}
├─ output: {friendly_summary, ...}
├─ prompt_version: v1
├─ status: success
│
ai_gen_6: type=kickoff_summary
├─ input: {project, scope, timeline}
├─ output: {checklist, next_steps, ...}
├─ prompt_version: v1
├─ status: success
```

---

## 3. Arquitetura Hexagonal — Visualizada

```
┌─────────────────────────────────────────────────────────────────────┐
│                      EXTERNAL SYSTEMS                                │
├─────────────────────────────────────────────────────────────────────┤
│  Browser (Frontend)    │ OpenAI API        │  AWS S3        │ Redis  │
└──────────┬──────────────┴─────────┬─────────┴────────┬────────┴───┬──┘
           │                        │                  │            │
     HTTP REST                 HTTP Calls          S3 API        BullMQ
           │                        │                  │            │
┌──────────┴────────────────────────┴──────────────────┴────────────┴──┐
│                                                                       │
│  ┌──────────────────────────── ADAPTER IN ─────────────────────────┐ │
│  │                                                                  │ │
│  │  ┌───────────────────────────────────────────────────────────┐ │ │
│  │  │ Controllers (HTTP Layer)                                  │ │ │
│  │  │ - BriefingController.post('/start') → use case           │ │ │
│  │  │ - ProposalController.post('/render') → use case          │ │ │
│  │  │ - ApprovalController.post('/approve') → use case         │ │ │
│  │  └───────────────────────────────────────────────────────────┘ │ │
│  │                                                                  │ │
│  │  ┌───────────────────────────────────────────────────────────┐ │ │
│  │  │ Middleware & Guards                                       │ │ │
│  │  │ - JwtAuthGuard (valida token)                            │ │ │
│  │  │ - WorkspaceScopingMiddleware (injeta workspace_id)       │ │ │
│  │  │ - ErrorHandlingFilter (trata exceptions)                 │ │ │
│  │  └───────────────────────────────────────────────────────────┘ │ │
│  │                                                                  │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌──────────────────────── APPLICATION ─────────────────────────────┐ │
│  │                                                                  │ │
│  │  ┌─────────────────┐ ┌──────────────┐ ┌────────────────────┐  │ │
│  │  │ Use Cases       │ │ Services     │ │ DTOs / Mappers    │  │ │
│  │  │ - Generate...UC │ │ - Briefing.. │ │ - BriefingReq     │  │ │
│  │  │ - Consolidate..│ │ - Proposal.. │ │ - BriefingResp    │  │ │
│  │  │ - RenderProposal│ │ - Approval.. │ │ - ProposalReq/Resp│  │ │
│  │  │ - ProcessApproval│ │ - Ai........│ │ - ApprovalReq/Resp│  │ │
│  │  └─────────────────┘ └──────────────┘ └────────────────────┘  │ │
│  │                                                                  │ │
│  │  Orchestration: LoadContext → CallIA → ValidateOutput → Save  │ │
│  │                                                                  │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌──────────────────────── DOMAIN ───────────────────────────────────┐ │
│  │                                                                  │ │
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌────────────────┐ │ │
│  │  │ Entities         │ │ Value Objects    │ │ Domain Service │ │ │
│  │  │ - Proposal       │ │ - BriefingAnswer │ │ - Consolidator │ │ │
│  │  │ - Approval       │ │ - ProposalScope  │ │ - Generator    │ │ │
│  │  │ - BriefingSession│ │ - Approval       │ │ - Tracker      │ │ │
│  │  └──────────────────┘ └──────────────────┘ └────────────────┘ │ │
│  │                                                                  │ │
│  │  Exception Hierarchy                                           │ │
│  │  - ProposalNotFoundException                                  │ │
│  │  - BriefingIncompleteError                                   │ │
│  │  - AiProviderError                                           │ │
│  │                                                                  │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌──────────────────────── ADAPTER OUT ──────────────────────────────┐ │
│  │                                                                  │ │
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌────────────────┐ │ │
│  │  │ Persistence      │ │ AI Provider      │ │ Storage        │ │ │
│  │  │ - Prisma*        │ │ - OpenAiProvider │ │ - S3Storage    │ │ │
│  │  │ - Repositories   │ │ - Fallback       │ │                │ │ │
│  │  └──────────────────┘ └──────────────────┘ └────────────────┘ │ │
│  │                                                                  │ │
│  │  ┌──────────────────┐ ┌──────────────────┐                     │ │
│  │  │ Queue            │ │ Email            │                     │ │
│  │  │ - BullQueueSvc   │ │ - EmailService   │                     │ │
│  │  │ - Workers        │ │                  │                     │ │
│  │  └──────────────────┘ └──────────────────┘                     │ │
│  │                                                                  │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Checklist de Validação Técnica

### Antes de Fase 1 (Sprint 1)

- [ ] Stack aprovado globalmente?
  - [ ] NestJS + Next.js 15?
  - [ ] PostgreSQL + Prisma?
  - [ ] S3 + BullMQ?
  
- [ ] Arquitetura hexagonal planejada?
  - [ ] Pastas de domain, application, adapter?
  - [ ] Interfaces de repositório?
  - [ ] Exceções de domínio?

- [ ] Modelo de dados revisado?
  - [ ] Todas entidades mapeadas?
  - [ ] Índices definidos?
  - [ ] Constraints corretos (FK, unique)?

- [ ] Segurança alinhada?
  - [ ] JWT strategy definido (15 min access, 7 dias refresh)?
  - [ ] Workspace segregation via middleware?
  - [ ] Sensitive fields não logging?

- [ ] CI/CD inicial?
  - [ ] GitHub Actions configured?
  - [ ] Lint + test rodando?
  - [ ] Build passando?

### Antes de Fase 3 (Sprint 3 — IA)

- [ ] Prompts v1 todos revisados?
  - [ ] Linguagem PT-BR natural?
  - [ ] Saída sempre JSON?
  - [ ] Fallback definido?

- [ ] OpenAI account setup?
  - [ ] API key em .env?
  - [ ] Rate limit entendido?
  - [ ] Custo por chamada estimado?

- [ ] AiOrchestrator implementado?
  - [ ] Template loading?
  - [ ] Context population?
  - [ ] Error handling + retry?
  - [ ] ai_generations logging?

- [ ] Confidence score logic?
  - [ ] Quando < 70% → alerta?
  - [ ] Fallback quando falha?
  - [ ] User feedback loop?

### Antes de Fase 5 (Sprint 5 — Approval)

- [ ] Public token strategy?
  - [ ] JWT com exp 7 dias?
  - [ ] Gerado seguramente (crypto)?
  - [ ] Validação em endpoint público?

- [ ] Rate limiting?
  - [ ] Público (approval): 50 req/min per IP?
  - [ ] Autenticado: 1000 req/min?

- [ ] Approval metadata?
  - [ ] IP captured?
  - [ ] User-Agent captured?
  - [ ] Timestamp UTC?
  - [ ] Version ID linked?

- [ ] PDF generation?
  - [ ] Puppeteer / pdfkit setup?
  - [ ] Async job via BullMQ?
  - [ ] S3 upload with presigned URL?
  - [ ] Error handling + retry?

### Antes de Fase 6 (Sprint 6 — Launch)

- [ ] Observability?
  - [ ] Winston logs structured (JSON)?
  - [ ] Critical paths logged (auth, IA, approval)?
  - [ ] Error logs with context?

- [ ] Metrics?
  - [ ] Briefing completion rate?
  - [ ] Approval rate?
  - [ ] IA confidence score tracking?
  - [ ] Time-to-approval?

- [ ] Dashboard?
  - [ ] Mostra propostas com status?
  - [ ] Taxa de aprovação?
  - [ ] Últimas ações?

- [ ] Quality Gate?
  - [ ] 80%+ test coverage?
  - [ ] ESLint + Prettier passing?
  - [ ] No console.log in production code?
  - [ ] SQL injection checks (Prisma safe)?

---

## 5. Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| IA genérica | Medium | High | Nicho fechado, contexto per-service, v2 iteração |
| Budget não realista | Medium | Medium | Educate prestador, sugerir budget médio |
| PDF falha | Low | Medium | Async + retry, fallback HTML |
| Token expiry confusão | Low | Low | Clear docs, UX hints |
| Database slow | Low | Medium | Índices corretos, query profiling |
| S3 cost high | Low | Low | Monitor upload size, compress PDFs |
| OpenAI rate limit | Low | Medium | Queue with backoff, alternative LLM |
| Workspace data leak | Low | Critical | Row-level filter audit, test data isolation |

---

## 6. Success Metrics (Fase 1-6)

### Product Metrics
- Briefing completion rate: **Target > 90%**
- Approval rate: **Target > 85%**
- Avg time to approval: **Target < 5 days**
- Avg edits after IA: **Target < 2 per proposal**

### Quality Metrics
- Test coverage: **Target > 80%**
- P99 latency IA: **Target < 10s**
- PDF generation success: **Target > 99%**
- Confidence score avg: **Target > 80%**

### Usage Metrics
- DAU (daily active users): **Track growth**
- Proposals per workspace: **Expected 3-5 by week 4**
- Public link access: **Track client conversion**

---

**Status:** ✅ VALIDAÇÃO VISUAL COMPLETA

Todos os fluxos mapeados, decisões visuais, checklists definidos.

Próximo: 🚀 EXECUÇÃO FASE 1

