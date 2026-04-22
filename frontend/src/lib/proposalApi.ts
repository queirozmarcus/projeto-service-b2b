/**
 * Proposal API client — endpoints autenticados (requer JWT).
 *
 * Usa a instância `api` de `@/lib/api`, que injeta automaticamente o header
 * `Authorization: Bearer <token>` e realiza refresh proativo quando necessário.
 *
 * Endpoints cobertos:
 *   GET    /proposals                         → ProposalPage
 *   GET    /proposals/{id}                    → Proposal
 *   POST   /proposals                         → Proposal (201 Created)
 *   PUT    /proposals/{id}                    → Proposal
 *   DELETE /proposals/{id}                    → 204 No Content
 *   POST   /proposals/{id}/update-scope       → Proposal
 *   POST   /proposals/{id}/publish            → Proposal
 *   POST   /proposals/{id}/initiate-approval  → Proposal
 *
 * Tratamento de erros:
 *   404 → ProposalApiError { kind: 'not_found' }
 *   409 → ProposalApiError { kind: 'conflict' }
 *   400 → ProposalApiError { kind: 'validation' }
 *   403 → ProposalApiError { kind: 'forbidden' }
 *   5xx → ProposalApiError { kind: 'server_error' }
 *   sem resposta → ProposalApiError { kind: 'network' }
 */

import axios, { AxiosError } from 'axios';
import api from '@/lib/api';
import type {
  CreateProposalPayload,
  InitiateApprovalPayload,
  ListProposalsParams,
  Proposal,
  ProposalApiError,
  ProposalPage,
  UpdateProposalPayload,
  UpdateScopePayload,
} from '@/types/proposal';

// ---------------------------------------------------------------------------
// Normalização de erros
// ---------------------------------------------------------------------------

/**
 * Converte um AxiosError ou erro genérico em ProposalApiError tipado.
 * Garante que o chamador receba sempre um tipo discriminado tratável com
 * switch/case, sem necessidade de type assertions sobre `unknown`.
 */
function normalizeProposalError(err: unknown): ProposalApiError {
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
        kind: 'not_found',
        message: serverMessage ?? 'Proposta não encontrada.',
      };
    }

    if (status === 409) {
      return {
        kind: 'conflict',
        message: serverMessage ?? 'Operação inválida para o estado atual da proposta.',
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

export const proposalApi = {
  /**
   * Lista proposals do workspace autenticado com paginação e filtro opcional de status.
   *
   * GET /proposals
   *
   * @param params - Filtros opcionais: page (zero-based), size, status
   * @returns ProposalPage com conteúdo paginado e metadados de paginação
   * @throws ProposalApiError (forbidden | server_error | network)
   */
  async list(params?: ListProposalsParams): Promise<ProposalPage> {
    try {
      const response = await api.get<ProposalPage>('/proposals', { params });
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Busca uma proposta pelo seu UUID.
   *
   * GET /proposals/{id}
   *
   * @param id - UUID da proposta
   * @returns Proposal completa com scope (se gerado)
   * @throws ProposalApiError (not_found | forbidden | server_error | network)
   */
  async getById(id: string): Promise<Proposal> {
    try {
      const response = await api.get<Proposal>(`/proposals/${id}`);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Cria uma nova proposta a partir de uma sessão de briefing concluída.
   *
   * POST /proposals → 201 Created
   *
   * O briefing referenciado deve estar no status COMPLETED.
   * A proposta é criada no status DRAFT.
   *
   * @param payload - clientId, briefingId e nome inicial da proposta
   * @returns Proposal recém-criada em status DRAFT
   * @throws ProposalApiError (conflict | validation | forbidden | server_error | network)
   */
  async create(payload: CreateProposalPayload): Promise<Proposal> {
    try {
      const response = await api.post<Proposal>('/proposals', payload);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Atualiza o nome de uma proposta.
   *
   * PUT /proposals/{id}
   *
   * Disponível apenas para propostas em status DRAFT.
   * Retorna 409 Conflict se a proposta não estiver em DRAFT.
   *
   * @param id - UUID da proposta
   * @param payload - Novo nome da proposta
   * @returns Proposal atualizada
   * @throws ProposalApiError (not_found | conflict | validation | forbidden | server_error | network)
   */
  async update(id: string, payload: UpdateProposalPayload): Promise<Proposal> {
    try {
      const response = await api.put<Proposal>(`/proposals/${id}`, payload);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Remove uma proposta (soft-delete).
   *
   * DELETE /proposals/{id} → 204 No Content
   *
   * @param id - UUID da proposta
   * @returns void
   * @throws ProposalApiError (not_found | conflict | forbidden | server_error | network)
   */
  async remove(id: string): Promise<void> {
    try {
      await api.delete(`/proposals/${id}`);
      // 204 No Content — sem body de resposta
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Atualiza o scope de uma proposta com entregáveis, exclusões e precificação.
   *
   * POST /proposals/{id}/update-scope
   *
   * Disponível apenas para propostas em status DRAFT.
   * Tipicamente chamado após geração de scope via AI.
   *
   * @param id - UUID da proposta
   * @param payload - ProposalScope completo (deliverables, exclusions, assumptions, price, timeline)
   * @returns Proposal atualizada com novo scope
   * @throws ProposalApiError (not_found | conflict | validation | forbidden | server_error | network)
   */
  async updateScope(id: string, payload: UpdateScopePayload): Promise<Proposal> {
    try {
      const response = await api.post<Proposal>(`/proposals/${id}/update-scope`, payload);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Publica a proposta, tornando-a visível ao cliente.
   *
   * POST /proposals/{id}/publish
   *
   * Transição de estado: DRAFT → PUBLISHED.
   * Retorna 409 Conflict se a proposta não estiver em DRAFT.
   *
   * @param id - UUID da proposta
   * @returns Proposal com status PUBLISHED
   * @throws ProposalApiError (not_found | conflict | forbidden | server_error | network)
   */
  async publish(id: string): Promise<Proposal> {
    try {
      const response = await api.post<Proposal>(`/proposals/${id}/publish`);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Inicia o workflow de aprovação enviando a proposta para revisão dos aprovadores.
   *
   * POST /proposals/{id}/initiate-approval
   *
   * Requer que a proposta esteja em status PUBLISHED.
   * Os aprovadores recebem notificação por e-mail.
   *
   * @param id - UUID da proposta
   * @param payload - Lista de e-mails dos aprovadores
   * @returns Proposal com status atualizado
   * @throws ProposalApiError (not_found | conflict | validation | forbidden | server_error | network)
   */
  async initiateApproval(id: string, payload: InitiateApprovalPayload): Promise<Proposal> {
    try {
      const response = await api.post<Proposal>(`/proposals/${id}/initiate-approval`, payload);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },

  /**
   * Gera o scope da proposta via IA baseado no briefing completo.
   *
   * POST /proposals/{id}/generate-scope-ai
   *
   * Requer que:
   * - Proposta esteja em status DRAFT
   * - Briefing vinculado esteja COMPLETED
   * - Briefing tenha completeness >= 80%
   *
   * Retorna a proposta com scope preenchido (deliverables, price, timeline, etc).
   * Pode levar 3-10s (síncrono).
   *
   * @param id - UUID da proposta
   * @returns Proposal atualizada com scope gerado
   * @throws ProposalApiError (not_found | conflict | validation | forbidden | server_error | network)
   */
  async generateScopeAI(id: string): Promise<Proposal> {
    try {
      const response = await api.post<Proposal>(`/proposals/${id}/generate-scope-ai`);
      return response.data;
    } catch (err) {
      throw normalizeProposalError(err);
    }
  },
};
