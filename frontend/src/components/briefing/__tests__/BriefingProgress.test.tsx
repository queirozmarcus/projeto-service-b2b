import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BriefingProgress } from '../BriefingProgress';

// ---------------------------------------------------------------------------
// Suite
// ---------------------------------------------------------------------------

describe('BriefingProgress', () => {
  // -------------------------------------------------------------------------
  // Texto de progresso
  // -------------------------------------------------------------------------

  it('should display "Question X of Y" text', () => {
    render(<BriefingProgress current={2} total={5} />);

    // O componente usa spans separados, mas o texto final deve estar visível
    expect(screen.getByText(/question/i)).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('should display correct percentage text', () => {
    render(<BriefingProgress current={2} total={5} />);

    // 2/5 = 40%
    expect(screen.getByText('40%')).toBeInTheDocument();
  });

  it('should display 20% on first question of 5', () => {
    render(<BriefingProgress current={1} total={5} />);

    expect(screen.getByText('20%')).toBeInTheDocument();
  });

  it('should display 100% on last question', () => {
    render(<BriefingProgress current={5} total={5} />);

    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  it('should display 60% on question 3 of 5', () => {
    render(<BriefingProgress current={3} total={5} />);

    expect(screen.getByText('60%')).toBeInTheDocument();
  });

  it('should round percentage to nearest integer', () => {
    // 1/3 = 33.33% → Math.round → 33%
    render(<BriefingProgress current={1} total={3} />);

    expect(screen.getByText('33%')).toBeInTheDocument();
  });

  it('should display 0% when total is 0 (edge case)', () => {
    render(<BriefingProgress current={0} total={0} />);

    expect(screen.getByText('0%')).toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Progressbar role e aria attributes
  // -------------------------------------------------------------------------

  it('should have role="progressbar"', () => {
    render(<BriefingProgress current={2} total={5} />);

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('should have correct aria-valuenow', () => {
    render(<BriefingProgress current={2} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '40');
  });

  it('should have aria-valuemin=0', () => {
    render(<BriefingProgress current={1} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuemin', '0');
  });

  it('should have aria-valuemax=100', () => {
    render(<BriefingProgress current={1} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuemax', '100');
  });

  it('should have descriptive aria-label', () => {
    render(<BriefingProgress current={3} total={10} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-label', 'Question 3 of 10');
  });

  it('should have aria-valuenow=100 on last question', () => {
    render(<BriefingProgress current={5} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });

  it('should have aria-valuenow=20 on first question of 5', () => {
    render(<BriefingProgress current={1} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '20');
  });

  // -------------------------------------------------------------------------
  // Atualização ao re-renderizar
  // -------------------------------------------------------------------------

  it('should update percentage when props change', () => {
    const { rerender } = render(<BriefingProgress current={1} total={5} />);

    expect(screen.getByText('20%')).toBeInTheDocument();

    rerender(<BriefingProgress current={3} total={5} />);

    expect(screen.getByText('60%')).toBeInTheDocument();
    expect(screen.queryByText('20%')).not.toBeInTheDocument();
  });

  it('should update aria-valuenow when props change', () => {
    const { rerender } = render(<BriefingProgress current={1} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '20');

    rerender(<BriefingProgress current={4} total={5} />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '80');
  });
});
