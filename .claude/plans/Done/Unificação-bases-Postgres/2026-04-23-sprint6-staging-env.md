# Sprint 6 — Atualizar docker-compose.staging.yml + .env.example

**Status:** Concluído  
**Data:** 2026-04-23

## Arquivos Modificados

### `docker-compose.staging.yml`
- Removida referência ao serviço `user-db` no `depends_on`
- Substituído `SPRING_DATASOURCE_URL: jdbc:postgresql://user-db:5432/scopeflow_users` por `jdbc:postgresql://postgres:5432/scopeflow_users`
- `depends_on` agora aponta para `postgres` (com `service_healthy`)
- Cabeçalho atualizado para refletir unificação PostgreSQL (S5)
- Adicionada nota de deploy com `down -v` + `up`

### `.env.example`
- `USER_SERVICE_DATABASE_URL` atualizado: `localhost:5432/scopeflow` → `localhost:5432/scopeflow_users`
- Removidas linhas comentadas do fluxo pré/pós-cutover (obsoletas)
- Adicionado comentário explicando que ambos os services usam o mesmo servidor PostgreSQL, databases separados
- `AUTH_SERVICE_URL` corrigido para `localhost:8081` (era `user-service:8081` — inválido fora do Docker)
- Seção Rate Limiter com comentário expandido

## Procedimento de Deploy em Staging

Após esta mudança, ao subir staging pela primeira vez:

```bash
# 1. Derrubar stack e volumes para garantir estado limpo
docker compose -f docker-compose.yml -f docker-compose.staging.yml down -v

# 2. Subir stack completa
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d

# 3. Aguardar postgres ficar healthy e Flyway aplicar migrations
docker logs scopeflow-user-service -f
```

O init script do postgres (S5) cria o database `scopeflow_users` automaticamente no primeiro boot. Flyway do user-service aplica V1 no startup.

## Contexto
- S5 removeu `user-db` do `docker-compose.yml` e adicionou init script ao `postgres`
- S4 já configurou user-service para apontar para `postgres:5432/scopeflow_users`
- Este sprint (S6) sincronizou os arquivos de staging e exemplo com essa realidade
