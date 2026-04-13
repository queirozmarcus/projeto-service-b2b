# Dashboard Refactor - Etapa 3

## Visão Geral

Refatoração completa do dashboard (`/dashboard`) implementando componentes reutilizáveis, gerenciamento de estado com Zustand, e integração com dados (mock) prontos para API.

## Arquitetura

### Nova Estrutura de Componentes

```
src/components/dashboard/
├── DashboardNavbar.tsx      # Navbar principal do dashboard
├── StatCard.tsx             # Card individual de estatísticas
├── StatsGrid.tsx            # Grid responsivo de StatCards (3-4 colunas)
├── ProposalCard.tsx         # Card resumido de proposta
├── ProposalList.tsx         # Lista/tabela de propostas com filtros
├── QuickActions.tsx         # Botões CTA: Nova Proposta, Briefing, Clientes
├── RecentActivity.tsx       # Timeline de atividades recentes
└── index.ts                 # Exportações centralizadas
```

### Estado Global (Zustand)

**Arquivo:** `src/stores/useDashboardStore.ts`

```typescript
interface DashboardState {
  proposals: Proposal[];
  statusFilter: 'ALL' | 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  sortBy: 'date' | 'status' | 'client';
  isLoading: boolean;
  error: string | null;

  setProposals(proposals: Proposal[]): void;
  setStatusFilter(filter): void;
  setSortBy(sortBy): void;
  setLoading(loading: boolean): void;
  setError(error: string | null): void;
  addProposal(proposal): void;
  removeProposal(id: string): void;
  updateProposal(id: string, updates): void;
  getFilteredProposals(): Proposal[];
}
```

### Atualizações em Arquivos Existentes

#### `/app/dashboard/page.tsx`
- **Antes:** 3 cards estáticos hardcoded
- **Depois:** ~115 linhas com composição de componentes
- Integração com `useDashboardStore`
- Mock data para demonstração (TODO: substituir por API)

#### `/app/dashboard/layout.tsx`
- Troca de `<Navbar />` para `<DashboardNavbar />`
- Mantém guard de autenticação
- Mesmo layout min-h-screen

## Componentes

### 1. DashboardNavbar
Navegação principal com logo, links e dropdown de usuário.

**Props:** Nenhuma (obtém dados de `useSessionStore`)

**Features:**
- Logo com link para `/dashboard`
- Nav links: Dashboard, Propostas, Briefings (responsivos em mobile)
- Dropdown de usuário: nome, email, logout
- Hover effects e transições suaves

### 2. StatCard
Card individual exibindo métrica com valor grande, label e trend.

**Props:**
```typescript
interface StatCardProps {
  label: string;           // "Propostas Ativas"
  value: string | number;  // 12 ou "87%"
  icon?: ReactNode;        // Opcional: ícone SVG
  trend?: string;          // "+3" ou "+5%"
  trendDirection?: 'up' | 'down' | 'neutral';
}
```

**Tailwind:**
- Cores: green-600 (up), red-600 (down), secondary-600 (neutral)
- Hover shadow elevado

### 3. StatsGrid
Grid responsivo (3-4 colunas) de StatCards com skeleton loading.

**Props:**
```typescript
interface StatsGridProps {
  stats?: Array<{
    id: string;
    label: string;
    value: string | number;
    trend?: string;
    trendDirection?: 'up' | 'down' | 'neutral';
  }>;
  isLoading?: boolean;
}
```

**Layout:**
- MD: 2 colunas
- LG: 4 colunas
- Mobile: 1 coluna

### 4. ProposalCard
Card visual (não tabela) exibindo resumo de proposta.

**Props:**
```typescript
interface ProposalCardProps {
  id: string;
  clientName: string;
  serviceType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}
```

**Features:**
- Status badge colorida
- Data formatada em pt-BR
- Botões: Ver (link), Editar

### 5. ProposalList
Lista tabular de propostas com suporte a filtros, ordenação, empty state, e loading skeletons.

**Props:**
```typescript
interface ProposalListProps {
  proposals?: Proposal[];
  isLoading?: boolean;
  onDelete?: (id: string) => Promise<void>;
}
```

**Features:**
- Colunas: Cliente, Serviço, Status (badge), Data, Ações (Ver, Deletar)
- Empty state com CTA
- Skeleton loading (3 linhas)
- Responsivo: stack em mobile, linha em desktop
- Formatação de datas em pt-BR

### 6. QuickActions
3 botões CTA destacados (primário, secundário, terciário).

**Props:**
```typescript
interface QuickActionsProps {
  onNewProposal?: () => void;
  onNewBriefing?: () => void;
}
```

**Botões:**
1. "+ Nova Proposta" → `/dashboard/proposals/new`
2. "+ Novo Briefing" → callback ou rota futura
3. "Ver Clientes" → `/dashboard/clients`

### 7. RecentActivity
Timeline de atividades recentes com ícones, descrição e timestamp.

**Props:**
```typescript
interface RecentActivityProps {
  activities?: ActivityItem[];
  isLoading?: boolean;
}

interface ActivityItem {
  id: string;
  type: 'created' | 'approved' | 'sent' | 'updated';
  description: string;
  timestamp: string;
}
```

**Features:**
- Dot colorido por tipo: blue (created), green (approved), purple (sent), orange (updated)
- Timestamp formatado em HH:mm
- Empty state

## Dados & Mock

Atualmente o dashboard utiliza mock data em `DashboardPage`:

```typescript
const mockProposals = [
  {
    id: 'p1',
    clientName: 'Acme Corp',
    serviceType: 'Social Media',
    status: 'APPROVED',
    createdAt: '2026-03-20T10:00:00Z',
  },
  // ...
];

const mockStats = [
  { id: 'active', label: 'Propostas Ativas', value: 5, trend: '+2', trendDirection: 'up' },
  // ...
];

const mockActivities = [
  { id: 'a1', type: 'approved', description: 'Proposta de Acme Corp aprovada', timestamp: '...' },
  // ...
];
```

### TODO: Integração com API

Substituir mock data por chamadas reais:

```typescript
// Em useEffect de DashboardPage
const fetchProposals = async () => {
  try {
    setLoading(true);
    const response = await api.get('/api/v1/proposals');
    setProposals(response.data);
  } catch (error) {
    setError('Erro ao carregar propostas');
  } finally {
    setLoading(false);
  }
};
fetchProposals();
```

## Responsividade

Todos os componentes implementam mobile-first design:

| Breakpoint | Comportamento |
|-----------|---|
| **Mobile (< 640px)** | StatsGrid: 1 col; ProposalList: stack vertical; Navbar: nav links ocultos |
| **Tablet (640px-1024px)** | StatsGrid: 2 cols; ProposalList: cards; Navbar: nav visible |
| **Desktop (> 1024px)** | StatsGrid: 4 cols; ProposalList: tabela completa; Navbar: full |

## Acessibilidade

- Tabelas: uso de `<thead>`, `<tbody>` semântico
- Badges: `aria-label` quando necessário
- Botões: texto descritivo (não só ícones)
- Loading: `aria-live` regions (via skeleton)
- Links: `href` correto para navegação
- Cores: contraste WCAG AA em todos os elementos

## Loading States

### Skeleton Placeholders
- StatsGrid: 4 cards cinzentos animados (`animate-pulse`)
- ProposalList: 3 linhas cinzentas animadas
- RecentActivity: 3 atividades cinzentas animadas

### Disabled States
- Botões em ação: `disabled:opacity-50`
- Deletar proposta: mostra "Deletando..." enquanto em progresso

## Tailwind Tokens

Usa design tokens já estabelecidos:

| Token | Valores |
|-------|---------|
| **primary** | sky-blue (`#0ea5e9`) |
| **secondary** | slate (`#64748b`) |
| **success** | green-600 (`#16a34a`) |
| **warning** | orange-600 (`#ea580c`) |
| **error** | red-600 (`#dc2626`) |

## Scripts & Comandos

```bash
# Verificar tipos (inclui dashboard)
npm run type-check

# Linting (requer config fix)
npm run lint

# Build
npm run build

# Dev
npm run dev          # http://localhost:3000/dashboard
```

## Fluxos Principais

### 1. Carregar Dashboard
1. Middleware verifica autenticação
2. Layout renderiza DashboardNavbar
3. Page fetches proposals (mock ou API)
4. StatsGrid renderiza com dados de dashboard store
5. ProposalList renderiza com propostas do store

### 2. Deletar Proposta
1. Usuário clica "Deletar" em ProposalList
2. onDelete callback dispara
3. API call `DELETE /api/v1/proposals/{id}`
4. Store remove proposta via `removeProposal(id)`
5. UI atualiza sem propostas deletadas

### 3. Criar Nova Proposta
1. Usuário clica "+ Nova Proposta" em QuickActions
2. Navegação para `/dashboard/proposals/new`
3. Formário de criação (componente futuro)

## Migração de Componentes Antigos

`/components/protected/Navbar.tsx` continua funcionando mas é **deprecated**. Use `DashboardNavbar` do novo sistema.

### Diferenças:
| Aspecto | Navbar (antigo) | DashboardNavbar (novo) |
|--------|---|---|
| **Ubicação** | `components/protected/` | `components/dashboard/` |
| **Props** | Nenhuma | Nenhuma |
| **Links** | Dashboard, Propostas | Dashboard, Propostas, Briefings |
| **Responsividade** | Não tem | Mobile-first (nav hidden em mobile) |
| **Estado** | useSessionStore | useSessionStore |

## Próximos Passos (Post-MVP)

1. **Integração com API Real**
   - Substituir mock data em `DashboardPage`
   - Lidar com erros de API
   - Retry logic para requests

2. **Filtros Avançados**
   - Dropdown de status em ProposalList
   - DateRange picker para criadas após X data
   - Search por cliente

3. **Ordenação**
   - Sorter columns em ProposalList (clicável)
   - Persist sort preference em localStorage

4. **Analytics**
   - Real-time stats (atualmente mock)
   - Gráficos históricos via `/api/v1/stats`

5. **Ações em Batch**
   - Checkboxes em ProposalList
   - Bulk delete/export

6. **WebSocket Updates**
   - Real-time quando outra aba cria proposta
   - Aprovação em tempo real

## Arquivos Alterados/Criados

### Novos
- `src/components/dashboard/DashboardNavbar.tsx`
- `src/components/dashboard/StatCard.tsx`
- `src/components/dashboard/StatsGrid.tsx`
- `src/components/dashboard/ProposalCard.tsx`
- `src/components/dashboard/ProposalList.tsx`
- `src/components/dashboard/QuickActions.tsx`
- `src/components/dashboard/RecentActivity.tsx`
- `src/components/dashboard/index.ts`
- `src/stores/useDashboardStore.ts`

### Modificados
- `src/app/dashboard/page.tsx` (refatorado de 46 linhas → 145 linhas)
- `src/app/dashboard/layout.tsx` (Navbar → DashboardNavbar)

## QA Checklist

- [x] Todos os componentes TypeScript compilam
- [x] Imports/exports com `@/` alias funcionam
- [x] StatsGrid responsivo (1/2/4 colunas)
- [x] ProposalList empty state mostra CTA
- [x] Loading skeletons sem erro
- [x] Datas formatadas em pt-BR
- [x] Status badges com cores corretas
- [x] QuickActions nav links funcionam
- [x] DashboardNavbar substitui Navbar
- [x] Zustand store inicializa corretamente

## Troubleshooting

### "Cannot find module @/components/dashboard"
- Verificar alias em `tsconfig.json` (deve ter `@` → `src`)
- Verificar que `index.ts` exporta todos os componentes

### ProposalList não renderiza propostas
- Verificar que array não é undefined/null
- PropsWithChildren default para `[]` em undefined

### Cores não aplicam (Tailwind)
- Verificar que Tailwind está rodando: `npm run dev`
- Verificar que arquivo `.tsx` está em `content` do `tailwind.config.ts`

### Mock data não aparece
- Verificar browser console para errors
- Verificar que `setProposals` foi chamado em useEffect

## Performance

- Memoização: não aplicada (componentes são pequenos)
- Skeleton loading: CSS `animate-pulse` (0 JS)
- Revalidation: via `useDashboardStore` (local state)
- Sem lazy loading (dashboard é rápido)

## Segurança

- Autenticação: guard em layout + middleware
- RBAC: futuro (atualmente role 'owner')
- CORS: configurado no api.ts
- XSS: Next.js sanitização built-in
- CSRF: JWT token via header Authorization

---

**Status:** ✅ Pronto para uso
**Última atualização:** 2026-03-25
