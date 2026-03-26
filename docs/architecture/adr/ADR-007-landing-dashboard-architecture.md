# ADR-007: Arquitetura Landing Page + Dashboard (Frontend)

**Status:** Proposto
**Data:** 2026-03-25

---

## Contexto

O frontend ScopeFlow possui uma landing page inline em `/page.tsx` (~200 linhas, nav+hero+features+CTA+footer monolítico) e um dashboard placeholder em `/dashboard/page.tsx` (3 cards estáticos). Não existem componentes UI reutilizáveis -- tudo é Tailwind inline sem abstração. A evolução para landing profissional e dashboard funcional exige decisões sobre:

1. Estrutura de rotas e layouts (público vs protegido)
2. Design system e hierarquia de componentes
3. SEO e performance
4. Caminho de migração sem quebrar rotas existentes

## Decisão

Adotar **Next.js App Router com layouts por segmento** + **design system baseado em componentes UI primitivos** em `components/ui/`, separando landing, dashboard e briefing como domínios visuais independentes com tokens de design compartilhados.

## Alternativas Consideradas

### Opção A: Single layout com flags condicionais
- **Prós:** Menos arquivos, aparente simplicidade
- **Contras:** Layout único vira god component; lógica condicional (isLanding, isAuth, isDashboard) cresce exponencialmente; SSR/SSG fica impossível de otimizar por rota; Navbar pública e protegida são fundamentalmente diferentes
- **Rejeitada:** Viola separação de concerns, dificulta otimização por rota

### Opção B: Monorepo separado (landing + app)
- **Prós:** Deploy independente, times separados, otimização total da landing
- **Contras:** Overhead de infra (2 deploys, 2 CI pipelines, 2 domínios ou proxy); compartilhar design system exige pacote npm ou git submodule; equipe é uma pessoa / micro-equipe
- **Rejeitada:** Overhead desproporcional para MVP com equipe pequena

### Opção C: App Router com layout.tsx por segmento (escolhida)
- **Prós:** Nativo do Next.js 15; cada rota tem layout próprio sem prop drilling; landing pode ser 100% SSG (zero JS); dashboard usa `'use client'` apenas onde precisa; middleware já existe e funciona; componentes UI compartilhados via `components/ui/`
- **Contras:** Mais arquivos; precisa disciplina para não duplicar estilos entre layouts
- **Escolhida:** Simplicidade, alinhamento com framework, performance otimizável por segmento

## Consequências

- Landing page será Server Component puro (SSG), sem JavaScript no bundle cliente
- Dashboard mantém `'use client'` no layout (guard de autenticação)
- Briefing `/briefing/[token]` permanece inalterado (público, sem layout adicional)
- Componentes UI primitivos em `components/ui/` eliminam duplicação de estilos
- Middleware existente já cobre redirects corretamente, sem mudanças necessárias

---

# Design System & Component Hierarchy

## Tokens de Design (já definidos no Tailwind config)

```
CORES
  primary:   sky-blue scale (50-900) -- #0ea5e9 base -- ações, links, brand
  secondary: slate scale (50-900) -- #64748b base -- texto, borders, backgrounds
  success:   green (50/500/700) -- feedback positivo
  warning:   amber (50/500/700) -- alertas
  danger:    red (50/500/700) -- erros, destruição

TIPOGRAFIA (globals.css já define)
  h1: 4xl extrabold tracking-tight
  h2: 3xl semibold tracking-tight + border-b
  h3: 2xl semibold
  h4-h6: xl/lg/base semibold
  body: base leading-7

SPACING
  Container: max-w-7xl mx-auto px-6
  Section padding: py-16 a py-20
  Card padding: p-6 a p-8
  Gap grid: gap-6 a gap-8

BORDER RADIUS
  Cards: rounded-xl
  Buttons: rounded-lg
  Inputs: rounded-md

SHADOWS
  Cards: nenhuma ou shadow-sm (flat design atual)
  Elevação: shadow-md para hover, shadow-lg para modais
```

## Hierarquia de Componentes

### Camada 1: UI Primitivos (`components/ui/`)

```
components/ui/
  Button.tsx          -- variantes: primary, secondary, ghost, danger
                         tamanhos: sm, md, lg
                         estados: loading (spinner), disabled
                         renderiza <button> ou <Link> via prop `href`

  Card.tsx            -- container com border + rounded-xl + bg-white + p-6
                         slots: header, body, footer (via children ou props)

  Badge.tsx           -- status labels: draft, active, approved, expired
                         cores mapeadas por status

  Input.tsx           -- label + input + error message
                         tipos: text, email, password, textarea
                         integração com react-hook-form via forwardRef

  Select.tsx          -- label + select nativo + error

  Avatar.tsx          -- iniciais ou imagem, tamanhos sm/md/lg

  EmptyState.tsx      -- ícone + título + descrição + CTA opcional
                         usado em: listas vazias, primeiro uso

  Spinner.tsx         -- loading indicator (já usado inline, extrair)

  Logo.tsx            -- ScopeFlow brand mark, tamanhos sm/md/lg
```

**Justificativa:** Esses 8 primitivos cobrem 90%+ dos padrões visuais repetidos na landing, auth forms, dashboard e briefing. Não usar biblioteca externa (Radix está no package.json mas não usado ainda) -- primitivos próprios são suficientes para MVP e evitam dependência.

### Camada 2: Feature Components

```
components/landing/
  LandingNavbar.tsx   -- nav pública: Logo + links âncora + Login + CTA
  Hero.tsx            -- headline + subtitle + 2 CTAs + visual/ilustração
  FeatureGrid.tsx     -- grid 3 colunas, recebe array de features
  FeatureCard.tsx     -- ícone + título + descrição (usa Card internamente)
  PricingTable.tsx    -- 3 planos lado a lado, highlight no recomendado
  PricingCard.tsx     -- preço + features list + CTA
  SocialProof.tsx     -- logos ou depoimentos (placeholder para MVP)
  CTASection.tsx      -- fundo primary + headline + CTA button
  Footer.tsx          -- 4 colunas + copyright (extrair do page.tsx atual)

components/dashboard/
  DashboardNavbar.tsx -- refactor do Navbar.tsx atual (renomear)
  StatsGrid.tsx       -- 3-4 cards com métricas (propostas, briefings, aprovações)
  StatCard.tsx        -- número + label + trend indicator
  ProposalList.tsx    -- tabela/lista de propostas com status badges
  ProposalCard.tsx    -- card resumo: cliente, serviço, status, data
  QuickActions.tsx    -- botões: Nova Proposta, Novo Briefing, Ver Clientes
  RecentActivity.tsx  -- timeline de eventos recentes
  Sidebar.tsx         -- navegação lateral (futuro, quando tiver mais seções)

components/auth/
  LoginForm.tsx       -- já existe, extrair Input para ui/
  RegisterForm.tsx    -- já existe, extrair Input para ui/
  SessionProvider.tsx -- já existe, sem mudanças
  AuthCard.tsx        -- wrapper visual para forms de auth (card centralizado)
```

### Camada 3: Layouts

```
app/
  layout.tsx              -- RootLayout: <html>, <body>, SessionProvider, fonts
                             NÃO inclui Navbar/Footer (cada segmento define o seu)

  page.tsx                -- Landing: importa componentes de components/landing/
                             Server Component, SSG, zero JS no cliente

  (landing)/              -- ALTERNATIVA: route group se precisar layout próprio
    layout.tsx            -- LandingLayout: LandingNavbar + Footer wrapper
    page.tsx              -- composição das sections

  auth/
    layout.tsx            -- AuthLayout: centralizado, fundo sutil, sem nav
                             Guard: redireciona autenticados → /dashboard

  dashboard/
    layout.tsx            -- DashboardLayout: DashboardNavbar + conteúdo
                             Guard: redireciona não autenticados → /auth/login
    page.tsx              -- Overview: StatsGrid + ProposalList + QuickActions

  briefing/
    [token]/
      page.tsx            -- Sem mudanças, público, sem layout adicional
```

**Decisão sobre route groups:** Não usar `(landing)/` route group agora. A landing é uma única página em `page.tsx`. Route group adiciona complexidade sem benefício até que existam múltiplas páginas públicas (ex: `/about`, `/pricing`, `/blog`). Quando isso acontecer, migrar para `(marketing)/` route group.

---

# Routing Strategy

## Mapa de Rotas

```
ROTA                    TIPO        LAYOUT              GUARD           RENDER
/                       Pública     RootLayout           nenhum          SSG
/auth/login             Pública*    RootLayout→AuthLay   logado→/dash    SSR
/auth/register          Pública*    RootLayout→AuthLay   logado→/dash    SSR
/dashboard              Protegida   RootLayout→DashLay   anon→/login     SSR
/dashboard/proposals    Protegida   RootLayout→DashLay   anon→/login     SSR
/dashboard/clients      Protegida   RootLayout→DashLay   anon→/login     SSR
/briefing/[token]       Pública     RootLayout           nenhum          SSR

* Pública com redirect: se já logado, vai para /dashboard
```

## Middleware (sem mudanças necessárias)

O `middleware.ts` atual já cobre todos os casos:
- `PROTECTED_PREFIXES = ['/dashboard', '/proposals', '/workspaces']` -- redireciona anon para login
- `AUTH_PREFIXES = ['/auth/login', '/auth/register']` -- redireciona logado para dashboard
- `/briefing/[token]` e `/` não são interceptados -- correto

**Ação:** Nenhuma mudança no middleware. Apenas mover `/proposals` para `/dashboard/proposals` no futuro (sub-rota do dashboard) e ajustar o prefix.

## Layout Hierarchy (diagrama de nesting)

```
<html>                          ← app/layout.tsx (RootLayout)
  <body>
    <SessionProvider>
      ┌─ / ─────────────────────────────────────────────┐
      │  LandingNavbar                                   │  ← inline em page.tsx
      │  Hero + Features + Pricing + CTA + Footer        │     ou componentes importados
      └──────────────────────────────────────────────────┘

      ┌─ /auth/* ───────────────────────────────────────┐
      │  AuthLayout (centered card, no nav)              │  ← app/auth/layout.tsx
      │    LoginForm | RegisterForm                      │
      └──────────────────────────────────────────────────┘

      ┌─ /dashboard/* ──────────────────────────────────┐
      │  DashboardLayout                                 │  ← app/dashboard/layout.tsx
      │    DashboardNavbar                               │
      │    <main> children </main>                       │
      └──────────────────────────────────────────────────┘

      ┌─ /briefing/[token] ─────────────────────────────┐
      │  (sem layout adicional, direto no RootLayout)    │
      │  BriefingFlow                                    │
      └──────────────────────────────────────────────────┘
    </SessionProvider>
  </body>
</html>
```

---

# SEO & Marketing Strategy

## Meta Tags por Rota

```
/                   title: "ScopeFlow - Transforme briefings em escopos aprovados com IA"
                    description: "Plataforma de IA para freelancers e microagências..."
                    og:image: /og-landing.png (1200x630)
                    canonical: https://scopeflow.app

/auth/login         title: "Login - ScopeFlow"
                    robots: noindex (não indexar páginas de auth)

/auth/register      title: "Cadastro Gratuito - ScopeFlow"
                    robots: noindex

/dashboard/*        robots: noindex, nofollow (área protegida)

/briefing/[token]   robots: noindex (conteúdo privado por token)
```

## Structured Data (Schema.org)

```json
{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "ScopeFlow",
  "applicationCategory": "BusinessApplication",
  "operatingSystem": "Web",
  "offers": {
    "@type": "AggregateOffer",
    "priceCurrency": "BRL",
    "lowPrice": "0",
    "highPrice": "299"
  }
}
```

Incluir no `<head>` da landing via `<script type="application/ld+json">` no `page.tsx`.

## Core Web Vitals Targets

```
LCP  < 2.5s   -- Landing SSG garante HTML pronto; hero text sem imagem pesada
FID  < 100ms  -- Landing sem JS interativo; dashboard carrega async
CLS  < 0.1    -- Definir dimensões fixas para cards, evitar layout shift
INP  < 200ms  -- Interações do dashboard (click, filter) devem responder rápido
```

**Ações para atingir:**
- Landing como Server Component puro (0 KB JS no bundle)
- Fontes com `next/font` (eliminam FOUT/FOIT)
- Imagens com `next/image` (lazy loading, srcset automático)
- Dashboard: carregar dados via SWR/React Query com skeleton placeholders

## Sitemap & Robots

```
# public/robots.txt
User-agent: *
Allow: /
Disallow: /auth/
Disallow: /dashboard/
Disallow: /briefing/
Sitemap: https://scopeflow.app/sitemap.xml
```

Sitemap gerado via `app/sitemap.ts` (Next.js metadata API):
```
URLs indexáveis: /, /pricing (futuro), /blog/* (futuro)
```

---

# Component Extraction Plan

## Fase 1: Extrair UI Primitivos (pré-requisito)

```
CRIAR                           EXTRAIR DE
components/ui/Button.tsx        Links/buttons inline de page.tsx, Navbar, auth forms
components/ui/Card.tsx          Divs com border+rounded+bg-white do dashboard
components/ui/Input.tsx         Inputs duplicados em LoginForm + RegisterForm
components/ui/Badge.tsx         Novo (para status de propostas)
components/ui/EmptyState.tsx    Textos "Nenhuma proposta criada" do dashboard
components/ui/Logo.tsx          Textos "ScopeFlow" hardcoded em 3 lugares
components/ui/Spinner.tsx       Loading states inline
components/ui/Avatar.tsx        Novo (para user info na navbar)
```

## Fase 2: Extrair Landing Components

```
CRIAR                               EXTRAIR DE
components/landing/LandingNavbar.tsx  <nav> de page.tsx (linhas 7-27)
components/landing/Hero.tsx           <section> de page.tsx (linhas 30-54)
components/landing/FeatureGrid.tsx    <section id="features"> (linhas 57-100)
components/landing/FeatureCard.tsx    Divs individuais dentro do grid
components/landing/CTASection.tsx     <section bg-primary> (linhas 103-118)
components/landing/Footer.tsx         <footer> de page.tsx (linhas 121-195)
components/landing/PricingTable.tsx   Novo (seção de preços)
components/landing/SocialProof.tsx    Novo (depoimentos/logos placeholder)
```

**page.tsx resultante (~30 linhas):**
```tsx
export default function LandingPage() {
  return (
    <main>
      <LandingNavbar />
      <Hero />
      <FeatureGrid features={FEATURES} />
      <SocialProof />
      <PricingTable plans={PLANS} />
      <CTASection />
      <Footer />
    </main>
  );
}
```

## Fase 3: Dashboard Components

```
CRIAR                                   REFATORAR
components/dashboard/DashboardNavbar.tsx  Renomear/refatorar protected/Navbar.tsx
components/dashboard/StatsGrid.tsx        Novo (substituir cards estáticos)
components/dashboard/StatCard.tsx         Novo (card com métrica + trend)
components/dashboard/ProposalList.tsx     Novo (lista de propostas)
components/dashboard/ProposalCard.tsx     Novo (card individual)
components/dashboard/QuickActions.tsx     Novo (ações rápidas)
components/dashboard/EmptyDashboard.tsx   Novo (primeiro acesso, onboarding)
```

## Fase 4: Auth Refinement

```
CRIAR                           REFATORAR
components/auth/AuthCard.tsx    Novo (wrapper visual centralizado)
LoginForm.tsx                   Substituir inputs inline por ui/Input
RegisterForm.tsx                Substituir inputs inline por ui/Input
```

---

# Migration Path

## Ordem de Execução

```
PASSO   BRANCH              DESCRIÇÃO                              RISCO
1       feat/ui-primitives   Criar components/ui/ (8 primitivos)    Nenhum (aditivo)
2       feat/landing-refac   Extrair landing em componentes         Baixo (mesma rota)
3       feat/landing-v2      Adicionar Pricing + SocialProof        Nenhum (aditivo)
4       feat/dashboard-v2    Dashboard funcional com stats/list     Baixo (rota protegida)
5       feat/seo             Meta tags, sitemap, robots, schema.org Nenhum (aditivo)
```

## Backward Compatibility

- `/briefing/[token]` -- ZERO mudanças. Componentes briefing/ não são tocados.
- `/auth/*` -- Mudanças visuais apenas (AuthCard wrapper). Lógica inalterada.
- `/dashboard` -- Visual melhora, mas funcionalidade depende de API backend (stats, lista de propostas). Degradação graciosa com EmptyState enquanto API não existe.
- Middleware -- Sem alteração.
- SessionProvider -- Sem alteração. Permanece no RootLayout.

## Arquivos Modificados vs Criados

```
MODIFICAR (5)
  app/page.tsx               -- Substituir monolito por composição de componentes
  app/layout.tsx             -- Adicionar next/font, ajustar metadata
  app/dashboard/page.tsx     -- Dashboard funcional com componentes
  app/dashboard/layout.tsx   -- Usar DashboardNavbar ao invés de Navbar
  app/auth/layout.tsx        -- Adicionar AuthCard wrapper

CRIAR (~20)
  components/ui/             -- 8 primitivos
  components/landing/        -- 8 componentes
  components/dashboard/      -- 6 componentes
  components/auth/AuthCard   -- 1 wrapper
  public/robots.txt          -- SEO
  app/sitemap.ts             -- SEO

MOVER/RENOMEAR (1)
  components/protected/Navbar.tsx → components/dashboard/DashboardNavbar.tsx

DELETAR (0)
  Nenhum arquivo deletado. protected/ fica vazia e pode ser removida.
```

## Git Strategy

Uma feature branch por fase. Commits lógicos dentro de cada branch:

```
feat/ui-primitives
  feat(ui): adiciona componente Button com variantes e estados
  feat(ui): adiciona componentes Card, Badge, Input, EmptyState
  feat(ui): adiciona Logo, Spinner, Avatar

feat/landing-refactor
  refactor(landing): extrai seções da landing em componentes
  feat(landing): adiciona PricingTable e SocialProof
  feat(seo): adiciona meta tags, sitemap, robots.txt

feat/dashboard-v2
  refactor(dashboard): renomeia Navbar para DashboardNavbar
  feat(dashboard): adiciona StatsGrid e QuickActions
  feat(dashboard): adiciona ProposalList com empty state
```

---

## Riscos Aceitos

| Risco | Mitigação |
|-------|-----------|
| Dashboard depende de API backend que não existe ainda | EmptyState + dados mock; componentes prontos para conectar |
| PricingTable sem planos definidos no backend | Dados hardcoded; ajustar quando pricing model estiver fechado |
| Tailwind 4.0 mencionado no briefing mas config é v3 syntax | Manter config v3 atual; migrar para v4 separadamente se necessário |
| Design system próprio vs usar Radix/shadcn | Primitivos próprios são suficientes para MVP; Radix já é dependência e pode ser adotado depois sem breaking changes |
