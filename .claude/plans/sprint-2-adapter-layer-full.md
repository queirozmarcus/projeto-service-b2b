# Sprint 2: Adapter Layer Implementation (Full)

**Data:** 2026-03-24
**Duração:** 2.5 semanas (18 dias)
**Status:** PLANEJAMENTO
**Orquestrador:** Marcus
**Agentes:** architect → backend-dev → dba → api-designer → code-reviewer → qa-engineer

---

## Objetivo Sprint

Implementar a **camada de adaptadores (adapters)** com foco em persistência (JPA), REST API e mapeamento bidirecional domain ↔ persistence.

**Escopo:** 3 bounded contexts (User, Briefing, Proposal)
1. **Persistence Layer** — JPA entities, Hibernate mappings, repositories concretos
2. **REST API Layer** — controllers, DTOs (requests/responses), error handling (Problem Details RFC 9457)
3. **Mapper Layer** — converters bidirecional (domain ↔ JPA ↔ DTO)

**Saída esperada:**
- 15+ JPA entities (mapeados para domain model)
- 12+ repository implementations (Spring Data JPA + custom queries)
- 25+ REST endpoints (/api/v1/users, /api/v1/briefings, /api/v1/proposals, etc.)
- 40+ request/response DTOs (records)
- 50+ unit tests (mapping logic, query validation)
- 40+ integration tests (full REST flow com DB real)
- OpenAPI 3.1 specification completa

---

## Fase 1: Design & Planning (Architect + API-Designer)

### 1.1 Bounded Context: User & Workspace (Persistence)

**JPA Entities:**
```
- UserJpaEntity (mapa para sealed User)
- WorkspaceJpaEntity
- WorkspaceMemberJpaEntity
- RoleEnumType (custom Hibernate type)
```

**Queries Esperadas:**
- `findByEmail(String email)` — login
- `findByWorkspaceId(UUID workspaceId)` — list members
- `findByStatusAndCreatedAtAfter(...)` — analytics
- `findActiveMembersByWorkspace(UUID workspaceId)` — audit

**REST Endpoints:**
- `POST /api/v1/auth/register` — RegisterRequest → UserResponse
- `POST /api/v1/auth/login` — LoginRequest → TokenResponse (JWT)
- `POST /api/v1/workspaces` — CreateWorkspaceRequest → WorkspaceResponse
- `GET /api/v1/workspaces/{id}` — WorkspaceResponse
- `POST /api/v1/workspaces/{id}/members/invite` — InviteMemberRequest → MemberResponse
- `PUT /api/v1/workspaces/{id}/members/{memberId}/role` — UpdateRoleRequest → MemberResponse
- `GET /api/v1/workspaces/{id}/members` — List<MemberResponse>

**DTOs (records):**
```
- record RegisterRequest(String email, String password, String fullName, String phone)
- record LoginRequest(String email, String password)
- record TokenResponse(String accessToken, String refreshToken, long expiresIn)
- record UserResponse(UUID id, String email, String fullName, String phone, String status)
- record CreateWorkspaceRequest(String name, String niche, String tone)
- record WorkspaceResponse(UUID id, String name, String niche, String tone, List<MemberResponse> members)
- record InviteMemberRequest(String email, Role role)
- record MemberResponse(UUID id, String email, String fullName, Role role, String status)
```

**Error Handling:**
- `EmailAlreadyRegisteredException` → 409 Conflict
- `UserNotFoundException` → 404 Not Found
- `InvalidPasswordException` → 401 Unauthorized
- `WorkspaceNotFoundException` → 404 Not Found
- `UnauthorizedException` → 403 Forbidden (RBAC check)

---

### 1.2 Bounded Context: Briefing (Persistence + REST)

**JPA Entities:**
```
- BriefingSessionJpaEntity (mapa para sealed BriefingSession)
- BriefingQuestionJpaEntity
- BriefingAnswerJpaEntity
- AIGenerationJpaEntity (audit trail)
```

**Queries:**
- `findByIdAndWorkspaceId(UUID id, UUID workspaceId)` — isolation
- `findByClientIdAndWorkspaceId(UUID clientId, UUID workspaceId)` — filter
- `findByStatus(BriefingStatus status)` — analytics
- Custom query: `findCompletedBriefingsByWorkspace(UUID workspaceId, Pageable)` — pagination

**REST Endpoints:**
- `POST /api/v1/briefings` — CreateBriefingSessionRequest → BriefingSessionResponse
- `GET /api/v1/briefings/{id}` — BriefingSessionResponse (private)
- `GET /api/v1/briefings/{id}/public?token={token}` — BriefingSessionPublicResponse (client)
- `GET /api/v1/briefings/{id}/questions` — List<QuestionResponse>
- `POST /api/v1/briefings/{id}/answers` — SubmitAnswerRequest → AnswerResponse
- `POST /api/v1/briefings/{id}/generate-followup` — GenerateFollowupRequest → QuestionResponse (IA)
- `POST /api/v1/briefings/{id}/complete` — CompleteBriefingRequest → BriefingSessionResponse

**DTOs:**
```
- record CreateBriefingSessionRequest(UUID clientId, UUID serviceId, String publicToken)
- record BriefingSessionResponse(UUID id, UUID clientId, String status, int progressPercentage, List<QuestionResponse> questions, Instant createdAt)
- record BriefingSessionPublicResponse(UUID id, String clientName, List<QuestionResponse> questions, int progressPercentage)
- record QuestionResponse(UUID id, int stepNumber, String text, String type, String aiPromptVersion)
- record SubmitAnswerRequest(UUID questionId, String answer, Map<String, Object> metadata)
- record AnswerResponse(UUID id, UUID questionId, String answer, String status, Instant submittedAt)
- record GenerateFollowupRequest(UUID answerId, String rationale)
- record CompleteBriefingRequest(Map<String, Object> consolidatedData)
```

**Error Handling:**
- `BriefingNotFoundException` → 404
- `BriefingCompletedException` → 409 (no more answers allowed)
- `InvalidAnswerException` → 422 Unprocessable Entity
- `AIGenerationFailedException` → 502 Bad Gateway (with retry header)

---

### 1.3 Bounded Context: Proposal (Persistence + REST)

**JPA Entities:**
```
- ProposalJpaEntity (mapa para sealed Proposal)
- ProposalVersionJpaEntity (immutable)
- ProposalScopeJpaEntity
- ApprovalWorkflowJpaEntity
- ApprovalJpaEntity (audit)
```

**Queries:**
- `findByIdAndWorkspaceId(UUID id, UUID workspaceId)` — isolation
- `findByStatusAndWorkspaceId(ProposalStatus status, UUID workspaceId)` — filter
- `findApprovedProposalsByClient(UUID clientId, Pageable)` — history
- Custom: `findProposalsNeedingApprovalByWorkspace(UUID workspaceId)` — dashboard

**REST Endpoints:**
- `POST /api/v1/proposals` — CreateProposalRequest → ProposalResponse
- `GET /api/v1/proposals/{id}` — ProposalResponse (private)
- `GET /api/v1/proposals/{id}/versions` — List<VersionResponse> (history)
- `POST /api/v1/proposals/{id}/publish` — PublishProposalRequest → ProposalResponse
- `POST /api/v1/proposals/{id}/update-scope` — UpdateScopeRequest → ProposalResponse
- `POST /api/v1/proposals/{id}/initiate-approval` — InitiateApprovalRequest → ApprovalWorkflowResponse
- `GET /api/v1/proposals/{id}/approve?token={token}` — ApprovalPageResponse (public)
- `POST /api/v1/proposals/{id}/approve` — ApproveProposalRequest → ApprovalResponse
- `POST /api/v1/proposals/{id}/generate-kickoff` — GenerateKickoffRequest → KickoffResponse

**DTOs:**
```
- record CreateProposalRequest(UUID clientId, UUID briefingId, String proposalName)
- record ProposalResponse(UUID id, UUID clientId, String status, ScopeResponse scope, List<ApprovalResponse> approvals, Instant createdAt)
- record VersionResponse(UUID versionId, ScopeResponse scope, Instant createdAt, String createdBy)
- record ScopeResponse(List<DeliverableResponse> deliverables, List<String> exclusions, List<String> assumptions, PriceResponse price, TimelineResponse timeline)
- record DeliverableResponse(String name, String description, String acceptanceCriteria)
- record PriceResponse(BigDecimal amount, String currency, String breakdown)
- record TimelineResponse(LocalDate startDate, LocalDate endDate, List<MilestoneResponse> milestones)
- record MilestoneResponse(String name, LocalDate dueDate, String description)
- record ApprovalWorkflowResponse(UUID id, String status, List<ApprovalResponse> approvals, Instant initiatedAt)
- record ApprovalResponse(UUID id, String approverName, String approverEmail, String status, String ipAddress, Instant approvedAt)
- record KickoffResponse(UUID id, String summary, List<MilestoneResponse> milestones, String nextSteps)
```

**Error Handling:**
- `ProposalNotFoundException` → 404
- `ProposalCannotBePublishedException` → 409 (wrong status)
- `ApprovalTokenExpiredException` → 401 (time-based)
- `InvalidScopeException` → 422 (missing deliverables)

---

### 1.4 API-Level Cross-Cutting Concerns

**Global Error Handler (ControllerAdvice):**
```
- 400 Bad Request (validation)
- 401 Unauthorized (no token/invalid)
- 403 Forbidden (RBAC check failed)
- 404 Not Found (resource missing)
- 409 Conflict (business rule violation)
- 422 Unprocessable Entity (semantic error)
- 502 Bad Gateway (IA API failure)
- 500 Internal Server Error (unexpected)
```

**All responses follow RFC 9457 (Problem Details):**
```json
{
  "type": "https://scopeflow.com/errors/email-already-registered",
  "title": "Email Already Registered",
  "status": 409,
  "detail": "Email user@example.com is already in use",
  "instance": "/api/v1/auth/register",
  "timestamp": "2026-03-24T10:30:45Z"
}
```

**Request/Response Validation:**
- `@Valid` on DTOs
- `@NotNull`, `@Email`, `@Min`, `@Max` constraints
- Custom validators (e.g., `@ValidEmail`, `@ValidWorkspaceName`)

**Security Headers:**
- `Spring Security` global CORS policy
- `Content-Security-Policy` (if serving HTML)
- `X-Content-Type-Options: nosniff`

---

## Fase 2: Implementation (Backend-Dev + DBA)

### 2.1 Directory Structure (Adapter Layer)

```
backend/src/main/java/com/scopeflow/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   ├── AuthController.java
│   │   │   ├── WorkspaceController.java
│   │   │   ├── BriefingController.java
│   │   │   ├── ProposalController.java
│   │   │   └── GlobalExceptionHandler.java (ControllerAdvice)
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── RegisterRequest.java (record)
│   │       │   ├── LoginRequest.java
│   │       │   ├── CreateWorkspaceRequest.java
│   │       │   ├── CreateBriefingSessionRequest.java
│   │       │   └── ... (25+ total)
│   │       └── response/
│   │           ├── UserResponse.java (record)
│   │           ├── WorkspaceResponse.java
│   │           ├── BriefingSessionResponse.java
│   │           └── ... (25+ total)
│   └── out/
│       ├── persistence/
│       │   ├── entity/
│       │   │   ├── UserJpaEntity.java
│       │   │   ├── WorkspaceJpaEntity.java
│       │   │   ├── BriefingSessionJpaEntity.java
│       │   │   ├── ProposalJpaEntity.java
│       │   │   └── ... (15+ total)
│       │   ├── repository/
│       │   │   ├── UserJpaRepository.java (extends JpaRepository)
│       │   │   ├── UserRepositoryAdapter.java (implements domain UserRepository)
│       │   │   ├── BriefingSessionJpaRepository.java
│       │   │   ├── BriefingSessionRepositoryAdapter.java
│       │   │   └── ... (12 adapters total)
│       │   └── mapper/
│       │       ├── UserMapper.java (domain ↔ entity)
│       │       ├── BriefingSessionMapper.java
│       │       ├── ProposalMapper.java
│       │       └── ... (3+ mappers)
│       └── rest/
│           ├── mapper/
│           │   ├── UserDtoMapper.java (dto ↔ domain)
│           │   ├── BriefingDtoMapper.java
│           │   ├── ProposalDtoMapper.java
│           │   └── ... (3+ mappers)
│           └── exception/
│               ├── EmailAlreadyRegisteredException.java (domain exception)
│               ├── ProblemDetailsException.java (REST wrapper)
│               └── ... (exception hierarchy)
└── config/
    ├── SecurityConfig.java (Spring Security JWT)
    ├── CorsConfig.java
    ├── JpaConfig.java (custom hibernte types)
    └── OpenApiConfig.java (SpringDoc OpenAPI 3.1)
```

### 2.2 JPA Entity Example: UserJpaEntity

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_workspace_id", columnList = "workspace_id")
})
@Data
@NoArgsConstructor
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;  // Optimistic locking

    // Relationships (lazy-loaded)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<WorkspaceMemberJpaEntity> workspaceMembers;

    // Mapper: JpaEntity → Domain
    public UserActive toDomain() {
        return new UserActive(
            new UserId(id),
            new Email(email),
            new PasswordHash(passwordHash),
            fullName,
            phone,
            createdAt,
            updatedAt
        );
    }

    // Mapper: Domain → JpaEntity (static factory)
    public static UserJpaEntity fromDomain(UserActive user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getId().value();
        entity.email = user.getEmail().normalized();
        entity.passwordHash = user.getPasswordHash().value();
        entity.fullName = user.getFullName();
        entity.phone = user.getPhone();
        entity.status = UserStatus.valueOf(user.status().toUpperCase());
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }
}
```

### 2.3 Repository Adapter (Domain ↔ Persistence)

```java
@Repository
@Transactional
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public UserActive save(UserActive user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<UserActive> findById(UserId userId) {
        return jpaRepository.findById(userId.value())
            .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<UserActive> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.normalized())
            .map(UserJpaEntity::toDomain);
    }

    @Override
    public List<UserActive> findByStatus(String status) {
        return jpaRepository.findByStatus(UserStatus.valueOf(status))
            .stream()
            .map(UserJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UserId userId) {
        jpaRepository.deleteById(userId.value());
    }
}
```

### 2.4 REST Controller Example: AuthController

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserActive user = userService.registerUser(
            new Email(request.email()),
            request.password(),
            request.fullName(),
            request.phone()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userDtoMapper.toUserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        UserActive user = userService.authenticateUser(
            new Email(request.email()),
            request.password()
        );

        String accessToken = jwtTokenProvider.generateToken(user.getId().value(), 15 * 60);  // 15 min
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().value(), 7 * 24 * 3600);  // 7 days

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, 15 * 60));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        UUID userId = jwtTokenProvider.validateRefreshToken(request.refreshToken());
        String newAccessToken = jwtTokenProvider.generateToken(userId, 15 * 60);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, request.refreshToken(), 15 * 60));
    }
}
```

### 2.5 Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException e, HttpServletRequest request) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            e.getMessage()
        );
        detail.setTitle("Email Already Registered");
        detail.setType(URI.create("https://scopeflow.com/errors/email-already-registered"));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            UserNotFoundException e, HttpServletRequest request) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "User not found"
        );
        detail.setTitle("User Not Found");
        detail.setType(URI.create("https://scopeflow.com/errors/user-not-found"));
        detail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationError(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        detail.setTitle("Validation Error");
        detail.setType(URI.create("https://scopeflow.com/errors/validation-error"));
        detail.setInstance(URI.create(request.getRequestURI()));

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        detail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }
}
```

---

## Fase 3: Testing (QA-Engineer)

### 3.1 Unit Tests: Repository Adapter

```java
@DisplayName("UserRepositoryAdapter Tests")
class UserRepositoryAdapterTest {

    private UserRepositoryAdapter adapter;
    private UserJpaRepository jpaRepository;

    @BeforeEach
    void setup() {
        jpaRepository = mock(UserJpaRepository.class);
        adapter = new UserRepositoryAdapter(jpaRepository, new UserMapper());
    }

    @Test
    void shouldSaveUserAndReturnDomain() {
        // Given
        UserActive user = User.create(
            UserId.generate(),
            new Email("user@example.com"),
            PasswordHash.fromPlaintext("password"),
            "John Doe",
            "+5511999999999"
        );

        UserJpaEntity jpaEntity = UserJpaEntity.fromDomain(user);
        when(jpaRepository.save(any())).thenReturn(jpaEntity);

        // When
        UserActive saved = adapter.save(user);

        // Then
        assertThat(saved.getId()).isEqualTo(user.getId());
        assertThat(saved.getEmail()).isEqualTo(user.getEmail());
        verify(jpaRepository).save(any(UserJpaEntity.class));
    }

    @Test
    void shouldFindByEmailNormalized() {
        // Given
        Email email = new Email("USER@EXAMPLE.COM");
        UserJpaEntity jpaEntity = createUserJpaEntity(email.normalized());
        when(jpaRepository.findByEmail("user@example.com"))
            .thenReturn(Optional.of(jpaEntity));

        // When
        Optional<UserActive> found = adapter.findByEmail(email);

        // Then
        assertThat(found).isPresent();
        verify(jpaRepository).findByEmail("user@example.com");
    }
}
```

### 3.2 Integration Tests: REST Controller (Testcontainers)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser@example.com",
            "SecurePassword123",
            "John Doe",
            "+5511999999999"
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.fullName").value("John Doe"))
            .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    void shouldReturnConflictOnDuplicateEmail() throws Exception {
        // Given: user already exists
        RegisterRequest existingRequest = new RegisterRequest(
            "existing@example.com",
            "Password123",
            "Jane Doe",
            "+5511999999998"
        );
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(existingRequest)))
            .andExpect(status().isCreated());

        // When: try to register same email
        RegisterRequest duplicateRequest = new RegisterRequest(
            "existing@example.com",
            "AnotherPassword",
            "John Smith",
            "+5511999999997"
        );

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Email Already Registered"))
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Given: user registered
        RegisterRequest registerRequest = new RegisterRequest(
            "login@example.com",
            "SecurePassword123",
            "Test User",
            "+5511999999990"
        );
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // When: login with correct credentials
        LoginRequest loginRequest = new LoginRequest("login@example.com", "SecurePassword123");

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.expiresIn").value(900));  // 15 * 60
    }

    @Test
    void shouldReturnUnauthorizedOnWrongPassword() throws Exception {
        // Given: user registered
        RegisterRequest registerRequest = new RegisterRequest(
            "auth@example.com",
            "CorrectPassword",
            "Test User",
            "+5511999999989"
        );
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // When: login with wrong password
        LoginRequest loginRequest = new LoginRequest("auth@example.com", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Invalid Credentials"));
    }
}
```

---

## Entregáveis Sprint 2

### Código
- [ ] 15+ JPA entities (com mappers para domain)
- [ ] 12+ repository adapters (implementam domain repositories)
- [ ] 25+ REST endpoints (auth, workspace, briefing, proposal)
- [ ] 50+ DTOs (request/response records)
- [ ] Global exception handler (RFC 9457 Problem Details)
- [ ] JWT token provider (Spring Security)
- [ ] OpenAPI 3.1 Spec (auto-generated by SpringDoc)

### Testes
- [ ] 50+ unit tests (mapping, adapter logic)
- [ ] 40+ integration tests (REST + DB, Testcontainers)
- [ ] 80%+ coverage (adapters layer)
- [ ] Contract tests com frontend (Pact ou Spring Cloud Contract)

### Documentação
- [ ] API specification (OpenAPI 3.1 YAML)
- [ ] Entity-DTO mapping guide
- [ ] JWT flow documentation
- [ ] Error handling catalog (all Problem Detail types)

### Commits
- [ ] 1 commit per context (user, briefing, proposal)
- [ ] Conventional Commits in PT-BR
- [ ] Linked to Sprint 2 issue

---

## Roadmap Pós-Sprint 2

**Sprint 3:** Application Services (use cases + orchestration)
**Sprint 4:** Message Queue (RabbitMQ consumers for async tasks)
**Sprint 5:** Frontend Authentication Flow
**Sprint 6:** Briefing Discovery UI (end-to-end)

---

## Status

- ✅ Branch: `feature/sprint-2-adapter-layer` criado
- ✅ Plano estruturado (3 contextos, 4 fases)
- ✅ **APROVADO** em 2026-03-24 10:35 UTC
- 🚀 **EXECUÇÃO INICIADA** — delegando ao architect (Fase 1)
