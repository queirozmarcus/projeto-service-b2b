import { create } from 'zustand';
import type { AuthState, User } from '@/types/auth';

const SESSION_NEEDS_WORKSPACE_KEY = 'scopeflow:needsWorkspace';

function readNeedsWorkspaceFromStorage(): boolean {
  if (typeof window === 'undefined') return false;
  return sessionStorage.getItem(SESSION_NEEDS_WORKSPACE_KEY) === 'true';
}

const useSessionStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  needsWorkspace: readNeedsWorkspaceFromStorage(),

  setSession: (token: string, user: User) => {
    const needsWorkspace = !user.workspaceId;
    if (typeof window !== 'undefined') {
      if (needsWorkspace) {
        sessionStorage.setItem(SESSION_NEEDS_WORKSPACE_KEY, 'true');
      } else {
        sessionStorage.removeItem(SESSION_NEEDS_WORKSPACE_KEY);
      }
    }
    set({ accessToken: token, user, isAuthenticated: true, error: null, needsWorkspace });
  },

  clearSession: () => {
    if (typeof window !== 'undefined') {
      sessionStorage.removeItem(SESSION_NEEDS_WORKSPACE_KEY);
    }
    set({ accessToken: null, user: null, isAuthenticated: false, needsWorkspace: false });
  },

  setLoading: (loading: boolean) => set({ isLoading: loading }),

  setError: (error: string | null) => set({ error }),

  setNeedsWorkspace: (value: boolean) => {
    if (typeof window !== 'undefined') {
      if (value) {
        sessionStorage.setItem(SESSION_NEEDS_WORKSPACE_KEY, 'true');
      } else {
        sessionStorage.removeItem(SESSION_NEEDS_WORKSPACE_KEY);
      }
    }
    set({ needsWorkspace: value });
  },
}));

export default useSessionStore;
