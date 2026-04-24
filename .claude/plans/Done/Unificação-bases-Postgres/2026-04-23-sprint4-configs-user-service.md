# Sprint 4 — Atualizar configs do user-service (datasource + env vars)

**Status:** Concluído  
**Data:** 2026-04-23  
**Executado por:** backend-dev

---

## Arquivos modificados

1. `user-service/src/main/resources/application.yml`
2. `user-service/src/main/resources/application-production.yml`
3. `user-service/src/main/resources/application-staging.yml`

---

## Diffs resumidos

### 1. application.yml
```diff
- url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/scopeflow}
+ url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/scopeflow_users}
```
O default local apontava para o banco do monólito (`scopeflow`). Corrigido para `scopeflow_users`.

### 2. application-production.yml
```diff
- # TODO (pós cut-over): alterar enabled para true e apontar datasource
- spring:
-   flyway:
-     enabled: false

+ spring:
+   datasource:
+     url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/scopeflow_users}
+   flyway:
+     enabled: true
```
Cut-over executado: Flyway habilitado e datasource adicionado apontando para `scopeflow_users`.
Cabeçalho do arquivo atualizado para refletir o estado pós cut-over.

### 3. application-staging.yml
```diff
- url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://user-db:5432/scopeflow_users}
+ url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/scopeflow_users}
```
Host `user-db` (container dedicado, porta 5433) substituído por `postgres` (container principal, porta 5432).
Cabeçalho e comentários inline atualizados para documentar a remoção do `user-db`.

---

## Dependência de env var em produção

Em produção, `SPRING_DATASOURCE_URL` **deve** ser definida como variável de ambiente obrigatória
(ex: RDS endpoint). O default `localhost:5432/scopeflow_users` em `application-production.yml`
existe apenas como referência — nunca será usado diretamente no ambiente real.

Env vars obrigatórias em produção:
- `SPRING_DATASOURCE_URL` — URL completa do PostgreSQL (ex: `jdbc:postgresql://rds-host:5432/scopeflow_users`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` — compartilhado com o monólito (cross-compatible)
