#!/bin/bash
# Validação completa — ScopeFlow AI QA Suite
# Backend + User Service + Contract Tests + Cobertura

set -e  # Parar no primeiro erro

PROJECT_ROOT="/home/mq/iGitHub/projeto-service-b2b"
LOG_FILE="$PROJECT_ROOT/logs/qa-validation-$(date +%Y%m%d-%H%M%S).log"
WITH_STACK=false

# Parse argumentos
while [[ $# -gt 0 ]]; do
  case $1 in
    --with-stack)
      WITH_STACK=true
      shift
      ;;
    *)
      echo "Uso: $0 [--with-stack]"
      echo "  --with-stack: Sobe a stack completa do Docker Compose antes dos testes"
      exit 1
      ;;
  esac
done

echo "📝 Log será salvo em: $LOG_FILE"
echo ""

# Redirecionar output para log e console
exec > >(tee -a "$LOG_FILE") 2>&1

# ============================================
# Pré-requisitos: Docker disponível
# ============================================
echo "🐳 [0/6] Verificando pré-requisitos..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker não encontrado. Instale o Docker para continuar."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker daemon não está rodando. Inicie o Docker Desktop/Engine."
    exit 1
fi

echo "✅ Docker disponível"

# Verificar recursos mínimos (4GB RAM recomendado para Testcontainers)
DOCKER_MEM=$(docker info --format '{{.MemTotal}}' 2>/dev/null || echo 0)
if [ "$DOCKER_MEM" -lt 4000000000 ]; then
    echo "⚠️  AVISO: Docker tem menos de 4GB RAM. Testcontainers pode falhar."
    echo "   Recomendado: 4GB+ para testes de integração."
fi

# ============================================
# Stack Docker Compose (opcional)
# ============================================
if [ "$WITH_STACK" = true ]; then
    echo ""
    echo "🚀 Subindo stack Docker Compose..."
    cd "$PROJECT_ROOT"

    # Subir apenas serviços essenciais (bancos + messaging + cache)
    docker compose up -d postgres user-db rabbitmq redis

    echo "⏳ Aguardando serviços ficarem prontos..."

    # Aguardar PostgreSQL (monólito)
    until docker exec scopeflow-postgres pg_isready -U postgres &> /dev/null; do
        echo "   Aguardando postgres..."
        sleep 2
    done
    echo "   ✅ postgres pronto"

    # Aguardar PostgreSQL (user-service)
    until docker exec scopeflow-user-db pg_isready -U postgres &> /dev/null; do
        echo "   Aguardando user-db..."
        sleep 2
    done
    echo "   ✅ user-db pronto"

    # Aguardar RabbitMQ
    until docker exec scopeflow-rabbitmq rabbitmq-diagnostics -q ping &> /dev/null; do
        echo "   Aguardando rabbitmq..."
        sleep 2
    done
    echo "   ✅ rabbitmq pronto"

    # Aguardar Redis
    until docker exec scopeflow-redis redis-cli ping &> /dev/null; do
        echo "   Aguardando redis..."
        sleep 2
    done
    echo "   ✅ redis pronto"

    echo "✅ Stack completa rodando"
else
    echo "ℹ️  Testcontainers criará containers efêmeros para testes de integração"
    echo "   (Use --with-stack para testar contra a stack completa)"
fi

echo ""
echo "🧪 [1/6] Backend — unitários..."
cd "$PROJECT_ROOT/backend"
./mvnw test

echo ""
echo "🐳 [2/6] Backend — integração (Testcontainers)..."
./mvnw verify

echo ""
echo "🔐 [3/6] User Service — completo..."
cd "$PROJECT_ROOT/user-service"
./mvnw verify

echo ""
echo "🤝 [4/6] Contract Tests (user-service ↔ monólito)..."
cd "$PROJECT_ROOT"
./scripts/validate-contracts.sh

echo ""
echo "📊 [5/6] Cobertura JaCoCo..."
cd "$PROJECT_ROOT/backend"
./mvnw jacoco:report

echo ""
echo "🧹 [6/6] Limpeza..."
if [ "$WITH_STACK" = true ]; then
    cd "$PROJECT_ROOT"
    echo "   Parando stack Docker Compose..."
    docker compose down
    echo "   ✅ Stack parada"
else
    echo "   ℹ️  Testcontainers já removeu containers efêmeros"
fi

echo ""
echo "✅ VALIDAÇÃO COMPLETA — SUCESSO!"
echo "📈 Relatório de cobertura: backend/target/site/jacoco/index.html"
echo "📝 Log completo salvo em: $LOG_FILE"
