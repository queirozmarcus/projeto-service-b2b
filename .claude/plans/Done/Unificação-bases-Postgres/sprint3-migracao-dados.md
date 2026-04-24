# Sprint 3 — Migração de Dados: user-db → postgres

**Status:** Done
**Data:** 2026-04-23

## Arquivo criado

- `scripts/migrate-user-db-to-postgres.sh` — executável (chmod +x)
- `.gitignore` — entrada `backups/` adicionada

## Resumo dos passos do script

1. Verifica Docker rodando e containers `scopeflow-postgres` + `scopeflow-user-db` com status healthy
2. Cria backup via `pg_dump -F c` (formato custom, restaurável) em `backups/scopeflow_users_backup_YYYYMMDD_HHMMSS.dump`
3. Verifica se database `scopeflow_users` já existe em `scopeflow-postgres` (criado pelo init script da Sprint 2)
4. Fallback: cria o database manualmente se não existir
5. Executa `pg_restore --no-owner --no-privileges -c --if-exists` — idempotente: limpa e reaplica em toda execução
6. Valida integridade: contagem de tabelas e contagem de registros por tabela (origem vs destino)
7. Reporta resultado e instrução do próximo passo

## Comando de execução

```bash
# Pré-condição: stack rodando e healthy
docker compose up -d
# Aguardar ~30s para todos ficarem healthy, então:
./scripts/migrate-user-db-to-postgres.sh
```

## Validações realizadas pelo script

| Validação | Método |
|-----------|--------|
| Docker health | `docker info` |
| Containers healthy | `docker inspect .State.Health.Status` |
| Contagem de tabelas | `information_schema.tables` count — origem vs destino |
| Contagem de registros | `SELECT COUNT(*)` por tabela — origem vs destino |

## Pré-condições

- Docker Desktop rodando
- `docker compose up -d` executado e todos os serviços com status `healthy`
- Containers obrigatórios: `scopeflow-postgres` (5432) e `scopeflow-user-db` (5433)

## Aviso Sprint 5

Antes de executar `docker compose down -v` (remove volumes — irreversível):
- Confirmar que o backup em `backups/` está seguro e acessível
- Confirmar Sprint 4 concluída (user-service datasource apontando para `scopeflow-postgres`)
- `docker compose down -v` destrói `user_db_data` volume permanentemente
