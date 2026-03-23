#!/usr/bin/env bash
# Helm Chart Validation Script
# Usage: ./validate.sh [chart-directory]

set -euo pipefail

CHART_DIR="${1:-scopeflow-briefing}"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔍 Validating Helm chart: ${CHART_DIR}"
echo ""

# Check if Helm is installed
if ! command -v helm &> /dev/null; then
    echo -e "${RED}❌ Helm is not installed${NC}"
    echo "Install Helm: https://helm.sh/docs/intro/install/"
    exit 1
fi

echo -e "${GREEN}✅ Helm is installed${NC}"
helm version --short
echo ""

# Change to chart directory
cd "${CHART_DIR}"

# 1. Lint chart
echo "📋 Step 1: Linting Helm chart..."
if helm lint .; then
    echo -e "${GREEN}✅ Helm lint passed${NC}"
else
    echo -e "${RED}❌ Helm lint failed${NC}"
    exit 1
fi
echo ""

# 2. Lint with staging values
echo "📋 Step 2: Linting with staging values..."
if helm lint . -f values-staging.yaml; then
    echo -e "${GREEN}✅ Staging values lint passed${NC}"
else
    echo -e "${RED}❌ Staging values lint failed${NC}"
    exit 1
fi
echo ""

# 3. Lint with production values
echo "📋 Step 3: Linting with production values..."
if helm lint . -f values-production.yaml; then
    echo -e "${GREEN}✅ Production values lint passed${NC}"
else
    echo -e "${RED}❌ Production values lint failed${NC}"
    exit 1
fi
echo ""

# 4. Template chart (dry-run)
echo "📋 Step 4: Templating chart (dry-run)..."
if helm template test-release . --set image.tag=test --debug > /dev/null; then
    echo -e "${GREEN}✅ Chart templating passed${NC}"
else
    echo -e "${RED}❌ Chart templating failed${NC}"
    exit 1
fi
echo ""

# 5. Template with staging values
echo "📋 Step 5: Templating with staging values..."
if helm template test-release . -f values-staging.yaml --set image.tag=test > /dev/null; then
    echo -e "${GREEN}✅ Staging values templating passed${NC}"
else
    echo -e "${RED}❌ Staging values templating failed${NC}"
    exit 1
fi
echo ""

# 6. Template with production values
echo "📋 Step 6: Templating with production values..."
if helm template test-release . -f values-production.yaml --set image.tag=test > /dev/null; then
    echo -e "${GREEN}✅ Production values templating passed${NC}"
else
    echo -e "${RED}❌ Production values templating failed${NC}"
    exit 1
fi
echo ""

# 7. Check required files
echo "📋 Step 7: Checking required files..."
REQUIRED_FILES=(
    "Chart.yaml"
    "values.yaml"
    "values-staging.yaml"
    "values-production.yaml"
    "templates/deployment.yaml"
    "templates/service.yaml"
    "templates/ingress.yaml"
    "templates/hpa.yaml"
    "templates/pdb.yaml"
    "templates/configmap.yaml"
    "templates/serviceaccount.yaml"
    "templates/_helpers.tpl"
)

ALL_PRESENT=true
for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "${file}" ]]; then
        echo -e "  ${GREEN}✅${NC} ${file}"
    else
        echo -e "  ${RED}❌${NC} ${file} (missing)"
        ALL_PRESENT=false
    fi
done

if [[ "${ALL_PRESENT}" == "false" ]]; then
    echo -e "${RED}❌ Some required files are missing${NC}"
    exit 1
fi
echo ""

# 8. Chart summary
echo "📊 Chart Summary:"
echo "  Name: $(yq eval '.name' Chart.yaml)"
echo "  Version: $(yq eval '.version' Chart.yaml)"
echo "  App Version: $(yq eval '.appVersion' Chart.yaml)"
echo "  Type: $(yq eval '.type' Chart.yaml)"
echo ""

echo -e "${GREEN}🎉 All validations passed!${NC}"
echo ""
echo "Next steps:"
echo "  1. Install chart locally:"
echo "     helm install test-release . --dry-run --debug"
echo ""
echo "  2. Deploy to Kubernetes:"
echo "     helm upgrade --install scopeflow-briefing . \\"
echo "       --namespace staging \\"
echo "       --values values-staging.yaml \\"
echo "       --set image.tag=latest"
echo ""
