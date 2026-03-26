import { test, expect, type Page, type BrowserContext } from '@playwright/test';

// ---------------------------------------------------------------------------
// Suite: Responsive Layout
// Valida que Landing Page e Dashboard renderizam corretamente em mobile e
// tablet sem elementos cortados ou sobrepostos.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Helper: sessão autenticada para acessar /dashboard
// ---------------------------------------------------------------------------
async function setupAuthenticatedSession(context: BrowserContext, page: Page) {
  await context.addCookies([
    {
      name: 'refreshToken',
      value: 'valid-refresh-token-responsive-e2e',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);

  await page.route('**/auth/refresh', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ accessToken: 'mock-access-token-responsive', expiresIn: 900 }),
    }),
  );

  await page.route('**/auth/me', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'user-responsive-001',
        fullName: 'Responsive User',
        email: 'responsive@scopeflow.dev',
        workspaceId: 'ws-responsive-001',
      }),
    }),
  );
}

/**
 * Aguarda o dashboard autenticar via SessionProvider antes de validar conteúdo.
 */
async function waitForDashboardReady(page: Page) {
  await expect(page.getByText('Redirecionando...')).not.toBeVisible({ timeout: 15_000 });
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 15_000 });
}

// ---------------------------------------------------------------------------
// Testes
// ---------------------------------------------------------------------------

test.describe('Responsive Layout', () => {
  // -------------------------------------------------------------------------
  // Teste 1 — Landing Page em mobile (375px / iPhone SE)
  // -------------------------------------------------------------------------
  test('landing page should be usable on mobile (375px)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');

    // Navbar logo visível em mobile
    await expect(page.locator('nav').getByText('ScopeFlow')).toBeVisible();

    // Pelo menos um link/botão de CTA aponta para /auth/register
    const registerLink = page.locator('a[href="/auth/register"]').first();
    await expect(registerLink).toBeVisible();

    // CTA não ultrapassa a largura do viewport
    const ctaBBox = await registerLink.boundingBox();
    expect(ctaBBox).not.toBeNull();
    expect(ctaBBox!.x + ctaBBox!.width).toBeLessThanOrEqual(375 + 16);

    // Footer visível em mobile
    await expect(page.locator('footer')).toBeVisible();

    // Sem scroll horizontal
    const bodyScrollWidth = await page.evaluate(() => document.body.scrollWidth);
    expect(bodyScrollWidth).toBeLessThanOrEqual(375 + 16);
  });

  // -------------------------------------------------------------------------
  // Teste 2 — Landing Page em tablet (768px / iPad)
  // -------------------------------------------------------------------------
  test('landing page should be usable on tablet (768px)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/');

    // Navbar logo e CTAs de auth visíveis
    await expect(page.locator('nav').getByText('ScopeFlow')).toBeVisible();
    await expect(page.locator('nav').getByRole('link', { name: /get started/i })).toBeVisible();
    await expect(page.locator('nav').getByRole('link', { name: /log.?in/i })).toBeVisible();

    // Feature section presente (ao menos um card)
    const featureTexts = [
      'AI-Powered Briefing',
      'Instant Scope Generation',
      'Approval Workflows',
    ];
    let featureFound = false;
    for (const text of featureTexts) {
      if ((await page.getByText(text).count()) > 0) {
        featureFound = true;
        break;
      }
    }
    expect(featureFound, 'At least one feature card should be visible on tablet').toBe(true);

    // Sem overflow horizontal
    const bodyScrollWidth = await page.evaluate(() => document.body.scrollWidth);
    expect(bodyScrollWidth).toBeLessThanOrEqual(768 + 16);
  });

  // -------------------------------------------------------------------------
  // Teste 3 — Dashboard em mobile (375px)
  // -------------------------------------------------------------------------
  test('dashboard should render correctly on mobile (375px)', async ({ page, context }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await setupAuthenticatedSession(context, page);
    await page.goto('/dashboard');
    await waitForDashboardReady(page);

    // Título principal visível
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

    // Seções principais visíveis em mobile
    await expect(page.getByRole('heading', { name: 'Propostas' })).toBeVisible();

    // Botão Sair visível e dentro do viewport
    const sairBtn = page.getByRole('button', { name: 'Sair' });
    await expect(sairBtn).toBeVisible();
    const bbox = await sairBtn.boundingBox();
    expect(bbox).not.toBeNull();
    expect(bbox!.x + bbox!.width).toBeLessThanOrEqual(375 + 16);

    // Sem overflow horizontal
    const bodyScrollWidth = await page.evaluate(() => document.body.scrollWidth);
    expect(bodyScrollWidth).toBeLessThanOrEqual(375 + 16);
  });
});
