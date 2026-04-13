import useDashboardStore from '@/stores/useDashboardStore';
import type { Proposal } from '@/types/proposal';

// ---------------------------------------------------------------------------
// Mock do proposalApi — evita chamadas HTTP reais nos testes de store
// ---------------------------------------------------------------------------

vi.mock('@/lib/proposalApi', () => ({
  proposalApi: {
    list: vi.fn(),
    remove: vi.fn(),
  },
}));

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const makeProposal = (overrides?: Partial<Proposal>): Proposal => ({
  id: 'p-1',
  workspaceId: 'ws-1',
  clientId: 'c-1',
  briefingId: 'b-1',
  proposalName: 'Proposta Teste',
  status: 'DRAFT',
  scope: null,
  createdAt: '2026-04-12T10:00:00Z',
  updatedAt: '2026-04-12T10:00:00Z',
  ...overrides,
});

// ---------------------------------------------------------------------------
// Reset de store entre testes
// ---------------------------------------------------------------------------

beforeEach(() => {
  useDashboardStore.setState({
    proposals: [],
    statusFilter: 'ALL',
    sortBy: 'date',
    isLoading: false,
    fetchError: null,
  });
});

// ---------------------------------------------------------------------------
// computeStats
// ---------------------------------------------------------------------------

describe('computeStats', () => {
  it('should return zeros and "—" when proposals is empty', () => {
    const stats = useDashboardStore.getState().computeStats();

    const active = stats.find((s) => s.id === 'active');
    const completed = stats.find((s) => s.id === 'completed');
    const approval = stats.find((s) => s.id === 'approval');
    const clients = stats.find((s) => s.id === 'clients');

    expect(active?.value).toBe(0);
    expect(completed?.value).toBe(0);
    expect(approval?.value).toBe('—');
    expect(clients?.value).toBe(0);
  });

  it('should count only DRAFT and PUBLISHED proposals as active', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'DRAFT' }),
        makeProposal({ id: 'p-2', status: 'PUBLISHED' }),
        makeProposal({ id: 'p-3', status: 'APPROVED' }),
        makeProposal({ id: 'p-4', status: 'REJECTED' }),
      ],
    });

    const stats = useDashboardStore.getState().computeStats();
    const active = stats.find((s) => s.id === 'active');

    expect(active?.value).toBe(2);
  });

  it('should return "—" for approval when there are no APPROVED or REJECTED proposals', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'DRAFT' }),
        makeProposal({ id: 'p-2', status: 'PUBLISHED' }),
      ],
    });

    const stats = useDashboardStore.getState().computeStats();
    const approval = stats.find((s) => s.id === 'approval');

    expect(approval?.value).toBe('—');
  });

  it('should calculate correct approval percentage when there are APPROVED and REJECTED proposals', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'APPROVED' }),
        makeProposal({ id: 'p-2', status: 'APPROVED' }),
        makeProposal({ id: 'p-3', status: 'REJECTED' }),
        makeProposal({ id: 'p-4', status: 'REJECTED' }),
      ],
    });

    const stats = useDashboardStore.getState().computeStats();
    const approval = stats.find((s) => s.id === 'approval');

    // 2 APPROVED / (2 + 2) = 50%
    expect(approval?.value).toBe('50%');
  });

  it('should count distinct clientIds correctly', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', clientId: 'c-1' }),
        makeProposal({ id: 'p-2', clientId: 'c-1' }), // mesmo cliente — não conta de novo
        makeProposal({ id: 'p-3', clientId: 'c-2' }),
        makeProposal({ id: 'p-4', clientId: 'c-3' }),
      ],
    });

    const stats = useDashboardStore.getState().computeStats();
    const clients = stats.find((s) => s.id === 'clients');

    expect(clients?.value).toBe(3);
  });

  it('should return 100% approval when all proposals are APPROVED', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'APPROVED' }),
        makeProposal({ id: 'p-2', status: 'APPROVED' }),
      ],
    });

    const stats = useDashboardStore.getState().computeStats();
    const approval = stats.find((s) => s.id === 'approval');

    expect(approval?.value).toBe('100%');
  });
});

// ---------------------------------------------------------------------------
// computeRecentActivity
// ---------------------------------------------------------------------------

describe('computeRecentActivity', () => {
  it('should return empty array when proposals is empty', () => {
    const activity = useDashboardStore.getState().computeRecentActivity();

    expect(activity).toEqual([]);
  });

  it('should generate a "created" event for every proposal', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', proposalName: 'Proposta A' }),
        makeProposal({ id: 'p-2', proposalName: 'Proposta B', status: 'APPROVED' }),
      ],
    });

    const activity = useDashboardStore.getState().computeRecentActivity();
    const createdEvents = activity.filter((e) => e.type === 'created');

    expect(createdEvents).toHaveLength(2);
  });

  it('should generate a "sent" event only for PUBLISHED proposals', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'DRAFT' }),
        makeProposal({ id: 'p-2', status: 'PUBLISHED' }),
        makeProposal({ id: 'p-3', status: 'APPROVED' }),
      ],
    });

    const activity = useDashboardStore.getState().computeRecentActivity();
    const sentEvents = activity.filter((e) => e.type === 'sent');

    expect(sentEvents).toHaveLength(1);
    expect(sentEvents[0].id).toBe('p-2-sent');
  });

  it('should generate an "approved" event only for APPROVED proposals', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'DRAFT' }),
        makeProposal({ id: 'p-2', status: 'APPROVED' }),
        makeProposal({ id: 'p-3', status: 'REJECTED' }),
      ],
    });

    const activity = useDashboardStore.getState().computeRecentActivity();
    const approvedEvents = activity.filter((e) => e.type === 'approved');

    expect(approvedEvents).toHaveLength(1);
    expect(approvedEvents[0].id).toBe('p-2-approved');
  });

  it('should generate an "updated" event only for DRAFT proposals with non-null scope', () => {
    const scopeWithData = {
      deliverables: [],
      exclusions: [],
      assumptions: [],
      price: null,
      timeline: null,
    };

    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1', status: 'DRAFT', scope: null }),          // sem escopo — sem updated
        makeProposal({ id: 'p-2', status: 'DRAFT', scope: scopeWithData }), // escopo presente → updated
        makeProposal({ id: 'p-3', status: 'PUBLISHED', scope: scopeWithData }), // PUBLISHED → sent, não updated
      ],
    });

    const activity = useDashboardStore.getState().computeRecentActivity();
    const updatedEvents = activity.filter((e) => e.type === 'updated');

    expect(updatedEvents).toHaveLength(1);
    expect(updatedEvents[0].id).toBe('p-2-updated');
  });

  it('should limit results to 10 items', () => {
    // 11 proposals × 1 evento cada (DRAFT sem scope → apenas created)
    const proposals = Array.from({ length: 11 }, (_, i) =>
      makeProposal({ id: `p-${i}`, proposalName: `Proposta ${i}` }),
    );
    useDashboardStore.setState({ proposals });

    const activity = useDashboardStore.getState().computeRecentActivity();

    expect(activity).toHaveLength(10);
  });

  it('should sort events by timestamp in descending order', () => {
    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-old', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }),
        makeProposal({ id: 'p-new', createdAt: '2026-04-12T10:00:00Z', updatedAt: '2026-04-12T10:00:00Z' }),
      ],
    });

    const activity = useDashboardStore.getState().computeRecentActivity();
    const timestamps = activity.map((e) => e.timestamp);

    // Primeiro evento deve ter timestamp mais recente
    expect(new Date(timestamps[0]).getTime()).toBeGreaterThanOrEqual(
      new Date(timestamps[timestamps.length - 1]).getTime(),
    );
  });
});

// ---------------------------------------------------------------------------
// deleteProposal
// ---------------------------------------------------------------------------

describe('deleteProposal', () => {
  it('should remove the proposal from state after successful delete', async () => {
    const { proposalApi } = await import('@/lib/proposalApi');
    (proposalApi.remove as ReturnType<typeof vi.fn>).mockResolvedValueOnce(undefined);

    useDashboardStore.setState({
      proposals: [
        makeProposal({ id: 'p-1' }),
        makeProposal({ id: 'p-2' }),
      ],
    });

    await useDashboardStore.getState().deleteProposal('p-1');

    const remaining = useDashboardStore.getState().proposals;
    expect(remaining).toHaveLength(1);
    expect(remaining[0].id).toBe('p-2');
  });

  it('should rethrow the error when proposalApi.remove fails', async () => {
    const { proposalApi } = await import('@/lib/proposalApi');
    const apiError = { kind: 'not_found', message: 'Proposta não encontrada.' };
    (proposalApi.remove as ReturnType<typeof vi.fn>).mockRejectedValueOnce(apiError);

    useDashboardStore.setState({ proposals: [makeProposal({ id: 'p-1' })] });

    await expect(useDashboardStore.getState().deleteProposal('p-1')).rejects.toEqual(
      apiError,
    );
  });

  it('should not remove proposal from state when delete fails', async () => {
    const { proposalApi } = await import('@/lib/proposalApi');
    (proposalApi.remove as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new Error('network error'),
    );

    useDashboardStore.setState({ proposals: [makeProposal({ id: 'p-1' })] });

    await expect(
      useDashboardStore.getState().deleteProposal('p-1'),
    ).rejects.toThrow();

    // Proposta permanece no estado — não foi removida
    expect(useDashboardStore.getState().proposals).toHaveLength(1);
  });
});
