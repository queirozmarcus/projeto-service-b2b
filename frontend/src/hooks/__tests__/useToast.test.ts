import { renderHook, act } from '@testing-library/react';
import { useToast } from '@/hooks/useToast';

// ---------------------------------------------------------------------------
// Reset do store entre testes
// ---------------------------------------------------------------------------
// useToast usa um Zustand store interno. Para garantir isolamento entre testes,
// descartamos toasts existentes chamando dismiss em todos antes de cada suite.
// O hook não expõe clearAll, por isso usamos dismiss individualmente via estado.

beforeEach(() => {
  // Renderiza uma instância temporária só para limpar os toasts
  const { result, unmount } = renderHook(() => useToast());
  act(() => {
    result.current.toasts.forEach((t) => result.current.dismiss(t.id));
  });
  unmount();
});

// ---------------------------------------------------------------------------
// Adição de toasts por tipo
// ---------------------------------------------------------------------------

describe('useToast — success', () => {
  it('should add a toast with type "success" when success() is called', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Operação realizada com sucesso');
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0]).toMatchObject({
      type: 'success',
      message: 'Operação realizada com sucesso',
    });
    expect(result.current.toasts[0].id).toBeTruthy();
  });
});

describe('useToast — error', () => {
  it('should add a toast with type "error" when error() is called', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.error('Algo deu errado');
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0]).toMatchObject({
      type: 'error',
      message: 'Algo deu errado',
    });
  });
});

describe('useToast — info', () => {
  it('should add a toast with type "info" when info() is called', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.info('Informação importante');
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0]).toMatchObject({
      type: 'info',
      message: 'Informação importante',
    });
  });
});

// ---------------------------------------------------------------------------
// Dismiss
// ---------------------------------------------------------------------------

describe('useToast — dismiss', () => {
  it('should remove the correct toast when dismiss() is called with its id', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Toast A');
      result.current.success('Toast B');
    });

    const idToRemove = result.current.toasts[0].id;

    act(() => {
      result.current.dismiss(idToRemove);
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0].message).toBe('Toast B');
  });

  it('should not remove other toasts when dismissing one specific id', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.error('Erro crítico');
      result.current.info('Dica');
    });

    const errorId = result.current.toasts.find((t) => t.type === 'error')!.id;

    act(() => {
      result.current.dismiss(errorId);
    });

    expect(result.current.toasts).toHaveLength(1);
    expect(result.current.toasts[0].type).toBe('info');
  });

  it('should result in empty list when the only toast is dismissed', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Único toast');
    });

    const { id } = result.current.toasts[0];

    act(() => {
      result.current.dismiss(id);
    });

    expect(result.current.toasts).toHaveLength(0);
  });

  it('should not throw when dismiss() is called with an unknown id', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Toast existente');
    });

    act(() => {
      result.current.dismiss('id-inexistente');
    });

    // Toast original permanece intacto
    expect(result.current.toasts).toHaveLength(1);
  });
});

// ---------------------------------------------------------------------------
// Limite de 3 toasts simultâneos
// ---------------------------------------------------------------------------

describe('useToast — max toasts limit', () => {
  it('should not exceed 3 simultaneous toasts when a 4th is added', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Toast 1');
      result.current.success('Toast 2');
      result.current.success('Toast 3');
      result.current.success('Toast 4'); // 4º deve substituir o mais antigo
    });

    expect(result.current.toasts).toHaveLength(3);
  });

  it('should remove the oldest toast when the limit is exceeded', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('Primeiro');
      result.current.success('Segundo');
      result.current.success('Terceiro');
    });

    const firstId = result.current.toasts[0].id;

    act(() => {
      result.current.info('Quarto — expulsa o primeiro');
    });

    const ids = result.current.toasts.map((t) => t.id);
    expect(ids).not.toContain(firstId);
  });

  it('should keep the newest toast as the last in the list when limit is exceeded', () => {
    const { result } = renderHook(() => useToast());

    act(() => {
      result.current.success('A');
      result.current.success('B');
      result.current.success('C');
      result.current.error('D — mais recente');
    });

    const last = result.current.toasts[result.current.toasts.length - 1];
    expect(last.message).toBe('D — mais recente');
    expect(last.type).toBe('error');
  });
});
