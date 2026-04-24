──────────────────────────────────
Plano: Validação de Ambiente Docker — DB-per-service
Data: 2026-04-17
Status: APROVADO
──────────────────────────────────

## Contexto

DB-per-service já está consolidado no docker-compose.yml padrão:
- Monólito → postgres:5432/scopeflow
- User Service → user-db:5432/scopeflow_users

Objetivo: validar que a stack completa funciona corretamente.

## Etapas

1. **Subir stack completa**
   → Agent: devops-engineer
   → Comando: `docker compose up -d`
   → Output: 7 serviços rodando

2. **Verificar health checks**
   → Agent: devops-engineer
   → Validar: postgres, user-db, rabbitmq, redis, traefik, app, user-service
   → Output: todos healthy ou diagnosticar falhas

3. **Validar conexões de banco**
   → Agent: devops-engineer
   → Monólito conecta em postgres:5432/scopeflow
   → User Service conecta em user-db:5432/scopeflow_users
   → Output: logs de conexão confirmados

4. **Validar Traefik routing (Strangler Fig)**
   → Agent: devops-engineer
   → /api/v1/auth/* → user-service (prioridade 100)
   → /api/* → monólito (prioridade 50)
   → Output: roteamento confirmado

5. **Smoke tests**
   → Agent: devops-engineer
   → Health endpoints acessíveis
   → Login via Traefik funcional
   → Tabelas existem nos bancos corretos
   → Output: relatório de smoke tests

6. **Relatório final**
   → Agent: devops-engineer
   → Consolidar resultados
   → Listar issues (se houver)
   → Propor fixes (se necessário)

## Entregáveis

- Relatório de validação (serviços OK/FAIL)
- Lista de issues encontrados (se houver)
- Comandos de troubleshooting (se necessário)

## Riscos

- Docker Desktop não rodando (WSL2)
- Portas ocupadas (5432, 5433, 8080, 8081, etc)
- Permissões de volumes
- Imagens não buildadas

## Decisões

**Why devops-engineer?**
- Especialista em Docker, infra, troubleshooting
- Conhece health checks, logs, networking
- Modelo Sonnet é adequado para validação técnica

## Tempo Estimado

15-20 minutos
