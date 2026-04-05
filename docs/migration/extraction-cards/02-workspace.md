# Extração: Workspace

**Prioridade:** 2 (segundo a extrair)
**Risco:** médio
**Acoplamento:** médio (depende de User; é dependido por Briefing e Proposal via `workspace_id`)

> **Impacto do ADR-004:** `service_context_profiles` e `service_context_questions` pertencem
> ao contexto Workspace (não ao Briefing). O Workspace service passa a ter 4 tabelas e a
> expor endpoints de templates de perguntas consumidos pelo Briefing service (card 04).

> **Pré-requisito de observabilidade:** ADR-003 determina que distributed tracing é
> obrigatório a partir do 2º serviço extraído. Este é o 2º. OpenTelemetry deve estar
> configurado antes da Fase 1.

---

## Dependências de entrada

Quem consome este contexto:

- **BriefingControllerV1** — usa `workspaceId` do JWT para filtrar briefings
- **BriefingSessionControllerV2** — usa `workspaceId` do JWT para autorização e carrega
  templates de perguntas via `JpaServiceContextProfileSpringRepository` (após ADR-004:
  deve chamar Workspace service via REST)
- **ProposalControllerV2** — usa `workspaceId` do JWT para filtrar proposals
- **ApprovalControllerV2** — usa `workspaceId` do JWT para autorização
- **JwtAuthenticationFilter** — popula `workspaceId` no `ScopeFlowPrincipal`

## Dependências de saída

O que este contexto chama:

- **User context** — `WorkspaceControllerV2.inviteMember()` injeta `UserService` e
  `PasswordEncoder` diretamente:
  - `UserService.getUserByEmail(email)` — buscar usuário pelo e-mail do convite
  - `UserService.saveInvitedUser(...)` — criar usuário convidado com senha temporária
  - `PasswordEncoder.encode(...)` — hash da senha temporária do usuário convidado
- **UserId** — value object importado em 8 classes do contexto Workspace

> ⚠️ **Pré-requisito crítico — G-02.1:** Refatorar `WorkspaceControllerV2.inviteMember()`
> para REST call ao User service ANTES de iniciar a extração. Dois acoplamentos precisam
> ser eliminados:
> 1. Injeção direta de `UserService` → substituir por `UserServiceClient` (REST)
> 2. Injeção de `PasswordEncoder` → remover completamente do Workspace service.
>    O User service passa a fazer o hash da senha ao processar `POST /users/invited`.
>    O Workspace service apenas envia e-mail e display name; o User service encapsula
>    a criação do usuário convidado com sua própria política de senha temporária.

---

## Dados

### Tabelas que pertencem a este contexto (ADR-004)

| Tabela | Aggregate | Observações |
|--------|-----------|-------------|
| `workspaces` | Workspace (root) | Referenciada por todos os outros contextos via `workspace_id` |
| `workspace_members` | WorkspaceMember | Relação M:N entre Workspace e User |
| `service_context_profiles` | ServiceContextProfile | Perfil de IA por tipo de serviço por workspace — ownership transferido do Briefing pelo ADR-004 |
| `service_context_questions` | ServiceContextQuestion | Templates de perguntas ligados a um perfil — ownership transferido do Briefing pelo ADR-004 |

### Tabelas compartilhadas e estratégia

- `activity_logs` — **G-02.3:** FK `user_id → users.id` (SET NULL on delete). A tabela
  é escrita tanto por User quanto por Workspace. Permanece no shared DB durante toda a
  fase de shared DB. O Workspace service continua escrevendo nela diretamente nesta fase;
  a separação de schemas (se ocorrer) será tratada como tarefa futura independente.
- `outbox_event` — compartilhada. Filtrar por
  `aggregate_type IN ('Workspace', 'WorkspaceMember', 'ServiceContextProfile')`.

### FKs saindo deste contexto (dependências de dados)

| FK | Para | Ação | Estratégia |
|----|------|------|------------|
| `workspaces.owner_id` | `users.id` | RESTRICT | Manter na fase shared DB; remover quando separar DBs |
| `workspace_members.user_id` | `users.id` | CASCADE | Manter na fase shared DB; remover quando separar DBs |
| `workspace_members.workspace_id` | `workspaces.id` | CASCADE | Intra-context — não alterar |
| `service_context_questions.service_context_profile_id` | `service_context_profiles.id` | CASCADE | Intra-context — não alterar |

### FKs apontando PARA workspaces (consumidores)

| FK | De | Estratégia |
|----|----|------------|
| `briefing_sessions.workspace_id` | Briefing | Manter — multi-tenancy esperada |
| `service_context_profiles.workspace_id` | Workspace (intra) | Intra-context após ADR-004 |
| `proposals.workspace_id` | Proposal | Manter — multi-tenancy esperada |

**Estratégia:** Manter todas as FKs intactas durante a fase shared DB. `workspace_id` é
multi-tenancy cross-cutting — não representa acoplamento de domínio problemático.

---

## Estratégia de roteamento

### Pré-requisito: Refatorar invite flow (ANTES de extrair)

**G-02.1 — Eliminar `PasswordEncoder` e `UserService` do Workspace service:**

```java
// Estado atual — dois acoplamentos ao contexto User
@Autowired private UserService userService;       // cross-context coupling
@Autowired private PasswordEncoder passwordEncoder; // deve pertencer ao User service

public ResponseEntity<?> inviteMember(...) {
    User user = userService.getUserByEmail(email)
        .orElseGet(() -> {
            String tempHash = passwordEncoder.encode(UUID.randomUUID().toString());
            UserInactive newUser = User.createInvited(newUserId, email, tempHash, displayName);
            userService.saveInvitedUser(newUser);
            return newUser;
        });
    // ...
}

// Estado alvo — REST call; User service é owner do hash de senha
@Autowired private UserServiceClient userServiceClient; // Feign/WebClient

public ResponseEntity<?> inviteMember(...) {
    UserResponse user = userServiceClient.findByEmail(email)
        .orElseGet(() -> userServiceClient.createInvited(
            new CreateInvitedUserRequest(email, extractDisplayNameFromEmail(email))
            // Sem senha — User service gera o hash internamente
        ));
    workspaceService.inviteMember(wsId, new UserId(user.userId()), request.role());
    // ...
}
```

O `PasswordEncoder` é removido do `WorkspaceControllerV2` e de qualquer outro ponto do
Workspace service. A `SecurityConfig` do Workspace service não precisa declarar o bean
`PasswordEncoder`.

### G-02.4 — Contrato do UserServiceClient

O `UserServiceClient` deve chamar exatamente os dois endpoints inter-service definidos no
card 01 (User service):

| Método | Path | Tipo | Comportamento |
|--------|------|------|---------------|
| GET | `/users/by-email/{email}` | interno (service token) | Retorna 200 com usuário ou 404 se não existir |
| POST | `/users/invited` | interno (service token) | Cria usuário INACTIVE; User service gera hash da senha; retorna 201 com `userId` |

**Autenticação:** header `X-Service-Token` com secret compartilhado (consistente com a
decisão do card 01).

**Fallback quando User service indisponível (Resilience4j):**

```java
@CircuitBreaker(name = "user-service", fallbackMethod = "inviteMemberFallback")
@Retry(name = "user-service")
public UserResponse findByEmail(String email) { ... }

private UserResponse inviteMemberFallback(String email, Throwable ex) {
    // O invite falha com 503 Service Unavailable (não silencioso)
    // O operador deve investigar — não é aceitável criar membros com dados inconsistentes
    throw new ServiceUnavailableException(
        "USER-SERVICE-DOWN",
        "User service unavailable. Invite cannot be processed at this time."
    );
}
```

O fallback para o invite é falha explícita (não silenciosa). Não é aceitável criar
`WorkspaceMember` sem um `UserId` válido, pois a FK `workspace_members.user_id → users.id`
ficaria inconsistente.

### G-02.2 — Exposição de dados do Workspace para inter-service

O JWT contém `workspaceId` mas não metadados do workspace (nome, niche, settings). Quando
Briefing ou Proposal precisarem de dados do workspace além do ID, devem chamar o Workspace
service via REST.

**Endpoint inter-service:**

| Método | Path | Tipo | Consumidores |
|--------|------|------|--------------|
| GET | `/workspaces/{id}` | interno (service token) | Briefing service, Proposal service |

**Cache nos consumidores:** Redis com TTL 1h. Dados do workspace mudam raramente; invalidação
proativa não é necessária na fase inicial — TTL é suficiente.

**Autenticação:** service token (header `X-Service-Token`), consistente com card 01.

**Nota:** `workspaceId` do JWT é suficiente para multi-tenancy (filtro de dados). A chamada
REST só é necessária quando o serviço consumidor precisa de metadados (ex: nome do workspace
para PDF de proposal, niche para personalização de IA no Briefing).

### G-02.5 — OpenTelemetry (pré-requisito obrigatório)

ADR-003 determina que distributed tracing é obrigatório a partir do 2º serviço extraído.
O Workspace service deve ser instrumentado com OpenTelemetry antes de entrar em produção:

- **Auto-instrumentação:** `opentelemetry-javaagent.jar` via `JAVA_TOOL_OPTIONS`
- **Propagação de contexto:** W3C Trace Context (`traceparent` header) entre serviços
- **Exportador:** OTLP para o coletor central (mesmo configurado no User service)
- **Spans customizados:** nas operações de invite e nas chamadas ao User service

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0   # staging; reduzir para 0.1 em prod
otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT}
```

### Fase 1: Proxy transparente (Strangler Fig)

1. Criar Workspace service como aplicação Spring Boot separada
2. API Gateway roteia `/workspaces/**` para o novo serviço
3. Monólito mantém os mesmos endpoints como fallback
4. Feature flag controla % de tráfego roteado para o novo serviço

### Fase 2: Corte completo

1. 100% do tráfego em `/workspaces/**` vai para Workspace service
2. Remover `WorkspaceControllerV2` e classes relacionadas do monólito
3. Outros serviços acessam dados do workspace via:
   - JWT claim `workspaceId` (sem chamada REST — latência zero para multi-tenancy)
   - `GET /workspaces/{id}` com cache Redis TTL 1h (para metadados)
4. Briefing service passa a chamar `GET /workspaces/{id}/service-context-profiles` e
   `GET /workspaces/{id}/service-context-profiles/{profileId}/questions` em vez de
   acessar `JpaServiceContextProfileSpringRepository` diretamente

---

## Endpoints a migrar

### Endpoints existentes (7)

| Método | Path | Tipo | Observações |
|--------|------|------|-------------|
| POST | `/workspaces` | autenticado (JWT) | Criar workspace |
| GET | `/workspaces/{workspaceId}` | autenticado (JWT) + interno (service token) | Buscar workspace por ID — dupla auth |
| GET | `/workspaces` | autenticado (JWT) | Listar workspaces do usuário |
| POST | `/workspaces/{workspaceId}/members/invite` | autenticado (JWT) | Convidar membro — requer `UserServiceClient` |
| PUT | `/workspaces/{workspaceId}/members/{memberId}/role` | autenticado (JWT) | Alterar role do membro |
| DELETE | `/workspaces/{workspaceId}/members/{memberId}` | autenticado (JWT) | Remover membro |
| GET | `/workspaces/{workspaceId}/members` | autenticado (JWT) | Listar membros |

### Endpoints novos — templates de perguntas (ADR-004)

| Método | Path | Tipo | Observações |
|--------|------|------|-------------|
| GET | `/workspaces/{workspaceId}/service-context-profiles` | autenticado (JWT) + interno (service token) | Listar perfis de contexto ativos do workspace |
| GET | `/workspaces/{workspaceId}/service-context-profiles/{profileId}/questions` | autenticado (JWT) + interno (service token) | Listar perguntas de um perfil — consumido pelo Briefing service |

**Nota sobre dupla autenticação:** `GET /workspaces/{id}` e os endpoints de templates
devem aceitar tanto JWT de usuário (chamadas do frontend) quanto service token
(chamadas inter-service). A `SecurityConfig` deve ter matcher separado para cada tipo.

---

## Classes a migrar

### Domain (portar sem alteração de comportamento)

- `Workspace`, `WorkspaceActive`, `WorkspaceSuspended` (sealed classes)
- `WorkspaceMember`, `MemberActive`, `MemberInvited`, `MemberLeft` (sealed classes)
- `WorkspaceId`, `WorkspaceName` (value objects)
- `Role` (enum)
- `WorkspaceRepository`, `WorkspaceMemberRepository` (ports out — interfaces)
- `WorkspaceService` (domain service)
- `WorkspaceNotFoundException`, `MemberNotFoundException`, `MemberAlreadyExistsException`,
  `CannotRemoveLastOwnerException`, `WorkspaceNameAlreadyExistsException` (domain exceptions)
- `WorkspaceMemberInvited` (domain event)
- **Novos (ADR-004):** `ServiceContextProfile`, `ServiceContextQuestion` (domain model —
  atualmente existem apenas como entidades JPA no pacote `briefing`; criar classes de
  domínio puras no Workspace context)
- **Novo (ADR-004):** `ServiceContextProfileRepository` (port out — interface)

### Adapter in/web

- `WorkspaceControllerV2` + DTOs (`workspace/dto/*`) — refatorar invite flow antes de portar
- **Novos (ADR-004):** `ServiceContextProfileControllerV1` + DTOs
  (`ServiceContextProfileResponse`, `ServiceContextQuestionResponse`)

### Adapter out/persistence

- `JpaWorkspace`, `JpaWorkspaceMember` (JPA entities)
- `JpaWorkspaceSpringRepository`, `JpaWorkspaceMemberSpringRepository` (Spring Data)
- `JpaWorkspaceRepositoryAdapter`, `JpaWorkspaceMemberRepositoryAdapter` (adapters)
- **Portar do pacote briefing (ADR-004):** `JpaServiceContextProfile`,
  `JpaServiceContextQuestion`, `JpaServiceContextProfileSpringRepository`,
  `JpaServiceContextQuestionSpringRepository`
- **Novo (ADR-004):** `JpaServiceContextProfileRepositoryAdapter` (implementa
  `ServiceContextProfileRepository`)

### Adapter out/http (novo)

- `UserServiceClient` — REST client (Feign ou `RestClient`) para User service
  - `GET /users/by-email/{email}` — buscar usuário por e-mail
  - `POST /users/invited` — criar usuário convidado (User service faz o hash da senha)
  - Resilience4j: circuit breaker + retry + timeout configurados
  - Autenticação: header `X-Service-Token`

### Infraestrutura (duplicar do monólito ou do User service)

- `SecurityConfig` — dupla autenticação: JWT de usuário + service token por path
- `JwtService`, `JwtAuthenticationFilter`, `ScopeFlowPrincipal` (stateless — duplicar)
- `GlobalExceptionHandler` (RFC 9457 — duplicar)
- `OpenTelemetryConfig` (obrigatório — ver G-02.5)

---

## Critérios de sucesso

- [ ] Todos os 7 endpoints existentes de `/workspaces/**` respondem identicamente (body + status codes)
- [ ] Endpoints novos de templates (`/service-context-profiles/**`) respondem corretamente
- [ ] Invite flow funciona via REST call ao User service (sem injeção de `UserService` ou `PasswordEncoder`)
- [ ] `POST /users/invited` chamado pelo `UserServiceClient` — User service encapsula o hash da senha
- [ ] Fallback do invite retorna 503 explícito quando User service indisponível (não silencioso)
- [ ] `GET /workspaces/{id}` aceita JWT de usuário e service token
- [ ] JWT tokens gerados pelo User service são aceitos pelo Workspace service (shared secret)
- [ ] Testes de integração do monólito continuam passando (fallback ativo)
- [ ] Latência p99 dos endpoints de workspace < 200ms (baseline do monólito)
- [ ] Health check do Workspace service em `/actuator/health`
- [ ] Zero downtime durante a migração (proxy switch)
- [ ] Traces do OpenTelemetry visíveis no coletor central para todas as operações
- [ ] Contract tests (Pact) entre Workspace e User service passando no CI
- [ ] Card 04 (Briefing) atualizado para consumir templates via REST (desbloqueador)

## Critérios de rollback

**Quando reverter:**

- Latência p99 > 500ms por mais de 5 minutos em qualquer endpoint
- Taxa de erro > 1% em `/workspaces/**`
- Falha na validação de `workspace_id` por outros serviços (Briefing, Proposal)
- Invite flow falhando (User service indisponível sem circuit breaker ativo)
- Briefing service sem acesso aos templates de perguntas (Workspace service indisponível)

**Como reverter:**

1. Feature flag: rotear 100% do tráfego de volta para o monólito
2. Monólito já tem os endpoints funcionando (não foram removidos)
3. Tempo de rollback estimado: < 1 minuto (switch de proxy)
4. Nenhuma mudança de schema necessária para rollback

---

## Pré-requisitos (antes de começar)

### Bloqueadores diretos

- [ ] User service (card 01) extraído e estável em produção
- [ ] Endpoints inter-service do User service disponíveis e acessíveis:
  - `GET /users/by-email/{email}` (protegido por service token)
  - `POST /users/invited` (protegido por service token; encapsula hash da senha)
- [ ] **Refatorar `WorkspaceControllerV2.inviteMember()`** para REST call ao User service,
      eliminando `UserService` e `PasswordEncoder` do Workspace service (G-02.1)

### Infra e observabilidade

- [ ] OpenTelemetry configurado no Workspace service (G-02.5 — ADR-003 obrigatório)
- [ ] Coletor OTLP disponível (mesmo do User service)
- [ ] `UserServiceClient` implementado com Resilience4j (circuit breaker + retry + timeout)
- [ ] Pipeline de CI/CD para o novo serviço (build, test, deploy)
- [ ] Health check e monitoring configurados
- [ ] Alertas de latência e taxa de erro configurados antes de ativar feature flag

### Testes e contratos

- [ ] Contract tests (Pact) entre Workspace e User service (pré-deploy):
  - Workspace como consumer: `GET /users/by-email/{email}` e `POST /users/invited`
  - User service como provider: validar que o contrato é atendido
- [ ] WireMock ou `@MockBean` no `UserServiceClient` para isolar testes de integração
      do Workspace service dos testes de integração do monólito
- [ ] Testes de integração para os novos endpoints de templates (ADR-004)

### Impacto em outros cards

- [ ] **Card 04 (Briefing):** após este card, `BriefingSessionService` deve ser atualizado
      para remover dependência direta nos repositórios JPA de `service_context_profiles` e
      `service_context_questions`, substituindo por chamadas REST ao Workspace service via
      `WorkspaceServiceClient`. Registrar como tarefa de acompanhamento no card 04.
