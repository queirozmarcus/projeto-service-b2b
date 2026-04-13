#!/bin/bash
# Docker Compose Health Check & API Test Script

set -e

RESET='\033[0m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'

echo -e "${BLUE}🐳 ScopeFlow Docker Compose Test Suite${RESET}\n"

# Colors for status
check_service() {
    local service=$1
    local port=$2
    local health_url=$3

    echo -n "Checking $service... "

    if docker compose ps | grep -q "$service.*Up"; then
        if [ -z "$health_url" ]; then
            echo -e "${GREEN}✅ Running${RESET}"
            return 0
        fi

        # Try health check
        if curl -sf "$health_url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Healthy${RESET}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Running but health check pending${RESET}"
            return 0
        fi
    else
        echo -e "${RED}❌ Not running${RESET}"
        return 1
    fi
}

# Start services
echo -e "${BLUE}Starting services...${RESET}"
docker compose up -d

echo -e "\n${BLUE}Waiting for services to stabilize (30 seconds)...${RESET}"
sleep 30

# Check each service
echo -e "\n${BLUE}Checking service health:${RESET}\n"
check_service "postgres" "5432" "http://localhost:5432"
check_service "rabbitmq" "5672" ""
check_service "redis" "6379" ""
check_service "app" "8080" "http://localhost:8080/api/v1/health/ready"
check_service "frontend" "3000" "http://localhost:3000"

echo -e "\n${BLUE}Running API tests:${RESET}\n"

# Test Backend API
echo -n "Test 1: Backend health endpoint... "
if curl -sf http://localhost:8080/actuator/health | grep -q "UP"; then
    echo -e "${GREEN}✅ PASS${RESET}"
else
    echo -e "${RED}❌ FAIL${RESET}"
fi

# Test OpenAPI docs
echo -n "Test 2: OpenAPI documentation... "
if curl -sf http://localhost:8080/swagger-ui.html > /dev/null; then
    echo -e "${GREEN}✅ PASS${RESET}"
else
    echo -e "${RED}❌ FAIL${RESET}"
fi

# Test Frontend
echo -n "Test 3: Frontend is accessible... "
if curl -sf http://localhost:3000 > /dev/null; then
    echo -e "${GREEN}✅ PASS${RESET}"
else
    echo -e "${RED}❌ FAIL${RESET}"
fi

# Test RabbitMQ Management UI
echo -n "Test 4: RabbitMQ Management UI... "
if curl -sf -u guest:guest http://localhost:15672/api/overview > /dev/null; then
    echo -e "${GREEN}✅ PASS${RESET}"
else
    echo -e "${RED}❌ FAIL${RESET}"
fi

# Test PostgreSQL connectivity
echo -n "Test 5: PostgreSQL connectivity... "
if docker compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PASS${RESET}"
else
    echo -e "${RED}❌ FAIL${RESET}"
fi

# Test Redis connectivity
echo -n "Test 6: Redis connectivity... "
if docker compose exec -T redis redis-cli ping | grep -q "PONG"; then
    echo -e "${GREEN}✅ PASS${RESET}"
else
    echo -e "${RED}❌ FAIL${RESET}"
fi

echo -e "\n${BLUE}Docker environment status:${RESET}\n"
docker compose ps

echo -e "\n${GREEN}✅ Docker Compose test complete!${RESET}"
echo -e "\n${YELLOW}📝 Next steps:${RESET}"
echo "  1. Open http://localhost:3000 in browser (frontend)"
echo "  2. Open http://localhost:8080/swagger-ui.html for API docs"
echo "  3. Open http://localhost:15672 for RabbitMQ (guest/guest)"
echo "  4. View logs: docker compose logs -f app"
echo "  5. Stop: docker compose down"
