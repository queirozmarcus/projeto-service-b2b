# Documentação Arquitetural — ScopeFlow AI

**Data:** 2026-04-17 (atualizado)
**Status:** Atualizado — User Service extraído, circuit breakers e purge jobs implementados, Sprint 10 ✅

Este diretório contém o mapeamento completo da arquitetura do ScopeFlow AI.

---

## 📚 Documentos Principais

| Documento | Descrição | Tamanho |
|-----------|-----------|---------|
| **[architectural-overview.md](architectural-overview.md)** | Visão geral completa: C4 model, bounded contexts, dependencies, tech stack | 12 KB |
| **[bounded-contexts.md](bounded-contexts.md)** | Detalhamento de cada bounded context: aggregates, entities, value objects, events, ports | 268 linhas |
| **[coupling-matrix.md](coupling-matrix.md)** | Matriz de acoplamento entre contextos: dependências, side effects, estratégia de desacoplamento | 9 KB |
| **[dependency-matrix.md](dependency-matrix.md)** | Matriz de dependências detalhada: sync, async, data | 453 linhas |
| **[data-ownership.md](data-ownership.md)** | Ownership de dados por bounded context: tabelas, FKs, conformidade, estratégia de split | 19 KB |
| **[schema-inventory.md](schema-inventory.md)** | Inventário completo do schema: 17 tabelas, colunas, índices, FKs, triggers, views | 31 KB |
| **[security-model.md](security-model.md)** | Modelo de segurança: auth, authz, multi-tenancy, PII, LGPD, rate limiting | 15 KB |

---

## 🎯 Quick Start

### Entenda a Arquitetura em 5 Minutos

1. **[architectural-overview.md](architectural-overview.md)** — comece aqui
2. **[coupling-matrix.md](coupling-matrix.md)** — entenda as dependências
3. **[security-model.md](security-model.md)** — veja autenticação e multi-tenancy

### Deep Dive por Domínio

| Interesse | Documentos |
|-----------|-----------|
| **Domain modeling** | [bounded-contexts.md](bounded-contexts.md) |
| **Database schema** | [schema-inventory.md](schema-inventory.md) + [data-ownership.md](data-ownership.md) |
| **Security & compliance** | [security-model.md](security-model.md) |
| **Dependencies & coupling** | [coupling-matrix.md](coupling-matrix.md) + [dependency-matrix.md](dependency-matrix.md) |

---

## 📊 Números da Análise

### Bounded Contexts
- **Total:** 4 (Briefing, Proposal, Workspace, User)
- **Aggregates:** 5 (BriefingSession, Proposal, Workspace, WorkspaceMember, User)
- **Domain events:** 9 (3 com listeners ativos)
- **Value objects:** 20+

### Database
- **Tabelas:** 17 (15 domínio + 2 infra)
- **FKs cross-context:** 3 (todas justificadas)
- **Conformidade de ownership:** 100%
- **Queries cross-context:** 0 (zero violações)

### Security
- **Auth:** JWT dual-token (access 15min + refresh 7 dias)
- **Authz:** RBAC (OWNER/ADMIN/MEMBER)
- **Multi-tenancy:** 100% conforme (workspace-scoped)
- **PII:** 7 campos críticos
- **LGPD:** 60% conforme

### Coupling
- **Dependências totais:** 4 (todas via `data`)
- **Chamadas síncronas:** 0 (zero diretas entre contextos)
- **Comunicação assíncrona:** 3 domain events
- **Acoplamento geral:** BAIXO ✅

---

## 🚀 Roadmap Arquitetural

### Curto Prazo (0-3 meses)
1. ✅ ~~Circuit breaker Resilience4j em SES~~ — implementado
2. ✅ ~~Purge jobs (outbox, idempotency, ai_generations)~~ — implementado
3. Validação de RI em `Proposal.create()`
4. Circuit breaker OpenAI + S3 (Phase 4 — aguarda adapters)
5. Row-Level Security (RLS) PostgreSQL
6. Testes de isolamento multi-tenancy

### Médio Prazo (3-6 meses)
1. Cache Redis (User, Workspace)
2. Read replicas PostgreSQL
3. OpenTelemetry + Jaeger (tracing)
4. CAPTCHA reCAPTCHA v3
5. LGPG completo (consentimento + portabilidade)

### Longo Prazo (6-12 meses)
1. Migração para microsserviços (se 10K+ workspaces)
2. Event Sourcing para Proposal
3. CQRS para queries pesadas
4. Multi-region deployment

---

## 🔍 Pontos de Atenção Identificados

### Resolvidos ✅

1. ✅ **Circuit breakers implementados** — `user-service` (CB+Retry) e `ses` (CB) via Resilience4j
2. ✅ **Purge jobs implementados** — outbox (7d), idempotency (30d), ai_generations (90d) — `PurgeJobService`
3. ✅ **User Service extraído** — Strangler Fig completo, staging ativo com DB-per-service
4. ✅ **GlobalExceptionHandler completo** — RFC 9457 + ServiceUnavailable + CircuitBreaker open handler

### Alta Prioridade ⚠️

1. **Validação de RI ausente** — `Proposal.create()` não valida briefing exists + completed
2. **Workspace creation não atômica** — não verificado se `Workspace + owner member` usa `@Transactional`
3. **Circuit breaker faltando** — OpenAI e S3 sem proteção (adapters não existem — Phase 4)

### Média Prioridade ⚠️

4. **Rate limiting in-memory** — Bucket4j não compartilha estado entre pods
5. **PostgreSQL encryption at rest** — PII não encriptado em disco
6. **CAPTCHA faltando** — Endpoints públicos vulneráveis a bots

### Baixa Prioridade ℹ️

7. **Tracing faltando** — OpenTelemetry não implementado
8. **LGPD 60%** — Consentimento + portabilidade + direito ao esquecimento faltando
9. **Log masking PII** — `briefing_answers.answer_text` pode vazar PII em logs

---

## 📁 Estrutura de Diretórios

```
docs/
├── architecture/                    # Este diretório
│   ├── README.md                    # Este arquivo
│   ├── architectural-overview.md    # Visão geral completa
│   ├── bounded-contexts.md          # Domain model detalhado
│   ├── coupling-matrix.md           # Matriz de acoplamento
│   ├── dependency-matrix.md         # Matriz de dependências
│   ├── data-ownership.md            # Ownership de dados
│   ├── schema-inventory.md          # Inventário de schema
│   └── security-model.md            # Modelo de segurança
│
├── migration/                       # Estratégia de migração (se necessário)
│   └── README.md                    # Guia de migração para microsserviços
│
└── api/                             # Documentação de API (OpenAPI)
    ├── BRIEFING-API-GUIDE.md
    └── briefing-api.yaml
```

---

## 🤝 Como Contribuir

### Atualizar Documentação

Quando modificar a arquitetura:

1. **Adicionar bounded context:** Atualizar `bounded-contexts.md` + `coupling-matrix.md`
2. **Adicionar tabela:** Atualizar `schema-inventory.md` + `data-ownership.md`
3. **Modificar auth:** Atualizar `security-model.md`
4. **Adicionar integração externa:** Atualizar `architectural-overview.md` (seção "Integração Externa")

### Revisar Anualmente

- **Trim 1:** Validar roadmap (curto/médio prazo concluído?)
- **Trim 4:** Atualizar números (tabelas, eventos, aggregates) + pontos de atenção

---

## 📞 Contato

Para dúvidas sobre a arquitetura, consulte:
- **Tech Lead:** [TBD]
- **Architect:** [TBD]
- **DBA:** [TBD]
- **Security Lead:** [TBD]

---

## 📝 Changelog

| Data | Versão | Mudança |
|------|--------|---------|
| 2026-04-05 | 1.0 | Análise inicial completa: 4 contextos, 17 tabelas, documentação gerada |
