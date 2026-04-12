/**
 * Zustand store para a listagem de briefing sessions no dashboard do service provider.
 *
 * Diferente de `useBriefingStore` (fluxo do cliente, público), este store gerencia
 * a visão do service provider sobre as sessões de briefing vinculadas às suas proposals.
 *
 * ## Por que não existe um endpoint de listagem?
 *
 * O backend não expõe `GET /api/v1/briefing-sessions` (lista geral). A abordagem
 * adotada é derivar as sessões a partir das proposals:
 *
 *   1. `proposalApi.list()` → coleta todos os `briefingId` não-nulos
 *   2. `briefingSessionApi.getById(id)` por ID → busca em paralelo via Promise.allSettled
 *   3. Resultados `fulfilled` são armazenados; `rejected` (ex: 404) são ignorados silenciosamente
 *
 * ## Relação com useDashboardStore
 *
 * Ambos os stores são independentes — `useDashboardStore` gerencia proposals,
 * este gerencia briefing sessions. As proposals são usadas apenas como índice
 * para descobrir os `briefingId`s existentes; os dados exibidos vêm do endpoint
 * `/api/v1/briefing-sessions/{id}` (mais completo).
 */

import { create } from 'zustand';
import { proposalApi } from '@/lib/proposalApi';
import { briefingSessionApi } from '@/lib/briefingSessionApi';
import type { BriefingSession, BriefingStatus } from '@/types/briefing';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type BriefingSessionFilter = BriefingStatus | 'ALL';

export interface BriefingSessionDashboardState {
  // ---- Data ----------------------------------------------------------------
  sessions: BriefingSession[];
  isLoading: boolean;
  fetchError: string | null;
  statusFilter: BriefingSessionFilter;

  // ---- Setters -------------------------------------------------------------
  setSessions: (sessions: BriefingSession[]) => void;
  setLoading: (loading: boolean) => void;
  setFetchError: (error: string | null) => void;
  setStatusFilter: (filter: BriefingSessionFilter) => void;

  // ---- Async actions -------------------------------------------------------
  /**
   * Carrega briefing sessions derivando-as das proposals do workspace.
   *
   * Fluxo:
   *   1. Busca proposals (até 100, sem filtro de status)
   *   2. Extrai briefingIds únicos e não-nulos
   *   3. Busca cada sessão em paralelo via Promise.allSettled
   *   4. Armazena apenas as requisições bem-sucedidas (ignora 404 silenciosamente)
   *
   * Em caso de falha na busca de proposals, armazena a mensagem de erro.
   */
  fetchSessions: () => Promise<void>;

  // ---- Computed ------------------------------------------------------------
  /**
   * Retorna sessões filtradas pelo statusFilter atual.
   * Ordenadas por `createdAt` decrescente (mais recente primeiro).
   */
  getFilteredSessions: () => BriefingSession[];
}

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

const useBriefingSessionStore = create<BriefingSessionDashboardState>((set, get) => ({
  sessions: [],
  isLoading: false,
  fetchError: null,
  statusFilter: 'ALL',

  setSessions: (sessions) => set({ sessions, fetchError: null }),

  setLoading: (isLoading) => set({ isLoading }),

  setFetchError: (fetchError) => set({ fetchError }),

  setStatusFilter: (statusFilter) => set({ statusFilter }),

  fetchSessions: async () => {
    const { setLoading, setSessions, setFetchError } = get();

    setLoading(true);

    try {
      // 1. Busca proposals para descobrir briefingIds
      const page = await proposalApi.list({ size: 100 });

      // 2. Extrai briefingIds únicos e não-nulos (uma proposta sempre tem no máximo um briefing)
      const briefingIds = [
        ...new Set(
          page.content
            .map((proposal) => proposal.briefingId)
            .filter((id): id is string => id !== null && id !== undefined),
        ),
      ];

      if (briefingIds.length === 0) {
        setSessions([]);
        return;
      }

      // 3. Busca cada sessão em paralelo — Promise.allSettled não aborta se um falhar
      const results = await Promise.allSettled(
        briefingIds.map((id) => briefingSessionApi.getById(id)),
      );

      // 4. Coleta apenas os resultados bem-sucedidos
      const sessions = results
        .filter(
          (result): result is PromiseFulfilledResult<BriefingSession> =>
            result.status === 'fulfilled',
        )
        .map((result) => result.value);

      setSessions(sessions);
    } catch (err) {
      // Falha no passo 1 (proposalApi.list): erro de rede ou auth
      const message =
        err instanceof Error ? err.message : 'Erro ao carregar briefings.';
      setFetchError(message);
    } finally {
      setLoading(false);
    }
  },

  getFilteredSessions: () => {
    const { sessions, statusFilter } = get();

    const filtered =
      statusFilter === 'ALL'
        ? sessions
        : sessions.filter((s) => s.status === statusFilter);

    // Mais recente primeiro
    return [...filtered].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  },
}));

export default useBriefingSessionStore;
