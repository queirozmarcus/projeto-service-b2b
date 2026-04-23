---
Plano: Fix "An unexpected error occurred" em /dashboard/proposals/new
Data: 2026-04-21
Status: APROVADO

[Contexto]
JWT gerado pelo user-service não inclui workspace_id.
SecurityUtil.getWorkspaceId() → null → SecurityException → catch-all → 500.
Healthcheck quebrado após remoção de context-path.

[Etapas]
1. docker-compose.yml → corrigir healthcheck URL user-service (remover /api/v1/)
2. user-service → Flyway V2 + User entity + TokenIssuer + use cases (workspace_id no JWT)
3. Script SQL para sincronizar workspace_id dos usuários existentes
4. GlobalExceptionHandler (monolith) → handler SecurityException → 403

[Riscos]
- Usuários sem workspace_id na user-service DB (Etapa 3 precisa rodar antes de testar)

[Decisões]
- workspace_id nullable no user-service (usuários existentes migrados via script)
- SecurityException → 403 Forbidden (não 500)
---
