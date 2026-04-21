'use client';

import { use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ChevronLeftIcon, TrashIcon } from '@heroicons/react/20/solid';
import useDashboardStore from '@/stores/useDashboardStore';
import { proposalApi } from '@/lib/proposalApi';
import type { Proposal, ProposalApiError, ProposalStatus } from '@/types/proposal';

// ---------------------------------------------------------------------------
// Status config
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function ProposalDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();

  const updateProposal = useDashboardStore((s) => s.updateProposal);
  const removeProposal = useDashboardStore((s) => s.removeProposal);

  const [proposal, setProposal] = useState<Proposal | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const [isPublishing, setIsPublishing] = useState(false);
  const [publishError, setPublishError] = useState<string | null>(null);

  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  // ---------------------------------------------------------------------------
  // Fetch proposal by ID
  // ---------------------------------------------------------------------------

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setFetchError(null);
      try {
        const data = await proposalApi.getById(id);
        if (!cancelled) setProposal(data);
      } catch (err) {
        if (!cancelled) {
          const apiErr = err as ProposalApiError;
          setFetchError(apiErr.message ?? 'Erro ao carregar proposta.');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => { cancelled = true; };
  }, [id]);

  // ---------------------------------------------------------------------------
  // Publish
  // ---------------------------------------------------------------------------

  async function handlePublish() {
    if (!proposal) return;
    setIsPublishing(true);
    setPublishError(null);
    try {
      const updated = await proposalApi.publish(proposal.id);
      setProposal(updated);
      updateProposal(updated.id, { status: updated.status, updatedAt: updated.updatedAt });
    } catch (err) {
      const apiErr = err as ProposalApiError;
      setPublishError(apiErr.message ?? 'Erro ao publicar proposta.');
    } finally {
      setIsPublishing(false);
    }
  }

  // ---------------------------------------------------------------------------
  // Delete
  // ---------------------------------------------------------------------------

  async function handleDelete() {
    if (!proposal) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await proposalApi.remove(proposal.id);
      removeProposal(proposal.id);
      router.push('/dashboard/proposals');
    } catch (err) {
      const apiErr = err as ProposalApiError;
      setDeleteError(apiErr.message ?? 'Erro ao deletar proposta.');
      setIsDeleting(false);
      setConfirmDelete(false);
    }
  }

  // ---------------------------------------------------------------------------
  // Render — loading
  // ---------------------------------------------------------------------------

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-6 py-8 space-y-6">
        <div className="h-5 w-48 animate-pulse rounded-lg bg-secondary-100" />
        <div className="h-10 w-72 animate-pulse rounded-xl bg-secondary-100" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-20 animate-pulse rounded-2xl bg-secondary-100" />
          ))}
        </div>
      </div>
    );
  }

  // ---------------------------------------------------------------------------
  // Render — error (fetch)
  // ---------------------------------------------------------------------------

  if (fetchError || !proposal) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-6 py-8">
        <p className="text-lg font-semibold text-ink-900">
          {fetchError ?? 'Proposta não encontrada'}
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

  // ---------------------------------------------------------------------------
  // Render — proposal detail
  // ---------------------------------------------------------------------------

  const config = statusConfig[proposal.status];
  const formattedDate = new Date(proposal.createdAt).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
  const formattedUpdated = new Date(proposal.updatedAt).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });

  return (
    <div className="mx-auto max-w-7xl px-6 py-8 space-y-8">

      {/* Breadcrumb */}
      <nav className="flex items-center gap-1.5 text-sm text-secondary-500">
        <Link href="/dashboard" className="transition-colors hover:text-ink-900">
          Dashboard
        </Link>
        <span>/</span>
        <Link href="/dashboard/proposals" className="transition-colors hover:text-ink-900">
          Propostas
        </Link>
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
          href="/dashboard/proposals"
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

        <DetailCard label="Criado em">
          <p className="text-sm font-semibold text-ink-900">{formattedDate}</p>
        </DetailCard>

        <DetailCard label="Atualizado em">
          <p className="text-sm font-semibold text-ink-900">{formattedUpdated}</p>
        </DetailCard>
      </div>

      {/* Scope section — only if scope exists */}
      {proposal.scope && (
        <div className="rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm space-y-5">
          <h2 className="text-base font-semibold text-ink-900">Escopo</h2>

          {proposal.scope.deliverables.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-medium uppercase tracking-wide text-secondary-500">
                Entregáveis
              </p>
              <ul className="divide-y divide-secondary-100">
                {proposal.scope.deliverables.map((d, i) => (
                  <li key={i} className="py-3">
                    <p className="text-sm font-semibold text-ink-900">{d.name}</p>
                    <p className="text-sm text-secondary-600">{d.description}</p>
                    {d.acceptanceCriteria && (
                      <p className="mt-1 text-xs text-secondary-500">
                        Critério: {d.acceptanceCriteria}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {proposal.scope.price && (
            <div className="flex items-center gap-3">
              <span className="text-xs font-medium uppercase tracking-wide text-secondary-500">
                Preço
              </span>
              <span className="text-sm font-bold text-ink-900">
                {proposal.scope.price.currency}{' '}
                {proposal.scope.price.amount.toLocaleString('pt-BR')}
              </span>
            </div>
          )}

          {proposal.scope.timeline && (
            <div className="flex items-center gap-3">
              <span className="text-xs font-medium uppercase tracking-wide text-secondary-500">
                Prazo
              </span>
              <span className="text-sm text-ink-900">
                {new Date(proposal.scope.timeline.startDate).toLocaleDateString('pt-BR')}{' '}
                →{' '}
                {new Date(proposal.scope.timeline.endDate).toLocaleDateString('pt-BR')}
              </span>
            </div>
          )}

          {proposal.scope.exclusions.length > 0 && (
            <div className="space-y-1">
              <p className="text-xs font-medium uppercase tracking-wide text-secondary-500">
                Exclusões
              </p>
              <ul className="list-inside list-disc space-y-1 text-sm text-secondary-600">
                {proposal.scope.exclusions.map((e, i) => (
                  <li key={i}>{e}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Actions — DRAFT: publish + delete */}
      {proposal.status === 'DRAFT' && (
        <div className="rounded-2xl border border-secondary-200 bg-surface p-6 shadow-sm space-y-4">
          <h2 className="text-base font-semibold text-ink-900">Ações</h2>

          {publishError && (
            <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
              {publishError}
            </p>
          )}

          {deleteError && (
            <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
              {deleteError}
            </p>
          )}

          <div className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center gap-3">
              {/* Publish */}
              <button
                onClick={handlePublish}
                disabled={isPublishing || isDeleting || !proposal.scope}
                className="inline-flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-semibold text-ink-900 shadow-sm transition-colors hover:bg-primary-400 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isPublishing ? 'Publicando…' : 'Publicar Proposta'}
              </button>

              {/* Delete — two-step confirm */}
              {!confirmDelete ? (
                <button
                  onClick={() => setConfirmDelete(true)}
                  disabled={isPublishing || isDeleting}
                  className="inline-flex items-center gap-2 rounded-xl border border-red-200 px-5 py-2.5 text-sm font-medium text-red-600 shadow-sm transition-all hover:border-red-300 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <TrashIcon className="h-4 w-4" />
                  Deletar
                </button>
              ) : (
                <div className="flex items-center gap-2">
                  <span className="text-sm text-secondary-600">Confirmar exclusão?</span>
                  <button
                    onClick={handleDelete}
                    disabled={isDeleting}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-red-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {isDeleting ? 'Deletando…' : 'Sim, deletar'}
                  </button>
                  <button
                    onClick={() => setConfirmDelete(false)}
                    disabled={isDeleting}
                    className="rounded-xl border border-secondary-200 px-4 py-2 text-sm font-medium text-secondary-600 transition-all hover:bg-secondary-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Cancelar
                  </button>
                </div>
              )}
            </div>

            {/* Help text when scope is missing */}
            {!proposal.scope && (
              <div className="rounded-xl bg-amber-50 border border-amber-200 px-4 py-3">
                <p className="text-sm text-amber-800">
                  <span className="font-semibold">Para publicar esta proposta:</span> defina o escopo com entregáveis, preço e prazo através do endpoint <code className="font-mono text-xs bg-amber-100 px-1.5 py-0.5 rounded">POST /proposals/{proposal.id}/update-scope</code>
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* IDs técnicos — colapsados no rodapé */}
      <details className="group rounded-2xl border border-secondary-100 bg-canvas p-4">
        <summary className="cursor-pointer list-none text-xs font-medium text-secondary-400 select-none group-open:mb-3">
          IDs técnicos ▾
        </summary>
        <dl className="space-y-2 text-xs text-secondary-500">
          <div className="flex gap-2">
            <dt className="w-24 shrink-0 font-medium">Proposta</dt>
            <dd className="truncate font-mono text-secondary-600">{proposal.id}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-24 shrink-0 font-medium">Briefing</dt>
            <dd className="truncate font-mono text-secondary-600">{proposal.briefingId}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-24 shrink-0 font-medium">Cliente</dt>
            <dd className="truncate font-mono text-secondary-600">{proposal.clientId}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-24 shrink-0 font-medium">Workspace</dt>
            <dd className="truncate font-mono text-secondary-600">{proposal.workspaceId}</dd>
          </div>
        </dl>
      </details>
    </div>
  );
}
