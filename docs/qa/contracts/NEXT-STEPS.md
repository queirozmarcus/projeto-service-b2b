# Contract Tests — Próximos Passos

**Status:** 25 contratos criados | 20 consumer tests implementados | Provider base class pendente

---

## 1. Criar Provider Base Class (user-service)

**Arquivo:** `user-service/src/test/java/com/scopeflow/user/contract/UserContractBase.java`

```java
package com.scopeflow.user.contract;

import com.scopeflow.user.adapter.out.persistence.JpaUserRepository;
import com.scopeflow.user.domain.Email;
import com.scopeflow.user.domain.PasswordHash;
import com.scopeflow.user.domain.User;
import com.scopeflow.user.domain.UserId;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public abstract class UserContractBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scopeflow_users_test")
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

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JpaUserRepository userRepository;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);

        // Seed test data expected by contracts
        // test@example.com — usado em login-success, get-user-me, etc.
        var testUser = User.create(
                UserId.generate(),
                new Email("test@example.com"),
                PasswordHash.fromPlaintext("ValidPassword123!"),
                "Test User",
                null,
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000") // workspace-A
        );
        userRepository.save(testUser);

        // Admin user para block-user tests
        var adminUser = User.create(
                UserId.of(UUID.fromString("660e8400-e29b-41d4-a716-446655440001")),
                new Email("admin@example.com"),
                PasswordHash.fromPlaintext("AdminPassword123!"),
                "Admin User",
                null,
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        );
        adminUser.promoteToAdmin();
        userRepository.save(adminUser);
    }
}
```

**Validação:**
```bash
cd user-service
./mvnw clean test  # Deve gerar stubs em target/stubs/
```

---

## 2. Configurar Maven Plugin (user-service)

**Arquivo:** `user-service/pom.xml`

Adicionar/verificar:

```xml
<plugin>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-contract-maven-plugin</artifactId>
    <version>${spring-cloud-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <baseClassForTests>com.scopeflow.user.contract.UserContractBase</baseClassForTests>
        <contractsDirectory>src/test/resources/contracts</contractsDirectory>
    </configuration>
</plugin>
```

---

## 3. Rodar Testes Localmente

### Provider (user-service)

```bash
cd user-service
./mvnw clean test              # Gera stubs
./mvnw install                 # Publica stubs localmente (~/.m2)
```

**Esperado:**
- 25 testes gerados automaticamente de contratos
- Stubs publicados em `~/.m2/repository/com/scopeflow/user-service/`

### Consumer (monólito)

```bash
cd backend
./mvnw test -Dtest=UserServiceContractTest
```

**Esperado:**
- 20 testes passando
- WireMock carrega stubs de `~/.m2` (porta 8090)

---

## 4. Adicionar ao CI Pipeline

### user-service (GitHub Actions / Bitbucket Pipelines)

```yaml
- name: Generate and publish stubs
  run: |
    cd user-service
    ./mvnw clean install -DskipTests=false
    # Stubs publicados em artifact registry ou ~/.m2 compartilhado
```

### backend (monólito)

```yaml
- name: Run contract tests
  run: |
    cd backend
    ./mvnw test -Dtest=UserServiceContractTest
```

**Dependência:** Consumer pipeline roda **depois** do provider ter publicado stubs.

---

## 5. Adicionar /block Endpoint no User Service

**Status:** Contratos criados, mas endpoint **não existe** no user-service.

**Arquivo:** `user-service/src/main/java/com/scopeflow/user/adapter/in/web/UserController.java`

```java
@PostMapping("/{id}/block")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> blockUser(@PathVariable UUID id) {
    blockUserUseCase.execute(UserId.of(id));
    return ResponseEntity.noContent().build();
}
```

**Use case:** `user-service/src/main/java/com/scopeflow/user/application/BlockUserUseCase.java`

```java
@Service
public class BlockUserUseCase {

    private final UserRepository userRepository;

    public BlockUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.block();  // Domain method: status = INACTIVE
        userRepository.save(user);
    }
}
```

**Domain method:** `user-service/src/main/java/com/scopeflow/user/domain/User.java`

```java
public void block() {
    if (this.status == Status.INACTIVE) {
        return; // Idempotent
    }
    this.status = Status.INACTIVE;
}
```

---

## 6. Implementar Workspace Validation

**Contrato:** `create-invited-user-wrong-workspace.yml` (403)

**Validação necessária em:** `user-service/src/main/java/com/scopeflow/user/application/InviteUserUseCase.java`

```java
public UserResponse execute(CreateInvitedUserRequest request, UUID currentWorkspaceId) {
    // 1. Buscar inviter
    User inviter = userRepository.findById(UserId.of(request.invitedByUserId()))
            .orElseThrow(() -> new InvalidInvitedByUserException(request.invitedByUserId()));

    // 2. Validar workspace
    if (!inviter.getWorkspaceId().equals(currentWorkspaceId)) {
        throw new ForbiddenException("Cannot invite user to a different workspace");
    }

    // 3. Resto da lógica...
}
```

**JWT claim:** Adicionar `workspaceId` ao JWT no `JwtService`:

```java
public String generateToken(User user) {
    return Jwts.builder()
            .setSubject(user.getId().value().toString())
            .claim("email", user.getEmail().value())
            .claim("workspaceId", user.getWorkspaceId().toString())  // NOVO
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
}
```

---

## 7. Validação Final

```bash
# Stack completa
./scripts/validate-qa-full.sh --with-stack

# Health check
./scripts/check-stack-health.sh

# Contract tests isolados
cd backend && ./mvnw test -Dtest=UserServiceContractTest
cd user-service && ./mvnw test
```

**Esperado:**
- 25 contratos provider: ✅ PASS
- 20 consumer tests: ✅ PASS
- Stubs publicados em `~/.m2`

---

## Checklist de Conclusão

- [ ] `UserContractBase.java` criado e configurado
- [ ] Plugin Maven configurado em `user-service/pom.xml`
- [ ] Endpoint `/users/{id}/block` implementado
- [ ] Workspace validation implementada em `InviteUserUseCase`
- [ ] JWT claim `workspaceId` adicionado
- [ ] Pipeline CI atualizado (provider → consumer)
- [ ] Testes rodando em verde localmente
- [ ] Documentação atualizada no README principal

---

## Tempo Estimado

| Tarefa | Tempo |
|--------|-------|
| Provider base class + Maven config | 30min |
| Implementar /block endpoint | 1h |
| Workspace validation + JWT claim | 1h |
| CI pipeline update | 30min |
| Testes e validação final | 1h |
| **TOTAL** | **4h** |

---

## Referências

- **Spring Cloud Contract Guide:** https://spring.io/guides/gs/contract-rest/
- **Provider base class example:** https://cloud.spring.io/spring-cloud-contract/reference/html/project-features.html#contract-dsl-rest
- **Resilience4j Circuit Breaker:** https://resilience4j.readme.io/docs/circuitbreaker
