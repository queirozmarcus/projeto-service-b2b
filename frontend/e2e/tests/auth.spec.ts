import { test, expect, type Page, type BrowserContext } from '@playwright/test';

// ---------------------------------------------------------------------------
// Credenciais de teste — substitua por variáveis de ambiente em CI
// ---------------------------------------------------------------------------
const TEST_EMAIL = process.env.E2E_TEST_EMAIL ?? 'e2e@scopeflow.dev';
const TEST_PASSWORD = process.env.E2E_TEST_PASSWORD ?? 'SenhaE2E@123';
const TEST_FULL_NAME = process.env.E2E_TEST_FULL_NAME ?? 'E2E User';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Injeta cookie refreshToken para simular sessão ativa sem depender do backend.
 * Usado em testes de rota protegida e redirect de usuário já autenticado.
 */
async function injectAuthSession(context: BrowserContext, page: Page) {
  // Cookie que o middleware verifica para conceder acesso a rotas protegidas
  await context.addCookies([
    {
      name: 'refreshToken',
      value: 'valid-refresh-token-e2e',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);

  // Mock do endpoint /auth/refresh (chamado pelo SessionProvider no cold start)
  await page.route('**/auth/refresh', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ accessToken: 'mock-access-token', expiresIn: 900 }),
    }),
  );

  // Mock do /auth/me (chamado para popular o store com dados do usuário)
  await page.route('**/auth/me', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'user-e2e-001',
        fullName: TEST_FULL_NAME,
        email: TEST_EMAIL,
        workspaceId: 'ws-e2e-001',
      }),
    }),
  );
}

// ---------------------------------------------------------------------------
// Suite: Authentication Flow
// ---------------------------------------------------------------------------

test.describe('Authentication', () => {
  // -------------------------------------------------------------------------
  // Teste 1 — Página de login renderiza corretamente
  // -------------------------------------------------------------------------
  test('should render login page with all form fields', async ({ page }) => {
    await page.goto('/auth/login');

    // Título do AuthCard
    await expect(page.getByText('Entrar na sua conta')).toBeVisible();

    // Campo de email
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('label[for="email"]')).toHaveText('Email');

    // Campo de senha
    await expect(page.locator('#password')).toBeVisible();

    // Botão de submit
    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();

    // Link para cadastro
    await expect(page.getByRole('link', { name: 'Crie uma aqui' })).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 2 — Página de cadastro renderiza corretamente
  // -------------------------------------------------------------------------
  test('should render register page with all form fields', async ({ page }) => {
    await page.goto('/auth/register');

    // Título do AuthCard
    await expect(page.getByText('Crie sua conta')).toBeVisible();

    // Todos os campos obrigatórios
    await expect(page.locator('#workspaceName')).toBeVisible();
    await expect(page.locator('#fullName')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.locator('#confirmPassword')).toBeVisible();

    // Botão de submit
    await expect(page.getByRole('button', { name: 'Criar Conta' })).toBeVisible();

    // Link para login
    await expect(page.getByRole('link', { name: 'Faça login' })).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 3 — Erro de validação client-side: email inválido no login
  // -------------------------------------------------------------------------
  test('should show client-side validation error for invalid email on login', async ({ page }) => {
    await page.goto('/auth/login');

    // Preenche email inválido e tenta submeter
    await page.locator('#email').fill('not-a-valid-email');
    await page.locator('#password').fill('qualquercoisa');
    await page.getByRole('button', { name: 'Entrar' }).click();

    // react-hook-form + zod dispara validação antes de chamar o backend
    const emailError = page.locator('[role="alert"]').first();
    await expect(emailError).toBeVisible();

    // Permanece em /auth/login
    await expect(page).toHaveURL('/auth/login');
  });

  // -------------------------------------------------------------------------
  // Teste 4 — Validação de senhas não conferem no cadastro
  // -------------------------------------------------------------------------
  test('should show error when passwords do not match on register', async ({ page }) => {
    await page.goto('/auth/register');

    await page.locator('#workspaceName').fill('Workspace Teste');
    await page.locator('#fullName').fill('Usuario Teste');
    await page.locator('#email').fill('teste@example.com');
    await page.locator('#password').fill('SenhaForte@123');
    await page.locator('#confirmPassword').fill('SenhaDiferente@456');

    await page.getByRole('button', { name: 'Criar Conta' }).click();

    // Zod refine: "Senhas não conferem"
    await expect(page.getByText('Senhas não conferem')).toBeVisible();

    // Permanece em /auth/register
    await expect(page).toHaveURL('/auth/register');
  });

  // -------------------------------------------------------------------------
  // Teste 5 — Rota protegida: /dashboard sem auth → /auth/login
  // -------------------------------------------------------------------------
  test('should redirect unauthenticated user from /dashboard to /auth/login', async ({
    page,
    context,
  }) => {
    // Garante ausência total de cookies de sessão
    await context.clearCookies();

    await page.goto('/dashboard');

    // Middleware Next.js detecta ausência do cookie refreshToken e redireciona
    await expect(page).toHaveURL('/auth/login');

    // Tela de login renderizou
    await expect(page.getByText('Entrar na sua conta')).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 6 — Usuário logado não deve ver /auth/login (redirect para /dashboard)
  // -------------------------------------------------------------------------
  test('should redirect authenticated user from /auth/login to /dashboard', async ({
    page,
    context,
  }) => {
    await injectAuthSession(context, page);

    // Acessa /auth/login com sessão ativa
    await page.goto('/auth/login');

    // Middleware redireciona para /dashboard
    await expect(page).toHaveURL('/dashboard');
  });

  // -------------------------------------------------------------------------
  // Teste 7 — Logout via Navbar redireciona para /auth/login
  // -------------------------------------------------------------------------
  test('should logout and redirect to /auth/login when clicking Sair', async ({
    page,
    context,
  }) => {
    await injectAuthSession(context, page);

    // Acessa dashboard (sessão ativa)
    await page.goto('/dashboard');
    await expect(page).toHaveURL('/dashboard');

    // Clica no botão de logout na Navbar (texto "Sair")
    await page.getByRole('button', { name: 'Sair' }).click();

    // Após logout: redireciona para /auth/login
    await expect(page).toHaveURL('/auth/login');
  });
});
