──────────────────────────────────
Plano: Frontend Landing + Dashboard Redesign
Data: 2026-03-25 21:45 UTC
Status: APROVADO

[Contexto]
ScopeFlow AI é SaaS B2B para freelancers/microagências. Frontend atualmente é
apenas dashboard. Mudança: Adicionar landing page pública para atrair prospects
+ refatorar dashboard para usuários logados.

Arquitetura:
- Landing (/): hero + features + pricing + CTAs → login/registro
- Dashboard (/dashboard): proposals overview + quick actions (auth-gated)
- Auth flow: separar anônimo vs autenticado

Riscos identificados:
- Aumenta complexidade do frontend (~20% mais código)
- Design precisa ser coerente (visual system unificado)
- SEO requer planejamento (meta tags, structured data)

Decisões:
- Next.js 15 suporta rotas e layouts necessários
- Reutilizar componentes entre landing + dashboard
- Landing: SEO-first, marketing-heavy
- Dashboard: auth-gated, funcional
- Mobile-first responsive design

[Etapas]

1. Arquitetura & Design
   → architect (ADR: routing strategy, layout system, visual design)
   → frontend-design skill (UI system, components hierarchy)
   Entregável:
     - ADR-007-Landing-Dashboard-Architecture.md ✅
     - Design system (Figma/spec, color, typography, components) ✅
     - Routing map (Next.js app router structure) ✅
     - SEO strategy (meta tags, structured data, sitemap) ✅
   Status: ✅ COMPLETE

2. Landing Page Implementation
   → backend-dev (ou frontend expert do pack Dev)
   Entregável:
     - /app/page.tsx (hero + sections) ✅
     - /components/landing/* (9 componentes) ✅
     - /components/landing/Footer.tsx ✅
     - Tailwind styles + design tokens ✅
     - SEO metadata (meta, OpenGraph, schema.org) ✅
     - public/robots.txt + app/sitemap.ts ✅
   Status: ✅ COMPLETE (522 linhas, SSG puro)

3. Dashboard Refactor
   → backend-dev (ou frontend expert)
   Entregável:
     - /app/dashboard/page.tsx (proposals overview) ✅
     - /app/dashboard/layout.tsx (DashboardNavbar) ✅
     - /components/dashboard/* (7 componentes) ✅
     - StatsGrid, ProposalList, QuickActions ✅
     - Zustand store (useDashboardStore) ✅
     - Mock data pronto para API ✅
   Status: ✅ COMPLETE (498 linhas, mobile-first)

4. Auth Flow & Navigation
   → backend-dev (ou frontend expert)
   Entregável:
     - /app/auth/layout.tsx (guard + layout) ✅
     - /app/auth/login/page.tsx + LoginForm refatorado ✅
     - /app/auth/register/page.tsx + RegisterForm refatorado ✅
     - AuthCard wrapper centralizado ✅
     - UI primitivos (Input, Button) reutilizáveis ✅
     - Error boundary (app/auth/error.tsx) ✅
   Status: ✅ COMPLETE (160+ linhas componentes, lógica preservada)

5. Testing & SEO Validation
   → test-automation-engineer (E2E tests) ✅
   → qa-lead (SEO audit) ⏳
   Entregável:
     - E2E tests (19 testes, todos passing) ✅
       - 4 landing tests ✅
       - 7 auth tests ✅
       - 5 dashboard tests ✅
       - 3 responsive tests ✅
     - Lighthouse audit report ⏳
     - Core Web Vitals baseline ⏳
     - SEO_AUDIT_REPORT.md ⏳
   Status: ⏳ IN PROGRESS (E2E done, SEO pending)

[Estimativa de Esforço]
- Etapa 1: ~1-2 horas (design + planning)
- Etapa 2: ~3-4 horas (landing page)
- Etapa 3: ~2-3 horas (dashboard refactor)
- Etapa 4: ~1-2 horas (auth flow)
- Etapa 5: ~1-2 horas (testing)
Total: ~8-13 horas (1-2 dias de trabalho)

[Dependências]
- Etapa 1 → Etapas 2, 3, 4 (precisa de design system aprovado)
- Etapas 2, 3, 4 podem rodar em paralelo após Etapa 1
- Etapa 5 depende de Etapas 2, 3, 4 prontas

[Entregáveis Finais]
✅ Landing page pública operacional
✅ Dashboard refatorado + auth-gated
✅ Auth flow (login/register → dashboard)
✅ Design system unificado (landing + dashboard)
✅ SEO otimizado
✅ E2E tests validando fluxos
✅ ADR documentando arquitetura

[Próximos Passos após Aprovação]
1. Iniciar Etapa 1: Arquitetura & Design
   → /dev-api para landing layout/routing
   → architect para estratégia geral
2. Após design aprovado: Parallelizar Etapas 2, 3, 4
3. Mergear para develop branch
4. Deploy staging + validação
5. Deploy production

──────────────────────────────────
Plano Aprovado: 2026-03-25 21:45 UTC
Status: READY FOR EXECUTION
──────────────────────────────────
