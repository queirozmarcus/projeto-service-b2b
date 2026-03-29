'use client';

import { useEffect } from 'react';
import useSessionStore from '@/stores/useSession';
import useDashboardStore from '@/stores/useDashboardStore';
import {
  StatsGrid,
  ProposalList,
  QuickActions,
  RecentActivity,
} from '@/components/dashboard';
import { DocumentPlusIcon } from '@heroicons/react/24/outline';

const mockProposals = [
  {
    id: 'p1',
    clientName: 'Acme Corp',
    serviceType: 'Social Media',
    status: 'APPROVED' as const,
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'p2',
    clientName: 'XYZ Design',
    serviceType: 'Landing Page',
    status: 'DRAFT' as const,
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'p3',
    clientName: 'Tech Startup',
    serviceType: 'Branding',
    status: 'SENT' as const,
    createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
  },
];

const mockStats = [
  {
    id: 'active',
    label: 'Propostas Ativas',
    value: 5,
    trend: '+2',
    trendDirection: 'up' as const,
  },
  {
    id: 'completed',
    label: 'Briefings Completos',
    value: 8,
    trend: '+3',
    trendDirection: 'up' as const,
  },
  {
    id: 'approval',
    label: 'Taxa de Aprovação',
    value: '87%',
    trend: '+5%',
    trendDirection: 'up' as const,
  },
  {
    id: 'clients',
    label: 'Novos Clientes',
    value: 3,
    trend: '+1',
    trendDirection: 'neutral' as const,
  },
];

const mockActivities = [
  {
    id: 'a1',
    type: 'approved' as const,
    description: 'Proposta de Acme Corp aprovada',
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'a2',
    type: 'sent' as const,
    description: 'Proposta enviada para Tech Startup',
    timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'a3',
    type: 'created' as const,
    description: 'Nova proposta criada para XYZ Design',
    timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
  },
];

export default function DashboardPage() {
  const { user } = useSessionStore();
  const { setProposals, isLoading } = useDashboardStore();

  useEffect(() => {
    setProposals(mockProposals);
  }, [setProposals]);

  const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bom dia';
    if (hour < 18) return 'Boa tarde';
    return 'Boa noite';
  };

  return (
    <div className="min-h-screen bg-canvas">
      <div className="mx-auto max-w-7xl px-6 py-8 space-y-8">

        {/* ── Page Header ──────────────────────────────────────────── */}
        <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-medium text-secondary-500">
              {greeting()}{user ? `, ${user.fullName.split(' ')[0]}` : ''}.
            </p>
            <h1 className="font-display text-3xl font-black text-ink-900 lg:text-4xl">
              Dashboard
            </h1>
          </div>

          {/* Primary action */}
          <a
            href="/dashboard/proposals/new"
            className="inline-flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-all hover:bg-primary-400 hover:shadow"
          >
            <DocumentPlusIcon className="h-4 w-4" />
            Nova Proposta
          </a>
        </div>

        {/* ── Stats ──────────────────────────────────────────────── */}
        <StatsGrid stats={mockStats} isLoading={isLoading} />

        {/* ── Quick Actions (secondary, below stats) ─────────────── */}
        <div>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-secondary-500">
            Ações Rápidas
          </h2>
          <QuickActions />
        </div>

        {/* ── Two-column: Proposals + Activity ───────────────────── */}
        <div className="grid gap-8 lg:grid-cols-[1fr_320px]">
          <div>
            <h2 className="mb-5 font-display text-xl font-black text-ink-900">
              Propostas Recentes
            </h2>
            <ProposalList proposals={mockProposals} isLoading={isLoading} />
          </div>

          <div>
            <h2 className="mb-5 font-display text-xl font-black text-ink-900">
              Atividade Recente
            </h2>
            <RecentActivity activities={mockActivities} isLoading={isLoading} />
          </div>
        </div>
      </div>
    </div>
  );
}
