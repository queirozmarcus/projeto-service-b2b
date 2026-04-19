#!/bin/bash
# Validação completa — ScopeFlow AI QA Suite
# Backend + User Service + Contract Tests + Cobertura

set -e  # Parar no primeiro erro

PROJECT_ROOT="/home/mq/iGitHub/projeto-service-b2b"
LOG_FILE="$PROJECT_ROOT/logs/qa-validation-$(date +%Y%m%d-%H%M%S).log"

echo "📝 Log será salvo em: $LOG_FILE"
echo ""

# Redirecionar output para log e console
exec > >(tee -a "$LOG_FILE") 2>&1

echo "🧪 [1/5] Backend — unitários..."
cd "$PROJECT_ROOT/backend"
./mvnw test

echo ""
echo "🐳 [2/5] Backend — integração (Testcontainers)..."
./mvnw verify

echo ""
echo "🔐 [3/5] User Service — completo..."
cd "$PROJECT_ROOT/user-service"
./mvnw verify

echo ""
echo "🤝 [4/5] Contract Tests (user-service ↔ monólito)..."
cd "$PROJECT_ROOT"
./scripts/validate-contracts.sh

echo ""
echo "📊 [5/5] Cobertura JaCoCo..."
cd "$PROJECT_ROOT/backend"
./mvnw jacoco:report

echo ""
echo "✅ VALIDAÇÃO COMPLETA — SUCESSO!"
echo "📈 Relatório de cobertura: backend/target/site/jacoco/index.html"
echo "📝 Log completo salvo em: $LOG_FILE"
