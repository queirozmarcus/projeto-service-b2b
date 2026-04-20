#!/bin/bash
# Health check da stack Docker Compose
# Valida se todos os serviços estão respondendo

set -e

PROJECT_ROOT="/home/mq/iGitHub/projeto-service-b2b"
cd "$PROJECT_ROOT"

echo "🔍 Verificando saúde da stack..."
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_service() {
    local service_name=$1
    local check_command=$2

    if $check_command &> /dev/null; then
        echo -e "${GREEN}✅ $service_name${NC}"
        return 0
    else
        echo -e "${RED}❌ $service_name${NC}"
        return 1
    fi
}

FAILED=0

# PostgreSQL (monólito)
if ! check_service "postgres (monólito)" "docker exec scopeflow-db pg_isready -U postgres"; then
    FAILED=$((FAILED + 1))
fi

# PostgreSQL (user-service)
if ! check_service "user-db (user-service)" "docker exec scopeflow-user-db pg_isready -U postgres"; then
    FAILED=$((FAILED + 1))
fi

# RabbitMQ
if ! check_service "rabbitmq" "docker exec scopeflow-rabbitmq rabbitmq-diagnostics -q ping"; then
    FAILED=$((FAILED + 1))
fi

# Redis
if ! check_service "redis" "docker exec scopeflow-redis redis-cli ping"; then
    FAILED=$((FAILED + 1))
fi

# Traefik (opcional)
if docker ps --format '{{.Names}}' | grep -q scopeflow-traefik; then
    if ! check_service "traefik" "docker exec scopeflow-traefik wget -q --spider http://localhost:8080/ping"; then
        echo -e "${YELLOW}   ⚠️  Traefik presente mas não respondendo${NC}"
    fi
fi

# Backend API (opcional)
if docker ps --format '{{.Names}}' | grep -q scopeflow-api; then
    if ! check_service "backend API" "docker exec scopeflow-api wget -q --spider http://localhost:8080/actuator/health"; then
        echo -e "${YELLOW}   ⚠️  Backend presente mas não respondendo${NC}"
    fi
fi

# User Service (opcional)
if docker ps --format '{{.Names}}' | grep -q scopeflow-user-service; then
    if ! check_service "user-service" "docker exec scopeflow-user-service wget -q --spider http://localhost:8081/actuator/health"; then
        echo -e "${YELLOW}   ⚠️  User service presente mas não respondendo${NC}"
    fi
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ Stack saudável${NC}"
    exit 0
else
    echo -e "${RED}❌ $FAILED serviço(s) com problema${NC}"
    echo ""
    echo "Para subir a stack:"
    echo "  docker compose up -d"
    echo ""
    echo "Para debugar:"
    echo "  docker compose ps"
    echo "  docker compose logs [service]"
    exit 1
fi
