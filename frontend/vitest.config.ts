import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

/**
 * Configuração do Vitest para testes unitários de componentes React.
 *
 * Requer (instalar antes de rodar):
 *   npm install -D @vitejs/plugin-react @testing-library/user-event
 *
 * - environment: 'jsdom'  → APIs de DOM para @testing-library/react
 * - globals: true         → describe/it/expect/vi sem imports
 * - setupFiles            → jest-dom matchers via @testing-library/jest-dom/vitest
 */
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: [
        'src/components/**',
        'src/hooks/**',
        'src/stores/**',
        'src/lib/**',
      ],
      exclude: ['src/test/**', 'src/**/*.d.ts'],
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 75,
        statements: 80,
      },
    },
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
});
