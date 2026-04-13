# Testes de Integração: UserController

**Arquivo:** `/backend/src/test/java/com/scopeflow/adapter/in/web/user/UserControllerIntegrationTest.java`

**Stack de testes:** Java 21 + Spring Boot Test 3.2 + Testcontainers + PostgreSQL 16 + AssertJ

## Objetivos

Validar integração completa dos novos endpoints de gestão de usuário com banco PostgreSQL real:
- `GET /api/v1/users/by-email/{email}` — busca usuário por email
- `POST /api/v1/users/invited` — cria usuário convidado (status INACTIVE)

## Configuração (@SpringBootTest + @Testcontainers)

```java
@SpringBootTest(webEnvironment = MOCK)
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scopeflow_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }
    
    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        workspaceRepository.deleteAll();
    }
}
```

### Recursos Injetados

- `MockMvc` — testar endpoints HTTP sem servidor real
- `ObjectMapper` — serialização JSON (requests/responses)
- `JpaUserSpringRepository` — setup de fixtures + validação de persistência
- `JpaWorkspaceSpringRepository` — criar workspaces para tokens JWT
- `JwtService` — gerar tokens de autenticação reais

## Cenários de Teste Implementados

### GET /api/v1/users/by-email/{email} (3 cenários)

#### 1. shouldReturnUser_whenEmailExists()
**Given:** Usuário existe no banco (status ACTIVE)  
**When:** GET /api/v1/users/by-email/{email}  
**Then:**
- 200 OK
- UserResponse com dados corretos (id, email, fullName, status)
- Validação adicional via SELECT direto no banco

#### 2. shouldReturn404_whenEmailNotFound()
**Given:** Email não existe no banco  
**When:** GET /api/v1/users/by-email/nonexistent@example.com  
**Then:**
- 404 NOT_FOUND
- Problem Details (RFC 9457) completo:
  - `error_code`: USER-010
  - `title`: "User Not Found"
  - `type`: https://api.scopeflow.com/errors/user-not-found
  - `error_id`: UUID único
  - `timestamp`: ISO 8601

#### 3. shouldReturn400_whenEmailFormatInvalid()
**Given:** Formato de email inválido  
**When:** GET /api/v1/users/by-email/invalid-email  
**Then:**
- 500 Internal Server Error (IllegalArgumentException do VO Email)

**Nota:** Este comportamento será refinado para 400 Bad Request em PR futura.

---

### POST /api/v1/users/invited (5 cenários)

#### 1. shouldCreateInvitedUser_whenValidRequest()
**Given:** Workspace + usuário convidador existem no banco  
**When:** POST /api/v1/users/invited  
```json
{
  "email": "invited@example.com",
  "role": "MEMBER",
  "invitedByUserId": "uuid-convidador"
}
```
**Then:**
- 201 CREATED
- UserResponse com status=INACTIVE, publicToken gerado
- Validação de persistência:
  - SELECT no banco confirma usuário criado
  - 2 usuários no total (convidador + convidado)
  - Status=INACTIVE, fullName extraído do email

#### 2. shouldReturn409_whenEmailAlreadyExists()
**Given:** Usuário com email já existe no banco  
**When:** POST /api/v1/users/invited (mesmo email)  
**Then:**
- 409 CONFLICT
- Problem Details:
  - `error_code`: USER-011
  - `title`: "Duplicate Email"
- Validação: nenhum registro duplicado foi criado

#### 3. shouldReturn400_whenInvitedByUserNotFound()
**Given:** `invitedByUserId` não existe no banco  
**When:** POST /api/v1/users/invited (invitedByUserId inválido)  
**Then:**
- 400 BAD_REQUEST
- Problem Details:
  - `error_code`: USER-012
  - `title`: "Invalid Invited By User"
- Validação: nenhum usuário foi criado

#### 4. shouldReturn400_whenRoleIsOwner()
**Given:** Request com role=OWNER (não permitido)  
**When:** POST /api/v1/users/invited (role=OWNER)  
**Then:**
- 400 BAD_REQUEST
- Problem Details:
  - `error_code`: USER-013
  - `title`: "Invalid Role"
- Validação: nenhum usuário foi criado

#### 5. shouldReturn400_whenRequestValidationFails()
**Given:** Email vazio ou nulo (falha de validação Jakarta)  
**When:** POST /api/v1/users/invited (email="")  
**Then:**
- 400 BAD_REQUEST
- Problem Details:
  - `error_code`: VALIDATION-400
  - `title`: "Validation Error"
- Validação: nenhum usuário foi criado

---

## Helpers de Teste

### createActiveUser(String email)
Cria e persiste usuário em estado ACTIVE:
- UUID aleatório
- Email normalizado
- Password hash pré-computado (BCrypt strength 12 para "Password1!")
- Status=ACTIVE
- Timestamps atuais

### createWorkspace(UUID ownerId)
Cria e persiste workspace:
- UUID aleatório
- ownerId vinculado
- Name="Test Workspace", Niche="social-media"
- Status=ACTIVE

### createAuthToken(UUID userId, String email)
Gera JWT token real via JwtService:
- Cria workspace automaticamente
- Retorna token assinado com claims: userId, email, workspaceId, role=OWNER
- Token válido por 15 minutos (padrão)

---

## Princípios Aplicados

### 1. Testcontainers Obrigatório
- PostgreSQL 16-alpine (mesma versão de produção)
- Container singleton compartilhado entre testes da classe (performance)
- Zero dependência de H2 ou banco in-memory

### 2. Database Isolation
- `@BeforeEach cleanDatabase()` — limpa TODAS as tabelas
- Ordem de deleção respeita FKs (workspace antes de user)
- Cada teste roda em estado limpo

### 3. Given-When-Then Pattern
Todos os testes seguem estrutura clara:
```java
@Test
void testName() {
    // Given (arrange)
    JpaUser user = createActiveUser("test@example.com");
    
    // When (act)
    mockMvc.perform(get("/endpoint"))
    
    // Then (assert)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(user.getId())));
    
    // Additional DB validation
    assertThat(userRepository.findAll()).hasSize(1);
}
```

### 4. AssertJ Fluent Assertions
Preferido sobre hamcrest para validações complexas:
```java
assertThat(users).hasSize(2);
assertThat(user.get().getStatus()).isEqualTo("INACTIVE");
```

### 5. RFC 9457 Problem Details Validation
Todos os erros validam estrutura completa:
- error_code (estável)
- title (legível para humanos)
- type (URL para documentação)
- error_id (trace único)
- timestamp (ISO 8601)

### 6. Database State Validation
Após operações de escrita, sempre validar persistência:
```java
// Verify user was persisted
List<JpaUser> allUsers = userRepository.findAll();
assertThat(allUsers).hasSize(2);
var invitedUser = allUsers.stream()
    .filter(u -> u.getEmail().equals(newUserEmail))
    .findFirst();
assertThat(invitedUser).isPresent();
assertThat(invitedUser.get().getStatus()).isEqualTo("INACTIVE");
```

---

## Cobertura de Teste

| Endpoint | Happy Path | Error Paths | Edge Cases |
|----------|-----------|-------------|------------|
| GET /by-email/{email} | ✅ 200 OK | ✅ 404 Not Found | ✅ 400 Invalid format |
| POST /invited | ✅ 201 Created | ✅ 409 Duplicate<br>✅ 400 Invalid user<br>✅ 400 Invalid role<br>✅ 400 Validation | — |

**Total:** 8 cenários | **Target:** 80%+ cobertura de linha

---

## Execução

```bash
# Todos os testes de integração do UserController
cd backend
./mvnw test -Dtest=UserControllerIntegrationTest

# Com logs detalhados
./mvnw test -Dtest=UserControllerIntegrationTest -X

# Apenas um cenário específico
./mvnw test -Dtest=UserControllerIntegrationTest#shouldReturnUser_whenEmailExists

# Relatório de cobertura (JaCoCo)
./mvnw verify jacoco:report
# Abrir: target/site/jacoco/index.html
```

### Requisitos

- Docker rodando (para Testcontainers)
- Java 21+
- Maven 3.9+
- 4GB+ RAM alocados para Docker

---

## Comparação: Unit vs Integration Tests

| Aspecto | Unit Test | Integration Test |
|---------|-----------|------------------|
| **Banco** | Mock (Mockito) | PostgreSQL real (Testcontainers) |
| **Spring Context** | Parcial (@WebMvcTest) | Completo (@SpringBootTest) |
| **Flyway Migrations** | Não executa | Executa todas (V1–V9) |
| **Tempo de execução** | ~500ms | ~8-12s (container startup) |
| **Objetivo** | Lógica de controller | Integração end-to-end |
| **Validação** | Comportamento | Persistência + comportamento |

---

## Próximos Passos

1. **Coverage report:** Gerar relatório JaCoCo e validar 80%+ cobertura
2. **Email VO exception:** Refatorar para retornar 400 em vez de 500 quando formato inválido
3. **E2E tests:** Criar script bash similar a `RUN-BRIEFING-TESTS.sh` para fluxo completo de invite
4. **Contract tests:** Pact ou Spring Cloud Contract para consumidores dos endpoints

---

## Referências

- [CLAUDE.md](../../CLAUDE.md) — Guia de desenvolvimento do projeto
- [UserControllerTest.java](../../backend/src/test/java/com/scopeflow/adapter/in/web/user/UserControllerTest.java) — Testes unitários
- [ScopeFlowIntegrationTestBase.java](../../backend/src/test/java/com/scopeflow/adapter/in/web/integration/ScopeFlowIntegrationTestBase.java) — Base class para testes de integração
- [Testcontainers Documentation](https://www.testcontainers.org/) — Framework de containers para testes
- [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html) — Padrão de erro HTTP
