export interface User {
  id: string;
  email: string;
  fullName: string;
  role: 'owner' | 'admin' | 'member';
  workspaceId: string;
}

export interface AuthState {
  accessToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  needsWorkspace: boolean;
  setSession: (token: string, user: User) => void;
  clearSession: () => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  setNeedsWorkspace: (value: boolean) => void;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  workspaceName: string;
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;
}
