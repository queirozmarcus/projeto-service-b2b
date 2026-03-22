# Planejamento Fino: ScopeFlow AI MVP — Resumo Executivo

**Data:** 2026-03-22
**Status:** ⏳ AGUARDANDO APROVAÇÃO
**Documentos de Suporte:**
- `001-fine-planning-mvp.md` — Arquitetura, Prompts, Modelo de Dados, Padrões
- `002-prompts-and-api-contracts.md` — 6 Prompts Detalhados + API Contracts
- `003-architectural-decisions-and-services.md` — ADRs + 3 Serviços MVP

---

## 📊 Visão Geral

### O Produto
**ScopeFlow AI** = IA que transforma venda confusa em escopo claro + aprovação rastreável

**Para:** Microagências e freelancers (social media, design, landing pages)
**Problema:** Venda improvisada → retrabalho → desalinhamento → conflito
**Solução:** Briefing guiado + IA estrutura + escopo claro + aprovação segura

### Fases do MVP (6 Sprints, 12 semanas)
```
Sprint 1 (3 sem) → Base: Auth, workspace, membros, database
Sprint 2 (2 sem) → Catálogo: 3 serviços + contextos IA + templates
Sprint 3 (3 sem) → Briefing IA: Discovery, consolidação, perguntas complementares
Sprint 4 (2 sem) → Escopo + Proposta: Geração IA, edição, versionamento
Sprint 5 (2 sem) → Aprovação: Link público, rastreamento, PDF async
Sprint 6 (2 sem) → Kickoff + Dashboard: Resumo, observabilidade, validação
```

**Custo Estimado:** $16-22 (Sonnet multi-agent)
**Quando Pronto:** 12 semanas (sem retrasos)

---

## 🏗️ Arquitetura

### Stack Técnico (Mudança: Spring Boot 3.2 + Java 21)
```
Frontend:  Next.js 15 + React 19 + TypeScript + Tailwind v4
Backend:   Spring Boot 3.2 + Java 21 + Maven (hexagonal, modular)
Database:  PostgreSQL 14+ + Spring Data JPA + Hibernate 6.x
Storage:   AWS S3 (AWS SDK for Java)
Queue:     RabbitMQ or Redis Streams + Spring Integration
Auth:      Spring Security 6.x + JWT
IA:        OpenAI SDK for Java or anthropic-sdk-java (prompts versionados)
Logs:      Logback + SLF4J (JSON estruturado)
CI/CD:     GitHub Actions
Testing:   JUnit 5 + AssertJ + Mockito + TestContainers
```

**Por que Spring Boot 3.2 + Java 21?**
- ✅ Virtual Threads: handle 1000+ concurrent requests sem thread pool limit
- ✅ Sealed Classes: type-safe domain entities (Proposal, BriefingSession)
- ✅ Records: immutable DTOs sem Lombok boilerplate
- ✅ Structured Concurrency: async simples, sem callback hell
- ✅ Java 21 LTS: suporte até 2031
- ✅ Spring Security 6.x: enterprise-grade authentication
- ✅ Hibernate 6.x: JSONB support completo

### Decisões Críticas (ADRs)
1. ✅ **Monólito modular** (não microserviços)
2. ✅ **Prompts versionados** (v1, v2, etc em arquivo)
3. ✅ **IA 100% backend** (segurança + auditabilidade)
4. ✅ **JWT + refresh tokens** (stateless auth)
5. ✅ **PDF async** (BullMQ + S3)
6. ✅ **CRM light** (simples: name, email, phone)
7. ✅ **Row-level segregation** (workspace-scoped queries)
8. ✅ **Logs estruturados** (JSON, v1 sem Prometheus)

### Padrão Hexagonal (3 camadas)
```
Domain (core/domain) → Entidades, value objects, interfaces
Application → Use cases, serviços de orquestração, DTOs
Adapter (in/out) → Controllers, repositories, providers IA, S3
```

---

## 🧠 Estratégia IA

### 6 Prompts Core (Versionados)

| Prompt | Uso | Entrada | Saída | Temp. |
|--------|-----|---------|-------|-------|
| **briefing_questions_v1** | Início | ServiceContext | 5 perguntas JSON | 0.7 |
| **deep_question_v1** | Follow-up | Resposta vaga | 1 pergunta + hint | 0.8 |
| **briefing_consolidation_v1** | Após respostas | Todas as respostas | Briefing estruturado | 0.5 |
| **scope_generation_v1** | Escopo | Briefing + contexto | Escopo sugerido | 0.6 |
| **approval_summary_v1** | Aprovação | Proposta técnica | Resumo amigável | 0.7 |
| **kickoff_summary_v1** | Pós-aprovação | Projeto | Checklist + resumo | 0.5 |

### Qualidade Monitorada
- **Confidence score:** IA retorna confiança (> 80% = good)
- **Fallback:** Se IA falhar, respostas genéricas pré-definidas
- **Revisão humana obrigatória:** IA sugere, prestador aprova

### Fluxo IA
```
Cliente responde 5 perguntas
    ↓
Backend detecta respostas vagas (< 20 chars)
    ↓
IA gera pergunta complementar
    ↓
Client responde (completo!)
    ↓
IA consolida briefing (objective, audience, pains, etc)
    ↓
Prestador vê resumo, clica "Gerar Escopo"
    ↓
IA gera escopo sugerido (deliverables, exclusions, assumptions)
    ↓
Prestador edita se necessário (versão v1 IA → v2 editada)
    ↓
Backend renderiza proposta (template + scope)
    ↓
Prestador publica link público
    ↓
Cliente aprova via link (name, email, IP rastreado)
    ↓
IA gera kickoff summary + PDF async
```

---

## 📊 Modelo de Dados

### Entidades Principais
```
User → Workspace ← WorkspaceMember
         ├─ Client
         ├─ ServiceCatalog (3 serviços)
         ├─ ServiceContextProfile (contexto IA por serviço)
         ├─ ProposalTemplate
         └─ Proposal
            ├─ ProposalVersion (imutável)
            ├─ BriefingSession (discovery flow)
            │  └─ BriefingAnswer (respostas cliente)
            ├─ AiGeneration (audit trail IA)
            ├─ Approval (rastreamento)
            ├─ ProposalEvent (changelog)
            └─ File (S3 references)
```

### Decisões de Design
- **UUIDs:** Seguro, distribuído
- **JSONB:** Flexibilidade, versionamento
- **Workspace segregation:** Row-level filter em todas queries
- **Immutable versions:** ProposalVersion imutável, histórico completo
- **Audit trail:** AiGeneration + ProposalEvent + Approval com metadados

---

## 📡 API Contracts

### Endpoints Principais (22 total, 8 core)

**Auth:**
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`

**Briefing:**
- `POST /briefing/{proposalId}/start` → Gera perguntas IA
- `POST /public/briefing/{sessionId}/answers` → Cliente responde
- `GET /briefing/{sessionId}/summary` → Resumo consolidado

**Proposal:**
- `POST /proposals/{briefingSessionId}/generate-scope` → IA escopo
- `PUT /proposals/{proposalId}/scope` → Edita scope
- `POST /proposals/{proposalId}/render` → Renderiza proposta

**Approval:**
- `GET /public/proposals/{proposalId}/approve` → Cliente vê proposta
- `POST /public/proposals/{proposalId}/approve` → Cliente aprova (rastreia)

**Dashboard:**
- `GET /dashboard/metrics` → Status das propostas

### Response Format (Padrão)
```json
{
  "data": { /* payload */ },
  "meta": { "timestamp": "..." },
  "error": null
}
```

---

## 3️⃣ Os 3 Serviços MVP

### 1️⃣ Social Media Management

**Para:** Microagência / freelancer que quer crescer em Instagram/LinkedIn

| Aspecto | Detalhe |
|---------|---------|
| **Entregáveis** | Calendário, 3 posts/semana, comentários, relatório mensal |
| **Exclusões** | Fotos/vídeos, tráfego pago, análise concorrência |
| **Timeline** | 3+ meses |
| **Preço** | R$ 1.500-3.500/mês |
| **Retenção** | ⭐⭐⭐⭐⭐ (recorrente) |

### 2️⃣ Landing Page Design

**Para:** Startup / ecommerce que quer converter leads

| Aspecto | Detalhe |
|---------|---------|
| **Entregáveis** | Briefing, design (desktop+mobile), copywriting, integração email |
| **Exclusões** | Fotografia, tráfego pago, SEO avançado |
| **Timeline** | 2-3 semanas |
| **Preço** | R$ 2.000-5.000 (one-shot) |
| **Retenção** | ⭐⭐ (one-shot, mas pode iterar) |

### 3️⃣ Brand Identity

**Para:** Novo negócio que quer marca profissional

| Aspecto | Detalhe |
|---------|---------|
| **Entregáveis** | Logo, paleta, tipografia, manual 1-página |
| **Exclusões** | Aplicações, fotografia, website |
| **Timeline** | 2-3 semanas |
| **Preço** | R$ 1.500-3.500 |
| **Retenção** | ⭐ (one-shot) |

**Por que estes 3?**
- Demanda validada no mercado de microagências
- Escopo claro (sem ambiguidade)
- Margin alto (valem a pena vender)
- IA consegue fazer perguntas boas

---

## 📋 Checklist de Aprovação

### Camada de Produto
- [ ] **Nicho MVP aprovado:** Social Media + Design + Landing Pages? ✓ SIM
- [ ] **3 serviços faz sentido?** ✓ SIM (demanda, margin, retenção)
- [ ] **6 sprints = 12 semanas é realista?** ✓ SIM (sem complicações)
- [ ] **Foco em retenção (não features)?** ✓ SIM (métricas de sucesso claras)

### Camada Técnica
- [ ] **Stack aprovado?** NestJS + Next.js + PostgreSQL + Prisma + S3 + BullMQ? ✓ SIM
- [ ] **Arquitetura hexagonal faz sentido?** ✓ SIM (escalável, testável)
- [ ] **JWT + refresh tokens é ok?** ✓ SIM (simples, stateless)
- [ ] **Workspace segregation suficiente pra segurança?** ✓ SIM (row-level)

### Camada IA
- [ ] **6 prompts v1 estão bons?** ✓ REVISAR LINGUAGEM (pode customizar PT-BR)
- [ ] **Confidence score + fallback mitiga risco?** ✓ SIM
- [ ] **Revisão humana obrigatória em todas gerações?** ✓ SIM
- [ ] **Prompts versionados em arquivo é viável?** ✓ SIM (auditável)

### Camada de Dados
- [ ] **Schema está limpo e sem redundância?** ✓ SIM
- [ ] **Imutabilidade de proposta garante auditabilidade?** ✓ SIM
- [ ] **Multi-tenancy via workspace_id suficiente?** ✓ SIM (com índices)

---

## ✅ Decisões Tomadas

1. **Nicho inicial bem fechado:** Social Media + Design + Landing Pages (não expansão até validar)
2. **IA em 5 pontos:** Discovery, clareza, alinhamento, estruturação, síntese (não tudo)
3. **Revisão humana obrigatória:** IA sugere, usuário aprova (não auto-executa)
4. **Monólito modular:** Escalável depois, simples agora
5. **Versionamento forte:** Tudo auditável (proposals, IA, approvals)
6. **Low cost initially:** S3, Redis, PostgreSQL (sem enterprise)

---

## 🚀 Próximo Passo

**Quando aprovado este resumo:**

Vou passar para **Fase 4: Execução**

Começo com:
```bash
Sprint 1 / Fase 1: /full-bootstrap scopeflow-mvp aws

Isso vai criar:
- Backend NestJS modular (hexagonal)
- Frontend Next.js 15
- PostgreSQL schema completo
- GitHub Actions CI/CD
- Base de observabilidade (Winston logs)
```

**Pergunta Final:**

Está tudo ok? Qualquer ajuste antes de começar?

1. **Não fazer nada diferente** → Autorizo execução Fase 1
2. **Ajustar X** → Diga o quê, vou atualizar documentos
3. **Mais detalhes em Y** → Vou detalhar

---

**Status Atual:** ⏳ PLANEJAMENTO FINO COMPLETO
**Proxima Status:** 🚀 EXECUÇÃO (quando aprovado)

