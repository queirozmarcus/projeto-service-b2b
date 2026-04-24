# Sprint 5 — Consolidação PostgreSQL no docker-compose.yml

**Data:** 2026-04-23
**Status:** Concluído

## Mudanças Aplicadas

### 1. `postgres` — init script montado
```diff
 volumes:
   - postgres_data:/var/lib/postgresql/data
+  - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
```
O script `01-create-databases.sql` cria `scopeflow_users` automaticamente na primeira inicialização do container.

### 2. Serviço `user-db` — REMOVIDO
Bloco completo removido (image, container_name, environment, ports, volumes, healthcheck, networks).

### 3. `user-service` — `depends_on` atualizado
```diff
 depends_on:
   postgres:
     condition: service_healthy
-  user-db:
-    condition: service_healthy
```

### 4. `user-service` — `SPRING_DATASOURCE_URL` atualizado
```diff
-SPRING_DATASOURCE_URL: ${USER_SERVICE_DATABASE_URL:-jdbc:postgresql://user-db:5432/scopeflow_users}
+SPRING_DATASOURCE_URL: ${USER_SERVICE_DATABASE_URL:-jdbc:postgresql://postgres:5432/scopeflow_users}
```

### 5. Volume `user_db_data` — REMOVIDO
```diff
 volumes:
   postgres_data:
-  user_db_data:
   rabbitmq_data:
   redis_data:
```

## Aviso Importante — Ambientes Existentes

Se já existe uma stack rodando com `user-db` e dados em `user_db_data`:

1. **Migrar dados antes** (recomendado):
   ```bash
   ./scripts/migrate-user-db-to-postgres.sh
   ```
2. Depois destruir os volumes e subir a stack consolidada:
   ```bash
   docker compose down -v
   docker compose up -d
   ```

`docker compose down -v` **destrói todos os volumes** — incluindo `postgres_data`. Use apenas em dev/staging sem dados críticos, ou faça backup antes.

## Validação

```bash
# Verificar sintaxe do compose
docker compose config

# Subir stack e verificar serviços
docker compose up -d
docker compose ps

# Confirmar que scopeflow_users foi criado pelo init script
docker exec scopeflow-postgres psql -U postgres -c "\l"
```
