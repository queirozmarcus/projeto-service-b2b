import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QuestionCard } from '../QuestionCard';
import type { Question } from '@/types/briefing';

// ---------------------------------------------------------------------------
// Fixtures
// ---------------------------------------------------------------------------

const requiredTextareaQuestion: Question = {
  questionId: 'q-textarea-001',
  questionText: 'What is your main project goal?',
  type: 'TEXTAREA',
  orderIndex: 0,
  required: true,
};

const requiredTextQuestion: Question = {
  questionId: 'q-text-001',
  questionText: 'What is your budget range?',
  type: 'TEXT',
  orderIndex: 1,
  required: true,
};

const optionalQuestion: Question = {
  questionId: 'q-optional-001',
  questionText: 'Any additional comments?',
  type: 'TEXTAREA',
  orderIndex: 4,
  required: false,
};

const unknownTypeQuestion: Question = {
  questionId: 'q-unknown-001',
  questionText: 'Rate your experience',
  type: 'RATING', // tipo desconhecido — deve fazer fallback para input TEXT
  orderIndex: 3,
  required: false,
};

// ---------------------------------------------------------------------------
// Suite
// ---------------------------------------------------------------------------

describe('QuestionCard', () => {
  // -------------------------------------------------------------------------
  // Render básico
  // -------------------------------------------------------------------------

  it('should render question text and textarea for TEXTAREA type', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.getByText('What is your main project goal?')).toBeInTheDocument();
    const field = screen.getByRole('textbox');
    expect(field).toBeInTheDocument();
    expect(field.tagName.toLowerCase()).toBe('textarea');
  });

  it('should render input[type="text"] for TEXT type', () => {
    render(
      <QuestionCard
        question={requiredTextQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const field = screen.getByRole('textbox');
    expect(field.tagName.toLowerCase()).toBe('input');
    expect(field).toHaveAttribute('type', 'text');
  });

  it('should fall back to input[type="text"] for unknown question type', () => {
    render(
      <QuestionCard
        question={unknownTypeQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const field = screen.getByRole('textbox');
    expect(field.tagName.toLowerCase()).toBe('input');
  });

  // -------------------------------------------------------------------------
  // Asterisco de required
  // -------------------------------------------------------------------------

  it('should show asterisk (*) for required questions', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.getByText('*')).toBeInTheDocument();
  });

  it('should NOT show asterisk for optional questions', () => {
    render(
      <QuestionCard
        question={optionalQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.queryByText('*')).not.toBeInTheDocument();
  });

  // -------------------------------------------------------------------------
  // Interação: digitação
  // -------------------------------------------------------------------------

  it('should call onAnswerChange when user types', () => {
    const handleChange = vi.fn();

    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={handleChange}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'Test answer' } });

    expect(handleChange).toHaveBeenCalledWith('Test answer');
  });

  it('should reflect the answer prop as the field value', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer="Pre-filled answer"
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.getByRole('textbox')).toHaveValue('Pre-filled answer');
  });

  it('should call onAnswerChange with empty string when field is cleared', () => {
    const handleChange = vi.fn();

    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer="existing answer"
        onAnswerChange={handleChange}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: '' } });

    expect(handleChange).toHaveBeenCalledWith('');
  });

  // -------------------------------------------------------------------------
  // Validação: required + blur
  // -------------------------------------------------------------------------

  it('should NOT show validation error before user interacts', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.queryByText('This field is required.')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('should show validation error after blur on empty required field', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.blur(textarea);

    // Mensagem com ponto final — ver QuestionCard.tsx linha 107
    expect(screen.getByText('This field is required.')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('should mark field as aria-invalid=true after blur on empty required field', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.blur(textarea);

    expect(textarea).toHaveAttribute('aria-invalid', 'true');
  });

  it('should NOT show validation error for optional empty field after blur', () => {
    render(
      <QuestionCard
        question={optionalQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.blur(textarea);

    expect(screen.queryByText('This field is required.')).not.toBeInTheDocument();
  });

  it('should show error when answer is only whitespace (trim check)', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer="   " // whitespace — falha no .trim()
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.blur(textarea);

    expect(screen.getByText('This field is required.')).toBeInTheDocument();
  });

  it('should associate error with field via aria-describedby', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const textarea = screen.getByRole('textbox');
    fireEvent.blur(textarea);

    const expectedId = `question-${requiredTextareaQuestion.questionId}-error`;
    expect(textarea).toHaveAttribute('aria-describedby', expectedId);
    expect(screen.getByRole('alert')).toHaveAttribute('id', expectedId);
  });

  // -------------------------------------------------------------------------
  // Loading state
  // -------------------------------------------------------------------------

  it('should disable the input when isLoading is true', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={true}
      />,
    );

    expect(screen.getByRole('textbox')).toBeDisabled();
  });

  it('should enable the input when isLoading is false', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.getByRole('textbox')).not.toBeDisabled();
  });

  // -------------------------------------------------------------------------
  // Acessibilidade
  // -------------------------------------------------------------------------

  it('should have a label associated with the input via htmlFor', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    const expectedId = `question-${requiredTextareaQuestion.questionId}`;
    expect(screen.getByRole('textbox')).toHaveAttribute('id', expectedId);
    expect(
      screen.getByText('What is your main project goal?').closest('label'),
    ).toHaveAttribute('for', expectedId);
  });

  it('should set aria-required=true for required questions', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.getByRole('textbox')).toHaveAttribute('aria-required', 'true');
  });

  it('should set placeholder "Your answer..."', () => {
    render(
      <QuestionCard
        question={requiredTextareaQuestion}
        answer=""
        onAnswerChange={vi.fn()}
        isLoading={false}
      />,
    );

    expect(screen.getByPlaceholderText('Your answer...')).toBeInTheDocument();
  });
});
