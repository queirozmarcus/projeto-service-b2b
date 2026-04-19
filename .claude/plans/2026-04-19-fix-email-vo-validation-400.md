---
title: "Fix: Email VO inválido deve retornar 400 (não 500)"
status: APROVADO
created: 2026-04-19
priority: MÉDIA
estimated_time: 2-3h (5 sprints)
---

# Plano: Email VO Inválido → 400 Bad Request

**Problema:** Email VO com formato inválido lança `IllegalArgumentException` que cai no catch-all do GlobalExceptionHandler → HTTP 500 (deveria ser 400).

**Contexto:**
- `Email.java` (linha 18): `throw new IllegalArgumentException("Invalid email format: " + value)`
- `GlobalExceptionHandler.handleGenericException()`: catch-all → 500
- Afeta: monólito (`backend/`) e user-service
- RFC 9457 Problem Details deve ser mantido

**Objetivo:** Criar exception domain específica + handler → 400 Bad Request consistente.

**Agentes envolvidos:**
- `backend-dev` — Sprints 1, 2, 3, 5 (implementação)
- `integration-test-engineer` — Sprint 4 (testes)
- `marcus` — Sprint 5 (documentação)

---

## Sprint 1: Criar Exception de Domínio para Validação de VO

**Agente:** `backend-dev`  
**Duração:** 20 min

### Tarefas
1. Criar `InvalidValueObjectException` em `core/domain/common/`
   ```java
   public class InvalidValueObjectException extends RuntimeException {
       private final String errorCode;
       private final String voType;
       
       public InvalidValueObjectException(String voType, String message) {
           super(message);
           this.voType = voType;
           this.errorCode = "VALIDATION-400";
       }
   }
   ```

2. Criar package `core/domain/common/` se não existir

### Entregáveis
- [x] `InvalidValueObjectException.java` criado
- [x] Javadoc explicando uso para VOs
- [x] ErrorCode padronizado: `VALIDATION-400`

### Critérios de Aceite
- Exception extends RuntimeException
- Contém errorCode + voType para rastreabilidade
- Javadoc com exemplos de uso

---

## Sprint 2: Atualizar Email VO (Backend)

**Agente:** `backend-dev`  
**Duração:** 15 min

### Tarefas
1. Editar `backend/src/main/java/com/scopeflow/core/domain/user/Email.java`
2. Trocar `IllegalArgumentException` por `InvalidValueObjectException`
   ```java
   if (!trimmed.matches(EMAIL_REGEX)) {
       throw new InvalidValueObjectException("Email", "Invalid email format: " + value);
   }
   ```
3. Atualizar imports

### Entregáveis
- [x] Email VO lança `InvalidValueObjectException`
- [x] Mensagem de erro mantida (backward compatible)

### Critérios de Aceite
- Compilação OK
- Testes existentes continuam passando (comportamento preservado)
- Exception message inalterado

---

## Sprint 3: Adicionar Handler no GlobalExceptionHandler

**Agente:** `backend-dev`  
**Duração:** 25 min

### Tarefas
1. Editar `backend/.../adapter/in/web/GlobalExceptionHandler.java`
2. Adicionar handler específico (antes do catch-all):
   ```java
   @ExceptionHandler(InvalidValueObjectException.class)
   public ResponseEntity<ProblemDetail> handleInvalidValueObject(
           InvalidValueObjectException ex,
           WebRequest request
   ) {
       ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
       problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-value-object"));
       problemDetail.setTitle("Invalid Value Object");
       problemDetail.setDetail(ex.getMessage());
       problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
       problemDetail.setProperty("vo_type", ex.getVoType());
       addCustomProperties(problemDetail, ex.getErrorCode());
       
       return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
   }
   ```

### Entregáveis
- [x] Handler adicionado antes de `handleGenericException`
- [x] RFC 9457 Problem Details respeitado
- [x] Property `vo_type` para rastreabilidade

### Critérios de Aceite
- HTTP 400 retornado
- JSON Problem Details bem formado
- Logs não poluídos (não deve logar stack trace em nível ERROR)

---

## Sprint 4: Escrever Testes de Integração

**Agente:** `integration-test-engineer`  
**Duração:** 40 min

### Tarefas
1. Criar `InvalidEmailValidationIntegrationTest.java`
2. Testar cenários:
   - POST `/api/v1/auth/register` com email inválido → 400
   - POST `/api/v1/users` com email inválido → 400
   - Validar RFC 9457 structure
   - Validar `error_code: "VALIDATION-400"`
   - Validar `vo_type: "Email"`

3. Criar teste unitário `EmailTest.shouldThrowInvalidValueObjectException_whenFormatInvalid()`

### Entregáveis
- [x] `InvalidEmailValidationIntegrationTest.java` com 3+ cenários
- [x] `EmailTest` atualizado para validar exception type
- [x] Todos os testes passando

### Critérios de Aceite
- `./mvnw test` → PASS
- `./mvnw verify` → PASS
- Cobertura de Email VO: 100%
- RFC 9457 validado (type, title, status, detail, error_code, vo_type)

---

## Sprint 5: Replicar para User Service + Documentação

**Agente:** `backend-dev` (replicação) + `marcus` (documentação)  
**Duração:** 30 min

### Tarefas
1. Replicar mudanças no `user-service/`:
   - Criar `InvalidValueObjectException`
   - Atualizar `Email.java`
   - Adicionar handler em `GlobalExceptionHandler`
   - Atualizar testes

2. Atualizar documentação:
   - `docs/qa/README.md`: remover gap "Email VO → 500"
   - `docs/architecture/adr/`: criar ADR-006-value-object-validation.md
   - `CHANGELOG.md`: documentar fix

### Entregáveis
- [x] User service alinhado com monólito
- [x] Gap removido da doc QA
- [x] ADR criado explicando padrão de validação de VO

### Critérios de Aceite
- `cd user-service && ./mvnw verify` → PASS
- Contract tests passando (backward compatible)
- Doc atualizada e revisada

---

## Validação Final (pós-Sprint 5)

```bash
# Validação completa
./scripts/validate-qa-full.sh

# Teste manual
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "invalid-email", "password": "Test@1234"}'

# Esperado: HTTP 400
# {
#   "type": "https://api.scopeflow.com/errors/invalid-value-object",
#   "title": "Invalid Value Object",
#   "status": 400,
#   "detail": "Invalid email format: invalid-email",
#   "error_code": "VALIDATION-400",
#   "vo_type": "Email",
#   "error_id": "...",
#   "timestamp": "..."
# }
```

---

## Riscos e Mitigações

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| Contract tests quebram (mudança de exception) | Alto | Exception message inalterado + backward compatible |
| Outros VOs usam IllegalArgumentException | Médio | Aplicar mesmo padrão em sprint futuro |
| Frontend depende de HTTP 500 | Baixo | Improvável — 400 é semanticamente correto |

---

## Extensões Futuras (não neste plano)

- Aplicar padrão para outros VOs: `PasswordHash`, `PublicToken`, `BriefingSessionId`
- Criar abstract `ValueObject<T>` base class com validação padronizada
- Adicionar validation annotations (@Valid) nos DTOs para catch mais cedo

---

**Aprovação:** ✅ Pronto para execução
**Estimativa total:** 2h10min (130 min)
**Complexidade:** Baixa (mudança localizada, pattern claro)
