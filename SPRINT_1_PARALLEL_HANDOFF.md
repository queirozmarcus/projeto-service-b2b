# Sprint 1 Parallel Orchestration — Handoff Document

**Data:** 2026-03-22
**Modo:** Full Orchestration Parallel (3 Terminals Simultâneos)
**Timeline:** ~2 semanas
**Status:** ✅ PRONTO PARA INICIAR

---

## 🎯 Visão Geral

Sprint 1 vai implementar o **domain layer completo** do ScopeFlow com 3 bounded contexts trabalhando em paralelo:

1. **User & Workspace** (Foundation — sem dependência)
2. **Briefing** (Depende de User — inicia ~80% após Term 1)
3. **Proposal** (Depende de Briefing — inicia ~80% após Term 2)

---

## 📋 Instruções de Execução

### Terminal 1: User & Workspace Domain (INICIE AGORA)

```bash
cd /home/mq/iGitHub/projeto-service-b2b
/dev-bootstrap user-workspace-domain
```

**O que o architect vai fazer:**
- Design sealed classes: User, Workspace, WorkspaceMember
- Value objects: Email, PasswordHash, UserId, WorkspaceId
- Invariants: Uma workspace = 1 OWNER obrigatório
- Domain events: UserRegistered, WorkspaceMemberInvited
- Services: UserService, WorkspaceService

**O que vai entregar:**
- 9+ sealed classes + value objects
- 2x domain services (UserService, WorkspaceService)
- 2x repository interfaces
- 40+ unit tests (100% domain coverage)
- 15+ integration tests (Testcontainers)

**Arquivos produzidos:**
```
src/main/java/com/scopeflow/core/domain/user/
  ├── User.java (sealed class)
  ├── UserId.java (record)
  ├── Email.java (record)
  ├── PasswordHash.java (record)
  ├── UserService.java
  └── UserRepository.java (interface)

src/main/java/com/scopeflow/core/domain/workspace/
  ├── Workspace.java (sealed class)
  ├── WorkspaceMember.java (sealed class)
  ├── Role.java (enum)
  ├── WorkspaceId.java (record)
  ├── WorkspaceService.java
  ├── WorkspaceRepository.java (interface)
  └── WorkspaceMemberRepository.java (interface)

src/test/java/com/scopeflow/core/domain/user/
  ├── UserTest.java (40+ tests)
  └── UserRepositoryIntegrationTest.java (15+ tests)

src/test/java/com/scopeflow/core/domain/workspace/
  ├── WorkspaceTest.java
  └── WorkspaceRepositoryIntegrationTest.java
```

**Estimado:** 5-7 dias
**Sinal de conclusão:** "✅ User & Workspace Domain COMPLETE"

---

### Terminal 2: Briefing Domain (INICIE QUANDO TERM 1 ~80% PRONTO)

```bash
cd /home/mq/iGitHub/projeto-service-b2b
/dev-bootstrap briefing-domain
```

**O que o architect vai fazer:**
- Design sealed classes: BriefingSession, BriefingQuestion, BriefingAnswer
- State machine: InProgress → Completed / Abandoned
- Value objects: BriefingProgress, AIGeneration, CompletionScore
- Invariants: Perguntas sequenciais, não pode pular
- Domain events: BriefingSessionStarted, QuestionAsked, AnswerSubmitted
- Services: BriefingService

**O que vai entregar:**
- 7+ sealed classes + value objects
- 1x domain service (BriefingService)
- 3x repository interfaces
- 35+ unit tests (100% domain coverage)
- 15+ integration tests (Testcontainers)

**Arquivos produzidos:**
```
src/main/java/com/scopeflow/core/domain/briefing/
  ├── BriefingSession.java (sealed class)
  ├── BriefingQuestion.java (record)
  ├── BriefingAnswer.java (sealed class)
  ├── BriefingProgress.java (record)
  ├── AIGeneration.java (record)
  ├── CompletionScore.java (record)
  ├── BriefingService.java
  ├── BriefingSessionRepository.java (interface)
  ├── BriefingQuestionRepository.java (interface)
  └── BriefingAnswerRepository.java (interface)

src/test/java/com/scopeflow/core/domain/briefing/
  ├── BriefingSessionTest.java (35+ tests)
  └── BriefingSessionRepositoryIntegrationTest.java (15+ tests)
```

**Estimado:** 5-7 dias
**Sinal de conclusão:** "✅ Briefing Domain COMPLETE"

---

### Terminal 3: Proposal Domain (INICIE QUANDO TERM 2 ~80% PRONTO)

```bash
cd /home/mq/iGitHub/projeto-service-b2b
/dev-bootstrap proposal-domain
```

**O que o architect vai fazer:**
- Design sealed classes: Proposal, ApprovalWorkflow, Approval
- Immutable records: ProposalVersion (snapshots), ProposalScope, Deliverable
- Value objects: Timeline, ProposalPrice, ApprovalToken
- Invariants: Não pode aprovar se não está PUBLISHED
- Domain events: ProposalCreated, ProposalPublished, ApprovalWorkflowInitiated
- Services: ProposalService

**O que vai entregar:**
- 11+ sealed classes + value objects
- 1x domain service (ProposalService)
- 4x repository interfaces
- 40+ unit tests (100% domain coverage)
- 15+ integration tests (Testcontainers)

**Arquivos produzidos:**
```
src/main/java/com/scopeflow/core/domain/proposal/
  ├── Proposal.java (sealed class)
  ├── ProposalVersion.java (immutable record)
  ├── ProposalScope.java (record)
  ├── Deliverable.java (record)
  ├── Timeline.java (record)
  ├── ProposalPrice.java (record)
  ├── ApprovalWorkflow.java (sealed class)
  ├── Approval.java (record)
  ├── KickoffSummary.java (record)
  ├── ApprovalToken.java (record)
  ├── ProposalService.java
  ├── ProposalRepository.java (interface)
  ├── ProposalVersionRepository.java (interface)
  ├── ApprovalWorkflowRepository.java (interface)
  └── ApprovalRepository.java (interface)

src/test/java/com/scopeflow/core/domain/proposal/
  ├── ProposalTest.java (40+ tests)
  └── ProposalRepositoryIntegrationTest.java (15+ tests)
```

**Estimado:** 5-7 dias
**Sinal de conclusão:** "✅ Proposal Domain COMPLETE"

---

## 📊 Monitoramento de Progresso

### Para monitorar Term 1 (User & Workspace):
```bash
# Em outro terminal
ls -la backend/src/main/java/com/scopeflow/core/domain/user/
ls -la backend/src/main/java/com/scopeflow/core/domain/workspace/
```

### Para monitorar Term 2 (Briefing):
```bash
# Em outro terminal
ls -la backend/src/main/java/com/scopeflow/core/domain/briefing/
```

### Para monitorar Term 3 (Proposal):
```bash
# Em outro terminal
ls -la backend/src/main/java/com/scopeflow/core/domain/proposal/
```

### Verificar testes
```bash
# Em cada terminal, após conclusão
cd backend
./mvnw clean verify -DskipIntegrationTests  # Unit tests rápido
./mvnw clean verify                          # Full test suite (lento)
```

---

## ✅ Checklist Final (Quando todos 3 estiverem COMPLETOS)

- [ ] Terminal 1: User & Workspace COMPLETE (git logs mostram commits)
- [ ] Terminal 2: Briefing COMPLETE (git logs mostram commits)
- [ ] Terminal 3: Proposal COMPLETE (git logs mostram commits)
- [ ] Todos os testes passing: `./mvnw clean verify`
- [ ] Code review: `/dev-review src/main/java/com/scopeflow/core/domain/`
- [ ] Nenhum merge conflict (context isolation deve evitar)

---

## 🔄 Consolidação Final (Após todas as 3 estarem COMPLETE)

```bash
# 1. Merge das 3 branches
git merge origin/feature/sprint-1a-user-workspace
git merge origin/feature/sprint-1b-briefing
git merge origin/feature/sprint-1c-proposal

# 2. Rodar suite completa de testes
cd backend
./mvnw clean verify

# 3. Code review final
/dev-review src/main/java/com/scopeflow/core/domain/

# 4. Merge para develop
git checkout develop
git merge feature/sprint-1-domain-layer

# 5. Tag release
git tag -a v1.0.0-sprint1 \
  -m "Sprint 1: Domain Layer Complete

- 15+ sealed classes (type-safe entities)
- 30+ records (immutable value objects)
- 8+ domain services
- 12+ repository interfaces
- 115+ unit tests (100% coverage)
- 45+ integration tests
- 3 bounded contexts fully tested"
```

---

## 📈 Métricas Esperadas ao Final

| Métrica | Target | Expected |
|---------|--------|----------|
| Sealed Classes | 15+ | 15-18 |
| Records (Value Objects) | 30+ | 32-35 |
| Domain Services | 8+ | 8-10 |
| Repository Interfaces | 12+ | 12-14 |
| Unit Tests | 115+ | 120-130 |
| Integration Tests | 45+ | 45-50 |
| Domain Coverage | 100% | ✅ 100% |
| Service Coverage | 80%+ | ✅ 85%+ |
| Mutation Score | 75%+ | ✅ 80%+ |

---

## 🎯 Próximos Passos Após Sprint 1

**Sprint 2 (Adapter Layer):**
- JPA entity mappings (Domain ↔ Adapter converters)
- Spring Data JPA repository implementations
- Query optimization + N+1 detection
- Flyway migration updates if needed

**Sprint 3 (Application Services):**
- Use cases (CreateUserUseCase, RegisterUserUseCase, etc.)
- Transaction management
- Event publishing + handling
- Error handling strategy

**Sprint 4 (Message Queue):**
- RabbitMQ listeners
- Async job processing (PDF, IA, email)
- Dead letter queue + retry logic

---

## 🚨 Troubleshooting

**Se Terminal 1 falhar:**
- Check: `./mvnw clean compile` (syntax errors?)
- Check: `./mvnw test` (domain tests failing?)
- Read agentes logs: `/dev-bootstrap user-workspace-domain` output
- Não avance para Terminal 2 até resolver

**Se Terminal 2 falhar (depende de User):**
- Verify: User sealed class está disponível em domain/user/
- Check: UserRepository interface criado
- Check: User domain events publicados
- Sync com output de Terminal 1

**Se Terminal 3 falhar (depende de Briefing):**
- Verify: BriefingSession sealed class está disponível
- Check: BriefingSessionRepository criado
- Check: Briefing domain events publicados
- Sync com output de Terminal 2

**Se merge conflicts:**
- Unlikely com context isolation (3 pastas diferentes)
- Se ocorrerem: communicate between terminals antes de merge

---

## 📞 Suporte

- **Documentação:** `.claude/plans/sprint-1-domain-layer-full.md`
- **Bootstrap Summary:** `BOOTSTRAP_SUMMARY.md`
- **Project Guide:** `CLAUDE.md`
- **Plano de Execução:** Este arquivo

---

**Status:** ✅ PRONTO PARA INICIAR
**Início:** Agora (Terminal 1)
**Timeline:** ~2 semanas (paralelo)
**Target:** v1.0.0-sprint1 release

🚀 **Vamos lá!**
