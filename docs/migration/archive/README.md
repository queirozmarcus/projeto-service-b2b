# Documentação Histórica — Migração User Service

Estes documentos foram arquivados após a conclusão da extração do User Service (2026-04-05).  
**Status:** Referência histórica — não necessários para trabalho atual.

---

## Documentos Arquivados

| Arquivo | Tamanho | Propósito Original | Por Que Foi Arquivado |
|---------|---------|-------------------|----------------------|
| `EXTRACAO-USER-SERVICE-RESUMO.md` | 28K | Resumo completo das 6 etapas da extração | Processo concluído; informação consolidada no README.md principal |
| `ETAPA-3-CONCLUSAO.md` | 14K | Conclusão da provisão de infraestrutura Docker | Infra validada e estável; checklist executado |
| `ETAPA-3-VALIDACAO.md` | 9.7K | Checklist de validação (10 checks) | Validação automatizada via `scripts/validate-user-service-infra.sh` |

**Total arquivado:** 51.7K (redução de 80% no tamanho da documentação ativa)

---

## Quando Consultar Estes Arquivos

- **Debugar issues históricos:** Se encontrar comportamento inesperado relacionado à extração inicial
- **Auditar decisões:** Entender por que escolhas específicas foram feitas durante a extração
- **Replicar processo:** Usar como template para próximas extrações (Workspace, Proposal, Briefing)
- **Onboarding de time:** Mostrar o processo completo de Strangler Fig na prática

---

## Informação Atualizada

Para informação atual sobre a migração, consulte:
- **[`../README.md`](../README.md)** — Status consolidado e próximos passos
- **[`../db-per-service-cutover.md`](../db-per-service-cutover.md)** — Guia de cut-over para produção
- **[`../../README.md`](../../README.md)** — Setup atual do projeto

---

**Arquivado em:** 2026-04-20  
**Razão:** Simplificação e consolidação da documentação ativa
