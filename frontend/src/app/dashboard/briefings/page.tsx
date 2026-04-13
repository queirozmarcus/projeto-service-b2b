'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  ClipboardDocumentListIcon,
  LinkIcon,
} from '@heroicons/react/24/outline';
import type { BriefingSession, BriefingStatus } from '@/types/briefing';
import useBriefingSessionStore, {
  type BriefingSessionFilter,
} from '@/stores/useBriefingSessionStore';

// ---------------------------------------------------------------------------
// Status config
// ---------------------------------------------------------------------------

type StatusConfig = {
  label: string;
  dot: string;
  badge: string;
};

const STATUS_CONFIG: Record<BriefingStatus, StatusConfig> = {
  IN_PROGRESS: {
    label: 'Em Andamento',
    dot: 'bg-primary-500',
    badge: 'bg-primary-50 text-primary-700',
  },
  COMPLETED: {
    label: 'Concluído',
    dot: 'bg-emerald-500',
    badge: 'bg-emerald-50 text-emerald-700',
  },
  ABANDONED: {
    label: 'Abandonado',
    dot: 'bg-secondary-400',
    badge: 'bg-secondary-100 text-secondary-700',
  },
};

// ---------------------------------------------------------------------------
// Filter options
// ---------------------------------------------------------------------------

const FILTER_OPTIONS: { label: string; value: BriefingSessionFilter }[] = [
  { label: 'Todos', value: 'ALL' },
  { label: 'Em Andamento', value: 'IN_PROGRESS' },
  { label: 'Concluído', value: 'COMPLETED' },
  { label: 'Abandonado', value: 'ABANDONED' },
];

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function pluralize(count: number, singular: string, plural: string): string {
  return count === 1 ? singular : plural;
}

// ---------------------------------------------------------------------------
// Loading skeleton
// ---------------------------------------------------------------------------

function BriefingCardSkeleton() {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-secondary-200 bg-surface p-5 shadow-sm sm:flex-row sm:items-center sm:gap-6 animate-pulse">
      <div className="h-11 w-11 shrink-0 rounded-full bg-secondary-100" />
      <div className="min-w-0 flex-1 space-y-2">
        <div className="flex items-center gap-3">
          <div className="h-4 w-32 rounded bg-secondary-100" />
          <div className="h-5 w-20 rounded-full bg-secondary-100" />
        </div>
        <div className="h-1.5 w-32 rounded-full bg-secondary-100" />
        <div className="h-3 w-48 rounded bg-secondary-100" />
      </div>
      <div className="h-8 w-24 shrink-0 rounded-xl bg-secondary-100" />
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div className="space-y-4">
      {[1, 2, 3].map((i) => (
        <BriefingCardSkeleton key={i} />
      ))}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Error state
// ---------------------------------------------------------------------------

interface ErrorStateProps {
  message: string;
  onRetry: () => void;
}

function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-2xl border border-red-200 bg-red-50 py-12 text-center">
      <p className="text-sm font-medium text-red-700">{message}</p>
      <button
        onClick={onRetry}
        className="rounded-xl bg-red-100 px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-200 transition-colors"
      >
        Tentar novamente
      </button>
    </div>
  );
}

// ---------------------------------------------------------------------------
// BriefingCard
// ---------------------------------------------------------------------------

interface BriefingCardProps {
  briefing: BriefingSession;
}

function BriefingCard({ briefing }: BriefingCardProps) {
  const [copied, setCopied] = useState(false);
  const config = STATUS_CONFIG[briefing.status];

  async function handleCopyToken() {
    await navigator.clipboard.writeText(briefing.publicToken);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-secondary-200 bg-surface p-5 shadow-sm sm:flex-row sm:items-center sm:gap-6">
      {/* Avatar */}
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary-50">
        <ClipboardDocumentListIcon className="h-5 w-5 text-primary-600" />
      </div>

      {/* Main info */}
      <div className="min-w-0 flex-1 space-y-2">
        {/* Token + status badge */}
        <div className="flex flex-wrap items-center gap-3">
          <span className="font-mono text-xs text-ink-900 truncate max-w-[160px]">
            {briefing.publicToken}
          </span>
          <span
            className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${config.badge}`}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
            {config.label}
          </span>
        </div>

        {/* Completeness progress bar */}
        {briefing.completenessScore !== null ? (
          <div className="flex items-center gap-3">
            <div className="h-1.5 w-32 overflow-hidden rounded-full bg-secondary-100">
              <div
                className="h-full rounded-full bg-emerald-500 transition-all"
                style={{ width: `${briefing.completenessScore}%` }}
              />
            </div>
            <span className="text-xs font-semibold text-ink-900">
              {briefing.completenessScore}%
            </span>
          </div>
        ) : (
          <p className="text-xs text-secondary-500">Aguardando respostas</p>
        )}

        {/* Meta row */}
        <div className="flex flex-wrap items-center gap-4 text-xs text-secondary-500">
          <span>Criado em {formatDate(briefing.createdAt)}</span>
          {briefing.proposalId ? (
            <Link
              href={`/dashboard/proposals/${briefing.proposalId}`}
              className="font-medium text-primary-600 hover:underline"
            >
              Ver proposta vinculada
            </Link>
          ) : (
            <span className="text-secondary-400">Sem proposta vinculada</span>
          )}
        </div>
      </div>

      {/* Copy token action */}
      <button
        onClick={handleCopyToken}
        className="flex shrink-0 items-center gap-1.5 rounded-xl border border-secondary-200 bg-surface px-4 py-2 text-xs font-semibold text-secondary-600 shadow-sm transition-colors hover:bg-secondary-50"
        title="Copiar link do briefing"
      >
        <LinkIcon className="h-3.5 w-3.5" />
        {copied ? 'Copiado!' : 'Ver link'}
      </button>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

function EmptyState({ hasFilter }: { hasFilter: boolean }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-2xl border border-secondary-200 bg-surface py-16 text-center shadow-sm">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary-50">
        <ClipboardDocumentListIcon className="h-7 w-7 text-primary-500" />
      </div>
      <div>
        <p className="font-display text-base font-black text-ink-900">
          {hasFilter ? 'Nenhum briefing com este status' : 'Nenhum briefing encontrado'}
        </p>
        <p className="mt-1 text-sm text-secondary-500">
          {hasFilter
            ? 'Ajuste o filtro para ver outros briefings.'
            : 'Crie uma proposta e inicie um briefing para começar.'}
        </p>
      </div>
      {!hasFilter && (
        <Link
          href="/dashboard/proposals/new"
          className="flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-colors hover:bg-primary-400"
        >
          <ClipboardDocumentListIcon className="h-4 w-4" />
          Nova Proposta
        </Link>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function BriefingsPage() {
  const { isLoading, fetchError, statusFilter, setStatusFilter, fetchSessions, getFilteredSessions } =
    useBriefingSessionStore();

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  const filtered = getFilteredSessions();
  const count = filtered.length;
  const hasFilter = statusFilter !== 'ALL';

  return (
    <div className="mx-auto max-w-7xl px-6 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-black text-ink-900">
            Briefings
          </h1>
          <p className="mt-1 text-sm text-secondary-500">
            {isLoading
              ? 'Carregando...'
              : `${count} ${pluralize(count, 'briefing encontrado', 'briefings encontrados')}`}
          </p>
        </div>
        <Link
          href="/dashboard/proposals/new"
          className="flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-bold text-ink-900 shadow-sm transition-colors hover:bg-primary-400"
        >
          <ClipboardDocumentListIcon className="h-4 w-4" />
          Nova Proposta
        </Link>
      </div>

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

      {/* Content */}
      {isLoading ? (
        <LoadingSkeleton />
      ) : fetchError ? (
        <ErrorState message={fetchError} onRetry={fetchSessions} />
      ) : filtered.length === 0 ? (
        <EmptyState hasFilter={hasFilter} />
      ) : (
        <div className="space-y-4">
          {filtered.map((briefing) => (
            <BriefingCard key={briefing.id} briefing={briefing} />
          ))}
        </div>
      )}
    </div>
  );
}
