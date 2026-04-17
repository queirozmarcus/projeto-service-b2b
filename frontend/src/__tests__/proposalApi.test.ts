/**
 * Testes unitários de proposalApi — mock do módulo @/lib/api (instância Axios).
 *
 * Estratégia: vi.mock('@/lib/api') substitui a instância Axios por um objeto
 * cujos métodos são vi.fn(). Cada teste configura o retorno esperado (mockResolvedValueOnce)
 * ou erro (mockRejectedValueOnce) e verifica o comportamento do cliente.
 *
 * Por que não axios-mock-adapter: o projeto não o tem instalado. vi.mock é
 * suficiente para testar normalizeProposalError e a delegação correta dos métodos.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { proposalApi } from '@/lib/proposalApi';
import type { Proposal, ProposalPage, ProposalApiError } from '@/types/proposal';

// ---------------------------------------------------------------------------
// Mock do módulo api — substitui a instância Axios real
// ---------------------------------------------------------------------------

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

// Importamos DEPOIS do mock para capturar o objeto já substituído
import api from '@/lib/api';

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const mockProposal: Proposal = {
  id: 'proposal-uuid-1',
  workspaceId: 'workspace-uuid-1',
  clientId: 'client-uuid-1',
  briefingId: 'briefing-uuid-1',
  proposalName: 'Projeto Website Institucional',
  status: 'DRAFT',
  scope: null,
  createdAt: '2026-04-01T10:00:00Z',
  updatedAt: '2026-04-01T10:00:00Z',
};

const mockProposalPage: ProposalPage = {
  content: [mockProposal],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
  first: true,
  last: true,
};

/** Cria um AxiosError simulado com response */
function makeAxiosError(status: number, data?: object) {
  const error = Object.assign(new Error('AxiosError'), {
    isAxiosError: true,
    response: { status, data: data ?? {} },
  });
  return error;
}

/** Cria um AxiosError sem response (network error) */
function makeNetworkError() {
  return Object.assign(new Error('Network Error'), {
    isAxiosError: true,
    response: undefined,
  });
}

// ---------------------------------------------------------------------------
// Setup — limpa mocks entre testes
// ---------------------------------------------------------------------------

beforeEach(() => {
  vi.clearAllMocks();
});

// ---------------------------------------------------------------------------
// proposalApi.list
// ---------------------------------------------------------------------------

describe('proposalApi.list', () => {
  it('should_returnProposalPage_whenRequestSucceeds', async () => {
    // Given
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockProposalPage });

    // When
    const result = await proposalApi.list();

    // Then
    expect(result).toEqual(mockProposalPage);
    expect(api.get).toHaveBeenCalledWith('/proposals', { params: undefined });
  });

  it('should_passQueryParams_whenParamsProvided', async () => {
    // Given
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockProposalPage });

    // When
    await proposalApi.list({ page: 1, size: 10, status: 'DRAFT' });

    // Then
    expect(api.get).toHaveBeenCalledWith('/proposals', {
      params: { page: 1, size: 10, status: 'DRAFT' },
    });
  });

  it('should_throwServerError_whenApiReturns500', async () => {
    // Given
    vi.mocked(api.get).mockRejectedValueOnce(makeAxiosError(500));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.list();
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('server_error');
  });

  it('should_throwNetworkError_whenNoResponseReceived', async () => {
    // Given
    vi.mocked(api.get).mockRejectedValueOnce(makeNetworkError());

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.list();
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('network');
    expect(caught?.message).toContain('Sem conexão');
  });
});

// ---------------------------------------------------------------------------
// proposalApi.getById
// ---------------------------------------------------------------------------

describe('proposalApi.getById', () => {
  it('should_returnProposal_whenIdExists', async () => {
    // Given
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockProposal });

    // When
    const result = await proposalApi.getById('proposal-uuid-1');

    // Then
    expect(result).toEqual(mockProposal);
    expect(api.get).toHaveBeenCalledWith('/proposals/proposal-uuid-1');
  });

  it('should_throwNotFoundError_whenApiReturns404', async () => {
    // Given
    vi.mocked(api.get).mockRejectedValueOnce(
      makeAxiosError(404, { detail: 'Proposta não encontrada.' })
    );

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.getById('non-existing-id');
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('not_found');
    expect(caught?.message).toBe('Proposta não encontrada.');
  });

  it('should_throwForbiddenError_whenApiReturns403', async () => {
    // Given
    vi.mocked(api.get).mockRejectedValueOnce(makeAxiosError(403));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.getById('uuid-1');
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('forbidden');
  });
});

// ---------------------------------------------------------------------------
// proposalApi.create
// ---------------------------------------------------------------------------

describe('proposalApi.create', () => {
  it('should_returnCreatedProposal_whenPayloadIsValid', async () => {
    // Given
    vi.mocked(api.post).mockResolvedValueOnce({ data: mockProposal });
    const payload = {
      clientId: 'client-uuid-1',
      briefingId: 'briefing-uuid-1',
      proposalName: 'Projeto Website Institucional',
    };

    // When
    const result = await proposalApi.create(payload);

    // Then
    expect(result).toEqual(mockProposal);
    expect(api.post).toHaveBeenCalledWith('/proposals', payload);
  });

  it('should_throwConflictError_whenApiReturns409', async () => {
    // Given
    vi.mocked(api.post).mockRejectedValueOnce(makeAxiosError(409));
    const payload = {
      clientId: 'client-uuid-1',
      briefingId: 'briefing-uuid-1',
      proposalName: 'Projeto Duplicado',
    };

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.create(payload);
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('conflict');
  });

  it('should_throwValidationError_whenApiReturns400WithMessage', async () => {
    // Given
    vi.mocked(api.post).mockRejectedValueOnce(
      makeAxiosError(400, { message: 'proposalName is required' })
    );

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.create({ clientId: '', briefingId: '', proposalName: '' });
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('validation');
    expect(caught?.message).toBe('proposalName is required');
  });
});

// ---------------------------------------------------------------------------
// proposalApi.update
// ---------------------------------------------------------------------------

describe('proposalApi.update', () => {
  it('should_returnUpdatedProposal_whenUpdateSucceeds', async () => {
    // Given
    const updated = { ...mockProposal, proposalName: 'Novo Nome' };
    vi.mocked(api.put).mockResolvedValueOnce({ data: updated });

    // When
    const result = await proposalApi.update('proposal-uuid-1', { proposalName: 'Novo Nome' });

    // Then
    expect(result.proposalName).toBe('Novo Nome');
    expect(api.put).toHaveBeenCalledWith('/proposals/proposal-uuid-1', { proposalName: 'Novo Nome' });
  });

  it('should_throwConflictError_whenProposalIsNotDraft', async () => {
    // Given
    vi.mocked(api.put).mockRejectedValueOnce(
      makeAxiosError(409, { detail: 'Operação inválida para o estado atual da proposta.' })
    );

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.update('proposal-uuid-1', { proposalName: 'Qualquer' });
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('conflict');
    expect(caught?.message).toBe('Operação inválida para o estado atual da proposta.');
  });
});

// ---------------------------------------------------------------------------
// proposalApi.remove
// ---------------------------------------------------------------------------

describe('proposalApi.remove', () => {
  it('should_resolveWithoutValue_whenDeleteSucceeds', async () => {
    // Given
    vi.mocked(api.delete).mockResolvedValueOnce({ status: 204 });

    // When / Then
    await expect(proposalApi.remove('proposal-uuid-1')).resolves.toBeUndefined();
    expect(api.delete).toHaveBeenCalledWith('/proposals/proposal-uuid-1');
  });

  it('should_throwNotFoundError_whenProposalDoesNotExist', async () => {
    // Given
    vi.mocked(api.delete).mockRejectedValueOnce(makeAxiosError(404));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.remove('non-existing-id');
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('not_found');
  });
});

// ---------------------------------------------------------------------------
// proposalApi.publish
// ---------------------------------------------------------------------------

describe('proposalApi.publish', () => {
  it('should_returnPublishedProposal_whenTransitionSucceeds', async () => {
    // Given
    const published = { ...mockProposal, status: 'PUBLISHED' as const };
    vi.mocked(api.post).mockResolvedValueOnce({ data: published });

    // When
    const result = await proposalApi.publish('proposal-uuid-1');

    // Then
    expect(result.status).toBe('PUBLISHED');
    expect(api.post).toHaveBeenCalledWith('/proposals/proposal-uuid-1/publish');
  });

  it('should_throwConflictError_whenProposalNotInDraft', async () => {
    // Given
    vi.mocked(api.post).mockRejectedValueOnce(makeAxiosError(409));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.publish('proposal-uuid-1');
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('conflict');
  });
});

// ---------------------------------------------------------------------------
// proposalApi.updateScope
// ---------------------------------------------------------------------------

describe('proposalApi.updateScope', () => {
  it('should_returnProposalWithScope_whenUpdateSucceeds', async () => {
    // Given
    const scopePayload = {
      scope: {
        deliverables: [{ name: 'Landing Page', description: 'Página de vendas', acceptanceCriteria: 'Aprovado pelo cliente' }],
        exclusions: ['SEO avançado'],
        assumptions: ['Cliente fornece copy'],
        price: { amount: 5000, currency: 'BRL', breakdown: 'R$ 5.000 à vista' },
        timeline: null,
      },
    };
    const updated = { ...mockProposal, scope: scopePayload.scope };
    vi.mocked(api.post).mockResolvedValueOnce({ data: updated });

    // When
    const result = await proposalApi.updateScope('proposal-uuid-1', scopePayload);

    // Then
    expect(result.scope).toEqual(scopePayload.scope);
    expect(api.post).toHaveBeenCalledWith('/proposals/proposal-uuid-1/update-scope', scopePayload);
  });
});

// ---------------------------------------------------------------------------
// proposalApi.initiateApproval
// ---------------------------------------------------------------------------

describe('proposalApi.initiateApproval', () => {
  it('should_callCorrectEndpoint_whenApproverEmailsProvided', async () => {
    // Given
    const approvalPayload = { approverEmails: ['approver@example.com', 'manager@example.com'] };
    vi.mocked(api.post).mockResolvedValueOnce({ data: mockProposal });

    // When
    await proposalApi.initiateApproval('proposal-uuid-1', approvalPayload);

    // Then
    expect(api.post).toHaveBeenCalledWith(
      '/proposals/proposal-uuid-1/initiate-approval',
      approvalPayload
    );
  });

  it('should_throwValidationError_whenApproverListIsEmpty', async () => {
    // Given
    vi.mocked(api.post).mockRejectedValueOnce(makeAxiosError(400));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.initiateApproval('proposal-uuid-1', { approverEmails: [] });
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('validation');
  });
});

// ---------------------------------------------------------------------------
// normalizeProposalError — casos de fallback
// ---------------------------------------------------------------------------

describe('normalizeProposalError (via any endpoint)', () => {
  it('should_throwServerError_withDefaultMessage_whenResponseHasNoBody', async () => {
    // Given — 502 sem body
    vi.mocked(api.get).mockRejectedValueOnce(makeAxiosError(502, {}));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.list();
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('server_error');
    expect(caught?.message).toContain('502');
  });

  it('should_throwServerError_whenNonAxiosErrorThrown', async () => {
    // Given — TypeError puro (não-Axios)
    vi.mocked(api.get).mockRejectedValueOnce(new TypeError('Cannot read properties of undefined'));

    // When
    let caught: ProposalApiError | undefined;
    try {
      await proposalApi.list();
    } catch (err) {
      caught = err as ProposalApiError;
    }

    // Then
    expect(caught?.kind).toBe('server_error');
  });
});
