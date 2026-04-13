'use client';

import { useBriefing } from '@/hooks/useBriefing';
import {
  useBriefingStore,
  selectCurrentQuestion,
  selectCurrentAnswer,
  selectAllRequiredAnswered,
} from '@/stores/useBriefingStore';
import { BriefingProgress } from './BriefingProgress';
import { QuestionCard } from './QuestionCard';
import { QuestionStepper } from './QuestionStepper';
import { CompletionSummary } from './CompletionSummary';
import type { BriefingApiError } from '@/types/briefing';

interface BriefingFlowProps {
  /** Token público da sessão (vem da URL: /briefing/[token]) */
  token: string;
}

// ---------------------------------------------------------------------------
// Sub-componentes de estado (loading / error)
// ---------------------------------------------------------------------------

function LoadingSkeletonStepper() {
  return (
    <div className="animate-pulse space-y-6" aria-label="Loading briefing questions">
      {/* Barra de progresso skeleton */}
      <div className="space-y-2">
        <div className="flex justify-between">
          <div className="h-4 w-32 rounded bg-secondary-200" />
          <div className="h-4 w-10 rounded bg-secondary-200" />
        </div>
        <div className="h-2 w-full rounded-full bg-secondary-200" />
      </div>

      {/* Pergunta skeleton */}
      <div className="space-y-3 my-8">
        <div className="h-5 w-3/4 rounded bg-secondary-200" />
        <div className="h-10 w-full rounded-lg bg-secondary-200" />
      </div>

      {/* Botões skeleton */}
      <div className="flex justify-between mt-8 pt-6 border-t border-secondary-200">
        <div className="h-10 w-28 rounded-lg bg-secondary-200" />
        <div className="h-10 w-28 rounded-lg bg-secondary-200" />
      </div>
    </div>
  );
}

interface ErrorBannerProps {
  error: BriefingApiError;
  onRetry: () => void;
}

function ErrorBanner({ error, onRetry }: ErrorBannerProps) {
  const isNetworkError = error.kind === 'network';
  const isInvalidToken = error.kind === 'token_invalid';

  return (
    <div
      role="alert"
      className="rounded-xl border border-red-200 bg-red-50 px-6 py-8 text-center space-y-4"
    >
      <div className="text-4xl" aria-hidden="true">
        {isInvalidToken ? '🔗' : '⚠️'}
      </div>
      <h2 className="text-lg font-semibold text-red-800">
        {isInvalidToken ? 'Invalid Briefing Link' : 'Something went wrong'}
      </h2>
      <p className="text-sm text-red-700 max-w-sm mx-auto">{error.message}</p>

      {/* Só mostrar retry para erros recuperáveis */}
      {(isNetworkError || error.kind === 'server_error') && (
        <button
          type="button"
          onClick={onRetry}
          className="
            inline-flex items-center gap-2 rounded-lg
            bg-red-600 px-5 py-2.5 text-sm font-semibold text-white
            hover:bg-red-700 transition-colors duration-150
            focus:outline-none focus:ring-2 focus:ring-red-500
          "
        >
          Try Again
        </button>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Componente principal
// ---------------------------------------------------------------------------

/**
 * Orquestrador do fluxo de briefing público.
 *
 * Responsabilidades:
 * - Dispara o fetch de perguntas via useBriefing (efeito interno do hook)
 * - Lê estado granular do useBriefingStore via seletores para evitar re-renders
 * - Delega rendering de cada fase para subcomponentes especializados
 * - Coordena a transação final: submitAllAnswers(sessionId)
 *
 * Fluxo de estados:
 * 1. isLoading + sem perguntas → LoadingSkeletonStepper
 * 2. error + sem perguntas    → ErrorBanner (com retry se recuperável)
 * 3. completionResult         → CompletionSummary
 * 4. padrão                   → BriefingProgress + QuestionCard + QuestionStepper
 */
export function BriefingFlow({ token }: BriefingFlowProps) {
  // ---- Hook: operações assíncronas -----------------------------------------
  const { isLoading, isCompleting, error, completionResult, canComplete, submitAllAnswers, retryFetchQuestions } =
    useBriefing(token);

  // ---- Store: estado síncrono — seletores granulares -----------------------
  const questions = useBriefingStore((s) => s.questions);
  const currentIndex = useBriefingStore((s) => s.currentIndex);
  const currentQuestion = useBriefingStore(selectCurrentQuestion);
  const currentAnswer = useBriefingStore(selectCurrentAnswer);
  const allRequiredAnswered = useBriefingStore(selectAllRequiredAnswered);

  const { nextQuestion, prevQuestion, addAnswer } = useBriefingStore((s) => ({
    nextQuestion: s.nextQuestion,
    prevQuestion: s.prevQuestion,
    addAnswer: s.addAnswer,
  }));

  // ---- Handlers ------------------------------------------------------------

  const handleAnswerChange = (text: string) => {
    if (!currentQuestion) return;
    addAnswer(currentQuestion.questionId, text);
  };

  const handleComplete = async () => {
    await submitAllAnswers();
  };

  // ---- Render: fase de carregamento inicial --------------------------------
  if (isLoading && questions.length === 0) {
    return <LoadingSkeletonStepper />;
  }

  // ---- Render: erro sem perguntas (fetch inicial falhou) -------------------
  if (error && questions.length === 0) {
    return <ErrorBanner error={error} onRetry={retryFetchQuestions} />;
  }

  // ---- Render: briefing concluído -----------------------------------------
  if (completionResult) {
    return <CompletionSummary completionResult={completionResult} />;
  }

  // ---- Render: guard — questions ainda vazias (edge case) -----------------
  if (questions.length === 0 || !currentQuestion) {
    return null;
  }

  // ---- Render: fluxo principal --------------------------------------------
  return (
    <div className="space-y-0">
      <BriefingProgress current={currentIndex + 1} total={questions.length} />

      <QuestionCard
        question={currentQuestion}
        answer={currentAnswer}
        onAnswerChange={handleAnswerChange}
        isLoading={isLoading || isCompleting}
      />

      {/* Erro durante submit (perguntas já carregadas — não remonta o flow) */}
      {error && (
        <div
          role="alert"
          className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
        >
          {error.message}
        </div>
      )}

      <QuestionStepper
        currentIndex={currentIndex}
        totalQuestions={questions.length}
        canComplete={canComplete && allRequiredAnswered}
        onPrevious={prevQuestion}
        onNext={nextQuestion}
        onComplete={handleComplete}
      />
    </div>
  );
}
