# DevOps — Scripts e Operações

Documentação consolidada de scripts operacionais e ferramentas de monitoramento do ScopeFlow AI.

---

## Scripts Disponíveis

### 1. Monitor & Auto-Healing

**Localização:** `scripts/monitor-and-heal.sh`

**Propósito:** Executa smoke tests E2E e captura logs automaticamente em caso de falha.

**Uso:**
```bash
./scripts/monitor-and-heal.sh
```

**Comportamento:**
- ✅ Valida pré-requisitos (Docker, espaço em disco, lockfile)
- 🧪 Executa `tests/e2e/auth-flow.test.sh`
- 📊 Captura logs em background (user-service, monolith, postgresql)
- 🔄 Retry automático (1 tentativa com 5s delay) em caso de falha
- 📋 Gera incident report consolidado em `logs/incident-{timestamp}.log`

**Output em caso de sucesso:**
```
✅ Tests passed! System is healthy.
📊 0 incidents detected.
```

**Output em caso de falha:**
```
❌ Incident report saved: logs/incident-1775438892.log

📋 To diagnose this incident, execute:
   claude --agent marcus
   > diagnosticar incidente logs/incident-1775438892.log
```

**Incident Report contém:**
- Timestamp e exit code do teste
- Status de todos os containers (`docker compose ps`)
- Últimas 100 linhas de logs de 3 containers críticos
- Instruções de diagnóstico

---

## Integração com CI/CD

### GitHub Actions (exemplo)
```yaml
- name: Run smoke tests with monitoring
  run: ./scripts/monitor-and-heal.sh
  continue-on-error: false
```

---

## Troubleshooting

| Problema | Diagnóstico | Solução |
|----------|-------------|---------|
| Script trava no "Aguardando..." | Lockfile stale | Remover `.monitor-and-heal.lock` manualmente |
| "Docker Compose is not available" | Docker não está rodando | `docker info` para verificar |
| "Insufficient disk space" | < 100MB disponível | Limpar logs antigos: `rm -rf logs/incident-*` |
| Teste falha logo após `docker compose up` | Monolith ainda inicializando | Aguardar healthcheck: `docker compose ps` |

---

## Logs e Artefatos

| Local | Propósito | Retenção |
|-------|-----------|---------|
| `logs/incident-*.log` | Incident reports consolidados | Manual (limpar quando necessário) |
| `.monitor-and-heal.lock` | Lockfile para evitar execuções concorrentes | Auto-removido ao final do script |

---

## Próximos Passos

- [ ] Adicionar `--wait-for-healthy` flag no script
- [ ] Criar runbook de diagnóstico de incidentes
- [ ] Validar em ambiente staging
- [ ] Integrar com `sre-engineer` agent para análise automatizada

---

**See Also:**
- [Deployment Guide](../deployment/README.md) — Kubernetes deployment procedures
- [QA Guide](../qa/README.md) — Testing and validation scripts
- [CLAUDE.md](../../CLAUDE.md) — Development guidelines
