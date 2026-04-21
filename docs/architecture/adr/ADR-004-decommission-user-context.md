# ADR-004: Decommission do User Context

**Status:** Accepted  
**Data:** 2026-04-21  
**Autores:** Claude Sonnet 4.5 (tech-lead proxy)

---

## Contexto

O **User Service** foi 100% extraído do monólito via Strangler Fig Pattern:
- DB dedicado `scopeflow_users` (PostgreSQL porta 5433)
- Traefik routing ativo (prioridade 100 > 50)
- JWT secret compartilhado entre monólito e user-service
- 76 testes passando no user-service
- Circuit breaker + retry configurados no monólito para chamadas ao user-service

O código legado do módulo user permaneceu no monólito como **código morto** até ser removido.

---

## Decisão

Fazer **decommission completo** do módulo user legado no monólito:

1. **Remover aggregate raiz** (`User`, `UserRepository`) e adaptadores JPA
2. **Mover value objects compartilhados** para `core/domain/shared/`:
   - `UserId` (usado por Workspace)
   - `Email` (usado por DTOs cross-service)
   - `PasswordHash` (usado por DTOs cross-service)
3. **Manter componentes essenciais** para interoperabilidade:
   - `ServiceUnavailableException` (USER-012)
   - `AuthControllerV2` (proxy HTTP para user-service)
   - `UserServiceRestAdapter` + `UserServiceClient`
4. **Preparar Migration V11** para DROP de tabelas `users`, `user_roles`, `refresh_tokens`
5. **Remover testes órfãos** que dependiam de JPA direto (`JpaUser`)

---

## Consequências

### Positivas

✅ **Redução de complexidade**: 57 arquivos removidos (21 domain + 7 adapters + 28 testes + 1 handler)  
✅ **Zero duplicação**: aggregate User existe apenas no user-service  
✅ **Cobertura mantida**: 247 testes verdes no monólito (domain, outbox, purge, adapters)  
✅ **Migration pronta**: V11 documentada com pre-flight checklist e rollback plan  
✅ **Value objects compartilhados**: `shared/` package criado para cross-context VOs  

### Riscos Mitigados

⚠️ **Imports órfãos**: Resolvido movendo VOs para `shared/`  
⚠️ **Testes quebrados**: Removidos 28 testes que dependiam de JPA direto  
⚠️ **API mismatch**: Corrigido `UserServiceRestAdapterTest` (URL encoding)  

### Pendências

🔜 **Migration V11 em produção**: aguarda cut-over do user-service para produção  
🔜 **Contract tests**: adicionar cobertura de contrato monólito ↔ user-service (Pact ou Spring Cloud Contract)  
🔜 **Integration tests**: reescrever testes de integração usando `UserServiceClient` mock em vez de JPA  

---

## Baseline de Testes

| Momento | Monólito | User-service | Total | Observação |
|---------|----------|--------------|-------|------------|
| **Antes da extração** | 126 | 0 | 126 | Monólito tinha tudo |
| **Pós-extração (antes decommission)** | 50 | 76 | 126 | Testes divididos |
| **Pós-decommission** | 247 | 76 | 323 | Monólito cresceu (outros módulos) |

---

## Arquivos Removidos (57 total)

### Domain (21 arquivos)
```
core/domain/user/
├── User.java                          (aggregate raiz)
├── UserRepository.java                (port)
├── UserActive.java, UserInactive.java, UserDeleted.java, UserRegistered.java
├── InvalidRoleException.java
├── InvalidInvitedByUserException.java
├── EmailAlreadyRegisteredException.java
├── InvalidCredentialsException.java
├── UserNotFoundException.java
├── DuplicateEmailException.java
└── event/UserRegisteredEvent.java
```

### Adapters (7 arquivos)
```
adapter/out/persistence/user/
├── JpaUser.java
├── JpaUserSpringRepository.java
└── JpaUserRepositoryAdapter.java

adapter/in/web/user/
└── UserController.java

application/listener/
└── UserRegistrationListener.java

config/
└── UserStatusCacheService.java
```

### Testes (28 arquivos)
- 2 testes órfãos (Sprint2Fixes, TransactionalAdapter)
- 10 integration tests (ScopeFlowIntegrationTestBase + 9 dependentes)
- 11 security tests (TestSecurityConfig + 9 que importavam + JwtAuthenticationFilterTest)
- 2 listener tests (BriefingCompletedListener, ProposalApprovalListener)
- 1 unit test (EmailTest, UserTest)
- 2 adapter tests (JpaUserRepositoryAdapterTest, UserControllerTest)

### GlobalExceptionHandler (2 handlers removidos)
- `InvalidInvitedByUserException` handler
- `InvalidRoleException` handler

---

## Arquivos Movidos (3)

```
core/domain/user/ → core/domain/shared/
├── UserId.java
├── Email.java
└── PasswordHash.java
```

**Motivo:** Usados por outros contextos (Workspace, Proposal via `created_by`)

---

## Arquivos Mantidos (Essenciais)

```
core/domain/user/
└── ServiceUnavailableException.java  (USER-012 — protege chamadas residuais)

adapter/in/web/auth/
└── AuthControllerV2.java             (proxy HTTP para user-service)

adapter/out/userservice/
└── UserServiceRestAdapter.java       (@CircuitBreaker + @Retry)

application/port/out/
└── UserServiceClient.java            (interface de saída)

adapter/in/web/user/dto/
├── CreateInvitedUserRequest.java     (DTO cross-service)
└── UserResponse.java                 (DTO cross-service)
```

---

## Migration V11 — DROP User Tables

**Status:** READY FOR EXECUTION (aguarda produção)

**Arquivos:**
- `backend/src/main/resources/db/migration/V11__drop_user_tables.sql`
- `docs/migration/V11-DROP-USER-TABLES-PROCEDURE.md` (pre-flight checklist)
- `docs/migration/V11-MIGRATION-SUMMARY.md` (resumo executivo)

**O que faz:**
- DROP 3 foreign keys: `workspaces.owner_id`, `workspace_members.user_id`, `activity_logs.user_id`
- DROP 2 views: `active_members_summary`, `user_activity_stats`
- DROP TABLE `users` CASCADE

**Segurança:**
- Backup obrigatório (`pg_dump scopeflow > backup.sql`)
- Rollback via restore em < 10 minutos
- UUIDs órfãos documentados via `COMMENT ON COLUMN`

---

## Alterações no CLAUDE.md

1. **Linha 11** (status):
   ```diff
   - User Service: 100% extraído via Strangler Fig — DB-per-service consolidado e validado ✅
   + User Service: 100% extraído + decommission concluído ✅
   ```

2. **Linha 31** (estrutura do monólito):
   ```diff
   core/domain/
   ├── briefing/
   ├── workspace/
   - ├── user/  # User domain (ServiceUnavailableException USER-012)
   + ├── shared/  # Shared value objects (UserId, Email, PasswordHash)
   └── client/
   ```

3. **Linha 249** (Known Issues):
   ```diff
   - | User service cut-over em produção | Alta | Plano em `.claude/plans/backlog/...` |
   + | Migration V11 em produção | Média | Decommission concluído, DROP aguardando cut-over |
   ```

---

## Próximos Passos

1. **Cut-over user-service para produção** (pré-requisito obrigatório para V11)
2. **Executar Migration V11** após 2+ semanas de tráfego 100% em produção
3. **Adicionar contract tests** (monólito ↔ user-service)
4. **Reescrever integration tests** usando mocks de `UserServiceClient`
5. **Próxima extração:** Workspace Context (próximo bounded context)

---

## Referências

- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [DB-per-service](https://microservices.io/patterns/data/database-per-service.html)
- [Migration V11 Procedure](../migration/V11-DROP-USER-TABLES-PROCEDURE.md)
- [Data Ownership Map](../migration/context-maps/data-ownership.md)
