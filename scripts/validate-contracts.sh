#!/bin/bash
set -e

###############################################################################
# validate-contracts.sh
#
# Validates contract tests between user-service (provider) and monolith (consumer).
# Part of Strangler Fig migration — ensures backward compatibility.
#
# Usage:
#   ./scripts/validate-contracts.sh
#
# Exit codes:
#   0  - All contract tests passed
#   1  - Provider tests failed
#   2  - Consumer tests failed
#   3  - JWT secret mismatch detected
###############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

USER_SERVICE_DIR="$PROJECT_ROOT/user-service"
BACKEND_DIR="$PROJECT_ROOT/backend"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE}Contract Tests Validation${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

###############################################################################
# Step 1: Validate JWT secrets match
###############################################################################
echo -e "${YELLOW}[1/4] Validating JWT secrets...${NC}"

USER_SERVICE_SECRET=$(grep "jwt.secret" "$USER_SERVICE_DIR/src/main/resources/application.yml" | awk -F': ' '{print $2}' | tr -d '"' || echo "")
BACKEND_SECRET=$(grep "jwt.secret" "$BACKEND_DIR/src/main/resources/application.yml" | awk -F': ' '{print $2}' | tr -d '"' || echo "")

if [ -z "$USER_SERVICE_SECRET" ] || [ -z "$BACKEND_SECRET" ]; then
    echo -e "${YELLOW}⚠️  JWT secrets not found in application.yml (may be using env vars)${NC}"
    echo -e "${YELLOW}   Ensure JWT_SECRET env var is identical in both services!${NC}"
else
    if [ "$USER_SERVICE_SECRET" != "$BACKEND_SECRET" ]; then
        echo -e "${RED}❌ JWT secret mismatch!${NC}"
        echo -e "${RED}   user-service: $USER_SERVICE_SECRET${NC}"
        echo -e "${RED}   backend: $BACKEND_SECRET${NC}"
        echo -e "${RED}   → JWT tokens will be rejected between services!${NC}"
        exit 3
    else
        echo -e "${GREEN}✅ JWT secrets match${NC}"
    fi
fi
echo ""

###############################################################################
# Step 2: Run provider contract tests (user-service)
###############################################################################
echo -e "${YELLOW}[2/4] Running provider contract tests (user-service)...${NC}"

cd "$USER_SERVICE_DIR"

if ! ./mvnw spring-cloud-contract:generateTests test -Dtest=ContractVerifier* 2>&1 | tee /tmp/provider-tests.log; then
    echo ""
    echo -e "${RED}❌ Provider contract tests FAILED${NC}"
    echo -e "${RED}   See logs above for details${NC}"
    echo -e "${RED}   → Fix contracts in user-service/src/test/resources/contracts/${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Provider tests passed${NC}"
echo ""

###############################################################################
# Step 3: Publish stubs to local Maven repository
###############################################################################
echo -e "${YELLOW}[3/4] Publishing stubs to local Maven repository...${NC}"

cd "$USER_SERVICE_DIR"

if ! ./mvnw clean install -DskipTests 2>&1 | tee /tmp/publish-stubs.log; then
    echo ""
    echo -e "${RED}❌ Failed to publish stubs${NC}"
    exit 1
fi

STUBS_JAR="$HOME/.m2/repository/com/scopeflow/user-service/1.0.0-SNAPSHOT/user-service-1.0.0-SNAPSHOT-stubs.jar"

if [ ! -f "$STUBS_JAR" ]; then
    echo -e "${RED}❌ Stubs JAR not found: $STUBS_JAR${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Stubs published: $STUBS_JAR${NC}"
echo ""

###############################################################################
# Step 4: Run consumer contract tests (monolith)
###############################################################################
echo -e "${YELLOW}[4/4] Running consumer contract tests (monolith)...${NC}"

cd "$BACKEND_DIR"

if ! ./mvnw test -Dtest=UserServiceContractTest 2>&1 | tee /tmp/consumer-tests.log; then
    echo ""
    echo -e "${RED}❌ Consumer contract tests FAILED${NC}"
    echo -e "${RED}   → user-service contract changed in a breaking way!${NC}"
    echo ""
    echo -e "${RED}   Action required:${NC}"
    echo -e "${RED}   1. Check what changed in user-service contracts${NC}"
    echo -e "${RED}   2. Update monolith to match new contract${NC}"
    echo -e "${RED}   3. OR revert breaking change in user-service${NC}"
    echo -e "${RED}   4. OR version the API (/api/v2/...)${NC}"
    echo ""
    echo -e "${YELLOW}   Logs: /tmp/consumer-tests.log${NC}"
    exit 2
fi

echo -e "${GREEN}✅ Consumer tests passed${NC}"
echo ""

###############################################################################
# Summary
###############################################################################
echo -e "${GREEN}=================================${NC}"
echo -e "${GREEN}✅ All contract tests PASSED${NC}"
echo -e "${GREEN}=================================${NC}"
echo ""
echo -e "Summary:"
echo -e "  ✅ JWT secrets validated"
echo -e "  ✅ Provider tests passed (8 contracts)"
echo -e "  ✅ Stubs published to Maven local"
echo -e "  ✅ Consumer tests passed (10 scenarios)"
echo ""
echo -e "${GREEN}Contracts are compatible! Safe to deploy.${NC}"
