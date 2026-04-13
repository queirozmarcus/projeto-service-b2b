/**
 * Fixtures compartilhadas para os testes E2E de briefing.
 *
 * Centraliza os shapes de dados que os mocks de API devem retornar.
 * Alinhados com os DTOs do backend: BriefingQuestionResponse,
 * BriefingSessionResponse, BriefingCompletionResponse.
 */

import type { BrowserContext, Page } from '@playwright/test';

// ---------------------------------------------------------------------------
// Shapes de dados (espelham os tipos do backend)
// ---------------------------------------------------------------------------

export interface MockQuestion {
  questionId: string;
  questionText: string;
  type: 'TEXT' | 'TEXTAREA' | 'SELECT' | string;
  orderIndex: number;
  required: boolean;
}

export interface MockSession {
  id: string;
  proposalId: string | null;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  publicToken: string;
  completenessScore: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface MockCompletionResult {
  completenessScore: number;
  status: 'COMPLETED';
  message: string;
}

// ---------------------------------------------------------------------------
// Conjuntos de perguntas de teste
// ---------------------------------------------------------------------------

/**
 * 5 perguntas padrão: 4 required + 1 optional.
 * Cobre os cenários de happy path (100% score possível)
 * e navegação completa do stepper.
 */
export const STANDARD_FIVE_QUESTIONS: MockQuestion[] = [
  {
    questionId: 'q-std-001',
    questionText: 'What is your main business goal for this project?',
    type: 'TEXTAREA',
    orderIndex: 0,
    required: true,
  },
  {
    questionId: 'q-std-002',
    questionText: 'Who is your target audience?',
    type: 'TEXTAREA',
    orderIndex: 1,
    required: true,
  },
  {
    questionId: 'q-std-003',
    questionText: 'What is your estimated budget range?',
    type: 'TEXT',
    orderIndex: 2,
    required: true,
  },
  {
    questionId: 'q-std-004',
    questionText: 'What is your expected delivery timeline?',
    type: 'TEXT',
    orderIndex: 3,
    required: true,
  },
  {
    questionId: 'q-std-005',
    questionText: 'Any additional comments or special requirements?',
    type: 'TEXTAREA',
    orderIndex: 4,
    required: false,
  },
];

/**
 * Sessão de briefing em progresso (estado inicial típico).
 */
export const SESSION_IN_PROGRESS: MockSession = {
  id: 'session-e2e-uuid-001',
  proposalId: 'proposal-e2e-uuid-001',
  status: 'IN_PROGRESS',
  publicToken: 'e2e-test-token-happy',
  completenessScore: null,
  createdAt: '2025-01-15T10:00:00Z',
  updatedAt: '2025-01-15T10:00:00Z',
};

/**
 * Sessão já concluída (para testar o guard de "already completed").
 */
export const SESSION_COMPLETED: MockSession = {
  ...SESSION_IN_PROGRESS,
  id: 'session-e2e-uuid-completed',
  status: 'COMPLETED',
  publicToken: 'e2e-test-token-done',
  completenessScore: 100,
};

// ---------------------------------------------------------------------------
// Mensagens de completion (espelham a lógica do backend)
// ---------------------------------------------------------------------------

export function buildCompletionMessage(score: number): string {
  if (score >= 80) {
    return `Briefing completed successfully! Your answers provide a solid foundation for the proposal. (Score: ${score}%)`;
  }
  return `Briefing completed with low score (${score}%). Some required questions were left unanswered. Consider filling them in.`;
}

export function buildCompletionResult(score: number): MockCompletionResult {
  return {
    completenessScore: score,
    status: 'COMPLETED',
    message: buildCompletionMessage(score),
  };
}

// ---------------------------------------------------------------------------
// Helper: injeta cookie de autenticação (para testar dashboard link)
// ---------------------------------------------------------------------------

export async function injectAuthCookie(context: BrowserContext): Promise<void> {
  await context.addCookies([
    {
      name: 'refreshToken',
      value: 'e2e-mock-refresh-token',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);
}

// ---------------------------------------------------------------------------
// Helper: configura todos os mocks de API para o fluxo de briefing
// ---------------------------------------------------------------------------

export interface BriefingMockOptions {
  token?: string;
  session?: MockSession;
  questions?: MockQuestion[];
  completionScore?: number;
  /** Status HTTP para POST /batch-answers. Padrão: 204 */
  batchAnswersStatus?: number;
  /** Status HTTP para POST /complete. Padrão: 200 */
  completionStatus?: number;
  /** Se true, simula timeout na requisição de questions */
  questionsNetworkError?: boolean;
}

export async function setupBriefingApiMocks(
  page: Page,
  options: BriefingMockOptions = {},
): Promise<void> {
  const {
    token = 'e2e-test-token-happy',
    session = SESSION_IN_PROGRESS,
    questions = STANDARD_FIVE_QUESTIONS,
    completionScore = 100,
    batchAnswersStatus = 204,
    completionStatus = 200,
    questionsNetworkError = false,
  } = options;

  // ----------------------------------------------------------
  // 1. Server-side fetch: GET /public/briefings/{token}
  //    Chamado pelo Next.js Server Component (fetchSessionByToken)
  // ----------------------------------------------------------
  await page.route(`**/public/briefings/${token}`, (route) => {
    const url = route.request().url();
    // Evita interceptar /questions (que cai neste padrão também)
    if (url.includes('/questions') || url.includes('/batch-answers')) {
      route.continue();
      return;
    }

    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...session, publicToken: token }),
    });
  });

  // ----------------------------------------------------------
  // 2. Client-side fetch: GET /public/briefings/{token}/questions
  //    Chamado pelo axios (briefingApi.getQuestions)
  // ----------------------------------------------------------
  await page.route(`**/public/briefings/${token}/questions`, (route) => {
    if (questionsNetworkError) {
      route.abort('failed');
      return;
    }

    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(questions),
    });
  });

  // ----------------------------------------------------------
  // 3. POST /public/briefings/{token}/batch-answers
  // ----------------------------------------------------------
  await page.route(`**/public/briefings/${token}/batch-answers`, (route) => {
    if (batchAnswersStatus >= 400) {
      route.fulfill({
        status: batchAnswersStatus,
        contentType: 'application/json',
        body: JSON.stringify({
          type: 'https://scopeflow.com/problems/server-error',
          title: 'Internal Server Error',
          status: batchAnswersStatus,
          detail: 'Simulated server error for E2E testing.',
        }),
      });
    } else {
      route.fulfill({ status: 204 });
    }
  });

  // ----------------------------------------------------------
  // 4. POST /api/v1/auth/refresh
  //    Necessário para o interceptor axios ao chamar /complete
  // ----------------------------------------------------------
  await page.route('**/auth/refresh', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        accessToken: 'e2e-mock-access-token',
        expiresIn: 900,
      }),
    });
  });

  // ----------------------------------------------------------
  // 5. POST /api/v1/briefing-sessions/{id}/complete
  // ----------------------------------------------------------
  await page.route(`**/briefing-sessions/${session.id}/complete`, (route) => {
    if (completionStatus >= 400) {
      route.fulfill({
        status: completionStatus,
        contentType: 'application/json',
        body: JSON.stringify({
          type: 'https://scopeflow.com/problems/server-error',
          title: 'Internal Server Error',
          status: completionStatus,
          detail: 'Simulated server error during completion.',
        }),
      });
    } else {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(buildCompletionResult(completionScore)),
      });
    }
  });
}
