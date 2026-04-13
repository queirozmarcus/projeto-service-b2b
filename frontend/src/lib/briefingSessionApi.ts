/**
 * Briefing Session API client — endpoints autenticados (dashboard do service provider).
 *
 * Usa a instância `api` principal com interceptors de JWT (Authorization header
 * injetado automaticamente, refresh proativo e redirect em 401).
 *
 * Endpoints cobertos:
 *   POST /api/v1/proposals/{proposalId}/briefing-sessions → BriefingSession (201)
 *   GET  /api/v1/briefing-sessions/{id}                  → BriefingSession
 *   GET  /api/v1/briefing-sessions/token/{token}         → BriefingSession
 *   GET  /api/v1/briefing-sessions/{id}/questions        → Question[]
 *   POST /api/v1/briefing-sessions/{id}/complete         → CompletionResult
 *
 * Todos os endpoints requerem JWT válido. Requisições sem token são redirecionadas
 * para /auth/login pelo interceptor de response de api.ts.
 *
 * Tratamento de erros:
 *   404 → BriefingApiError { kind: 'token_invalid' }
 *   409 → BriefingApiError { kind: 'session_completed' }
 *   403 → BriefingApiError { kind: 'server_error' }   (BriefingApiError não tem 'forbidden')
 *   400 → BriefingApiError { kind: 'validation' }
 *   5xx → BriefingApiError { kind: 'server_error' }
 *   sem resposta → BriefingApiError { kind: 'network' }
 */

import axios, { AxiosError } from 'axios';
import api from '@/lib/api';
import type {
  BriefingApiError,
  BriefingSession,
  CompletionResult,
  Question,
} from '@/types/briefing';

// ---------------------------------------------------------------------------
// Normalização de erros
// ---------------------------------------------------------------------------

/**
 * Converte um AxiosError ou erro genérico em BriefingApiError tipado.
 *
 * Mapeia status HTTP para os kinds disponíveis no union type BriefingApiError.
 * 403 é mapeado para 'server_error' porque BriefingApiError não tem kind 'forbidden'.
 */
function normalizeBriefingSessionError(err: unknown): BriefingApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError<{ detail?: string; message?: string }>;

    if (!axiosErr.response) {
      // Sem resposta: timeout, offline, CORS pré-flight bloqueado
      return {
        kind: 'network',
        message: 'Sem conexão com o servidor. Verifique sua internet.',
      };
    }

    const status = axiosErr.response.status;
    const serverMessage =
      axiosErr.response.data?.detail ??
      axiosErr.response.data?.message ??
      undefined;

    if (status === 404) {
      return {
        kind: 'token_invalid',
        message: serverMessage ?? 'Sessão de briefing não encontrada.',
      };
    }

    if (status === 409) {
      return {
        kind: 'session_completed',
        message: serverMessage ?? 'Esta sessão de briefing já foi finalizada.',
      };
    }

    if (status === 403) {
      return {
        kind: 'server_error',
        message: serverMessage ?? 'Acesso negado. Esta sessão pertence a outro workspace.',
      };
    }

    if (status === 400) {
      return {
        kind: 'validation',
        message: serverMessage ?? 'Dados inválidos. Verifique as informações e tente novamente.',
      };
    }

    // 5xx e outros status inesperados
    return {
      kind: 'server_error',
      message: serverMessage ?? `Erro no servidor (${status}). Tente novamente em instantes.`,
    };
  }

  // Erro não-Axios (ex: TypeError, erro de programação)
  return {
    kind: 'server_error',
    message: 'Ocorreu um erro inesperado. Tente novamente.',
  };
}

// ---------------------------------------------------------------------------
// Shapes internas dos responses do backend
// ---------------------------------------------------------------------------

/** Shape exata de BriefingSessionResponse.java */
interface RawBriefingSession {
  id: string;
  proposalId: string | null;
  status: string;
  publicToken: string;
  completenessScore: number | null;
  createdAt: string;
  updatedAt: string;
}

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

export const briefingSessionApi = {
  /**
   * Cria uma nova sessão de briefing vinculada a uma proposta existente.
   *
   * POST /api/v1/proposals/{proposalId}/briefing-sessions → 201 Created
   *
   * A proposta deve existir e pertencer ao workspace autenticado.
   * A sessão é criada em status IN_PROGRESS com um publicToken gerado
   * automaticamente pelo backend — use-o para compartilhar o link com o cliente.
   *
   * @throws BriefingApiError (validation | server_error | network)
   */
  async createForProposal(proposalId: string): Promise<BriefingSession> {
    try {
      const response = await api.post<RawBriefingSession>(
        `/api/v1/proposals/${proposalId}/briefing-sessions`,
      );

      return response.data as BriefingSession;
    } catch (err) {
      throw normalizeBriefingSessionError(err);
    }
  },

  /**
   * Busca uma sessão de briefing pelo UUID interno.
   *
   * GET /api/v1/briefing-sessions/{id}
   *
   * Retorna 403 se a sessão pertencer a um workspace diferente do autenticado.
   * Use este endpoint quando você já tem o UUID da sessão (ex: vindo de uma proposta).
   *
   * @throws BriefingApiError (token_invalid | server_error | network)
   */
  async getById(id: string): Promise<BriefingSession> {
    try {
      const response = await api.get<RawBriefingSession>(
        `/api/v1/briefing-sessions/${id}`,
      );

      return response.data as BriefingSession;
    } catch (err) {
      throw normalizeBriefingSessionError(err);
    }
  },

  /**
   * Busca uma sessão de briefing pelo token público (acesso autenticado).
   *
   * GET /api/v1/briefing-sessions/token/{token}
   *
   * Útil para o service provider verificar o estado de uma sessão a partir do
   * link que foi compartilhado com o cliente (ex: exibir status no dashboard).
   * Difere do endpoint público em /public/briefings/{token}: este valida o JWT
   * e retorna 403 se a sessão não pertencer ao workspace autenticado.
   *
   * @throws BriefingApiError (token_invalid | server_error | network)
   */
  async getByPublicToken(token: string): Promise<BriefingSession> {
    try {
      const response = await api.get<RawBriefingSession>(
        `/api/v1/briefing-sessions/token/${token}`,
      );

      return response.data as BriefingSession;
    } catch (err) {
      throw normalizeBriefingSessionError(err);
    }
  },

  /**
   * Retorna as perguntas do template de serviço configurado para a sessão.
   *
   * GET /api/v1/briefing-sessions/{id}/questions
   *
   * As perguntas já vêm ordenadas por `orderIndex` (backend garante).
   * Retorna lista vazia se nenhum profile de serviço estiver configurado para a sessão.
   * Não refaça o fetch desnecessariamente — as perguntas são imutáveis para a sessão.
   *
   * @throws BriefingApiError (token_invalid | server_error | network)
   */
  async getQuestions(id: string): Promise<Question[]> {
    try {
      const response = await api.get<RawQuestion[]>(
        `/api/v1/briefing-sessions/${id}/questions`,
      );

      // Garante ordenação mesmo que o backend mude comportamento futuramente
      return response.data.slice().sort((a, b) => a.orderIndex - b.orderIndex);
    } catch (err) {
      throw normalizeBriefingSessionError(err);
    }
  },

  /**
   * Finaliza a sessão de briefing e calcula o completeness score.
   *
   * POST /api/v1/briefing-sessions/{id}/complete → CompletionResult
   *
   * Retorna 409 se a sessão não estiver em status IN_PROGRESS.
   * O score é calculado com base nas respostas obrigatórias preenchidas.
   * Sessões com score < 80% são marcadas como COMPLETED com aviso no message.
   *
   * Nota: este endpoint também existe em briefingApi.completeBriefingSession (para
   * o fluxo público). Esta versão é a direta — sem import dinâmico — adequada para
   * uso no dashboard do service provider.
   *
   * @throws BriefingApiError (session_completed | validation | server_error | network)
   */
  async complete(id: string): Promise<CompletionResult> {
    try {
      const response = await api.post<RawCompletionResult>(
        `/api/v1/briefing-sessions/${id}/complete`,
      );

      return {
        completenessScore: response.data.completenessScore,
        status: 'COMPLETED',
        message: response.data.message,
      };
    } catch (err) {
      throw normalizeBriefingSessionError(err);
    }
  },
};
