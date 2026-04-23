──────────────────────────────────
Plano: Extração User (Auth) — ScopeFlow AI
Data: 2026-04-05
Status: CONCLUÍDO ✅
Duração: ~3h (6 etapas)

[Contexto]
Primeira extração do monólito ScopeFlow AI → microsserviços via Strangler Fig.
Contexto User (Auth) escolhido por ter "zero dependências de saída" (ADR-001),
validando infra de microsserviços com menor risco.

Stack: Java 21, Spring Boot 3.2, PostgreSQL, Docker Compose, Traefik.
Ambiente: local (dev).
Database strategy: shared inicial (ADR-002 — mesma instância PostgreSQL).

Bloqueadores resolvidos no plano:
- Faltam 2 endpoints no monólito (GET /users/by-email, POST /users/invited) → etapa 1
- Infra local não existe → etapa 3

[Etapas]
1. ✅ /dev-feature "endpoints de gestão de usuário para invite flow"
   → backend-dev
   → GET /users/by-email/{email} + POST /users/invited implementados no monólito
   → 2 endpoints + 4 exceções (USER-010 a USER-013) + 8 testes unitários

2. ✅ /qa-generate UserController (novos endpoints)
   → unit-test-engineer + integration-test-engineer
   → Testes unitários + integração (Testcontainers) dos 2 endpoints
   → 8 testes de integração (PostgreSQL 16 real) + RFC 9457 validation

3. ✅ /devops-provision user-service local
   → iac-engineer → devops-engineer
   → Docker Compose: Traefik + PostgreSQL compartilhado + user-service
   → Dockerfile user-service (multi-stage, non-root, 471MB)
   → Traefik routing: /api/v1/auth/* → user-service (priority 100)
   → Validação: 9/10 checks OK, script validate-user-service-infra.sh

4. ✅ /migration-extract User (Auth) — OPUS
   → tech-lead → backend-engineer → data-engineer → qa-engineer
   → User Service extraído com arquitetura hexagonal
   → Shared DB (tabela users, flyway.enabled=false)
   → JWT validation com shared secret (via .env)
   → Strangler Fig via Traefik + feature flag (auth.service.use-extracted=false default)
   → 17/17 testes unitários PASS, proxy config RestTemplate, código sintaticamente correto

5. ✅ /qa-contract user-service ↔ monolito
   → contract-test-engineer
   → Contract tests (Spring Cloud Contract: 8 contratos YAML)
   → Provider tests (user-service) + Consumer tests (monólito, 10 testes)
   → CI script validate-contracts.sh, JWT compatibility validation ✅

6. ✅ /qa-e2e "smoke tests auth flow completo"
   → e2e-test-engineer
   → Smoke tests E2E: registro → login → JWT validation → acesso protegido
   → Core script auth-flow.test.sh (500 linhas, 7 cenários)
   → Testa ambos os modos (flag=false + flag=true), JWT cross-service validation ✅
   → CI/CD pipeline GitHub Actions (3 jobs), documentação completa

[Riscos]
⚠️ FK workspaces.owner_id → users.id: Shared DB resolve agora, mas split futuro de DB vai virar chamada REST (acoplamento). Considerar evento UserCreatedEvent.

⚠️ JWT shared secret: Rotação manual. Em produção migrar para JWKS endpoint.

⚠️ Performance baseline: Medir latência do monólito ANTES da extração (network hop adiciona ~5-10ms).

⚠️ Sem observabilidade: Debug cross-service via logs. Adicionar OpenTelemetry quando escalar.

⚠️ Etapa 4 (migration-extract): RISCO ALTO — primeira extração do monólito.

[Decisões]
- JWT strategy: shared secret via .env (simplicidade local). Em prod → JWKS endpoint.
  **Why:** ambiente local, zero overhead. JWKS é necessário quando múltiplos serviços assinam tokens.

- API Gateway: Traefik (leve, config via Docker labels).
  **Why:** já usado no projeto, zero learning curve.

- Service discovery: nenhum (Docker Compose DNS nativo).
  **Why:** suficiente para dev local. Em prod → Kubernetes DNS.

- Database: shared inicial (ADR-002 aprovado).
  **Why:** split de DB é fase futura. Validar infra de microsserviços primeiro.

- Deployment: Docker Compose puro (sem K8s, sem observability).
  **Why:** simplicidade local. K8s + observability = produção.

- Endpoints faltantes: implementar ANTES da extração (etapa 1).
  **Why:** bloquear extração com código faltando = receita de rollback.

- Strangler Fig: Traefik routing + feature flag.
  **Why:** rollback instantâneo (toggle flag), zero downtime.
──────────────────────────────────
