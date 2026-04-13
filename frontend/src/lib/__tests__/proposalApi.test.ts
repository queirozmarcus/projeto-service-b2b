import { AxiosError } from 'axios';
import { proposalApi } from '@/lib/proposalApi';
import type { Proposal, ProposalPage } from '@/types/proposal';

// ---------------------------------------------------------------------------
// Mock do módulo @/lib/api — substitui o cliente Axios real por vi.fn()
// ---------------------------------------------------------------------------

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

// ---------------------------------------------------------------------------
// Helpers para simular erros Axios
// ---------------------------------------------------------------------------

function makeAxiosError(status: number, data?: object): AxiosError {
  const err = new Error('Request failed') as AxiosError;
  err.isAxiosError = true;
  err.response = {
    status,
    data: data ?? {},
    headers: {},
    config: {} as AxiosError['response']['config'],
    statusText: '',
  };
  return err;
}

function makeNetworkError(): AxiosError {
  const err = new Error('Network Error') as AxiosError;
  err.isAxiosError = true;
  err.response = undefined;
  return err;
}

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

const makeProposalPage = (overrides?: Partial<ProposalPage>): ProposalPage => ({
  content: [makeProposal()],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
  first: true,
  last: true,
  ...overrides,
});

// ---------------------------------------------------------------------------
// Acesso ao mock após import
// ---------------------------------------------------------------------------

async function getApiMock() {
  const { default: api } = await import('@/lib/api');
  return api as {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    put: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
}

// ---------------------------------------------------------------------------
// Suites
// ---------------------------------------------------------------------------

describe('proposalApi.list', () => {
  it('should return ProposalPage when API responds 200', async () => {
    const page = makeProposalPage();
    const mock = await getApiMock();
    mock.get.mockResolvedValueOnce({ data: page });

    const result = await proposalApi.list();

    expect(result).toEqual(page);
    expect(mock.get).toHaveBeenCalledWith('/proposals', { params: undefined });
  });

  it('should throw ProposalApiError with kind "network" when there is no response', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(makeNetworkError());

    await expect(proposalApi.list()).rejects.toMatchObject({ kind: 'network' });
  });

  it('should throw ProposalApiError with kind "server_error" when API responds 500', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(makeAxiosError(500));

    await expect(proposalApi.list()).rejects.toMatchObject({ kind: 'server_error' });
  });

  it('should forward optional params to the GET request', async () => {
    const page = makeProposalPage({ content: [] });
    const mock = await getApiMock();
    mock.get.mockResolvedValueOnce({ data: page });

    await proposalApi.list({ page: 1, size: 10, status: 'DRAFT' });

    expect(mock.get).toHaveBeenCalledWith('/proposals', {
      params: { page: 1, size: 10, status: 'DRAFT' },
    });
  });
});

describe('proposalApi.getById', () => {
  it('should return Proposal when API responds 200', async () => {
    const proposal = makeProposal();
    const mock = await getApiMock();
    mock.get.mockResolvedValueOnce({ data: proposal });

    const result = await proposalApi.getById('p-1');

    expect(result).toEqual(proposal);
    expect(mock.get).toHaveBeenCalledWith('/proposals/p-1');
  });

  it('should throw ProposalApiError with kind "not_found" when API responds 404', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(makeAxiosError(404));

    await expect(proposalApi.getById('p-missing')).rejects.toMatchObject({
      kind: 'not_found',
    });
  });
});

describe('proposalApi.create', () => {
  it('should return created Proposal when API responds 201', async () => {
    const proposal = makeProposal({ status: 'DRAFT' });
    const mock = await getApiMock();
    mock.post.mockResolvedValueOnce({ data: proposal });

    const result = await proposalApi.create({
      clientId: 'c-1',
      briefingId: 'b-1',
      proposalName: 'Proposta Teste',
    });

    expect(result).toEqual(proposal);
    expect(mock.post).toHaveBeenCalledWith('/proposals', {
      clientId: 'c-1',
      briefingId: 'b-1',
      proposalName: 'Proposta Teste',
    });
  });

  it('should throw ProposalApiError with kind "conflict" when API responds 409', async () => {
    const mock = await getApiMock();
    mock.post.mockRejectedValueOnce(makeAxiosError(409));

    await expect(
      proposalApi.create({ clientId: 'c-1', briefingId: 'b-1', proposalName: 'X' }),
    ).rejects.toMatchObject({ kind: 'conflict' });
  });
});

describe('proposalApi.publish', () => {
  it('should return Proposal with status PUBLISHED when API responds 200', async () => {
    const proposal = makeProposal({ status: 'PUBLISHED' });
    const mock = await getApiMock();
    mock.post.mockResolvedValueOnce({ data: proposal });

    const result = await proposalApi.publish('p-1');

    expect(result.status).toBe('PUBLISHED');
    expect(mock.post).toHaveBeenCalledWith('/proposals/p-1/publish');
  });

  it('should throw ProposalApiError with kind "conflict" when API responds 409', async () => {
    const mock = await getApiMock();
    mock.post.mockRejectedValueOnce(makeAxiosError(409));

    await expect(proposalApi.publish('p-1')).rejects.toMatchObject({ kind: 'conflict' });
  });
});

describe('proposalApi.remove', () => {
  it('should resolve without value when API responds 204', async () => {
    const mock = await getApiMock();
    mock.delete.mockResolvedValueOnce({ status: 204 });

    await expect(proposalApi.remove('p-1')).resolves.toBeUndefined();
    expect(mock.delete).toHaveBeenCalledWith('/proposals/p-1');
  });

  it('should throw ProposalApiError with kind "not_found" when API responds 404', async () => {
    const mock = await getApiMock();
    mock.delete.mockRejectedValueOnce(makeAxiosError(404));

    await expect(proposalApi.remove('p-missing')).rejects.toMatchObject({
      kind: 'not_found',
    });
  });
});

// ---------------------------------------------------------------------------
// Cobertura extra: normalizeProposalError por status code
// ---------------------------------------------------------------------------

describe('proposalApi error normalization', () => {
  it('should throw kind "validation" on 400', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(makeAxiosError(400));

    await expect(proposalApi.getById('p-1')).rejects.toMatchObject({ kind: 'validation' });
  });

  it('should throw kind "forbidden" on 403', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(makeAxiosError(403));

    await expect(proposalApi.getById('p-1')).rejects.toMatchObject({ kind: 'forbidden' });
  });

  it('should include server message in error when API provides detail field', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(
      makeAxiosError(404, { detail: 'Proposta não existe' }),
    );

    await expect(proposalApi.getById('p-1')).rejects.toMatchObject({
      kind: 'not_found',
      message: 'Proposta não existe',
    });
  });

  it('should fall back to default message when server provides no detail', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(makeAxiosError(404));

    await expect(proposalApi.getById('p-1')).rejects.toMatchObject({
      kind: 'not_found',
      message: 'Proposta não encontrada.',
    });
  });

  it('should throw kind "server_error" for non-Axios errors', async () => {
    const mock = await getApiMock();
    mock.get.mockRejectedValueOnce(new TypeError('unexpected'));

    await expect(proposalApi.list()).rejects.toMatchObject({ kind: 'server_error' });
  });
});
