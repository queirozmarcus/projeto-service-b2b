# Sprint 1 — Diagnóstico PostgreSQL
Data: 2026-04-23
Status: ✅ CONCLUÍDO

---

## 1. Docker Compose — Serviços de Banco

| Arquivo | Serviço | Image | Porta host:container | Database | Volume | depends_on |
|---------|---------|-------|---------------------|----------|--------|------------|
| docker-compose.yml | postgres | postgres:16-alpine | 5432:5432 | scopeflow | postgres_data | — |
| docker-compose.yml | user-db | postgres:16-alpine | 5433:5432 | scopeflow_users | user_db_data | — |
| docker-compose.yml | app (monólito) | build ./backend | 8080:8080 | scopeflow | — | postgres (healthy) |
| docker-compose.yml | user-service | build ./user-service | 8081:8081 | scopeflow_users | — | postgres (healthy), user-db (healthy) |
| docker-compose.staging.yml | user-service | (override) | — | scopeflow_users | — | user-db (healthy) |

---

## 2. Spring Datasource — Referências

| Arquivo | Serviço | spring.datasource.url | Flyway | Usuário |
|---------|---------|----------------------|--------|---------|
| backend/src/main/resources/application.yml | monólito | `${DATABASE_URL:jdbc:postgresql://localhost:5432/scopeflow}` | enabled | `${DATABASE_USER:postgres}` |
| user-service/src/main/resources/application.yml | user-service | `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/scopeflow}` ⚠️ | enabled | `${SPRING_DATASOURCE_USERNAME:postgres}` |
| user-service/src/main/resources/application-staging.yml | user-service | `${SPRING_DATASOURCE_URL:jdbc:postgresql://user-db:5432/scopeflow_users}` | enabled | — |
| user-service/src/main/resources/application-production.yml | user-service | (não definido — depende 100% de env var) | **disabled** ⚠️ | — |
| docker-compose.yml (app) | monólito | `jdbc:postgresql://postgres:5432/scopeflow` | — | postgres |
| docker-compose.yml (user-service) | user-service | `${USER_SERVICE_DATABASE_URL:-jdbc:postgresql://user-db:5432/scopeflow_users}` | — | postgres |

---

## 3. Variáveis de Ambiente

| Arquivo | Variável | Valor Atual |
|---------|----------|-------------|
| .env | DATABASE_URL | jdbc:postgresql://localhost:5432/scopeflow |
| .env | DATABASE_USER | postgres |
| .env | DATABASE_PASSWORD | postgres |
| .env | USER_SERVICE_DATABASE_URL | jdbc:postgresql://user-db:5432/scopeflow_users |
| .env | AUTH_SERVICE_EXTRACTED | true |
| .env.example | DATABASE_URL | jdbc:postgresql://localhost:5432/scopeflow |
| .env.example | USER_SERVICE_DATABASE_URL | jdbc:postgresql://localhost:5432/scopeflow ⚠️ (pré-cutover) |

---

## 4. Testcontainers

| Arquivo de Teste | Serviço | Container | DB Name | DynamicPropertySource |
|-----------------|---------|-----------|---------|----------------------|
| backend/.../MessagingIntegrationTestBase.java | monólito | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |
| backend/.../BriefingIntegrationTestBase.java | monólito | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |
| backend/.../UserServiceContractTest.java | monólito | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |
| user-service/.../AuthControllerIntegrationTest.java | user-service | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |
| user-service/.../UserControllerIntegrationTest.java | user-service | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |
| user-service/.../InvalidEmailValidationIntegrationTest.java | user-service | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |
| user-service/.../RegisterInvalidEmailE2ETest.java | user-service | PostgreSQLContainer postgres:16-alpine | scopeflow_test | sim |

> **Nota:** Testcontainers já são independentes de ambiente — sobem containers próprios.
> Nenhuma alteração necessária para a unificação.

---

## 5. Scripts

| Arquivo | Referência | Impacto |
|---------|-----------|---------|
| scripts/validate-qa-full.sh | `docker compose up -d postgres user-db rabbitmq redis` (ln 65) | Remover `user-db` |
| scripts/validate-qa-full.sh | `docker exec scopeflow-user-db pg_isready` (ln 77-81) | Remover healthcheck |
| scripts/check-stack-health.sh | `docker exec scopeflow-user-db pg_isready` (ln 40) | Remover check |
| scripts/README.md | `netstat ... grep -E '5432\|5433\|...'` (ln 104) | Remover porta 5433 |
| scripts/sync-workspace-id-to-user-service.sql | `docker exec scopeflow-user-db psql ... scopeflow_users` | Atualizar para postgres |

---

## 6. Volumes Docker

| Volume | Usado por | Tipo |
|--------|-----------|------|
| postgres_data | scopeflow-postgres (monólito) | named |
| user_db_data | scopeflow-user-db (user-service) | named — **será removido** |
| rabbitmq_data | scopeflow-rabbitmq | named |
| redis_data | scopeflow-redis | named |

---

## 7. Resumo de Impacto

**Total de arquivos a modificar: 9**

### Arquivos críticos (risco alto)

| Arquivo | Mudança necessária |
|---------|------------------|
| `docker-compose.yml` | Remover serviço `user-db`, redirecionar `user-service` para `postgres` |
| `user-service/application.yml` | Corrigir default datasource: `scopeflow` → `scopeflow_users` |
| `user-service/application-production.yml` | Habilitar Flyway + definir datasource |
| `docker-compose.staging.yml` | Remover override de banco |
| `.env` + `.env.example` | Consolidar `USER_SERVICE_DATABASE_URL` apontando para postgres:5432 |
| `scripts/validate-qa-full.sh` | Remover espera e referências a `user-db` |
| `scripts/check-stack-health.sh` | Remover check de `user-db` |
| `scripts/sync-workspace-id-to-user-service.sql` | Atualizar host de conexão |

---

## 8. Inconsistências Detectadas (bônus)

| # | Arquivo | Problema |
|---|---------|---------|
| ⚠️ 1 | `user-service/application.yml` | Default datasource aponta para `scopeflow` (monólito) — funciona só com profile staging ou env var injetada |
| ⚠️ 2 | `user-service/application-production.yml` | Flyway desabilitado + sem datasource configurado — cut-over de produção nunca foi executado |
| ⚠️ 3 | `.env.example` | Documenta estado pré-cutover — diverge do `.env` atual |

---

## 9. Ordem Sugerida de Mudanças (Sprints 2–10)

1. **S2** — Init script: criar database `scopeflow_users` no container `postgres` principal
2. **S3** — Migração de dados: `pg_dump scopeflow_users | pg_restore` no postgres
3. **S4** — Atualizar `user-service/application.yml` + `application-production.yml`
4. **S5** — Atualizar `docker-compose.yml` — remover `user-db`
5. **S6** — Atualizar `docker-compose.staging.yml` + `.env.example`
6. **S7** — Validar Testcontainers (já independentes — checar apenas)
7. **S8** — Validação QA completa
8. **S9** — ADR-010
9. **S10** — Cleanup: remover volume `user_db_data`, atualizar docs
