import { test, expect } from '@playwright/test';
import {
  setupBriefingApiMocks,
  SESSION_IN_PROGRESS,
  SESSION_COMPLETED,
  STANDARD_FIVE_QUESTIONS,
} from './fixtures/briefing-fixtures';

// ---------------------------------------------------------------------------
// Tokens usados nos testes
// ---------------------------------------------------------------------------
const TOKEN_HAPPY = 'e2e-test-token-happy';
const TOKEN_NETWORK = 'e2e-test-token-network';
const TOKEN_COMPLETED = 'e2e-test-token-done';
const TOKEN_INVALID = 'invalid-token-xyz-not-found';

// ---------------------------------------------------------------------------
// Suite principal: Briefing Flow
// ---------------------------------------------------------------------------

test.describe('Briefing Flow', () => {
  // -------------------------------------------------------------------------
  // Teste 1 — Happy Path: Fluxo Completo (100% score)
  // -------------------------------------------------------------------------
  test('should complete briefing with all required questions answered', async ({ page }) => {
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
      completionScore: 100,
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);

    // Página carregou: header visível
    await expect(page.getByText('Project Briefing')).toBeVisible();

    // Q1: contador de progresso e percentual
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();
    await expect(page.getByText('20%')).toBeVisible();

    // Preenche Q1 e avança
    await page.locator('textarea, input[type="text"]').first().fill('Our goal is to expand market share in Brazil.');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q2
    await expect(page.getByText(/question 2 of 5/i)).toBeVisible();
    await page.locator('textarea, input[type="text"]').first().fill('Small business owners aged 30-50.');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q3
    await expect(page.getByText(/question 3 of 5/i)).toBeVisible();
    await page.locator('textarea, input[type="text"]').first().fill('R$ 5.000 - R$ 10.000');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q4
    await expect(page.getByText(/question 4 of 5/i)).toBeVisible();
    await page.locator('textarea, input[type="text"]').first().fill('3 months');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q5 (última — opcional)
    await expect(page.getByText(/question 5 of 5/i)).toBeVisible();
    await page.locator('textarea, input[type="text"]').first().fill('No additional comments.');

    // Botão Complete deve estar visível e habilitado (todas as required respondidas)
    const completeButton = page.getByRole('button', { name: /complete briefing/i });
    await expect(completeButton).toBeVisible();
    await expect(completeButton).toBeEnabled();

    await completeButton.click();

    // Tela de resultado
    await expect(page.getByRole('heading', { name: 'Briefing Completed!' })).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByText('100%')).toBeVisible();
    await expect(page.getByText(/successfully/i)).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 2 — Partial Completion (Required validation client-side)
  // Quando o usuário não responde as perguntas required, o botão Complete
  // fica desabilitado — comportamento client-side que previne score baixo.
  // -------------------------------------------------------------------------
  test('should disable Complete button when required questions are unanswered', async ({ page }) => {
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
      completionScore: 40,
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    // Responde apenas Q1 (required) e navega até a última sem responder Q2-Q4
    await page.locator('textarea, input[type="text"]').first().fill('Partial answer');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q2: pula sem responder
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q3: pula sem responder
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q4: pula sem responder
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q5 (última): Complete fica desabilitado pois Q2, Q3, Q4 (required) não respondidas
    await expect(page.getByText(/question 5 of 5/i)).toBeVisible();

    const completeButton = page.getByRole('button', { name: /complete briefing/i });
    await expect(completeButton).toBeDisabled();
    await expect(completeButton).toHaveAttribute(
      'title',
      'Answer all required questions to complete',
    );
  });

  // -------------------------------------------------------------------------
  // Teste 3 — Low Score via Backend: submete com respostas mas score < 80%
  // -------------------------------------------------------------------------
  test('should show low score message when backend returns score below 80%', async ({ page }) => {
    // Backend retorna score baixo mesmo com respostas submetidas
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
      completionScore: 60,
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    // Preenche todas as required para habilitar o botão
    await page.locator('textarea, input[type="text"]').first().fill('Q1 answer');
    await page.getByRole('button', { name: /go to next question/i }).click();

    await page.locator('textarea, input[type="text"]').first().fill('Q2 answer');
    await page.getByRole('button', { name: /go to next question/i }).click();

    await page.locator('textarea, input[type="text"]').first().fill('Q3 answer');
    await page.getByRole('button', { name: /go to next question/i }).click();

    await page.locator('textarea, input[type="text"]').first().fill('Q4 answer');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q5: Complete habilitado (todas as required respondidas)
    const completeButton = page.getByRole('button', { name: /complete briefing/i });
    await expect(completeButton).toBeEnabled();
    await completeButton.click();

    // Tela de resultado com score baixo
    await expect(page.getByRole('heading', { name: 'Briefing Completed!' })).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByText('60%')).toBeVisible();
    await expect(page.getByText(/low score/i)).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 4 — Invalid Token (404)
  // -------------------------------------------------------------------------
  test('should show error page for invalid or expired token', async ({ page }) => {
    // Mock: server-side retorna 404 para o token inválido
    await page.route(`**/public/briefings/${TOKEN_INVALID}`, (route) => {
      const url = route.request().url();
      if (!url.includes('/questions') && !url.includes('/batch-answers')) {
        route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ detail: 'Briefing session not found' }),
        });
      } else {
        route.continue();
      }
    });

    await page.goto(`/briefing/${TOKEN_INVALID}`);

    // Server Component renderiza InvalidTokenPage
    await expect(page.getByRole('heading', { name: /invalid or expired link/i })).toBeVisible();
    await expect(page.getByText(/please contact the service provider/i)).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 5 — Sessão Já Completada
  // -------------------------------------------------------------------------
  test('should show already-completed page when session status is COMPLETED', async ({ page }) => {
    // Mock: server-side retorna sessão com status COMPLETED
    await page.route(`**/public/briefings/${TOKEN_COMPLETED}`, (route) => {
      const url = route.request().url();
      if (!url.includes('/questions') && !url.includes('/batch-answers')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...SESSION_COMPLETED, publicToken: TOKEN_COMPLETED }),
        });
      } else {
        route.continue();
      }
    });

    await page.goto(`/briefing/${TOKEN_COMPLETED}`);

    await expect(page.getByRole('heading', { name: 'Briefing Already Completed' })).toBeVisible();
    await expect(page.getByText(/already been submitted/i)).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 6 — Navigation: Previous Desabilitado na Q1
  // -------------------------------------------------------------------------
  test('should disable Previous button on first question and enable on subsequent', async ({
    page,
  }) => {
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    const prevButton = page.getByRole('button', { name: /go to previous question/i });

    // Q1: Previous desabilitado
    await expect(prevButton).toBeDisabled();

    // Avança para Q2
    await page.locator('textarea, input[type="text"]').first().fill('Answer Q1');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q2: Previous habilitado
    await expect(page.getByText(/question 2 of 5/i)).toBeVisible();
    await expect(prevButton).toBeEnabled();

    // Clica Previous — volta para Q1
    await prevButton.click();
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    // Q1 novamente: Previous desabilitado
    await expect(prevButton).toBeDisabled();
  });

  // -------------------------------------------------------------------------
  // Teste 7 — Progress Bar: Percentual Correto
  // -------------------------------------------------------------------------
  test('should show correct progress bar percentage at each step', async ({ page }) => {
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    const progressbar = page.getByRole('progressbar');

    // Q1: 20% (1/5)
    await expect(progressbar).toHaveAttribute('aria-valuenow', '20');
    await expect(page.getByText('20%')).toBeVisible();

    // Avança para Q2
    await page.locator('textarea, input[type="text"]').first().fill('A');
    await page.getByRole('button', { name: /go to next question/i }).click();
    await expect(page.getByText(/question 2 of 5/i)).toBeVisible();

    // Q2: 40%
    await expect(progressbar).toHaveAttribute('aria-valuenow', '40');
    await expect(page.getByText('40%')).toBeVisible();

    // Avança para Q3
    await page.locator('textarea, input[type="text"]').first().fill('B');
    await page.getByRole('button', { name: /go to next question/i }).click();
    await expect(page.getByText(/question 3 of 5/i)).toBeVisible();

    // Q3: 60%
    await expect(progressbar).toHaveAttribute('aria-valuenow', '60');
    await expect(page.getByText('60%')).toBeVisible();

    // Avança para Q4
    await page.locator('textarea, input[type="text"]').first().fill('C');
    await page.getByRole('button', { name: /go to next question/i }).click();
    await expect(page.getByText(/question 4 of 5/i)).toBeVisible();

    // Q4: 80%
    await expect(progressbar).toHaveAttribute('aria-valuenow', '80');

    // Avança para Q5
    await page.locator('textarea, input[type="text"]').first().fill('D');
    await page.getByRole('button', { name: /go to next question/i }).click();
    await expect(page.getByText(/question 5 of 5/i)).toBeVisible();

    // Q5: 100%
    await expect(progressbar).toHaveAttribute('aria-valuenow', '100');
    await expect(page.getByText('100%')).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 8 — Required Field Validation (On Blur)
  // -------------------------------------------------------------------------
  test('should show validation error on blur when required field is empty', async ({ page }) => {
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    // Q1 é TEXTAREA e required: true
    const textarea = page.locator('textarea').first();
    await expect(textarea).toBeVisible();

    // Focar e fazer blur sem preencher
    await textarea.focus();
    await textarea.blur();

    // "This field is required." — com ponto final (QuestionCard.tsx)
    await expect(page.getByText('This field is required.')).toBeVisible();
    await expect(textarea).toHaveAttribute('aria-invalid', 'true');

    // Após preencher: erro desaparece
    await textarea.fill('Now I answered');
    await expect(page.getByText('This field is required.')).not.toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 9 — Network Error no fetch de questions: Try Again
  // -------------------------------------------------------------------------
  test('should show Try Again button when questions fetch fails with network error', async ({
    page,
    context,
  }) => {
    // Mock server-side: sessão válida
    await page.route(`**/public/briefings/${TOKEN_NETWORK}`, (route) => {
      const url = route.request().url();
      if (!url.includes('/questions') && !url.includes('/batch-answers')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...SESSION_IN_PROGRESS, publicToken: TOKEN_NETWORK }),
        });
      } else {
        route.continue();
      }
    });

    // Mock client-side: questions falhadas (network abort)
    await page.route(`**/public/briefings/${TOKEN_NETWORK}/questions`, (route) => {
      route.abort('failed');
    });

    await page.goto(`/briefing/${TOKEN_NETWORK}`);

    // ErrorBanner deve aparecer
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 15_000 });
    const tryAgainButton = page.getByRole('button', { name: /try again/i });
    await expect(tryAgainButton).toBeVisible();

    // Configura retry para ter sucesso via context.route (maior prioridade)
    await context.route(`**/public/briefings/${TOKEN_NETWORK}/questions`, (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(STANDARD_FIVE_QUESTIONS),
      });
    });

    // Mock /auth/refresh para o /complete autenticado
    await context.route('**/auth/refresh', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ accessToken: 'mock-token', expiresIn: 900 }),
      });
    });

    // Mock /complete
    await context.route(`**/briefing-sessions/${SESSION_IN_PROGRESS.id}/complete`, (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ completenessScore: 100, status: 'COMPLETED', message: 'Successfully completed.' }),
      });
    });

    await tryAgainButton.click();

    // Após retry: perguntas carregam
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible({ timeout: 15_000 });
  });

  // -------------------------------------------------------------------------
  // Teste 10 — Error durante Submit (batch-answers 500)
  // -------------------------------------------------------------------------
  test('should show error message when submit fails with server error', async ({ page }) => {
    // Perguntas carregam normalmente, mas batch-answers retorna 500
    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
      batchAnswersStatus: 500,
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    // Preenche todas as perguntas required para habilitar Complete
    await page.locator('textarea, input[type="text"]').first().fill('Q1');
    await page.getByRole('button', { name: /go to next question/i }).click();

    await page.locator('textarea, input[type="text"]').first().fill('Q2');
    await page.getByRole('button', { name: /go to next question/i }).click();

    await page.locator('textarea, input[type="text"]').first().fill('Q3');
    await page.getByRole('button', { name: /go to next question/i }).click();

    await page.locator('textarea, input[type="text"]').first().fill('Q4');
    await page.getByRole('button', { name: /go to next question/i }).click();

    // Q5: Complete habilitado
    const completeButton = page.getByRole('button', { name: /complete briefing/i });
    await expect(completeButton).toBeEnabled();
    await completeButton.click();

    // Erro inline no BriefingFlow (aparece quando questions já estão carregadas)
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 15_000 });
  });

  // -------------------------------------------------------------------------
  // Teste 11 — Mobile Responsiveness (375px)
  // -------------------------------------------------------------------------
  test('should be fully usable on mobile viewport (375px)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });

    await setupBriefingApiMocks(page, {
      token: TOKEN_HAPPY,
      session: { ...SESSION_IN_PROGRESS, publicToken: TOKEN_HAPPY },
    });

    await page.goto(`/briefing/${TOKEN_HAPPY}`);

    // Título principal visível
    await expect(page.getByText('Project Briefing')).toBeVisible();

    // Indicador de progresso visível
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible();

    // Campo de resposta acessível e utilizável
    const input = page.locator('textarea, input[type="text"]').first();
    await expect(input).toBeVisible();
    await input.fill('Mobile viewport answer');

    // Botão Next visível e clicável
    const nextButton = page.getByRole('button', { name: /go to next question/i });
    await expect(nextButton).toBeVisible();

    // Verifica que botão está dentro do viewport (não cortado)
    const box = await nextButton.boundingBox();
    expect(box).not.toBeNull();
    expect(box!.x + box!.width).toBeLessThanOrEqual(375 + 20); // margem de tolerância

    await nextButton.click();

    // Q2 carregou
    await expect(page.getByText(/question 2 of 5/i)).toBeVisible();

    // Progress bar acessível
    await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '40');
  });
});

// ---------------------------------------------------------------------------
// Suite: Loading Skeleton
// ---------------------------------------------------------------------------

test.describe('Briefing Loading State', () => {
  test('should show loading skeleton while questions are fetching', async ({ page }) => {
    const TOKEN_SLOW = 'e2e-test-token-slow';

    // Mock server-side: sessão válida
    await page.route(`**/public/briefings/${TOKEN_SLOW}`, (route) => {
      const url = route.request().url();
      if (!url.includes('/questions') && !url.includes('/batch-answers')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...SESSION_IN_PROGRESS, publicToken: TOKEN_SLOW }),
        });
      } else {
        route.continue();
      }
    });

    // Mock questions com delay para capturar o skeleton
    await page.route(`**/public/briefings/${TOKEN_SLOW}/questions`, async (route) => {
      await new Promise<void>((resolve) => setTimeout(resolve, 1_500));
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(STANDARD_FIVE_QUESTIONS),
      });
    });

    await page.goto(`/briefing/${TOKEN_SLOW}`);

    // Skeleton aparece durante o carregamento
    await expect(page.getByLabel('Loading briefing questions')).toBeVisible({ timeout: 5_000 });

    // Após carregar: skeleton some e primeira pergunta aparece
    await expect(page.getByText(/question 1 of 5/i)).toBeVisible({ timeout: 15_000 });
  });
});
