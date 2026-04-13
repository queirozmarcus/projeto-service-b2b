import { test, expect, type Page, type BrowserContext } from '@playwright/test';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Injeta cookie de sessão e mocks de API para acessar /dashboard sem backend.
 *
 * Fluxo crítico:
 * 1. Cookie `refreshToken` → middleware Next.js permite acesso a /dashboard
 * 2. Mock POST /auth/refresh → SessionProvider obtém accessToken
 * 3. Mock GET /auth/me → SessionProvider popula o Zustand store com user
 * 4. DashboardLayout detecta isAuthenticated=true → renderiza conteúdo
 *
 * Atenção: os mocks devem ser registrados ANTES de page.goto() para
 * interceptar as chamadas do SessionProvider desde a inicialização.
 */
async function setupAuthenticatedSession(
  context: BrowserContext,
  page: Page,
  overrides: { fullName?: string; email?: string } = {},
) {
  const fullName = overrides.fullName ?? 'E2E Dashboard User';
  const email = overrides.email ?? 'dashboard@scopeflow.dev';

  // Cookie que o middleware Next.js verifica para conceder acesso
  await context.addCookies([
    {
      name: 'refreshToken',
      value: 'valid-refresh-token-dashboard-e2e',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);

  // Mock: POST /auth/refresh — SessionProvider troca cookie por accessToken
  await page.route('**/auth/refresh', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ accessToken: 'mock-access-token-dashboard', expiresIn: 900 }),
    }),
  );

  // Mock: GET /auth/me — popula o store com dados do usuário
  await page.route('**/auth/me', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'user-dashboard-001',
        fullName,
        email,
        workspaceId: 'ws-dashboard-001',
      }),
    }),
  );
}

/**
 * Aguarda o DashboardLayout completar a autenticação client-side.
 * O guard exibe "Redirecionando..." enquanto o SessionProvider não termina.
 */
async function waitForDashboardReady(page: Page) {
  // "Redirecionando..." some quando isAuthenticated=true no store
  await expect(page.getByText('Redirecionando...')).not.toBeVisible({ timeout: 15_000 });
  // Título "Dashboard" confirma que o conteúdo foi renderizado
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible({ timeout: 15_000 });
}

// ---------------------------------------------------------------------------
// Suite: Dashboard Flow
// ---------------------------------------------------------------------------

test.describe('Dashboard', () => {
  // -------------------------------------------------------------------------
  // Teste 1 — Dashboard carrega com heading e seções principais
  // -------------------------------------------------------------------------
  test('should render Dashboard heading and main sections', async ({
    page,
    context,
  }) => {
    await setupAuthenticatedSession(context, page);
    await page.goto('/dashboard');
    await waitForDashboardReady(page);

    // Heading principal
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

    // Seções principais do dashboard (refletem o conteúdo do servidor atual)
    await expect(page.getByRole('heading', { name: 'Propostas' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Briefings' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Aprovações' })).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 2 — Navbar do dashboard: logo, links de navegação e botão Sair
  // -------------------------------------------------------------------------
  test('should display DashboardNavbar with logo and logout button', async ({
    page,
    context,
  }) => {
    await setupAuthenticatedSession(context, page);
    await page.goto('/dashboard');

    // Botão Sair aparece imediatamente na Navbar (não aguarda guard do layout)
    await expect(page.getByRole('button', { name: 'Sair' })).toBeVisible({ timeout: 15_000 });

    // Logo ScopeFlow visível na Navbar
    await expect(page.locator('nav').getByRole('link', { name: 'ScopeFlow' })).toBeVisible();

    // Links de navegação
    await expect(page.locator('nav').getByRole('link', { name: 'Dashboard' })).toBeVisible();
    await expect(page.locator('nav').getByRole('link', { name: 'Propostas' })).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 3 — Nome e email do usuário exibidos na Navbar após hydration
  // -------------------------------------------------------------------------
  test('should display user name and email in Navbar after session hydration', async ({
    page,
    context,
  }) => {
    const fullName = 'Usuario Navbar E2E';
    const email = 'navbar-e2e@scopeflow.dev';
    await setupAuthenticatedSession(context, page, { fullName, email });
    await page.goto('/dashboard');

    // Aguarda o SessionProvider popular o store (nome aparece na Navbar)
    await expect(page.locator('nav').getByText(fullName)).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('nav').getByText(email)).toBeVisible({ timeout: 15_000 });
  });

  // -------------------------------------------------------------------------
  // Teste 4 — Estado vazio: mensagens de "Nenhum" para cada seção
  // -------------------------------------------------------------------------
  test('should show empty state messages when no data exists', async ({
    page,
    context,
  }) => {
    await setupAuthenticatedSession(context, page);
    await page.goto('/dashboard');
    await waitForDashboardReady(page);

    // Estado vazio — mensagens padrão do servidor atual
    await expect(page.getByText('Nenhuma proposta criada ainda.')).toBeVisible();
    await expect(page.getByText('Nenhum briefing em andamento.')).toBeVisible();
    await expect(page.getByText('Nenhuma aprovação pendente.')).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 5 — Boas-vindas com nome do usuário no corpo da página
  // -------------------------------------------------------------------------
  test('should display welcome message with user name', async ({
    page,
    context,
  }) => {
    const fullName = 'Usuário Boas Vindas';
    await setupAuthenticatedSession(context, page, { fullName });
    await page.goto('/dashboard');
    await waitForDashboardReady(page);

    // Mensagem de boas-vindas personalizada
    await expect(page.getByText(`Bem-vindo, ${fullName}!`)).toBeVisible();
  });
});
