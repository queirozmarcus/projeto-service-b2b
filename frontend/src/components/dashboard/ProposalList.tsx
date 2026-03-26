'use client';

import { useState } from 'react';
import Link from 'next/link';

export interface Proposal {
  id: string;
  clientName: string;
  serviceType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

interface ProposalListProps {
  proposals?: Proposal[];
  isLoading?: boolean;
  onDelete?: (id: string) => Promise<void>;
}

export function ProposalList({
  proposals = [],
  isLoading = false,
  onDelete,
}: ProposalListProps) {
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const statusConfig = {
    DRAFT: { label: 'Rascunho', color: 'bg-secondary-100 text-secondary-800' },
    SENT: { label: 'Enviado', color: 'bg-blue-100 text-blue-800' },
    APPROVED: {
      label: 'Aprovado',
      color: 'bg-green-100 text-green-800',
    },
    REJECTED: { label: 'Rejeitado', color: 'bg-red-100 text-red-800' },
  };

  const handleDelete = async (id: string) => {
    if (!onDelete) return;
    setDeletingId(id);
    try {
      await onDelete(id);
    } finally {
      setDeletingId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div
            key={i}
            className="rounded-xl border border-secondary-200 bg-white p-4 animate-pulse"
          >
            <div className="flex items-center gap-4">
              <div className="flex-1 space-y-2">
                <div className="h-4 w-32 rounded bg-secondary-200" />
                <div className="h-3 w-48 rounded bg-secondary-100" />
              </div>
              <div className="h-6 w-20 rounded bg-secondary-200" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (proposals.length === 0) {
    return (
      <div className="rounded-xl border border-secondary-200 bg-white p-12 text-center">
        <p className="text-secondary-600">
          Nenhuma proposta criada. Começe criando sua primeira!
        </p>
        <Link
          href="/dashboard/proposals/new"
          className="mt-4 inline-block rounded-lg bg-primary-600 px-6 py-2.5 text-sm font-medium text-white transition hover:bg-primary-700"
        >
          Nova Proposta
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {proposals.map((proposal) => {
        const config = statusConfig[proposal.status];
        const formattedDate = new Date(proposal.createdAt).toLocaleDateString(
          'pt-BR'
        );

        return (
          <div
            key={proposal.id}
            className="flex items-center justify-between rounded-xl border border-secondary-200 bg-white p-4 shadow-sm transition hover:shadow-md"
          >
            <div className="flex-1">
              <h3 className="font-semibold text-secondary-900">
                {proposal.clientName}
              </h3>
              <div className="mt-1 flex flex-col gap-1 text-sm text-secondary-600 sm:flex-row sm:gap-4">
                <span>{proposal.serviceType}</span>
                <span className="text-secondary-500">{formattedDate}</span>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <span className={`rounded-full px-3 py-1 text-xs font-medium ${config.color}`}>
                {config.label}
              </span>
              <div className="flex gap-2">
                <Link
                  href={`/dashboard/proposals/${proposal.id}`}
                  className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-primary-700"
                >
                  Ver
                </Link>
                <button
                  disabled={deletingId === proposal.id}
                  onClick={() => handleDelete(proposal.id)}
                  className="rounded-lg border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 transition hover:bg-red-50 disabled:opacity-50"
                >
                  {deletingId === proposal.id ? 'Deletando...' : 'Deletar'}
                </button>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
