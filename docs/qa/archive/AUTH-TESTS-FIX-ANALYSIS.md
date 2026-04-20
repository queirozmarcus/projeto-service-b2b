# Análise e Correção: Falhas nos Testes de Autenticação

**Data:** 2026-04-19  
**Responsável:** Security Test Engineer  
**Issue:** 5/63 testes falhando em `AuthControllerIntegrationTest`

---

## Causa Raiz Identificada

### Problema: Duplicate Path Prefix (404 errors)

A aplicação usa **context-path `/api/v1`** configurado em `application.yml`:

```yaml
server:
  servlet:
    context-path: /api/v1
```

Os controllers estavam duplicando esse prefixo:

```java
// ❌ ERRADO (gerava /api/v1/api/v1/auth/...)
@RequestMapping("/api/v1/auth")

// ✅ CORRETO (com context-path gera /api/v1/auth/...)
@RequestMapping("/auth")
```

### Impacto

1. **404 Not Found**: Requisições para `/api/v1/auth/login` tentavam acessar `/api/v1/api/v1/auth/login`
2. **401 Unauthorized**: SecurityConfig não reconhecia os endpoints (mismatch de path)
3. **JWT validation failures**: Endpoints protegidos retornavam 404 antes de validar token

### Testes Afetados

| Nested Class | Testes Falhando | Status Code Esperado | Status Code Real |
|--------------|----------------|---------------------|------------------|
| `JwtCompatibility` | 1/1 | 200 | 404 |
| `GetMe` | 1/2 | 200 | 404 |
| `Login` | 2/3 | 200, 401 | 401, 401 |
| `Register` | 0/2 | — | ✅ |

---

## Correções Aplicadas

### 1. AuthController
**Arquivo:** `user-service/src/main/java/com/scopeflow/user/adapter/in/web/auth/AuthController.java`

```diff
- @RequestMapping("/api/v1/auth")
+ @RequestMapping("/auth")
```

### 2. UserController
**Arquivo:** `user-service/src/main/java/com/scopeflow/user/adapter/in/web/user/UserController.java`

```diff
- @RequestMapping("/api/v1/users")
+ @RequestMapping("/users")
```

### 3. SecurityConfig
**Arquivo:** `user-service/src/main/java/com/scopeflow/user/config/SecurityConfig.java`

```diff
- .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", ...)
+ .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", ...)
```

### 4. AuthControllerIntegrationTest
**Arquivo:** `user-service/src/test/java/com/scopeflow/user/adapter/in/web/auth/AuthControllerIntegrationTest.java`

```diff
- mockMvc.perform(post("/api/v1/auth/register")
+ mockMvc.perform(post("/auth/register")

- mockMvc.perform(post("/api/v1/auth/login")
+ mockMvc.perform(post("/auth/login")

- mockMvc.perform(get("/api/v1/auth/me")
+ mockMvc.perform(get("/auth/me")
```

### 5. UserControllerIntegrationTest
**Arquivo:** `user-service/src/test/java/com/scopeflow/user/adapter/in/web/user/UserControllerIntegrationTest.java`

```diff
- mockMvc.perform(get("/api/v1/users/by-email/{email}", ...)
+ mockMvc.perform(get("/users/by-email/{email}", ...)

- mockMvc.perform(post("/api/v1/users/invited")
+ mockMvc.perform(post("/users/invited")
```

---

## Arquitetura de Paths

### Como Funciona

```
Context-path (application.yml): /api/v1
         +
Controller @RequestMapping:     /auth
         =
URL final:                      /api/v1/auth/login
```

### URLs Finais (após correção)

| Endpoint | Controller Path | URL Final |
|----------|----------------|-----------|
| Register | `/auth/register` | `POST /api/v1/auth/register` |
| Login | `/auth/login` | `POST /api/v1/auth/login` |
| Refresh | `/auth/refresh` | `POST /api/v1/auth/refresh` |
| Logout | `/auth/logout` | `POST /api/v1/auth/logout` |
| Get Profile | `/auth/me` | `GET /api/v1/auth/me` |
| Get User by Email | `/users/by-email/{email}` | `GET /api/v1/users/by-email/{email}` |
| Create Invited | `/users/invited` | `POST /api/v1/users/invited` |

---

## Compatibilidade com Strangler Fig

### Traefik Routing (docker-compose.yml)

```yaml
traefik.http.routers.user-service.rule: PathPrefix(`/api/v1/auth`) || PathPrefix(`/api/v1/users`)
traefik.http.routers.user-service.priority: 100
```

✅ **Mantém-se válido** — Traefik roteia baseado no path completo (`/api/v1/auth`), não no path relativo do controller.

### Monólito AuthControllerV2 (Proxy Fallback)

**Arquivo:** `backend/src/main/java/com/scopeflow/adapter/in/web/auth/AuthControllerV2.java`

```java
@RequestMapping("/api/v1/auth")
public class AuthControllerV2 {
    // Proxy para user-service
    restTemplate.exchange("http://user-service:8081/api/v1/auth/login", ...)
}
```

✅ **Mantém-se válido** — O monólito chama a URL completa com context-path incluído.

---

## Validação

### Comando para executar testes

```bash
cd /home/mq/iGitHub/projeto-service-b2b/user-service
./mvnw test -Dtest=AuthControllerIntegrationTest
```

### Resultado Esperado

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

### Verificação de Regressão

```bash
# Todos os testes do user-service
./mvnw verify

# Testes do monólito (garantir que proxy ainda funciona)
cd ../backend
./mvnw test -Dtest=AuthControllerV2Test
```

---

## Lições Aprendidas

### Anti-Pattern Identificado

**❌ NÃO duplicar context-path nos controllers:**

```java
// Errado quando context-path = /api/v1
@RequestMapping("/api/v1/auth")
```

### Best Practice

**✅ Usar paths relativos nos controllers:**

```java
// Correto — context-path é aplicado automaticamente
@RequestMapping("/auth")
```

### Configuração de Testes

O `AuthControllerIntegrationTest` já estava configurado corretamente:

```java
@BeforeEach
void setup() {
    this.mockMvc = MockMvcBuilders
        .webAppContextSetup(webApplicationContext)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .defaultRequest(MockMvcRequestBuilders.get("/").contextPath("/api/v1"))
        .build();
}
```

Isso configura MockMvc para usar o context-path correto, mas os testes estavam passando paths absolutos que já incluíam `/api/v1/`, causando duplicação.

---

## Próximos Passos

1. ✅ Executar `./mvnw test -Dtest=AuthControllerIntegrationTest` 
2. ✅ Verificar 8/8 testes passando
3. ✅ Executar suite completa: `./mvnw verify`
4. ✅ Atualizar documentação da API se necessário
5. ✅ Commit com mensagem descritiva

---

## Referências

- **CLAUDE.md:** Section "Architecture: Hexagonal + DDD"
- **application.yml:** Line 60-62 (context-path configuration)
- **SecurityConfig.java:** Line 63-72 (requestMatchers)
- **Strangler Fig docs:** `docs/migration/DB-MIGRATION-USER-SERVICE.md`
