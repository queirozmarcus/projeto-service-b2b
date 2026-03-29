# Plano ScopeFlow AI — 10 Sprints até MVP em Produção
**Data:** 2026-03-29
**Status:** APROVADO
**Duração:** 10 sprints × 2 semanas = ~20 semanas

---

## Contexto

### Estado Atual do Projeto
| Camada | Status | Observações |
|--------|--------|-------------|
| **Backend Java 21 + Spring Boot 3.2** | ✅ ~70% | Domínio completo: Auth, Workspace, Briefing, Proposal, Approval, AI, PDF, Outbox |
| **Frontend Next.js 15 + React 19** | 🔴 ~20% | CSS quebrado (Tailwind v4 sem PostCSS), páginas parcialmente criadas |
| **Database PostgreSQL + Flyway** | ✅ ~85% | 8 migrations, schema completo para MVP |
| **Infra Docker + RabbitMQ** | ✅ docker-compose | Ambiente local funcional |
| **CI/CD GitHub Actions** | ✅ criado | Workflows criados mas não validados end-to-end |
| **Observabilidade** | ❌ | Métricas, dashboards, alertas não configurados |
| **Testes** | ❌ | Sem cobertura unitária nem integração |
| **Segurança** | 🔄 | JWT implementado, OWASP não auditado |

### Decisões Arquiteturais
- Monólito modular (hexagonal) para MVP — sem microsserviços ainda
- Java 21 virtual threads para chamadas OpenAI assíncronas
- Outbox Pattern para eventos confiáveis (email, PDF, notificações)
- Tailwind v4 CSS-first + Framer Motion para animações
- PostgreSQL JSONB para respostas de briefing e scope
- AWS SES para email, S3 para PDF/assets

---

## Sprint 1 — Fix Foundations & CSS (Semanas 1–2)

**Objetivo:** Landing page e dashboard funcionando 100% com estilo correto.

**Problema bloqueante:** Tailwind v4 sem `@tailwindcss/postcss` → zero CSS aplicado.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 1.1 | Instalar `@tailwindcss/postcss`, criar `postcss.config.mjs`, remover `tailwind.config.js` legacy | `backend-dev` | CSS funcionando, dark theme visível |
| 1.2 | Definir `animate-ambient-pulse` no globals.css + corrigir animações | `backend-dev` | Hero sem erros de animação |
| 1.3 | Verificar e corrigir classes v4-incompatíveis no Dashboard + Auth pages | `code-reviewer` | Todas as páginas styled |
| 1.4 | Validar responsividade: mobile (375px), tablet (768px), desktop (1440px) | `backend-dev` | Landing responsiva |
| 1.5 | Configurar `next.config.js` com `@tailwindcss/next` se disponível | `backend-dev` | Build de produção passando |
| 1.6 | Review geral do CSS e design tokens | `code-reviewer` | Sem regressões visuais |

**Commands:**
```bash
/dev-review frontend/src/
/dev-feature "fix Tailwind v4 PostCSS config e animações"
```

**Quality Gate:** Screenshot do Playwright mostrando dark theme (#09090E background) + amber primary.

---

## Sprint 2 — Auth & Workspace UI (Semanas 3–4)

**Objetivo:** Fluxo completo de registro → login → criar workspace → dashboard funcionando end-to-end.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 2.1 | Conectar `AuthControllerV2` ao frontend: register form + validação Zod | `backend-dev` | Formulário de cadastro funcional |
| 2.2 | Login com JWT: armazenar access + refresh token, interceptor Axios | `backend-dev` | Login persistente entre refreshes |
| 2.3 | Middleware Next.js: rotas protegidas redirecionam para /auth/login | `backend-dev` | Guards de autenticação |
| 2.4 | Workspace setup wizard: nome, nicho, branding básico | `backend-dev` | Wizard de onboarding |
| 2.5 | Convidar membros (admin role): UI + integração com `WorkspaceControllerV2` | `backend-dev` | Gestão de membros |
| 2.6 | Testes: auth flow unitário + integração com Testcontainers | `unit-test-engineer` + `integration-test-engineer` | Cobertura 100% em auth |

**Commands:**
```bash
/dev-feature "auth completo: register, login, JWT refresh, workspace setup"
/qa-generate AuthControllerV2
/qa-generate WorkspaceService
```

**Quality Gate:** E2E: register → login → create workspace → reach dashboard em < 30s.

---

## Sprint 3 — Dashboard Core (Semanas 5–6)

**Objetivo:** Dashboard operacional com overview de métricas e navegação completa.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 3.1 | Layout principal: sidebar, topbar, breadcrumbs | `backend-dev` | Shell do dashboard |
| 3.2 | Página overview: KPIs (briefings/mês, taxa aprovação, tempo médio) | `backend-dev` | Cards de métricas |
| 3.3 | API endpoint: `GET /api/v1/workspaces/{id}/stats` | `backend-dev` + `api-designer` | Endpoint de stats com queries otimizadas |
| 3.4 | Lista de propostas com status (rascunho, enviada, aprovada, rejeitada) | `backend-dev` | Tabela de propostas paginada |
| 3.5 | Estado vazio: primeira vez sem dados — orientar para criar briefing | `backend-dev` | Empty states UX |
| 3.6 | Settings: workspace config, perfil do usuário | `backend-dev` | Páginas de configuração |
| 3.7 | Queries N+1: revisar `ProposalService` e `WorkspaceService` | `dba` | Zero N+1 queries |

**Commands:**
```bash
/dev-feature "dashboard overview com KPIs e lista de propostas"
/data-optimize "SELECT proposals with workspace stats"
```

**Quality Gate:** Dashboard carrega em < 2s com 50 propostas simuladas.

---

## Sprint 4 — Client & Service Catalog (Semanas 7–8)

**Objetivo:** CRUD completo de clientes e catálogo de serviços com perfis de contexto para IA.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 4.1 | UI: listagem e CRUD de clientes (nome, email, empresa, histórico) | `backend-dev` | Módulo de clientes |
| 4.2 | UI: catálogo de serviços (Social Media, Landing Page, Web, Branding, etc.) | `backend-dev` | Módulo de serviços |
| 4.3 | UI: ServiceContextProfile — editar perguntas-template, entregáveis padrão, exclusões | `backend-dev` | Editor de contexto por serviço |
| 4.4 | UI: ProposalTemplates — HTML templates com placeholders por serviço | `backend-dev` | Templates de proposta |
| 4.5 | Seed: popular catálogo inicial com 5 tipos de serviço (dados realistas) | `dba` | Seed script com dados de exemplo |
| 4.6 | Testes: ClientService + ServiceContextProfile integração | `integration-test-engineer` | Cobertura 80%+ |

**Commands:**
```bash
/dev-feature "CRUD clients e service catalog com context profiles"
/data-migrate "seed service_catalog com 5 tipos de serviço"
```

**Quality Gate:** Criar um cliente, associar um serviço e configurar contexto em < 5 passos.

---

## Sprint 5 — Briefing Session UI (Semanas 9–10)

**Objetivo:** Fluxo público de briefing funcionando: link compartilhável → cliente responde → sessão concluída.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 5.1 | UI: criar sessão de briefing (selecionar cliente, serviço, gerar link) | `backend-dev` | Form de criação de briefing |
| 5.2 | Página pública `/briefing/[token]`: multi-step form com perguntas | `backend-dev` | Form público sem auth |
| 5.3 | Progress bar: mostrar completude (%) conforme perguntas respondidas | `backend-dev` | Indicador de progresso |
| 5.4 | Integração com `PublicBriefingControllerV1` + `BriefingSessionService` | `backend-dev` | Submissão de respostas |
| 5.5 | Follow-up automático: se resposta vaga → carregar pergunta de aprofundamento | `backend-dev` | Lógica de follow-up no frontend |
| 5.6 | Página de sucesso após conclusão: "briefing enviado, aguarde o escopo" | `backend-dev` | Tela de conclusão |
| 5.7 | Dashboard: view do briefing respondido com todas as respostas | `backend-dev` | Tela de revisão de respostas |
| 5.8 | Testes E2E: fluxo completo de briefing | `e2e-test-engineer` | Testes Playwright para briefing flow |

**Commands:**
```bash
/dev-feature "briefing session: criar, link público, multi-step, conclusão"
/qa-e2e "fluxo completo de briefing: criar sessão até conclusão pelo cliente"
```

**Quality Gate:** Cliente completa briefing de 10 perguntas em < 5 minutos sem auxílio.

---

## Sprint 6 — AI Integration Completa (Semanas 11–12)

**Objetivo:** OpenAI integrado com geração de perguntas, análise de gaps e consolidação de escopo.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 6.1 | Revisar e completar `BriefingService`: geração de perguntas via OpenAI por ServiceContext | `backend-dev` | Perguntas adaptadas ao nicho |
| 6.2 | Gap analysis: detectar respostas vagas e gerar follow-ups automáticos | `backend-dev` | GapAnalysis funcionando |
| 6.3 | Consolidação: `BriefingCompletedListener` → gerar scope estruturado em JSONB | `backend-dev` | Scope gerado automaticamente |
| 6.4 | Versioning de prompts: `prompts/` folder + referência em `ai_generations` | `backend-dev` | Auditoria de prompts versionados |
| 6.5 | Rate limiting por workspace: evitar abuso da API OpenAI | `backend-dev` | Rate limit por workspace no `RateLimitInterceptor` |
| 6.6 | Cache de contextos: evitar chamar OpenAI para cada pergunta de mesmo serviço | `backend-dev` | Redis cache para ServiceContextProfile |
| 6.7 | Testes: mock OpenAI em unit tests, real API em integration (Testcontainers + WireMock) | `unit-test-engineer` + `integration-test-engineer` | Cobertura completa sem chamadas reais nos CIs |
| 6.8 | Segurança: validar prompt injection, sanitizar inputs do usuário | `security-test-engineer` | Relatório de vulnerabilidades de prompt |

**Commands:**
```bash
/dev-feature "OpenAI integration: geração de perguntas, gap analysis, scope consolidation"
/qa-generate BriefingService
/qa-security "validar prompt injection no BriefingService"
```

**Quality Gate:** Briefing de social media gera scope estruturado em < 10s com 3+ entregáveis relevantes.

---

## Sprint 7 — Proposal UI & PDF (Semanas 13–14)

**Objetivo:** Editor de escopo, proposta enviável, PDF profissional, download e preview.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 7.1 | UI: editor de escopo (entregáveis, exclusões, timeline, valor) sobre o scope gerado pela IA | `backend-dev` | Scope editor com rich editing |
| 7.2 | Integração com `ProposalControllerV2`: criar e atualizar proposta | `backend-dev` | Proposal CRUD conectado |
| 7.3 | Versionamento: ao editar, criar nova `ProposalVersion` imutável | `backend-dev` | Histórico de versões na UI |
| 7.4 | Preview da proposta: render HTML com template + dados do scope | `backend-dev` | Proposal preview page |
| 7.5 | Geração de PDF: integrar `ITextPdfServiceAdapter` via fila RabbitMQ | `backend-dev` | PDF assíncrono via Outbox |
| 7.6 | Download do PDF: URL presigned do S3, disponível no dashboard | `backend-dev` | Link de download |
| 7.7 | Otimização: índices nas queries de `ProposalVersion` | `dba` | EXPLAIN ANALYZE + índices |
| 7.8 | Testes: ProposalService + PDF generation | `unit-test-engineer` + `integration-test-engineer` | Cobertura 80%+ |

**Commands:**
```bash
/dev-feature "proposal UI: editor de escopo, preview, PDF via Outbox, download S3"
/data-optimize "queries de ProposalVersion por workspace e status"
/qa-generate ProposalService
```

**Quality Gate:** Proposta criada, PDF gerado e disponível para download em < 30s.

---

## Sprint 8 — Approval Flow & Notificações (Semanas 15–16)

**Objetivo:** Fluxo de aprovação público, rastreável, com email e PDF de kickoff gerado ao aprovar.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 8.1 | Página pública `/proposals/[id]/approve?token=...`: visualizar proposta + form de aprovação | `backend-dev` | Página de aprovação pública |
| 8.2 | Form de aprovação: nome, email, checkbox de confirmação | `backend-dev` | Form de aceite |
| 8.3 | Integração com `ApprovalControllerV2`: registrar IP, timestamp, versão | `backend-dev` | Aprovação rastreável |
| 8.4 | Email: confirmação para cliente + notificação para freelancer via AWS SES | `backend-dev` | Templates de email |
| 8.5 | PDF de kickoff: gerado automaticamente ao aprovar, enviado por email | `backend-dev` | Kickoff PDF via Outbox |
| 8.6 | Dashboard: histórico de aprovações, eventos, timeline de proposta | `backend-dev` | Audit trail na UI |
| 8.7 | Rate limit no endpoint público de aprovação: anti-spam | `backend-dev` | Rate limiting por token |
| 8.8 | Testes E2E: fluxo completo briefing → proposta → aprovação | `e2e-test-engineer` | Suite E2E completa |

**Commands:**
```bash
/dev-feature "approval flow: página pública, aceite rastreável, email SES, kickoff PDF"
/qa-e2e "fluxo completo: briefing → escopo → proposta → aprovação"
```

**Quality Gate:** Cliente aprova → email enviado → PDF kickoff disponível em < 60s.

---

## Sprint 9 — QA, Segurança & Performance (Semanas 17–18)

**Objetivo:** Cobertura de testes completa, auditoria OWASP, performance validada para carga real.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 9.1 | Estratégia de testes: mapa de cobertura atual → gaps críticos | `qa-lead` | Test coverage report |
| 9.2 | Testes unitários: domínio Java (entidades, use cases, value objects) | `unit-test-engineer` | 80%+ coverage no domínio |
| 9.3 | Testes de integração: Testcontainers para PostgreSQL + RabbitMQ + Redis | `integration-test-engineer` | Integration suite funcionando |
| 9.4 | Contract tests: API entre frontend e backend (Spring Cloud Contract) | `contract-test-engineer` | Contracts para auth + briefing + proposal |
| 9.5 | Testes de segurança: OWASP Top 10, JWT bypass, IDOR, rate limit | `security-test-engineer` | Relatório OWASP sem P0/P1 abertos |
| 9.6 | Performance: load test briefing flow + proposal + PDF | `performance-engineer` | Baseline: p99 < 2s para chamadas não-IA |
| 9.7 | Corrigir todos os findings de P0/P1 dos testes de segurança | `backend-dev` | Zero vulnerabilidades críticas |

**Commands:**
```bash
/qa-audit
/qa-generate BriefingSessionService
/qa-contract briefing-service
/qa-security scopeflow
/qa-performance "briefing completion flow"
```

**Quality Gate:**
- Unit: 80%+ no domínio
- Integration: 100% dos happy paths com Testcontainers
- Security: zero P0/P1
- Performance: p99 < 2s (exceto chamadas OpenAI)

---

## Sprint 10 — DevOps, CI/CD & Produção (Semanas 19–20)

**Objetivo:** Infraestrutura AWS provisionada com Terraform, pipelines CI/CD, observabilidade e deploy em produção.

### Tasks
| # | Task | Agent | Entregável |
|---|------|-------|-----------|
| 10.1 | Dockerfiles otimizados: backend (multi-stage JDK→JRE alpine) + frontend (Node alpine) | `devops-engineer` | Imagens < 200MB |
| 10.2 | Terraform: VPC, RDS PostgreSQL, ElastiCache Redis, S3, SES, ECS Fargate | `iac-engineer` | Infra como código para staging + prod |
| 10.3 | Pipeline CI: GitHub Actions — test + lint + security scan + build | `cicd-engineer` | Pipeline CI com quality gates |
| 10.4 | Pipeline CD: deploy automático em staging, manual em produção | `cicd-engineer` | Deploy pipeline com approval gate |
| 10.5 | Observabilidade: Prometheus, Grafana dashboards (RED metrics), alertas SLO | `observability-engineer` | Dashboards para briefing + proposal + AI latency |
| 10.6 | Segurança de infra: IAM least-privilege, Vault para secrets, Network Policies | `security-ops` | Hardening completo |
| 10.7 | Runbook operacional: incident response, deploy rollback, DR | `sre-engineer` | Runbooks em `docs/devops/runbooks/` |
| 10.8 | Smoke tests pós-deploy: validar fluxo end-to-end em produção | `e2e-test-engineer` | Smoke suite para prod |

**Commands:**
```bash
/devops-provision scopeflow-api aws
/devops-pipeline scopeflow-api
/devops-observe scopeflow-api
/qa-security "hardening infra AWS"
/devops-dr scopeflow-api
```

**Quality Gate:**
- Deploy em staging sem intervenção manual
- Dashboards Grafana funcionando
- Smoke tests passando em prod
- RTO < 30min, RPO < 1h

---

## Visão Geral dos 10 Sprints

```
Sprint 1  ████████░░░░░░░░░░░░  Fix CSS + Foundations
Sprint 2  ████████░░░░░░░░░░░░  Auth & Workspace
Sprint 3  ████████░░░░░░░░░░░░  Dashboard Core
Sprint 4  ████████░░░░░░░░░░░░  Clients & Services
Sprint 5  ████████░░░░░░░░░░░░  Briefing Session UI
Sprint 6  ████████░░░░░░░░░░░░  AI Integration
Sprint 7  ████████░░░░░░░░░░░░  Proposal & PDF
Sprint 8  ████████░░░░░░░░░░░░  Approval Flow
Sprint 9  ████████░░░░░░░░░░░░  QA & Security
Sprint 10 ████████░░░░░░░░░░░░  DevOps & Produção
```

## Mapa de Agents por Sprint

| Sprint | Agents Principais | Commands |
|--------|------------------|----------|
| 1 | `code-reviewer`, `backend-dev` | `/dev-review`, `/dev-feature` |
| 2 | `backend-dev`, `unit-test-engineer`, `integration-test-engineer` | `/dev-feature`, `/qa-generate` |
| 3 | `backend-dev`, `api-designer`, `dba` | `/dev-feature`, `/data-optimize` |
| 4 | `backend-dev`, `dba`, `integration-test-engineer` | `/dev-feature`, `/data-migrate` |
| 5 | `backend-dev`, `e2e-test-engineer` | `/dev-feature`, `/qa-e2e` |
| 6 | `backend-dev`, `unit-test-engineer`, `integration-test-engineer`, `security-test-engineer` | `/dev-feature`, `/qa-generate`, `/qa-security` |
| 7 | `backend-dev`, `dba`, `unit-test-engineer` | `/dev-feature`, `/data-optimize`, `/qa-generate` |
| 8 | `backend-dev`, `e2e-test-engineer` | `/dev-feature`, `/qa-e2e` |
| 9 | `qa-lead`, `unit-test-engineer`, `integration-test-engineer`, `contract-test-engineer`, `security-test-engineer`, `performance-engineer` | `/qa-audit`, `/qa-contract`, `/qa-security`, `/qa-performance` |
| 10 | `iac-engineer`, `cicd-engineer`, `observability-engineer`, `security-ops`, `sre-engineer`, `e2e-test-engineer` | `/devops-provision`, `/devops-pipeline`, `/devops-observe`, `/devops-dr` |

## Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|-------------|---------|-----------|
| Custo OpenAI alto em dev | Alta | Médio | Cache por ServiceContext + WireMock em testes |
| PDF lento bloqueando UX | Média | Alto | Geração assíncrona via RabbitMQ + polling no frontend |
| Token de aprovação vencendo | Baixa | Alto | TTL configurável + renovação via dashboard |
| Escopo de IA genérico | Média | Alto | Prompts versionados + revisão humana obrigatória antes de enviar |
| Deploy com DB migration falha | Baixa | Crítico | Flyway com lock + rollback plan em playbook |

---

## Referências

- **Feedback de layout:** `.claude/plans/layout-feedback.md`
- **Product spec:** `scopeflow_ai_documento_master_completo.md`
- **Playbook de deploy:** `~/.claude/playbooks/k8s-deploy-safe.md`
- **Playbook de incident:** `~/.claude/playbooks/incident-response.md`
- **Playbook de database migration:** `~/.claude/playbooks/database-migration.md`
