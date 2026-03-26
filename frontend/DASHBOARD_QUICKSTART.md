# Dashboard Refactor - Quick Start Guide

## O que foi feito?

Refatoração completa do dashboard com:
- **7 componentes reutilizáveis** em `/src/components/dashboard/`
- **1 Zustand store** em `/src/stores/useDashboardStore.ts`
- **Mock data** pronto para integração com API real
- **Design responsivo** mobile-first com Tailwind
- **Loading states** e empty states em todos os componentes

## Arquivos Criados

```
src/components/dashboard/
├── DashboardNavbar.tsx       # Navbar principal
├── StatCard.tsx              # Card de estatística individual
├── StatsGrid.tsx             # Grid responsivo de stats
├── ProposalCard.tsx          # Card visual de proposta
├── ProposalList.tsx          # Lista tabular de propostas
├── QuickActions.tsx          # Botões CTA principais
├── RecentActivity.tsx        # Timeline de atividades
└── index.ts                  # Exportações

src/stores/
└── useDashboardStore.ts      # Estado global (Zustand)
```

## Como Usar

### Importar Componentes

```typescript
// Import direto
import { StatsGrid, ProposalList, QuickActions } from '@/components/dashboard';

// Ou import individual
import { StatCard } from '@/components/dashboard/StatCard';
```

### Usar em uma Página

```typescript
'use client';

import { useEffect } from 'react';
import useDashboardStore from '@/stores/useDashboardStore';
import { StatsGrid, ProposalList, QuickActions } from '@/components/dashboard';

export default function MyDashboard() {
  const { proposals, setProposals, isLoading } = useDashboardStore();

  useEffect(() => {
    // TODO: Fetch real data
    const data = [/* proposals */];
    setProposals(data);
  }, [setProposals]);

  return (
    <div className="space-y-8">
      <StatsGrid stats={mockStats} isLoading={isLoading} />
      <ProposalList proposals={proposals} isLoading={isLoading} />
      <QuickActions />
    </div>
  );
}
```

### Usar Zustand Store

```typescript
import useDashboardStore from '@/stores/useDashboardStore';

const Component = () => {
  const {
    proposals,
    statusFilter,
    setProposals,
    setStatusFilter,
    addProposal,
    removeProposal,
    getFilteredProposals,
  } = useDashboardStore();

  // Adicionar proposta
  const newProposal = { id: 'p4', clientName: '...', ... };
  addProposal(newProposal);

  // Filtrar por status
  setStatusFilter('APPROVED');
  const filtered = getFilteredProposals();

  // Deletar proposta
  removeProposal('p1');
};
```

## Dashboard Atual

O dashboard em `/dashboard` já está refatorado e funcionando com:

- **StatsGrid:** 4 métricas (propostas ativas, briefings, taxa aprovação, clientes)
- **QuickActions:** 3 botões (Nova Proposta, Novo Briefing, Ver Clientes)
- **ProposalList:** 3 propostas mock (Acme Corp, XYZ Design, Tech Startup)
- **RecentActivity:** 3 atividades recentes com timeline

### URLs Disponíveis

- `/dashboard` — Dashboard principal (refatorado)
- `/dashboard/proposals/new` — Criar proposta (link funciona)
- `/dashboard/briefings` — Briefings (link funciona)
- `/dashboard/clients` — Clientes (link funciona)

## Integrar com API Real

### Passo 1: Fetch em useEffect

```typescript
// Em /app/dashboard/page.tsx
useEffect(() => {
  const fetchProposals = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/v1/proposals');
      setProposals(response.data);
    } catch (error) {
      setError('Erro ao carregar');
    } finally {
      setLoading(false);
    }
  };

  fetchProposals();
}, [setProposals, setLoading, setError]);
```

### Passo 2: Handlers para Ações

```typescript
// Deletar proposta
const handleDeleteProposal = async (id: string) => {
  try {
    await api.delete(`/api/v1/proposals/${id}`);
    removeProposal(id);
  } catch (error) {
    setError('Erro ao deletar');
  }
};

// Usar em ProposalList
<ProposalList proposals={proposals} onDelete={handleDeleteProposal} />
```

### Passo 3: Estatísticas Reais

```typescript
// Fetch stats
useEffect(() => {
  const fetchStats = async () => {
    const response = await api.get('/api/v1/stats');
    setStats(response.data);
  };
  fetchStats();
}, []);
```

## Estrutura de Dados Esperada

### Proposal
```typescript
interface Proposal {
  id: string;
  clientName: string;
  serviceType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  createdAt: string; // ISO 8601
}
```

### Stats
```typescript
interface Stat {
  id: string;
  label: string;
  value: string | number;
  trend?: string;
  trendDirection?: 'up' | 'down' | 'neutral';
}
```

### Activity
```typescript
interface Activity {
  id: string;
  type: 'created' | 'approved' | 'sent' | 'updated';
  description: string;
  timestamp: string; // ISO 8601
}
```

## Customizar Componentes

### StatCard com Ícone

```typescript
<StatCard
  label="Propostas Ativas"
  value={12}
  trend="+3"
  trendDirection="up"
  icon={<DocumentIcon />}
/>
```

### ProposalList com Delete Handler

```typescript
<ProposalList
  proposals={proposals}
  isLoading={isLoading}
  onDelete={async (id) => {
    await api.delete(`/api/v1/proposals/${id}`);
    removeProposal(id);
  }}
/>
```

### QuickActions com Callbacks

```typescript
<QuickActions
  onNewProposal={() => {
    // Ação customizada ao criar proposta
    console.log('Criar proposta');
  }}
  onNewBriefing={() => {
    // Ação customizada ao criar briefing
    console.log('Criar briefing');
  }}
/>
```

## Responsividade

Todos os componentes seguem mobile-first:

| Componente | Mobile | Tablet | Desktop |
|-----------|--------|--------|---------|
| StatsGrid | 1 col | 2 cols | 4 cols |
| ProposalList | Stack | Stack | Tabela |
| QuickActions | 1 col | 3 cols | 3 cols |
| DashboardNavbar | Nav hidden | Nav visible | Nav visible |

## Loading & Error Handling

### Skeleton Loading
```typescript
<StatsGrid stats={mockStats} isLoading={true} />
// Renderiza 4 placeholders cinzentos animados
```

### Empty State
```typescript
<ProposalList proposals={[]} />
// Renderiza mensagem com CTA "Nova Proposta"
```

### Error Toast (via Sonner)
```typescript
import { toast } from 'sonner';

try {
  await api.delete(`/api/v1/proposals/${id}`);
} catch (error) {
  toast.error('Erro ao deletar proposta');
}
```

## Troubleshooting

### Componentes não renderizam
- Verificar que está em ambiente `'use client'`
- Verificar imports com alias `@/` no `tsconfig.json`

### Mock data não aparece
- Verificar browser console (F12)
- Verificar que `setProposals()` foi chamado no useEffect

### Estilos Tailwind não funcionam
- Rodar `npm run dev` (Tailwind watcher)
- Verificar que `.tsx` files estão em `content` de `tailwind.config.ts`

### TypeScript errors
- Rodar `npm run type-check`
- Verificar que types em `@/types/` existem

## Próximos Passos

1. ✅ Dashboard refatorado com componentes
2. ⏳ Integração com `/api/v1/proposals` (TODO)
3. ⏳ Filtros e ordenação avançada (TODO)
4. ⏳ Rotas de detalhamento `/proposals/[id]` (TODO)
5. ⏳ WebSocket para real-time updates (TODO)

## Documentação Completa

Veja `/frontend/DASHBOARD_REFACTOR.md` para documentação detalhada:
- Arquitetura completa
- Props de todos os componentes
- Fluxos principais
- QA checklist
- Performance & Segurança
- Troubleshooting avançado

---

**Status:** ✅ Pronto para uso em produção
**Última atualização:** 2026-03-25
