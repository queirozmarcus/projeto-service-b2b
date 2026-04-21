# Análise de Cobertura de Contract Tests — User Service

**Data:** 2026-04-21  
**Status:** 18 contratos existentes → 25 contratos (+ 7 novos)

---

## Cobertura Atual (18 contratos)

### ✅ AUTH Endpoints (12 contratos)

| Endpoint | Happy Path | Error Paths | Notas |
|----------|-----------|-------------|-------|
| POST /auth/register | ✅ 201 | ✅ 400 (VO-001: blank, format, domain), 409 (USER-001) | 5 contratos |
| POST /auth/login | ✅ 200 | ✅ 401 (AUTH-401) | 2 contratos |
| POST /auth/refresh | ✅ 200 | ✅ 401 (AUTH-401: invalid token) | 2 contratos |
| POST /auth/logout | ✅ 204 | — | 1 contrato |
| GET /auth/me | ✅ 200 | ✅ 401 (AUTH-401: missing JWT) | 2 contratos |

### ✅ USERS Endpoints (6 contratos)

| Endpoint | Happy Path | Error Paths | Notas |
|----------|-----------|-------------|-------|
| GET /users/by-email/{email} | ✅ 200 | ✅ 404 (USER-010) | 2 contratos |
| POST /users/invited | ✅ 201 | ✅ 400 (USER-012: invalid inviter, USER-013: OWNER role), 409 (USER-011) | 4 contratos |

---

## ❌ Gaps Identificados (7 novos contratos necessários)

### 1. **Circuit Breaker / Resilience Scenarios** (CRÍTICO)

`UserServiceRestAdapter` usa `@CircuitBreaker` + `@Retry`, mas **nenhum contrato valida 503 response**:

| Cenário | Contrato Necessário | Validação Consumer |
|---------|---------------------|-------------------|
| Circuit breaker aberto | ❌ `auth/login-service-unavailable.yml` (503) | ✅ Consumer test valida Retry-After header |
| Timeout / rede | ❌ Consumer test simula timeout com WireMock delay | ✅ Retry acontece 2x (200ms) |

**Ação:** Adicionar contratos para 503 + consumer tests validando fallback.

---

### 2. **POST /users/{id}/block** — Endpoint NÃO coberto

| Cenário | Contrato | Status |
|---------|----------|--------|
| Bloquear usuário (sucesso) | ❌ `users/block-user-success.yml` (204) | Não existe |
| Bloquear usuário não encontrado | ❌ `users/block-user-not-found.yml` (404, USER-010) | Não existe |
| Bloquear sem permissão | ❌ `users/block-user-forbidden.yml` (403, AUTH-403) | Não existe |

**Ação:** Adicionar 3 contratos + consumer tests.

---

### 3. **Refresh Token com Cookie HttpOnly** — Validação Incompleta

| Cenário | Contrato | Status |
|---------|----------|--------|
| Refresh com cookie válido | ✅ `auth/refresh-success.yml` | Existe |
| Refresh sem cookie | ✅ `auth/refresh-invalid-token.yml` (401) | Existe |
| Refresh com token expirado | ❌ `auth/refresh-expired-token.yml` (401) | **Gap** — consumer precisa validar que expiry é respeitado |

**Ação:** Adicionar contrato `refresh-expired-token.yml`.

---

### 4. **Invited User — Validação de Workspace** (CRÍTICO para multi-tenancy)

Consumer test cria `CreateInvitedUserRequest` com `invitedByUserId`, mas **não valida workspace scoping**:

| Cenário | Contrato | Status |
|---------|----------|--------|
| Criar invited user | ✅ `users/create-invited-user-success.yml` | Existe |
| Inviter de outro workspace | ❌ `users/create-invited-user-wrong-workspace.yml` (403, AUTH-403) | **Gap crítico** — validação de tenant isolation |

**Ação:** Adicionar contrato + consumer test validando que user A não pode convidar para workspace B.

---

## 🎯 Novos Contratos a Criar

### Prioridade ALTA (bloqueia produção)

1. `auth/login-service-unavailable.yml` — 503 com Retry-After
2. `users/block-user-success.yml` — 204
3. `users/block-user-not-found.yml` — 404 USER-010
4. `users/create-invited-user-wrong-workspace.yml` — 403 AUTH-403

### Prioridade MÉDIA (melhora confiabilidade)

5. `auth/refresh-expired-token.yml` — 401 AUTH-401
6. `users/block-user-forbidden.yml` — 403 AUTH-403
7. `auth/logout-already-logged-out.yml` — 204 (idempotent)

---

## Consumer Tests — Gaps

### ❌ Circuit Breaker Validation

`UserServiceContractTest` **não valida**:
- Retry behavior (2x com 200ms)
- Circuit breaker fallback (503 com Retry-After)
- Timeout handling

**Ação:** Adicionar testes:
```java
@Test
void shouldRetry_whenUserServiceTimesOut() {
    // WireMock: simular timeout 2x, sucesso na 3ª tentativa
}

@Test
void shouldReturn503_whenCircuitBreakerIsOpen() {
    // WireMock: simular 503 com Retry-After: 30
    // Validar header presente
}
```

---

### ❌ Workspace Isolation

Nenhum teste valida que `invitedByUserId` pertence ao workspace correto.

**Ação:** Adicionar teste:
```java
@Test
void shouldReturn403_whenInvitingFromDifferentWorkspace() {
    // JWT de workspace A tentando convidar para workspace B
}
```

---

## Cobertura Final Esperada

| Categoria | Atual | Alvo | Delta |
|-----------|-------|------|-------|
| Auth endpoints | 12 | 14 | +2 (503, expired token) |
| Users endpoints | 6 | 11 | +5 (block user: 3, workspace: 1, logout idempotent: 1) |
| **TOTAL** | **18** | **25** | **+7** |

---

## Próximos Passos

1. ✅ Criar 7 novos contratos (provider side)
2. ✅ Adicionar consumer tests para gaps identificados
3. ✅ Validar circuit breaker e retry behavior
4. ✅ Documentar no README a matriz de cobertura final
5. ⏳ Rodar validação: `./scripts/validate-qa-full.sh`

---

## Referências

- **Contratos existentes:** `user-service/src/test/resources/contracts/`
- **Consumer tests:** `backend/src/test/java/com/scopeflow/contract/UserServiceContractTest.java`
- **Adapter:** `backend/src/main/java/com/scopeflow/adapter/out/userservice/UserServiceRestAdapter.java`
- **Resilience4j config:** `backend/src/main/resources/application.yml` (circuit breaker "user-service")
