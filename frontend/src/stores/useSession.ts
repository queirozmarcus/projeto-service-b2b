import { create } from 'zustand';
import type { AuthState, User } from '@/types/auth';

const useSessionStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  setSession: (token: string, user: User) =>
    set({ accessToken: token, user, isAuthenticated: true, error: null }),

  clearSession: () =>
    set({ accessToken: null, user: null, isAuthenticated: false }),

  setLoading: (loading: boolean) => set({ isLoading: loading }),

  setError: (error: string | null) => set({ error }),
}));

export default useSessionStore;
