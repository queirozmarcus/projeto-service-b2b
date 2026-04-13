# ADR-001: Ordem de extracao dos bounded contexts

**Status:** Proposto
**Data:** 2026-04-05
**Contexto:**

O ScopeFlow AI e um monolito modular Spring Boot 3.2 + Java 21 com 4 bounded contexts
identificados: User, Workspace, Briefing e Proposal. A equipe deseja migrar para
microsservicos usando Strangler Fig pattern. Precisa-se definir a ordem de extracao
que minimize risco e maximize aprendizado incremental.

**Decisao:**

Extrair na seguinte ordem, um contexto por vez, com validacao em producao entre cada passo:

1. **User (Auth)** — zero dependencias de saida, valida infra de microsservicos
2. **Workspace** — depende apenas de User (ja extraido), resolve multi-tenancy
3. **Proposal** — depende de Workspace + referencia a BriefingSessionId (UUID opaco)
4. **Briefing** — ultimo, por ser o mais complexo (11 endpoints, IA async, state machine)

**Alternativas consideradas:**

1. *Briefing primeiro (maior valor de negocio)*: Descartado porque e o contexto mais
   complexo (11 endpoints, IA, async, public token flow). Risco alto para primeira extracao.
   Melhor validar a infra com um servico simples.

2. *Workspace primeiro (multi-tenancy e transversal)*: Descartado porque Workspace depende
   de User (importa UserId, WorkspaceController injeta UserService). Extrair Workspace sem
   ter User como servico separado criaria acoplamento bidirecional.

3. *Proposal primeiro (mais autocontido no dominio)*: Viavel, mas Proposal depende de
   Workspace e Briefing (2 FKs). Sem os servicos upstream extraidos, as dependencias
   ficariam todas apontando para o monolito.

**Trade-offs:**

| Ganha | Perde |
|-------|-------|
| Validacao incremental da infra com menor risco | Briefing (maior valor) fica por ultimo |
| Cada passo depende apenas de servicos ja extraidos | User service sozinho nao traz valor de negocio visivel ao cliente |
| Rollback trivial a cada passo (JWT stateless) | Overhead operacional cresce a cada servico extraido |

**Impacto:**

- Infra de microsservicos (service discovery, health checks, deployment) validada com o
  servico mais simples (User/Auth)
- WorkspaceController precisa ser refatorado ANTES da extracao 2 (substituir injecao direta
  de UserService por REST call)
- Service context profiles (V8 schema) precisam de decisao de ownership (ADR separado)
- JWT strategy precisa ser definida (shared secret vs JWKS endpoint) antes da extracao 1
