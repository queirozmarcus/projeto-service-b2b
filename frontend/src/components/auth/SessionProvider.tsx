'use client';

import { useEffect } from 'react';
import api from '@/lib/api';
import { authBroadcaster } from '@/lib/broadcast';
import useSessionStore from '@/stores/useSession';
import type { User } from '@/types/auth';

// /auth/refresh retorna apenas o novo accessToken (sem dados do usuário)
interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
}

// /auth/me retorna o perfil completo do usuário autenticado
interface MeResponse {
  id: string;
  email: string;
  fullName: string;
  workspaceId?: string;
}

/**
 * Inicializa sessão no cold start via silent refresh e sincroniza logout entre abas.
 * Renderizado como filho do RootLayout (Server Component) para preservar metadata.
 *
 * Fluxo de inicialização:
 * 1. POST /auth/refresh → troca httpOnly cookie por novo accessToken
 * 2. GET /auth/me com token explícito no header — necessário porque o store ainda
 *    não foi populado neste ponto, então o interceptor proativo não injeta o header.
 *    Passar o token diretamente evita a race condition onde /auth/me seria chamado
 *    sem Authorization e retornaria 401, disparando redirect para /auth/login.
 * 3. setSession(token, user) → popula o store e marca sessão como autenticada.
 */
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const { setSession, clearSession } = useSessionStore();

  useEffect(() => {
    api
      .post<RefreshResponse>('/auth/refresh')
      .then(async ({ data }) => {
        // Header explícito necessário: o store ainda não tem o token neste momento,
        // portanto o interceptor proativo não adicionaria Authorization automaticamente.
        const me = await api.get<MeResponse>('/auth/me', {
          headers: { Authorization: `Bearer ${data.accessToken}` },
        });
        const user: User = {
          id: me.data.id,
          email: me.data.email,
          fullName: me.data.fullName,
          role: 'owner',
          workspaceId: me.data.workspaceId ?? '',
        };
        setSession(data.accessToken, user);
      })
      .catch(() => {
        // Cookie ausente ou expirado — sessão inicia limpa
        clearSession();
      });

    // Sincronizar logout entre abas via BroadcastChannel:
    // limpa estado local e faz hard navigation para que o middleware
    // veja o cookie já removido pelo logout da aba originária.
    authBroadcaster.onMessage((message) => {
      if (message.type === 'logout') {
        clearSession();
        if (typeof window !== 'undefined') {
          window.location.href = '/auth/login';
        }
      }
    });

    return () => {
      authBroadcaster.close();
    };
  }, [setSession, clearSession]);

  return <>{children}</>;
}
