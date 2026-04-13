import axios from 'axios';
import { env } from '@/env';
import { shouldRefreshToken } from './jwt';
import useSessionStore from '@/stores/useSession';

let refreshTokenPromise: Promise<string> | null = null;

/**
 * Wraps a promise com um timeout máximo.
 * Se o promise não resolver/rejeitar dentro do prazo, rejeita com Error('Request timeout').
 */
function withTimeout<T>(promise: Promise<T>, timeoutMs: number): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((_, reject) =>
      setTimeout(() => reject(new Error('Request timeout')), timeoutMs),
    ),
  ]);
}

const api = axios.create({
  baseURL: env.apiUrl,
  withCredentials: true, // Envia httpOnly cookie de refresh
});

// Interceptor de request: adiciona Authorization e faz refresh proativo
api.interceptors.request.use(async (config) => {
  const { accessToken } = useSessionStore.getState();

  if (accessToken && shouldRefreshToken(accessToken)) {
    if (!refreshTokenPromise) {
      // /auth/refresh retorna apenas { accessToken, expiresIn } — sem dados do usuário
      const rawPromise = api
        .post<{ accessToken: string; expiresIn: number }>('/auth/refresh')
        .then((res) => {
          const newToken = res.data.accessToken;
          // Preserva o usuário já armazenado — refresh não retorna user no body.
          // Se user for null (edge case de store corrupto), clearSession evita estado inválido.
          const currentUser = useSessionStore.getState().user;
          if (currentUser) {
            useSessionStore.getState().setSession(newToken, currentUser);
          }
          return newToken;
        })
        .catch((err: unknown) => {
          useSessionStore.getState().clearSession();
          if (typeof window !== 'undefined') {
            window.location.href = '/auth/login';
          }
          throw err;
        })
        .finally(() => {
          refreshTokenPromise = null;
        });

      // Timeout de 30s: evita que o mutex trave requisições indefinidamente
      refreshTokenPromise = withTimeout(rawPromise, 30_000);
    }

    try {
      const newToken = await refreshTokenPromise;
      config.headers.Authorization = `Bearer ${newToken}`;
    } catch {
      // Refresh falhou; prossegue sem token — response interceptor trata o 401
    }

    return config;
  }

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

// Interceptor de response: redireciona para login em 401
// Exclui /auth/refresh e /auth/login para evitar loops de redirecionamento
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const url = error.config?.url ?? '';
      const isAuthEndpoint =
        url.includes('/auth/refresh') || url.includes('/auth/login');
      if (!isAuthEndpoint) {
        useSessionStore.getState().clearSession();
        if (typeof window !== 'undefined') {
          window.location.href = '/auth/login';
        }
      }
    }
    return Promise.reject(error);
  },
);

export default api;
