# Extracao: User (Auth)

**Prioridade:** 1 (primeiro a extrair)
**Risco:** baixo
**Acoplamento:** baixo (zero dependencias de saida)

> **Nota:** Este e o primeiro servico extraido. Alem de portar o contexto User/Auth, esta
> extracao estabelece a infra base de microsservicos — API Gateway e estrategia de testes
> durante transicao — da qual os cards 02, 03 e 04 dependem.

## Dependencias de entrada
Quem consome este contexto:
- **WorkspaceService** — usa `UserId` como value object
- **WorkspaceControllerV2** — injeta `UserService` diretamente (invite flow); apos extracao,
  chama User service via REST usando os endpoints inter-service abaixo
- **JwtAuthenticationFilter** — valida token e extrai userId
- **SecurityUtil** — extrai userId/workspaceId do SecurityContext
- **BriefingControllerV1** — usa userId do JWT para workspace validation
- **ProposalControllerV2** — usa userId do JWT para authorization

## Dependencias de saida
O que este contexto chama:
- **Nenhuma.** UserService depende apenas de UserRepository (mesmo contexto).

## Dados

### Tabelas que pertencem a este contexto
- `users` (id, email, password_hash, full_name, phone, status, version, timestamps)

### Tabelas compartilhadas e estrategia
- `activity_logs` — FK para `users.id` (SET NULL on delete). Manter no shared DB.
- `outbox` — compartilhada. Filtrar por `aggregate_type = 'User'` para eventos deste contexto.

### FKs apontando PARA users (consumidores)
- `workspaces.owner_id` -> `users.id` (RESTRICT)
- `workspace_members.user_id` -> `users.id` (CASCADE)
- `activity_logs.user_id` -> `users.id` (SET NULL)

**Estrategia:** Manter FKs intactas durante shared DB phase. Nao alterar schema.

## Estrategia de roteamento

### Fase 1: Proxy transparente (Strangler Fig)
1. Criar User service como aplicacao Spring Boot separada
2. API Gateway configurado (ver pre-requisito abaixo) roteia `/auth/**` e `/users/**`
   para o novo servico
3. Monolito mantem os mesmos endpoints como fallback
4. Feature flag controla % de trafego roteado para o novo servico

### Fase 2: Corte completo
1. 100% do trafego em `/auth/**` e `/users/**` vai para User service
2. Remover `AuthControllerV2`, `UserManagementController` e classes relacionadas do monolito
3. Monolito usa REST client para consultar User service quando necessario

### Endpoints de Auth (cliente final)
Endpoints publicos e autenticados expostos ao cliente via API Gateway.

| Metodo | Path | Tipo |
|--------|------|------|
| POST | /auth/register | publico |
| POST | /auth/login | publico |
| POST | /auth/refresh | publico |
| GET | /auth/me | autenticado (JWT de usuario) |
| POST | /auth/logout | publico |

### Endpoints de Gestao de Usuario (inter-service)
Endpoints internos consumidos exclusivamente por outros microsservicos — nao expostos ao
cliente final. Devem ser protegidos por service token (Bearer estatico rotacionavel) ou
mTLS, **nao** por JWT de usuario.

| Metodo | Path | Tipo | Consumidor |
|--------|------|------|------------|
| GET | /users/by-email/{email} | interno (service-to-service) | Workspace service — invite flow: buscar usuario pelo e-mail do convite |
| POST | /users/invited | interno (service-to-service) | Workspace service — invite flow: criar usuario convidado se nao existir |

**Importante:** Sem estes dois endpoints o card 02 (Workspace) nao pode refatorar
`WorkspaceControllerV2.inviteMember()` e fica bloqueado. Eles devem estar funcionando
e acessiveis pelo Workspace service antes do card 02 comecar.

**Autenticacao inter-service:** Decidir entre:
- Service token: header `X-Service-Token` com secret compartilhado (simples, baixo overhead)
- mTLS: certificados mutuos entre servicos (robusto, recomendado para producao)

### Classes a migrar

**Auth (cliente final):**
- `AuthControllerV2` + DTOs (auth/dto/*)
- `UserService` (domain)
- `User`, `UserActive`, `UserInactive`, `UserDeleted`, `UserRegistered` (sealed classes)
- `UserId`, `Email`, `PasswordHash` (value objects)
- `UserRepository` (port) + `JpaUserRepositoryAdapter` + `JpaUser` (adapter)
- `JwtService`, `JwtAuthenticationFilter`, `ScopeFlowPrincipal` (security)
- `SecurityConfig` (sera duplicada — cada servico tera sua propria)
- `RateLimit`, `RateLimitInterceptor` (rate limiting)

**Gestao de usuario (inter-service) — novo:**
- `UserManagementController` — controller separado do `AuthControllerV2`, expoe apenas
  os endpoints inter-service; `SecurityConfig` do User service protege `/users/**` com
  service token em vez de JWT de usuario

## Criterios de sucesso
- [ ] Todos os 5 endpoints de auth respondem identicamente (response body + status codes)
- [ ] Endpoints inter-service `/users/by-email/{email}` e `/users/invited` funcionando e
      acessiveis pelo Workspace service (card 02 desbloqueado)
- [ ] Endpoints inter-service rejeitam requests sem service token valido (401)
- [ ] Testes de integracao do monolito continuam passando (fallback ativo)
- [ ] JWT tokens gerados pelo novo servico sao aceitos pelo monolito (shared secret)
- [ ] Latencia p99 do /auth/login < 200ms (baseline do monolito)
- [ ] Rate limiting funciona identicamente
- [ ] Health check do User service no /actuator/health
- [ ] API Gateway configurado e roteando `/auth/**` e `/users/**` corretamente
- [ ] Cards 02, 03 e 04 conseguem ser roteados pelo API Gateway (infra base validada)
- [ ] Zero downtime durante a migracao (proxy switch)

## Criterios de rollback
**Quando reverter:**
- Latencia p99 > 500ms por mais de 5 minutos
- Taxa de erro > 1% em qualquer endpoint de auth
- Falha de validacao de JWT no monolito (tokens incompativeis)
- Falha no rate limiting (flood de requests)
- Endpoints inter-service `/users/by-email` ou `/users/invited` inacessiveis pelo
  Workspace service — bloqueia diretamente o inicio do card 02

**Como reverter:**
1. Feature flag: rotear 100% do trafego de volta para o monolito
2. Monolito ja tem os endpoints funcionando (nao foram removidos)
3. Tempo de rollback estimado: < 1 minuto (switch de proxy)
4. Nenhuma mudanca de schema necessaria para rollback

## Pre-requisitos (antes de comecar)

### Decisoes tecnicas
- [ ] Definir estrategia de JWT: shared HMAC secret (simples) ou RS256 + JWKS endpoint
      (robusto — recomendado se cards 02-04 precisam validar tokens independentemente)
- [ ] Definir autenticacao inter-service: service token estatico ou mTLS
- [ ] Definir como o monolito vai chamar o User service (REST client com circuit breaker —
      Resilience4j recomendado)
- [ ] Decision: Docker Compose para dev ou ja Kubernetes?

### API Gateway — infra base (bloqueador para todos os cards seguintes)
Este e o primeiro servico extraido; a configuracao do API Gateway acontece aqui e e
reutilizada pelos cards 02, 03 e 04. Sem API Gateway configurado, nenhum card subsequente
pode implementar seu roteamento.

- [ ] Escolher e provisionar API Gateway:
  - **Kong** (recomendado): suporte nativo a service-to-service auth, rate limiting,
    plugins de observabilidade; adequado tanto para Docker Compose quanto para Kubernetes
  - **nginx upstream**: simples, sem dependencia extra; adequado se o projeto nao usa
    Kubernetes ainda
  - **AWS ALB + target groups**: adequado se o deploy e em ECS/EKS na AWS
- [ ] Configurar rota `/auth/**` → User service com fallback para o monolito
- [ ] Configurar rota `/users/**` → User service (acesso restrito a services internos)
- [ ] Documentar convencao de rotas para os cards 02, 03 e 04 seguirem

### Infra operacional
- [ ] Pipeline de CI/CD para o novo servico (build, test, deploy)
- [ ] Health check e monitoring configurados
- [ ] Alertas de latencia e taxa de erro configurados antes de ativar feature flag

### Estrategia de testes durante transicao (R-05)
Apos a extracao, os testes de integracao E2E do monolito que dependem de auth (registro,
login, validacao de JWT) precisam continuar funcionando sem depender do User service
estar no ar.

- [ ] Definir abordagem para testes do monolito:
  - **WireMock** (recomendado): stub do User service nos testes de integracao do monolito;
    os endpoints inter-service `/users/by-email` e `/users/invited` sao stubados para
    simular o comportamento do User service externo
  - **Test doubles em Spring**: `@MockBean` no `UserServiceClient` (Feign/RestClient) para
    isolar o monolito nos testes unitarios de `WorkspaceControllerV2.inviteMember()`
- [ ] Atualizar `BriefingControllerV1IntegrationTest` e outros testes que fazem login real
      para usar o stub/mock do User service em vez de depender do `AuthControllerV2` local
- [ ] Testes de contrato (Spring Cloud Contract ou Pact) entre User service e Workspace
      service para os endpoints inter-service — garante que mudancas no contrato sao
      detectadas antes do deploy
