# Sprint 5: Frontend Authentication & Session Management

**Data:** 2026-03-24
**Status:** APROVADO — Pronto para Execução
**Estimativa:** ~10 horas (1.5 dias)

---

## Decisões Arquiteturais (D11-D15)

| Decisão | O quê | Por quê | Trade-off |
|---------|-------|---------|----------|
| **D11** | httpOnly cookies (refresh) + memory (access) | XSS security, HTTP-only prevents JS theft | Silent refresh no cold start (~100ms) |
| **D12** | Zustand para state mgmt | Seletores granulares, acesso fora React (axios) | +1 dependência (~1KB) |
| **D13** | Token refresh com mutex | Evita race conditions em múltiplos requests | Complexidade moderada |
| **D14** | Next.js middleware + layout guards | Fast edge redirect + client-side validation | Cookie check apenas (não JWT validation) |
| **D15** | BroadcastChannel para sync multi-aba | Logout sincronizado sem localStorage | Safari < 15.4 não suporta (fallback health check) |

---

## Plano de Execução

### Etapa 1: Backend Prep (1h) → `backend-dev`
**O quê:** Modificar AuthController para retornar refresh token via Set-Cookie httpOnly

**Mudanças:**
- Endpoint `/auth/login`: retorna access token (body) + refresh token (Set-Cookie httpOnly)
- Endpoint `/auth/refresh`: recebe refresh token (cookie), retorna novo access token (body)
- SecurityConfig: configure CORS para incluir `credentials: 'include'`

**Entregável:**
- ✅ /auth/login retorna JWT access token + Set-Cookie refresh
- ✅ /auth/refresh funciona com httpOnly cookie
- ✅ CORS permite credentials

**Success Criteria:**
- Testar com Postman: Set-Cookie header presente e HttpOnly flag setado

---

### Etapa 2: Frontend Setup (1h) → `backend-dev` (JS/TS)
**O quê:** Estrutura de pastas, Zustand store, axios config, dependências

**Dependências a Adicionar:**
```json
{
  "zustand": "^4.4.0",
  "axios": "^1.6.0",
  "jwtdecode": "^4.0.0"
}
```

**Estrutura:**
```
frontend/
├── app/
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   ├── register/page.tsx
│   │   └── layout.tsx
│   ├── (protected)/
│   │   ├── dashboard/page.tsx
│   │   └── layout.tsx
│   ├── api/ (helpers)
│   └── middleware.ts
├── hooks/
│   ├── useAuth.ts (login/register/logout)
│   └── useSession.ts (Zustand store)
├── lib/
│   ├── api.ts (axios instance)
│   ├── jwt.ts (token helpers)
│   └── broadcast.ts (BroadcastChannel)
├── components/
│   ├── auth/ (LoginForm, RegisterForm)
│   ├── protected/ (Navbar)
│   └── ...
```

**Entregável:**
- ✅ Zustand store com: accessToken, user, isAuthenticated, setSession(), clearSession()
- ✅ axios instance com interceptor (placeholder para refresh logic)
- ✅ JWT decode helper
- ✅ BroadcastChannel wrapper
- ✅ npm install + estrutura de pastas

---

### Etapa 3: Auth Forms (4h) → `backend-dev` (implementação)
**O quê:** LoginForm + RegisterForm com validação, error handling, loading

#### 3a. LoginForm (2h)
- Email + Password inputs
- Chamar API POST /auth/login
- Armazenar access token no Zustand
- Error handling (invalid credentials, server error)
- Loading spinner
- Link para RegisterForm

**Validações:**
- Email válido (regex ou library)
- Password mínimo 8 chars
- Show/hide password toggle

**Entregável:**
- ✅ src/components/auth/LoginForm.tsx
- ✅ Integração com useAuth hook
- ✅ Error toast UI
- ✅ Redirect para /dashboard após login bem-sucedido

#### 3b. RegisterForm (2h)
- Workspace name, User email, Password, Confirm password
- Chamar API POST /auth/register
- Auto-login após registro
- Loading + error handling

**Validações:**
- Workspace name: 3-50 chars
- Email válido
- Password mínimo 8 chars
- Confirm password match

**Entregável:**
- ✅ src/components/auth/RegisterForm.tsx
- ✅ Integração com useAuth hook
- ✅ Redirect para /dashboard após registro
- ✅ Mensagem de sucesso (workspace criado)

---

### Etapa 4: Session Management (1h) → `backend-dev`
**O quê:** Axios interceptor com refresh logic + Zustand initialization

**Axios Interceptor:**
```
1. Before request: decodifica access token, verifica expiração
2. Se expira em < 60s: POST /auth/refresh (cookie vai automático)
3. Mutex: se múltiplos requests, aguarda primeiro refresh
4. Se refresh falha (401): clearSession + redirect /login
5. Repete request original com novo token
```

**Zustand Initialization:**
- useSessionStore.hydrate() no `app/layout.tsx`
- Tenta silent refresh ao carregar app (verifica se cookie existe)
- Se falha, redireciona para /login

**Entregável:**
- ✅ src/lib/api.ts com interceptor completo
- ✅ Mutex para evitar race conditions
- ✅ useSession hook para componentes
- ✅ Silent refresh no app init

---

### Etapa 5: Protected Routes (1h) → `backend-dev` ou `api-designer`
**O quê:** Middleware + layout guards + PrivateRoute logic

#### 5a. Next.js Middleware
```typescript
// app/middleware.ts
- Verifica se cookie refresh_token existe
- Rotas /(protected)/* sem cookie → redirect /login (edge)
- Rotas /(auth)/* com cookie → redirect /dashboard (edge)
```

#### 5b. Layout Guards
```typescript
// app/(protected)/layout.tsx
- useSessionStore.accessToken existe?
- Não → Silent refresh
- Falhou → Redirect /login
- Sim → Renderiza children
```

#### 5c. Navbar Component
```typescript
// components/protected/Navbar.tsx
- Exibe user name, email, workspace
- Logout button (clearSession + BroadcastChannel logout message)
- Links para dashboard, proposals, settings
```

**Entregável:**
- ✅ src/app/middleware.ts
- ✅ src/app/(protected)/layout.tsx com guard
- ✅ src/components/protected/Navbar.tsx
- ✅ BroadcastChannel sync logout entre abas

---

### Etapa 6: Tests (2h) → `e2e-test-engineer`
**O quê:** E2E tests com Playwright para fluxos críticos

#### Test Cases:
1. **Happy path login**
   - Acessa /login
   - Entra credentials válidos
   - Redireciona para /dashboard
   - Navbar mostra user info

2. **Happy path register**
   - Acessa /register
   - Cria workspace + user
   - Auto-login funciona
   - Redireciona para /dashboard

3. **Token refresh**
   - Login bem-sucedido
   - Aguarda 15min de inatividade
   - Próximo request dispara refresh silencioso
   - Request é bem-sucedido (não pede novo login)

4. **Logout multi-aba**
   - Login em aba 1
   - Abra aba 2 (verifica sincronismo)
   - Logout em aba 1
   - Aba 2 detecta e redireciona para /login

5. **Invalid credentials**
   - Login com email inválido
   - Mostra error toast
   - Permanece em /login

6. **Protected route redirect**
   - Tenta acessar /dashboard sem login
   - Redireciona para /login

**Entregável:**
- ✅ playwright.config.ts (configure base URL, browser)
- ✅ e2e/auth.spec.ts (6 testes)
- ✅ CI integration (GitHub Actions rodando e2e antes de deploy)

---

## Dependência no Backend

**CRÍTICO:** Antes de começar frontend, backend precisa fazer:
- Modificar `/auth/login` e `/auth/refresh` para retornar refresh token via Set-Cookie httpOnly
- Estimativa: 1-2 horas
- Status: ⏳ **Deve ser feito em Etapa 1**

---

## Sequência de Delegação

1. **architect** — Brainstorm (✅ COMPLETO)
2. **backend-dev** → Etapa 1 (backend fix) + Etapa 2 (frontend setup) + Etapa 3 (forms) + Etapa 4 (session) + Etapa 5 (routes)
3. **e2e-test-engineer** → Etapa 6 (tests)

Alternativa paralela:
- backend-dev trabalha Etapas 1-5 (8h total)
- e2e-test-engineer pronta após Etapa 5 (2h testes)

---

## Riscos & Mitigações

| Risco | Impacto | Mitigação |
|-------|--------|-----------|
| JWT expiração não tratada | App força logout inesperado | Mutex + queue resolve race conditions |
| Refresh token theft via XSS | Account takeover | httpOnly cookie + CSP headers |
| Silent refresh falha | User perde sessão | Fallback: redireciona /login |
| localStorage data exposure | PII leak (briefings, propostas) | Decisão D11 elimina localStorage |
| Safari offline (< 15.4) | Logout não sincroniza | Health check fallback ao focar aba |

---

## Success Criteria

- ✅ Login form funciona, recebe access token no Zustand
- ✅ Register form cria workspace + user, faz auto-login
- ✅ Access token expira 15min, refresh automático antes de timeout
- ✅ PrivateRoute redireciona /login se !token
- ✅ Logout sincronizado entre abas via BroadcastChannel
- ✅ Todas 6 E2E tests passam
- ✅ Sem security warnings (CSP, CORS, X-Frame-Options)

---

## Timeline

| Etapa | Tempo | Agent | Status |
|-------|-------|-------|--------|
| 1. Backend prep | 1h | backend-dev | ⏳ Ready |
| 2. Frontend setup | 1h | backend-dev | ⏳ Ready |
| 3. Forms | 4h | backend-dev | ⏳ Ready |
| 4. Session mgmt | 1h | backend-dev | ⏳ Ready |
| 5. Protected routes | 1h | backend-dev | ⏳ Ready |
| 6. E2E tests | 2h | e2e-test-engineer | ⏳ Ready |
| **Total** | **~10h** | — | **PRONTO** |

---

## Próximos Passos Após Sprint 5

- [ ] Deploy em staging
- [ ] Smoke tests: login → dashboard → logout
- [ ] Sprint 6: Advanced features (proposals, briefings UX)
- [ ] Sprint 7: Workspace management (members, roles, permissions)

---

**Status Final:** ✅ PRONTO PARA EXECUÇÃO

Agentes: **backend-dev**, **e2e-test-engineer**
Começar com: **Etapa 1 (backend-dev)**

