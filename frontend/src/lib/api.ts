import axios from 'axios';
import { env } from '@/env';
import { shouldRefreshToken } from './jwt';
import useSessionStore from '@/stores/useSession';

let refreshTokenPromise: Promise<string> | null = null;

const api = axios.create({
  baseURL: env.apiUrl,
  withCredentials: true, // Envia httpOnly cookie de refresh
});

// Interceptor de request: adiciona Authorization e faz refresh proativo
api.interceptors.request.use(async (config) => {
  const { accessToken, user } = useSessionStore.getState();

  if (accessToken && shouldRefreshToken(accessToken)) {
    if (!refreshTokenPromise) {
      refreshTokenPromise = api
        .post<{ accessToken: string; user: typeof user }>('/auth/refresh')
        .then((res) => {
          const { accessToken: newToken, user: refreshedUser } = res.data;
          if (refreshedUser) {
            useSessionStore.getState().setSession(newToken, refreshedUser);
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
    }

    const newToken = await refreshTokenPromise;
    config.headers.Authorization = `Bearer ${newToken}`;
    return config;
  }

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

// Interceptor de response: redireciona para login em 401
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401
    ) {
      useSessionStore.getState().clearSession();
      if (typeof window !== 'undefined') {
        window.location.href = '/auth/login';
      }
    }
    return Promise.reject(error);
  },
);

export default api;
