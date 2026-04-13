# ScopeFlow AI — Documentação

Índice central de toda a documentação técnica do projeto.
Para setup e comandos, ver [`README.md`](../README.md) na raiz. Para status atual, ver [`PROJECT-STATUS.md`](PROJECT-STATUS.md).

---

## Navegação Rápida

| Preciso de... | Vá para |
|--------------|---------|
| Estado atual do projeto | [`PROJECT-STATUS.md`](PROJECT-STATUS.md) |
| Setup e comandos | [`../README.md`](../README.md) |
| Visão de produto | [`scopeflow_ai_documento_master_completo.md`](scopeflow_ai_documento_master_completo.md) |
| API do Briefing | [`api/BRIEFING-API-GUIDE.md`](api/BRIEFING-API-GUIDE.md) |
| Arquitetura + ADRs | [`architecture/README.md`](architecture/README.md) |
| Migração Strangler Fig | [`migration/README.md`](migration/README.md) |
| Schema do banco | [`database/schema-diagram.md`](database/schema-diagram.md) |
| Deploy em staging | [`deployment/DEPLOYMENT-GUIDE.md`](deployment/DEPLOYMENT-GUIDE.md) |
| Cut-over produção | [`../user-service/docs/DB_MIGRATION_GUIDE.md`](../user-service/docs/DB_MIGRATION_GUIDE.md) |
| Testes de contrato | [`qa/contracts/README.md`](qa/contracts/README.md) |
| Cobertura de testes | [`qa/test-coverage-report.md`](qa/test-coverage-report.md) |

---

## Estrutura

```
docs/
├── README.md                                  # Este arquivo — índice central
├── PROJECT-STATUS.md                          # Status atual do projeto
├── scopeflow_ai_documento_master_completo.md  # Produto: spec, personas, roadmap
│
├── api/
│   └── BRIEFING-API-GUIDE.md                 # API do Briefing com exemplos
│
├── architecture/
│   ├── README.md                              # Índice de arquitetura
│   ├── architectural-overview.md             # Visão geral hexagonal + DDD
│   ├── bounded-contexts.md                   # Mapa de bounded contexts
│   ├── coupling-matrix.md                    # Matriz de acoplamento entre contextos
│   ├── data-ownership.md                     # Ownership de dados por contexto
│   ├── dependency-matrix.md                  # Dependências entre módulos
│   ├── schema-inventory.md                   # Inventário de tabelas por contexto
│   ├── security-model.md                     # Modelo de segurança e auth
│   └── adr/                                  # Architecture Decision Records
│       ├── ADR-002 — Briefing domain design
│       ├── ADR-003 — Sealed classes + JPA separation
│       ├── ADR-004 — Records for DTOs
│       ├── ADR-005 — Lazy loading default
│       ├── ADR-006 — RFC 9457 Problem Details
│       ├── ADR-007 — Landing + Dashboard architecture
│       ├── ADR-008 — User service extraction complete
│       └── ADR-009 — User module decommission
│
├── database/
│   ├── schema-diagram.md                     # Diagrama do schema
│   ├── flyway-changelog.md                   # Histórico de migrations
│   ├── index-strategy.md                     # Estratégia de índices
│   └── query-performance-baseline.md         # Baseline de performance de queries
│
├── deployment/
│   └── DEPLOYMENT-GUIDE.md                   # Guia de deploy staging
│
├── devops/
│   ├── MONITOR-AND-HEAL-VALIDATION.md        # Validação monitor-and-heal script
│   └── SEO_AUDIT_REPORT.md                   # Relatório de SEO (landing + dashboard)
│
├── migration/
│   ├── README.md                              # Status da migração Strangler Fig
│   ├── ETAPA-3-CONCLUSAO.md                  # Etapa 3 — conclusão
│   ├── ETAPA-3-VALIDACAO.md                  # Etapa 3 — validação
│   ├── EXTRACAO-USER-SERVICE-RESUMO.md       # Resumo da extração user-service
│   ├── adr/                                  # ADRs específicos de migração
│   │   ├── ADR-001 — Ordem de extração
│   │   ├── ADR-002 — Database strategy (shared inicial)
│   │   ├── ADR-003 — Comunicação entre serviços
│   │   └── ADR-004 — Ownership service context
│   └── extraction-cards/
│       ├── 01-user-auth.md                   # ✅ Extraído
│       ├── 02-workspace.md                   # 🔄 Próximo
│       ├── 03-proposal.md                    # ⏳ Backlog
│       └── 04-briefing.md                    # ⏳ Backlog
│
├── qa/
│   ├── CONTRACT-TESTING-SUMMARY.md           # Resumo dos contract tests
│   ├── CONTRACT-TESTS-MONOLITH.md            # Contract tests do monólito (consumer)
│   ├── test-coverage-report.md               # Relatório de cobertura
│   └── contracts/
│       ├── README.md                          # Índice de contratos
│       └── CONTRACT-TESTING-GUIDE.md         # Guia de contract testing
│
├── testing/
│   ├── smoke-tests.md                        # Smoke tests documentados
│   └── user-controller-integration-tests.md  # Integration tests do UserController
│
└── sessions/
    └── archive/                              # Histórico de sessões e artefatos antigos
```

---

## ADRs — Decisões Arquiteturais

### Arquitetura do Sistema

| ADR | Decisão | Status |
|-----|---------|--------|
| [ADR-002](architecture/adr/ADR-002-briefing-domain.md) | Briefing domain design | ✅ Ativo |
| [ADR-003](architecture/adr/ADR-003-sealed-domain-separate-jpa.md) | Sealed classes + JPA separation | ✅ Ativo |
| [ADR-004](architecture/adr/ADR-004-records-for-dtos.md) | Records para DTOs | ✅ Ativo |
| [ADR-005](architecture/adr/ADR-005-lazy-loading-default.md) | Lazy loading como default | ✅ Ativo |
| [ADR-006](architecture/adr/ADR-006-rfc9457-problem-details.md) | RFC 9457 Problem Details | ✅ Ativo |
| [ADR-007](architecture/adr/ADR-007-landing-dashboard-architecture.md) | Landing + Dashboard architecture | ✅ Ativo |
| [ADR-008](architecture/adr/ADR-008-user-service-extraction-complete.md) | User service extraction complete | ✅ Ativo |
| [ADR-009](architecture/adr/ADR-009-user-module-decommission.md) | User module decommission | ✅ Ativo |

### Migração (Strangler Fig)

| ADR | Decisão | Status |
|-----|---------|--------|
| [ADR-001](migration/adr/ADR-001-ordem-de-extracao-bounded-contexts.md) | Ordem de extração dos bounded contexts | ✅ Ativo |
| [ADR-002](migration/adr/ADR-002-database-strategy-shared-inicial.md) | Database strategy — shared inicial → DB-per-service | ✅ Ativo |
| [ADR-003](migration/adr/ADR-003-comunicacao-entre-servicos.md) | Comunicação entre serviços | ✅ Ativo |
| [ADR-004](migration/adr/ADR-004-ownership-service-context.md) | Ownership de service context | ✅ Ativo |
