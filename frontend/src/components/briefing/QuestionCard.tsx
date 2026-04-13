'use client';

import { useState } from 'react';
import type { Question } from '@/types/briefing';

interface QuestionCardProps {
  question: Question;
  answer: string;
  onAnswerChange: (text: string) => void;
  isLoading: boolean;
}

/**
 * Renderiza uma pergunta do briefing com o campo de resposta adequado.
 *
 * Suporta os tipos conhecidos: TEXT (input), TEXTAREA (textarea).
 * Tipos desconhecidos fazem fallback para TEXT — alinhado com a definição
 * de QuestionType em types/briefing.ts.
 *
 * Validação inline: mostra erro apenas após o usuário interagir com o campo
 * (dirty check via estado `touched`), evitando erros prematuros.
 */
export function QuestionCard({
  question,
  answer,
  onAnswerChange,
  isLoading,
}: QuestionCardProps) {
  const [touched, setTouched] = useState(false);

  const showError = touched && question.required && !answer.trim();

  const inputBaseClass = `
    w-full rounded-lg border px-3 py-2.5 text-sm text-secondary-900
    placeholder:text-secondary-400
    focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500
    disabled:cursor-not-allowed disabled:bg-secondary-50 disabled:opacity-60
    transition-colors duration-150
    ${showError ? 'border-red-400 bg-red-50' : 'border-secondary-300 bg-white'}
  `.trim();

  const handleBlur = () => setTouched(true);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    onAnswerChange(e.target.value);
  };

  const inputId = `question-${question.questionId}`;

  return (
    <div className="space-y-3 my-8">
      <label htmlFor={inputId} className="block">
        <span className="text-base font-semibold text-secondary-800 leading-snug">
          {question.questionText}
        </span>
        {question.required && (
          <span
            aria-hidden="true"
            className="ml-1 text-red-500 font-bold"
            title="Required"
          >
            *
          </span>
        )}
      </label>

      {/* TEXTAREA para respostas longas */}
      {question.type === 'TEXTAREA' ? (
        <textarea
          id={inputId}
          value={answer}
          onChange={handleChange}
          onBlur={handleBlur}
          placeholder="Your answer..."
          disabled={isLoading}
          rows={5}
          aria-required={question.required}
          aria-invalid={showError}
          aria-describedby={showError ? `${inputId}-error` : undefined}
          className={`${inputBaseClass} resize-y min-h-[120px]`}
        />
      ) : (
        // TEXT (default para tipos desconhecidos)
        <input
          id={inputId}
          type="text"
          value={answer}
          onChange={handleChange}
          onBlur={handleBlur}
          placeholder="Your answer..."
          disabled={isLoading}
          aria-required={question.required}
          aria-invalid={showError}
          aria-describedby={showError ? `${inputId}-error` : undefined}
          className={inputBaseClass}
        />
      )}

      {showError && (
        <p
          id={`${inputId}-error`}
          role="alert"
          className="text-xs text-red-600"
        >
          This field is required.
        </p>
      )}
    </div>
  );
}
