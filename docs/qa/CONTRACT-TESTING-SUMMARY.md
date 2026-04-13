# Contract Testing — Implementation Summary

## Objetivo

Garantir compatibilidade entre `user-service` (provider) e `monólito` (consumer) durante migração Strangler Fig.

**Framework:** Spring Cloud Contract 4.1.0

## O Que Foi Implementado

### 1. Provider Contracts (user-service)

**Arquivos criados:**
```
user-service/
├── src/test/resources/contracts/
│   ├── auth/
│   │   ├── login-success.yml                      # POST /auth/login → 200
│   │   ├── login-invalid-credentials.yml           # POST /auth/login → 401
│   │   ├── get-user-me-success.yml                 # GET /auth/me → 200
│   │   └── get-user-me-unauthorized.yml            # GET /auth/me → 401
│   └── users/
│       ├── get-user-by-email-success.yml           # GET /users/by-email → 200
│       ├── get-user-by-email-not-found.yml         # GET /users/by-email → 404
│       ├── create-invited-user-success.yml         # POST /users/invited → 201
│       └── create-invited-user-duplicate.yml       # POST /users/invited → 409
├── src/test/java/com/scopeflow/user/contract/
│   ├── ContractVerifierBase.java                   # Base class com mocks
│   └── ContractVerifierSecurityConfig.java         # Security config para testes
├── src/main/java/com/scopeflow/user/config/
│   └── GlobalExceptionHandler.java                 # RFC 9457 error handling
└── pom.xml                                         # spring-cloud-contract-maven-plugin
```

**Validações implementadas:**
- ✅ JWT token format (regex `^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+$`)
- ✅ UUID format (regex `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`)
- ✅ Email format (regex)
- ✅ RFC 9457 Problem Details (`error_code`, `error_id`, `timestamp`, `status`)

### 2. Consumer Tests (monólito)

**Arquivos criados:**
```
backend/
├── src/test/java/com/scopeflow/contract/
│   └── UserServiceContractTest.java                # 10 testes de consumer
└── pom.xml                                         # spring-cloud-starter-contract-stub-runner
```

**Cenários de teste:**
1. ✅ Login com credenciais válidas → JWT válido
2. ✅ Login com credenciais inválidas → 401 + Problem Details
3. ✅ Get current user com JWT válido → UserResponse
4. ✅ Get current user sem JWT → 401
5. ✅ Get user by email (existente) → UserResponse
6. ✅ Get user by email (inexistente) → 404 + Problem Details
7. ✅ Criar usuário convidado → 201 + status=INACTIVE
8. ✅ Criar usuário convidado (email duplicado) → 409 + Problem Details
9. ✅ JWT do user-service aceito pelo monólito
10. ✅ Validação de formato de error responses (RFC 9457)

### 3. CI/CD Integration

**Arquivos criados:**
```
scripts/
└── validate-contracts.sh                           # Script de validação automatizada
```

**Fluxo CI:**
1. Valida `JWT_SECRET` match entre serviços
2. Roda provider tests (user-service)
3. Publica stubs JAR
4. Roda consumer tests (monólito)
5. ❌ Falha → bloqueia deploy
6. ✅ Passa → deploy permitido

### 4. Documentação

**Arquivos criados:**
```
docs/qa/contracts/
├── README.md                                       # Hub de navegação
└── CONTRACT-TESTING-GUIDE.md                       # Guia completo (8 páginas)

docs/qa/CONTRACT-TESTS-USER-SERVICE.md              # Provider docs
docs/qa/CONTRACT-TESTS-MONOLITH.md                 # Consumer docs
```

**Conteúdo da documentação:**
- Arquitetura e fluxo de contratos
- Como executar localmente
- CI/CD integration (GitHub Actions)
- Troubleshooting (6 cenários comuns)
- Schema evolution rules
- Roadmap (4 fases)

## Validações Críticas

### 1. JWT Compatibility (CRÍTICO)

**Problema:** Durante Strangler Fig, JWT gerado no user-service DEVE ser aceito pelo monólito.

**Solução:**
- Shared secret: `JWT_SECRET` idêntico em ambos os serviços
- Validado por: `validate-contracts.sh` + consumer test
- Teste específico: `jwtTokenFromUserService_shouldBeAcceptedByMonolith()`

### 2. RFC 9457 Problem Details (OBRIGATÓRIO)

**Validação:** Todos os erros seguem estrutura RFC 9457.

**Campos validados:**
```json
{
  "type": "https://api.scopeflow.com/errors/{error-type}",
  "title": "Human-Readable Title",
  "status": 401,
  "detail": "Specific error message",
  "error_code": "AUTH-401",
  "error_id": "uuid",
  "timestamp": "ISO-8601"
}
```

**Contratos de erro:** 4 (401, 404, 409 validados)

### 3. Response Schema Validation

**JWT claims validados:**
- `accessToken`: formato JWT
- `userId`: UUID
- `email`: formato email
- `expiresIn`: número positivo

**UserResponse validado:**
- `id`: UUID
- `email`: formato email
- `status`: enum (ACTIVE|INACTIVE)
- `createdAt`: ISO-8601 timestamp

## Como Executar

### Opção 1: Script Automatizado (Recomendado)

```bash
./scripts/validate-contracts.sh
```

**Output esperado:**
```
[1/4] Validating JWT secrets...
✅ JWT secrets match

[2/4] Running provider contract tests (user-service)...
✅ Provider tests passed

[3/4] Publishing stubs to local Maven repository...
✅ Stubs published: ~/.m2/repository/com/scopeflow/user-service/...

[4/4] Running consumer contract tests (monolith)...
✅ Consumer tests passed

================================
✅ All contract tests PASSED
================================

Summary:
  ✅ JWT secrets validated
  ✅ Provider tests passed (8 contracts)
  ✅ Stubs published to Maven local
  ✅ Consumer tests passed (10 scenarios)

Contracts are compatible! Safe to deploy.
```

### Opção 2: Manual

```bash
# Provider
cd user-service
./mvnw spring-cloud-contract:generateTests test -Dtest=ContractVerifier*
./mvnw clean install -DskipTests

# Consumer
cd backend
./mvnw test -Dtest=UserServiceContractTest
```

## Breaking Change Detection

### Exemplo Real

**Cenário:** user-service muda campo `accessToken` → `token`

**Resultado:**
1. ✅ Provider tests passam (contratos atualizados)
2. ❌ **Consumer tests FALHAM**
   ```
   java.lang.AssertionError: expected: accessToken but was: token
   ```
3. ❌ CI bloqueia deploy
4. 👨💍💻 Ação necessária:
   - Atualizar monólito para usar novo campo
   - OU reverter mudança
   - OU criar v2 do endpoint

## Métricas

| Métrica | Valor |
|---------|-------|
| **Contratos implementados** | 8 YAML files |
| **Provider tests gerados** | 8 tests (auto-generated) |
| **Consumer tests** | 10 scenarios |
| **Cobertura de endpoints** | 4 endpoints × 2 scenarios (happy + error) |
| **Validação de JWT** | ✅ Compatibilidade garantida |
| **RFC 9457 compliance** | ✅ 100% dos erros |
| **CI integration** | ✅ Script pronto |

## Próximos Passos

### Fase 1: Auth & User (✅ CONCLUÍDO)
- ✅ 8 contratos REST implementados
- ✅ Provider + consumer tests
- ✅ JWT validation
- ✅ CI script

### Fase 2: Workspace Extraction (⏳ PRÓXIMO)
- ⏳ Contratos de Workspace endpoints
- ⏳ Multi-tenancy validation (workspaceId em todos os requests)
- ⏳ Consumer tests no monólito

### Fase 3: Event-Driven Communication (⏳ FUTURO)
- ⏳ Kafka contracts (Pact CDC)
- ⏳ Event schema validation (Avro/JSON Schema)
- ⏳ Backward compatibility de eventos

### Fase 4: Pact Broker (⏳ QUANDO > 3 SERVIÇOS)
- ⏳ Migrar de Spring Cloud Contract para Pact
- ⏳ Pact Broker: UI visual + versionamento avançado
- ⏳ Can-I-Deploy matrix

## Troubleshooting

### Problema: Maven permission denied

```
java.nio.file.AccessDeniedException: /home/mq/.m2/repository/...
```

**Fix:**
```bash
# Remover diretório corrompido
rm -rf ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/3.2.0

# Re-rodar testes
./scripts/validate-contracts.sh
```

### Problema: Stubs JAR não encontrado

```
Stub not found: com.scopeflow:user-service:+:stubs
```

**Fix:**
```bash
cd user-service
./mvnw clean install -DskipTests
```

### Problema: JWT signature mismatch

```
io.jsonwebtoken.security.SignatureException: JWT signature does not match
```

**Fix:**
1. Verificar `JWT_SECRET` em `.env` ou `application.yml` de ambos os serviços
2. Garantir que são idênticos
3. Re-rodar `./scripts/validate-contracts.sh`

## Arquivos de Referência

| Arquivo | Propósito | Linhas |
|---------|-----------|--------|
| `user-service/src/test/resources/contracts/auth/login-success.yml` | Contrato de login bem-sucedido | 30 |
| `user-service/src/test/java/.../ContractVerifierBase.java` | Base class com mocks | 150 |
| `backend/src/test/java/.../UserServiceContractTest.java` | Consumer tests | 300 |
| `scripts/validate-contracts.sh` | CI validation script | 120 |
| `docs/qa/contracts/CONTRACT-TESTING-GUIDE.md` | Guia completo | 600 |

**Total:** ~1200 linhas de código + documentação

## Referências

- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Consumer-Driven Contracts](https://martinfowler.com/articles/consumerDrivenContracts.html)

---

**Status:** ✅ Implementação completa de contract tests para Auth & User domains.

**Validação:** Pronto para CI/CD integration.

**Próximo:** Fase 2 — Workspace extraction contracts.
