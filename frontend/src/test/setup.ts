import { afterEach, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

// Limpa todos os mocks entre os testes para evitar contaminação de estado
afterEach(() => {
  vi.clearAllMocks();
});
