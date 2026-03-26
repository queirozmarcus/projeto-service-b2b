import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Suite: Landing Page
// Valida: carregamento, CTAs, navegação por âncoras, links internos, footer.
// Não depende de backend — página é completamente estática.
//
// Nota: os textos abaixo refletem o conteúdo servido pelo servidor em execução.
// Ao atualizar a landing page, atualize também os seletores nesta suite.
// ---------------------------------------------------------------------------

test.describe('Landing Page', () => {
  // -------------------------------------------------------------------------
  // Teste 1 — Carregamento sem erros e headline principal visível
  // -------------------------------------------------------------------------
  test('should load without errors and display hero headline', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page.goto('/');

    // Sem erros de JS fatais no console
    expect(errors).toHaveLength(0);

    // Título do browser contém "ScopeFlow"
    await expect(page).toHaveTitle(/ScopeFlow/);

    // Headline principal da Hero visível (h1 ou h2 com qualquer versão do texto)
    await expect(
      page.getByRole('heading', { level: 2 }).first(),
    ).toBeVisible();

    // Navbar com logo ScopeFlow visível
    await expect(page.locator('nav').getByText('ScopeFlow')).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 2 — CTAs de navegação visíveis na Navbar
  // -------------------------------------------------------------------------
  test('should display Login and Get Started CTAs in Navbar', async ({ page }) => {
    await page.goto('/');

    // Navbar: botão de login — texto pode ser "Login" ou "Log in"
    const navbarLogin = page.locator('nav').getByRole('link', { name: /log.?in/i });
    await expect(navbarLogin).toBeVisible();
    await expect(navbarLogin).toHaveAttribute('href', '/auth/login');

    // Navbar: botão de cadastro — texto pode ser "Get Started" ou variação
    const navbarGetStarted = page.locator('nav').getByRole('link', { name: /get started/i });
    await expect(navbarGetStarted).toBeVisible();
    await expect(navbarGetStarted).toHaveAttribute('href', '/auth/register');
  });

  // -------------------------------------------------------------------------
  // Teste 3 — Seções da landing page renderizam (Features, CTA, Footer)
  // -------------------------------------------------------------------------
  test('should render feature highlights and footer', async ({ page }) => {
    await page.goto('/');

    // Pelo menos um botão/link de CTA que aponta para /auth/register
    const registerLinks = page.locator('a[href="/auth/register"]');
    await expect(registerLinks.first()).toBeVisible();
    expect(await registerLinks.count()).toBeGreaterThanOrEqual(1);

    // Features section — ao menos um dos textos presentes
    const featureTexts = ['AI-Powered Briefing', 'Instant Scope Generation', 'Approval Workflows'];
    let foundFeature = false;
    for (const text of featureTexts) {
      const count = await page.getByText(text).count();
      if (count > 0) {
        foundFeature = true;
        break;
      }
    }
    expect(foundFeature, 'At least one feature heading should be visible').toBe(true);

    // Footer presente
    const footer = page.locator('footer');
    await expect(footer).toBeVisible();
  });

  // -------------------------------------------------------------------------
  // Teste 4 — "Get Started" (Navbar) navega para /auth/register
  // -------------------------------------------------------------------------
  test('should navigate to /auth/register when clicking Get Started', async ({ page }) => {
    await page.goto('/');

    // Clica no CTA da navbar
    await page.locator('nav').getByRole('link', { name: /get started/i }).click();

    await expect(page).toHaveURL('/auth/register');

    // Formulário de cadastro está presente
    await expect(page.locator('#email')).toBeVisible();
  });
});
