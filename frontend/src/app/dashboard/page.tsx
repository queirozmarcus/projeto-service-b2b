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

export default function DashboardPage() {
  const { user } = useSessionStore();
  const { isLoading, fetchError, fetchProposals, getFilteredProposals, computeStats, computeRecentActivity } = useDashboardStore();

  useEffect(() => {
    fetchProposals();
  }, [fetchProposals]);

  const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bom dia';
    if (hour < 18) return 'Boa tarde';
    return 'Boa noite';
  };

  const recentProposals = getFilteredProposals();
  const stats = computeStats();
  const recentActivity = computeRecentActivity();

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

        {/* ── Fetch error banner ─────────────────────────────────── */}
        {fetchError && (
          <div className="rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
            {fetchError}
          </div>
        )}

        {/* ── Stats ──────────────────────────────────────────────── */}
        <StatsGrid stats={stats} isLoading={isLoading} />

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
            <ProposalList proposals={recentProposals} isLoading={isLoading} />
          </div>

          <div>
            <h2 className="mb-5 font-display text-xl font-black text-ink-900">
              Atividade Recente
            </h2>
            <RecentActivity activities={recentActivity} isLoading={isLoading} />
          </div>
        </div>
      </div>
    </div>
  );
}
