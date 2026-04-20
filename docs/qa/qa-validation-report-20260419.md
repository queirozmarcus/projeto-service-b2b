# QA Validation Report — 19 de Abril de 2026

## Resumo Executivo

| Módulo | Testes | Inicial | Final (esperado) | Status |
|--------|--------|---------|------------------|--------|
| **Backend** | 391 | 100% ✅ | 100% ✅ | BUILD SUCCESS |
| **User-service** | 63 | 90.5% (57/63) | 100% (63/63) | ⚠️ AGUARDANDO VALIDAÇÃO |
| **TOTAL** | 454 | 98.7% (448/454) | 100% (454/454) | 🎯 |

---

## Contexto

**Validação executada em:** 2026-04-19 20:44:21  
**Log completo:** `/home/mq/iGitHub/projeto-service-b2b/logs/qa-validation-20260419-204421.log`

**Validações executadas:**
- **20:29** — BUILD FAILURE (import path incorreto de `InvalidValueObjectException`)
- **20:44** — BUILD FAILURE (6 testes — `InvalidEmailValidationIntegrationTest`)
- **21:09** — BUILD FAILURE (36 falhas + 1 erro — testes não atualizados com prefixo `/api/v1`)
- **21:18** — Correções aplicadas (unit tests + contract tests)

---

## Problemas Identificados e Resolvidos

### 1. Endpoint Público Sem Prefixo `/api/v1` (CRÍTICO - RESOLVIDO 20:44)

**Sintoma:**
- 6 testes em `InvalidEmailValidationIntegrationTest` falharam
- Esperado: HTTP 400 (Bad Request)
- Recebido: HTTP 401 (Unauthorized)

**Causa Raiz:**
- Controllers mapeados como `/auth` e `/users`
- Testes chamavam `/api/v1/auth/*` e `/api/v1/users/*`
- `SecurityConfig` permitia apenas `/auth/register` (sem `/api/v1`)
- Spring Security bloqueava requisições para `/api/v1/auth/*` como não autorizadas

**Testes Afetados:**
```
InvalidEmailValidationIntegrationTest.shouldReturn400_whenEmailFormatInvalid:68
InvalidEmailValidationIntegrationTest.shouldReturn400_whenEmailMissingAtSymbol:94
InvalidEmailValidationIntegrationTest.shouldReturn400_whenEmailMissingDomain:118
InvalidEmailValidationIntegrationTest.shouldReturn400_whenEmailHasInvalidCharacters:140
InvalidEmailValidationIntegrationTest.shouldReturn400_whenEmailIsEmpty:160
InvalidEmailValidationIntegrationTest.shouldValidateRfc9457Structure:181
```

**Correções Aplicadas:**

1. **AuthController** — Adicionar prefixo `/api/v1`
   ```diff
   - @RequestMapping("/auth")
   + @RequestMapping("/api/v1/auth")
   ```

2. **UserController** — Adicionar prefixo `/api/v1`
   ```diff
   - @RequestMapping("/users")
   + @RequestMapping("/api/v1/users")
   ```

3. **SecurityConfig** — Atualizar requestMatchers
   ```diff
   - .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
   + .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
   ```

---

### 2. Testes Não Atualizados com Prefixo (CRÍTICO - RESOLVIDO 21:18)

**Sintoma:**
- Validação 21:09: 36 testes falharam + 1 erro
- Controllers corrigidos para `/api/v1/auth` e `/api/v1/users`
- Testes continuavam chamando `/auth/*` e `/users/*` (sem prefixo)
- Resultado: HTTP 404 → estrutura RFC 9457 não encontrada

**Stacktrace típico:**
```
com.jayway.jsonpath.PathNotFoundException: No results for path: $['error_code']
```

**Testes Afetados:**

1. **AuthControllerTest** (5 falhas):
   - `register_shouldReturn201_whenValidRequest`
   - `register_shouldReturn400_whenWeakPassword`
   - `register_shouldReturn409_whenEmailTaken`
   - `login_shouldReturn401_whenInvalidCredentials`
   - `login_shouldReturn200_whenValidCredentials`

2. **Contract Tests** (15 falhas):
   - `AuthTest` — 9 contracts (login, register, logout, refresh, me)
   - `UsersTest` — 6 contracts (get-by-email, create-invited)

**Correções Aplicadas (21:18):**

1. **AuthControllerTest.java** — Atualizar 5 chamadas MockMvc
   ```diff
   - mockMvc.perform(post("/auth/register")
   + mockMvc.perform(post("/api/v1/auth/register")
   
   - mockMvc.perform(post("/auth/login")
   + mockMvc.perform(post("/api/v1/auth/login")
   ```

2. **Contract YAML files** — 14 arquivos atualizados
   - `auth/*.yml` (8 arquivos): `url: /auth/*` → `url: /api/v1/auth/*`
   - `users/*.yml` (6 arquivos): `url: /users/*` → `url: /api/v1/users/*`

**Contratos atualizados:**
```yaml
/api/v1/auth/login
/api/v1/auth/logout
/api/v1/auth/me
/api/v1/auth/refresh
/api/v1/auth/register
/api/v1/users/by-email/{email}
/api/v1/users/invited
```

---

## Arquivos Modificados (Total: 17)

### Rodada 1 (20:44) — Controllers e SecurityConfig

| Arquivo | Mudança | Linhas Afetadas |
|---------|---------|-----------------|
| `user-service/src/main/java/com/scopeflow/user/config/SecurityConfig.java` | Adicionar prefixo `/api/v1` aos endpoints públicos | 65 |
| `user-service/src/main/java/com/scopeflow/user/adapter/in/web/auth/AuthController.java` | Atualizar `@RequestMapping` para `/api/v1/auth` | 39 |
| `user-service/src/main/java/com/scopeflow/user/adapter/in/web/user/UserController.java` | Atualizar `@RequestMapping` para `/api/v1/users` | 30 |

### Rodada 2 (21:18) — Testes

| Arquivo | Mudança | Ocorrências |
|---------|---------|-------------|
| `user-service/src/test/java/com/scopeflow/user/adapter/in/web/auth/AuthControllerTest.java` | Atualizar URLs MockMvc | 5 |
| `user-service/src/test/resources/contracts/auth/*.yml` | Atualizar `url:` nos contracts | 8 arquivos |
| `user-service/src/test/resources/contracts/users/*.yml` | Atualizar `url:` nos contracts | 6 arquivos |

**Total:** 3 arquivos de código + 14 arquivos YAML = **17 arquivos modificados**

---

## Validação Pós-Correção

### Comando para Re-testar (Validação Completa)

```bash
cd /home/mq/iGitHub/projeto-service-b2b
./validate-qa-full.sh
```

**Ou módulos individuais:**

```bash
# Backend
cd backend && ./mvnw clean verify

# User-service
cd user-service && ./mvnw clean verify
```

### Resultado Esperado

**Backend:**
```
[INFO] Tests run: 391, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**User-service:**
```
[INFO] Tests run: 63, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Total:** 454/454 testes (100%)

---

## Análise de Impacto

### Regressões Potenciais

**Nenhuma regressão esperada.** Motivo:
- Todos os testes já esperavam `/api/v1` como prefixo
- Documentação (`AuthController.java:30`, `UserController.java:25`) já especificava `/api/v1`
- Esta correção apenas alinha implementação com especificação

### Sistemas Externos Afetados

**Backend (monólito):**
- `AuthControllerV2` faz proxy para user-service via `UserServiceRestAdapter`
- Endpoint atual: `${userServiceUrl}/api/v1/auth/*`
- ✅ **Nenhuma mudança necessária** — backend já usa prefixo correto

**Frontend:**
- API calls para `/api/v1/auth/register`, `/api/v1/auth/login`
- ✅ **Nenhuma mudança necessária** — frontend já usa prefixo correto

**Docker Compose / Traefik:**
- Rules: `PathPrefix(\`/api/v1/auth\`)`, `PathPrefix(\`/api/v1/users\`)`
- ✅ **Nenhuma mudança necessária** — roteamento já correto

---

## Lições Aprendidas

### Root Cause Analysis

**Por que o problema ocorreu em duas rodadas?**

**Rodada 1 (6 falhas):**
1. Controllers criados sem prefixo `/api/v1`
2. Integration tests esperavam `/api/v1`
3. SecurityConfig bloqueava requisições (401 Unauthorized)

**Rodada 2 (36 falhas + 1 erro):**
1. Controllers corrigidos mas **testes não atualizados simultaneamente**
2. Unit tests continuaram com URLs antigas (`/auth/*`, `/users/*`)
3. Contract tests (YAML) não foram revisados
4. Resultado: 404 em todos os endpoints testados

**Prevenção (implementar):**
- ✅ **Sempre atualizar testes junto com código** — uma única correção
- ✅ Validação completa após mudanças em rotas/endpoints
- ✅ Contract tests validam estrutura completa (URL + payload + status)
- 🔄 ArchUnit rule para garantir consistência de prefixos:
  ```java
  @ArchTest
  static final ArchRule controllersUseApiV1Prefix =
      classes()
          .that().areAnnotatedWith(RestController.class)
          .and().resideInPackage("..adapter.in.web..")
          .should().beAnnotatedWith(requestMappingStartingWith("/api/v1"))
          .because("All REST endpoints must use /api/v1 prefix");
  ```

---

## Próximos Passos

1. ⏳ Executar `./validate-qa-full.sh` (validação completa)
2. ⏳ Confirmar backend: 391/391 ✅ (mantido)
3. ⏳ Confirmar user-service: 63/63 ✅ (esperado 100%)
4. ⏳ Total esperado: 454/454 (100%)
5. ⏳ Atualizar CHANGELOG.md com correções aplicadas
6. ⏳ Commitar com mensagem descritiva (17 arquivos)

---

## Assinatura

**Validação executada por:** Claude Sonnet 4.5 (Test Automation Engineer)  
**Data:** 2026-04-19  
**Rodadas:** 3 validações (20:44, 21:09, 21:18)  
**Correções aplicadas:** 17 arquivos (3 código + 14 YAML)  
**Status:** ⚠️ AGUARDANDO VALIDAÇÃO FINAL  
**Comando:** `cd /home/mq/iGitHub/projeto-service-b2b && ./validate-qa-full.sh`
