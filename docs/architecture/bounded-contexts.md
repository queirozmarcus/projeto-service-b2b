# Bounded Contexts — ScopeFlow AI

**Versão:** 1.0  
**Data:** 2026-04-05  
**Autor:** Domain Analyst (Claude Code Agent)

## Visão Geral

ScopeFlow AI é estruturado em **4 bounded contexts** seguindo princípios de Domain-Driven Design (DDD):

1. **Briefing** — Descoberta de requisitos via fluxo de perguntas e respostas
2. **Proposal** — Geração, edição e aprovação de propostas comerciais
3. **User** — Autenticação, registro e gestão de usuários
4. **Workspace** — Multi-tenancy, membros e permissões

Todos os contextos aplicam **Hexagonal Architecture** com domain puro (zero dependências de framework) e comunicação via **Domain Events** através do padrão Outbox + RabbitMQ.

**Filosofia DDD aplicada:**
- **Aggregates como raiz transacional**: Cada contexto tem 1+ agregados modelados com sealed classes (Java 21) para type safety
- **Value Objects para conceitos de negócio**: `BriefingSessionId`, `WorkspaceId`, `Email`, `AnswerText`, etc.
- **Invariantes no domínio**: Regras de negócio enforçadas nos métodos do agregado (ex: score >= 80% para completar briefing)
- **Domain Events para comunicação assíncrona**: Eventos publicados via Outbox garantem entrega exatamente uma vez
- **Ports & Adapters**: Domínio isolado, adaptadores implementam portas de entrada (REST) e saída (JPA, RabbitMQ, S3)

---

## Contexto: Briefing

**Responsabilidade:**  
Gerenciar sessões de descoberta de requisitos através de perguntas estruturadas e respostas do cliente. Detectar gaps, gerar follow-ups via IA e completar briefings quando score >= 80%.

### Aggregate Root

#### `BriefingSession` (sealed)

**Estados (sealed variants):**
- `BriefingInProgress` — sessão ativa, aceitando respostas
- `BriefingCompleted` — fechada para edição, pronta para geração de proposta (score >= 80%)
- `BriefingAbandoned` — cliente abandonou, pode reiniciar nova sessão

**Entities:**
- `BriefingQuestion` — pergunta do roteiro (sequencial, step-based)
- `BriefingAnswer` (sealed):
  - `AnsweredDirect` — resposta direta sem follow-up
  - `AnsweredWithFollowup` — resposta com pergunta adicional gerada via IA
- `AIGeneration` — audit trail de chamadas à OpenAI (prompt, response, tokens)

**Value Objects:**
- `BriefingSessionId`, `QuestionId`, `AnswerId`
- `ClientId` — identifica cliente final (não user do workspace)
- `ServiceType` — enum: `SOCIAL_MEDIA`, `LANDING_PAGE`, `LOGO`, `BRANDING`, etc.
- `PublicToken` — token UUID para acesso público do cliente (sem auth)
- `AnswerText` — resposta do cliente (validado não-vazio)
- `CompletionScore` — score 0-100 de completude do briefing
- `BriefingProgress` — currentStep, totalSteps, completionPercentage
- `GapAnalysis` — gaps detectados, score, isEligibleForCompletion()

**Domain Events:**
- `BriefingSessionStartedEvent`
- `QuestionAskedEvent`
- `AnswerSubmittedEvent`
- `FollowupQuestionGeneratedEvent`
- `BriefingCompletedEvent` → consumido por `BriefingCompletedListener`
- `BriefingAbandonedEvent`

**Invariantes:**
1. Apenas 1 briefing ativo por cliente por service type (enforce em `startBriefing()`)
2. Perguntas respondidas sequencialmente (sem skip)
3. Max 1 follow-up por pergunta
4. Completion score >= 80% para marcar como completado
5. Respostas não podem ser vazias

**Ports de Saída:**
- `BriefingSessionRepository`
- `BriefingQuestionRepository`
- `BriefingAnswerRepository`
- `AIGenerationRepository`

**Domain Service:**
- `BriefingService` — orquestra transições de estado, valida invariantes, coordena repositórios

**Dependências externas:**
- `WorkspaceId` (do contexto Workspace) — para multi-tenancy
- Nenhuma dependência de outros agregados

---

## Contexto: Proposal

**Responsabilidade:**  
Gerenciar propostas comerciais desde draft até aprovação pelo cliente. Controla versionamento de escopo, workflow de aprovação e geração de PDF.

### Aggregate Root

#### `Proposal` (sealed)

**Estados (sealed variants):**
- `ProposalDraft` — em edição, não visível ao cliente
- `ProposalPublished` — compartilhada com cliente, aguardando aprovação
- `ProposalApproved` — cliente aprovou
- `ProposalRejected` — cliente rejeitou

**Entities:**
- `ProposalVersion` — snapshot imutável de cada alteração de escopo
- `ApprovalWorkflow` — workflow de aprovação multi-step
- `Approval` — registro individual de aprovação (approverEmail, status, IP, userAgent)

**Value Objects:**
- `ProposalId`
- `ProposalScope` — escopo da proposta (estrutura não detalhada no código visto)
- `ProposalStatus` — enum: `DRAFT`, `PUBLISHED`, `APPROVED`, `REJECTED`
- `ApprovalStatus` — enum: `PENDING`, `IN_PROGRESS`, `APPROVED`, `REJECTED`

**Domain Events:**
- `ProposalApprovedEvent` → consumido por `ProposalApprovalListener` (gera PDF + envia email)

**Invariantes:**
1. Só pode publicar proposta com escopo definido
2. Transições de estado: DRAFT → PUBLISHED → APPROVED/REJECTED
3. Versão criada a cada update de escopo e no publish
4. Workflow de aprovação só para propostas PUBLISHED
5. Soft delete: deleted_at marca proposta como invisível

**Ports de Saída:**
- `ProposalRepository`
- `ProposalVersionRepository`
- `ApprovalWorkflowRepository`
- `PdfService` (adapter: geração de PDF via iText + upload S3)
- `EmailService` (adapter: envio via SES)

**Domain Service:**
- `ProposalService` — orquestra ciclo de vida, valida transições de estado, coordena versionamento

**Dependências externas:**
- `BriefingSessionId` (do contexto Briefing) — proposta nasce de um briefing completado
- `WorkspaceId` (do contexto Workspace) — para multi-tenancy
- `clientId` (UUID) — identifica cliente final

---

## Contexto: Workspace

**Responsabilidade:**  
Implementar multi-tenancy (isolamento de dados por tenant), gerenciar membros do workspace e controlar roles (OWNER, ADMIN, MEMBER, VIEWER).

### Aggregate Roots

#### `Workspace` (sealed)

**Estados (sealed variants):**
- `WorkspaceActive` — operação normal
- `WorkspaceSuspended` — owner pausou assinatura

**Value Objects:**
- `WorkspaceId`
- `niche` — nicho do workspace (ex: "social-media", "landing-page")
- `toneSettings` — JSON com configurações de tom de comunicação para IA

**Invariantes:**
1. Todo workspace tem exatamente 1 OWNER (invariante crítica)
2. Nome do workspace único por tenant (não verificado globalmente, mas por user)

**Ports de Saída:**
- `WorkspaceRepository`

#### `WorkspaceMember` (sealed)

**Estados (sealed variants):**
- `MemberActive` — membro ativo
- `MemberInvited` — convidado, aguardando aceitação
- `MemberLeft` — removido/saiu (histórico)

**Value Objects:**
- `Role` — enum: `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`

**Invariantes:**
1. Não pode remover último OWNER do workspace
2. Usuário pode ser membro de múltiplos workspaces

**Ports de Saída:**
- `WorkspaceMemberRepository`

**Domain Service:**
- `WorkspaceService` — orquestra criação, convites, remoção de membros, validações de roles

**Dependências externas:**
- `UserId` (do contexto User) — relacionamento com usuários

---

## Contexto: User

**Responsabilidade:**  
Autenticação, registro de usuários, gestão de perfil e soft delete (GDPR compliance).

### Aggregate Root

#### `User` (sealed)

**Estados (sealed variants):**
- `UserActive` — pode fazer login, sessão ativa
- `UserInactive` — convidado mas não confirmou email
- `UserDeleted` — soft-deleted (GDPR)

**Value Objects:**
- `UserId`
- `Email` — validado como email válido
- `PasswordHash` — hash BCrypt da senha
- `fullName`, `phone` (opcional)

**Domain Events:**
- `UserRegisteredEvent` → consumido por `UserRegistrationListener` (envia welcome email)

**Invariantes:**
1. Email único globalmente (verificado em registro)
2. Apenas `UserActive` pode fazer login
3. Soft delete: preserva dados históricos mas invalida acesso

**Ports de Saída:**
- `UserRepository`
- `EmailService` (adapter: envio via SES)

**Domain Service:**
- `UserService` — orquestra registro, login, validação de credenciais

**Dependências externas:**
- Nenhuma dependência de outros contextos (é o contexto mais upstream)

---

## Interações Entre Contextos

### Briefing → Workspace
- **Tipo:** data (foreign key)
- **Como:** `BriefingSession` guarda `WorkspaceId` para multi-tenancy
- **Por quê:** Todo briefing pertence a um workspace, queries sempre filtram por workspace

### Proposal → Briefing
- **Tipo:** data (foreign key)
- **Como:** `Proposal` guarda `BriefingSessionId` como origem
- **Por quê:** Proposta é criada a partir de um briefing completado

### Proposal → Workspace
- **Tipo:** data (foreign key)
- **Como:** `Proposal` guarda `WorkspaceId` para multi-tenancy
- **Por quê:** Proposta pertence ao workspace que criou o briefing

### Workspace → User
- **Tipo:** data (foreign key)
- **Como:** `Workspace` guarda `UserId` do owner; `WorkspaceMember` guarda `UserId`
- **Por quê:** Relacionamento de ownership e membership

### Listeners (Event-Driven)
- **Briefing → Proposal**: `BriefingCompletedEvent` → `BriefingCompletedListener` (fallback question generation, pode disparar geração de proposta no futuro)
- **Proposal → Email/PDF**: `ProposalApprovedEvent` → `ProposalApprovalListener` (gera PDF, envia email)
- **User → Email**: `UserRegisteredEvent` → `UserRegistrationListener` (envia welcome email)

### Comunicação via Outbox Pattern
Todos os domain events vão para a tabela `outbox_events` antes do RabbitMQ. `OutboxEventPublisher` (scheduled job a cada 5s) publica para o broker e marca como published. Isso garante **exatamente uma vez** de entrega mesmo em caso de falha.

---

## Decisões Arquiteturais

1. **Sealed Classes (Java 21)**: Type safety para estados de agregados, pattern matching nas transições
2. **Records para Value Objects e Events**: Imutabilidade zero-boilerplate
3. **Outbox Pattern**: Garantia de entrega de eventos mesmo com falha de RabbitMQ durante transação
4. **Multi-tenancy via WorkspaceId**: Filtro obrigatório em todos os queries, compound indexes `(workspace_id, id)`
5. **Idempotency em Listeners**: Chave `{listener-id}:{entity-id}` garante processamento único de eventos duplicados
6. **Domain Services no core/domain**: Orquestração pura sem Spring, mantém domínio isolado
7. **Repository Ports no domínio**: Interfaces definidas no domínio, implementadas nos adapters JPA

---

## Próximos Passos (Recomendações)

1. **Adicionar ADRs** para decisões principais (sealed classes, outbox, multi-tenancy)
2. **Mapear transações cruzadas**: Verificar se há SAGAs implícitas que precisam ser explicitadas
3. **Documentar eventos de integração**: Separar domain events internos de integration events externos
4. **Considerar Event Sourcing** para Briefing (audit trail completo de respostas)
5. **Adicionar Context Map visual** (diagrama C4 ou PlantUML)

---

## Glossário de Termos de Negócio

| Termo | Definição |
|-------|-----------|
| **Briefing** | Sessão de descoberta de requisitos com cliente via perguntas estruturadas |
| **Gap** | Informação faltante detectada pela IA para completar briefing |
| **Follow-up** | Pergunta adicional gerada pela IA baseada na resposta do cliente |
| **Completion Score** | Score 0-100% de completude do briefing, threshold 80% para finalizar |
| **Proposal** | Documento comercial gerado após briefing, com escopo e preço |
| **Workspace** | Tenant/organização no SaaS, agrupa usuários e dados |
| **Service Type** | Tipo de serviço oferecido (social media, landing page, logo, etc.) |
| **Public Token** | UUID para cliente acessar briefing sem autenticação |

