import { create } from 'zustand';

type ToastType = 'success' | 'error' | 'info';

interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

interface ToastStore {
  toasts: Toast[];
  add: (type: ToastType, message: string) => void;
  dismiss: (id: string) => void;
}

const MAX_TOASTS = 3;
const DISMISS_DELAY_MS: Record<ToastType, number> = {
  success: 4000,
  info: 4000,
  error: 6000,
};

const useToastStore = create<ToastStore>((set) => ({
  toasts: [],

  add: (type, message) => {
    const id = crypto.randomUUID();

    set((state) => {
      const existing = state.toasts;
      // Remove o mais antigo se atingiu o limite
      const trimmed =
        existing.length >= MAX_TOASTS ? existing.slice(1) : existing;
      return { toasts: [...trimmed, { id, type, message }] };
    });

    // Auto-dismiss
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id),
      }));
    }, DISMISS_DELAY_MS[type]);
  },

  dismiss: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    })),
}));

interface UseToastReturn {
  toasts: Toast[];
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
  dismiss: (id: string) => void;
}

export function useToast(): UseToastReturn {
  const { toasts, add, dismiss } = useToastStore();

  return {
    toasts,
    success: (message) => add('success', message),
    error: (message) => add('error', message),
    info: (message) => add('info', message),
    dismiss,
  };
}
