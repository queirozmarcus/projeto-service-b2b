---
Plano: Code Review — Feature Geração de Scope via IA (Sprints 1–7)
Data: 2026-04-23
Status: BACKLOG
Pack: Dev + QA
---

## Contexto

Code review completo da feature de geração de scope via IA implementada nos Sprints 1–7 do ScopeFlow AI.

**Stack:** Java 21 · Spring Boot 3.2 · Arquitetura Hexagonal · Next.js 15 · TypeScript

**Critérios obrigatórios:**
- Arquitetura Hexagonal (zero deps de framework no domain)
- RFC 9457 Problem Details em todos os erros
- Testcontainers (sem H2)
- Multi-tenancy por `workspace_id` em todas as queries
- Flyway V12 não toca migrations V1–V11
- TypeScript strict mode

**Output final por sprint:** issues categorizados por severidade
- 🔴 **Critical** — viola contrato arquitetural, segurança, ou corretude
- 🟡 **Warning** — violação de convenção, risco latente, má prática
- 🟢 **Suggestion** — melhoria de qualidade, legibilidade, testabilidade

---

## Sprints

### Sprint 1 — UseCase: GenerateScopeAIUseCase.java
**Agent:** `code-reviewer` + `architect`
**Arquivo:** `backend/src/main/java/com/scopeflow/application/GenerateScopeAIUseCase.java`
**Foco:**
- Dependências: zero Spring/JPA no application layer?
- Orquestração: chama Port (interface), não Adapter diretamente?
- Regra de negócio: está no domain ou vazou pro use case?
- Tratamento de erro: lança domain exceptions corretas?
- Multi-tenancy: `workspace_id` validado antes de chamar Port?

---

### Sprint 2 — Port: ScopeGenerationPort.java
**Agent:** `architect`
**Arquivo:** `backend/src/main/java/com/scopeflow/application/port/out/ScopeGenerationPort.java`
**Foco:**
- Está em `application/port/out/`? (não em `adapter/`)
- Interface pura sem dependências de framework
- Contrato bem definido: parâmetros tipados, retorno expressivo
- Exceções declaradas no contrato?
- Nome do método expressa intenção de negócio (não técnica)?

---

### Sprint 3 — Adapter: StubScopeGenerationAdapter.java
**Agent:** `code-reviewer` + `architect`
**Arquivo:** `backend/src/main/java/com/scopeflow/adapter/out/ai/StubScopeGenerationAdapter.java`
**Foco:**
- Está em `adapter/out/ai/`? (caminho correto para IA)
- Implementa `ScopeGenerationPort`? (não outra interface)
- `@Component` ou `@Service` correto
- Stub retorna dados realistas para testes manuais?
- TODOs documentados para implementação real (OpenAI/etc)?
- Circuit breaker preparado (`@CircuitBreaker`) ou ao menos TODO?

---

### Sprint 4 — Controller: ProposalControllerV2.java
**Agent:** `code-reviewer`
**Arquivo:** `backend/src/main/java/com/scopeflow/adapter/in/web/proposal/ProposalControllerV2.java`
**Foco:**
- RFC 9457: todos os erros retornam `ProblemDetail`?
- Multi-tenancy: `workspace_id` extraído do JWT e passado ao use case?
- Endpoints RESTful: verbos corretos, paths em kebab-case?
- Idempotency-Key nos endpoints de geração (POST)?
- `@PreAuthorize` ou equivalente protegendo os endpoints?
- DTOs: records imutáveis? Sem lógica de negócio?
- Resposta 202 Accepted para operação assíncrona de IA?

---

### Sprint 5 — Exceptions: ScopeGenerationException, BriefingIncompleteException, BriefingNotReadyException
**Agent:** `code-reviewer`
**Arquivos:** exceptions do domínio de scope/briefing
**Foco:**
- Herdam de `BriefingDomainException` ou exception base correta?
- Código estável no formato `SCOPE-NNN` ou `BRIEFING-NNN`?
- Mensagem expressa contexto (inclui ID do recurso)?
- `GlobalExceptionHandler` mapeado para HTTP correto (400, 422, 409)?
- RFC 9457: `type`, `title`, `status`, `errorCode`, `errorId`, `timestamp`?

---

### Sprint 6 — Migration: V12__add_scope_generation_type.sql
**Agent:** `dba`
**Arquivo:** `backend/src/main/resources/db/migration/V12__add_scope_generation_type.sql`
**Foco:**
- V12 não altera tabelas de V1–V11?
- Checksum não vai conflitar (arquivo nunca modificado após criado)?
- DDL seguro: `ADD COLUMN ... DEFAULT` sem lock longo?
- Rollback documentado (comentário com DROP equivalente)?
- Índice criado junto se coluna usada em queries de filtro?
- Enum ou VARCHAR para `scope_generation_type`? Trade-offs?

---

### Sprint 7 — Testes Unitários: GenerateScopeAIUseCaseTest.java
**Agent:** `unit-test-engineer` + `code-reviewer`
**Arquivo:** `backend/src/test/java/com/scopeflow/.../GenerateScopeAIUseCaseTest.java`
**Foco:**
- Given-When-Then estruturado?
- Port mockado (não o Adapter diretamente)?
- Cenários cobertos: happy path, briefing incompleto, briefing não pronto, falha no Port?
- Asserções expressivas com AssertJ?
- Nenhuma dependência de Spring (`@ExtendWith(MockitoExtension.class)` apenas)?
- Mutation score: lógica de negócio 100% coberta?

---

### Sprint 8 — Testes de Integração: GenerateScopeAIIntegrationTest.java
**Agent:** `integration-test-engineer` + `code-reviewer`
**Arquivo:** `backend/src/test/java/com/scopeflow/.../GenerateScopeAIIntegrationTest.java`
**Foco:**
- Testcontainers: PostgreSQL real (sem H2)?
- `@SpringBootTest` com contexto completo ou slice?
- Flyway aplicando V12 no Testcontainer corretamente?
- Multi-tenancy testado: workspace_id isolado entre tenants?
- Stub adapter ativado por profile de teste?
- Idempotency testada (chamada duplicada retorna mesmo resultado)?
- Tempo de execução aceitável (< 60s)?

---

### Sprint 9 — Frontend API: frontend/src/lib/proposalApi.ts
**Agent:** `code-reviewer`
**Arquivo:** `frontend/src/lib/proposalApi.ts`
**Foco:**
- TypeScript strict: sem `any`, sem `!` (non-null assertion) desnecessário?
- Tipos de resposta alinhados com contratos do backend (RFC 9457)?
- Tratamento de erro: captura `ProblemDetail` e expõe `errorCode`?
- Função de geração de scope: headers corretos (`Idempotency-Key`)?
- Retry ou feedback para operações longas de IA?
- Separação: funções puras de fetch vs. lógica de estado?

---

### Sprint 10 — Frontend Page + Consolidação: [id]/page.tsx
**Agent:** `code-reviewer`
**Arquivo:** `frontend/src/app/dashboard/proposals/[id]/page.tsx`
**Foco:**
- TypeScript strict: props tipadas, sem inferência implícita `any`?
- Loading state durante geração de scope (pode demorar)?
- Error boundary ou tratamento de erro visível ao usuário?
- Server Component vs. Client Component: decisão correta?
- Polling ou WebSocket para resultado assíncrono de IA?
- Acessibilidade: botão de geração com `aria-busy` durante loading?

**Consolidação final:**
- Agregar todos os issues das Sprints 1–9
- Categorizar: 🔴 Critical / 🟡 Warning / 🟢 Suggestion
- Priorizar por impacto: Arquitetura > Segurança > Qualidade > Estilo
- Criar plano de ação com estimativa por issue

---

## Execução

Cada sprint é executada individualmente via Marcus:

```
Sprint 1: /dev-review backend/.../GenerateScopeAIUseCase.java
Sprint 2: /dev-review backend/.../port/out/ScopeGenerationPort.java
...
Sprint 10: /dev-review frontend/.../[id]/page.tsx → consolidar relatório
```

**Estimativa:** ~15–20 min por sprint · Total: ~3h para review completo
