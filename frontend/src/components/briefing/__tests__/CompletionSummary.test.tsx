import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { CompletionResult } from '@/types/briefing';

// ---------------------------------------------------------------------------
// Mock do módulo next/link — sem contexto de router do Next.js nos testes
// ---------------------------------------------------------------------------
vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

// ---------------------------------------------------------------------------
// Mock do Zustand store useSession
// CompletionSummary usa o seletor: useSessionStore((s) => s.isAuthenticated)
// ---------------------------------------------------------------------------
vi.mock('@/stores/useSession', () => ({
  default: vi.fn(),
}));

import { CompletionSummary } from '../CompletionSummary';
import useSessionStore from '@/stores/useSession';

// Tipagem do mock
const mockUseSessionStore = vi.mocked(useSessionStore) as unknown as ReturnType<typeof vi.fn>;

// Simula o comportamento do Zustand: recebe seletor e aplica no estado mockado
function mockSession(isAuthenticated: boolean) {
  mockUseSessionStore.mockImplementation(
    (selector: (s: { isAuthenticated: boolean }) => boolean) =>
      selector({ isAuthenticated }),
  );
}

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const highScoreResult: CompletionResult = {
  completenessScore: 95,
  status: 'COMPLETED',
  message: 'Briefing completed successfully! All required questions were answered.',
};

const lowScoreResult: CompletionResult = {
  completenessScore: 60,
  status: 'COMPLETED',
  message: 'Briefing completed with low score (60%). Consider providing more details.',
};

const exactBorderResult: CompletionResult = {
  completenessScore: 80,
  status: 'COMPLETED',
  message: 'Briefing completed successfully! Good job.',
};

const SESSION_ID = 'session-test-uuid-abc123';

// ---------------------------------------------------------------------------
// Suite
// ---------------------------------------------------------------------------

describe('CompletionSummary', () => {
  beforeEach(() => {
    // Padrão: usuário não autenticado (cenário do cliente final)
    mockSession(false);
  });

  // -------------------------------------------------------------------------
  // Elementos sempre presentes
  // -------------------------------------------------------------------------

  it('should always display the "Briefing Completed!" heading', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByRole('heading', { level: 1, name: 'Briefing Completed!' })).toBeInTheDocument();
  });

  it('should display the completeness score as a percentage', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    // Score como texto: "95%" (ver CompletionSummary.tsx)
    expect(screen.getByText('95%')).toBeInTheDocument();
  });

  it('should display the message returned by the backend', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText(highScoreResult.message)).toBeInTheDocument();
  });

  it('should display the thank-you paragraph about proposal preparation', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText(/thank you for taking the time/i)).toBeInTheDocument();
  });

  it('should display "Completeness Score" label above the percentage', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText(/completeness score/i)).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Score >= 80%: estado positivo (verde)
  // -------------------------------------------------------------------------

  it('should display score in green color class for score >= 80%', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText('95%')).toHaveClass('text-green-600');
  });

  it('should display score in green color class at exact 80% boundary', () => {
    render(<CompletionSummary completionResult={exactBorderResult} sessionId={SESSION_ID} />);

    expect(screen.getByText('80%')).toHaveClass('text-green-600');
  });

  it('should display celebration emoji for score >= 80%', () => {
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    // role="img" com aria-label="Celebration" — ver CompletionSummary.tsx
    expect(screen.getByRole('img', { name: 'Celebration' })).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Score < 80%: estado de aviso (laranja)
  // -------------------------------------------------------------------------

  it('should display low score (60%) in orange color class', () => {
    render(<CompletionSummary completionResult={lowScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText('60%')).toHaveClass('text-orange-500');
  });

  it('should display thumbs-up emoji for score < 80%', () => {
    render(<CompletionSummary completionResult={lowScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByRole('img', { name: 'Thumbs up' })).toBeInTheDocument();
  });

  it('should display the low score message from backend', () => {
    render(<CompletionSummary completionResult={lowScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText(/low score/i)).toBeInTheDocument();
  });

  it('should NOT show celebration emoji for low score', () => {
    render(<CompletionSummary completionResult={lowScoreResult} sessionId={SESSION_ID} />);

    expect(screen.queryByRole('img', { name: 'Celebration' })).not.toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // CTA: Usuário NÃO autenticado (cliente final)
  // -------------------------------------------------------------------------

  it('should show "we will reach out" message for unauthenticated user', () => {
    mockSession(false);
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.getByText(/we.ll reach out shortly/i)).toBeInTheDocument();
  });

  it('should NOT show dashboard link for unauthenticated user', () => {
    mockSession(false);
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(
      screen.queryByRole('link', { name: /review proposal in dashboard/i }),
    ).not.toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // CTA: Usuário AUTENTICADO (service provider testando o fluxo)
  // -------------------------------------------------------------------------

  it('should show dashboard link for authenticated user', () => {
    mockSession(true);
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    const link = screen.getByRole('link', { name: /review proposal in dashboard/i });
    expect(link).toBeInTheDocument();
  });

  it('should link to correct proposal URL using sessionId', () => {
    mockSession(true);
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    const link = screen.getByRole('link', { name: /review proposal in dashboard/i });
    expect(link).toHaveAttribute('href', `/dashboard/proposals/${SESSION_ID}`);
  });

  it('should NOT show "we will reach out" for authenticated user', () => {
    mockSession(true);
    render(<CompletionSummary completionResult={highScoreResult} sessionId={SESSION_ID} />);

    expect(screen.queryByText(/we.ll reach out shortly/i)).not.toBeInTheDocument();
  });

  it('should show dashboard link even for low score when authenticated', () => {
    mockSession(true);
    render(<CompletionSummary completionResult={lowScoreResult} sessionId={SESSION_ID} />);

    const link = screen.getByRole('link', { name: /review proposal in dashboard/i });
    expect(link).toHaveAttribute('href', `/dashboard/proposals/${SESSION_ID}`);
  });
});
