#!/bin/bash

###############################################################################
#                                                                             #
#  Terminal 2: Briefing Domain — Full Orchestration Script                   #
#  Sequences all 6 steps (Architect → Backend-Dev → DBA → API-Designer       #
#  → DevOps → Consolidation) with agent forks                                #
#                                                                             #
#  Usage:
#    ./run-terminal2.sh              # Run all steps sequentially
#    ./run-terminal2.sh 2            # Start from step 2 (if step 1 done)
#    ./run-terminal2.sh --help       # Show help
#                                                                             #
###############################################################################

set -e

PROJECT_ROOT="/home/mq/iGitHub/projeto-service-b2b"
FORKS_DIR="${PROJECT_ROOT}/.claude/forks"
PLANS_DIR="${PROJECT_ROOT}/.claude/plans"
LOG_FILE="${PROJECT_ROOT}/.claude/terminal2-execution.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Timestamp
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

###############################################################################
# Logging Functions
###############################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

###############################################################################
# Validation
###############################################################################

validate_environment() {
    log_info "Validating environment..."

    if [ ! -d "$PROJECT_ROOT" ]; then
        log_error "Project root not found: $PROJECT_ROOT"
        exit 1
    fi

    if [ ! -d "$FORKS_DIR" ]; then
        log_error "Forks directory not found: $FORKS_DIR"
        exit 1
    fi

    # Check for required prompts
    for step in 1 2 3 4 5; do
        case $step in
            1) agent="ARCHITECT" ;;
            2) agent="BACKEND-DEV" ;;
            3) agent="DBA" ;;
            4) agent="API-DESIGNER" ;;
            5) agent="DEVOPS-ENGINEER" ;;
        esac

        prompt_file="${FORKS_DIR}/${agent}-PROMPT-Step${step}.md"
        if [ ! -f "$prompt_file" ]; then
            log_error "Missing prompt: $prompt_file"
            exit 1
        fi
    done

    log_success "Environment validated"
}

###############################################################################
# Step Execution Functions
###############################################################################

run_step() {
    local step=$1
    local agent=$2
    local agent_name=$3
    local prompt_file="${FORKS_DIR}/${agent_name}-PROMPT-Step${step}.md"

    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║                                                                ║"
    echo "║  STEP ${step}: ${agent_name}                                           "
    echo "║                                                                ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""

    log_info "Starting Step $step: $agent_name"
    log_info "Agent: $agent"
    log_info "Prompt: $prompt_file"
    log_info "Timestamp: $TIMESTAMP"

    echo ""
    echo "Running: claude --agent $agent --input-file $prompt_file"
    echo ""

    # Run the agent
    if claude --agent "$agent" --input-file "$prompt_file"; then
        log_success "Step $step ($agent_name) completed successfully"
        return 0
    else
        log_error "Step $step ($agent_name) failed"
        return 1
    fi
}

###############################################################################
# Step 1: Architect
###############################################################################

step_1_architect() {
    log_info "═══════════════════════════════════════════════════════════════"
    log_info "STEP 1: ARCHITECT — Design Briefing Domain Architecture"
    log_info "═══════════════════════════════════════════════════════════════"

    if run_step 1 "architect" "ARCHITECT"; then
        log_success "STEP 1 COMPLETE"
        log_info "Expected artifacts:"
        log_info "  ✓ docs/architecture/adr/ADR-002-briefing-domain.md"
        log_info "  ✓ .claude/plans/ARCHITECT-OUTPUT-Step1-Briefing.md"
        log_info "  ✓ Commit: feat(architect): adr-002-briefing-domain-architecture"
        return 0
    else
        log_error "STEP 1 FAILED"
        return 1
    fi
}

###############################################################################
# Step 2: Backend-Dev
###############################################################################

step_2_backend_dev() {
    log_info "═══════════════════════════════════════════════════════════════"
    log_info "STEP 2: BACKEND-DEV — Implement Domain Layer"
    log_info "═══════════════════════════════════════════════════════════════"

    if run_step 2 "backend-dev" "BACKEND-DEV"; then
        log_success "STEP 2 COMPLETE"
        log_info "Expected artifacts:"
        log_info "  ✓ src/main/java/com/scopeflow/core/domain/briefing/ (26+ files)"
        log_info "  ✓ src/test/java/com/scopeflow/core/domain/briefing/ (50+ tests)"
        log_info "  ✓ .claude/plans/BACKEND-DEV-OUTPUT-Step2-Briefing.md"
        log_info "  ✓ Commit: feat(backend-dev): implement-briefing-domain"
        return 0
    else
        log_error "STEP 2 FAILED"
        return 1
    fi
}

###############################################################################
# Step 3: DBA
###############################################################################

step_3_dba() {
    log_info "═══════════════════════════════════════════════════════════════"
    log_info "STEP 3: DBA — Create Database Schema"
    log_info "═══════════════════════════════════════════════════════════════"

    if run_step 3 "dba" "DBA"; then
        log_success "STEP 3 COMPLETE"
        log_info "Expected artifacts:"
        log_info "  ✓ backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql"
        log_info "  ✓ .claude/plans/DBA-OUTPUT-Step3-Briefing.md"
        log_info "  ✓ Commit: feat(dba): v3-briefing-domain-schema"
        return 0
    else
        log_error "STEP 3 FAILED"
        return 1
    fi
}

###############################################################################
# Step 4: API-Designer
###############################################################################

step_4_api_designer() {
    log_info "═══════════════════════════════════════════════════════════════"
    log_info "STEP 4: API-DESIGNER — Create REST Endpoints"
    log_info "═══════════════════════════════════════════════════════════════"

    if run_step 4 "api-designer" "API-DESIGNER"; then
        log_success "STEP 4 COMPLETE"
        log_info "Expected artifacts:"
        log_info "  ✓ BriefingControllerV1.java (5 admin endpoints)"
        log_info "  ✓ PublicBriefingControllerV1.java (3 public endpoints)"
        log_info "  ✓ BriefingExceptionHandler.java (error handling)"
        log_info "  ✓ .claude/plans/API-DESIGNER-OUTPUT-Step4-Briefing.md"
        log_info "  ✓ Commit: feat(api-designer): briefing-rest-controllers-openapi"
        return 0
    else
        log_error "STEP 4 FAILED"
        return 1
    fi
}

###############################################################################
# Step 5: DevOps-Engineer
###############################################################################

step_5_devops_engineer() {
    log_info "═══════════════════════════════════════════════════════════════"
    log_info "STEP 5: DEVOPS-ENGINEER — Production Deployment"
    log_info "═══════════════════════════════════════════════════════════════"

    if run_step 5 "devops-engineer" "DEVOPS-ENGINEER"; then
        log_success "STEP 5 COMPLETE"
        log_info "Expected artifacts:"
        log_info "  ✓ backend/Dockerfile.prod (multi-stage)"
        log_info "  ✓ k8s/helm/values-briefing.yaml (Kubernetes)"
        log_info "  ✓ .github/workflows/briefing-ci-cd.yml (CI/CD)"
        log_info "  ✓ .claude/plans/DEVOPS-ENGINEER-OUTPUT-Step5-Briefing.md"
        log_info "  ✓ Commit: feat(devops): briefing-docker-kubernetes-cicd"
        return 0
    else
        log_error "STEP 5 FAILED"
        return 1
    fi
}

###############################################################################
# Step 6: Consolidation
###############################################################################

step_6_consolidation() {
    log_info "═══════════════════════════════════════════════════════════════"
    log_info "STEP 6: CONSOLIDATION — Merge & Release"
    log_info "═══════════════════════════════════════════════════════════════"

    cd "$PROJECT_ROOT"

    log_info "Merging feature branch to main..."
    if git checkout main && git pull origin main; then
        if git merge --no-ff feature/sprint-1b-briefing-domain \
            -m "merge(sprint-1): briefing-domain from feature/sprint-1b-briefing-domain"; then
            log_success "Merge completed"
        else
            log_error "Merge conflict or failed"
            return 1
        fi
    else
        log_error "Failed to checkout main"
        return 1
    fi

    log_info "Running full test suite..."
    cd backend
    if ./mvnw clean test verify; then
        log_success "All tests passed"
    else
        log_error "Tests failed"
        return 1
    fi

    log_info "Running code quality checks..."
    if ./mvnw checkstyle:check; then
        log_success "Checkstyle passed"
    else
        log_error "Checkstyle violations found"
        return 1
    fi

    cd "$PROJECT_ROOT"

    log_info "Creating release tag..."
    if git tag -a v1.0.0-sprint1-briefing \
        -m "Release: Sprint 1 Terminal 2 (Briefing Domain)

Complete Briefing bounded context:
- Domain: sealed classes, value objects, services, events
- Database: V3 migration with 4 tables + 15+ indexes
- API: 2 controllers, 8+ REST endpoints, OpenAPI 3.1
- DevOps: Docker, Kubernetes, GitHub Actions CI/CD

Status: ✅ READY FOR INTEGRATION"; then
        log_success "Tag created: v1.0.0-sprint1-briefing"
        git push origin v1.0.0-sprint1-briefing
    else
        log_error "Failed to create tag"
        return 1
    fi

    log_success "STEP 6 COMPLETE"
    log_info "Expected artifacts:"
    log_info "  ✓ Main branch merged and pushed"
    log_info "  ✓ Tag v1.0.0-sprint1-briefing created"
    log_info "  ✓ All tests passing"
    log_info "  ✓ Checkstyle: 0 violations"
    log_info "  ✓ .claude/plans/CONSOLIDATION-SUMMARY-Terminal2.md"

    return 0
}

###############################################################################
# Help
###############################################################################

show_help() {
    cat << EOF
Terminal 2: Briefing Domain — Full Orchestration

Usage:
  ./run-terminal2.sh              # Run all steps (1-6) sequentially
  ./run-terminal2.sh 2            # Start from step 2 (if step 1 done)
  ./run-terminal2.sh 3            # Start from step 3
  ./run-terminal2.sh --help       # Show this help

Steps:
  1 = Architect (Design ADR-002)
  2 = Backend-Dev (Implement domain layer)
  3 = DBA (Create schema)
  4 = API-Designer (Create REST endpoints)
  5 = DevOps-Engineer (Docker + K8s + CI/CD)
  6 = Consolidation (Merge + test + release)

Timeline:
  Each step runs sequentially. Wait for completion before proceeding.
  Estimated total: ~1 week (parallel agents can reduce this)

Logging:
  All output logged to: ${LOG_FILE}

Requirements:
  - Claude CLI installed (claude command available)
  - Git configured
  - Maven 3.8+ (for backend tests)
  - All delegation prompts in: ${FORKS_DIR}

EOF
}

###############################################################################
# Main Flow
###############################################################################

main() {
    # Create log file
    mkdir -p "$(dirname "$LOG_FILE")"
    > "$LOG_FILE"  # Clear log

    log_info "╔════════════════════════════════════════════════════════════════╗"
    log_info "║  Terminal 2: Briefing Domain — Orchestration Started           ║"
    log_info "║  $(date '+%Y-%m-%d %H:%M:%S')                                       ║"
    log_info "╚════════════════════════════════════════════════════════════════╝"

    # Determine start step
    START_STEP=${1:-1}

    if [ "$START_STEP" = "--help" ] || [ "$START_STEP" = "-h" ]; then
        show_help
        exit 0
    fi

    if ! [[ "$START_STEP" =~ ^[1-6]$ ]]; then
        log_error "Invalid step: $START_STEP (must be 1-6)"
        show_help
        exit 1
    fi

    # Validate environment
    validate_environment

    # Run steps
    if [ "$START_STEP" -le 1 ]; then
        step_1_architect || exit 1
    fi

    if [ "$START_STEP" -le 2 ]; then
        step_2_backend_dev || exit 1
    fi

    if [ "$START_STEP" -le 3 ]; then
        step_3_dba || exit 1
    fi

    if [ "$START_STEP" -le 4 ]; then
        step_4_api_designer || exit 1
    fi

    if [ "$START_STEP" -le 5 ]; then
        step_5_devops_engineer || exit 1
    fi

    if [ "$START_STEP" -le 6 ]; then
        step_6_consolidation || exit 1
    fi

    # Success
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║                                                                ║"
    echo "║  ✅ TERMINAL 2 ORCHESTRATION COMPLETE!                        ║"
    echo "║                                                                ║"
    echo "║  Briefing Domain is ready for integration:                    ║"
    echo "║  → Tag: v1.0.0-sprint1-briefing                              ║"
    echo "║  → Branch: main (merged)                                      ║"
    echo "║  → Status: All tests passing ✅                              ║"
    echo "║                                                                ║"
    echo "║  Next: Terminal 3 (Proposal Domain) or consolidate Terminal 1 ║"
    echo "║                                                                ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""

    log_success "Terminal 2 orchestration completed successfully!"
    log_info "Log file: $LOG_FILE"
}

# Run main
main "$@"
