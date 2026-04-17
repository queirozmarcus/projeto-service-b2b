/**
 * Testes unitários de useDashboardStore (Zustand).
 *
 * Estratégia: vi.mock('@/lib/proposalApi') isola o store do client HTTP.
 * O estado Zustand é resetado entre testes via store.setState() com o
 * estado inicial — evita vazamento entre casos.
 *
 * Testes cobrem:
 *   - Estado inicial
 *   - fetchProposals (sucesso e erro)
 *   - deleteProposal (sucesso e propagação de erro)
 *   - addProposal / removeProposal / updateProposal
 *   - computeStats (contagens e taxa de aprovação)
 *   - computeRecentActivity (geração de eventos e ordenação)
 *   - getFilteredProposals (filter por status e sort)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import useDashboardStore from '@/stores/useDashboardStore';
import type { Proposal } from '@/types/proposal';

// ---------------------------------------------------------------------------
// Mock da camada de API
// ---------------------------------------------------------------------------

vi.mock('@/lib/proposalApi', () => ({
  proposalApi: {
    list: vi.fn(),
    remove: vi.fn(),
  },
}));

import { proposalApi } from '@/lib/proposalApi';

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

function makeProposal(overrides: Partial<Proposal> = {}): Proposal {
  return {
    id: 'prop-1',
    workspaceId: 'ws-1',
    clientId: 'client-1',
    briefingId: 'briefing-1',
    proposalName: 'Projeto Alpha',
    status: 'DRAFT',
    scope: null,
    createdAt: '2026-04-01T10:00:00Z',
    updatedAt: '2026-04-01T10:00:00Z',
    ...overrides,
  };
}

const DRAFT_PROPOSAL   = makeProposal({ id: 'prop-draft',     status: 'DRAFT',     proposalName: 'Draft Proposal',     clientId: 'client-1', createdAt: '2026-04-01T08:00:00Z', updatedAt: '2026-04-01T08:00:00Z' });
const PUBLISHED_PROPOSAL = makeProposal({ id: 'prop-pub',     status: 'PUBLISHED', proposalName: 'Published Proposal', clientId: 'client-2', createdAt: '2026-04-02T08:00:00Z', updatedAt: '2026-04-02T09:00:00Z' });
const APPROVED_PROPOSAL  = makeProposal({ id: 'prop-approved', status: 'APPROVED', proposalName: 'Approved Proposal',  clientId: 'client-3', createdAt: '2026-04-03T08:00:00Z', updatedAt: '2026-04-03T10:00:00Z' });
const REJECTED_PROPOSAL  = makeProposal({ id: 'prop-rejected', status: 'REJECTED', proposalName: 'Rejected Proposal',  clientId: 'client-4', createdAt: '2026-04-04T08:00:00Z', updatedAt: '2026-04-04T10:00:00Z' });

// ---------------------------------------------------------------------------
// Reset do store antes de cada teste
// ---------------------------------------------------------------------------

beforeEach(() => {
  vi.clearAllMocks();
  useDashboardStore.setState({
    proposals: [],
    statusFilter: 'ALL',
    sortBy: 'date',
    isLoading: false,
    fetchError: null,
  });
});

// ---------------------------------------------------------------------------
// Estado inicial
// ---------------------------------------------------------------------------

describe('initial state', () => {
  it('should_haveEmptyProposals_byDefault', () => {
    const { proposals } = useDashboardStore.getState();
    expect(proposals).toEqual([]);
  });

  it('should_haveCorrectDefaults', () => {
    const { statusFilter, sortBy, isLoading, fetchError } = useDashboardStore.getState();
    expect(statusFilter).toBe('ALL');
    expect(sortBy).toBe('date');
    expect(isLoading).toBe(false);
    expect(fetchError).toBeNull();
  });
});

// ---------------------------------------------------------------------------
// fetchProposals
// ---------------------------------------------------------------------------

describe('fetchProposals', () => {
  it('should_populateProposals_whenApiReturnsContent', async () => {
    // Given
    vi.mocked(proposalApi.list).mockResolvedValueOnce({
      content: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL],
      totalElements: 2, totalPages: 1, size: 20, number: 0, first: true, last: true,
    });

    // When
    await useDashboardStore.getState().fetchProposals();

    // Then
    const { proposals, isLoading, fetchError } = useDashboardStore.getState();
    expect(proposals).toHaveLength(2);
    expect(proposals[0].id).toBe('prop-draft');
    expect(isLoading).toBe(false);
    expect(fetchError).toBeNull();
  });

  it('should_setFetchError_whenApiThrows', async () => {
    // Given
    vi.mocked(proposalApi.list).mockRejectedValueOnce(
      new Error('Erro ao carregar propostas.')
    );

    // When
    await useDashboardStore.getState().fetchProposals();

    // Then
    const { proposals, isLoading, fetchError } = useDashboardStore.getState();
    expect(proposals).toHaveLength(0);
    expect(isLoading).toBe(false);
    expect(fetchError).toBe('Erro ao carregar propostas.');
  });

  it('should_setFetchErrorToFallback_whenErrorHasNoMessage', async () => {
    // Given — ProposalApiError não é instância de Error
    vi.mocked(proposalApi.list).mockRejectedValueOnce({ kind: 'network', message: 'Sem conexão' });

    // When
    await useDashboardStore.getState().fetchProposals();

    // Then
    expect(useDashboardStore.getState().fetchError).toBe('Erro ao carregar propostas.');
  });

  it('should_passParamsToApi_whenParamsProvided', async () => {
    // Given
    vi.mocked(proposalApi.list).mockResolvedValueOnce({
      content: [], totalElements: 0, totalPages: 0, size: 10, number: 0, first: true, last: true,
    });

    // When
    await useDashboardStore.getState().fetchProposals({ page: 2, size: 10, status: 'DRAFT' });

    // Then
    expect(proposalApi.list).toHaveBeenCalledWith({ page: 2, size: 10, status: 'DRAFT' });
  });
});

// ---------------------------------------------------------------------------
// deleteProposal
// ---------------------------------------------------------------------------

describe('deleteProposal', () => {
  it('should_removeProposalFromStore_whenDeleteSucceeds', async () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL] });
    vi.mocked(proposalApi.remove).mockResolvedValueOnce(undefined);

    // When
    await useDashboardStore.getState().deleteProposal('prop-draft');

    // Then
    const { proposals } = useDashboardStore.getState();
    expect(proposals).toHaveLength(1);
    expect(proposals[0].id).toBe('prop-pub');
  });

  it('should_propagateError_whenApiFails', async () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL] });
    vi.mocked(proposalApi.remove).mockRejectedValueOnce({ kind: 'not_found', message: 'Proposta não encontrada.' });

    // When / Then
    await expect(useDashboardStore.getState().deleteProposal('prop-draft')).rejects.toMatchObject({
      kind: 'not_found',
    });
    // Proposta NÃO deve ser removida do store quando a API falha
    expect(useDashboardStore.getState().proposals).toHaveLength(1);
  });
});

// ---------------------------------------------------------------------------
// addProposal / removeProposal / updateProposal
// ---------------------------------------------------------------------------

describe('addProposal', () => {
  it('should_prependProposal_toBeginningOfList', () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL] });

    // When
    useDashboardStore.getState().addProposal(PUBLISHED_PROPOSAL);

    // Then
    const { proposals } = useDashboardStore.getState();
    expect(proposals[0].id).toBe('prop-pub');
    expect(proposals[1].id).toBe('prop-draft');
  });
});

describe('removeProposal', () => {
  it('should_removeOnlyTargetProposal_keepingOthers', () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL, APPROVED_PROPOSAL] });

    // When
    useDashboardStore.getState().removeProposal('prop-pub');

    // Then
    const { proposals } = useDashboardStore.getState();
    expect(proposals).toHaveLength(2);
    expect(proposals.find((p) => p.id === 'prop-pub')).toBeUndefined();
  });

  it('should_doNothing_whenIdDoesNotExist', () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL] });

    // When
    useDashboardStore.getState().removeProposal('non-existing-id');

    // Then
    expect(useDashboardStore.getState().proposals).toHaveLength(1);
  });
});

describe('updateProposal', () => {
  it('should_mergeUpdates_intoTargetProposal', () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL] });

    // When
    useDashboardStore.getState().updateProposal('prop-draft', {
      proposalName: 'Projeto Alpha Revisado',
      status: 'PUBLISHED',
    });

    // Then
    const updated = useDashboardStore.getState().proposals.find((p) => p.id === 'prop-draft');
    expect(updated?.proposalName).toBe('Projeto Alpha Revisado');
    expect(updated?.status).toBe('PUBLISHED');
  });

  it('should_notModifyOtherProposals_whenUpdatingOne', () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL] });

    // When
    useDashboardStore.getState().updateProposal('prop-draft', { proposalName: 'Novo Nome' });

    // Then
    const untouched = useDashboardStore.getState().proposals.find((p) => p.id === 'prop-pub');
    expect(untouched?.proposalName).toBe('Published Proposal');
  });
});

// ---------------------------------------------------------------------------
// computeStats
// ---------------------------------------------------------------------------

describe('computeStats', () => {
  it('should_returnZeroCounts_whenProposalsIsEmpty', () => {
    // Given — estado inicial com proposals vazio

    // When
    const stats = useDashboardStore.getState().computeStats();

    // Then
    const active    = stats.find((s) => s.id === 'active');
    const completed = stats.find((s) => s.id === 'completed');
    const approval  = stats.find((s) => s.id === 'approval');
    const clients   = stats.find((s) => s.id === 'clients');

    expect(active?.value).toBe(0);
    expect(completed?.value).toBe(0);
    expect(approval?.value).toBe('—');
    expect(clients?.value).toBe(0);
  });

  it('should_countDraftAndPublishedAsActive', () => {
    // Given
    useDashboardStore.setState({
      proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL, APPROVED_PROPOSAL],
    });

    // When
    const stats = useDashboardStore.getState().computeStats();

    // Then
    expect(stats.find((s) => s.id === 'active')?.value).toBe(2);
  });

  it('should_countOnlyApprovedAsCompleted', () => {
    // Given
    useDashboardStore.setState({ proposals: [APPROVED_PROPOSAL, REJECTED_PROPOSAL] });

    // When
    const stats = useDashboardStore.getState().computeStats();

    // Then
    expect(stats.find((s) => s.id === 'completed')?.value).toBe(1);
  });

  it('should_computeApprovalRate_asPercentage', () => {
    // Given — 1 aprovada de 2 (aprovada + rejeitada) = 50%
    useDashboardStore.setState({ proposals: [APPROVED_PROPOSAL, REJECTED_PROPOSAL] });

    // When
    const stats = useDashboardStore.getState().computeStats();

    // Then
    expect(stats.find((s) => s.id === 'approval')?.value).toBe('50%');
  });

  it('should_return100PercentApproval_whenAllApproved', () => {
    // Given
    const approved2 = makeProposal({ id: 'prop-approved-2', status: 'APPROVED', clientId: 'client-5' });
    useDashboardStore.setState({ proposals: [APPROVED_PROPOSAL, approved2] });

    // When
    const stats = useDashboardStore.getState().computeStats();

    // Then
    expect(stats.find((s) => s.id === 'approval')?.value).toBe('100%');
  });

  it('should_countUniqueClients_byClientId', () => {
    // Given — 3 propostas, 2 clients únicos (client-1 aparece duas vezes)
    const dup = makeProposal({ id: 'prop-dup', clientId: 'client-1' });
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL, dup] });

    // When
    const stats = useDashboardStore.getState().computeStats();

    // Then
    expect(stats.find((s) => s.id === 'clients')?.value).toBe(2);
  });
});

// ---------------------------------------------------------------------------
// computeRecentActivity
// ---------------------------------------------------------------------------

describe('computeRecentActivity', () => {
  it('should_returnEmptyArray_whenProposalsIsEmpty', () => {
    // Given — estado inicial

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();

    // Then
    expect(activity).toHaveLength(0);
  });

  it('should_generateCreatedEvent_forEveryProposal', () => {
    // Given
    useDashboardStore.setState({ proposals: [DRAFT_PROPOSAL, APPROVED_PROPOSAL] });

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();
    const createdEvents = activity.filter((a) => a.type === 'created');

    // Then
    expect(createdEvents).toHaveLength(2);
  });

  it('should_generateSentEvent_forPublishedProposal', () => {
    // Given
    useDashboardStore.setState({ proposals: [PUBLISHED_PROPOSAL] });

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();
    const sentEvent = activity.find((a) => a.type === 'sent');

    // Then
    expect(sentEvent).toBeDefined();
    expect(sentEvent?.description).toContain('Published Proposal');
  });

  it('should_generateApprovedEvent_forApprovedProposal', () => {
    // Given
    useDashboardStore.setState({ proposals: [APPROVED_PROPOSAL] });

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();
    const approvedEvent = activity.find((a) => a.type === 'approved');

    // Then
    expect(approvedEvent).toBeDefined();
    expect(approvedEvent?.description).toContain('Approved Proposal');
  });

  it('should_generateUpdatedEvent_whenDraftHasScope', () => {
    // Given
    const draftWithScope = makeProposal({
      id: 'prop-scoped',
      status: 'DRAFT',
      proposalName: 'Scoped Draft',
      scope: {
        deliverables: [],
        exclusions: [],
        assumptions: [],
        price: null,
        timeline: null,
      },
      updatedAt: '2026-04-05T12:00:00Z',
    });
    useDashboardStore.setState({ proposals: [draftWithScope] });

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();
    const updatedEvent = activity.find((a) => a.type === 'updated');

    // Then
    expect(updatedEvent).toBeDefined();
    expect(updatedEvent?.description).toContain('Scoped Draft');
  });

  it('should_returnAtMost10Events_whenManyProposalsExist', () => {
    // Given — 8 proposals cada uma gera ao menos 1 evento, alguns geram 2
    const proposals = Array.from({ length: 8 }, (_, i) =>
      makeProposal({
        id: `prop-${i}`,
        status: 'PUBLISHED',
        clientId: `client-${i}`,
        createdAt: `2026-04-0${i + 1}T08:00:00Z`,
        updatedAt: `2026-04-0${i + 1}T09:00:00Z`,
      })
    );
    useDashboardStore.setState({ proposals });

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();

    // Then
    expect(activity.length).toBeLessThanOrEqual(10);
  });

  it('should_returnEventsOrderedByTimestampDesc', () => {
    // Given
    useDashboardStore.setState({
      proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL, APPROVED_PROPOSAL],
    });

    // When
    const activity = useDashboardStore.getState().computeRecentActivity();

    // Then — cada evento deve ter timestamp >= o próximo
    for (let i = 0; i < activity.length - 1; i++) {
      const current = new Date(activity[i].timestamp).getTime();
      const next    = new Date(activity[i + 1].timestamp).getTime();
      expect(current).toBeGreaterThanOrEqual(next);
    }
  });
});

// ---------------------------------------------------------------------------
// getFilteredProposals
// ---------------------------------------------------------------------------

describe('getFilteredProposals', () => {
  beforeEach(() => {
    useDashboardStore.setState({
      proposals: [DRAFT_PROPOSAL, PUBLISHED_PROPOSAL, APPROVED_PROPOSAL, REJECTED_PROPOSAL],
    });
  });

  it('should_returnAllProposals_whenFilterIsALL', () => {
    // Given — statusFilter padrão é 'ALL'

    // When
    const result = useDashboardStore.getState().getFilteredProposals();

    // Then
    expect(result).toHaveLength(4);
  });

  it('should_returnOnlyDraft_whenFilterIsDRAFT', () => {
    // Given
    useDashboardStore.setState({ statusFilter: 'DRAFT' });

    // When
    const result = useDashboardStore.getState().getFilteredProposals();

    // Then
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe('DRAFT');
  });

  it('should_sortByDateDesc_whenSortByIsDate', () => {
    // Given
    useDashboardStore.setState({ sortBy: 'date', statusFilter: 'ALL' });

    // When
    const result = useDashboardStore.getState().getFilteredProposals();

    // Then — prop-rejected criado mais recente (2026-04-04) vem primeiro
    expect(result[0].id).toBe('prop-rejected');
    expect(result[result.length - 1].id).toBe('prop-draft');
  });

  it('should_sortByNameAsc_whenSortByIsClient', () => {
    // Given
    useDashboardStore.setState({ sortBy: 'client', statusFilter: 'ALL' });

    // When
    const result = useDashboardStore.getState().getFilteredProposals();

    // Then — "Approved" < "Draft" < "Published" < "Rejected" (alphabetical)
    expect(result[0].proposalName).toBe('Approved Proposal');
    expect(result[result.length - 1].proposalName).toBe('Rejected Proposal');
  });

  it('should_sortByStatus_whenSortByIsStatus', () => {
    // Given
    useDashboardStore.setState({ sortBy: 'status', statusFilter: 'ALL' });

    // When
    const result = useDashboardStore.getState().getFilteredProposals();

    // Then — "APPROVED" < "DRAFT" < "PUBLISHED" < "REJECTED"
    expect(result[0].status).toBe('APPROVED');
  });

  it('should_returnEmpty_whenFilterMatchesNoProposals', () => {
    // Given — nenhuma proposta em PUBLISHED no estado atual... adicionar apenas DRAFT
    useDashboardStore.setState({
      proposals: [DRAFT_PROPOSAL],
      statusFilter: 'APPROVED',
    });

    // When
    const result = useDashboardStore.getState().getFilteredProposals();

    // Then
    expect(result).toHaveLength(0);
  });
});
