'use client';

import Link from 'next/link';
import useSessionStore from '@/stores/useSession';
import type { CompletionResult } from '@/types/briefing';

interface CompletionSummaryProps {
  completionResult: CompletionResult;
  /** UUID da proposta — usado para montar o link no dashboard (/dashboard/proposals/{proposalId}). */
  proposalId: string;
}

/**
 * Tela exibida após completion bem-sucedido do briefing.
 *
 * Dois cenários:
 * 1. Cliente não autenticado (fluxo normal): agradecimento + mensagem de próximos passos.
 * 2. Usuário autenticado (provider testando): link direto para a proposal no dashboard.
 *
 * Score visual:
 * - >= 80%: verde (briefing completo e rico)
 * - < 80%: laranja (briefing aceito mas com lacunas)
 */
export function CompletionSummary({ completionResult, proposalId }: CompletionSummaryProps) {
  const isAuthenticated = useSessionStore((s) => s.isAuthenticated);
  const { completenessScore, message } = completionResult;

  const isHighScore = completenessScore >= 80;

  const scoreColorClass = isHighScore
    ? 'text-green-600'
    : 'text-orange-500';

  const scoreBadgeClass = isHighScore
    ? 'bg-green-50 border-green-200'
    : 'bg-orange-50 border-orange-200';

  const iconEmoji = isHighScore ? '🎉' : '👍';

  return (
    <div className="flex flex-col items-center text-center space-y-8 py-12 px-4">
      {/* Ícone / Emoji de confirmação */}
      <div className="text-6xl" role="img" aria-label={isHighScore ? 'Celebration' : 'Thumbs up'}>
        {iconEmoji}
      </div>

      {/* Título */}
      <div className="space-y-2">
        <h1 className="text-2xl font-bold text-secondary-900 md:text-3xl">
          Briefing Completed!
        </h1>
        <p className="text-secondary-600 max-w-md mx-auto">
          Thank you for taking the time to fill out your briefing. Your answers help us
          prepare an accurate and tailored proposal.
        </p>
      </div>

      {/* Score badge */}
      <div
        className={`
          inline-flex flex-col items-center rounded-2xl border-2 px-8 py-6
          ${scoreBadgeClass}
        `}
      >
        <span className="text-sm font-medium text-secondary-600 uppercase tracking-wide">
          Completeness Score
        </span>
        <span className={`text-6xl font-bold mt-1 ${scoreColorClass}`}>
          {completenessScore}%
        </span>
      </div>

      {/* Mensagem do backend */}
      <p className="text-base text-secondary-700 max-w-sm leading-relaxed">
        {message}
      </p>

      {/* CTA condicional */}
      {isAuthenticated ? (
        <Link
          href={`/dashboard/proposals/${proposalId}`}
          className="
            inline-flex items-center gap-2 rounded-lg
            bg-primary-600 px-6 py-3 text-sm font-semibold text-white
            hover:bg-primary-700 transition-colors duration-150
            focus:outline-none focus:ring-2 focus:ring-primary-500
          "
        >
          Review Proposal in Dashboard
          <span aria-hidden="true">→</span>
        </Link>
      ) : (
        <div className="rounded-lg bg-secondary-50 border border-secondary-200 px-6 py-4 max-w-sm">
          <p className="text-sm text-secondary-600">
            Our team will review your briefing and prepare a detailed proposal.
            We&apos;ll reach out shortly.
          </p>
        </div>
      )}
    </div>
  );
}
