# Sprint 2 — Init Script PostgreSQL

## Arquivo criado
`docker/postgres/init/01-create-databases.sql`

## Conteúdo
```sql
-- Init script executado pelo container postgres na primeira inicialização
-- O database 'scopeflow' já é criado automaticamente via POSTGRES_DB env var
-- Este script garante que 'scopeflow_users' também exista no mesmo container (staging unificado)
SELECT 'CREATE DATABASE scopeflow_users'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'scopeflow_users')\gexec
```

## Mudança necessária no docker-compose.yml (Sprint 5)

Bloco `volumes:` do serviço `postgres` após adicionar o mount:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: scopeflow-postgres
    environment:
      POSTGRES_DB: scopeflow
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - scopeflow-network
```

## Como testar

Após aplicar a mudança no docker-compose.yml e rebuild:

```bash
# 1. Derrubar stack e remover volume para forçar reinicialização
docker compose down -v

# 2. Subir novamente (init scripts só rodam com volume vazio)
docker compose up -d postgres

# 3. Verificar que ambos os databases existem
docker exec scopeflow-postgres psql -U postgres -c "\l" | grep scopeflow

# Saída esperada:
# scopeflow       | postgres | ...
# scopeflow_users | postgres | ...
```

## Notas

- O init script só é executado na **primeira inicialização** do container (volume vazio).
  Se o volume `postgres_data` já existir, o script não roda — use `docker compose down -v` para forçar.
- O `\gexec` é uma extensão psql; funciona corretamente no entrypoint do container oficial postgres.
- A abordagem com `WHERE NOT EXISTS` é idempotente — não falha se o database já existir.
- O arquivo `.gitkeep` garante que o diretório `docker/postgres/init/` seja versionado mesmo que
  o SQL seja removido futuramente.
- Este setup é voltado para **desenvolvimento/staging unificado**. Em produção, `scopeflow_users`
  deve continuar no container `user-db` dedicado (porta 5433).
