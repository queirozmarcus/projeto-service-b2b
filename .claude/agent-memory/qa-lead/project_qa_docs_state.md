---
name: docs/qa consolidado (2026-04-19)
description: Estado final da pasta docs/qa após revisão cirúrgica — quais docs existem e por que
type: project
---

Revisão cirúrgica executada em 2026-04-19. Pasta reduzida de 9 para 3 documentos.

**Documentos mantidos/criados:**
- `README.md` — índice criado (novo)
- `test-coverage-report.md` — mapa real de cobertura, 100% fiel ao código
- `strategies/qa-strategy-scopeflow.md` — estratégia consolidada, pirâmide, quality gates
- `contract-tests.md` — ex-CONTRACT-TESTING-SUMMARY.md renomeado + diagrama de arquitetura absorvido do GUIDE

**Deletados (6 arquivos):**
- `CONTRACT-TESTS-MONOLITH.md` — subconjunto de contract-tests.md
- `CONTRACT-TESTS-USER-SERVICE.md` — subconjunto de contract-tests.md
- `contracts/README.md` — subconjunto de contract-tests.md
- `contracts/CONTRACT-TESTING-GUIDE.md` — conteúdo único (diagrama) absorvido em contract-tests.md
- `smoke-tests.md` — conteúdo coberto pelo test-coverage-report.md
- `user-controller-integration-tests.md` — doc de uma única classe, subconjunto do coverage report

**Why:** docs/qa tinha fragmentação severa — 4 arquivos sobre contract testing cobrindo o mesmo assunto. Consolidação mantém rastreabilidade sem redundância.

**How to apply:** Ao atualizar docs de QA, o ponto de entrada é test-coverage-report.md (cobertura) e qa-strategy-scopeflow.md (estratégia). Não criar docs separados por classe de teste ou por sprint — incorporar no report.
