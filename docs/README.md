# ScopeFlow AI — Documentação

**Status:** User Service extraído ✅ | 126 testes passando | 7/7 serviços operacionais

---

## 🎯 Quick Start

| Preciso... | Ver |
|-----------|-----|
| **Entender o projeto** | [CLAUDE.md](../CLAUDE.md) — Architecture, conventions, troubleshooting |
| **Rodar localmente** | [README.md](../README.md) — Setup instructions |
| **API REST** | [api/README.md](api/README.md) — Briefing API guide |
| **Escrever testes** | [qa/README.md](qa/README.md) — Testing guide |
| **Deploy** | [deployment/README.md](deployment/README.md) — Deployment procedures |
| **Arquitetura** | [architecture/README.md](architecture/README.md) — Architecture docs |
| **Banco de dados** | [database/README.md](database/README.md) — Database guide |
| **Migração** | [migration/README.md](migration/README.md) — Migration strategy |
| **Scripts** | [devops/README.md](devops/README.md) — DevOps scripts |
| **Frontend** | [frontend/README.md](frontend/README.md) — Frontend docs |

---

## Estrutura

```
docs/
├── README.md                    # Este arquivo — índice central
│
├── api/                         # API Documentation
│   ├── README.md                # Briefing API quick reference
│   └── briefing-api.yaml        # OpenAPI 3.1 spec
│
├── architecture/                # Architectural Documentation
│   ├── README.md                # Overview + navigation
│   ├── architectural-overview.md
│   ├── bounded-contexts.md
│   ├── coupling-matrix.md
│   ├── dependency-matrix.md
│   ├── data-ownership.md
│   ├── schema-inventory.md
│   ├── security-model.md
│   └── adr/                     # Architecture Decision Records
│
├── database/                    # Database Documentation
│   ├── README.md                # Schema, migrations, performance
│   └── archive/                 # Historical migration details
│
├── deployment/                  # Deployment Guides
│   └── README.md                # Local, Staging, Production
│
├── devops/                      # DevOps & Operations
│   └── README.md                # Scripts and monitoring
│
├── frontend/                    # Frontend Documentation
│   ├── README.md                # Next.js app guide
│   └── archive/                 # Implementation history
│
├── migration/                   # Migration Strategy
│   ├── README.md                # Strangler Fig overview
│   ├── db-per-service-cutover.md
│   ├── adr/                     # Migration ADRs
│   ├── extraction-cards/        # Roadmap
│   └── archive/                 # Historical docs
│
└── qa/                          # Quality Assurance
    └── README.md                # Testing guide
```

---

## 📊 Status do Projeto

| Componente | Status | Notas |
|-----------|--------|-------|
| **Backend (Monólito)** | ✅ Operacional | 50 testes passando |
| **User Service** | ✅ Staging ativo | 76 testes, 8 contratos |
| **Frontend** | ✅ Pronto | Next.js 15 + Tailwind |
| **Database** | ✅ V9 aplicado | PostgreSQL 16 |
| **Docker Stack** | ✅ 7/7 healthy | Traefik routing ativo |
| **QA Scripts** | ✅ Automatizado | validate-qa-full.sh |

---

## 🔍 Por Onde Começar?

### Novo no Projeto
1. [CLAUDE.md](../CLAUDE.md) — contexto completo
2. [README.md](../README.md) — setup e quick start
3. [architecture/README.md](architecture/README.md) — arquitetura

### Desenvolvedor Backend
- [CLAUDE.md](../CLAUDE.md) — conventions
- [database/README.md](database/README.md) — schema e migrations
- [qa/README.md](qa/README.md) — como escrever testes

### Desenvolvedor Frontend
- [frontend/README.md](frontend/README.md) — componentes
- [api/README.md](api/README.md) — endpoints

### DevOps/SRE
- [deployment/README.md](deployment/README.md) — deploy procedures
- [devops/README.md](devops/README.md) — scripts operacionais
- [migration/README.md](migration/README.md) — estratégia de migração

---

## 🤝 Contribuindo com Documentação

| Mudança | Atualizar |
|---------|-----------|
| Novo endpoint REST | `api/README.md` + `api/briefing-api.yaml` |
| Nova migration | `database/README.md` |
| Mudança arquitetural | `architecture/*` + criar ADR se necessário |
| Novo bounded context | `architecture/bounded-contexts.md` + `coupling-matrix.md` |
| Mudança em auth/security | `architecture/security-model.md` |
| Novo procedimento deploy | `deployment/README.md` |
| Novo script operacional | `devops/README.md` |

**Princípios:**
- ✅ Mantenha conciso — links para detalhes, não duplique
- ✅ Quick reference primeiro — exemplos práticos antes de teoria
- ✅ Archive o que passou — histórico em `archive/`, não delete

---

## 📞 Suporte

- **Dúvidas técnicas:** [CLAUDE.md](../CLAUDE.md)
- **Troubleshooting:** [CLAUDE.md](../CLAUDE.md) § Troubleshooting
- **Scripts:** [devops/README.md](devops/README.md) + `scripts/README.md`

---

**Mantido por:** Equipe ScopeFlow
