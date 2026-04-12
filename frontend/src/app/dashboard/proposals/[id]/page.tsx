'use client';

import Link from 'next/link';
import { ChevronLeftIcon } from '@heroicons/react/20/solid';
import useDashboardStore from '@/stores/useDashboardStore';
import type { ProposalStatus } from '@/types/proposal';

const statusConfig: Record<ProposalStatus, { label: string; classes: string; dot: string }> = {
  DRAFT: {
    label: 'Rascunho',
    classes: 'bg-secondary-100 text-secondary-700',
    dot: 'bg-secondary-400',
  },
  PUBLISHED: {
    label: 'Publicado',
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

interface DetailCardProps {
  label: string;
  children: React.ReactNode;
}

function DetailCard({ label, children }: DetailCardProps) {
  return (
    <div className="rounded-2xl border border-secondary-200 bg-surface p-5 shadow-sm">
      <p className="mb-2 text-xs font-medium uppercase tracking-wide text-secondary-500">
        {label}
      </p>
      {children}
    </div>
  );
}

export default function ProposalDetailPage({
  params,
}: {
  params: { id: string };
}) {
  const { id } = params;
  const proposals = useDashboardStore((s) => s.proposals);
  const proposal = proposals.find((p) => p.id === id);

  if (!proposal) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-6 py-8">
        <p className="text-lg font-semibold text-ink-900">
          Proposta não encontrada
        </p>
        <p className="text-sm text-secondary-500">
          A proposta com o ID <span className="font-mono">{id}</span> não existe
          ou foi removida.
        </p>
        <Link
          href="/dashboard"
          className="mt-2 inline-flex items-center gap-1.5 rounded-xl bg-primary-500 px-4 py-2.5 text-sm font-semibold text-ink-900 transition-colors hover:bg-primary-400"
        >
          <ChevronLeftIcon className="h-4 w-4" />
          Voltar ao Dashboard
        </Link>
      </div>
    );
  }

  const config = statusConfig[proposal.status];
  const formattedDate = new Date(proposal.createdAt).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });

  return (
    <div className="mx-auto max-w-7xl px-6 py-8 space-y-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-1.5 text-sm text-secondary-500">
        <Link
          href="/dashboard"
          className="transition-colors hover:text-ink-900"
        >
          Dashboard
        </Link>
        <span>/</span>
        <span>Propostas</span>
        <span>/</span>
        <span className="truncate font-medium text-ink-900">
          {proposal.proposalName}
        </span>
      </nav>

      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <h1 className="font-display text-3xl font-bold text-ink-900">
            {proposal.proposalName}
          </h1>
          <span
            className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${config.classes}`}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
            {config.label}
          </span>
        </div>

        <Link
          href="/dashboard"
          className="inline-flex w-fit items-center gap-1.5 rounded-xl border border-secondary-200 px-4 py-2.5 text-sm font-medium text-secondary-600 shadow-sm transition-all hover:border-secondary-300 hover:bg-secondary-50"
        >
          <ChevronLeftIcon className="h-4 w-4" />
          Voltar
        </Link>
      </div>

      {/* Detail cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <DetailCard label="Nome da Proposta">
          <p className="text-sm font-semibold text-ink-900">{proposal.proposalName}</p>
        </DetailCard>

        <DetailCard label="Status">
          <span
            className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${config.classes}`}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
            {config.label}
          </span>
        </DetailCard>

        <DetailCard label="Data de Criação">
          <p className="text-sm font-semibold text-ink-900">{formattedDate}</p>
        </DetailCard>

        <DetailCard label="ID da Proposta">
          <p className="truncate font-mono text-xs text-secondary-600" title={proposal.id}>
            {proposal.id}
          </p>
        </DetailCard>
      </div>

      {/* Actions — only for DRAFT */}
      {proposal.status === 'DRAFT' && (
        <div className="rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm space-y-4">
          <h2 className="text-base font-semibold text-ink-900">Ações</h2>
          <div className="flex flex-wrap gap-3">
            <Link
              href="#"
              className="inline-flex items-center gap-2 rounded-xl border border-secondary-200 px-5 py-2.5 text-sm font-medium text-secondary-700 shadow-sm transition-all hover:border-secondary-300 hover:bg-secondary-50"
            >
              Editar
            </Link>
            <Link
              href="#"
              className="inline-flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-semibold text-ink-900 shadow-sm transition-colors hover:bg-primary-400"
            >
              Publicar Proposta
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
