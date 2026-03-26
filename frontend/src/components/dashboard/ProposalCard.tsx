'use client';

import Link from 'next/link';

interface ProposalCardProps {
  id: string;
  clientName: string;
  serviceType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

export function ProposalCard({
  id,
  clientName,
  serviceType,
  status,
  createdAt,
}: ProposalCardProps) {
  const statusConfig = {
    DRAFT: { label: 'Rascunho', color: 'bg-secondary-100 text-secondary-800' },
    SENT: { label: 'Enviado', color: 'bg-blue-100 text-blue-800' },
    APPROVED: {
      label: 'Aprovado',
      color: 'bg-green-100 text-green-800',
    },
    REJECTED: { label: 'Rejeitado', color: 'bg-red-100 text-red-800' },
  };

  const config = statusConfig[status];
  const formattedDate = new Date(createdAt).toLocaleDateString('pt-BR');

  return (
    <div className="rounded-xl border border-secondary-200 bg-white p-5 shadow-sm transition hover:shadow-md">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-secondary-900">
            {clientName}
          </h3>
          <p className="mt-1 text-sm text-secondary-600">{serviceType}</p>
          <p className="mt-3 text-xs text-secondary-500">{formattedDate}</p>
        </div>
        <span
          className={`inline-block rounded-full px-3 py-1 text-xs font-medium ${config.color}`}
        >
          {config.label}
        </span>
      </div>

      <div className="mt-4 flex gap-2 pt-4 border-t border-secondary-100">
        <Link
          href={`/dashboard/proposals/${id}`}
          className="flex-1 rounded-lg bg-primary-600 px-3 py-2 text-center text-sm font-medium text-white transition hover:bg-primary-700"
        >
          Ver
        </Link>
        <button
          className="rounded-lg border border-secondary-300 px-3 py-2 text-sm font-medium text-secondary-700 transition hover:bg-secondary-50"
        >
          Editar
        </button>
      </div>
    </div>
  );
}
