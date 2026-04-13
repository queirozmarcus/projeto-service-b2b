'use client';

import Link from 'next/link';
import { ArrowRightIcon, PencilSquareIcon } from '@heroicons/react/20/solid';

interface ProposalCardProps {
  id: string;
  clientName: string;
  serviceType: string;
  status: 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

const statusConfig = {
  DRAFT: {
    label: 'Rascunho',
    classes: 'bg-secondary-100 text-secondary-700',
    dot: 'bg-secondary-400',
  },
  SENT: {
    label: 'Enviado',
    classes: 'bg-primary-50 text-primary-700',
    dot: 'bg-primary-500',
  },
  APPROVED: {
    label: 'Aprovado',
    classes: 'bg-emerald-50 text-emerald-700',
    dot: 'bg-emerald-500',
  },
  REJECTED: {
    label: 'Rejeitado',
    classes: 'bg-red-50 text-red-700',
    dot: 'bg-red-500',
  },
};

function ClientAvatar({ name }: { name: string }) {
  const initials = name
    .split(' ')
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('');

  const colors = [
    'bg-primary-600',
    'bg-ink-700',
    'bg-emerald-600',
    'bg-orange-600',
    'bg-violet-600',
  ];
  const idx = name.charCodeAt(0) % colors.length;

  return (
    <div
      className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-sm font-bold text-white ${colors[idx]}`}
    >
      {initials}
    </div>
  );
}

export function ProposalCard({
  id,
  clientName,
  serviceType,
  status,
  createdAt,
}: ProposalCardProps) {
  const config = statusConfig[status];
  const formattedDate = new Date(createdAt).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });

  return (
    <div className="group rounded-2xl border border-secondary-200 bg-surface p-5 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
      {/* Header */}
      <div className="flex items-start gap-3">
        <ClientAvatar name={clientName} />

        <div className="min-w-0 flex-1">
          <h3 className="truncate text-base font-semibold text-ink-800">{clientName}</h3>
          <p className="mt-0.5 truncate text-sm text-secondary-500">{serviceType}</p>
        </div>

        {/* Status badge */}
        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${config.classes}`}
        >
          <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
          {config.label}
        </span>
      </div>

      {/* Date */}
      <p className="mt-4 text-xs text-secondary-400">{formattedDate}</p>

      {/* Actions */}
      <div className="mt-4 flex gap-2 border-t border-secondary-100 pt-4">
        <Link
          href={`/dashboard/proposals/${id}`}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-primary-600 px-3 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-700"
        >
          Ver
          <ArrowRightIcon className="h-3.5 w-3.5 transition-transform duration-200 group-hover:translate-x-0.5" />
        </Link>
        <button className="flex items-center gap-1.5 rounded-xl border border-secondary-200 px-3 py-2.5 text-sm font-medium text-secondary-600 transition-all hover:border-secondary-300 hover:bg-secondary-50">
          <PencilSquareIcon className="h-4 w-4" />
          Editar
        </button>
      </div>
    </div>
  );
}
