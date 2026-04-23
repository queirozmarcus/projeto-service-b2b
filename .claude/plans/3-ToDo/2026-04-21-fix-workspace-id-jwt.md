──────────────────────────────────
Plano: Fix workspace_id em JWT — users órfãos
Data: 2026-04-21
Status: APROVADO ✅ — Iniciando execução

[Contexto]
Usuários criados antes da migration V3 (`add_workspace_id_to_users.sql`) têm `workspace_id = NULL`.
JWT gerado com claim `workspace_id: null` → monólito rejeita operações multi-tenant (ex: criar briefing).

Confirmado: `smoke-test-@example.com` (id: d7bec7ea-...) tem workspace_id null.

**DESCOBERTA CRÍTICA (2026-04-21):**
- User-service ainda usa SHARED DB com monólito (tabela workspaces NÃO existe no user-service DB)
- Workspace context está no backlog (não foi extraído ainda)
- Migration precisa ser aplicada no MONÓLITO, não no user-service

Decisões do brainstorm REVISADAS:
- Opção E + G: Migration V4 no MONÓLITO cria workspace default (one-time fix) + Backend retorna 412 "Setup Required" se workspace ausente + Frontend redireciona
- Trade-off: JWT válido para auth, mas operações multi-tenant exigem workspace (UX clara)

[Etapas]

1. ✅ **Migration V10 no MONÓLITO — Criar workspace default para users órfãos**
   → Agent: dba
   → Output: `backend/src/main/resources/db/migration/V10__assign_default_workspace_to_orphan_users.sql`
   → Lógica:
     - INSERT workspace default "My Workspace" para cada user sem workspace_id
     - UPDATE users SET workspace_id = workspace.id WHERE workspace_id IS NULL
     - COMMIT transacional (rollback manual via DELETE + UPDATE se necessário)
   → Dependência: nenhuma (tabela workspaces existe no monólito desde V2)
   → Risco: workspace órfão permanece se rollback (benigno — user.workspace_id volta a null)

2. **Aplicar migration V10 no monólito**
   → Command: `docker restart scopeflow-api` (Flyway aplica V10 no startup)
   → Output: V10 aplicada, tabela `flyway_schema_history` do monólito atualizada
   → Dependência: etapa 1 (migration criada)
   → Validação: `docker exec scopeflow-db psql -U postgres -d scopeflow -c "SELECT COUNT(*) FROM users WHERE workspace_id IS NULL;"` → esperado: 0

3. **Teste de regressão — JWT com workspace_id preenchido**
   → Agent: integration-test-engineer
   → Output: teste automatizado que valida:
     - Login com usuário órfão → JWT contém workspace_id (não null)
     - Criar briefing com JWT → sucesso (200 OK)
   → Dependência: etapa 2 (migration aplicada)
   → Risco: nenhum (teste de validação apenas)

4. ✅ **Backend — Retornar 412 Precondition Failed se workspace ausente**
   → Agent: backend-dev
   → Output:
     - GlobalExceptionHandler captura operações multi-tenant com workspace_id null
     - Retorna 412 Precondition Failed com RFC 9457 Problem Details
     - Error code: WORKSPACE-001
     - Payload inclui link para `/setup/workspace`
   → Dependência: etapa 2 (migration resolve casos históricos primeiro)
   → Risco: pode impactar outros endpoints — aceito, será descoberto nos testes

5. **Frontend — Detectar 412 e redirecionar para setup**
   → Agent: frontend-dev (UI)
   → Output:
     - Interceptor Axios captura 412 com WORKSPACE-001
     - Redireciona para `/setup/workspace` (wizard de criação)
   → Dependência: etapa 4 (backend retorna 412)
   → Risco: nenhum (UX clara)

6. **Documentar rollback manual da V10**
   → Output: `docs/migration/V10-ROLLBACK.md`
   → Conteúdo:
     ```sql
     -- Identificar workspaces criados pela V4 (nome default)
     SELECT id FROM workspaces WHERE name = 'My Workspace' AND description = 'Default workspace';
     -- Reverter users para workspace_id null
     UPDATE users SET workspace_id = NULL WHERE workspace_id IN (...);
     -- Deletar workspaces órfãos (CUIDADO: verifica dependências)
     DELETE FROM workspaces WHERE id IN (...);
     ```
   → Dependência: etapa 1 (migration criada)

[Riscos Gerais]
- Migration V10 cria workspace sem dono explícito → aceito (user é dono implícito por FK)
- Rollback manual exige SQL cuidadoso → documentado na etapa 6
- Backend 412 pode impactar endpoints não mapeados → será descoberto nos testes (aceito)

[Próximos Passos Após Conclusão]
- `/qa-generate` para etapa 3 (cobertura de testes)
- `/dev-review` para migration V4 (validar SQL)
- Considerar ADR para política de workspace default em novos serviços extraídos
──────────────────────────────────
