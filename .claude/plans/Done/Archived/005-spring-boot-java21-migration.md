# Plano: Migração para Spring Boot 3.2 + Java 21

**Data:** 2026-03-22
**Status:** MIGRAÇÃO DE STACK APROVADA
**Impacto:** Backend refatorado, frontend sem mudança, timeline +1-2 sprints

---

## 1. Por que Spring Boot 3.2 + Java 21?

### Features Java 21 que usamos
| Feature | Uso | Benefício |
|---------|-----|----------|
| **Virtual Threads** | Async I/O (IA, S3, DB) | 1000+ concurrent sem thread pool limit |
| **Sealed Classes** | Domain entities (Proposal, BriefingSession) | Type safety: só subclasses permitidas |
| **Records** | DTOs (BriefingQuestionRequest, ProposalResponse) | Imutáveis sem Lombok boilerplate |
| **Pattern Matching** | Switch melhorado (generationType, proposalStatus) | Code mais legível |
| **Structured Concurrency** | Parallel tasks com timeout | Async simples sem callback hell |
| **Java 21 LTS** | Suporte até 2031 | Estável pra produção, 8 anos de suporte |

### Spring Boot 3.2 vs NestJS

| Aspecto | Spring Boot 3.2 | NestJS |
|---------|-----------------|--------|
| **Virtual Threads** | ✅ Nativo (Java 21) | ❌ Node.js single-threaded |
| **Performance Async** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Type Safety** | ⭐⭐⭐⭐⭐ (sealed, records) | ⭐⭐⭐⭐ (TypeScript) |
| **Enterprise Features** | ⭐⭐⭐⭐⭐ (Spring Security, etc) | ⭐⭐⭐ (DIY) |
| **Development Speed** | ⭐⭐⭐⭐ (mature ecosystem) | ⭐⭐⭐⭐⭐ (quick start) |
| **Memory Footprint** | ⭐⭐⭐ (~300MB) | ⭐⭐⭐⭐ (~150MB Node.js) |
| **Startup Time** | ⭐⭐⭐ (~5s) | ⭐⭐⭐⭐⭐ (<1s) |
| **Scaling (vert)** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 2. Arquitetura Spring Boot 3.2 + Java 21

### pom.xml (Parent)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.scopeflow</groupId>
  <artifactId>scopeflow</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <name>ScopeFlow API</name>
  <description>AI-powered briefing and scope alignment SaaS</description>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
  </parent>

  <properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <!-- Spring Boot Web -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security 6.x -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.5</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.5</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.5</version>
      <scope>runtime</scope>
    </dependency>

    <!-- Data JPA + Hibernate 6.x -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.7.2</version>
      <scope>runtime</scope>
    </dependency>

    <!-- Flyway (Database Migrations) -->
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-postgresql</artifactId>
    </dependency>

    <!-- Spring Integration (for async queues) -->
    <dependency>
      <groupId>org.springframework.integration</groupId>
      <artifactId>spring-integration-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.integration</groupId>
      <artifactId>spring-integration-amqp</artifactId>
    </dependency>

    <!-- RabbitMQ -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- AWS SDK for Java (S3) -->
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>s3</artifactId>
      <version>2.23.0</version>
    </dependency>

    <!-- OpenAI SDK for Java -->
    <dependency>
      <groupId>com.theokanning.openai-gpt3-java</groupId>
      <artifactId>service</artifactId>
      <version>0.18.0</version>
    </dependency>

    <!-- PDF Generation: iText -->
    <dependency>
      <groupId>com.itextpdf</groupId>
      <artifactId>itext8-core</artifactId>
      <version>8.0.1</version>
    </dependency>

    <!-- Logging: Logback + SLF4J (included in spring-boot-starter) -->

    <!-- Jackson (JSON processing) -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.datatype</groupId>
      <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>

    <!-- TestContainers (Integration Testing) -->
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers</artifactId>
      <version>1.19.6</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <version>1.19.6</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>rabbitmq</artifactId>
      <version>1.19.6</version>
      <scope>test</scope>
    </dependency>

    <!-- AssertJ (Better assertions) -->
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>

      <!-- Checkstyle for code quality -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.3.1</version>
        <configuration>
          <configLocation>checkstyle.xml</configLocation>
        </configuration>
      </plugin>

      <!-- JaCoCo for code coverage -->
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.11</version>
        <executions>
          <execution>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <!-- GraalVM Native Image (optional) -->
      <plugin>
        <groupId>org.graalvm.buildtools</groupId>
        <artifactId>native-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 3. Entidades Java 21 com Records & Sealed Classes

### Domain Entity: Sealed Proposal

```java
// com/scopeflow/core/domain/proposal/Proposal.java

public sealed class Proposal {
  private final String id;
  private final String clientId;
  private final String serviceId;
  private final UUID workspaceId;

  protected Proposal(String id, String clientId, String serviceId, UUID workspaceId) {
    this.id = id;
    this.clientId = clientId;
    this.serviceId = serviceId;
    this.workspaceId = workspaceId;
  }

  public static ProposalDraft createDraft(String clientId, String serviceId, UUID workspaceId) {
    return new ProposalDraft(UUID.randomUUID().toString(), clientId, serviceId, workspaceId);
  }

  public abstract String getStatus();

  // Sealed subclasses
  public static final class ProposalDraft extends Proposal {
    public ProposalDraft(String id, String clientId, String serviceId, UUID workspaceId) {
      super(id, clientId, serviceId, workspaceId);
    }

    @Override
    public String getStatus() {
      return "DRAFT";
    }
  }

  public static final class ProposalPublished extends Proposal {
    private final String publicToken;
    private final Instant publishedAt;

    public ProposalPublished(String id, String clientId, String serviceId, UUID workspaceId,
                              String publicToken, Instant publishedAt) {
      super(id, clientId, serviceId, workspaceId);
      this.publicToken = publicToken;
      this.publishedAt = publishedAt;
    }

    @Override
    public String getStatus() {
      return "PUBLISHED";
    }
  }

  public static final class ProposalApproved extends Proposal {
    private final Instant approvedAt;
    private final String approverEmail;

    public ProposalApproved(String id, String clientId, String serviceId, UUID workspaceId,
                            Instant approvedAt, String approverEmail) {
      super(id, clientId, serviceId, workspaceId);
      this.approvedAt = approvedAt;
      this.approverEmail = approverEmail;
    }

    @Override
    public String getStatus() {
      return "APPROVED";
    }
  }
}
```

### Value Object: ProposalVersion (Record)

```java
// com/scopeflow/core/domain/proposal/ProposalVersion.java

public record ProposalVersion(
  String id,
  String proposalId,
  int versionNumber,
  ProposalScope scope,
  BigDecimal pricing,
  String timeline,
  String htmlContent,
  String status,
  Instant createdAt
) {
  public ProposalVersion {
    if (versionNumber < 1) throw new IllegalArgumentException("Version must be >= 1");
    if (scope == null) throw new IllegalArgumentException("Scope is required");
  }

  public boolean isPublished() {
    return "published".equals(status);
  }
}

public record ProposalScope(
  String objective,
  List<String> deliverables,
  List<String> exclusions,
  List<String> assumptions,
  List<String> dependencies
) {
  public ProposalScope {
    deliverables = deliverables == null ? List.of() : List.copyOf(deliverables);
    exclusions = exclusions == null ? List.of() : List.copyOf(exclusions);
    assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
  }
}
```

### DTO: Request/Response Records

```java
// com/scopeflow/adapter/in/web/dto/request/CreateProposalRequest.java

public record CreateProposalRequest(
  @NotBlank String clientId,
  @NotBlank String serviceId
) {}

// com/scopeflow/adapter/in/web/dto/response/ProposalResponse.java

public record ProposalResponse(
  String id,
  String status,
  ClientSummary client,
  ServiceSummary service,
  Instant createdAt,
  ProposalVersion latestVersion
) {}

public record ClientSummary(String id, String name, String email) {}
public record ServiceSummary(String id, String name) {}
```

---

## 4. Virtual Threads & Structured Concurrency

### PDF Generation Service (Virtual Threads)

```java
// com/scopeflow/adapter/out/storage/PdfGenerationService.java

@Service
public class PdfGenerationService {

  private final AmazonS3Client s3Client;
  private final HtmlToDocumentConverter converter;

  public void generateProposalPdfAsync(ProposalVersion version, UUID workspaceId) {
    // Virtual Thread: lightweight, no thread pool exhaustion
    Thread.ofVirtual().start(() -> {
      try {
        byte[] pdfContent = converter.convertHtmlToPdf(version.htmlContent());
        String s3Key = "pdfs/%s/proposal-%s.pdf".formatted(workspaceId, version.proposalId());
        s3Client.putObject(new PutObjectRequest(S3_BUCKET, s3Key, new ByteArrayInputStream(pdfContent)));
      } catch (Exception e) {
        logger.error("PDF generation failed", e);
        // Retry logic, notification, etc.
      }
    });
  }
}
```

### Structured Concurrency: Parallel Task Execution

```java
// com/scopeflow/core/application/briefing/ConsolidateBriefingUseCase.java

@Service
public class ConsolidateBriefingUseCase {

  private final AiOrchestrator aiOrchestrator;
  private final BriefingRepository briefingRepository;

  public BriefingConsolidation execute(String sessionId) throws Exception {
    var session = briefingRepository.findSession(sessionId);

    // Structured Concurrency: parallel tasks com timeout
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      // Task 1: IA consolidação
      var futureConsolidation = scope.fork(() ->
        aiOrchestrator.consolidateBriefing(session.answers())
      );

      // Task 2: Validação em paralelo
      var futureValidation = scope.fork(() ->
        validateBriefingCompleteness(session.answers())
      );

      // Aguarda ambas com timeout de 30 segundos
      scope.joinUntil(Instant.now().plusSeconds(30));

      var consolidation = futureConsolidation.resultNow();
      var validation = futureValidation.resultNow();

      // Salva resultado
      briefingRepository.saveConsolidation(session.id(), consolidation);
      return consolidation;
    }
  }
}
```

### AI Orchestrator with Pattern Matching

```java
// com/scopeflow/adapter/out/ai/AiOrchestrator.java

@Service
public class AiOrchestrator {

  private final OpenAiClient openAiClient;
  private final PromptTemplateLoader promptLoader;

  public String generateAiContent(String generationType, Map<String, Object> context) throws Exception {
    var prompt = promptLoader.loadPrompt(generationType + "_v1");
    var populatedPrompt = populateTemplate(prompt, context);

    var response = openAiClient.createChatCompletion(populatedPrompt);

    // Pattern Matching: switch com type checking
    return switch(generationType) {
      case "briefing_questions" -> parseQuestionsJson(response);
      case "briefing_consolidation" -> parseBriefingJson(response);
      case "scope_generation" -> parseScopeJson(response);
      case "approval_summary" -> parseSummaryJson(response);
      case "kickoff_summary" -> parseKickoffJson(response);
      default -> throw new UnknownAiGenerationTypeException("Unknown type: " + generationType);
    };
  }
}
```

---

## 5. Spring Security + JWT Configuration

```java
// com/scopeflow/config/SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // Stateless API
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/public/**").permitAll()
        .anyRequest().authenticated()
      )
      .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}
```

---

## 6. Flyway Migrations

```sql
-- src/main/resources/db/migration/V1__initial_schema.sql

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE workspaces (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  slug VARCHAR(255) UNIQUE NOT NULL,
  niche_primary VARCHAR(50),
  tone_of_voice VARCHAR(50),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE workspace_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(50) NOT NULL,
  UNIQUE(workspace_id, user_id)
);

-- ... (rest of schema from CLAUDE.md Database Schema section)
```

---

## 7. Timeline de Migração

### Sprint 1 (Refazer com Java 21)
- **Bootstrap:** `/full-bootstrap scopeflow-mvp aws --java 21`
- **Setup:** Maven, Spring Boot 3.2 parent, pom.xml
- **Domain:** Sealed classes + records + exceptions
- **Database:** PostgreSQL + Flyway + schema
- **Auth:** Spring Security 6.x + JWT
- **Tests:** JUnit 5 + Mockito + TestContainers base

**Entregável:** Backend rodando com `/health` + `/auth/register` + `/auth/login`

### Sprint 2 (Catálogo)
- **Services:** ServiceCatalog + ServiceContextProfile JPA entities
- **Templates:** ProposalTemplate management
- **Seed data:** 3 serviços (social media, landing page, brand)
- **Tests:** Repository tests com TestContainers

**Entregável:** CRUD endpoints funcionando

### Sprint 3 (Briefing IA)
- **Virtual Threads:** async PDF, async IA calls
- **Structured Concurrency:** parallel tasks com timeout
- **AI Integration:** OpenAI SDK, prompt loading, consolidation
- **RabbitMQ:** async queue listeners

**Entregável:** Briefing completo (discovery → consolidação)

### Sprint 4 (Escopo + Proposta)
- **Scope generation:** IA + versioning
- **Proposal rendering:** Handlebars → HTML → PDF async
- **Virtual threads:** PDF generation em background

**Entregável:** Scope + Proposal rendering

### Sprint 5 (Aprovação)
- **Public link:** token validation
- **Approval tracking:** IP, UA, timestamp
- **PDF async:** BullMQ → RabbitMQ workers

**Entregável:** Approval rastreável

### Sprint 6 (Kickoff + Dashboard)
- **Kickoff summary:** IA + PDF async
- **Dashboard:** status, metrics, approval rate
- **Observability:** Logback + JSON logs
- **Final audit:** coverage, security, performance

**Entregável:** MVP completo pronto pra validação

---

## 8. Comparação: Custo de Mudança vs Benefício

### Custo
- **Sprint 1:** Refazer backend inteiro (maior investimento)
- **Timeline:** +1-2 semanas vs NestJS MVP
- **Learning curve:** Java 21 features (sealed classes, records, virtual threads)

### Benefício
- **Performance:** Virtual threads = 1000+ concurrent requests sem thead pool
- **Type safety:** Sealed classes + records reduzem bugs
- **LTS:** Java 21 suporte até 2031 vs Node.js volatilidade
- **Enterprise:** Spring Security, proven at scale, large ecosystem
- **Scalability:** Melhor vertical scaling que Node.js

### ROI
**Recomendação:** ✅ APROVADO

- MVP valida 3 meses
- Se retenção > 80%, Java 21 investment compensa
- Se retenção < 50%, teria sido melhor NestJS rápido

---

## 9. Próximos Passos

1. ✅ Documentação atualizada (CLAUDE.md + 5 planos)
2. ✅ Stack Java 21 + Spring Boot 3.2 aprovado
3. ⏳ **Próximo:** Sprint 1 / `/full-bootstrap scopeflow-mvp aws --java 21`

---

**Status:** ✅ MIGRAÇÃO DOCUMENTADA
**Quando começar:** Aprovado, pronto pra Fase 4 (Execução)

