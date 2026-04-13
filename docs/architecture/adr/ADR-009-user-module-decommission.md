# ADR-009: Decommission do Módulo User no Monólito

**Status:** Aceito  
**Data:** 2026-04-12  
**Contexto:** Conclusão da Fase 4 do Strangler Fig — remoção do código legado de User do monólito

---

## Contexto

O `user-service` foi extraído do monólito via Strangler Fig em 20 sprints (ADR-008). Com a feature flag `auth.service.use-extracted=true` ativa e estável em staging, o código legado de autenticação e gestão de usuários no monólito tornou-se dead code. Esta ADR documenta a limpeza desse código.

## Decisão

Remover todos os caminhos de execução local (monolítico) do contexto User do monólito, mantendo apenas os proxies para o `user-service`.

### O que foi removido

| Arquivo | Ação | Motivo |
|---------|------|--------|
| `core/domain/user/UserService.java` | **Deletado** | Sem consumidores após limpeza dos proxies |
| `UserServiceTest.java` | **Deletado** | Testa classe inexistente |
| `AuthControllerV2.java` | Refatorado — proxy puro | Removidos `if (!useExtractedAuthService)` e dependências `UserService`, `JwtService`, `PasswordEncoder`, `buildLoginResponse()`, `extractCookie()` |
| `UserController.java` | Refatorado — proxy puro | Removidos `if (!useExtractedAuthService)` e dependências `UserService`, `PasswordEncoder`, `validateRole()`, `extractDisplayNameFromEmail()` |
| `WorkspaceControllerV2.java` | Refatorado — RestTemplate | Substituída dependência `UserService` por chamadas HTTP ao `user-service` (`GET /users/by-email`, `POST /users/invited`) |
| `DomainServiceConfig.java` | Bean removido | `userService()` sem consumidores |
| `application.yml` / `application-staging.yml` / `application-production.yml` | Propriedade removida | `auth.service.use-extracted` — flag de migração obsoleta |

### O que foi mantido (intencionalmente)

| Componente | Motivo para manter |
|-----------|-------------------|
| `UserRepository` + `JpaUserRepositoryAdapter` | Ainda usado por `UserStatusCacheService` (validação de status no JWT filter) |
| `UserStatusCacheService` | Lookup de status de usuário no banco compartilhado para o JWT auth filter |
| Classes de domínio: `User`, `UserId`, `Email`, `UserActive`, `UserInactive`, `UserDeleted` | Usadas por `WorkspaceService` (via `UserId`) e `UserStatusCacheService` |
| Exceptions de domínio user (`UserNotFoundException`, `InvalidCredentialsException`, etc.) | Referenciadas no `GlobalExceptionHandler` |
| `UserRegistrationListener` | Recebe eventos do `user-service` via RabbitMQ (`user.registered`) |
| `pom.xml` adição: `testcontainers:junit-jupiter` | Dependência faltando (bug pré-existente) — adicionada para habilitar `@Testcontainers` / `@Container` nos testes de integração |

## Arquitetura pós-decommission

```
monólito (porta 8080)
  └── AuthControllerV2  → proxy puro → user-service :8081/api/v1/auth/*
  └── UserController    → proxy puro → user-service :8081/api/v1/users/*
  └── WorkspaceControllerV2.inviteMember()
        → GET  user-service :8081/api/v1/users/by-email/{email}
        → POST user-service :8081/api/v1/users/invited  (se não encontrado)
  └── JwtAuthenticationFilter
        → UserStatusCacheService → UserRepository (banco compartilhado)

user-service (porta 8081)  ← fonte de verdade para usuários
  └── /api/v1/auth/* (register, login, refresh, me, logout)
  └── /api/v1/users/* (by-email, invited)
```

## Trade-offs

### ✅ Benefícios

- Eliminação de ~200 linhas de dead code no monólito
- Sem mais feature flag — caminho de execução único e previsível
- `WorkspaceControllerV2` deixa de depender do domínio User diretamente
- Menor footprint de Spring beans no monólito

### ⚠️ Limitações conhecidas

1. **Sem circuit breaker** — `WorkspaceControllerV2.inviteMember()` lança `RuntimeException` se o user-service estiver indisponível. Registrado no ADR-008 como gap pendente.
2. **Banco compartilhado** — `UserStatusCacheService` ainda lê a tabela `users` diretamente. Resolução futura: DB-per-service (separar banco do user-service).
3. **Testes de Briefing com erros de compilação pré-existentes** — não relacionados ao decommission; foram mascarados por erro de sintaxe no `AuthControllerV2Test.java` anterior. Precisam de atenção separada.

## Próximos passos

1. **Circuit breaker** no `WorkspaceControllerV2` (Resilience4j) — evitar cascata de falha quando user-service indisponível
2. **DB-per-service** — migrar tabela `users` para banco próprio do user-service e remover `UserRepository` / `UserStatusCacheService` do monólito
3. **Corrigir testes Briefing** — erros de compilação pré-existentes expostos por este cleanup
4. **Próxima extração** — contexto Workspace (ver fila de extração no ADR-008)
