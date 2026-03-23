# Terminal 2 — Briefing Domain Orchestration

**Complete automation script for Terminal 2 (Briefing Domain) execution.**

---

## 🚀 Quick Start

```bash
cd /home/mq/iGitHub/projeto-service-b2b

# Run ALL steps (1-6) sequentially
./.claude/forks/run-terminal2.sh

# Or start from a specific step
./.claude/forks/run-terminal2.sh 3    # Start from DBA (if steps 1-2 already done)
```

---

## 📋 What This Script Does

The script **sequences all 6 steps** with isolated agent forks:

```
┌─────────────────────────────────────────────────────────────┐
│ Step 1: Architect                                           │
│ └─ Agent: architect (fork isolated)                         │
│    Input: TERMINAL2-BRIEFING-HANDOFF.md                     │
│    Output: ADR-002, architecture decisions                  │
│    Commit: feat(architect): adr-002-briefing-domain         │
│                                                              │
│ Step 2: Backend-Dev (waits for Step 1 ✓)                   │
│ └─ Agent: backend-dev (fork isolated)                       │
│    Input: ADR-002 (from architect)                          │
│    Output: 26+ Java files, 50+ tests                        │
│    Commit: feat(backend-dev): implement-briefing-domain     │
│                                                              │
│ Step 3: DBA (waits for Step 2 ✓)                           │
│ └─ Agent: dba (fork isolated)                               │
│    Input: Domain entities (from backend-dev)                │
│    Output: V3__briefing_domain_schema.sql                   │
│    Commit: feat(dba): v3-briefing-domain-schema             │
│                                                              │
│ Step 4: API-Designer (waits for Step 3 ✓)                  │
│ └─ Agent: api-designer (fork isolated)                      │
│    Input: Domain + Schema                                   │
│    Output: 2 controllers, 8+ endpoints, OpenAPI             │
│    Commit: feat(api-designer): briefing-rest-controllers    │
│                                                              │
│ Step 5: DevOps-Engineer (waits for Step 4 ✓)               │
│ └─ Agent: devops-engineer (fork isolated)                   │
│    Input: All previous steps                                │
│    Output: Docker, Kubernetes, CI/CD                        │
│    Commit: feat(devops): briefing-docker-kubernetes-cicd    │
│                                                              │
│ Step 6: Consolidation (waits for Step 5 ✓)                │
│ └─ Marcus (you, local terminal)                             │
│    Merge → Test → Tag → Release                             │
│    Tag: v1.0.0-sprint1-briefing                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📖 Detailed Usage

### Run All Steps (Default)

```bash
./.claude/forks/run-terminal2.sh
```

**What happens:**
1. Architect runs → creates ADR-002 → commits
2. Script waits for architect to finish ⏳
3. Backend-Dev runs → creates 26 Java files → commits
4. Script waits for backend-dev to finish ⏳
5. DBA runs → creates V3 migration → commits
6. Script waits for DBA to finish ⏳
7. API-Designer runs → creates controllers → commits
8. Script waits for API-Designer to finish ⏳
9. DevOps-Engineer runs → creates Docker/K8s/CI-CD → commits
10. Script waits for DevOps to finish ⏳
11. Consolidation (local): merge → test → tag → release

**Total time:** ~1 week (sequential execution)

---

### Run From Specific Step

If you need to resume from a specific step (because one failed or you paused):

```bash
# Start from Step 2 (if architect already done)
./.claude/forks/run-terminal2.sh 2

# Start from Step 4 (if steps 1-3 done)
./.claude/forks/run-terminal2.sh 4

# Start consolidation (if all steps 1-5 done)
./.claude/forks/run-terminal2.sh 6
```

---

### View Help

```bash
./.claude/forks/run-terminal2.sh --help
```

---

## 📊 File Structure

```
.claude/forks/
├── run-terminal2.sh                 ← Run this script
├── ARCHITECT-PROMPT-Step1.md
├── BACKEND-DEV-PROMPT-Step2.md
├── DBA-PROMPT-Step3.md
├── API-DESIGNER-PROMPT-Step4.md
├── DEVOPS-ENGINEER-PROMPT-Step5.md
├── CONSOLIDATION-PROMPT-Step6.md
├── TERMINAL2-EXECUTION-COMMANDS.md  (reference)
└── README.md                         ← This file

.claude/plans/
├── TERMINAL2-BRIEFING-HANDOFF.md    (spec)
└── [Agent outputs created during execution]

.claude/
└── terminal2-execution.log          (created by script)
```

---

## 🔍 Monitoring Execution

### While Script Runs

The script outputs progress to console **and** logs to file:

```bash
# Watch logs in real-time
tail -f .claude/terminal2-execution.log

# Or at end of execution:
cat .claude/terminal2-execution.log
```

---

### Check Agent Output

After each agent completes, verify artifacts:

```bash
# After Step 1 (Architect)
ls -lah docs/architecture/adr/ADR-002-briefing-domain.md
cat .claude/plans/ARCHITECT-OUTPUT-Step1-Briefing.md

# After Step 2 (Backend-Dev)
find src/main/java/com/scopeflow/core/domain/briefing/ -name "*.java" | wc -l
find src/test/java/com/scopeflow/core/domain/briefing/ -name "*Test.java" | wc -l

# After Step 3 (DBA)
cat backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql

# After Step 4 (API-Designer)
grep -r "@PostMapping\|@GetMapping" src/main/java/com/scopeflow/adapter/in/web/Briefing*.java

# After Step 5 (DevOps)
cat backend/Dockerfile.prod | head -20
helm lint k8s/helm -f k8s/helm/values-briefing.yaml
```

---

## 🚦 Expected Behavior

### Each Step Flow

```
1. Script outputs: "STEP N: [AGENT] — [Description]"
2. Script runs: claude --agent [agent] --input-file [prompt]
3. Agent works in isolated fork context
4. Agent creates files + commits to git
5. Script continues to next step (or exits if failed)
```

### Timing

- **Step 1 (Architect):** ~1 day
- **Step 2 (Backend-Dev):** ~2-3 days
- **Step 3 (DBA):** ~1 day
- **Step 4 (API-Designer):** ~1-2 days
- **Step 5 (DevOps):** ~1 day
- **Step 6 (Consolidation):** ~1-2 days

**Total:** ~7-8 days (sequential) or ~3-4 days (if agents work in parallel)

---

## ❌ If Something Fails

### Script Exits

If any step fails, the script **stops and shows error**:

```
[ERROR] Step 2 (BACKEND-DEV) failed
```

**What to do:**

1. **Check the log:**
   ```bash
   tail -100 .claude/terminal2-execution.log
   ```

2. **Understand the error** (usually agent couldn't complete task)

3. **Fix manually** (if needed) or **re-run agent:**
   ```bash
   claude --agent backend-dev --input-file .claude/forks/BACKEND-DEV-PROMPT-Step2.md
   ```

4. **Resume script from that step:**
   ```bash
   ./.claude/forks/run-terminal2.sh 2
   ```

---

## 📝 Log File

Script logs everything to:

```
.claude/terminal2-execution.log
```

**Format:**
```
[INFO] Step X starting...
[SUCCESS] Step X completed
[ERROR] Step X failed
[WARNING] Issue detected but continuing
```

---

## 🎉 Success

When script completes successfully:

```
╔════════════════════════════════════════════════════════════════╗
║                                                                ║
║  ✅ TERMINAL 2 ORCHESTRATION COMPLETE!                        ║
║                                                                ║
║  Briefing Domain is ready for integration:                    ║
║  → Tag: v1.0.0-sprint1-briefing                              ║
║  → Branch: main (merged)                                      ║
║  → Status: All tests passing ✅                              ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
```

**You get:**
- ✅ 26+ Java domain files (sealed classes, records, services)
- ✅ 50+ unit tests (all passing)
- ✅ Database schema (V3 migration, 4 tables, 20+ indexes)
- ✅ REST API (2 controllers, 8 endpoints)
- ✅ Docker + Kubernetes + CI/CD ready
- ✅ Tagged: v1.0.0-sprint1-briefing
- ✅ All code on main branch

---

## 🔗 What's Happening Behind the Scenes

Each agent runs in **fork isolated context:**

1. **Architect fork:**
   - Reads: TERMINAL2-BRIEFING-HANDOFF.md
   - Thinks: "I'm the architect. Design the domain."
   - Outputs: ADR-002, sealed class hierarchy
   - Commits: "feat(architect): adr-002-..."
   - Clean context (no token bloat from previous steps)

2. **Backend-Dev fork:**
   - Reads: ADR-002 (from architect) + BACKEND-DEV-PROMPT-Step2.md
   - Thinks: "I'm backend-dev. Implement these sealed classes."
   - Outputs: 26 Java files, 50 tests
   - Commits: "feat(backend-dev): implement-..."
   - Clean context (fresh, focused)

3. **And so on...**

**Result:** Each agent is expert at their job, no context pollution, high quality output.

---

## 🎯 Next Steps After Terminal 2

Once script completes:

1. **Terminal 1 Consolidation** (if not done)
   - Run similar orchestration for User & Workspace domain

2. **Terminal 3 (Proposal Domain)**
   - Create Terminal 3 handoff document
   - Create similar orchestration script

3. **Sprint 1 Final Release**
   - Consolidate all 3 terminals
   - Full test suite (200+ tests)
   - Tag: v1.0.0-sprint1

---

## 📞 Support

If script fails or you have questions:

1. Check log: `cat .claude/terminal2-execution.log`
2. Re-run specific agent manually
3. Resume script from that step

---

**Ready? Run this:**

```bash
cd /home/mq/iGitHub/projeto-service-b2b
./.claude/forks/run-terminal2.sh
```

**Let the orchestration begin! 🚀**
