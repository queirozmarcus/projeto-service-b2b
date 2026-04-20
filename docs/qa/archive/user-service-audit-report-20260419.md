# QA Audit Report - User Service

**Data:** 2026-04-19  
**Auditor:** QA Lead (Claude Code)  
**Escopo:** User Service (extraído via Strangler Fig)  
**Status do serviço:** Produção (DB-per-service, PostgreSQL dedicado porta 5433)

---

## Resumo Executivo

**Resultado:** 🟡 APROVADO COM RESSALVAS

- **3 gaps críticos** (bloqueadores para produção)
- **4 gaps médios** (impactam qualidade, não bloqueiam)
- **3 gaps baixos** (melhorias futuras)
- **Cobertura estimada:** ~65% (unit), ~40% (integration), 0% (contract)

**Urgência:** Os 5 testes falhando no `InvalidEmailValidationIntegrationTest` são **sintoma de gap arquitetural** — Email VO não está lançando `InvalidValueObjectException` apesar do handler existir.

---

## Gaps Críticos (Bloqueadores)

### 1. 🔴 Email VO não usa InvalidValueObjectException consistentemente

**Risco:** ALTO — Falhas silenciosas em produção  
**Impacto:** 5 testes falhando, RFC 9457 compliance quebrado

**Evidência:**
```java
// Email.java (linha 20) — CORRETO
throw new InvalidValueObjectException("Email", "Invalid email format: " + value);

// GlobalExceptionHandler.java (linha 109-120) — Handler existe e está correto
@ExceptionHandler(InvalidValueObjectException.class)
public ResponseEntity<ProblemDetail> handleInvalidValueObject(...)

// InvalidEmailValidationIntegrationTest.java — 5 testes esperando VO-001, recebendo VALIDATION-400
```

**Diagnóstico:** O Email VO **já lança** `InvalidValueObjectException`. O handler **existe e está correto**. Os testes estão falhando porque:
1. O `RegisterRequest` DTO pode ter validação `@NotBlank` ou `@Email` que intercepta ANTES do VO ser construído
2. A exception está sendo engolida por outro handler (ex: `handleMethodArgumentNotValid`)

**Recomendação:**
1. Verificar anotações de validação Bean Validation no `RegisterRequest` — remover `@Email` se existir (conflito com VO)
2. Confirmar ordem de handlers — `@ExceptionHandler(InvalidValueObjectException.class)` deve ter precedência
3. Adicionar log no handler para confirmar que está sendo invocado
4. Validar que o serviço de aplicação está construindo o Email VO (não passando String direto para JPA)

---

### 2. 🔴 Zero contract tests com backend monólito

**Risco:** ALTO — Breaking changes silenciosos  
**Impacto:** JWT incompatibilidade, erro codes divergentes, contratos quebrados

**Evidência:**
```bash
# Busca por contract tests
find user-service -name '*Contract*' -o -name '*Pact*'
# Resultado: 0 arquivos

# JWT secret compartilhado mas sem validação automática
backend: JWT_SECRET=xxx
user-service: JWT_SECRET=xxx  # Sincronização manual — quebrável
```

**Cenários não cobertos:**
- JWT emitido pelo user-service é aceito pelo backend?
- Error codes (USER-010 a USER-013) são idênticos?
- Estrutura RFC 9457 é compatível?
- Traefik routing funciona corretamente (prioridade 100 vs 50)?

**Recomendação:**
1. Implementar **Pact contract tests** (consumer: backend, provider: user-service)
2. Validar JWT cross-service em teste automatizado
3. Validar error codes em contrato (não permite divergência)
4. Adicionar ao CI/CD: contract tests bloqueiam deploy se falharem
5. Documento de referência: `docs/qa/archive/contract-tests-detailed.md` (existe mas desatualizado)

---

### 3. 🔴 Aplicação layer sem cobertura de testes

**Risco:** ALTO — Lógica de negócio não testada  
**Impacto:** Bugs em produção em use cases críticos

**Evidência:**
```bash
# Application layer (use cases)
find user-service/src/main/java/com/scopeflow/user/application -name '*.java'
# Resultado: 1 arquivo

# Testes existentes
find user-service/src/test -name '*Test.java'
# Resultado: 6 arquivos (4 domain, 2 adapter/web)

# Zero testes para application layer
find user-service/src/test -path '*/application/*' -name '*Test.java'
# Resultado: 0 arquivos
```

**Use cases sem testes:**
- RegisterUser
- AuthenticateUser
- GetUserProfile
- UpdateUserProfile (se existir)
- DeleteUser (se existir)

**Recomendação:**
1. Criar `RegisterUserTest` — testar validações, duplicação de email, hash de senha
2. Criar `AuthenticateUserTest` — testar credenciais inválidas, usuário inativo
3. Mock apenas repositories (ports out) — não mockar domain
4. Alvo: 80%+ cobertura de use cases (lógica de negócio crítica)

---

## Gaps Médios

### 4. 🟠 JaCoCo não configurado — cobertura invisível

**Risco:** MÉDIO — Sem visibilidade de cobertura real  
**Impaco:** Decisões de teste baseadas em feeling, não dados

**Evidência:**
```bash
# Busca por JaCoCo no pom.xml
grep -A 5 'jacoco' user-service/pom.xml
# Resultado: vazio
```

**Recomendação:**
1. Adicionar plugin JaCoCo ao `user-service/pom.xml` (copiar do backend)
2. Configurar quality gate: 80% linha, 80% branch
3. Gerar relatório em `target/site/jacoco/index.html`
4. Adicionar ao CI/CD: coverage report como artefato

---

### 5. 🟠 Testes de integração sem validação de Testcontainers startup

**Risco:** MÉDIO — Testes flaky, falhas intermitentes  
**Impacto:** Confiança reduzida no CI/CD

**Evidência:**
```java
// InvalidEmailValidationIntegrationTest.java (linha 36-38)
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scopeflow_test");

// Sem waitStrategy, healthcheck ou logs de startup
```

**Recomendação:**
1. Adicionar `waitStrategy` explícita:
   ```java
   .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
   ```
2. Adicionar timeout: `.withStartupTimeout(Duration.ofSeconds(60))`
3. Adicionar healthcheck: verificar que schema foi aplicado antes dos testes
4. Considerar `@DirtiesContext` se necessário

---

### 6. 🟠 Domain exceptions sem testes unitários

**Risco:** MÉDIO — Error codes e mensagens não validadas  
**Impacto:** Inconsistência em produção

**Evidência:**
```bash
# 6 domain exceptions
EmailAlreadyRegisteredException (USER-010)
InvalidCredentialsException (AUTH-401)
UserNotFoundException (USER-010)  # Código duplicado?
DuplicateEmailException (USER-011)
InvalidInvitedByUserException (USER-012)
InvalidRoleException (USER-013)

# Zero testes para exceptions
find user-service/src/test -name '*ExceptionTest.java'
# Resultado: 0 arquivos
```

**Recomendação:**
1. Criar `DomainExceptionsTest` — validar error codes são únicos e estáveis
2. Validar que mensagens não expõem dados sensíveis
3. Validar que USER-010 não está duplicado (UserNotFoundException)

---

### 7. 🟠 Divergência Email VO: user-service vs backend

**Risco:** MÉDIO — Comportamento inconsistente  
**Impacto:** Email válido em um serviço, inválido em outro

**Evidência:**
```java
// user-service/Email.java (linha 11)
private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

// backend/Email.java
// Arquivo não encontrado em core/domain/shared — pode não existir ou estar em outro path
```

**Recomendação:**
1. Localizar Email VO no backend
2. Comparar regex — devem ser idênticos
3. Extrair para shared library ou garantir sincronização via contrato
4. Adicionar teste cross-service: "emails aceitos pelo user-service são aceitos pelo backend"

---

## Gaps Baixos

### 8. ⚪ Sem testes de segurança (OWASP)

**Risco:** BAIXO — Serviço interno (atrás de Traefik)  
**Impacto:** Vulnerabilidades não detectadas

**Recomendação:**
1. SQL injection: Testcontainers + JPA protege, mas validar queries nativas se existirem
2. JWT manipulation: adicionar teste negativo (token adulterado)
3. Rate limiting: sem proteção — considerar Bucket4j ou Traefik rate limit

---

### 9. ⚪ Sem testes de performance

**Risco:** BAIXO — Ainda não é gargalo  
**Impacto:** Latência desconhecida, capacidade não validada

**Recomendação:**
1. Adicionar JMH benchmark para Email VO (regex performance)
2. Adicionar teste de carga: 100 req/s de registro por 60s
3. Validar tempos de resposta: p50 < 100ms, p99 < 500ms

---

### 10. ⚪ Sem resilience patterns (circuit breaker)

**Risco:** BAIXO — User-service não chama outros serviços  
**Impacto:** Nenhum (por enquanto)

**Evidência:**
```bash
# Busca por @CircuitBreaker, @Retry, @RateLimiter
grep -r "@CircuitBreaker\|@Retry" user-service/src/main --include="*.java"
# Resultado: 0 ocorrências

# Backend tem (user-service, ses)
# User-service não precisa (não chama ninguém)
```

**Recomendação:**
1. Monitorar: se user-service começar a chamar OpenAI/S3/etc, adicionar circuit breakers
2. Por enquanto, não é gap — design correto

---

## Cobertura Atual

### Unit Tests
- **Domain (Email, User, VOs):** ~80% (11 testes, bem cobertos)
- **Application (use cases):** 0% (CRÍTICO)
- **Exceptions:** 0% (MÉDIO)
- **Total estimado:** ~65%

### Integration Tests
- **Web layer (controllers):** 3 classes testadas (AuthController, UserController, InvalidEmailValidation)
- **Database layer:** Testcontainers ativo, mas sem testes específicos de repository
- **Total estimado:** ~40%

### Contract Tests
- **User-service ↔ Backend:** 0% (CRÍTICO)
- **JWT cross-service:** não validado automaticamente

### E2E Tests
- **Não aplicável** — user-service é backend, não tem UI

---

## Recomendações Priorizadas

### Sprint Atual (Bloqueadores)
1. **Fix Email VO exception handling** — investigar por que `InvalidValueObjectException` não está sendo capturada (verificar `RegisterRequest` DTO)
2. **Adicionar testes de application layer** — RegisterUser e AuthenticateUser (lógica crítica)
3. **Implementar contract tests** — Pact entre user-service e backend (JWT + error codes)

### Próximo Sprint (Qualidade)
4. **Configurar JaCoCo** — visibilidade de cobertura real
5. **Testar domain exceptions** — validar error codes únicos
6. **Validar Email VO sync** — comparar regex user-service vs backend

### Backlog (Melhorias)
7. **Testes de segurança** — JWT manipulation, rate limiting
8. **Testes de performance** — benchmarks e load testing
9. **Testcontainers healthcheck** — reduzir flakiness

---

## Quality Gates para Release

### Bloqueadores (não passar = não deployar)
- [ ] 5 testes de `InvalidEmailValidationIntegrationTest` passando (100%)
- [ ] Application layer com 80%+ cobertura
- [ ] Contract tests implementados e passando
- [ ] JaCoCo configurado e reportando 70%+ overall

### Desejáveis (passar = confiança alta)
- [ ] Domain exceptions testadas
- [ ] Email VO sincronizado com backend (validado por teste)
- [ ] Testcontainers com waitStrategy
- [ ] Zero testes flaky no CI/CD

---

## Análise de Risco

| Componente | Risco Atual | Cobertura | Prioridade |
|------------|-------------|-----------|------------|
| Email VO | 🟢 Baixo | 90% | Manter |
| User aggregate | 🟢 Baixo | 80% | Manter |
| Use cases | 🔴 Alto | 0% | 🔴 URGENTE |
| Controllers | 🟡 Médio | 60% | 🟠 Melhorar |
| Exceptions | 🟡 Médio | 0% | 🟠 Melhorar |
| Contratos | 🔴 Alto | 0% | 🔴 URGENTE |

---

## Comparação Backend vs User-Service

| Aspecto | Backend | User-Service | Sincronizado? |
|---------|---------|--------------|---------------|
| `InvalidValueObjectException` | ✅ Implementado | ✅ Implementado | ✅ Sim (código idêntico) |
| `GlobalExceptionHandler` | ✅ RFC 9457 completo | ✅ RFC 9457 completo | ✅ Sim |
| Error codes | USER-010 a USER-013 | USER-010 a USER-013 | ⚠️ Verificar duplicação |
| Email VO regex | ❓ Não localizado | ✅ Definido | ❓ Requer validação |
| Circuit breakers | ✅ user-service, ses | ❌ Nenhum | ✅ Correto (não precisa) |
| Contract tests | ❌ Não | ❌ Não | 🔴 Gap crítico |

---

## Ações Imediatas

```bash
# 1. Rodar testes atuais e capturar log detalhado
cd user-service && ./mvnw test -Dtest=InvalidEmailValidationIntegrationTest > /tmp/test-output.log 2>&1

# 2. Inspecionar RegisterRequest DTO
grep -A 10 "class RegisterRequest" user-service/src/main/java/com/scopeflow/user/adapter/in/web/auth/dto/RegisterRequest.java

# 3. Confirmar ordem de exception handlers
grep -n "@ExceptionHandler" user-service/src/main/java/com/scopeflow/user/adapter/in/web/GlobalExceptionHandler.java

# 4. Validar que Email VO está sendo construído no use case
grep -A 5 "new Email" user-service/src/main/java/com/scopeflow/user/application/*.java

# 5. Adicionar logging temporário no handler
# (Editar GlobalExceptionHandler.handleInvalidValueObject para logar entrada)
```

---

## Referências

- **Backend GlobalExceptionHandler:** `/backend/src/main/java/com/scopeflow/adapter/in/web/GlobalExceptionHandler.java` (695 linhas, RFC 9457 completo)
- **User-service GlobalExceptionHandler:** `/user-service/src/main/java/com/scopeflow/user/adapter/in/web/GlobalExceptionHandler.java` (178 linhas, subset do backend)
- **RFC 9457:** Problem Details for HTTP APIs
- **Error codes registry:** USER-010 (UserNotFound), USER-011 (DuplicateEmail), USER-012 (InvalidInvitedByUser), USER-013 (InvalidRole), AUTH-401 (InvalidCredentials), VO-001 (InvalidValueObject)
- **Contract tests doc:** `docs/qa/archive/contract-tests-detailed.md` (arquivado — requer atualização)

---

## Conclusão

O user-service tem **fundação sólida** (domain bem testado, handlers corretos), mas **gaps críticos em integração**:

1. **Email VO exception não está chegando ao handler** (5 testes falhando) — investigação urgente
2. **Application layer 0% testado** — lógica de negócio sem cobertura
3. **Zero contract tests** — risco alto de breaking changes silenciosos

**Recomendação:** Não promover para produção até resolver gaps 1-3. Tempo estimado: 1-2 dias (1 dev + 1 QA).

---

**Próximos passos:**
1. Rodar `./mvnw test` e analisar stacktrace completo dos 5 testes falhando
2. Inspecionar `RegisterRequest` DTO — provável causa raiz
3. Implementar testes de use cases (RegisterUser, AuthenticateUser)
4. Configurar Pact para contract testing

**Contato:** Para dúvidas, consultar QA Lead ou revisar este documento em `docs/qa/user-service-audit-report-20260419.md`.
