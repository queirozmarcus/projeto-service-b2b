# ScopeFlow Frontend - Gemini Instructions

Este arquivo fornece as diretrizes operacionais e o contexto técnico específico para o desenvolvimento do frontend do **ScopeFlow AI**.

## 🚀 Visão Geral do Projeto

O frontend do ScopeFlow é uma aplicação **Next.js 15 (App Router)** construída com **React 19**, **TypeScript** e **Tailwind CSS 4**. Ele serve como a interface principal para freelancers e agências gerenciarem briefings, propostas e alinhamento de escopo.

### Core Tech Stack
- **Framework:** Next.js 15+ (App Router, Server & Client Components).
- **Linguagem:** TypeScript (Strict Mode).
- **Estilo:** Tailwind CSS 4 + Radix UI (Primitivos acessíveis).
- **Estado Global:** Zustand (Stores leves e performáticas).
- **Formulários:** React Hook Form + Zod (Validação type-safe).
- **Comunicação:** Axios (com interceptores para Auth/Refresh).
- **Autenticação:** JWT (Stateless) com `refreshToken` via HttpOnly cookie.

---

## 🏗️ Arquitetura e Estrutura de Pastas

```
src/
├── app/                  # Routes, Layouts e Server Components
│   ├── auth/             # Login, Register, Password Recovery
│   ├── dashboard/        # Área logada principal
│   ├── briefing/         # Fluxo de descoberta/briefing
│   └── page.tsx          # Landing Page (SSG)
├── components/           # Componentes React reutilizáveis
│   ├── ui/               # Componentes base (Botões, Inputs, Modais)
│   ├── dashboard/        # Componentes específicos do dashboard
│   ├── landing/          # Componentes da Landing Page
│   └── auth/             # SessionProvider e guards de autenticação
├── hooks/                # Hooks customizados (useAuth, useBriefing, etc.)
├── lib/                  # Utilitários, Instância Axios (api.ts), JWT, Broadcast
├── stores/               # Zustand Stores (useSession, useDashboard, useBriefing)
├── styles/               # CSS Global e configurações de fontes
└── types/                # Definições de tipos TypeScript
```

### Convenções de Componentes
- **Localização:** Componentes de domínio ficam em `src/components/[domain]`. Componentes genéricos em `src/components/ui`.
- **Exportação:** Use barrel exports (`index.ts`) em cada pasta de componentes para facilitar o consumo.
- **State:** Prefira componentes controlados para formulários. Use Zustand apenas para estado que realmente precisa ser global (Sessão, Cache de Dashboard, etc).
- **Estilo:** Utilize as classes utilitárias do Tailwind 4. Siga os tokens de design definidos em `tailwind.config.js`.

---

## 🔐 Fluxo de Autenticação e Segurança

- **Middleware:** O arquivo `src/middleware.ts` protege rotas sob `/dashboard`, `/proposals` e `/workspaces` verificando o cookie `refreshToken`.
- **Silent Refresh:** O `SessionProvider` em `src/components/auth/SessionProvider.tsx` realiza o refresh silencioso no carregamento inicial para obter o `accessToken`.
- **Interceptores Axios:** Localizados em `src/lib/api.ts`, gerenciam automaticamente a injeção do `Authorization: Bearer [token]` e realizam o refresh proativo se o token estiver prestes a expirar.
- **CSRF & XSS:** O `refreshToken` é armazenado como HttpOnly, enquanto o `accessToken` reside apenas em memória (Zustand).

---

## 🧪 Testes e Qualidade

- **Unitários/Componente:** Vitest + React Testing Library.
  - Comando: `npm run test`
- **E2E:** Playwright.
  - Comando: `npm run test:e2e`
- **Linting:** ESLint com regras Next.js e TypeScript.
  - Comando: `npm run lint`
- **Tipagem:** Verificação estrita via TSC.
  - Comando: `npm run type-check`

---

## ⚙️ Comandos Úteis

```bash
npm run dev           # Iniciar servidor de desenvolvimento
npm run build         # Gerar build de produção
npm run start         # Iniciar servidor de produção (após build)
npm run lint:fix      # Corrigir erros de lint automaticamente
npm run format        # Formatar código com Prettier
```

---

## 📋 Guias de Referência Interna

Para detalhes específicos sobre partes do sistema, consulte:
- `DASHBOARD_QUICKSTART.md`: Guia rápido para novos componentes de dashboard.
- `DASHBOARD_REFACTOR.md`: Detalhes da arquitetura de componentes do dashboard.
- `LANDING_PAGE_GUIDE.md`: Guia de implementação e SEO da Landing Page.
- `SEO_AUDIT_REPORT.md`: Relatório de auditoria e melhorias de SEO.

---
*Este arquivo é o guia mestre para o Gemini CLI atuar no frontend do ScopeFlow.*
