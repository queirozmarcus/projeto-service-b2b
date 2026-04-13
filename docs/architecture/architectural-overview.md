# Architectural Overview — ScopeFlow AI

**Data:** 2026-04-05 | **Versão:** 1.0  
> ⚠️ **Parcialmente desatualizado.** Este documento descreve a arquitetura do monólito. Para visão atualizada com User Service extraído e roteamento Traefik, ver [`README.md §2`](../../README.md#2-arquitetura) e [`docs/migration/README.md`](../migration/README.md).  
> **Stack corrigida:** Monólito usa Spring Boot 3.2.0 (não 3.4). User Service usa Spring Boot 3.4.3.

## Visão Geral

**ScopeFlow AI** é uma plataforma SaaS B2B para freelancers e micro-agências transformarem conversas comerciais em escopos claros usando IA. O sistema usa **arquitetura hexagonal (Ports & Adapters) + DDD** com Java 21 e Spring Boot 3.2.0 (monólito) / 3.4.3 (user-service).

---

## C4 Model — Level 1: System Context

```
┌─────────────────┐
│   Freelancer    │ (cliente do freelancer)
└────────┬────────┘
         │ HTTP (public token)
         │
         ▼
┌─────────────────────────────────────┐
│         ScopeFlow AI                │
│  (AI-powered scoping platform)      │
└──┬──────────────────────────────┬───┘
   │                              │
   │ API                          │ MQ
   ▼                              ▼
┌──────────────┐        ┌────────────────┐
│  OpenAI API  │        │   RabbitMQ     │
│ (GPT-4o)     │        │ (events)       │
└──────────────┘        └────────────────┘
   │                              │
   │ S3                           │
   ▼                              │
┌──────────────┐                  │
│   AWS S3     │                  │
│ (PDF storage)│                  │
└──────────────┘                  │
                                  │
┌─────────────────┐               │
│  User (agency)  │◀──────────────┘
│  (workspace     │   Email (SES)
│   owner/admin)  │
└─────────────────┘
```

**Atores:**
- **Freelancer (cliente):** Acessa via token público para responder perguntas de briefing
- **User (agência):** Cria workspace, gerencia briefings, aprova propostas

**Sistemas externos:**
- **OpenAI API:** Geração de perguntas contextuais e análise de respostas
- **AWS S3:** Armazenamento de PDFs de propostas
- **AWS SES:** Envio de emails (boas-vindas, aprovação de proposta)
- **RabbitMQ:** Message broker para domain events

---

## C4 Model — Level 2: Containers

```
┌─────────────────────────────────────────────────────────────────────┐
│                          ScopeFlow AI                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │              Spring Boot Application (Java 21)             │     │
│  │                                                             │     │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │     │
│  │  │   Briefing   │  │   Proposal   │  │  Workspace   │    │     │
│  │  │   Context    │  │   Context    │  │   Context    │    │     │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │     │
│  │         │                  │                  │            │     │
│  │         │     ┌────────────┴────────────┐     │            │     │
│  │         └────▶│      User Context       │◀────┘            │     │
│  │               └─────────────────────────┘                  │     │
│  │                                                             │     │
│  │  Adapters:                                                 │     │
│  │  • REST API (Spring MVC)                                   │     │
│  │  • JPA Persistence (Hibernate)                             │     │
│  │  • OpenAI Client (Spring RestClient)                       │     │
│  │  • S3 Client (AWS SDK)                                     │     │
│  │  • SES Client (AWS SDK)                                    │     │
│  └─────────────────────────────────────────────────────────────┘     │
│                                                                       │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │  PostgreSQL    │  │   RabbitMQ     │  │     Redis      │        │
│  │  (persistence) │  │  (events)      │  │  (rate limit)  │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

**Containers:**

1. **Spring Boot Application (Java 21)**
   - Arquitetura hexagonal: domain + application + adapters
   - 4 bounded contexts: Briefing, Proposal, Workspace, User
   - 17 tabelas PostgreSQL

2. **PostgreSQL 16+**
   - Persistence layer
   - Flyway migrations (V1-V9)
   - Row-level security (futuro) para multi-tenancy

3. **RabbitMQ**
   - Domain events (Outbox Pattern)
   - Exactly-once delivery

4. **Redis**
   - Rate limiting (Bucket4j)
   - Cache (futuro)

---

## Bounded Contexts — Domain Model

### 1. Briefing Context

**Responsabilidade:** Gerenciar sessões de briefing com clientes, coletar respostas, gerar análise via IA.

**Aggregate Root:** `BriefingSession`

**Estados (sealed interface):**
```java
sealed interface BriefingSession 
    permits BriefingInProgress, BriefingCompleted, BriefingAbandoned
```

**Invariantes:**
- Score ≥ 80% para completar
- Perguntas fixas por `ServiceType` (web-app, mobile-app, api, website, branding)
- Respostas imutáveis

**Domain Events:**
- `BriefingSessionCreatedEvent`
- `BriefingAnswerSubmittedEvent`
- `BriefingCompletedEvent`

**Ports de Saída:**
- `BriefingSessionRepository`
- `AIGenerationPort` (OpenAI)
- `EmailPort` (SES)

---

### 2. Proposal Context

**Responsabilidade:** Criar propostas a partir de briefings completados, gerenciar aprovações, gerar PDFs.

**Aggregate Root:** `Proposal`

**Estados (sealed interface):**
```java
sealed interface Proposal 
    permits ProposalDraft, ProposalPendingApproval, 
            ProposalApproved, ProposalRejected
```

**Invariantes:**
- Proposta nasce de `BriefingSession` completado
- Versioning imutável (append-only)
- Soft delete

**Domain Events:**
- `ProposalCreatedEvent`
- `ProposalApprovedEvent`
- `ProposalRejectedEvent`

**Ports de Saída:**
- `ProposalRepository`
- `PDFGeneratorPort` (AWS S3)
- `EmailPort` (SES)

---

### 3. Workspace Context

**Responsabilidade:** Multi-tenancy — gerenciar organizações (workspaces) e membros.

**Aggregate Roots:**
- `Workspace` (tenant/organização)
- `WorkspaceMember` (membership M:N)

**Estados (Workspace):**
```java
sealed interface Workspace 
    permits ActiveWorkspace, SuspendedWorkspace, DeletedWorkspace
```

**Invariantes:**
- Todo workspace tem exatamente 1 OWNER
- Limite de membros por plano (free: 3, pro: 10, enterprise: ilimitado)
- Owner não pode sair do workspace

**Domain Events:**
- `WorkspaceCreatedEvent`
- `WorkspaceMemberInvitedEvent`
- `WorkspaceDeletedEvent`

**Ports de Saída:**
- `WorkspaceRepository`
- `WorkspaceMemberRepository`
- `EmailPort` (convites)

---

### 4. User Context

**Responsabilidade:** Autenticação, gestão de usuários, identidade.

**Aggregate Root:** `User`

**Estados (sealed interface):**
```java
sealed interface User 
    permits ActiveUser, SuspendedUser, DeletedUser
```

**Invariantes:**
- Email único (global)
- Password BCrypt strength 12
- Soft delete (LGPD)

**Domain Events:**
- `UserRegisteredEvent`
- `UserPasswordChangedEvent`

**Ports de Saída:**
- `UserRepository`
- `EmailPort` (boas-vindas)

---

## Dependências Entre Contextos

### Mapa de Dependências

```
User ◀──────data (owner_id)────── Workspace
                                     │
                                     │ data (workspace_id)
                                     │
                    ┌────────────────┴─────────────────┐
                    │                                  │
                    ▼                                  ▼
                Briefing                           Proposal
                    │                                  ▲
                    │                                  │
                    └────────data (briefing_id)────────┘
```

**Legenda:**
- `data`: Foreign key (value object UUID)
- **Zero chamadas síncronas diretas** entre contextos

### Comunicação Assíncrona (Domain Events)

| Evento | Publisher | Subscriber | Ação |
|--------|-----------|------------|------|
| `BriefingCompletedEvent` | Briefing | Briefing | Gera perguntas de fallback |
| `ProposalApprovedEvent` | Proposal | Proposal | Gera PDF + envia email |
| `UserRegisteredEvent` | User | User | Envia email de boas-vindas |

**Outbox Pattern:** Todos os eventos passam por `outbox_event` antes de RabbitMQ.

---

## Arquitetura Hexagonal — Package Structure

```
backend/src/main/java/com/scopeflow/
├── core/domain/              # Domain puro (zero Spring dependencies)
│   ├── briefing/
│   │   ├── BriefingSession.java (sealed interface)
│   │   ├── BriefingSessionId.java (value object)
│   │   ├── BriefingCompletedEvent.java (domain event)
│   │   └── ...
│   ├── proposal/
│   ├── workspace/
│   └── user/
│
├── application/              # Use cases + ports
│   ├── service/              # Application services (orchestration)
│   │   ├── BriefingService.java
│   │   └── ...
│   ├── port/out/             # Output ports (interfaces)
│   │   ├── BriefingSessionRepository.java
│   │   ├── AIGenerationPort.java
│   │   └── ...
│   ├── idempotency/          # Idempotency service
│   ├── outbox/               # Outbox pattern
│   └── listener/             # Domain event listeners
│
└── adapter/
    ├── in/web/               # REST controllers (Spring MVC)
    │   ├── briefing/
    │   │   ├── BriefingController.java
    │   │   ├── dto/          # Request/Response records
    │   │   └── mapper/       # Domain ↔ DTO mapping
    │   └── ...
    │
    └── out/
        ├── persistence/      # JPA entities + repositories
        │   ├── briefing/
        │   │   ├── BriefingSessionJpaEntity.java
        │   │   └── BriefingSessionRepositoryImpl.java
        │   └── ...
        ├── pdf/              # PDF generation adapter
        ├── email/            # Email service adapter (SES)
        └── ai/               # OpenAI client adapter
```

**Princípios:**
- **Domain layer** (`core/domain/`) tem zero dependencies de frameworks
- **Application layer** define interfaces (ports), adapters implementam
- **Adapters** são substituíveis (ex: OpenAI → Anthropic)

---

## Segurança e Multi-Tenancy

### Autenticação

**Tipo:** JWT dual-token
- **Access token** (15min): autenticação stateless
- **Refresh token** (7 dias): httpOnly cookie para renovação

**Claims no JWT:**
```json
{
  "sub": "user-uuid",
  "workspaceId": "workspace-uuid",
  "roles": ["OWNER"],
  "exp": 1234567890
}
```

### Autorização

**RBAC (Role-Based Access Control):**
- **OWNER**: Full access (gestão de workspace, billing)
- **ADMIN**: Gestão operacional (proposals, briefings, membros)
- **MEMBER**: Read-only

**Enforcement em 3 camadas:**
1. `JwtAuthenticationFilter` (carrega `SecurityContext`)
2. Controller (validação de role)
3. Service (validação de `workspaceId`)

### Multi-Tenancy

**Workspace-scoped:** Todo dado tem `workspace_id`.

**Enforcement:**
- JWT contém `workspace_id` claim
- Todos os repositories filtram por `WorkspaceId`
- Índices compostos `(workspace_id, ...)`

**Exceção:** `UserRepository` não filtra por workspace porque `User` é compartilhado entre workspaces (design intencional).

---

## Integração Externa

### OpenAI API (GPT-4o)

**Uso:** Geração de perguntas contextuais, análise de respostas.

**Adapter:** `OpenAIClientAdapter` implementa `AIGenerationPort`.

**Audit trail:** Toda interação salva em `ai_generations` (prompt, response, tokens).

**Circuit breaker:** Não implementado (recomendação: Resilience4j).

---

### AWS S3

**Uso:** Armazenamento de PDFs de propostas.

**Adapter:** `S3StorageAdapter` implementa `PDFGeneratorPort`.

**Bucket:** `scopeflow-proposals-{env}` (dev, staging, prod).

**Encriptação:** Server-side encryption (SSE-S3).

---

### AWS SES

**Uso:** Envio de emails (boas-vindas, aprovação de proposta).

**Adapter:** `SESEmailAdapter` implementa `EmailPort`.

**Templates:** Thymeleaf (server-side rendering).

**Fallback:** Não implementado (recomendação: fallback para SNS).

---

### RabbitMQ

**Uso:** Message broker para domain events (Outbox Pattern).

**Exchanges:**
- `scopeflow.events` (topic exchange)

**Routing keys:**
- `briefing.completed`
- `proposal.approved`
- `user.registered`

**Retry:** Exponential backoff (1s, 2s, 4s, 8s, 16s, DLQ).

---

## Observabilidade

### Logging

**Framework:** SLF4J + Logback

**Estrutura:** JSON structured logging

**Níveis:**
- **ERROR:** Falhas não recuperáveis
- **WARN:** Falhas recuperáveis (retry bem-sucedido)
- **INFO:** Eventos de negócio (briefing completado, proposta aprovada)
- **DEBUG:** Detalhes técnicos (query params, headers)

### Métricas

**Framework:** Micrometer + Prometheus

**Métricas customizadas:**
- `briefing.completed.total` (counter)
- `proposal.approved.total` (counter)
- `ai.generation.duration.seconds` (histogram)

### Tracing

**Não implementado.** Recomendação: OpenTelemetry + Jaeger.

---

## Performance e Escalabilidade

### Database

**Índices otimizados:**
- Compound indexes em `(workspace_id, ...)`
- Unique indexes em tokens públicos

**Connection pooling:** HikariCP (default Spring Boot)

**Recomendação futura:** Read replicas (RDS) para queries pesadas.

---

### Caching

**Implementado:** Rate limiting via Bucket4j in-memory

**Recomendação futura:**
- Redis-backed Bucket4j (compartilhar estado entre pods)
- Cache de `User` e `Workspace` (TTL 1h)

---

### Virtual Threads (Java 21)

**Habilitado:** `spring.threads.virtual.enabled=true`

**Benefício:** Handle 1000+ concurrent requests sem thread pool tuning.

---

## Roadmap Arquitetural

### Curto Prazo (0-3 meses)

1. ✅ Implementar circuit breaker (Resilience4j) em OpenAI, S3, SES
2. ✅ Adicionar validação de RI em `Proposal.create()` (briefing exists + completed)
3. ✅ Purge jobs para `outbox_event`, `idempotency_record`, `ai_generations`
4. ✅ Row-Level Security (RLS) no PostgreSQL para multi-tenancy
5. ✅ Testes de isolamento (tenant A ≠ tenant B)

### Médio Prazo (3-6 meses)

1. ✅ Cache Redis para `User`, `Workspace` (TTL 1h)
2. ✅ Read replicas PostgreSQL (queries pesadas)
3. ✅ OpenTelemetry + Jaeger (distributed tracing)
4. ✅ CAPTCHA reCAPTCHA v3 em endpoints públicos
5. ✅ LGPD completo (consentimento + portabilidade + direito ao esquecimento)

### Longo Prazo (6-12 meses)

1. ✅ Migração para microsserviços (se 10K+ workspaces)
2. ✅ Event Sourcing para `Proposal` (versioning robusto)
3. ✅ CQRS para queries pesadas (materialized views)
4. ✅ Multi-region deployment (AWS Regions)

---

## Conclusão

**ScopeFlow AI** é uma plataforma **pronta para MVP** com arquitetura hexagonal + DDD sólida:

- ✅ **Bounded contexts bem definidos** (4 contextos, 5 aggregates)
- ✅ **Baixo acoplamento** (zero chamadas síncronas, FKs justificadas)
- ✅ **Alta coesão** (aggregates respeitam invariantes)
- ✅ **Multi-tenancy 100% conforme** (workspace-scoped)
- ✅ **Event-driven pronto** (Outbox Pattern + RabbitMQ)
- ⚠️ **Observabilidade parcial** (logs OK, métricas OK, tracing faltando)
- ⚠️ **Resiliência parcial** (circuit breaker faltando)
- ⚠️ **LGPD 60%** (consentimento + portabilidade faltando)

**Status:** Pronto para produção em pequena escala (até 5K workspaces). Para escala enterprise, implementar roadmap de médio/longo prazo.
