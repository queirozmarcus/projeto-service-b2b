# Plano: Fine Planning do MVP ScopeFlow AI

**Data:** 2026-03-22
**Status:** PLANEJAMENTO FINO
**Duração estimada:** 6 sprints (~12 semanas)
**Custo estimado:** $16-22 (Sonnet multi-agent)

---

## 1. Contexto Estratégico

### 1.1 Tese do Produto
Transformar uma venda subjetiva e desgastante em um fluxo guiado, inteligente e rastreável:

**contexto do prestador + serviço + IA + briefing estruturado + escopo claro + aprovação segura**

### 1.2 Público-Alvo Inicial (MVP)
- **Microagências e freelancers** em: social media, design, landing pages
- **3 serviços core:** Social Media Management, Landing Page Design, Brand Identity (visual + tone)
- **Problema:** venda improvisada → retrabalho → desalinhamento → conflito
- **Solução:** fluxo guiado → escopo claro → aprovação rastreável → kickoff organizado

### 1.3 Princípios de Execução
1. **Foco no nicho:** sem expansão até validar retenção
2. **IA em 5 pontos:** discovery, clareza, alinhamento, estruturação, síntese
3. **Revisão humana obrigatória:** IA sugere, usuário aprova
4. **Monólito modular:** pronto pra escalar, sem prematuridade
5. **Baixo custo inicial:** S3, Redis, PostgreSQL, sem enterprise tools

---

## 2. Arquitetura do MVP

### 2.1 Visão 360°

```
┌─────────────────────────────────────────────────────────────────┐
│                    ScopeFlow MVP — Visão Geral                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  CLIENT (Browser)                                                │
│    ├─ Login/Register                                             │
│    ├─ Dashboard (propostas, status)                              │
│    ├─ Briefing (public link + form)                              │
│    ├─ Scope (edição)                                             │
│    ├─ Proposal (gerada, edição)                                  │
│    ├─ Approve (link público)                                     │
│    └─ Kickoff (resumo + PDF)                                     │
│         ↓                                                         │
│  FRONTEND (Next.js 15 + React 19 + TS)                           │
│    ├─ Pages: auth, dashboard, briefing, scope, proposal, approve │
│    ├─ Components: form, table, modal, PDF viewer                 │
│    ├─ Hooks: useBriefing, useProposal, useApproval              │
│    ├─ Context: AuthContext, WorkspaceContext                     │
│    ├─ API client: fetch wrapper com JWT                          │
│    └─ Styling: Tailwind v4                                       │
│         ↓ (HTTP REST + JWT)                                      │
│  BACKEND (NestJS + TS — Hexagonal)                               │
│    ├─ Controllers: auth, workspace, briefing, proposal, approval │
│    ├─ Services: business logic por domínio                       │
│    ├─ Repositories: Prisma queries                               │
│    ├─ Middleware: JWT validation, workspace scoping              │
│    ├─ Modules: auth, workspace, briefing, scope, proposal        │
│    ├─ Guards: role-based (owner, admin, member)                  │
│    └─ Filters: global error handling                             │
│         ↓                                                         │
│  DATABASE (PostgreSQL + Prisma)                                  │
│    ├─ users (auth, basic profile)                                │
│    ├─ workspaces (tenant, niche, settings)                       │
│    ├─ workspace_members (roles)                                  │
│    ├─ clients (CRM light)                                        │
│    ├─ service_catalog (3 serviços MVP)                           │
│    ├─ service_context_profiles (IA context per service)          │
│    ├─ proposal_templates (HTML com placeholders)                 │
│    ├─ proposals (active state)                                   │
│    ├─ proposal_versions (immutable snapshots)                    │
│    ├─ briefing_sessions (discovery flow)                         │
│    ├─ briefing_answers (client responses)                        │
│    ├─ ai_generations (audit trail IA)                           │
│    ├─ approvals (rastreamento)                                   │
│    ├─ proposal_events (changelog)                                │
│    └─ files (S3 references)                                      │
│         ↓ (via Prisma)                                           │
│  STORAGE (AWS S3)                                                │
│    ├─ logos/{workspace_id}/*.png                                 │
│    ├─ pdfs/{workspace_id}/proposal-{id}.pdf                      │
│    ├─ pdfs/{workspace_id}/kickoff-{id}.pdf                       │
│    └─ temp/{uuid}.html (temp proposals)                          │
│         ↓ (signed URLs)                                          │
│  QUEUE (BullMQ + Redis)                                          │
│    ├─ pdf-generation (async)                                     │
│    ├─ ai-generation (async, com retry)                           │
│    ├─ email-notification (async)                                 │
│    └─ export-data (async)                                        │
│         ↓ (workers)                                              │
│  LLM API (OpenAI / Anthropic)                                    │
│    ├─ briefing_questions (generated)                             │
│    ├─ briefing_summary (consolidated)                            │
│    ├─ scope_generation (suggestions)                             │
│    ├─ approval_summary (friendly version)                        │
│    └─ kickoff_summary (checklist + notes)                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Padrão Hexagonal (Layering) — Spring Boot 3.2 + Java 21

```
src/main/java/com/scopeflow/
├─ core/
│  ├─ domain/
│  │  ├─ briefing/
│  │  │  ├─ BriefingSession.java           (entity, no framework dependency)
│  │  │  ├─ BriefingAnswer.java            (value object)
│  │  │  ├─ BriefingConsolidator.java      (domain service)
│  │  │  └─ BriefingRepository.java        (interface)
│  │  ├─ proposal/
│  │  │  ├─ Proposal.java                  (sealed class)
│  │  │  ├─ ProposalVersion.java           (record - immutable)
│  │  │  ├─ ProposalGenerator.java         (domain service)
│  │  │  └─ ProposalRepository.java        (interface)
│  │  ├─ approval/
│  │  │  ├─ Approval.java                  (entity)
│  │  │  ├─ ApprovalTracker.java           (domain service)
│  │  │  └─ ApprovalRepository.java        (interface)
│  │  ├─ workspace/
│  │  │  ├─ Workspace.java                 (aggregate root)
│  │  │  ├─ ServiceContext.java            (record - value object)
│  │  │  └─ WorkspaceRepository.java       (interface)
│  │  └─ shared/
│  │     ├─ exception/
│  │     │  ├─ DomainException.java        (base)
│  │     │  ├─ ProposalNotFoundException.java
│  │     │  └─ BriefingIncompleteException.java
│  │     ├─ AiProvider.java                (interface)
│  │     └─ StorageProvider.java           (interface)
│  │
│  └─ application/
│     ├─ briefing/
│     │  ├─ BriefingService.java           (orchestration)
│     │  ├─ GenerateBriefingQuestionsUseCase.java
│     │  ├─ ConsolidateBriefingUseCase.java
│     │  └─ BriefingResponse.java          (record DTO)
│     ├─ proposal/
│     │  ├─ ProposalService.java
│     │  ├─ GenerateProposalUseCase.java
│     │  ├─ RenderProposalUseCase.java
│     │  └─ ProposalResponse.java          (record DTO)
│     ├─ approval/
│     │  ├─ ApprovalService.java
│     │  ├─ ProcessApprovalUseCase.java
│     │  └─ ApprovalResponse.java          (record DTO)
│     ├─ workspace/
│     │  ├─ WorkspaceService.java
│     │  └─ WorkspaceResponse.java         (record DTO)
│     └─ shared/
│        ├─ AiOrchestrator.java            (prompt generation + fallback)
│        ├─ PdfGenerator.java              (async via virtual threads)
│        ├─ EmailNotifier.java             (async)
│        └─ FileStorage.java               (S3 abstraction)
│
├─ adapter/
│  ├─ in/
│  │  └─ web/
│  │     ├─ controller/
│  │     │  ├─ AuthController.java
│  │     │  ├─ BriefingController.java
│  │     │  ├─ ProposalController.java
│  │     │  ├─ ApprovalController.java
│  │     │  └─ WorkspaceController.java
│  │     ├─ filter/
│  │     │  ├─ JwtAuthenticationFilter.java
│  │     │  ├─ WorkspaceScopingFilter.java
│  │     │  └─ GlobalExceptionHandler.java
│  │     └─ dto/
│  │        ├─ request/
│  │        └─ response/
│  │
│  └─ out/
│     ├─ persistence/
│     │  ├─ jpa/
│     │  │  ├─ UserJpaRepository.java
│     │  │  ├─ BriefingJpaRepository.java
│     │  │  ├─ ProposalJpaRepository.java
│     │  │  ├─ ApprovalJpaRepository.java
│     │  │  └─ WorkspaceJpaRepository.java
│     │  └─ adapter/
│     │     ├─ UserRepositoryAdapter.java
│     │     ├─ BriefingRepositoryAdapter.java
│     │     └─ ProposalRepositoryAdapter.java
│     ├─ ai/
│     │  ├─ OpenAiProviderImpl.java
│     │  ├─ AnthropicProviderImpl.java      (fallback)
│     │  └─ PromptTemplateLoader.java
│     ├─ storage/
│     │  └─ S3StorageImpl.java
│     ├─ queue/
│     │  ├─ RabbitQueueService.java        (or Redis)
│     │  └─ listener/
│     │     ├─ AiGenerationListener.java
│     │     ├─ PdfGenerationListener.java
│     │     └─ EmailNotificationListener.java
│     └─ email/
│        └─ SmtpEmailService.java
│
├─ config/
│  ├─ SecurityConfig.java                 (Spring Security 6.x)
│  ├─ JpaConfig.java                      (Hibernate)
│  ├─ AiConfig.java
│  ├─ StorageConfig.java
│  ├─ QueueConfig.java
│  └─ WebConfig.java
│
├─ module/
│  ├─ AuthModule.java
│  ├─ BriefingModule.java
│  ├─ ProposalModule.java
│  ├─ ApprovalModule.java
│  └─ WorkspaceModule.java
│
├─ ScopeflowApplication.java
└─ pom.xml
```

**Comparação com NestJS:**
- ✅ Java 21 **sealed classes** + **records** (DTOs sem Lombok)
- ✅ **Virtual Threads** pra async (melhor que Node.js promises)
- ✅ **Spring Security** enterprise-ready (vs NestJS simples)
- ✅ **Hibernate/JPA** robusto (vs Prisma light)
- ✅ **Structured Concurrency** (Java 21 feature pra parallel tasks)
- ✅ **GraalVM native** (if needed, <100ms startup)

### 2.3 Fluxo de Dados — Por Domínio

#### Briefing Flow
```
1. Prestador escolhe serviço
   → Backend carrega ServiceContextProfile (niche, entregáveis, exclusões, tom)
   → Backend popula PromptTemplate com contexto

2. Backend chama IA: "Gere 5 perguntas pra social media management"
   → IA generator recebe: service_context + workspace tone
   → Retorna: JSON com pergunta, tipo (open/multiple-choice), ordem

3. Cliente responde no link público (token-validated)
   → Frontend POST /api/v1/briefing/{id}/answers
   → Backend armazena em BriefingAnswer (JSON estruturado)
   → Backend mede completude: 5 respostas? sim → next

4. Backend detecta respostas vagas: if response.length < 20 chars
   → Chama IA: "Pergunta complementar pra: '{resposta}'"
   → Armazena em ai_generations com prompt_version
   → Cliente vê pergunta extra

5. Client completa → Backend consolidação
   → Chama IA: "Consolide briefing em: objetivo, contexto, público-alvo, dores, entregáveis"
   → Armazena resultado em briefing_sessions.consolidated_brief (JSONB)
   → Proposta, mede confiança de IA: se < 70%, marcar pra revisão manual
```

#### Proposal Flow
```
1. Prestador clica "Gerar Escopo"
   → Backend carrega: briefing consolidado + service_context + workspace config
   → Chama IA: "Gere escopo: objetivo, entregáveis, exclusões, premissas, dependências"
   → Armazena em ai_generations com prompt_version

2. Backend armazena escopo sugerido em rascunho
   → ProposalVersion marcada como draft
   → UI mostra: "revise e confirme"

3. Prestador edita escopo
   → Backend salva nova versão imutável
   → proposal_versions agora tem 2 registros: v1 (AI), v2 (edited)

4. Prestador clica "Gerar Proposta"
   → Backend renderiza ProposalTemplate com scope + pricing + prazo
   → Usa Handlebars: {{deliverables}}, {{exclusions}}, {{timeline}}, {{price}}
   → Gera HTML → async PDF job via BullMQ

5. Proposta fica pronta
   → Status: ready_for_approval
   → Backend gera public_token (JWT com exp 7 dias)
   → Cria endpoint: GET /public/proposals/{id}/approve?token={token}
```

#### Approval Flow
```
1. Prestador compartilha link
   → Cliente acessa: /proposals/{id}/approve?token={token}
   → Backend valida token, verifica expiração, workspace

2. Cliente vê:
   → Proposta renderizada (HTML, read-only)
   → Resumo amigável: IA simplifica linguagem técnica
   → Form: Nome, Email

3. Cliente aprova
   → POST /api/v1/public/proposals/{id}/approve
   → Backend valida token novamente
   → Cria Approval record: name, email, ip, user_agent, timestamp, version_id
   → Cria ProposalEvent: type=approved
   → Enqueue: PDF export job, email notification job

4. Backend responde com sucesso
   → Frontend mostra: "Aprovado em {timestamp}"
   → Disponibiliza links pra download PDF
```

#### Kickoff Flow
```
1. Após aprovação, job async:
   → Carrega Approval + Proposal + ProposalVersion
   → Chama IA: "Gere resumo executivo do kickoff"
   → IA produz: checklist, próximos passos, pendências do cliente, resumo do escopo

2. Backend armazena em ai_generations
   → Gera PDF com resumo
   → Salva em S3 com chave {workspace_id}/kickoff-{approval_id}.pdf
   → Cria File record

3. Prestador visualiza
   → Dashboard mostra proposta com status "Approved"
   → Disponibiliza downloads: proposal.pdf, kickoff.pdf
```

---

## 3. Prompts IA — Estratégia Detalhada

### 3.1 Arquitetura de Prompts

**Todos os prompts:**
- Versionados em `src/adapter/out/ai/prompts/`
- Estrutura: `{type}_v{n}.md`
- Carregam contexto da ServiceContextProfile + WorkspaceConfig
- Saída sempre em JSON estruturado (com fallback a texto)
- Logged em `ai_generations` com `prompt_version`

### 3.2 Os 6 Prompts Core

#### 1. Briefing Questions Generator (v1)

**Arquivo:** `src/adapter/out/ai/prompts/briefing_questions_v1.md`

```markdown
# Prompt: Geração de Perguntas de Briefing

## Contexto
- Nicho: {{niche}}
- Serviço: {{service}}
- Tom: {{tone}}
- Entregáveis padrão: {{deliverables}}
- Exclusões padrão: {{exclusions}}

## Objetivo
Gerar 5 perguntas iniciais para descoberta de projeto de **{{service}}**.
As perguntas devem ser claras, não técnicas, adequadas para cliente pequeno.
Cada pergunta deve ajudar a definir objetivo, público-alvo, dores, expectativas.

## Restrições
- Sem jargão técnico
- Respostas esperadas: 50-200 palavras cada
- Ordem: começa fácil, termina profunda
- Sem pergunta de budget (vem depois)

## Formato de Resposta
```json
{
  "questions": [
    {
      "id": "q1",
      "text": "Qual é o principal objetivo desta campanha?",
      "type": "open",
      "order": 1,
      "hint": "ex: aumentar seguidores, gerar leads, construir comunidade"
    },
    ...
  ]
}
```

## Exemplos por Serviço

### Social Media Management
1. Qual é o principal objetivo desta campanha?
2. Qual é seu público-alvo ideal?
3. Qual é a principal dor do seu negócio que as redes sociais devem resolver?
4. Quantas postagens por semana você acha adequado?
5. Tem referência de conta no Instagram/TikTok que você gosta?

### Landing Page Design
1. Qual é o principal objetivo desta landing page?
2. O que você quer que o visitante faça (call-to-action)?
3. Qual é o maior desafio em converter visitantes em clientes?
4. Você tem textos prontos ou quer que a gente ajude?
5. Qual é a referência visual de estilo que você gosta?

### Brand Identity
1. Qual é a missão / razão de existir da sua marca?
2. Quem é seu cliente ideal?
3. O que diferencia você dos concorrentes?
4. Qual é o tom de voz que você quer passar?
5. Você tem cores ou estilos preferidos?
```

#### 2. Briefing Consolidator (v1)

**Arquivo:** `src/adapter/out/ai/prompts/briefing_consolidation_v1.md`

```markdown
# Prompt: Consolidação de Briefing

## Contexto
- Nicho: {{niche}}
- Serviço: {{service}}
- Respostas do cliente: {{answers}}

## Objetivo
Consolidar as respostas do cliente em uma estrutura clara para briefing.
Extrair: objetivo, público-alvo, dores, expectativas, constraints.
Alertar se informação crítica está faltando.

## Formato de Resposta
```json
{
  "objective": "...",
  "target_audience": "...",
  "pains": ["...", "..."],
  "expectations": "...",
  "constraints": {
    "timeline": "...",
    "budget_range": "...",
    "team_size": "..."
  },
  "missing_info": ["...", "..."],
  "confidence_score": 0.85,
  "alerts": ["Se escopo incerto, alertar aqui"]
}
```

#### 3. Scope Generator (v1)

**Arquivo:** `src/adapter/out/ai/prompts/scope_generation_v1.md`

```markdown
# Prompt: Geração de Escopo

## Contexto
- Nicho: {{niche}}
- Serviço: {{service}}
- Briefing consolidado: {{briefing}}
- Entregáveis padrão: {{default_deliverables}}
- Exclusões padrão: {{default_exclusions}}

## Objetivo
Gerar escopo inicial baseado no briefing consolidado.
Sugerir: objetivo, entregáveis, exclusões, premissas, dependências, timeline.

## Formato de Resposta
```json
{
  "objective": "...",
  "deliverables": ["...", "..."],
  "exclusions": ["...", "..."],
  "assumptions": ["Assumo que...", "..."],
  "dependencies": ["Precisa de...", "..."],
  "suggested_timeline": "4 semanas",
  "notes_for_review": "Revisar budget, decidir sobre..."
}
```

#### 4. Approval Summary Simplifier (v1)

**Arquivo:** `src/adapter/out/ai/prompts/approval_summary_v1.md`

```markdown
# Prompt: Resumo Amigável para Aprovação

## Contexto
- Proposta técnica: {{proposal}}
- Tom desejado: {{tone}}

## Objetivo
Simplificar a proposta em linguagem amigável.
Cliente deve entender em 5 minutos: o quê, quanto, quando, quem é responsável.

## Formato de Resposta
```json
{
  "friendly_summary": "Aqui está o que vamos fazer...",
  "what_included": ["...", "..."],
  "what_excluded": ["...", "..."],
  "timeline_simple": "4 semanas, começando em X",
  "your_responsibilities": ["...", "..."],
  "questions_to_clarify": ["...", "..."]
}
```

#### 5. Kickoff Summary Generator (v1)

**Arquivo:** `src/adapter/out/ai/prompts/kickoff_summary_v1.md`

```markdown
# Prompt: Resumo Executivo de Kickoff

## Contexto
- Projeto aprovado: {{project_name}}
- Escopo: {{scope}}
- Timeline: {{timeline}}
- Responsáveis: {{team}}

## Objetivo
Gerar um resumo executivo + checklist pra começar o projeto.

## Formato de Resposta
```json
{
  "executive_summary": "...",
  "kickoff_checklist": [
    {
      "item": "Reunião inicial com cliente",
      "owner": "prestador",
      "deadline": "dia 1"
    },
    ...
  ],
  "client_responsibilities": ["...", "..."],
  "next_steps": ["...", "..."],
  "risk_flags": ["...", "..."]
}
```

#### 6. Deep Question Generator (v1)

**Arquivo:** `src/adapter/out/ai/prompts/deep_question_v1.md`

```markdown
# Prompt: Pergunta Complementar para Resposta Vaga

## Contexto
- Serviço: {{service}}
- Pergunta original: {{original_question}}
- Resposta do cliente: {{vague_answer}}

## Objetivo
Gerar uma pergunta complementar que aprofunde a resposta vaga.
Deixar clara qual é a ambiguidade.

## Formato de Resposta
```json
{
  "follow_up_question": "...",
  "type": "open",
  "hint": "...",
  "why_asking": "Precisamos entender melhor X porque..."
}
```

### 3.3 Orquestração de Prompts (AiOrchestrator)

```typescript
// src/adapter/out/ai/AiOrchestrator.ts

class AiOrchestrator {
  constructor(
    private aiProvider: AiProvider,      // OpenAI ou Anthropic
    private promptLoader: PromptLoader,  // Carrega de arquivo
  ) {}

  async generateBriefingQuestions(
    serviceContext: ServiceContextProfile,
  ): Promise<BriefingQuestion[]> {
    // 1. Carregar template
    const template = await this.promptLoader.load('briefing_questions_v1');

    // 2. Popular contexto
    const prompt = template.populate({
      niche: serviceContext.niche,
      service: serviceContext.service,
      tone: serviceContext.tone,
      deliverables: serviceContext.defaultDeliverables,
      exclusions: serviceContext.defaultExclusions,
    });

    // 3. Chamar IA
    const response = await this.aiProvider.call({
      prompt,
      model: 'gpt-4-turbo',
      temperature: 0.7, // Criativo mas determinístico
      maxTokens: 1000,
      jsonMode: true,
    });

    // 4. Parse e validar JSON
    const parsed = JSON.parse(response.content);

    // 5. Log em ai_generations
    await this.logGeneration({
      type: 'briefing_questions',
      promptVersion: 'v1',
      input: { serviceContext },
      output: parsed,
      status: 'success',
    });

    return parsed.questions;
  }

  async consolidateBriefing(
    answers: BriefingAnswer[],
    serviceContext: ServiceContextProfile,
  ): Promise<ConsolidatedBriefing> {
    // Similar pattern: load → populate → call → log
  }

  async generateScope(
    briefing: ConsolidatedBriefing,
    serviceContext: ServiceContextProfile,
  ): Promise<ScopeProposal> {
    // Similar pattern
  }

  // ... outros métodos
}
```

### 3.4 Estratégia de Fallback

```typescript
// Se IA falhar ou retornar low confidence:

async generateBriefingQuestions(context) {
  try {
    return await this.aiOrchestrator.generateBriefingQuestions(context);
  } catch (error) {
    // Fallback: perguntas genéricas pré-definidas
    return this.loadDefaultQuestions(context.service);
  }
}

// Se confiança < 70%:
if (result.confidence_score < 0.70) {
  await this.notifyModerator({
    type: 'low_confidence_briefing',
    proposalId,
    confidenceScore: result.confidence_score,
  });
  // Flag pra revisão manual
}
```

---

## 4. Modelo de Dados — Deep Dive

### 4.1 Prisma Schema (Simplificado)

```prisma
// prisma/schema.prisma

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

// ============= AUTH & TENANT =============

model User {
  id                 String   @id @default(cuid())
  email              String   @unique
  passwordHash       String
  name               String
  createdAt          DateTime @default(now())
  updatedAt          DateTime @updatedAt

  workspaceMembers   WorkspaceMember[]
  proposalEvents     ProposalEvent[]

  @@map("users")
}

model Workspace {
  id                 String   @id @default(cuid())
  name               String
  slug               String   @unique
  nichePrimary       String   // "marketing" | "design" | "web"
  toneOfVoice        String   // "consultivo" | "casual" | "formal"

  createdAt          DateTime @default(now())
  updatedAt          DateTime @updatedAt

  members            WorkspaceMember[]
  clients            Client[]
  services           ServiceCatalog[]
  serviceContexts    ServiceContextProfile[]
  proposals          Proposal[]
  proposalTemplates  ProposalTemplate[]
  briefingSessions   BriefingSession[]
  aiGenerations      AiGeneration[]
  files              File[]

  @@map("workspaces")
}

model WorkspaceMember {
  id                 String   @id @default(cuid())
  workspaceId        String
  userId             String
  role               String   // "owner" | "admin" | "member"

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  user               User @relation(fields: [userId], references: [id], onDelete: Cascade)

  @@unique([workspaceId, userId])
  @@map("workspace_members")
}

// ============= CLIENTS & SERVICES =============

model Client {
  id                 String   @id @default(cuid())
  workspaceId        String
  name               String
  email              String
  phone              String?

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  proposals          Proposal[]

  @@index([workspaceId])
  @@map("clients")
}

model ServiceCatalog {
  id                 String   @id @default(cuid())
  workspaceId        String
  name               String   // "Social Media Management", "Landing Page", etc
  description        String?

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  contexts           ServiceContextProfile[]
  templates          ProposalTemplate[]

  @@index([workspaceId])
  @@map("service_catalog")
}

model ServiceContextProfile {
  id                 String   @id @default(cuid())
  workspaceId        String
  serviceId          String

  // Template de perguntas (JSON)
  questionsTemplate  Json     // Array<{ id, text, type, order }>

  // Entregáveis e exclusões padrão
  defaultDeliverables Json    // Array<string>
  defaultExclusions  Json     // Array<string>

  // Configuração de IA
  toneOverride       String?  // override workspace tone
  promptVersion      String   @default("v1") // qual versão de prompt usar

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  service            ServiceCatalog @relation(fields: [serviceId], references: [id], onDelete: Cascade)

  @@unique([workspaceId, serviceId])
  @@index([workspaceId])
  @@map("service_context_profiles")
}

// ============= PROPOSALS & VERSIONING =============

model Proposal {
  id                 String   @id @default(cuid())
  workspaceId        String
  clientId           String
  serviceId          String
  status             String   // "draft" | "ready_for_approval" | "approved" | "rejected"

  // Relacionamentos
  briefingSessionId  String?  @unique

  createdAt          DateTime @default(now())
  updatedAt          DateTime @updatedAt
  approvedAt         DateTime?

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  client             Client @relation(fields: [clientId], references: [id], onDelete: Restrict)
  briefingSession    BriefingSession? @relation(fields: [briefingSessionId], references: [id])

  versions           ProposalVersion[]
  aiGenerations      AiGeneration[]
  events             ProposalEvent[]
  approvals          Approval[]
  files              File[]

  @@index([workspaceId])
  @@index([clientId])
  @@map("proposals")
}

model ProposalVersion {
  id                 String   @id @default(cuid())
  proposalId         String
  versionNumber      Int      @default(1)

  // Conteúdo imutável
  scope              Json     // { objective, deliverables, exclusions, assumptions, dependencies }
  pricing            Decimal
  timeline           String
  htmlContent        String?  // HTML renderizado

  status             String   // "draft" | "published"

  createdAt          DateTime @default(now())

  proposal           Proposal @relation(fields: [proposalId], references: [id], onDelete: Cascade)
  approvals          Approval[]

  @@unique([proposalId, versionNumber])
  @@index([proposalId])
  @@map("proposal_versions")
}

model ProposalTemplate {
  id                 String   @id @default(cuid())
  workspaceId        String
  serviceId          String
  name               String
  htmlTemplate       String   // Handlebars {{deliverables}}, {{timeline}}, etc

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  // serviceId = reference to ServiceCatalog

  @@index([workspaceId])
  @@map("proposal_templates")
}

// ============= BRIEFING & IA =============

model BriefingSession {
  id                 String   @id @default(cuid())
  workspaceId        String
  proposalId         String   @unique

  publicToken        String   @unique // JWT para link público
  tokenExpiresAt     DateTime

  // Consolidação
  consolidatedBrief  Json?    // { objective, audience, pains, expectations, constraints }
  completionStatus   String   @default("incomplete") // "incomplete" | "complete"

  createdAt          DateTime @default(now())
  updatedAt          DateTime @updatedAt

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  proposal           Proposal @relation(fields: [proposalId], references: [id], onDelete: Cascade)
  answers            BriefingAnswer[]

  @@index([workspaceId])
  @@index([publicToken])
  @@map("briefing_sessions")
}

model BriefingAnswer {
  id                 String   @id @default(cuid())
  briefingSessionId  String
  questionId         String   // "q1", "q2", etc

  answer             String   // Resposta do cliente
  isVague            Boolean  @default(false) // Flag se resposta < 20 chars
  followUpAsked      Boolean  @default(false) // Se pergunta complementar foi feita

  createdAt          DateTime @default(now())

  briefingSession    BriefingSession @relation(fields: [briefingSessionId], references: [id], onDelete: Cascade)

  @@index([briefingSessionId])
  @@map("briefing_answers")
}

model AiGeneration {
  id                 String   @id @default(cuid())
  workspaceId        String
  proposalId         String?

  generationType     String   // "briefing_questions" | "briefing_summary" | "scope_generation" | "approval_summary" | "kickoff_summary"
  promptVersion      String   // "v1", "v2", etc

  inputJson          Json     // O que foi passado pra IA
  outputJson         Json     // O que IA retornou
  confidenceScore    Float?   // Se IA retornou confidence

  status             String   @default("success") // "success" | "failed" | "low_confidence"
  errorMessage       String?

  createdAt          DateTime @default(now())

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  proposal           Proposal? @relation(fields: [proposalId], references: [id], onDelete: SetNull)

  @@index([workspaceId])
  @@index([proposalId])
  @@index([generationType])
  @@map("ai_generations")
}

// ============= APPROVAL & EVENTS =============

model Approval {
  id                 String   @id @default(cuid())
  proposalId         String
  proposalVersionId  String

  approverName       String
  approverEmail      String
  ipAddress          String?
  userAgent          String?

  approvedAt         DateTime @default(now())

  proposal           Proposal @relation(fields: [proposalId], references: [id], onDelete: Cascade)
  proposalVersion    ProposalVersion @relation(fields: [proposalVersionId], references: [id], onDelete: Restrict)

  @@index([proposalId])
  @@map("approvals")
}

model ProposalEvent {
  id                 String   @id @default(cuid())
  proposalId         String

  eventType          String   // "created" | "viewed" | "edited" | "approved" | "rejected"
  eventPayload       Json?    // { editedFields, reason, etc }
  createdByUserId    String?

  createdAt          DateTime @default(now())

  proposal           Proposal @relation(fields: [proposalId], references: [id], onDelete: Cascade)
  createdByUser      User? @relation(fields: [createdByUserId], references: [id], onDelete: SetNull)

  @@index([proposalId])
  @@map("proposal_events")
}

// ============= FILES & STORAGE =============

model File {
  id                 String   @id @default(cuid())
  workspaceId        String
  proposalId         String?

  fileType           String   // "logo" | "attachment" | "proposal_pdf" | "kickoff_pdf"
  fileName           String
  storageKey         String   // S3 key: {workspace_id}/{proposal_id}/{uuid}-{name}
  contentType        String?
  fileSizeBytes      BigInt?

  createdAt          DateTime @default(now())

  workspace          Workspace @relation(fields: [workspaceId], references: [id], onDelete: Cascade)
  proposal           Proposal? @relation(fields: [proposalId], references: [id], onDelete: SetNull)

  @@index([workspaceId])
  @@index([proposalId])
  @@map("files")
}
```

### 4.2 Decisões de Design

| Decisão | Justificativa |
|---------|---------------|
| **UUID PKs** | Distribuído, seguro, evita exposição de IDs sequenciais |
| **JSONB para contexto** | Flexibilidade: cada serviço pode ter estrutura diferente sem migration |
| **ProposalVersion imutável** | Auditabilidade: histórico completo de mudanças |
| **AiGeneration separado** | Rastreamento de prompts, confidence, fallback logic |
| **PublicToken em BriefingSession** | Evita exposição de IDs, permite validação de acesso |
| **Workspace segregation** | Multi-tenancy: todas as queries filtram por workspace_id |
| **Events table** | Changelog completo: quem fez o quê, quando |
| **Indexes em FK + workspace** | Performance: queries rápidas mesmo com crescimento |

---

## 5. Padrões de Implementação

### 5.1 Camadas de Serviço

#### Use Case: "Gerar Perguntas de Briefing"

```typescript
// domain/briefing/GenerateBriefingQuestionsUseCase.ts
export class GenerateBriefingQuestionsUseCase {
  constructor(
    private aiOrchestrator: AiOrchestrator,
    private contextRepository: ServiceContextRepository,
    private briefingRepository: BriefingRepository,
    private logger: Logger,
  ) {}

  async execute(command: GenerateBriefingQuestionsCommand): Promise<BriefingQuestion[]> {
    // 1. Validar entrada
    if (!command.serviceId) throw new InvalidServiceError();

    // 2. Carregar contexto
    const context = await this.contextRepository.findByServiceId(command.serviceId);
    if (!context) throw new ServiceContextNotFoundError();

    // 3. Chamar IA
    this.logger.info('Generating briefing questions', { serviceId: command.serviceId });
    const questions = await this.aiOrchestrator.generateBriefingQuestions(context);

    // 4. Armazenar sessão (será completada com respostas depois)
    const session = BriefingSession.create({
      workspaceId: command.workspaceId,
      proposalId: command.proposalId,
      publicToken: generateToken(),
    });
    await this.briefingRepository.save(session);

    this.logger.info('Briefing questions generated', { questionsCount: questions.length });
    return questions;
  }
}

// application/briefing/BriefingService.ts (Orchestration)
@Injectable()
export class BriefingService {
  constructor(
    private generateQuestionsUseCase: GenerateBriefingQuestionsUseCase,
    private consolidateBriefingUseCase: ConsolidateBriefingUseCase,
  ) {}

  async startBriefing(command: StartBriefingCommand): Promise<BriefingResponse> {
    const questions = await this.generateQuestionsUseCase.execute(command);
    return BriefingResponse.from(questions);
  }

  async submitAnswers(command: SubmitAnswersCommand): Promise<void> {
    // Armazena respostas
    // Detecta respostas vagas → pede pergunta complementar
    // Se completo → consolida
  }
}

// adapter/in/http/BriefingController.ts (HTTP Layer)
@Controller('api/v1/briefing')
export class BriefingController {
  constructor(private briefingService: BriefingService) {}

  @Post(':proposalId/start')
  @UseGuards(JwtAuthGuard)
  async startBriefing(@Param('proposalId') proposalId: string) {
    return await this.briefingService.startBriefing({ proposalId });
  }

  @Post(':sessionId/answers')
  // PUBLIC endpoint (token-validated)
  async submitAnswers(@Body() dto: SubmitAnswersDto) {
    return await this.briefingService.submitAnswers(dto);
  }
}
```

### 5.2 Error Handling

```typescript
// core/domain/exceptions/

export class DomainException extends Error {
  constructor(
    public code: string,
    public message: string,
    public details?: any,
  ) {
    super(message);
  }
}

export class ProposalNotFoundException extends DomainException {
  constructor(proposalId: string) {
    super('PROPOSAL_NOT_FOUND', `Proposal ${proposalId} not found`, { proposalId });
  }
}

export class BriefingIncompleteError extends DomainException {
  constructor(missingFields: string[]) {
    super('BRIEFING_INCOMPLETE', 'Briefing incomplete', { missingFields });
  }
}

// adapter/in/http/ErrorHandlingFilter.ts
@Catch(DomainException)
export class DomainExceptionFilter implements ExceptionFilter {
  catch(exception: DomainException, host: HttpArgumentsHost) {
    const response = host.getResponse<Response>();
    const status = this.getHttpStatus(exception.code);

    response.status(status).json({
      data: null,
      meta: {},
      error: {
        code: exception.code,
        message: exception.message,
        details: exception.details,
      },
    });
  }

  private getHttpStatus(code: string): number {
    const map = {
      PROPOSAL_NOT_FOUND: 404,
      BRIEFING_INCOMPLETE: 400,
      UNAUTHORIZED: 401,
      FORBIDDEN: 403,
    };
    return map[code] || 500;
  }
}
```

### 5.3 Testing Strategy

```typescript
// tests/briefing.service.spec.ts

describe('BriefingService', () => {
  let service: BriefingService;
  let aiOrchestrator: jest.Mocked<AiOrchestrator>;
  let repository: jest.Mocked<BriefingRepository>;

  beforeEach(() => {
    aiOrchestrator = createMock<AiOrchestrator>();
    repository = createMock<BriefingRepository>();
    service = new BriefingService(aiOrchestrator, repository);
  });

  describe('startBriefing', () => {
    it('should generate questions for service', async () => {
      // Arrange
      const command = { serviceId: 'social-media', workspaceId: 'w1' };
      const mockQuestions = [
        { id: 'q1', text: 'What is your goal?', type: 'open' },
      ];
      aiOrchestrator.generateBriefingQuestions.mockResolvedValue(mockQuestions);

      // Act
      const result = await service.startBriefing(command);

      // Assert
      expect(result.questions).toEqual(mockQuestions);
      expect(repository.save).toHaveBeenCalled();
    });

    it('should throw if service context not found', async () => {
      // Arrange
      const command = { serviceId: 'unknown', workspaceId: 'w1' };
      aiOrchestrator.generateBriefingQuestions.mockRejectedValue(
        new ServiceContextNotFoundError(),
      );

      // Act & Assert
      await expect(service.startBriefing(command)).rejects.toThrow(
        ServiceContextNotFoundError,
      );
    });
  });
});
```

---

## 6. Roadmap de Execução

### Sprint 1 — Foundation (3 semanas)

**Objetivo:** Base sólida de auth, workspace, membros, database

**Tarefas:**
1. `/full-bootstrap scopeflow-mvp aws`
   - Scaffold NestJS + Next.js
   - GitHub Actions CI/CD
   - PostgreSQL schema
   - Prisma migrations

2. Auth + Workspace
   - Register, login, forgot password
   - Workspace CRUD (create, update, delete)
   - Member management (RBAC: owner, admin, member)
   - JWT token strategy (access + refresh)

3. Database seeds
   - 3 serviços: Social Media, Landing Page, Brand Identity
   - ServiceContextProfile templates
   - Proposal templates (HTML com Handlebars)

**Entregável:** App com login, workspace, e pessoas conseguem navegar pra dashboard vazio

---

### Sprint 2 — Catálogo & Contexto (2 semanas)

**Objetivo:** Serviços bem mapeados com contexto de IA

**Tarefas:**
1. ServiceCatalog CRUD
2. ServiceContextProfile setup (3 serviços bem definidos)
3. ProposalTemplate CRUD (frontend pra editar templates)
4. Tests + quality audit

**Entregável:** Dashboard mostra 3 serviços, pode criar novo cliente

---

### Sprint 3 — Briefing IA (3 semanas)

**Objetivo:** Fluxo completo de descoberta com cliente

**Tarefas:**
1. BriefingSession + public link
2. AI question generation (OpenAI integration)
3. Answer submission (client-side form)
4. Answer storage + vagueness detection
5. Deep question generation (follow-up)
6. Briefing consolidation (AI summary)
7. Contract tests + E2E

**Entregável:** Cliente consegue responder briefing, vê consolidação, prestador vê resultado

---

### Sprint 4 — Escopo & Proposta (2 semanas)

**Objetivo:** Geração de escopo e renderização de proposta

**Tarefas:**
1. Scope generation (IA)
2. Scope editing (UI pra editar antes de usar)
3. Proposal rendering (Handlebars + scope)
4. Proposal versioning (immutable snapshots)
5. Tests

**Entregável:** Prestador consegue gerar proposta a partir de briefing consolidado

---

### Sprint 5 — Aprovação (2 semanas)

**Objetivo:** Link público de aprovação rastreável

**Tarefas:**
1. Public approval link (token-validated)
2. Approval form (name, email)
3. Approval tracking (IP, UA, timestamp, version)
4. Security tests (rate limit, token validation)
5. PDF generation async (BullMQ job)

**Entregável:** Cliente consegue aprovar via link, prestador vê que foi aprovado

---

### Sprint 6 — Kickoff & Dashboard (2 semanas)

**Objetivo:** Resumo de kickoff + observabilidade

**Tarefas:**
1. Kickoff summary generation (IA)
2. PDF export (proposal + kickoff)
3. Dashboard básico (status, taxa aprovação)
4. Email notifications (when approved, when submitted)
5. Observability (logs, metrics, alerts)
6. Final audit (coverage, security, performance)

**Entregável:** MVP completo, pronto pra validar com clientes reais

---

## 7. Próximos Passos

### Aprovação de Planejamento

Quando aprovado, vou passar pra **Fase 4: Execução**.

Vou começar com:

```
Sprint 1 Task 1: /full-bootstrap scopeflow-mvp aws

Isso vai:
- Criar estrutura NestJS modular (hexagonal)
- Criar estrutura Next.js 15 com React 19
- Setup PostgreSQL + Prisma
- Setup GitHub Actions CI/CD
- Setup observability base (Winston logs)
```

### Decisões Pendentes

Confirma se está ok:

1. **Stack aprovada?** NestJS + Next.js 15 + PostgreSQL + Prisma + S3 + BullMQ
2. **Prompts v1 estão bons?** Ou quer ajustar linguagem/estrutura?
3. **3 serviços iniciais?** Social Media, Landing Page, Brand Identity
4. **6 sprints = 12 semanas é realista?** Ou acelerar/desacelerar?
5. **Começar com Fase 1 agora ou quer mais detalhes de algo?**

---

**Status:** ⏳ Aguardando aprovação para Fase 4 (Execução)
