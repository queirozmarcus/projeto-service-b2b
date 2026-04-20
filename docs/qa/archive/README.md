# Arquivo Histórico — Documentação QA

Este diretório contém documentação histórica e detalhada que foi consolidada no `README.md` principal.

---

## Índice

### 📋 Documentação Técnica Detalhada

| Documento | Descrição | Linhas | Quando Consultar |
|-----------|-----------|--------|------------------|
| `contract-tests-detailed.md` | Guia completo de contract tests: arquitetura, troubleshooting avançado, JWT validation, Pact migration plan | 341 | Problemas complexos com WireMock/stubs, migração para Pact Broker |
| `test-coverage-detailed.md` | Mapa completo de cobertura: todas as 47 classes de teste com escopos e gaps | 321 | Planejamento de novos testes, análise de gaps por domínio |
| `qa-strategy-detailed.md` | Estratégia de QA: análise de risco, pirâmide de testes, plano de ação | 290 | Definir roadmap de testes, priorizar backlog |

### 📊 Relatórios de Sprint (Executados)

| Sprint | Arquivo | Foco | Data | Status |
|--------|---------|------|------|--------|
| **Sprint 5** | `sprint5-test-fix-report.md` | Correção de testes quebrados (auth, user) | 2026-04-19 | ✅ Concluído |
| **Sprint 6** | `sprint6-unit-tests-report.md` | 50 novos testes unitários (proposal, purge) | 2026-04-19 | ✅ Concluído |
| **Sprint 7** | `sprint7-contract-tests-report.md` | 8 contratos Auth+User (Spring Cloud Contract) | 2026-04-19 | ✅ Concluído |
| **Sprint 7** | `SPRINT7-SUMMARY.md` | Resumo executivo do Sprint 7 | 2026-04-19 | ✅ Concluído |
| **Sprint 8** | `sprint8-e2e-tests-report.md` | 2 smoke test classes (journey + OWASP) | 2026-04-19 | ✅ Concluído |
| **Sprint 9** | `sprint9-code-review-report.md` | Code review completo (11 findings) | 2026-04-19 | ✅ Concluído |
| **Sprint 9** | `sprint9-code-review-summary.txt` | Resumo executivo do code review | 2026-04-19 | ✅ Concluído |
| **Sprint 10** | `sprint10-final-audit-report.md` | Auditoria final: 426 testes, 0 falhas | 2026-04-19 | ✅ Concluído |
| **Retrospective** | `SPRINT-RETROSPECTIVE.md` | Retrospectiva das 10 sprints executadas | 2026-04-19 | ✅ Concluído |

### 🔍 Análises e Validações

| Arquivo | Descrição | Data |
|---------|-----------|------|
| `AUTH-TESTS-FIX-ANALYSIS.md` | Análise detalhada da correção de testes de auth | 2026-04-19 |
| `EMAIL-VALIDATION-SYNC.md` | Sincronização da validação de email entre serviços | 2026-04-19 |
| `qa-validation-report-20260419.md` | Relatório de validação QA completa | 2026-04-19 |
| `user-service-audit-report-20260419.md` | Auditoria completa do user-service | 2026-04-19 |

---

## Como Usar Este Arquivo

### Quando Consultar Documentos Detalhados

- **Problema específico não resolvido pelo README.md principal** → consulte o documento técnico detalhado correspondente
- **Contexto histórico de uma decisão** → consulte o relatório de sprint que implementou a feature
- **Troubleshooting avançado** → `contract-tests-detailed.md` tem seções de debugging profundas
- **Planejamento de novos testes** → `test-coverage-detailed.md` mostra todos os gaps

### Quando NÃO Consultar

- **Quick start** → use `../README.md` (seção "Quick Start")
- **Executar testes** → use `../README.md` (comandos já estão lá)
- **Quality gates** → use `../README.md` (seção "Quality Gates")
- **Padrões de teste** → use `../README.md` (seção "Padrões de Teste")

---

## Estrutura de Arquivamento

### Critérios para Arquivamento

Um documento é arquivado quando:
1. ✅ Conteúdo essencial foi consolidado no README.md principal
2. ✅ Informação histórica ou extremamente detalhada (>300 linhas)
3. ✅ Relevante para troubleshooting avançado mas não para uso diário
4. ✅ Relatório de sprint concluído (documentação de execução)

### Manutenção

- **Novos relatórios de sprint**: adicionar ao índice acima quando a sprint for concluída
- **Documentos técnicos**: arquivar apenas quando consolidação no README.md principal estiver completa
- **Não deletar**: documentos arquivados são referência histórica e troubleshooting

---

## Referências Rápidas

Para uso diário, veja sempre primeiro o **[README.md principal](../README.md)** que contém:
- Quick start (comandos prontos)
- Estado atual (métricas atualizadas)
- Quality gates (critérios de release)
- Padrões de teste (stack, naming, separação)
- Contract tests (arquitetura, executar, troubleshooting básico)
- Troubleshooting (problemas comuns)

Este arquivo serve como **índice navegável** para documentação de apoio e histórico.
