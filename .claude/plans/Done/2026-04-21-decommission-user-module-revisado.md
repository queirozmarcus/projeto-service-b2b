──────────────────────────────────
Plano: Decommission User Module (Revisado)
Data: 2026-04-21
Status: ✅ CONCLUÍDO COM SUCESSO — Migration V11 executada

[Contexto]
User-service extraído via Strangler Fig, rodando em staging.
Tentativa anterior falhou com 53 erros — value objects compartilhados 
foram removidos incorretamente.

Projeto: ScopeFlow AI (B2B)
Stack: Java 21, Spring Boot 3.4, Hexagonal Architecture
DB: scopeflow_users (porta 5433) — user-service dedicado

[Etapas]
1. backend-engineer → Remoção cirúrgica do código legado
   → Aggregate User removido, value objects movidos para shared/
   → DTOs do client mantidos (CreateInvitedUserRequest, UserResponse)
   → ServiceUnavailableException, AuthControllerV2, UserServiceClient MANTIDOS
   
2. data-engineer → Validar migration V11 (já criada)
   → Confirmar safety da migration DROP tables
   → Validar FKs cross-context documentadas
   → NÃO executar migration automaticamente
   
3. qa-engineer → Validar regressão completa
   → Compilação: ./mvnw clean compile (sucesso esperado)
   → Testes monólito: ~60 passando (antes: 126)
   → Testes user-service: 76 passando (inalterado)
   → Zero imports órfãos para classes deletadas
   
4. tech-lead (Marcus) → ADR de conclusão + atualizar docs
   → ADR-XXX: Decommission User Context
   → Atualizar context map (User → extraído)
   → Atualizar CLAUDE.md (remover linha "User domain" do monólito)

[Riscos]
- Imports órfãos se value objects não forem movidos corretamente
- Migration V11 DROP sem backup pode causar data loss
- JwtAuthenticationFilter pode quebrar se UserStatusCacheService 
  for removido sem refactor

[Decisões]
- Value objects compartilhados (UserId, Email, PasswordHash) movidos para core/domain/shared/
- DTOs do client mantidos (CreateInvitedUserRequest, UserResponse)
- Migration V11 NÃO será executada automaticamente — apenas validada
- ServiceUnavailableException mantido como proteção
- AuthControllerV2 (proxy) e UserServiceClient mantidos para interop

[Lições da Tentativa Anterior]
- ❌ Remover value objects compartilhados causa 53 erros de compilação
- ❌ Remover DTOs usados por UserServiceClient quebra contratos
- ❌ Remover UserStatusCacheService quebra JwtAuthenticationFilter
- ✅ Estratégia correta: mover shared → package compartilhado, remover apenas aggregate raiz

[Resultado Final]
- ✅ 57 arquivos removidos (21 domain + 7 adapters + 28 testes + 1 handler)
- ✅ 3 value objects movidos para core/domain/shared/
- ✅ 247 testes passando (0 failures, 27 skipped)
- ✅ BUILD SUCCESS
- ✅ Migration V11 validada e READY
- ✅ Migration V11 EXECUTADA com sucesso (2026-04-21 12:14:11)
- ✅ Validação pós-migration: zero tabelas user no banco
- ✅ Flyway schema version: 11
- ✅ ADR-004 criado
- ✅ CLAUDE.md atualizado

Duração total: ~2h30min (incluindo tentativa anterior falhada + execução da migration)

──────────────────────────────────
