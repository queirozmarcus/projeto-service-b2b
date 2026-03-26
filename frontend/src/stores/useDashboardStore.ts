import { create } from 'zustand';
import type { Proposal } from '@/components/dashboard/ProposalList';

export interface DashboardState {
  proposals: Proposal[];
  statusFilter: 'ALL' | 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  sortBy: 'date' | 'status' | 'client';
  isLoading: boolean;
  error: string | null;

  setProposals: (proposals: Proposal[]) => void;
  setStatusFilter: (filter: DashboardState['statusFilter']) => void;
  setSortBy: (sortBy: DashboardState['sortBy']) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  addProposal: (proposal: Proposal) => void;
  removeProposal: (id: string) => void;
  updateProposal: (id: string, proposal: Partial<Proposal>) => void;

  getFilteredProposals: () => Proposal[];
}

const useDashboardStore = create<DashboardState>((set, get) => ({
  proposals: [],
  statusFilter: 'ALL',
  sortBy: 'date',
  isLoading: false,
  error: null,

  setProposals: (proposals: Proposal[]) =>
    set({ proposals, error: null }),

  setStatusFilter: (statusFilter: DashboardState['statusFilter']) =>
    set({ statusFilter }),

  setSortBy: (sortBy: DashboardState['sortBy']) => set({ sortBy }),

  setLoading: (isLoading: boolean) => set({ isLoading }),

  setError: (error: string | null) => set({ error }),

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

  getFilteredProposals: () => {
    const state = get();
    let filtered = state.proposals;

    if (state.statusFilter !== 'ALL') {
      filtered = filtered.filter((p) => p.status === state.statusFilter);
    }

    // Sort proposals
    filtered.sort((a, b) => {
      switch (state.sortBy) {
        case 'client':
          return a.clientName.localeCompare(b.clientName);
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
