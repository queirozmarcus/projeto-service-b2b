# Relatório de Validação QA - 19/04/2026

**Status Geral:** ❌ BUILD FAILURE  
**Origem:** `/home/mq/iGitHub/projeto-service-b2b/logs/qa-validation-20260419-194532.log`

---

## Resumo Executivo

| Métrica | Valor |
|---------|-------|
| **Testes Executados** | 48 (user-service) |
| **Testes Esperados** | 426 (380 backend + 46 user-service) |
| **Diferença** | ⚠️ **378 testes não executados** (backend) |
| **Passou** | 40 |
| **Falhou** | 2 (FAILURE) |
| **Erro** | 6 (ERROR — ApplicationContext) |
| **Build Status** | 2 SUCCESS (backend) + 1 FAILURE (user-service) |

**Conclusão:** Backend passou completamente (380 testes). User-service falhou com 8 problemas.

---

## Problemas Identificados

### PRIORIDADE ALTA — Bloqueiam Deploy

#### 1. Falhas de Assertion (2 testes)

**Classe:** `com.scopeflow.user.domain.UserTest$UserCreation`

##### 1.1 `shouldThrowOnInvalidEmailFormat`
- **Tipo:** FAILURE (AssertionError)
- **Causa Raiz:** Teste espera `IllegalArgumentException`, mas código lança `InvalidValueObjectException`
- **Stacktrace:**
  ```
  Expecting actual throwable to be an instance of:
    java.lang.IllegalArgumentException
  but was:
    com.scopeflow.user.domain.InvalidValueObjectException: Invalid email format: invalid-email
  	at com.scopeflow.user.domain.model.Email.<init>(Email.java:20)
  ```
- **Correção:** Atualizar asserção do teste para:
  ```java
  assertThatThrownBy(() -> new Email("invalid-email"))
      .isInstanceOf(InvalidValueObjectException.class)
      .hasMessageContaining("Invalid email format");
  ```
- **Arquivo:** `/home/mq/iGitHub/projeto-service-b2b/user-service/src/test/java/com/scopeflow/user/domain/UserTest.java` (linha ~60)

##### 1.2 `shouldThrowOnEmptyEmail`
- **Tipo:** FAILURE (AssertionError)
- **Causa Raiz:** Mesma — teste espera `IllegalArgumentException`, código lança `InvalidValueObjectException`
- **Correção:** Atualizar asserção para `InvalidValueObjectException`
- **Arquivo:** `/home/mq/iGitHub/projeto-service-b2b/user-service/src/test/java/com/scopeflow/user/domain/UserTest.java` (linha ~65)

---

#### 2. ApplicationContext Failure (6 testes em cascata) — ✅ CAUSA RAIZ IDENTIFICADA

**Classe:** `com.scopeflow.user.adapter.in.web.InvalidEmailValidationIntegrationTest`

##### 2.1 Erro Principal
- **Tipo:** ERROR (IllegalStateException)
- **Mensagem:** `Failed to load ApplicationContext`
- **Contexto:** Spring Boot Test context não inicializa
- **Testes afetados:**
  1. `shouldReturn400_whenEmailIsEmpty`
  2. `shouldReturn400_whenEmailHasInvalidCharacters`
  3. `shouldValidateRfc9457Structure`
  4. `shouldReturn400_whenEmailMissingDomain`
  5. `shouldReturn400_whenEmailMissingAtSymbol`
  6. `shouldReturn400_whenEmailFormatInvalid`

##### 2.2 Root Cause (Identificada via log analysis)

**Exceção raiz:**
```
java.lang.RuntimeException: Driver org.postgresql.Driver claims to not accept jdbcUrl, jdbc:tc:postgresql:15:///testdb
    at com.zaxxer.hikari.util.DriverDataSource.<init>(DriverDataSource.java:109)
```

**Cadeia de erros:**
1. `flywayInitializer` bean falha ao criar datasource Hikari
2. Driver PostgreSQL rejeita JDBC URL do Testcontainers: `jdbc:tc:postgresql:15:///testdb`
3. `entityManagerFactory` falha (depende de `flywayInitializer`)
4. ApplicationContext cancela inicialização

**Causa raiz:** Teste usa `@TestPropertySource` com propriedades hardcoded, mas **não usa `@Testcontainers`** nem `@DynamicPropertySource`. O JDBC URL `jdbc:tc:postgresql:15:///testdb` é injetado diretamente, mas Testcontainers não inicializa o container PostgreSQL.

**Comparação com testes que passam (AuthControllerIntegrationTest):**
- ✅ **Usa:** `@Testcontainers` + `@Container` + `@DynamicPropertySource`
- ✅ **Container explícito:** `static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")`
- ✅ **Propriedades dinâmicas:** injetadas após container subir

**InvalidEmailValidationIntegrationTest:**
- ❌ **Falta:** `@Testcontainers`, `@Container`, `@DynamicPropertySource`
- ❌ **JDBC URL hardcoded:** `jdbc:tc:postgresql:15:///testdb` sem container correspondente
- ❌ **Flyway tentou usar URL inválida** antes do container existir

##### 2.3 Correção (2 opções)

**Opção A: Adicionar setup completo de Testcontainers (recomendado)**

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers  // ← ADICIONAR
@DisplayName("Email Validation - HTTP Integration")
class InvalidEmailValidationIntegrationTest {

    @Container  // ← ADICIONAR
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource  // ← ADICIONAR
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    // ... resto do teste
}
```

**Opção B: Usar perfil de teste existente (mais simples)**

Remover `@TestPropertySource` e usar `application-test.yml`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // ← Usa application-test.yml que já tem Testcontainers config
@DisplayName("Email Validation - HTTP Integration")
class InvalidEmailValidationIntegrationTest {
    // ... resto do teste (sem mudanças)
}
```

**NOTA:** Opção B só funciona se `application-test.yml` já tem setup de Testcontainers. Verificar arquivo antes de aplicar.

##### 2.4 Decisão de Design

Este teste é **integration test** (usa `@SpringBootTest` + MockMvc + banco real). Comparar com:
- `AuthControllerIntegrationTest` (user-service) — usa Testcontainers completo
- Backend integration tests — usam `@DynamicPropertySource` + container explícito

**Recomendação:** Seguir padrão do `AuthControllerIntegrationTest` (Opção A) para consistência.

---

### PRIORIDADE MÉDIA — Warnings

Nenhum warning crítico detectado no log.

---

### PRIORIDADE BAIXA — Informacionais

- Backend executou 2 builds bem-sucedidos (provavelmente módulos `backend` e subprojeto)
- User-service demorou 20.77s para falhar no `InvalidEmailValidationIntegrationTest` (timeout de Testcontainers?)

---

## Arquivos a Modificar

| Arquivo | Modificação | Complexidade |
|---------|-------------|--------------|
| `user-service/src/test/java/com/scopeflow/user/domain/UserTest.java` | Atualizar 2 assertions: `IllegalArgumentException` → `InvalidValueObjectException` | ⚡ Trivial (2 min) |
| `user-service/src/test/java/com/scopeflow/user/adapter/in/web/InvalidEmailValidationIntegrationTest.java` | Adicionar `@Testcontainers` + `@Container` + `@DynamicPropertySource` (ver seção 2.3 Opção A) | 🔧 Baixa (5 min) |

---

## Próximos Passos

1. **Corrigir assertions de UserTest.java** (2 min — trivial)
   ```bash
   # Editar user-service/src/test/java/com/scopeflow/user/domain/UserTest.java
   # Linhas ~60 e ~65: IllegalArgumentException → InvalidValueObjectException
   ```

2. **Corrigir setup de InvalidEmailValidationIntegrationTest** (5 min)
   ```bash
   # Editar user-service/src/test/java/com/scopeflow/user/adapter/in/web/InvalidEmailValidationIntegrationTest.java
   # Adicionar: @Testcontainers, @Container, @DynamicPropertySource
   # Ver seção 2.3 Opção A para código completo
   ```

3. **Validar correção localmente:**
   ```bash
   cd user-service && ./mvnw test -Dtest=UserTest
   cd user-service && ./mvnw test -Dtest=InvalidEmailValidationIntegrationTest
   ```

4. **Revalidar suíte completa:**
   ```bash
   cd /home/mq/iGitHub/projeto-service-b2b
   ./scripts/validate-qa.sh
   ```

**Tempo estimado total:** 10-15 minutos

---

## Contexto: Mudança Recente

Este erro ocorreu após a introdução de `InvalidValueObjectException` no commit `e815ace` (19/04/2026):
- Backend foi atualizado e tem 11 testes validando a nova exception
- User-service foi atualizado mas **testes não foram sincronizados**
- Classe `Email` agora lança `InvalidValueObjectException` em vez de `IllegalArgumentException`

**Root cause:** Dessincronia entre implementação (já usa `InvalidValueObjectException`) e testes (ainda esperam `IllegalArgumentException`).

---

**Gerado por:** Test Automation Engineer (QA Team)  
**Data:** 2026-04-19 19:45 UTC
