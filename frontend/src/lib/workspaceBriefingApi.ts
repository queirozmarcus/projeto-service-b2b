/**
 * Workspace Briefing API client — endpoints autenticados (BriefingControllerV1).
 *
 * Usa a instância `api` de `@/lib/api`, que injeta automaticamente o header
 * `Authorization: Bearer <token>` e realiza refresh proativo quando necessário.
 *
 * Diferente de briefingApi.ts (endpoints públicos) e briefingSessionApi.ts (sessões
 * vinculadas a proposals via BriefingSessionControllerV2).
 * Este client acessa BriefingControllerV1 que gerencia briefings autenticados do
 * service provider.
 *
 * Endpoints cobertos:
 *   POST /api/v1/briefings → WorkspaceBriefing (201)
 *
 * Tratamento de erros:
 *   409 → WorkspaceBriefingApiError { kind: 'conflict' }   (briefing ativo duplicado)
 *   400 → WorkspaceBriefingApiError { kind: 'validation' }
 *   403 → WorkspaceBriefingApiError { kind: 'forbidden' }
 *   5xx → WorkspaceBriefingApiError { kind: 'server_error' }
 *   sem resposta → WorkspaceBriefingApiError { kind: 'network' }
 */

import axios, { AxiosError } from 'axios';
import api from '@/lib/api';
import type {
  CreateWorkspaceBriefingPayload,
  WorkspaceBriefing,
  WorkspaceBriefingApiError,
} from '@/types/workspace-briefing';

// ---------------------------------------------------------------------------
// Shapes internas dos responses do backend
// ---------------------------------------------------------------------------

/**
 * Shape exata de BriefingResponse.java retornada por BriefingControllerV1.
 * O backend serializa camelCase diretamente — campos idênticos ao WorkspaceBriefing,
 * exceto `completionScore` que pode vir como null do JSON.
 */
interface RawWorkspaceBriefing {
  id: string;
  workspaceId: string;
  clientId: string;
  serviceType: string;
  status: string;
  publicToken: string;
  completionScore: number | null;
  createdAt: string;
  updatedAt: string;
}

// ---------------------------------------------------------------------------
// Normalização de erros
// ---------------------------------------------------------------------------

/**
 * Converte um AxiosError ou erro genérico em WorkspaceBriefingApiError tipado.
 * Garante que o chamador receba sempre um tipo discriminado tratável com
 * switch/case, sem necessidade de type assertions sobre `unknown`.
 */
function normalizeWorkspaceBriefingError(err: unknown): WorkspaceBriefingApiError {
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

    if (status === 409) {
      return {
        kind: 'conflict',
        message: serverMessage ?? 'Já existe um briefing ativo para este cliente e tipo de serviço.',
      };
    }

    if (status === 400) {
      return {
        kind: 'validation',
        message: serverMessage ?? 'Dados inválidos. Verifique os campos e tente novamente.',
      };
    }

    if (status === 403) {
      return {
        kind: 'forbidden',
        message: serverMessage ?? 'Você não tem permissão para realizar esta ação.',
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
// API client
// ---------------------------------------------------------------------------

export const workspaceBriefingApi = {
  /**
   * Cria um novo briefing no workspace autenticado.
   *
   * POST /api/v1/briefings → 201 Created
   *
   * O `clientId` deve ser gerado pelo frontend antes da chamada (crypto.randomUUID()).
   * O backend retorna o `publicToken` que deve ser usado para montar o link do cliente
   * (ex: http://localhost:3000/briefing/{publicToken}).
   *
   * Retorna 409 Conflict se já existir um briefing IN_PROGRESS para o mesmo
   * clientId + serviceType no workspace.
   *
   * @param payload - clientId (UUID gerado pelo frontend) e serviceType
   * @returns WorkspaceBriefing recém-criado com status IN_PROGRESS
   * @throws WorkspaceBriefingApiError (conflict | validation | forbidden | server_error | network)
   */
  async create(payload: CreateWorkspaceBriefingPayload): Promise<WorkspaceBriefing> {
    try {
      const response = await api.post<RawWorkspaceBriefing>('/briefings', payload);
      return response.data as WorkspaceBriefing;
    } catch (err) {
      throw normalizeWorkspaceBriefingError(err);
    }
  },
};
