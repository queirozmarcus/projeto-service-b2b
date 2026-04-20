# Sprint 5 — Test Fix Report: InvalidEmailValidationIntegrationTest

**Data:** 2026-04-19  
**Executor:** Claude Code (Test Automation Engineer)  
**Objetivo:** Validar e corrigir os 5 testes falhando do `InvalidEmailValidationIntegrationTest`

---

## 1. Contexto

Após Sprints 2-4 (criação de `InvalidValueObjectException`, handler RFC 9457, sincronização do Email VO), 5 dos 6 testes de `InvalidEmailValidationIntegrationTest` ainda falhavam.

**Falhas observadas:**
```
Expected: $.type = "https://api.scopeflow.com/errors/invalid-value-object"
Actual: $.type = "https://api.scopeflow.com/errors/validation-error"

Expected: $.error_code = "VO-001"
Actual: $.error_code = "VALIDATION-400"
```

---

## 2. Root Cause

**Bean Validation (`@Email`) interceptava ANTES do domain layer.**

O DTO `RegisterRequest` tinha anotação `@Email` do Jakarta Validation:
```java
public record RegisterRequest(
    @NotBlank @Email String email,  // ❌ Intercepta antes do Email VO
    ...
) {}
```

Quando email inválido chegava, `@Email` validava primeiro → retornava 400 com `type=validation-error` → domain Email VO nunca era chamado → `InvalidValueObjectException` nunca era lançada.

---

## 3. Correção Aplicada

### 3.1. Remover Bean Validation do DTO

**Arquivo:** `user-service/src/main/java/com/scopeflow/user/adapter/in/web/auth/dto/RegisterRequest.java`

```diff
- import jakarta.validation.constraints.Email;
  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.Pattern;
  import jakarta.validation.constraints.Size;

  public record RegisterRequest(
-     @NotBlank @Email String email,
+     @NotBlank String email,  // ✅ Validação acontece no Email VO
      ...
  ) {}
```

**Rationale:** Deixar validação de formato do email acontecer no domain layer (Email VO), onde `InvalidValueObjectException` é lançada.

### 3.2. Corrigir Imports dos Testes

Dois arquivos de teste tinham import incorreto de `InvalidValueObjectException`:

**Arquivo 1:** `user-service/src/test/java/com/scopeflow/user/domain/model/EmailTest.java`
```diff
- import com.scopeflow.user.domain.InvalidValueObjectException;
+ import com.scopeflow.user.domain.shared.InvalidValueObjectException;
```

**Arquivo 2:** `user-service/src/test/java/com/scopeflow/user/domain/UserTest.java`
```diff
- import com.scopeflow.user.domain.InvalidValueObjectException;
+ import com.scopeflow.user.domain.shared.InvalidValueObjectException;
```

---

## 4. Validação

### 4.1. InvalidEmailValidationIntegrationTest (target)

```bash
cd user-service && ./mvnw test -Dtest=InvalidEmailValidationIntegrationTest
```

**Resultado:** ✅ **6/6 testes passando**

```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4.2. Suite Completa (regressão)

```bash
cd user-service && ./mvnw test
```

**Resultado:** ✅ **48/48 testes passando**

```
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
Total time: 02:06 min
```

**Arquivos de teste validados:**
- `InvalidEmailValidationIntegrationTest` (6 testes)
- `EmailTest` (11 testes)
- `UserTest` (4 nested classes)
- `AuthControllerIntegrationTest` (11 testes)
- `UserControllerIntegrationTest` (10 testes)
- Outros (6 classes restantes)

---

## 5. Testes Agora Passando

| Teste | Status Anterior | Status Atual |
|-------|----------------|--------------|
| `shouldReturn400_whenEmailFormatInvalid` | ❌ Falha ($.type wrong) | ✅ Passa |
| `shouldReturn400_whenEmailHasInvalidCharacters` | ❌ Falha ($.type wrong) | ✅ Passa |
| `shouldReturn400_whenEmailMissingAtSymbol` | ❌ Falha ($.type wrong) | ✅ Passa |
| `shouldReturn400_whenEmailMissingDomain` | ❌ Falha ($.type wrong) | ✅ Passa |
| `shouldValidateRfc9457Structure` | ❌ Falha ($.error_code wrong) | ✅ Passa |
| `shouldReturn400_whenEmailIsBlank` | ✅ Já passava | ✅ Passa |

---

## 6. Fluxo de Validação (After Fix)

```
1. POST /api/v1/auth/register { "email": "invalid" }
2. @NotBlank valida → OK (não vazio)
3. @Email foi REMOVIDO → não intercepta mais
4. Controller chama application layer
5. Application layer tenta criar Email VO
6. Email VO lança InvalidValueObjectException("Email", "Invalid email format")
7. GlobalExceptionHandler captura → retorna RFC 9457:
   {
     "type": "https://api.scopeflow.com/errors/invalid-value-object",
     "status": 400,
     "error_code": "VO-001",
     "title": "Invalid Value Object",
     "detail": "Email validation failed: Invalid email format: invalid"
   }
```

---

## 7. Checklist Final

- [x] Testes rodados: `InvalidEmailValidationIntegrationTest`
- [x] Root cause identificado: Bean Validation `@Email` interceptava antes do domain
- [x] Correção aplicada: removido `@Email` do DTO + corrigido imports dos testes
- [x] 6/6 testes passando no `InvalidEmailValidationIntegrationTest`
- [x] Suite completa rodada: 48/48 testes passando
- [x] Zero regressão detectada
- [x] Relatório documentado: `docs/qa/sprint5-test-fix-report.md`

---

## 8. Decisão Arquitetural

**Princípio aplicado:** Validação de formato de domínio deve acontecer no **domain layer** (Value Objects), não na **adapter layer** (DTOs).

**Bean Validation em DTOs:** usar apenas para validações sintáticas genéricas (`@NotBlank`, `@Size`, `@Pattern` para formatos não-domínio como telefone E.164). Validações semânticas de domínio (email, CPF, regras de negócio) devem acontecer no domain model.

**Benefício:** Exceções de domínio são capturadas pelo handler correto → RFC 9457 consistente em toda a API.

---

## 9. Próximos Passos

1. ✅ Sprint 5 concluída — todos os testes de validação de email funcionais
2. Sprint 6+ (backlog): aplicar mesmo padrão para outros VOs (se houver `@Pattern` ou validações genéricas que poderiam ser domain-driven)
3. Documentar em ADR (se apropriado): "Validação de domínio deve acontecer em VOs, não em Bean Validation"

---

**Status Final:** ✅ **Sprint 5 — SUCESSO**  
**Testes user-service:** 48/48 passando  
**Testes target:** 6/6 passando  
**Regressão:** Zero
