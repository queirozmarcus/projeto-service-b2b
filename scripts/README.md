# Scripts de Validação — ScopeFlow AI

Este diretório contém scripts para validação completa do sistema.

## validate-qa-full.sh

Script principal de validação que executa:
1. ✅ Verificação de pré-requisitos (Docker disponível, memória)
2. 🧪 Testes unitários (backend)
3. 🐳 Testes de integração com Testcontainers (backend)
4. 🔐 Testes completos do user-service
5. 🤝 Testes de contrato (provider + consumer)
6. 📊 Relatório de cobertura JaCoCo

### Uso Básico

```bash
# Modo padrão: Testcontainers cria containers efêmeros
./scripts/validate-qa-full.sh

# Com stack completa: sobe Docker Compose antes dos testes
./scripts/validate-qa-full.sh --with-stack
```

### Quando usar --with-stack?

| Cenário | Modo recomendado | Por quê |
|---------|------------------|---------|
| CI/CD pipeline | **Padrão** | Isolamento total, sem conflitos |
| Dev local rápido | **Padrão** | Testcontainers é mais rápido (containers efêmeros) |
| Debug de integração | `--with-stack` | Inspecionar estado do DB após testes |
| Validar contra staging | `--with-stack` | Usar docker-compose.staging.yml |

### Output

- **Console**: Progresso em tempo real
- **Log**: `logs/qa-validation-YYYYMMDD-HHMMSS.log`
- **Cobertura**: `backend/target/site/jacoco/index.html`

## check-stack-health.sh

Health check rápido da stack Docker Compose.

```bash
# Verificar se todos os serviços estão respondendo
./scripts/check-stack-health.sh
```

Valida:
- ✅ PostgreSQL (monólito)
- ✅ PostgreSQL (user-service)
- ✅ RabbitMQ
- ✅ Redis
- ⚠️ Traefik (se presente)
- ⚠️ Backend API (se presente)
- ⚠️ User Service (se presente)

### Exit Codes

- `0`: Stack saudável
- `1`: Um ou mais serviços com problema

## validate-contracts.sh

Valida contratos entre user-service (provider) e monólito (consumer).

```bash
./scripts/validate-contracts.sh
```

**Executado automaticamente por `validate-qa-full.sh`** — não precisa rodar manualmente.

## Troubleshooting

### Docker não encontrado

```bash
# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Ou via WSL2
wsl --install
```

### Docker daemon não está rodando

```bash
# Windows: Abrir Docker Desktop
# Linux: sudo systemctl start docker
```

### Testcontainers falha (memória insuficiente)

```bash
# Docker Desktop: Settings → Resources → Memory → 4GB+
# Linux: Verificar cgroups memory limit
```

### Stack não sobe

```bash
# Verificar portas em uso
netstat -tuln | grep -E '5432|5433|5672|6379|8080|8081|80'

# Limpar containers órfãos
docker compose down -v
docker system prune -f
```

## Exemplos de Uso

### CI/CD (GitHub Actions)

```yaml
- name: Validate QA
  run: ./scripts/validate-qa-full.sh
  timeout-minutes: 10
```

### Dev Local — Validação Rápida

```bash
# Só unitários (30s)
cd backend && ./mvnw test

# Só integração (2min)
cd backend && ./mvnw verify

# Full suite (5min)
./scripts/validate-qa-full.sh
```

### Debug de Integração

```bash
# 1. Subir stack completa
docker compose up -d

# 2. Verificar saúde
./scripts/check-stack-health.sh

# 3. Rodar testes apontando para stack
./scripts/validate-qa-full.sh --with-stack

# 4. Inspecionar DB após falha
docker exec -it scopeflow-db psql -U postgres -d scopeflow
\dt
SELECT * FROM outbox_events WHERE published_at IS NULL;

# 5. Logs de serviço
docker logs scopeflow-user-service -f
```

## Relatórios Gerados

| Arquivo | Conteúdo |
|---------|----------|
| `logs/qa-validation-*.log` | Log completo da execução |
| `backend/target/site/jacoco/index.html` | Cobertura de código |
| `backend/target/surefire-reports/` | Relatórios de testes unitários |
| `backend/target/failsafe-reports/` | Relatórios de testes de integração |
| `user-service/target/surefire-reports/` | Relatórios do user-service |

## Performance

| Etapa | Tempo médio | Docker |
|-------|-------------|--------|
| Pré-requisitos | 2s | ✓ |
| Backend unitários | 20s | ✗ |
| Backend integração | 90s | ✓ Testcontainers |
| User-service | 60s | ✓ Testcontainers |
| Contract tests | 30s | ✗ |
| Cobertura | 10s | ✗ |
| **Total** | **~3-4min** | Testcontainers |
| **Total (--with-stack)** | **~4-5min** | Docker Compose |

## Manutenção

### Atualizar versões de dependências

```bash
# Backend
cd backend && ./mvnw versions:display-dependency-updates

# User-service
cd user-service && ./mvnw versions:display-dependency-updates
```

### Limpar caches

```bash
# Maven
rm -rf ~/.m2/repository/com/scopeflow

# Docker
docker system prune -af --volumes
```
