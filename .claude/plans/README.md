# ScopeFlow AI — Índice de Planos

**Última atualização:** 2026-04-13
**Branch ativa:** `develop`
**Status geral:** User Service extraído e ativo em staging ✅

---

## Estrutura

```
plans/
├── README.md          # Este arquivo
├── concluido/         # Planos e artefatos de execução concluídos
│   ├── adr/           # Architecture Decision Records
│   └── *.md           # Planos, sprints, outputs de agentes
└── backlog/           # Próximos planos aprovados aguardando execução
```

---

## Backlog — Próximos Ciclos

| Arquivo | Descrição | Prioridade |
|---------|-----------|-----------|
| `backlog/2026-04-12-migration-fase3-cutover-producao.md` | Cut-over user-service para banco dedicado em produção | Alta — após staging estável 48h+ |

**Itens técnicos sem plano formal ainda:**
- `BriefingControllerV1IntegrationTest` — corrigir compile error (`.questionId()` inexistente)
- Circuit breaker OpenAI / S3 — Phase 4 (adapters não existem ainda, TODOs em `ITextPdfServiceAdapter`)
- Extração Workspace context — próximo bounded context após user-service estável em prod

---

## Concluído

### Planejamento Inicial (2026-03)
| Arquivo | Descrição |
|---------|-----------|
| `concluido/000-executive-summary.md` | Resumo executivo MVP |
| `concluido/001-fine-planning-mvp.md` | Arquitetura 360°, roadmap, padrões |
| `concluido/002-prompts-and-api-contracts.md` | Estratégia IA + 22 contratos de API |
| `concluido/003-architectural-decisions-and-services.md` | 8 ADRs + serviços MVP |
| `concluido/004-validation-and-diagrams.md` | Fluxos, checklists, risk matrix |
| `concluido/005-spring-boot-java21-migration.md` | Stack Java 21 + Spring Boot 3.2 |
| `concluido/bootstrap-execution-plan.md` | Plano de execução inicial |
| `concluido/adr/ADR-001-user-workspace-service.md` | ADR: boundaries User + Workspace service |
| `concluido/ADR-001-HEXAGONAL-ARCHITECTURE-DEBT.md` | ADR: dívida técnica hexagonal |

### Implementação Backend (2026-03)
| Arquivo | Descrição |
|---------|-----------|
| `concluido/sprint-1-domain-layer-full.md` | Domain layer — entidades, value objects, regras |
| `concluido/sprint-2-adapter-layer-full.md` | Adapter layer — JPA, REST, mappers |
| `concluido/sprint-4-phase-2-status.md` | Phase 2 — use cases e application services |
| `concluido/sprint-5-final-status.md` | Sprint 5 — 100% concluído |
| `concluido/ARCHITECT-OUTPUT-Step1.md` | Output architect — design inicial |
| `concluido/ARCHITECT-OUTPUT-Step1-Briefing.md` | Output architect — módulo briefing |
| `concluido/BACKEND-DEV-OUTPUT-Step2.md` | Output backend-dev — implementação |
| `concluido/DBA-OUTPUT-Step3.md` | Output dba — schema e migrations |
| `concluido/API-DESIGNER-OUTPUT-Step4.md` | Output api-designer — OpenAPI spec |

### Frontend (2026-03)
| Arquivo | Descrição |
|---------|-----------|
| `concluido/sprint-5-frontend-auth.md` | Auth frontend — login, register, JWT, refresh |
| `concluido/FRONTEND-LANDING-DASHBOARD-REDESIGN.md` | Redesign landing + dashboard |
| `concluido/layout-feedback.md` | Feedback de layout incorporado |
| `concluido/deployment-guide.md` | Guia de deploy staging |

### Sessões Multi-terminal (2026-03)
| Arquivo | Descrição |
|---------|-----------|
| `concluido/TERMINAL2-*.md` (8 arquivos) | Handoffs e progresso das sessões paralelas |
| `concluido/SPRINT6-TASK3-BRIEFINGSESSION.md` | Sprint 6 task 3 — BriefingSession |
| `concluido/STEP5-PART2-REST-IMPLEMENTATION-STATUS.md` | Step 5 part 2 — REST status |

### Strangler Fig — User Service (2026-04)
| Arquivo | Descrição |
|---------|-----------|
| `concluido/USER-SERVICE-EXTRACTION-20-SPRINTS.md` | Plano completo 20 sprints — **20/20 ✅** |
| `concluido/2026-03-29-scopeflow-10-sprints.md` | Plano 10 sprints MVP (Sprint 1 done, supersedido) |
| `concluido/2026-04-04-sprint9-ecosystem-validation.md` | Sprint 9 — validação ecossistema Marcus no ScopeFlow |
