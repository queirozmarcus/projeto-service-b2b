'use client';

import { useState } from 'react';

interface QuestionStepperProps {
  currentIndex: number;
  totalQuestions: number;
  canComplete: boolean;
  onPrevious: () => void;
  onNext: () => void;
  onComplete: () => Promise<void>;
}

/**
 * Botões de navegação entre perguntas do briefing.
 *
 * Comportamento:
 * - Q1: "Previous" desabilitado
 * - Q1..Q(n-1): botão "Next →"
 * - Última pergunta: botão "Complete Briefing" (em vez de "Next")
 * - Durante completion: ambos os botões desabilitados, spinner no "Complete"
 *
 * O botão "Complete" é desabilitado se `canComplete` for false
 * (perguntas obrigatórias sem resposta).
 */
export function QuestionStepper({
  currentIndex,
  totalQuestions,
  canComplete,
  onPrevious,
  onNext,
  onComplete,
}: QuestionStepperProps) {
  const [isCompleting, setIsCompleting] = useState(false);

  const isFirst = currentIndex === 0;
  const isLast = currentIndex === totalQuestions - 1;

  const handleComplete = async () => {
    setIsCompleting(true);
    try {
      await onComplete();
    } finally {
      // onComplete pode navegar para CompletionSummary — se o componente
      // desmontar antes, o setState é no-op (React 18 não lança warning).
      setIsCompleting(false);
    }
  };

  return (
    <div className="flex items-center justify-between mt-8 pt-6 border-t border-secondary-200">
      {/* Botão Previous */}
      <button
        type="button"
        onClick={onPrevious}
        disabled={isFirst || isCompleting}
        aria-label="Go to previous question"
        className="
          inline-flex items-center gap-2 rounded-lg border border-secondary-300
          bg-white px-5 py-2.5 text-sm font-medium text-secondary-700
          hover:bg-secondary-50 hover:border-secondary-400
          disabled:cursor-not-allowed disabled:opacity-40
          transition-colors duration-150
          focus:outline-none focus:ring-2 focus:ring-primary-500
        "
      >
        <span aria-hidden="true">←</span>
        Previous
      </button>

      {/* Botão Next ou Complete */}
      {isLast ? (
        <button
          type="button"
          onClick={handleComplete}
          disabled={isCompleting || !canComplete}
          aria-label={isCompleting ? 'Submitting briefing...' : 'Complete briefing'}
          title={!canComplete ? 'Answer all required questions to complete' : undefined}
          className="
            inline-flex items-center gap-2 rounded-lg
            bg-green-600 px-5 py-2.5 text-sm font-semibold text-white
            hover:bg-green-700
            disabled:cursor-not-allowed disabled:opacity-50
            transition-colors duration-150
            focus:outline-none focus:ring-2 focus:ring-green-500
          "
        >
          {isCompleting ? (
            <>
              {/* Spinner inline SVG — sem dependência extra */}
              <svg
                className="h-4 w-4 animate-spin"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8v8H4z"
                />
              </svg>
              Submitting...
            </>
          ) : (
            <>
              Complete Briefing
              <span aria-hidden="true">✓</span>
            </>
          )}
        </button>
      ) : (
        <button
          type="button"
          onClick={onNext}
          disabled={isCompleting}
          aria-label="Go to next question"
          className="
            inline-flex items-center gap-2 rounded-lg
            bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white
            hover:bg-primary-700
            disabled:cursor-not-allowed disabled:opacity-50
            transition-colors duration-150
            focus:outline-none focus:ring-2 focus:ring-primary-500
          "
        >
          Next
          <span aria-hidden="true">→</span>
        </button>
      )}
    </div>
  );
}
