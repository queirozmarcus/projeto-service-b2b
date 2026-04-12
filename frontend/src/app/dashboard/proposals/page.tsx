'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { DocumentPlusIcon } from '@heroicons/react/24/outline';
import { ProposalList } from '@/components/dashboard/ProposalList';
import useDashboardStore from '@/stores/useDashboardStore';
import type { DashboardState } from '@/stores/useDashboardStore';

type StatusFilter = DashboardState['statusFilter'];

const FILTER_OPTIONS: { label: string; value: StatusFilter }[] = [
  { label: 'Todos',     value: 'ALL' },
  { label: 'Rascunho',  value: 'DRAFT' },
  { label: 'Publicado', value: 'PUBLISHED' },
  { label: 'Aprovado',  value: 'APPROVED' },
  { label: 'Rejeitado', value: 'REJECTED' },
];

export default function ProposalsPage() {
  const { statusFilter, isLoading, fetchError, setStatusFilter, getFilteredProposals, fetchProposals } =
    useDashboardStore();

  useEffect(() => {
    fetchProposals();
  }, [fetchProposals]);

  const filtered = getFilteredProposals();

  return (
    <div className="mx-auto max-w-7xl px-6 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-black text-ink-900">Propostas</h1>
          <p className="mt-1 text-sm text-secondary-500">
            {filtered.length} proposta{filtered.length !== 1 ? 's' : ''} encontrada
            {filtered.length !== 1 ? 's' : ''}
          </p>
        </div>
        <Link
          href="/dashboard/proposals/new"
          className="flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 hover:bg-primary-400 transition-colors shadow-sm"
        >
          <DocumentPlusIcon className="h-4 w-4" />
          Nova Proposta
        </Link>
      </div>

      {/* Error banner */}
      {fetchError && (
        <div className="rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {fetchError}
        </div>
      )}

      {/* Status filter */}
      <div className="flex flex-wrap gap-2">
        {FILTER_OPTIONS.map(({ label, value }) => (
          <button
            key={value}
            onClick={() => setStatusFilter(value)}
            className={
              statusFilter === value
                ? 'rounded-xl px-4 py-2 text-sm font-semibold bg-primary-500 text-ink-900 transition-colors'
                : 'rounded-xl px-4 py-2 text-sm font-semibold border border-secondary-200 bg-surface text-secondary-600 hover:bg-secondary-50 transition-colors'
            }
          >
            {label}
          </button>
        ))}
      </div>

      {/* Proposal list */}
      <ProposalList proposals={filtered} isLoading={isLoading} />
    </div>
  );
}
