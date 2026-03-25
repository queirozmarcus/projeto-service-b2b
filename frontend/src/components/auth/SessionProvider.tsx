'use client';

import { useEffect } from 'react';
import api from '@/lib/api';
import { authBroadcaster } from '@/lib/broadcast';
import useSessionStore from '@/stores/useSession';
import type { User } from '@/types/auth';

interface RefreshResponse {
  accessToken: string;
  user: User;
}

/**
 * Inicializa sessão no cold start via silent refresh e sincroniza logout entre abas.
 * Renderizado como filho do RootLayout (Server Component) para preservar metadata.
 */
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const { setSession, clearSession } = useSessionStore();

  useEffect(() => {
    // Silent refresh: verifica se há refresh token (httpOnly cookie) válido
    api
      .post<RefreshResponse>('/auth/refresh')
      .then(({ data }) => {
        setSession(data.accessToken, data.user);
      })
      .catch(() => {
        // Cookie ausente ou expirado — sessão inicia limpa
        clearSession();
      });

    // Sincronizar logout entre abas via BroadcastChannel
    authBroadcaster.onMessage((message) => {
      if (message.type === 'logout') {
        clearSession();
      }
    });

    return () => {
      authBroadcaster.close();
    };
  }, [setSession, clearSession]);

  return <>{children}</>;
}
