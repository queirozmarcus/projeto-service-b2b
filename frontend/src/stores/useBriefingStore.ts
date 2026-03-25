/**
 * Zustand store para o fluxo de briefing do cliente.
 *
 * Responsabilidades:
 * - Gerenciar o índice atual do stepper (qual pergunta está sendo exibida)
 * - Acumular respostas localmente antes do envio (sem chamadas API por resposta)
 * - Expor estado de loading, erro e resultado de completion
 *
 * O que este store NÃO faz:
 * - Não chama a API (isso é responsabilidade do hook `useBriefing`)
 * - Não persiste em localStorage (MVP: se o cliente fechar a aba, perde rascunho)
 * - Não valida regras de negócio (validação fica no hook)
 *
 * Seletores exportados separadamente para evitar re-renders desnecessários
 * em componentes que dependem apenas de uma fatia do estado.
 */

import { create } from 'zustand';
import type { AnswersMap, BriefingApiError, CompletionResult, Question } from '@/types/briefing';

// ---------------------------------------------------------------------------
// State shape
// ---------------------------------------------------------------------------

interface BriefingState {
  // ---- Data ----------------------------------------------------------------
  /** Perguntas carregadas do backend. Vazio enquanto carregando. */
  questions: Question[];
  /**
   * Respostas acumuladas localmente durante o fluxo.
   * Chave: Question.questionId | Valor: texto digitado pelo cliente.
   * Usamos Map (não objeto) para preservar ordem de inserção.
   */
  answers: AnswersMap;
  /** Índice da pergunta atualmente exibida no stepper (base 0). */
  currentIndex: number;
  /** Status da sessão — IN_PROGRESS enquanto o cliente responde. */
  status: 'IN_PROGRESS' | 'COMPLETED';
  /** Score calculado pelo backend após completion. null enquanto não completado. */
  completionResult: CompletionResult | null;

  // ---- UI ------------------------------------------------------------------
  /** true durante fetch de questions ou envio de respostas */
  isLoading: boolean;
  /** true especificamente durante o POST /complete (para UX separado) */
  isCompleting: boolean;
  /** Erro normalizado da última operação. null se não há erro. */
  error: BriefingApiError | null;

  // ---- Actions -------------------------------------------------------------
  /** Define as perguntas após fetch bem-sucedido. */
  setQuestions: (questions: Question[]) => void;
  /**
   * Registra ou atualiza a resposta de uma pergunta.
   * Aceita string vazia para limpar (ex: usuário apagou o campo).
   */
  addAnswer: (questionId: string, text: string) => void;
  /** Avança para a próxima pergunta. No-op se já está na última. */
  nextQuestion: () => void;
  /** Volta para a pergunta anterior. No-op se já está na primeira. */
  prevQuestion: () => void;
  /** Vai diretamente para o índice indicado. Usado pelo stepper de navegação. */
  goToQuestion: (index: number) => void;
  /** Ativa/desativa loading global (fetch de questions). */
  setLoading: (loading: boolean) => void;
  /** Ativa/desativa loading específico do completion. */
  setCompleting: (completing: boolean) => void;
  /** Armazena o resultado de completion e muda status para COMPLETED. */
  setCompleted: (result: CompletionResult) => void;
  /** Armazena o erro normalizado da última operação. */
  setError: (error: BriefingApiError | null) => void;
  /**
   * Reseta o store para o estado inicial.
   * Deve ser chamado ao navegar para um novo token de briefing.
   */
  reset: () => void;
}

// ---------------------------------------------------------------------------
// Estado inicial — extraído para facilitar o reset
// ---------------------------------------------------------------------------

const initialState = {
  questions: [] as Question[],
  answers: new Map<string, string>() as AnswersMap,
  currentIndex: 0,
  status: 'IN_PROGRESS' as const,
  completionResult: null,
  isLoading: false,
  isCompleting: false,
  error: null,
};

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

export const useBriefingStore = create<BriefingState>((set, get) => ({
  ...initialState,

  setQuestions: (questions) =>
    set({ questions, currentIndex: 0 }),

  addAnswer: (questionId, text) => {
    // Map é mutável; criar nova instância para disparar re-render corretamente.
    const next = new Map(get().answers);
    next.set(questionId, text);
    set({ answers: next });
  },

  nextQuestion: () => {
    const { currentIndex, questions } = get();
    if (currentIndex < questions.length - 1) {
      set({ currentIndex: currentIndex + 1 });
    }
  },

  prevQuestion: () => {
    const { currentIndex } = get();
    if (currentIndex > 0) {
      set({ currentIndex: currentIndex - 1 });
    }
  },

  goToQuestion: (index) => {
    const { questions } = get();
    if (index >= 0 && index < questions.length) {
      set({ currentIndex: index });
    }
  },

  setLoading: (isLoading) => set({ isLoading }),

  setCompleting: (isCompleting) => set({ isCompleting }),

  setCompleted: (result) =>
    set({ completionResult: result, status: 'COMPLETED', isCompleting: false }),

  setError: (error) => set({ error }),

  reset: () =>
    set({
      ...initialState,
      // Map precisa de nova instância — spread de initialState copia a referência
      answers: new Map<string, string>(),
    }),
}));

// ---------------------------------------------------------------------------
// Seletores granulares (evitam re-render em componentes de UI específicos)
// ---------------------------------------------------------------------------

/** Retorna apenas a pergunta atual. undefined se questions ainda vazio. */
export const selectCurrentQuestion = (s: BriefingState): Question | undefined =>
  s.questions[s.currentIndex];

/** Retorna a resposta da pergunta atual, ou string vazia se não respondida. */
export const selectCurrentAnswer = (s: BriefingState): string =>
  s.questions[s.currentIndex]
    ? (s.answers.get(s.questions[s.currentIndex].questionId) ?? '')
    : '';

/** true se o usuário pode avançar (não está na última pergunta). */
export const selectCanGoNext = (s: BriefingState): boolean =>
  s.currentIndex < s.questions.length - 1;

/** true se o usuário pode voltar (não está na primeira pergunta). */
export const selectCanGoPrev = (s: BriefingState): boolean =>
  s.currentIndex > 0;

/** true se o usuário está na última pergunta do fluxo. */
export const selectIsLastQuestion = (s: BriefingState): boolean =>
  s.questions.length > 0 && s.currentIndex === s.questions.length - 1;

/**
 * Número de perguntas obrigatórias respondidas.
 * Usado para validar antes de habilitar o botão "Concluir".
 */
export const selectRequiredAnsweredCount = (s: BriefingState): number =>
  s.questions.filter(
    (q) => q.required && (s.answers.get(q.questionId) ?? '').trim().length > 0,
  ).length;

/**
 * Total de perguntas obrigatórias.
 * Use em conjunto com selectRequiredAnsweredCount para barra de progresso.
 */
export const selectRequiredTotalCount = (s: BriefingState): number =>
  s.questions.filter((q) => q.required).length;

/**
 * true se todas as perguntas obrigatórias foram respondidas.
 * Habilita o botão "Concluir" no último step.
 */
export const selectAllRequiredAnswered = (s: BriefingState): boolean => {
  const total = selectRequiredTotalCount(s);
  if (total === 0) return true; // sem perguntas obrigatórias → pode concluir
  return selectRequiredAnsweredCount(s) === total;
};
