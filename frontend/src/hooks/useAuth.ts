import api from '@/lib/api';
import { authBroadcaster } from '@/lib/broadcast';
import useSessionStore from '@/stores/useSession';
import type { LoginRequest, RegisterRequest, User } from '@/types/auth';

interface AuthResponse {
  accessToken: string;
  user: User;
}

export function useAuth() {
  const { setSession, clearSession, setLoading, setError } = useSessionStore();

  const login = async (credentials: LoginRequest): Promise<User> => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.post<AuthResponse>('/auth/login', credentials);
      const { accessToken, user } = response.data;
      setSession(accessToken, user);
      return user;
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { error?: string } } })?.response?.data
          ?.error ?? 'Erro ao fazer login';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const register = async (data: RegisterRequest): Promise<User> => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.post<AuthResponse>('/auth/register', data);
      const { accessToken, user } = response.data;
      setSession(accessToken, user);
      return user;
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { error?: string } } })?.response?.data
          ?.error ?? 'Erro ao criar conta';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = (): void => {
    clearSession();
    authBroadcaster.broadcast({ type: 'logout' });
    if (typeof window !== 'undefined') {
      window.location.href = '/auth/login';
    }
  };

  return { login, register, logout };
}
