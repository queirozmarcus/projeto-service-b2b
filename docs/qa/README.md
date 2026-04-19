# docs/qa — Documentação de Qualidade

**Atualizado em:** 2026-04-19

## Documentos

| Documento | Propósito |
|-----------|-----------|
| [`test-coverage-report.md`](test-coverage-report.md) | Mapa real de cobertura: todas as classes de teste por domínio, gaps críticos, quality gates e comandos de execução |
| [`strategies/qa-strategy-scopeflow.md`](strategies/qa-strategy-scopeflow.md) | Estratégia consolidada: pirâmide de testes, análise de risco, padrões obrigatórios, plano de ação e métricas |
| [`contract-tests.md`](contract-tests.md) | Contract tests (Spring Cloud Contract): arquitetura, 8 contratos YAML implementados, consumer/provider, CI/CD, troubleshooting |

## Estado atual (Sprint 10)

- Backend: 47 classes de teste (unit + integração + smoke)
- User Service: 7 classes de teste
- Contract tests: 8 contratos / 10 cenários consumer (Auth + User)
- Gap crítico: domínio `client` sem nenhum teste

## Como executar

```bash
# Unitários (rápido, sem Docker)
cd backend && ./mvnw test

# Integração completa (Testcontainers)
cd backend && ./mvnw verify

# User service
cd user-service && ./mvnw verify

# Contract tests
./scripts/validate-contracts.sh
```
