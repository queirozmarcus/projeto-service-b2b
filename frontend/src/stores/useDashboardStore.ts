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
    const { removeProposal, setFetchError } = get();
    try {
      await proposalApi.remove(id);
      removeProposal(id);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao deletar proposta.';
      setFetchError(message);
    }
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
