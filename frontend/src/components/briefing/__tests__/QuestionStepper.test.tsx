import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { QuestionStepper } from '../QuestionStepper';

// ---------------------------------------------------------------------------
// Helper
// ---------------------------------------------------------------------------

function renderStepper(
  overrides: Partial<{
    currentIndex: number;
    totalQuestions: number;
    canComplete: boolean;
    onPrevious: () => void;
    onNext: () => void;
    onComplete: () => Promise<void>;
  }> = {},
) {
  const defaults = {
    currentIndex: 0,
    totalQuestions: 5,
    canComplete: true,
    onPrevious: vi.fn(),
    onNext: vi.fn(),
    onComplete: vi.fn<[], Promise<void>>().mockResolvedValue(undefined),
  };

  return render(<QuestionStepper {...defaults} {...overrides} />);
}

// ---------------------------------------------------------------------------
// Suite
// ---------------------------------------------------------------------------

describe('QuestionStepper', () => {
  // -------------------------------------------------------------------------
  // Estado do botão Previous
  // -------------------------------------------------------------------------

  it('should disable Previous button on the first question (index 0)', () => {
    renderStepper({ currentIndex: 0 });

    expect(screen.getByRole('button', { name: /go to previous question/i })).toBeDisabled();
  });

  it('should enable Previous button on the second question (index 1)', () => {
    renderStepper({ currentIndex: 1 });

    expect(screen.getByRole('button', { name: /go to previous question/i })).toBeEnabled();
  });

  it('should enable Previous button on the last question', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5 });

    expect(screen.getByRole('button', { name: /go to previous question/i })).toBeEnabled();
  });

  // -------------------------------------------------------------------------
  // Visibilidade: Next vs Complete
  // -------------------------------------------------------------------------

  it('should show Next button when NOT on the last question', () => {
    renderStepper({ currentIndex: 0, totalQuestions: 5 });

    expect(screen.getByRole('button', { name: /go to next question/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /complete briefing/i })).not.toBeInTheDocument();
  });

  it('should show Complete Briefing button on the last question', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5 });

    expect(screen.getByRole('button', { name: /complete briefing/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /go to next question/i })).not.toBeInTheDocument();
  });

  it('should show Next button for a middle question', () => {
    renderStepper({ currentIndex: 2, totalQuestions: 5 });

    expect(screen.getByRole('button', { name: /go to next question/i })).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Estado do botão Complete
  // -------------------------------------------------------------------------

  it('should enable Complete button when canComplete is true', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: true });

    expect(screen.getByRole('button', { name: /complete briefing/i })).toBeEnabled();
  });

  it('should disable Complete button when canComplete is false', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: false });

    expect(screen.getByRole('button', { name: /complete briefing/i })).toBeDisabled();
  });

  it('should show title tooltip when canComplete is false', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: false });

    expect(screen.getByRole('button', { name: /complete briefing/i })).toHaveAttribute(
      'title',
      'Answer all required questions to complete',
    );
  });

  it('should NOT have title attribute when canComplete is true', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: true });

    // title é undefined quando canComplete é true
    const btn = screen.getByRole('button', { name: /complete briefing/i });
    expect(btn.getAttribute('title')).toBeNull();
  });

  // -------------------------------------------------------------------------
  // Click handlers
  // -------------------------------------------------------------------------

  it('should call onNext when Next button is clicked', () => {
    const handleNext = vi.fn();
    renderStepper({ currentIndex: 0, onNext: handleNext });

    fireEvent.click(screen.getByRole('button', { name: /go to next question/i }));

    expect(handleNext).toHaveBeenCalledTimes(1);
  });

  it('should call onPrevious when Previous button is clicked', () => {
    const handlePrevious = vi.fn();
    renderStepper({ currentIndex: 1, onPrevious: handlePrevious });

    fireEvent.click(screen.getByRole('button', { name: /go to previous question/i }));

    expect(handlePrevious).toHaveBeenCalledTimes(1);
  });

  it('should call onComplete when Complete button is clicked', async () => {
    const handleComplete = vi.fn<[], Promise<void>>().mockResolvedValue(undefined);

    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: true, onComplete: handleComplete });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /complete briefing/i }));
    });

    expect(handleComplete).toHaveBeenCalledTimes(1);
  });

  // -------------------------------------------------------------------------
  // Estado de submitting (isCompleting interno ao componente)
  // -------------------------------------------------------------------------

  it('should show "Submitting..." and disable buttons during onComplete execution', async () => {
    let resolveCompletion!: () => void;
    const slowCompletion = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveCompletion = resolve;
        }),
    );

    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: true, onComplete: slowCompletion });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /complete briefing/i }));
      // yield para o microtask queue processar o início da Promise
      await Promise.resolve();
    });

    // Spinner + "Submitting..."
    expect(screen.getByText('Submitting...')).toBeInTheDocument();
    expect(screen.queryByText('Complete Briefing')).not.toBeInTheDocument();

    // Previous também desabilitado durante submissão
    expect(screen.getByRole('button', { name: /go to previous question/i })).toBeDisabled();

    // Libera a Promise
    await act(async () => {
      resolveCompletion();
    });
  });

  it('should re-enable buttons after onComplete resolves', async () => {
    const handleComplete = vi.fn<[], Promise<void>>().mockResolvedValue(undefined);

    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: true, onComplete: handleComplete });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /complete briefing/i }));
    });

    // Após resolução: estado de loading removido
    // (QuestionStepper.tsx: finally { setIsCompleting(false) })
    expect(screen.queryByText('Submitting...')).not.toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Acessibilidade
  // -------------------------------------------------------------------------

  it('should have aria-label "Go to previous question" on Previous button', () => {
    renderStepper({ currentIndex: 1 });

    expect(screen.getByRole('button', { name: 'Go to previous question' })).toBeInTheDocument();
  });

  it('should have aria-label "Go to next question" on Next button', () => {
    renderStepper({ currentIndex: 0 });

    expect(screen.getByRole('button', { name: 'Go to next question' })).toBeInTheDocument();
  });

  it('should have aria-label "Complete briefing" on Complete button when idle', () => {
    renderStepper({ currentIndex: 4, totalQuestions: 5, canComplete: true });

    expect(screen.getByRole('button', { name: 'Complete briefing' })).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Edge case: totalQuestions = 1
  // -------------------------------------------------------------------------

  it('should show Complete button immediately when there is only 1 question', () => {
    renderStepper({ currentIndex: 0, totalQuestions: 1 });

    expect(screen.getByRole('button', { name: /complete briefing/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /go to next question/i })).not.toBeInTheDocument();
  });

  it('should have Previous disabled with only 1 question', () => {
    renderStepper({ currentIndex: 0, totalQuestions: 1 });

    expect(screen.getByRole('button', { name: /go to previous question/i })).toBeDisabled();
  });
});
