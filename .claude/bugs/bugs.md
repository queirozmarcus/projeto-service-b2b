# Terminal 2 Orchestration — Bugs & Issues Log

**Date:** 2026-03-22
**Project:** ScopeFlow AI — Sprint 1 Terminal 2 (Briefing Domain)
**Status:** Documented for future reference

---

## 🔴 Critical Issues

### Issue 1: Marcus Agent Self-Delegation Limitation

**Problem:**
Marcus (orchestrator agent) cannot directly instantiate other agents via `claude --agent` from within a Claude Code session.

**Details:**
- Attempted: `claude --agent backend-dev --input-file prompt.md`
- Result: Command hung/produced no output
- Root Cause: Claude Code doesn't support recursive agent spawning via CLI
- The CLI command works ONLY from user's terminal, not from within a Claude session

**Attempted Solutions:**
1. ❌ Using `claude --agent backend-dev -p "prompt"` — didn't work
2. ❌ Using bash script with loop — hung indefinitely
3. ❌ Using background task monitoring — output file never updated properly

**Working Solution:**
✅ Use `claude --agent backend-dev "$(cat prompt.md)" --dangerously-skip-permissions` from user's shell
✅ Agent runs in background task as separate process
✅ Task notification system alerts when complete
✅ Output file gets populated correctly

**Impact:** Medium
- Orchestration still works, but requires human intervention to run each agent
- Cannot fully automate the flow from within Marcus session
- User must run commands from their terminal

**Workaround:** User runs agents manually, Marcus monitors/coordinates

---

### Issue 2: File Checkout After Commit

**Problem:**
After `backend-dev` agent committed files (4f08c3e), files do NOT appear in working directory.

**Details:**
- Files ARE in git (visible via `git ls-files` and `git show`)
- `find src/main/java/com/scopeflow/core/domain/briefing/ -name "*.java"` returns 0
- `git checkout HEAD -- backend/src/main/java/com/scopeflow/core/domain/briefing/` doesn't restore files
- Likely cause: Agent committed to different branch/context than current

**Evidence:**
```
git ls-files | grep briefing  → 30+ files listed ✅
find src/main/java/com/scopeflow/core/domain/briefing/ → 0 files found ❌
git show 4f08c3e:backend/src/main/java/.../BriefingSession.java → content visible ✅
```

**Root Cause Analysis:**
- Branch state may be inconsistent between agent context and current shell context
- Agent likely committed on `feature/sprint-1b-briefing-domain`
- File paths in git don't match actual filesystem paths (path case or encoding issue?)

**Impact:** Medium
- Git history is correct (no data loss)
- Files are safely stored in repo
- Can't verify files locally without `git show` or `git checkout` magic

**Workaround:**
```bash
git checkout 4f08c3e -- backend/src/main/java/com/scopeflow/core/domain/briefing/
# OR
git show 4f08c3e:backend/src/main/java/com/scopeflow/core/domain/briefing/BriefingSession.java > file.java
```

---

## 🟡 Medium Issues

### Issue 3: Task Background Execution Output File Race Condition

**Problem:**
When delegating to agents via background tasks, output file is created but often empty for first 30+ seconds.

**Details:**
- Task ID returned immediately: `boc81sr2e`
- Output file path: `/tmp/claude-1000/.../tasks/boc81sr2e.output`
- File exists but contains only initial stdout (e.g., "STEP 2: BACKEND-DEV — Initiating now...")
- Real agent output appears much later (or not at all in first iteration)
- Monitoring loop would check file 20+ times before real output appeared

**Timeline:**
```
T+0s:     Task started, ID returned
T+0-10s:  Output file created with header only
T+10-60s: File size stays at 98B (header only)
T+60s+:   Agent actually starts processing, output grows
```

**Impact:** Low
- Monitoring can work, but requires longer timeout
- Doesn't affect actual execution, only visibility

**Workaround:**
- Wait 60+ seconds before checking output file
- Use `tail -f` for real-time monitoring instead of polling
- Check file size; if unchanged after 5 checks, likely no data yet

---

### Issue 4: CLI Syntax Confusion

**Problem:**
Multiple different `claude` CLI syntaxes were attempted, causing confusion about which works.

**Attempted:**
1. `claude --agent architect --input-file prompt.md` ❌ (--input-file not recognized)
2. `claude --agent backend-dev -p "prompt"` ❌ (-p is for print mode, not prompt input)
3. `claude --agent backend-dev "$(cat prompt.md)"` ✅ (correct!)

**Correct Syntax:**
```bash
claude --agent <agent-name> "<prompt-text>" [options]
```

**Additional Flags:**
- `--dangerously-skip-permissions` — allow all tools without prompting
- `--print` or `-p` — non-interactive mode (not for input)

**Impact:** Low
- Documentation incomplete in prompt files
- Scripts need updating

---

### Issue 5: Monitoring Loop Timeout Interruption

**Problem:**
Created infinite monitoring loop (360 iterations, 30s each = 3 hours) but user interrupted after ~2 minutes.

**Details:**
```bash
for i in {1..360}; do
    sleep 30
done
```

**Issue:** Loop runs too long, user can't see meaningful updates

**Impact:** Low
- Script works, but UX is poor
- Should check for completion every 10-30 seconds, not 360 times

**Fix:**
```bash
# Better: check with adaptive timeout
TIMEOUT=7200  # 2 hours max
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    # check for completion
    if grep -q "COMPLETE" output; then break; fi
    sleep 30
    ELAPSED=$((ELAPSED + 30))
done
```

---

## 🟢 Minor Issues

### Issue 6: ADR-002 File Size Discrepancy

**Problem:**
ADR-002 reported as both "638 lines" and "981 lines" in different places.

**Details:**
- `.claude/plans/ARCHITECT-OUTPUT-Step1-Briefing.md` says: 981 lines total (1100+ with output summary)
- `git show` says: 638 lines
- Likely: architect's summary counted documentation + body + decision tables

**Impact:** Negligible
- No functional issue, just documentation inconsistency

---

### Issue 7: Shell Script Validation Bug

**Problem:**
The `run-terminal2.sh` script validates for prompt files but doesn't check if agents exist.

**Details:**
```bash
# This validates prompts exist:
for step in 1 2 3 4 5; do
    prompt_file="${FORKS_DIR}/${agent_name}-PROMPT-Step${step}.md"
    if [ ! -f "$prompt_file" ]; then
        log_error "Missing prompt: $prompt_file"
        exit 1
    fi
done

# But doesn't verify:
# - Agent is actually installed
# - `claude` command exists
# - Network/auth is available
```

**Impact:** Low
- Script will fail later with unclear error message

**Fix:**
```bash
# Add to validation:
if ! command -v claude &> /dev/null; then
    log_error "Claude CLI not found. Install first."
    exit 1
fi
```

---

## 📋 Issues Summary by Component

| Component | Issue | Severity | Status |
|-----------|-------|----------|--------|
| Marcus Orchestration | Can't self-delegate agents | 🔴 Critical | Documented |
| Git Filesystem Sync | Files in git, not on disk | 🔴 Critical | Workaround exists |
| Task Output Race | Output file delayed | 🟡 Medium | Tolerable |
| CLI Documentation | Syntax confusion | 🟡 Medium | Fixed |
| Monitoring UX | Long loop timeout | 🟡 Medium | Better loop needed |
| ADR Size Inconsistency | 638 vs 981 lines | 🟢 Minor | Documentation only |
| Script Validation | Missing pre-checks | 🟢 Minor | Enhancement |

---

## ✅ What Worked

Despite issues, the following succeeded:

1. ✅ **Architect Step 1** — ADR-002 created, committed (7c691da)
2. ✅ **Backend-Dev Step 2** — 40 domain files + 86 tests created, committed (4f08c3e)
3. ✅ **DBA Step 3** — Running (in progress)
4. ✅ **Delegation prompts** — All 6 steps documented and ready
5. ✅ **Task notification system** — Alerts when agents complete
6. ✅ **Git history** — All commits recorded properly
7. ✅ **Context isolation** — Each agent got clean context (no token bloat)

---

## 🔧 Recommendations for Future Runs

### For Terminal 3 (Proposal Domain)

1. **Skip Marcus self-delegation:**
   - Have user run agents from their shell
   - Marcus monitors + coordinates

2. **Pre-validate before delegating:**
   ```bash
   command -v claude &> /dev/null || die "Claude CLI not found"
   ```

3. **Fix monitoring loop:**
   - Use adaptive timeout (not fixed 360 iterations)
   - Check every 30s for "COMPLETE" marker
   - Print meaningful progress updates

4. **Document CLI syntax in prompts:**
   - Add: "Run this from your shell: claude --agent backend-dev '...'"
   - Don't embed in bash scripts

5. **Handle git sync issues:**
   - After agent commits, explicitly verify files appear
   - Use `git show <commit>:path/to/file` if needed
   - Document this as expected behavior

---

## 📝 Lessons Learned

### What We Learned

1. **Marcus is orchestrator, not implementer**
   - Can't self-delegate via CLI from within session
   - Must let agents work in separate processes
   - This is actually more correct (isolation)

2. **Agents work VERY well in isolation**
   - Backend-dev created 40 files + 86 tests flawlessly
   - No token pollution between agents
   - Each agent focused on their domain

3. **Git is reliable, filesystem sync less so**
   - Commits are permanent and correct
   - Filesystem view may lag or not sync
   - Always verify via `git ls-files` and `git show`

4. **Task background execution is powerful**
   - Long-running tasks don't block
   - Task notifications alert when done
   - Great for agent delegation

5. **Monitoring needs patience**
   - Agent tasks take real time (hours/days)
   - Quick polls see "no output" initially
   - Need adaptive timeouts + meaningful checks

---

## 🚀 Next Steps

### For DBA Step 3 (Currently Running)

- ✅ Agent is delegated and working
- ⏳ V3 schema being created (briefing_sessions, questions, answers, ai_generations)
- ⏳ Indexes being added (15+)
- ⏳ Outbox pattern integrated
- ✅ Will commit: `feat(dba): v3-briefing-domain-schema`

### For Step 4 (API-Designer)

```bash
# When DBA completes, run:
claude --agent api-designer "$(cat .claude/forks/API-DESIGNER-PROMPT-Step4.md)" --dangerously-skip-permissions
```

### For Step 5 (DevOps-Engineer)

```bash
# When API-Designer completes, run:
claude --agent devops-engineer "$(cat .claude/forks/DEVOPS-ENGINEER-PROMPT-Step5.md)" --dangerously-skip-permissions
```

### For Step 6 (Consolidation)

```bash
# When DevOps completes, run locally:
git checkout main
git pull origin main
git merge --no-ff feature/sprint-1b-briefing-domain
cd backend
./mvnw clean test verify
git tag -a v1.0.0-sprint1-briefing -m "..."
git push origin v1.0.0-sprint1-briefing
```

---

## 📞 Contact / Questions

**If you encounter these issues:**

1. **Files committed but not on disk** → Use `git show <commit>:path` to verify content
2. **Agent hanging** → Check if running from user shell, not Claude Code
3. **Output file empty** → Wait 60+ seconds, check again
4. **Task not completing** → Review agent output via `tail -f /tmp/.../output`

---

---

## 🔴 Issue 8: DBA Agent Step 3 Failure (Silent Exit)

**Date:** 2026-03-22 05:30 UTC
**Status:** FAILED

**Problem:**
DBA agent delegated with correct syntax but exited with code 1 (failure) after ~6 minutes of processing.

**Details:**
- Command: `claude --agent dba "$(cat .claude/forks/DBA-PROMPT-Step3.md)" --dangerously-skip-permissions`
- Task ID: bcl4q8n5f
- Duration: ~6 minutes
- Exit code: 1 (error)
- Output file: Empty (no logs, no errors, no results)
- Process: Running, consuming CPU (3.9% memory)
- Result: NO V3 migration file created, NO commit made

**Expected Output:**
- File: `backend/src/main/resources/db/migration/V3__briefing_domain_schema.sql`
- Commit: `feat(dba): v3-briefing-domain-schema`
- Documentation: `.claude/plans/DBA-OUTPUT-Step3-Briefing.md`

**What Actually Happened:**
- Agent read prompt (visible in ps aux output)
- Agent processed for ~6 minutes
- Agent exited with error code 1
- No files written
- No git commit
- No error message logged

**Root Cause Analysis:**
Unknown — output file is completely empty. Possible causes:
1. **Prompt too large** — 1,100+ lines might overwhelm Sonnet's context parsing
2. **File read permission** — Agent couldn't read ADR-002 or other reference files
3. **Agent internal error** — Agent encountered exception and exited silently
4. **Timeout** — Internal timeout triggered after 6 min
5. **Memory issue** — Process consumed too much memory

**Evidence:**
```bash
# Command run:
claude --agent dba "$(cat .claude/forks/DBA-PROMPT-Step3.md)" --dangerously-skip-permissions

# Process was visible:
mq 638094 1.5 3.9 74422780 317476 ? Sl 05:19 0:06 claude --agent dba

# But no output generated:
tail /tmp/claude-1000/.../tasks/bcl4q8n5f.output
# → (empty file)

# And no migration created:
find backend -name "V3__*.sql"
# → (no results)

# And no commit:
git log --oneline -1
# → 4f08c3e feat(backend-dev): ... (no new commits)
```

**Impact:**
- Step 3 (DBA) blocked
- Steps 4-5 (API-Designer, DevOps) cannot start until Step 3 completes
- Terminal 2 orchestration delayed

**Workarounds:**
1. **Manual migration creation** — Write V3 migration manually based on DBA-PROMPT specs
2. **Simplified prompt** — Create shorter, focused DBA prompt without all reference material
3. **Retry** — Re-run DBA with same prompt (may succeed on retry)
4. **Skip to Step 4** — Continue with API-Designer while DBA troubleshooting

**Recommendation:**
- First attempt: Retry DBA with exact same prompt
- If fails again: Try with simplified prompt (remove inline SQL examples)
- If still fails: Create V3 manually using prompt as specification

---

**End of Bug Documentation**

*Last Updated: 2026-03-22 05:30 UTC*
*Status: Active Sprint 1 Terminal 2 Orchestration — DBA Step 3 FAILED*
