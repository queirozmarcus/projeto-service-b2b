'use client';

interface BriefingProgressProps {
  /** Índice 1-based da pergunta atual (ex: 1, 2, 3...) */
  current: number;
  /** Total de perguntas na sessão */
  total: number;
}

/**
 * Barra de progresso visual + contador textual para o stepper de briefing.
 *
 * Acessibilidade: usa role="progressbar" com aria-valuenow/min/max
 * para que leitores de tela anunciem o progresso corretamente.
 */
export function BriefingProgress({ current, total }: BriefingProgressProps) {
  const percentage = total > 0 ? Math.round((current / total) * 100) : 0;

  return (
    <div className="space-y-2">
      <div className="flex justify-between text-sm text-secondary-600">
        <span>
          Question <span className="font-medium text-secondary-800">{current}</span> of{' '}
          <span className="font-medium text-secondary-800">{total}</span>
        </span>
        <span className="font-medium text-primary-600">{percentage}%</span>
      </div>

      <div
        role="progressbar"
        aria-valuenow={percentage}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`Question ${current} of ${total}`}
        className="h-2 w-full overflow-hidden rounded-full bg-secondary-200"
      >
        <div
          className="h-full rounded-full bg-primary-500 transition-all duration-300 ease-in-out"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
