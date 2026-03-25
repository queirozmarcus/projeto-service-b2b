import api from '@/lib/api';
import { authBroadcaster } from '@/lib/broadcast';
import useSessionStore from '@/stores/useSession';
import type { LoginRequest, RegisterRequest, User } from '@/types/auth';

// Resposta flat do backend: access token + campos do usuário direto no body.
// O refresh token é entregue via httpOnly cookie (Set-Cookie) — nunca no body.
interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  userId: string;
  email: string;
  fullName: string;
  workspaceId?: string;
}

function mapLoginResponseToUser(data: LoginResponse): User {
  return {
    id: data.userId,
    email: data.email,
    fullName: data.fullName,
    role: 'owner',
    workspaceId: data.workspaceId ?? '',
  };
}

export function useAuth() {
  const { setSession, clearSession, setLoading, setError } = useSessionStore();

  const login = async (credentials: LoginRequest): Promise<User> => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.post<LoginResponse>('/auth/login', credentials);
      const user = mapLoginResponseToUser(response.data);
      setSession(response.data.accessToken, user);
      return user;
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { detail?: string; error?: string } } })
          ?.response?.data?.detail ??
        (err as { response?: { data?: { error?: string } } })?.response?.data
          ?.error ??
        'Erro ao fazer login';
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
      // Backend não recebe workspaceName nem confirmPassword — omite antes de enviar
      const { workspaceName: _ws, confirmPassword: _cp, ...payload } = data;
      void _ws;
      void _cp;
      const response = await api.post<LoginResponse>('/auth/register', payload);
      const user = mapLoginResponseToUser(response.data);
      setSession(response.data.accessToken, user);
      return user;
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { detail?: string; error?: string } } })
          ?.response?.data?.detail ??
        (err as { response?: { data?: { error?: string } } })?.response?.data
          ?.error ??
        'Erro ao criar conta';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = (): void => {
    clearSession();
    // Invalida o httpOnly cookie no servidor: Set-Cookie refreshToken=; max-age=0.
    // Broadcast e navegação ocorrem após o cookie ser removido, para que o
    // middleware permita /auth/login em todas as abas sem loop de redirect.
    api
      .post('/auth/logout')
      .catch(() => {
        // Ignora erros — logout local já foi feito; cookie pode já ter expirado
      })
      .finally(() => {
        authBroadcaster.broadcast({ type: 'logout' });
        if (typeof window !== 'undefined') {
          window.location.href = '/auth/login';
        }
      });
  };

  return { login, register, logout };
}
