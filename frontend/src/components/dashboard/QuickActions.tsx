'use client';

import Link from 'next/link';

interface QuickActionsProps {
  onNewProposal?: () => void;
  onNewBriefing?: () => void;
}

export function QuickActions({
  onNewProposal,
  onNewBriefing,
}: QuickActionsProps) {
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <Link
        href="/dashboard/proposals/new"
        onClick={(e) => {
          if (onNewProposal) {
            e.preventDefault();
            onNewProposal();
          }
        }}
        className="flex items-center justify-center rounded-lg bg-primary-600 px-6 py-3 text-center font-semibold text-white transition hover:bg-primary-700"
      >
        + Nova Proposta
      </Link>

      <button
        onClick={onNewBriefing}
        className="flex items-center justify-center rounded-lg border-2 border-primary-600 px-6 py-3 text-center font-semibold text-primary-600 transition hover:bg-primary-50"
      >
        + Novo Briefing
      </button>

      <Link
        href="/dashboard/clients"
        className="flex items-center justify-center rounded-lg border border-secondary-300 px-6 py-3 text-center font-semibold text-secondary-700 transition hover:bg-secondary-50"
      >
        Ver Clientes
      </Link>
    </div>
  );
}
