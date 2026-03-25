/**
 * Briefing API client — endpoints públicos (sem autenticação).
 *
 * Usa uma instância axios dedicada, sem os interceptors de auth da instância
 * principal (api.ts). Isso garante que nenhum Authorization header vaze para
 * endpoints públicos e que o fluxo de refresh não seja ativado desnecessariamente.
 *
 * Endpoints cobertos:
 *   GET  /public/briefings/{token}/questions       → Question[]
 *   POST /public/briefings/{token}/batch-answers   → 204 No Content
 *   POST /api/v1/briefing-sessions/{id}/complete   → CompletionResult (autenticado)
 *
 * Tratamento de erros:
 *   404 → BriefingApiError { kind: 'token_invalid' }
 *   409 → BriefingApiError { kind: 'session_completed' }
 *   400 → BriefingApiError { kind: 'validation' }
 *   5xx → BriefingApiError { kind: 'server_error' }
 *   sem resposta → BriefingApiError { kind: 'network' }
 */

import axios, { AxiosError } from 'axios';
import { env } from '@/env';
import type {
  Answer,
  BriefingApiError,
  CompletionResult,
  Question,
} from '@/types/briefing';

// ---------------------------------------------------------------------------
// Instância axios dedicada para endpoints públicos
// ---------------------------------------------------------------------------

/**
 * Instância sem interceptors de autenticação.
 * Nunca anexa Authorization header — necessário para endpoints públicos.
 */
const publicApi = axios.create({
  baseURL: env.apiUrl,
  headers: {
    'Content-Type': 'application/json',
  },
  // withCredentials false: endpoints públicos não usam cookie de sessão
  withCredentials: false,
});

// ---------------------------------------------------------------------------
// Normalização de erros
// ---------------------------------------------------------------------------

/**
 * Converte um AxiosError ou erro genérico em BriefingApiError tipado.
 * Garante que o chamador nunca receba um `unknown` cru — sempre um tipo
 * discriminado que pode ser tratado com switch/case sem type assertions.
 */
function normalizeBriefingError(err: unknown): BriefingApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError<{ detail?: string; message?: string }>;

    if (!axiosErr.response) {
      // Sem resposta: timeout, offline, CORS pré-flight bloqueado
      return { kind: 'network', message: 'Sem conexão com o servidor. Verifique sua internet.' };
    }

    const status = axiosErr.response.status;
    const serverMessage =
      axiosErr.response.data?.detail ??
      axiosErr.response.data?.message ??
      undefined;

    if (status === 404) {
      return {
        kind: 'token_invalid',
        message: serverMessage ?? 'Link de briefing inválido ou expirado.',
      };
    }

    if (status === 409) {
      return {
        kind: 'session_completed',
        message: serverMessage ?? 'Esta sessão de briefing já foi finalizada.',
      };
    }

    if (status === 400) {
      return {
        kind: 'validation',
        message: serverMessage ?? 'Dados inválidos. Verifique as respostas e tente novamente.',
      };
    }

    // 5xx e outros status inesperados
    return {
      kind: 'server_error',
      message: serverMessage ?? `Erro no servidor (${status}). Tente novamente em instantes.`,
    };
  }

  // Erro não-Axios (ex: erro de programação, TypeError)
  return {
    kind: 'server_error',
    message: 'Ocorreu um erro inesperado. Tente novamente.',
  };
}

// ---------------------------------------------------------------------------
// Shapes internas dos responses do backend
// ---------------------------------------------------------------------------

/** Shape exata de BriefingQuestionResponse.java */
interface RawQuestion {
  questionId: string;
  questionText: string;
  type: string;
  orderIndex: number;
  required: boolean;
}

/** Shape exata de BriefingCompletionResponse.java */
interface RawCompletionResult {
  completenessScore: number;
  status: string;
  message: string;
}

// ---------------------------------------------------------------------------
// API client
// ---------------------------------------------------------------------------

export const briefingApi = {
  /**
   * Carrega todas as perguntas da sessão de briefing identificada pelo token público.
   *
   * GET /public/briefings/{token}/questions
   *
   * As perguntas já vêm ordenadas por `orderIndex` (backend garante).
   * Não refaça o fetch — as perguntas são imutáveis para a sessão.
   *
   * @throws BriefingApiError (token_invalid | server_error | network)
   */
  async getQuestions(token: string): Promise<Question[]> {
    try {
      const response = await publicApi.get<RawQuestion[]>(
        `/public/briefings/${token}/questions`,
      );

      // Garante ordenação mesmo que o backend mude comportamento
      return response.data.slice().sort((a, b) => a.orderIndex - b.orderIndex);
    } catch (err) {
      throw normalizeBriefingError(err);
    }
  },

  /**
   * Envia todas as respostas coletadas de uma vez ao backend.
   *
   * POST /public/briefings/{token}/batch-answers → 204 No Content
   *
   * Idempotente: perguntas já respondidas são ignoradas pelo backend.
   * Respostas vazias são filtradas antes do envio — o backend rejeita
   * answerText vazio com 400.
   *
   * @throws BriefingApiError (token_invalid | session_completed | validation | server_error | network)
   */
  async submitAnswersBatch(token: string, answers: Answer[]): Promise<void> {
    // Filtra respostas em branco para não disparar 400 desnecessariamente.
    // O hook/componente já deve ter validado, mas esta camada é defensiva.
    const validAnswers = answers.filter((a) => a.answerText.trim().length > 0);

    try {
      await publicApi.post(`/public/briefings/${token}/batch-answers`, {
        answers: validAnswers,
      });
      // 204 No Content — sem body de resposta
    } catch (err) {
      throw normalizeBriefingError(err);
    }
  },

  /**
   * Finaliza a sessão de briefing e obtém o score de completude.
   *
   * POST /api/v1/briefing-sessions/{sessionId}/complete → CompletionResult
   *
   * IMPORTANTE: este endpoint é AUTENTICADO (requer JWT).
   * Usa a instância `api` principal (com interceptors de auth), não `publicApi`.
   * O `sessionId` é o UUID interno da sessão, obtido via BriefingSession.id.
   *
   * Separado do fluxo público: o cliente responde via token público;
   * o service provider finaliza via UUID autenticado no dashboard.
   *
   * @throws BriefingApiError (session_completed | server_error | network)
   */
  async completeBriefingSession(sessionId: string): Promise<CompletionResult> {
    // Import dinâmico para não criar dependência circular na árvore de módulos.
    // briefingApi (lib) → api (lib): seria circular se api.ts importasse briefingApi.
    const { default: api } = await import('@/lib/api');

    try {
      const response = await api.post<RawCompletionResult>(
        `/api/v1/briefing-sessions/${sessionId}/complete`,
      );

      return {
        completenessScore: response.data.completenessScore,
        status: 'COMPLETED',
        message: response.data.message,
      };
    } catch (err) {
      throw normalizeBriefingError(err);
    }
  },
};
