# Migração Monólito → Microsserviços — ScopeFlow AI

**Padrão:** Strangler Fig  
**Status:** User Service ✅ Extraído (staging) | Próximo: Workspace

---

## Status Atual

| Contexto | Status | DB Strategy | Risco | Bloqueadores |
|----------|--------|-------------|-------|--------------|
| **User (Auth)** | ✅ **Staging ativo** | DB compartilhado | Baixo | Cut-over produção pendente |
| **Workspace** | 🔄 Próximo | — | Médio | User-service estável em prod |
| **Proposal** | ⏳ Backlog | — | Médio | Workspace extraído |
| **Briefing** | ⏳ Backlog | — | Alto | Proposal extraído + circuit breaker OpenAI |

**Ambiente:**
- ✅ 7/7 serviços operacionais (Docker Compose)
- ✅ 126 testes passando (50 backend + 76 user-service)
- ✅ Traefik routing ativo (`/api/v1/auth/*` → user-service)
- ✅ Scripts de validação QA implementados

---

## Decisões Arquiteturais (ADRs)

| ADR | Decisão | Impacto |
|-----|---------|---------|
| [ADR-001](adr/ADR-001-ordem-de-extracao-bounded-contexts.md) | Ordem: User → Workspace → Proposal → Briefing | Define prioridade por acoplamento |
| [ADR-002](adr/ADR-002-database-strategy-shared-inicial.md) | DB compartilhado → split em staging | Validação de infra sem migração de dados |
| [ADR-003](adr/ADR-003-comunicacao-entre-servicos.md) | REST síncrono + circuit breaker | `UserServiceRestAdapter` com Resilience4j |
| [ADR-004](adr/ADR-004-ownership-service-context.md) | Ownership de `service_context_*` tables | Bloqueador do card 04 (Briefing) |

---

## Próximas Extrações

### Card 02 — Workspace (Próximo)

**Pré-requisitos:**
- User-service estável em produção (~48h pós-cut-over)
- OpenTelemetry configurado (trace ID cross-service)

**Blockers conhecidos:**
- `inviteMember()` refatorado para REST (✅ endpoints já implementados)
- Definir contrato `UserServiceClient`

**Comando:**
```bash
claude --agent marcus
> retomada Migração — ScopeFlow AI, Workspace (card 02)
```

### Card 03 — Proposal

**Depende de:** Workspace extraído  
**Risco:** FK `proposals.briefing_id` → migrar para evento `BriefingCompletedEvent`

### Card 04 — Briefing

**Depende de:** Proposal extraído  
**Bloqueadores:**
- ADR-004: ownership de `service_context_profiles` / `service_context_questions`
- Circuit breaker OpenAI (Phase 4 — adapter não existe)

---

## Comandos Úteis

### Validação Completa

```bash
# Validação QA automatizada (~4 min)
./scripts/validate-qa-full.sh

# Com stack Docker (debug)
./scripts/validate-qa-full.sh --with-stack

# Health check rápido da stack (~2s)
./scripts/check-stack-health.sh
```

### User Service (Staging)

```bash
# Build e start
docker compose build user-service
docker compose up -d user-service

# Health checks
curl http://localhost:8081/actuator/health/liveness   # Direto
curl http://localhost/api/v1/auth/health              # Via Traefik

# Logs
docker logs -f scopeflow-user-service

# Verificar migration aplicada
docker exec scopeflow-user-db psql -U postgres -d scopeflow_users \
  -c "SELECT * FROM flyway_schema_history;"
```

### Rollback (Se Necessário)

**Cenário 1: Rollback de routing (Traefik)**
```bash
# Desabilitar user-service no Traefik (remover labels)
docker compose stop user-service
# Tráfego volta automaticamente para o monólito
```

**Cenário 2: Rollback de banco (pré-cut-over)**
```bash
# User-service volta para DB compartilhado
# Editar docker-compose.yml:
USER_SERVICE_DATABASE_URL=jdbc:postgresql://postgres:5432/scopeflow
docker compose restart user-service
```

---

## Cut-Over Produção (Próximo Milestone)

### Pré-requisitos

- [ ] User-service estável em staging por 7+ dias (zero erros 5xx)
- [ ] Performance baseline medida (latência p99 < +20ms com network hop)
- [ ] Banco `scopeflow_users` provisionado em produção
- [ ] Backup verificado do banco `scopeflow`
- [ ] Janela de manutenção agendada (~5-15 min)

### Guia de Execução

Ver **[db-per-service-cutover.md](db-per-service-cutover.md)** para:
- Passos de migração de dados (pg_dump → psql)
- Validação de integridade (contagens + checksum)
- Estratégia de rollback
- Queries de diagnóstico

**Estimativa de downtime:** 2–10 minutos (depende do volume de `users`)

---

## Estratégia de Split Futura

### Quando Split para 4 Microsserviços?

Considere quando **2+ critérios** forem verdadeiros:

1. **Escala:** 10K+ workspaces ativos OU 100K+ briefing sessions
2. **Performance:** Contenção de recursos entre contextos
3. **Equipe:** Time cresce para 8+ devs (1 squad por serviço)
4. **Deploy:** Necessidade de deploy independente
5. **Resiliência:** Failure de um contexto não pode derrubar outro

**Status atual:** Monólito modular apropriado até ~5K workspaces (~2-3 anos).

### Ordem Sugerida de Split

```
1. Briefing Service  (alto volume de writes, mais isolado)
2. Proposal Service  (remove FK briefing_id → evento)
3. User + Workspace  (juntos ou separados, low-churn)
```

---

## Análise de Acoplamento

Ver **[`../architecture/coupling-matrix.md`](../architecture/coupling-matrix.md)** para:
- Matriz de dependências entre os 4 bounded contexts
- Score de acoplamento (chamadas diretas, dados compartilhados, eventos)
- Zonas de ambiguidade e recomendações de desacoplamento

Ver **[`../architecture/data-ownership.md`](../architecture/data-ownership.md)** para:
- Mapeamento de ownership por tabela
- Foreign keys cross-context (3 FKs justificadas)
- Conformidade: 100% (zero violações detectadas)

---

## Arquivos de Referência

| Arquivo | Propósito |
|---------|-----------|
| `db-per-service-cutover.md` | Guia de migração de banco compartilhado → DB-per-service |
| `adr/*.md` | Decisões arquiteturais (4 ADRs aplicados) |
| `extraction-cards/*.md` | Roadmap de extração (cards 01-04) |
| `archive/*.md` | Documentação histórica (etapas concluídas) |
| `../architecture/coupling-matrix.md` | Matriz de dependências entre contextos |
| `../architecture/data-ownership.md` | Ownership de tabelas (17 tabelas inventariadas) |
| `../architecture/schema-inventory.md` | Schema PostgreSQL completo |

---

## Documentação Arquivada

Documentos históricos movidos para `archive/`:

- `EXTRACAO-USER-SERVICE-RESUMO.md` — Resumo detalhado da extração (6 etapas, 28K)
- `ETAPA-3-CONCLUSAO.md` — Conclusão da provisão de infra Docker (14K)
- `ETAPA-3-VALIDACAO.md` — Checklist de validação executado (9.7K)

Mantidos para referência histórica, mas não são necessários para trabalho atual.

---

**Última atualização:** 2026-04-20  
**Próxima ação:** Cut-over produção (user-service) → ver plano em `.claude/plans/backlog/`
