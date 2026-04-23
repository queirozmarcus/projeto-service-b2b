──────────────────────────────────
Plano: Fix Login 403 Forbidden
Data: 2026-04-21
Status: CONCLUÍDO ✅
──────────────────────────────────

[Contexto]
Frontend http://localhost:3000/auth/login retornava "Erro ao fazer login".
Backend rejeitava com 403 Forbidden.

[Diagnóstico]
- Logs mostravam requisições em /auth/register e /auth/refresh (sem prefixo /api/v1)
- Spring Security configurado para permitir /api/v1/auth/* (correto)
- next.config.js linha 54: rewrite usando 'http://localhost:8080/api' (SEM /v1) ❌
- next.config.js linha 86: env usando 'http://localhost:8080/api/v1' (COM /v1) ✅
- **Rewrite tinha precedência sobre env** → rotas iam para /api/auth/* → 403

[Etapas Executadas]
1. ✅ Diagnóstico — identificado conflito no next.config.js
2. ✅ Correção — linha 54 alterada para incluir /v1
3. ⏳ Validação — aguardando reinício do Next.js pelo usuário

[Mudança Aplicada]
Arquivo: frontend/next.config.js
- Antes: const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
- Depois: const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

[Ação Necessária do Usuário]
1. Reiniciar Next.js: cd frontend && npm run dev
2. Testar login em http://localhost:3000/auth/login
3. Verificar logs do backend — não deve mais ter 403 nos endpoints /auth/*

[Decisões]
- Correção no rewrite (linha 54) em vez de ajustar SecurityConfig
- Mantém consistência: ambas as configs agora usam /api/v1
- Next.js precisa reiniciar para aplicar mudança em next.config.js

[Próximos Passos Sugeridos]
- Após validar: criar .env.local com NEXT_PUBLIC_API_URL para sobrescrever defaults
- Considerar remover rewrites se não forem necessários (simplificação)
- Documentar essa config no README.md do frontend
──────────────────────────────────
