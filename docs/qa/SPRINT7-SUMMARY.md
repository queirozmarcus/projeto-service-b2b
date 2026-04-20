# Sprint 7: Contract Tests — Resumo Executivo

**Status:** ✅ Implementado (aguardando execução de testes)  
**Data:** 2026-04-19

## O Que Foi Feito

### 3 Novos Contratos (Provider — user-service)

Criados em `user-service/src/test/resources/contracts/auth/`:

1. ✅ `register-invalid-email-format.yml` — email sem @
2. ✅ `register-invalid-email-missing-domain.yml` — email incompleto
3. ✅ `register-invalid-email-blank.yml` — email vazio

**Padrão validado:** RFC 9457 com error code `VO-001`

### 3 Novos Testes (Consumer — backend)

Adicionados em `UserServiceContractTest.java`:

1. ✅ `shouldReturn400WithVO001_whenRegisteringWithInvalidEmailFormat()`
2. ✅ `shouldReturn400WithVO001_whenRegisteringWithEmailMissingDomain()`
3. ✅ `shouldReturn400WithVO001_whenRegisteringWithBlankEmail()`

### Mocks Atualizados

`ContractVerifierBase.java` mockado para lançar `InvalidValueObjectException` nos 3 cenários.

## Como Validar

```bash
# 1. Provider tests (user-service)
cd user-service
./mvnw test -Dtest=ContractVerifierTest

# 2. Gerar stubs
./mvnw clean install

# 3. Consumer tests (backend)
cd ../backend
./mvnw test -Dtest=UserServiceContractTest
```

**Esperado:**
- user-service: 12 testes (9 existentes + 3 novos)
- backend: 14 testes (11 existentes + 3 novos)

## Garantias

- ✅ RFC 9457 validado (type, title, status, error_code, error_id, timestamp)
- ✅ Content-Type: application/problem+json
- ✅ UUID format (error_id)
- ✅ ISO 8601 format (timestamp)
- ✅ Backward compatibility enforced
- ✅ Breaking changes bloqueadas no CI

## Arquivos Criados/Modificados

**Criados (4):**
- `user-service/src/test/resources/contracts/auth/register-invalid-email-format.yml`
- `user-service/src/test/resources/contracts/auth/register-invalid-email-missing-domain.yml`
- `user-service/src/test/resources/contracts/auth/register-invalid-email-blank.yml`
- `docs/qa/sprint7-contract-tests-report.md` (documentação completa)

**Modificados (2):**
- `user-service/src/test/java/com/scopeflow/user/contract/ContractVerifierBase.java`
- `backend/src/test/java/com/scopeflow/contract/UserServiceContractTest.java`

## Próximos Passos

Rodar os comandos de validação acima para confirmar que todos os testes passam.

---

**Documentação completa:** [sprint7-contract-tests-report.md](sprint7-contract-tests-report.md)
