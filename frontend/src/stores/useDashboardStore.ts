import { create } from 'zustand';
import { proposalApi } from '@/lib/proposalApi';
import type { Proposal, ProposalStatus, ListProposalsParams } from '@/types/proposal';

export interface DashboardState {
  proposals: Proposal[];
  statusFilter: 'ALL' | ProposalStatus;
  sortBy: 'date' | 'status' | 'client';
  isLoading: boolean;
  fetchError: string | null;

  setProposals: (proposals: Proposal[]) => void;
  setStatusFilter: (filter: DashboardState['statusFilter']) => void;
  setSortBy: (sortBy: DashboardState['sortBy']) => void;
  setLoading: (loading: boolean) => void;
  setFetchError: (error: string | null) => void;
  addProposal: (proposal: Proposal) => void;
  removeProposal: (id: string) => void;
  updateProposal: (id: string, updates: Partial<Proposal>) => void;

  fetchProposals: (params?: ListProposalsParams) => Promise<void>;
  deleteProposal: (id: string) => Promise<void>;

  getFilteredProposals: () => Proposal[];

  computeStats: () => Array<{
    id: string;
    label: string;
    value: string | number;
    trend: string;
    trendDirection: 'up' | 'down' | 'neutral';
  }>;

  computeRecentActivity: () => Array<{
    id: string;
    type: 'created' | 'approved' | 'sent' | 'updated';
    description: string;
    timestamp: string;
  }>;
}

const useDashboardStore = create<DashboardState>((set, get) => ({
  proposals: [],
  statusFilter: 'ALL',
  sortBy: 'date',
  isLoading: false,
  fetchError: null,

  setProposals: (proposals: Proposal[]) =>
    set({ proposals, fetchError: null }),

  setStatusFilter: (statusFilter: DashboardState['statusFilter']) =>
    set({ statusFilter }),

  setSortBy: (sortBy: DashboardState['sortBy']) => set({ sortBy }),

  setLoading: (isLoading: boolean) => set({ isLoading }),

  setFetchError: (fetchError: string | null) => set({ fetchError }),

  addProposal: (proposal: Proposal) =>
    set((state) => ({
      proposals: [proposal, ...state.proposals],
    })),

  removeProposal: (id: string) =>
    set((state) => ({
      proposals: state.proposals.filter((p) => p.id !== id),
    })),

  updateProposal: (id: string, updates: Partial<Proposal>) =>
    set((state) => ({
      proposals: state.proposals.map((p) =>
        p.id === id ? { ...p, ...updates } : p
      ),
    })),

  fetchProposals: async (params?: ListProposalsParams) => {
    const { setLoading, setProposals, setFetchError } = get();
    setLoading(true);
    try {
      const page = await proposalApi.list(params);
      setProposals(page.content);
      setFetchError(null);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar propostas.';
      setFetchError(message);
    } finally {
      setLoading(false);
    }
  },

  deleteProposal: async (id: string) => {
    const { removeProposal } = get();
    await proposalApi.remove(id);
    removeProposal(id);
  },

  computeStats: () => {
    const { proposals } = get();

    const active = proposals.filter(
      (p) => p.status === 'DRAFT' || p.status === 'PUBLISHED'
    ).length;

    // TODO: substituir por briefings com status COMPLETED via useBriefingSessionStore
    // quando o store for compartilhado ou o dado for exposto via selector
    const completed = proposals.filter((p) => p.status === 'APPROVED').length;

    const approved = proposals.filter((p) => p.status === 'APPROVED').length;
    const rejected = proposals.filter((p) => p.status === 'REJECTED').length;
    const approvalBase = approved + rejected;
    const approval =
      approvalBase === 0
        ? '—'
        : `${Math.round((approved / approvalBase) * 100)}%`;

    const clients = new Set(proposals.map((p) => p.clientId)).size;

    return [
      { id: 'active',    label: 'Propostas Ativas',   value: active,    trend: '', trendDirection: 'neutral' as const },
      { id: 'completed', label: 'Propostas Aprovadas', value: completed, trend: '', trendDirection: 'neutral' as const },
      { id: 'approval',  label: 'Taxa de Aprovação',   value: approval,  trend: '', trendDirection: 'neutral' as const },
      { id: 'clients',   label: 'Novos Clientes',      value: clients,   trend: '', trendDirection: 'neutral' as const },
    ];
  },

  computeRecentActivity: () => {
    const { proposals } = get();

    type ActivityType = 'created' | 'approved' | 'sent' | 'updated';
    const events: Array<{
      id: string;
      type: ActivityType;
      description: string;
      timestamp: string;
    }> = [];

    for (const proposal of proposals) {
      events.push({
        id: `${proposal.id}-created`,
        type: 'created',
        description: `Proposta "${proposal.proposalName}" criada`,
        timestamp: proposal.createdAt,
      });

      if (proposal.status === 'PUBLISHED') {
        events.push({
          id: `${proposal.id}-sent`,
          type: 'sent',
          description: `Proposta "${proposal.proposalName}" publicada para o cliente`,
          timestamp: proposal.updatedAt,
        });
      } else if (proposal.status === 'APPROVED') {
        events.push({
          id: `${proposal.id}-approved`,
          type: 'approved',
          description: `Proposta "${proposal.proposalName}" aprovada`,
          timestamp: proposal.updatedAt,
        });
      } else if (proposal.status === 'DRAFT' && proposal.scope !== null) {
        events.push({
          id: `${proposal.id}-updated`,
          type: 'updated',
          description: `Escopo da proposta "${proposal.proposalName}" atualizado`,
          timestamp: proposal.updatedAt,
        });
      }
    }

    return events
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 10);
  },

  getFilteredProposals: () => {
    const state = get();
    let filtered = state.proposals;

    if (state.statusFilter !== 'ALL') {
      filtered = filtered.filter((p) => p.status === state.statusFilter);
    }

    filtered = [...filtered].sort((a, b) => {
      switch (state.sortBy) {
        case 'client':
          return a.proposalName.localeCompare(b.proposalName);
        case 'status':
          return a.status.localeCompare(b.status);
        case 'date':
        default:
          return (
            new Date(b.createdAt).getTime() -
            new Date(a.createdAt).getTime()
          );
      }
    });

    return filtered;
  },
}));

export default useDashboardStore;
