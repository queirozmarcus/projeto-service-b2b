# DB Migration Guide — user-service

Guia de cut-over do banco compartilhado (`scopeflow`) para o banco exclusivo do user-service (`scopeflow_users`).

## Contexto

Durante a extração do user-service pelo padrão Strangler Fig, o serviço inicialmente compartilhou o banco do monólito (Fase 3–4). Este guia cobre a Fase 5: separação do banco com zero perda de dados.

**Estado atual (pré-cutover):**
- user-service lê/escreve tabela `users` em `scopeflow` (porta 5432)
- Monólito também lê/escreve `users` no mesmo banco
- `SPRING_DATASOURCE_URL` no user-service aponta para `postgres:5432/scopeflow`

**Estado alvo (pós-cutover):**
- user-service é source of truth para `users` em `scopeflow_users` (porta 5433)
- Monólito consulta users via API do user-service (sem acesso direto ao banco)
- `SPRING_DATASOURCE_URL` no user-service aponta para `user-db:5432/scopeflow_users`

---

## Pré-requisitos

1. user-service estável em staging por no mínimo 48 horas sem erros 5xx
2. Banco `scopeflow_users` provisionado e acessível (container `user-db` rodando)
3. Flyway migration `V1__create_users_table.sql` validada em banco vazio
4. Monólito preparado para chamar user-service via API para operações em `users` (requisito de desenvolvimento prévio — fora do escopo deste guia)
5. Janela de manutenção agendada (estimativa: 5–15 min dependendo do volume)
6. Backup verificado do banco `scopeflow`

---

## Passos de Migração de Dados

### 1. Backup preventivo

```bash
# Backup completo do banco do monólito antes de qualquer operação
pg_dump -h localhost -p 5432 -U postgres -d scopeflow \
  -f /tmp/scopeflow_backup_$(date +%Y%m%d_%H%M%S).sql

# Verificar que o backup foi criado corretamente
ls -lh /tmp/scopeflow_backup_*.sql
```

### 2. Verificar contagem de registros na origem

```sql
-- Executar no banco scopeflow (porta 5432)
SELECT COUNT(*) AS total_users FROM users;
SELECT status, COUNT(*) FROM users GROUP BY status ORDER BY status;

-- Guardar estes números para validação pós-migração
```

### 3. Parar o user-service

```bash
# Em produção: drenar conexões com graceful shutdown
docker stop scopeflow-user-service

# Verificar que não há writes pendentes
docker logs scopeflow-user-service --tail 20
```

### 4. Dump da tabela users para o banco exclusivo

```bash
# Dump somente da tabela users (dados + sequências, sem schema — Flyway cria o schema)
pg_dump -h localhost -p 5432 -U postgres -d scopeflow \
  --table=users \
  --data-only \
  --no-owner \
  --no-acl \
  -f /tmp/users_data_$(date +%Y%m%d_%H%M%S).sql

# Restore no banco exclusivo
# ATENÇÃO: Flyway V1 deve ter sido executado antes deste passo para que a tabela exista
psql -h localhost -p 5433 -U postgres -d scopeflow_users \
  -f /tmp/users_data_*.sql
```

### 5. Validar integridade pós-restore

```sql
-- Executar no banco scopeflow_users (porta 5433) e comparar com os números do passo 2

-- Contagem total
SELECT COUNT(*) AS total_users FROM users;

-- Distribuição por status (deve ser idêntica)
SELECT status, COUNT(*) FROM users GROUP BY status ORDER BY status;

-- Checksum das colunas críticas (deve ser idêntico ao da origem)
SELECT MD5(STRING_AGG(
    id::text || email || status || created_at::text,
    ',' ORDER BY id
)) AS checksum
FROM users;

-- Sampling: 10 registros aleatórios para inspeção visual
SELECT id, email, full_name, status, created_at
FROM users
ORDER BY RANDOM()
LIMIT 10;
```

### 6. Atualizar variável de ambiente e subir user-service

```bash
# Atualizar USER_SERVICE_DATABASE_URL no .env ou no orquestrador (Kubernetes/ECS)
# De: USER_SERVICE_DATABASE_URL=jdbc:postgresql://postgres:5432/scopeflow
# Para: USER_SERVICE_DATABASE_URL=jdbc:postgresql://user-db:5432/scopeflow_users

# Subir o user-service apontando para o novo banco
docker start scopeflow-user-service

# Aguardar readiness probe
curl -f http://localhost:8081/api/v1/actuator/health/readiness
```

### 7. Smoke tests pós-cutover

```bash
# Login com usuário existente (valida que dados foram migrados corretamente)
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@teste.com", "password": "senha123"}'

# Registro de novo usuário (valida que writes funcionam no novo banco)
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "novo@teste.com", "password": "senha123", "fullName": "Novo Usuário"}'
```

---

## Ordem de Operações — Resumo

| # | Ação | Downtime? |
|---|------|-----------|
| 1 | Backup `scopeflow` | Não |
| 2 | Registrar contagens de validação | Não |
| 3 | Parar user-service | **Sim — início da janela** |
| 4 | `pg_dump users --data-only` → `psql scopeflow_users` | Sim |
| 5 | Validar contagem + checksum + sampling | Sim |
| 6 | Atualizar `USER_SERVICE_DATABASE_URL` + subir user-service | Sim |
| 7 | Smoke tests | **Não — fim da janela** |

Estimativa de downtime: **2–10 minutos** (depende do volume de `users`).
Para tabelas com >1M de registros, considerar replicação lógica (Debezium/pglogical) para reduzir janela.

---

## Rollback

Se qualquer etapa falhar após o passo 3:

```bash
# 1. Parar o user-service (se estiver rodando com configuração incorreta)
docker stop scopeflow-user-service

# 2. Reverter USER_SERVICE_DATABASE_URL para o banco compartilhado
# USER_SERVICE_DATABASE_URL=jdbc:postgresql://postgres:5432/scopeflow

# 3. Subir o user-service apontando para o banco original
docker start scopeflow-user-service

# 4. Verificar healthcheck
curl -f http://localhost:8081/api/v1/actuator/health/readiness
```

O banco `scopeflow_users` pode ser descartado ou mantido para nova tentativa.
O banco compartilhado `scopeflow` NÃO foi modificado em nenhum passo — rollback é imediato.

---

## Validação de Integridade — Queries de Referência

```sql
-- Contagem por status (executar em ambos os bancos e comparar)
SELECT status, COUNT(*) FROM users GROUP BY status ORDER BY status;

-- Checksum reproduzível (executar em ambos os bancos e comparar)
SELECT MD5(STRING_AGG(
    id::text || email || password_hash || full_name || status || created_at::text,
    ',' ORDER BY id
)) AS checksum
FROM users;

-- Verificar ausência de registros órfãos
SELECT COUNT(*) FROM users WHERE status NOT IN ('ACTIVE', 'INACTIVE', 'DELETED');

-- Verificar integridade do campo version (deve ser >= 0 em todos)
SELECT COUNT(*) FROM users WHERE version IS NULL OR version < 0;
```

---

## Pós-cutover: Limpeza (executar após estabilização — mínimo 7 dias)

Após confirmação de estabilidade em produção:

1. Remover `users` do schema do monólito (nova migration no monólito — não alterar V2)
2. Atualizar `ddl-auto: validate` no monólito para não validar `users`
3. Remover `depends_on: user-db` do serviço `app` no docker-compose
4. Arquivar este guia em `docs/architecture/adr/` como registro histórico

**Nunca remover a tabela `users` do monólito antes de confirmar estabilidade.**
