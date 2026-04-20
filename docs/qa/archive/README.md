# Arquivo Histórico — Documentação QA

Este diretório contém documentação histórica consolidada. Para uso diário, consulte `../README.md`.

---

## Arquivos Mantidos

### 📋 Guia Técnico Profundo

**`contract-tests-detailed.md`** (341 linhas)
- Arquitetura completa de contract tests
- Troubleshooting avançado de WireMock/stubs
- JWT validation patterns
- Migração para Pact Broker (quando necessário)

**Quando consultar:** Problemas complexos com contract tests, setup de novo consumer/provider

---

### 📊 Retrospectiva Consolidada

**`SPRINT-RETROSPECTIVE.md`** (504 linhas)
- Lessons learned de 10 sprints (Email VO Validation Sync)
- Métricas: 53 testes, 6h duração, 0% defect leakage
- Best practices: incremental approach, test-first mindset, RFC 9457
- Action items para Sprint 11+

**Quando consultar:** Entender decisões históricas, aplicar lessons learned em novos projetos

---

## Documentação Consolidada

Relatórios individuais de sprints foram **consolidados** em `SPRINT-RETROSPECTIVE.md`:
- Sprints 5-10 (test fixes, unit, contract, E2E, code review, final audit)
- Análises pontuais (AUTH, EMAIL-VALIDATION, user-service audit)
- Estratégia e coverage detalhados

Toda informação essencial está preservada na retrospectiva ou derivável do código atual.

---

## Referências Rápidas

**Uso diário:** `../README.md`
- Quick start (comandos prontos)
- Estado atual (126 testes, 0 failures)
- Quality gates & padrões

**Troubleshooting avançado:** `contract-tests-detailed.md`

**Contexto histórico:** `SPRINT-RETROSPECTIVE.md`
