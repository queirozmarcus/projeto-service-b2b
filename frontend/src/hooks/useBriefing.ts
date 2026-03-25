/**
 * Hook que coordena o fluxo completo de briefing público.
 *
 * Responsabilidades:
 * 1. Fetch de perguntas no mount (uma única vez por token)
 * 2. Expor ações de navegação do stepper (via store)
 * 3. Submissão em batch + completion com tratamento de erro
 * 4. Retry em caso de falha de rede (sem mudar o estado das respostas)
 *
 * Contrato com os componentes:
 * - Componentes de UI chamam as actions do hook (não a API diretamente)
 * - Componentes leem estado do store (não do hook — apenas o retorno necessário)
 * - Erros são do tipo BriefingApiError: use `error.kind` no switch para mensagens
 *
 * Exemplo de uso:
 *   const { isLoading, error, submitAllAnswers } = useBriefing(token);
 *   const questions = useBriefingStore((s) => s.questions);
 *
 * Separação de concerns:
 *   - useBriefing  → operações assíncronas (API) + orquestração
 *   - useBriefingStore → estado síncrono (stepper, respostas, UI flags)
 */

import { useCallback, useEffect, useRef } from 'react';
import { briefingApi } from '@/lib/briefingApi';
import {
  useBriefingStore,
  selectAllRequiredAnswered,
} from '@/stores/useBriefingStore';
import type { BriefingApiError, CompletionResult } from '@/types/briefing';

// ---------------------------------------------------------------------------
// Tipos de retorno
// ---------------------------------------------------------------------------

export interface UseBriefingReturn {
  /** true durante qualquer operação async (fetch ou submit) */
  isLoading: boolean;
  /** true especificamente durante POST /complete */
  isCompleting: boolean;
  /** Erro normalizado da última operação. null se sem erro. */
  error: BriefingApiError | null;
  /** Resultado após completion bem-sucedido. null enquanto não completado. */
  completionResult: CompletionResult | null;
  /** true se todas as perguntas obrigatórias foram respondidas */
  canComplete: boolean;
  /**
   * Submete todas as respostas em batch e finaliza a sessão.
   *
   * Fluxo:
   * 1. POST /public/briefings/{token}/batch-answers (sem auth)
   * 2. POST /api/v1/briefing-sessions/{sessionId}/complete (com auth)
   *
   * Requer `sessionId` para o step 2 (UUID interno da sessão).
   * O `sessionId` deve ser obtido pela página server-side e passado ao componente.
   *
   * @throws never — erros são capturados e armazenados em `error`
   */
  submitAllAnswers: (sessionId: string) => Promise<void>;
  /**
   * Recarrega as perguntas em caso de falha de rede no fetch inicial.
   * Limpa o erro atual antes de tentar novamente.
   */
  retryFetchQuestions: () => Promise<void>;
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

/**
 * @param token Token público da sessão de briefing (da URL: /briefing/[token])
 */
export function useBriefing(token: string): UseBriefingReturn {
  const {
    isLoading,
    isCompleting,
    error,
    completionResult,
    answers,
    setQuestions,
    setLoading,
    setCompleting,
    setCompleted,
    setError,
    reset,
  } = useBriefingStore();

  // Derivado do store — não armazenado separadamente para evitar dessincronia
  const canComplete = useBriefingStore(selectAllRequiredAnswered);

  // Ref para evitar fetch duplicado em Strict Mode (React 18 monta/desmonta 2x em dev)
  const hasFetched = useRef(false);
  // Ref para o token anterior — detecta mudança de sessão
  const prevTokenRef = useRef<string | null>(null);

  // ---- Fetch de perguntas --------------------------------------------------

  const fetchQuestions = useCallback(
    async (forceReset = false) => {
      if (!token) return;

      if (forceReset) {
        reset();
        hasFetched.current = false;
      }

      if (hasFetched.current) return;
      hasFetched.current = true;

      setLoading(true);
      setError(null);

      try {
        const questions = await briefingApi.getQuestions(token);
        setQuestions(questions);
      } catch (err) {
        // err já é BriefingApiError (normalizado em briefingApi.ts)
        setError(err as BriefingApiError);
        hasFetched.current = false; // permite retry manual
      } finally {
        setLoading(false);
      }
    },
    [token, reset, setLoading, setError, setQuestions],
  );

  // Fetch inicial + reset quando o token mudar (novo briefing na mesma tab)
  useEffect(() => {
    const tokenChanged = prevTokenRef.current !== null && prevTokenRef.current !== token;
    prevTokenRef.current = token;

    fetchQuestions(tokenChanged);
  }, [token, fetchQuestions]);

  // ---- Submissão + completion ----------------------------------------------

  const submitAllAnswers = useCallback(
    async (sessionId: string): Promise<void> => {
      if (!token || !sessionId) return;

      // Constrói array de Answer a partir do Map de respostas
      const answersArray = Array.from(answers.entries()).map(([questionId, answerText]) => ({
        questionId,
        answerText,
      }));

      // Se não há respostas, o backend rejeitaria com 400 — falha silenciosa defensiva
      if (answersArray.length === 0) {
        setError({
          kind: 'validation',
          message: 'Responda ao menos uma pergunta antes de concluir.',
        });
        return;
      }

      setCompleting(true);
      setError(null);

      try {
        // Step 1: submete todas as respostas (endpoint público)
        await briefingApi.submitAnswersBatch(token, answersArray);

        // Step 2: finaliza a sessão (endpoint autenticado)
        const result = await briefingApi.completeBriefingSession(sessionId);

        setCompleted(result);
      } catch (err) {
        setError(err as BriefingApiError);
        // Não chama setCompleting(false) aqui — setCompleted já faz isso.
        // Se caiu no catch, garantimos reset manual do flag.
        setCompleting(false);
      }
    },
    [token, answers, setCompleting, setError, setCompleted],
  );

  // ---- Retry ---------------------------------------------------------------

  const retryFetchQuestions = useCallback(async (): Promise<void> => {
    hasFetched.current = false;
    await fetchQuestions();
  }, [fetchQuestions]);

  // ---- Return --------------------------------------------------------------

  return {
    isLoading,
    isCompleting,
    error,
    completionResult,
    canComplete,
    submitAllAnswers,
    retryFetchQuestions,
  };
}
