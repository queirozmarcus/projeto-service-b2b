# Terminal 2 — Execution Commands (Ready to Run)

**Date:** 2026-03-22
**Branch:** `feature/sprint-1b-briefing-domain` (already created)
**Status:** All delegation prompts ready ✅

---

## 📋 Quick Reference: Run These Commands in Order

```bash
# Current directory: /home/mq/iGitHub/projeto-service-b2b

# STEP 1: ARCHITECT (just delegated)
# ✅ Already running, or will be when you run this:
# claude --agent architect --input-file .claude/forks/ARCHITECT-PROMPT-Step1.md

# Wait for Step 1 to complete, then:

# STEP 2: BACKEND-DEV
claude --agent backend-dev --input-file .claude/forks/BACKEND-DEV-PROMPT-Step2.md

# Wait for Step 2 to complete, then:

# STEP 3: DBA
claude --agent dba --input-file .claude/forks/DBA-PROMPT-Step3.md

# Wait for Step 3 to complete, then:

# STEP 4: API-DESIGNER
claude --agent api-designer --input-file .claude/forks/API-DESIGNER-PROMPT-Step4.md

# Wait for Step 4 to complete, then:

# STEP 5: DEVOPS-ENGINEER
claude --agent devops-engineer --input-file .claude/forks/DEVOPS-ENGINEER-PROMPT-Step5.md

# Wait for Step 5 to complete, then:

# STEP 6: CONSOLIDATION
# No agent delegation — YOU run these commands:
git checkout main
git pull origin main
git merge --no-ff feature/sprint-1b-briefing-domain
cd backend
./mvnw clean test verify
./mvnw checkstyle:check
./mvnw jacoco:report
/qa-security
/dev-review src/main/java/com/scopeflow/core/domain/briefing/
git tag -a v1.0.0-sprint1-briefing -m "Sprint 1 Terminal 2: Briefing Domain Complete"
git push origin v1.0.0-sprint1-briefing
```

---

## 📁 Files Created (All Delegation Prompts Ready)

```
.claude/forks/
├── ARCHITECT-PROMPT-Step1.md ✅
├── BACKEND-DEV-PROMPT-Step2.md ✅
├── DBA-PROMPT-Step3.md ✅
├── API-DESIGNER-PROMPT-Step4.md ✅
├── DEVOPS-ENGINEER-PROMPT-Step5.md ✅
├── CONSOLIDATION-PROMPT-Step6.md ✅
└── TERMINAL2-EXECUTION-COMMANDS.md (this file)
```

---

## 📊 Expected Artifacts by Step

### After STEP 1 (Architect) ✅
```
docs/architecture/adr/
└── ADR-002-briefing-domain.md (981 lines)

.claude/plans/
└── ARCHITECT-OUTPUT-Step1-Briefing.md
```
**Files:** 2 | **Commits:** 1

---

### After STEP 2 (Backend-Dev) ⏳
```
src/main/java/com/scopeflow/core/domain/briefing/
├── BriefingSession.java (sealed)
├── BriefingInProgress.java
├── BriefingCompleted.java
├── BriefingAbandoned.java
├── BriefingAnswer.java (sealed)
├── AnsweredDirect.java
├── AnsweredWithFollowup.java
├── BriefingQuestion.java
├── BriefingSessionId.java (record)
├── QuestionId.java (record)
├── AnswerId.java (record)
├── AnswerText.java (record)
├── PublicToken.java (record)
├── CompletionScore.java (record)
├── BriefingProgress.java (record)
├── AIGeneration.java (record)
├── [8+ more value objects]
├── BriefingService.java
├── [5 exception classes]
├── DomainEvent.java (sealed interface)
├── [6 domain event records]
├── [4 repository interfaces]
└── package-info.java

src/test/java/com/scopeflow/core/domain/briefing/
├── BriefingSessionTest.java
├── BriefingAnswerTest.java
├── ValueObjectTests.java (8+ classes)
├── BriefingServiceTest.java
└── [50+ total tests]

.claude/plans/
└── BACKEND-DEV-OUTPUT-Step2-Briefing.md
```
**Files:** 26+ | **Tests:** 50+ | **Lines:** 2,000+ | **Commits:** 1

---

### After STEP 3 (DBA) ⏳
```
backend/src/main/resources/db/migration/
└── V3__briefing_domain_schema.sql (600-800 lines)

.claude/plans/
└── DBA-OUTPUT-Step3-Briefing.md
```
**Files:** 1 | **Tables:** 4 | **Indexes:** 15+ | **Commits:** 1

---

### After STEP 4 (API-Designer) ⏳
```
backend/src/main/java/com/scopeflow/adapter/in/web/
├── BriefingControllerV1.java (300 lines, 5 endpoints)
├── PublicBriefingControllerV1.java (250 lines, 3 endpoints)
└── BriefingExceptionHandler.java (150 lines)

[DTOs as nested records in controllers or separate file]

.claude/plans/
└── API-DESIGNER-OUTPUT-Step4-Briefing.md
```
**Files:** 3+ | **Endpoints:** 8+ | **Commits:** 1

---

### After STEP 5 (DevOps) ⏳
```
backend/
└── Dockerfile.prod (updated with briefing considerations)

k8s/helm/
├── values-briefing.yaml (new override file)
└── Chart.yaml (updated version to 1.0.0-sprint1)

.github/workflows/
└── briefing-ci-cd.yml (new workflow)

.claude/plans/
└── DEVOPS-ENGINEER-OUTPUT-Step5-Briefing.md
```
**Files:** 3 | **Commits:** 1

---

### After STEP 6 (Consolidation) ⏳
```
Git:
├── Merged: feature/sprint-1b-briefing-domain → main
├── Tagged: v1.0.0-sprint1-briefing
└── Develop branch updated

Test Results:
├── Unit tests: 50+ passing ✅
├── Integration tests: 15+ passing ✅
├── Coverage: 85%+ (domain) ✅
├── Checkstyle: 0 violations ✅
└── Security: 0 critical ✅

.claude/plans/
└── CONSOLIDATION-SUMMARY-Terminal2.md
```

---

## ⏱️ Timeline Estimate

| Step | Agent | Duration | Est. Complete |
|------|-------|----------|----------------|
| 1 | architect | ~1 day | 2026-03-23 |
| 2 | backend-dev | 2-3 days | 2026-03-25 |
| 3 | dba | 1 day | 2026-03-26 |
| 4 | api-designer | 1-2 days | 2026-03-27 |
| 5 | devops-engineer | 1 day | 2026-03-28 |
| 6 | consolidation | 1-2 days | 2026-03-29 |
| **TOTAL** | **PARALLEL** | **~1 week** | **2026-03-29** |

---

## 🚀 Next Step After Terminal 2

Once Terminal 2 consolidation is complete:

1. **Terminal 1 Consolidation** (if not already done)
   - Merge feature/sprint-1a-user-workspace-domain
   - Tag: v1.0.0-sprint1-user-workspace

2. **Terminal 3 Orchestration** (Proposal Domain)
   - Create TERMINAL3-PROPOSAL-HANDOFF.md
   - Delegate to agents: Architect → Backend-Dev → DBA → API-Designer → DevOps → Consolidation

3. **Sprint 1 Final Release**
   - Consolidate all 3 terminals
   - Merge all migrations (V1, V2, V3)
   - Full test suite (200+ tests)
   - Tag: v1.0.0-sprint1
   - Release notes

---

## 📝 How to Read Each Prompt

Each agent receives a file like `.claude/forks/[AGENT]-PROMPT-Step[N].md`.

The prompt contains:
- **Mission:** What the agent builds
- **Input:** Files to read + context needed
- **Output:** Exact deliverables + file paths
- **Constraints:** Technical rules (no Spring in domain, immutability, etc.)
- **Checklist:** Success criteria
- **Git workflow:** Exactly how to commit

**Each prompt is self-contained** — agent doesn't need to read anything else.

---

## 🎯 Monitoring Execution

After delegating each step, monitor:

```bash
# Check git status/commits after each agent finishes
git log --oneline -10

# Verify files were created
ls -la src/main/java/com/scopeflow/core/domain/briefing/

# Check test count after backend-dev
find src/test/java/com/scopeflow/core/domain/briefing/ -name "*Test.java" | wc -l

# Verify migration syntax
cat backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql

# Compile to check for errors
cd backend && ./mvnw compile

# Verify API endpoints
grep -r "@PostMapping\|@GetMapping\|@PutMapping" src/main/java/com/scopeflow/adapter/in/web/Briefing*.java
```

---

## ⚠️ Troubleshooting

### If backend-dev Step 2 fails:
- Check: Did architect Step 1 complete? (ADR-002 exists?)
- Check: Are there compilation errors? (`./mvnw compile`)
- Action: Review ADR-002 for clarifications, delegate again

### If DBA Step 3 fails:
- Check: PostgreSQL running locally? (`docker-compose up -d`)
- Check: V3 migration SQL syntax valid? (paste into psql directly)
- Action: Verify Flyway conventions (V{n}__{description}.sql)

### If API-Designer Step 4 fails:
- Check: Are domain classes available? (did Step 2 complete?)
- Check: Do controllers compile? (`./mvnw compile`)
- Action: Verify @RestController, @RequestMapping are present

### If DevOps Step 5 fails:
- Check: Does Docker build work? (`docker build -f backend/Dockerfile.prod .`)
- Check: Helm syntax valid? (`helm lint k8s/helm -f k8s/helm/values-briefing.yaml`)
- Action: Review Terminal 1 DevOps output as reference

---

## 📞 Communication with Agents

Each agent will:
1. Read their delegation prompt
2. Understand the exact deliverables
3. Implement + test locally
4. Create summary file (.claude/plans/[AGENT]-OUTPUT-Step[N]-Briefing.md)
5. Commit + push to origin

**You** should:
1. Run the delegation command
2. Wait for completion
3. Verify files were created
4. Move to next step

---

## ✅ Final Checklist (Before Release)

```bash
# After STEP 6 consolidation, verify:
[ ] git log shows all 5 commits merged
[ ] v1.0.0-sprint1-briefing tag exists
[ ] main branch has all new files
[ ] ./mvnw clean test passes (all tests)
[ ] ./mvnw checkstyle:check passes (no violations)
[ ] /qa-security passed (no critical issues)
[ ] /dev-review approved
[ ] Documentation complete
[ ] Ready for Terminal 3 orchestration

# Check:
git tag | grep v1.0.0-sprint1-briefing
git log --oneline origin/main | head -10
find src/main/java/com/scopeflow/core/domain/briefing/ -name "*.java" | wc -l
```

---

## 🎉 Success Looks Like

When Terminal 2 is complete:

```
✅ Briefing Domain Architecture (ADR-002)
✅ 26+ Java domain classes (sealed, records)
✅ 50+ unit tests (all passing)
✅ Database schema with Outbox pattern
✅ REST API (2 controllers, 8 endpoints)
✅ Docker + Kubernetes + CI/CD ready
✅ All commits on main branch
✅ v1.0.0-sprint1-briefing tagged

READY FOR:
→ Terminal 1 consolidation (if not done)
→ Terminal 3 orchestration (Proposal domain)
→ Sprint 1 final release (v1.0.0-sprint1)
```

---

**You're in control.** Run the commands when ready.** 🚀
