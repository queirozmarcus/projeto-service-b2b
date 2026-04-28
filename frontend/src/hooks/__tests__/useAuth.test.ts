import { renderHook, act } from '@testing-library/react';
import { useAuth } from '@/hooks/useAuth';
import useSessionStore from '@/stores/useSession';

// ---------------------------------------------------------------------------
// Mock do módulo @/lib/api
// ---------------------------------------------------------------------------

vi.mock('@/lib/api', () => ({
  default: {
    post: vi.fn(),
  },
}));

// Mock do broadcast — evita erro de BroadcastChannel em jsdom
vi.mock('@/lib/broadcast', () => ({
  authBroadcaster: { broadcast: vi.fn() },
}));

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mockApi() {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  return (await import('@/lib/api')).default as { post: ReturnType<typeof vi.fn> };
}

const REGISTER_RESPONSE = {
  data: {
    accessToken: 'token-initial',
    expiresIn: 3600,
    userId: 'user-1',
    email: 'test@example.com',
    fullName: 'Test User',
    workspaceId: '',
  },
};

const REFRESH_RESPONSE = {
  data: { accessToken: 'token-refreshed', expiresIn: 3600 },
};

const WORKSPACE_RESPONSE = { data: { id: 'ws-1' } };

// ---------------------------------------------------------------------------
// Reset entre testes
// ---------------------------------------------------------------------------

beforeEach(() => {
  useSessionStore.setState({
    accessToken: null,
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
    needsWorkspace: false,
  });
  vi.clearAllMocks();
});

// ---------------------------------------------------------------------------
// register — happy path
// ---------------------------------------------------------------------------

describe('useAuth.register — happy path', () => {
  it('should call /auth/register → /workspaces → /auth/refresh in sequence', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)   // POST /auth/register
      .mockResolvedValueOnce(WORKSPACE_RESPONSE)  // POST /workspaces
      .mockResolvedValueOnce(REFRESH_RESPONSE);   // POST /auth/refresh

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        workspaceName: 'Minha Agência',
        confirmPassword: 'secret',
      });
    });

    expect(api.post).toHaveBeenCalledTimes(3);
    expect(api.post).toHaveBeenNthCalledWith(1, '/auth/register', {
      email: 'test@example.com',
      password: 'secret',
      fullName: 'Test User',
    });
    expect(api.post).toHaveBeenNthCalledWith(
      2,
      '/workspaces',
      expect.objectContaining({ name: 'Minha Agência' }),
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer token-initial' }) }),
    );
    expect(api.post).toHaveBeenNthCalledWith(3, '/auth/refresh');
  });

  it('should set session with refreshed token and needsWorkspace = false after full success', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockResolvedValueOnce(WORKSPACE_RESPONSE)
      .mockResolvedValueOnce(REFRESH_RESPONSE);

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        confirmPassword: 'secret',
      });
    });

    const state = useSessionStore.getState();
    expect(state.accessToken).toBe('token-refreshed');
    expect(state.needsWorkspace).toBe(false);
    expect(state.isAuthenticated).toBe(true);
  });

  it('should use default workspace name when workspaceName is not provided', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockResolvedValueOnce(WORKSPACE_RESPONSE)
      .mockResolvedValueOnce(REFRESH_RESPONSE);

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        confirmPassword: 'secret',
      });
    });

    expect(api.post).toHaveBeenNthCalledWith(
      2,
      '/workspaces',
      expect.objectContaining({ name: 'Meu Workspace' }),
      expect.anything(),
    );
  });
});

// ---------------------------------------------------------------------------
// register — workspace creation fails
// ---------------------------------------------------------------------------

describe('useAuth.register — workspace creation fails', () => {
  it('should set needsWorkspace = true when POST /workspaces returns 500', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockRejectedValueOnce(new Error('Internal Server Error')); // /workspaces fails

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        confirmPassword: 'secret',
      });
    });

    const state = useSessionStore.getState();
    expect(state.needsWorkspace).toBe(true);
    expect(state.isAuthenticated).toBe(true); // login prossegue mesmo assim
    expect(state.accessToken).toBe('token-initial');
  });

  it('should not throw when workspace creation fails', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockRejectedValueOnce(new Error('Internal Server Error'));

    const { result } = renderHook(() => useAuth());

    await expect(
      act(async () => {
        await result.current.register({
          email: 'test@example.com',
          password: 'secret',
          fullName: 'Test User',
          confirmPassword: 'secret',
        });
      }),
    ).resolves.not.toThrow();
  });
});

// ---------------------------------------------------------------------------
// register — refresh fails after workspace created
// ---------------------------------------------------------------------------

describe('useAuth.register — refresh fails after workspace creation', () => {
  it('should set needsWorkspace = true when /auth/refresh fails', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockResolvedValueOnce(WORKSPACE_RESPONSE)
      .mockRejectedValueOnce(new Error('Refresh failed')); // /auth/refresh fails

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        confirmPassword: 'secret',
      });
    });

    const state = useSessionStore.getState();
    expect(state.needsWorkspace).toBe(true);
    expect(state.isAuthenticated).toBe(true);
  });

  it('should not throw when refresh fails after workspace creation', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockResolvedValueOnce(WORKSPACE_RESPONSE)
      .mockRejectedValueOnce(new Error('Refresh failed'));

    const { result } = renderHook(() => useAuth());

    await expect(
      act(async () => {
        await result.current.register({
          email: 'test@example.com',
          password: 'secret',
          fullName: 'Test User',
          confirmPassword: 'secret',
        });
      }),
    ).resolves.not.toThrow();
  });
});

// ---------------------------------------------------------------------------
// register — /auth/register itself fails
// ---------------------------------------------------------------------------

describe('useAuth.register — registration endpoint fails', () => {
  it('should throw and set error when /auth/register returns 409', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post.mockRejectedValueOnce({
      response: { data: { detail: 'Email já cadastrado.' } },
    });

    const { result } = renderHook(() => useAuth());

    await expect(
      act(async () => {
        await result.current.register({
          email: 'taken@example.com',
          password: 'secret',
          fullName: 'Test User',
          confirmPassword: 'secret',
        });
      }),
    ).rejects.toBeTruthy();

    expect(useSessionStore.getState().error).toBe('Email já cadastrado.');
    expect(useSessionStore.getState().isAuthenticated).toBe(false);
  });

  it('should not call /workspaces when /auth/register fails', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        confirmPassword: 'secret',
      }).catch(() => {});
    });

    // Only one call — to /auth/register
    expect(api.post).toHaveBeenCalledTimes(1);
  });
});

// ---------------------------------------------------------------------------
// register — confirmPassword and workspaceName are stripped from payload
// ---------------------------------------------------------------------------

describe('useAuth.register — payload sanitization', () => {
  it('should not send confirmPassword to the backend', async () => {
    const api = await import('@/lib/api').then((m) => m.default) as { post: ReturnType<typeof vi.fn> };

    api.post
      .mockResolvedValueOnce(REGISTER_RESPONSE)
      .mockResolvedValueOnce(WORKSPACE_RESPONSE)
      .mockResolvedValueOnce(REFRESH_RESPONSE);

    const { result } = renderHook(() => useAuth());

    await act(async () => {
      await result.current.register({
        email: 'test@example.com',
        password: 'secret',
        fullName: 'Test User',
        workspaceName: 'Agência X',
        confirmPassword: 'secret',
      });
    });

    const registerPayload = api.post.mock.calls[0][1];
    expect(registerPayload).not.toHaveProperty('confirmPassword');
    expect(registerPayload).not.toHaveProperty('workspaceName');
  });
});
