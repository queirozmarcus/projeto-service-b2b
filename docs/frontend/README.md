# Frontend Documentation

**Status:** ✅ Pronto para uso  
**Stack:** Next.js 15 + TypeScript + Tailwind CSS

---

## Quick Start

### Estrutura

```
frontend/
├── src/
│   ├── app/
│   │   ├── page.tsx ..................... Landing page (SSG)
│   │   ├── auth/ ........................ Login/Register
│   │   └── dashboard/ ................... Dashboard (7 componentes)
│   └── components/
│       ├── landing/ ..................... 9 componentes reutilizáveis
│       └── dashboard/ ................... 7 componentes reutilizáveis
└── public/
    └── robots.txt ....................... SEO directives
```

### Rodar Localmente

```bash
cd frontend
npm install
npm run dev              # http://localhost:3000
npm run build            # Build de produção
npm run type-check       # Validar TypeScript
```

---

## Componentes

### Landing Page (9 componentes)

| Componente | Propósito | Status |
|-----------|-----------|--------|
| `LandingNavbar` | Navegação sticky com brand + auth CTAs | ✅ |
| `Hero` | Seção hero com headline + CTAs | ✅ |
| `FeatureGrid` | Grid de 3 colunas com features | ✅ |
| `FeatureCard` | Card individual de feature | ✅ |
| `PricingTable` | Tabela de preços (3 tiers) | ✅ |
| `PricingCard` | Card de pricing individual | ✅ |
| `SocialProof` | Testimonials + trust indicators | ✅ |
| `CTASection` | Call-to-action full-width | ✅ |
| `Footer` | Footer com 4 colunas de links | ✅ |

**Import:**
```tsx
import { Hero, FeatureGrid, PricingTable } from '@/components/landing';
```

### Dashboard (7 componentes)

| Componente | Propósito | Status |
|-----------|-----------|--------|
| `DashboardNavbar` | Navbar principal do dashboard | ✅ |
| `StatsGrid` | Grid responsivo de estatísticas | ✅ |
| `StatCard` | Card de estatística individual | ✅ |
| `ProposalList` | Lista/tabela de propostas | ✅ |
| `ProposalCard` | Card visual de proposta | ✅ |
| `QuickActions` | Botões de ação rápida | ✅ |
| `RecentActivity` | Timeline de atividades recentes | ✅ |

**Import:**
```tsx
import { StatsGrid, ProposalList, QuickActions } from '@/components/dashboard';
```

**Estado global:**
```tsx
import useDashboardStore from '@/stores/useDashboardStore';

const { proposals, setProposals, statusFilter, getFilteredProposals } = useDashboardStore();
```

---

## Como Usar

### Customizar Landing Page

**Features:**
```tsx
// Em /src/app/page.tsx
const FEATURES = [
  { icon: '🎯', title: 'Discovery', description: 'AI asks questions...' },
  // Adicionar mais features aqui
];
```

**Pricing:**
```tsx
const PLANS = [
  {
    name: 'Freelancer',
    price: 'R$ 29',
    features: ['10 projects/month', 'AI discovery'],
    highlight: false, // true para "Most Popular"
  },
];
```

### Integrar com API Real

```tsx
'use client';

import { useEffect } from 'react';
import useDashboardStore from '@/stores/useDashboardStore';

export default function DashboardPage() {
  const { setProposals, setLoading } = useDashboardStore();

  useEffect(() => {
    const fetchProposals = async () => {
      setLoading(true);
      try {
        const response = await fetch('/api/v1/proposals');
        const data = await response.json();
        setProposals(data);
      } catch (error) {
        console.error('Error fetching proposals:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProposals();
  }, [setProposals, setLoading]);

  return (
    <div className="space-y-8">
      <StatsGrid stats={stats} />
      <ProposalList proposals={proposals} />
      <QuickActions />
    </div>
  );
}
```

---

## SEO & Performance

### SEO Implementation

✅ **Meta tags completos** — Title, description, OpenGraph, Twitter Card  
✅ **Schema.org JSON-LD** — SoftwareApplication type  
✅ **Sitemap dinâmico** — `/sitemap.xml` gerado automaticamente  
✅ **robots.txt** — Indexa landing, bloqueia /dashboard e /auth  
✅ **Canonical URLs** — Evita conteúdo duplicado

### Performance

| Página | Performance | SEO | Accessibility |
|--------|-------------|-----|---------------|
| Landing | **95+** ⚡ | **94+** 🔍 | **92+** ♿ |
| Dashboard | **85+** ⚡ | **70** 🔒 | **88+** ♿ |
| Auth | **94+** ⚡ | **60** 🔒 | **91+** ♿ |

**Otimizações:**
- Landing page é SSG (zero JavaScript)
- Tailwind CSS minificado
- Next.js Image com lazy loading
- Code splitting automático

---

## Responsividade

Todos os componentes seguem **mobile-first**:

| Componente | Mobile | Tablet (md) | Desktop (lg) |
|-----------|--------|-------------|--------------|
| StatsGrid | 1 col | 2 cols | 4 cols |
| FeatureGrid | 1 col | 2 cols | 3 cols |
| PricingTable | Stack | 2 cols | 3 cols |
| ProposalList | Stack | Stack | Tabela |

Breakpoints: `md` (768px), `lg` (1024px), `xl` (1280px)

---

## Troubleshooting

### Componentes não renderizam
- Verificar `'use client'` em componentes com interatividade
- Verificar imports com alias `@/` no `tsconfig.json`

### Estilos Tailwind não funcionam
- Rodar `npm run dev` (Tailwind watcher)
- Verificar que `.tsx` está em `content` de `tailwind.config.ts`

### TypeScript errors
```bash
npm run type-check       # Verifica erros
```

### Build falha
```bash
npm run build            # Ver erros específicos
```

---

## Próximos Passos

**Implementado:**
- ✅ Landing page completa (9 componentes)
- ✅ Dashboard refatorado (7 componentes)
- ✅ SEO completo (meta tags, sitemap, schema.org)
- ✅ Zustand store para estado global
- ✅ Design system com Tailwind

**Backlog:**
- ⏳ Integração com API real (`/api/v1/proposals`)
- ⏳ Filtros avançados no dashboard
- ⏳ Rotas de detalhamento `/proposals/[id]`
- ⏳ WebSocket para updates em tempo real
- ⏳ Dark mode
- ⏳ Internacionalização (i18n)

---

## Referências Rápidas

### URLs Disponíveis

- `/` — Landing page
- `/auth/login` — Login
- `/auth/register` — Cadastro
- `/dashboard` — Dashboard principal
- `/dashboard/proposals/new` — Nova proposta
- `/dashboard/briefings` — Briefings
- `/dashboard/clients` — Clientes

### Estrutura de Dados

**Proposal:**
```typescript
interface Proposal {
  id: string;
  clientName: string;
  serviceType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  createdAt: string; // ISO 8601
}
```

**Stats:**
```typescript
interface Stat {
  id: string;
  label: string;
  value: string | number;
  trend?: string;
  trendDirection?: 'up' | 'down' | 'neutral';
}
```

---

## Documentação Arquivada

Para detalhes históricos, consulte:
- [`archive/DASHBOARD-GUIDE.md`](archive/DASHBOARD-GUIDE.md) — Guia completo do dashboard
- [`archive/LANDING-ARCHITECTURE.md`](archive/LANDING-ARCHITECTURE.md) — Arquitetura detalhada
- [`archive/LANDING-PAGE-GUIDE.md`](archive/LANDING-PAGE-GUIDE.md) — Guia de implementação
- [`archive/SEO_AUDIT_REPORT.md`](archive/SEO_AUDIT_REPORT.md) — Auditoria completa de SEO

---

**Última atualização:** 2026-04-20  
**Mantido por:** Equipe ScopeFlow
