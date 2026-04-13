# Sprint 2 Code Review — Adapter Layer

**Data:** 2026-03-24
**Revisor:** Code-Reviewer (Agent)
**Sprint:** Sprint 2 — JPA Entities + Repository Adapters + REST Controllers + Testes
**Veredicto:** Aprovado com ressalvas (ver secoes critica e importante)

---

## Sumario Executivo

A adapter layer do ScopeFlow respeita os limites da arquitetura hexagonal com rigor: nenhum import de `adapter.*` foi encontrado no `core/domain`, as entidades JPA estao completamente separadas das entidades de dominio, os converters `toDomain()`/`fromDomain()` usam pattern matching com `switch` expression (Java 21), e os DTOs sao todos `record`. A configuracao de seguranca e sólida (BCrypt 12 rounds, JWT stateless, filtro correto). A base de testes com Testcontainers + PostgreSQL real e um padrao de qualidade acima da media para MVP.

Foram identificados **3 problemas criticos** que bloqueiam um deploy seguro, **6 importantes** que degradam qualidade, e **5 sugestoes** de melhoria.

---

## Pontos Positivos

- **Hexagonal boundary intacta**: `grep` em `core/domain/**` nao encontrou nenhum import de `adapter.*`. Zero violations.
- **Pattern matching correto**: todos os `toDomain()` usam `switch (entity.getStatus())` com exaustividade verificada em compile time (o `default` com throw cobre casos inesperados).
- **DTOs como records**: todos os 50+ DTOs sao records imutaveis com Bean Validation correto (`@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@NotNull`).
- **Constructor injection em toda parte**: nenhum `@Autowired` em field foi encontrado. Controllers e adapters usam injecao por construtor.
- **BCrypt 12 rounds**: acima do minimo recomendado (10), adequado para producao.
- **RFC 9457 Problem Details**: todos os erros retornam `type`, `title`, `status`, `detail`, `instance`, `error_code`, `error_id`, `timestamp`. Implementacao correta e completa.
- **Testcontainers com PostgreSQL real**: nenhum H2 in-memory. O `ScopeFlowIntegrationTestBase` sobe PostgreSQL 16-alpine e aplica todas as migrations Flyway (V1-V4) antes dos testes. Isso elimina a classe inteira de bugs por divergencia entre H2 e Postgres.
- **`@Version` para optimistic locking**: presente em `JpaUser`, `JpaProposal`. Protege contra atualizacoes concorrentes.
- **Separacao public/authenticated**: endpoints de cliente (`/public/briefings/**`, `/proposals/*/approve`) estao corretamente em paths separados com rotas publicas no `SecurityConfig`, sem mistura de autenticacao.
- **Password never logged**: o log de login registra apenas `userId`, nunca o hash ou a senha em texto.
- **Error message generica para credenciais invalidas**: `AuthControllerV2` retorna "Invalid email or password" tanto para email inexistente quanto para senha errada. Nao ha timing side-channel via mensagem diferenciada.

---

## Critico (bloqueia merge)

### C1 — `JwtAuthenticationFilter` importa `JpaUserSpringRepository` diretamente

**Arquivo:** `config/JwtAuthenticationFilter.java:3,37`

```java
import com.scopeflow.adapter.out.persistence.user.JpaUserSpringRepository;
// ...
private final JpaUserSpringRepository userRepo;
```

**Problema:** O filtro de seguranca, que fica no pacote `config`, esta acoplado diretamente ao repositorio JPA de infraestrutura. Isso cria uma dependencia da camada de config para `adapter.out`, violando o principio de que a config so deve conhecer ports (interfaces de dominio), nao implementacoes. Mais grave: o filtro **faz uma query ao banco a cada requisicao autenticada** para verificar se o usuario ainda existe e esta ativo. Em 1.000 req/s isso seria 1.000 queries extras por segundo sem cache. Para um SaaS com crescimento, isso e um gargalo garantido.

**Sugestao:** Injetar `UserRepository` (port de dominio) em vez de `JpaUserSpringRepository`. Ou, se a verificacao em banco for necessaria, adicionar `@Cacheable` com TTL curto (ex: 30s) sobre o metodo. A alternativa mais limpa para MVP e remover a validacao de status do filtro e confiar no claim do token — revogar tokens via blacklist quando necessario.

---

### C2 — `inviteMember` gera `UserId.generate()` fake em vez de buscar o usuario real

**Arquivo:** `adapter/in/web/workspace/WorkspaceControllerV2.java:113-115`

```java
// Placeholder userId — in production, look up or create user by email
UserId invitedUserId = UserId.generate();
workspaceService.inviteMember(wsId, invitedUserId, request.role());
```

**Problema:** O endpoint `POST /workspaces/{id}/members/invite` aceita um email valido via `@Email`, mas ignora-o completamente e cria um UUID aleatorio. O registro de membro no banco nao aponta para nenhum usuario real. Qualquer teste ou cliente que usar este endpoint recebera 201 Created com dados silenciosamente invalidos, criando inconsistencia referencial no banco. Isso nao e um TODO aceitavel — o endpoint esta mentindo para o chamador.

**Sugestao:** Ou (a) implementar a busca por email via `UserRepository.findByEmail()` e retornar 404 se nao encontrado, ou (b) retornar 501 Not Implemented com `Problem Detail` ate a feature estar pronta, ou (c) remover o endpoint do contrato publico ate estar implementado. Nao e aceitavel retornar 201 com dados inventados.

---

### C3 — `BriefingService.detectGaps()` retorna `null` quando score < 80

**Arquivo:** `core/domain/briefing/BriefingService.java:194-196`

```java
if (score < 80) {
    return null; // Caller should handle retry or abandonment
}
```

**Problema:** Um metodo de dominio retornando `null` sem declarar isso via `Optional<>` e uma violacao de contrato implicita. O caller em `BriefingControllerV1.getBriefingProgress()` passa o resultado diretamente para `mapper.toProgressResponse(score)` sem verificar null, o que gera `NullPointerException` em runtime quando o score for < 80. Este e um bug real que chegara em producao no primeiro briefing incompleto que um usuario tentar visualizar o progresso.

**Sugestao:** Mudar a assinatura para `Optional<CompletionScore>` ou, melhor, sempre retornar um `CompletionScore` — pois um score de 40% e informacao valida, nao ausencia de informacao. O `IncompleteGapsException` ja existe para o caso de tentativa de conclusao prematura; o progresso deve ser reportado independentemente.

---

## Importante (deveria corrigir antes do QA)

### I1 — IP Spoofing via X-Forwarded-For nao validado

**Arquivo:** `adapter/in/web/proposal/ApprovalControllerV2.java:101-108`

```java
private String extractClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
        return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
```

**Problema:** O header `X-Forwarded-For` pode ser forjado por qualquer cliente se o load balancer nao for configurado para sobrescrever este header. Um atacante pode enviar `X-Forwarded-For: 127.0.0.1` e o sistema registrara o IP de loopback como IP do aprovador, invalidando o valor forensico do campo para auditorias e disputas contratuais. O IP registrado na aprovacao tem implicacoes legais (LGPD, evidencia de consentimento).

**Sugestao:** Configurar o Spring para confiar apenas em proxies conhecidos via `server.forward-headers-strategy=NATIVE` ou `ForwardedHeaderFilter` com lista de trusted proxies. Alternativamente, usar `RemoteAddrFilter` com validacao de CIDR do load balancer.

---

### I2 — `GET /proposals/{id}/versions` sem verificacao de workspace isolation

**Arquivo:** `adapter/in/web/proposal/ProposalControllerV2.java:99-106`

```java
@GetMapping("/{id}/versions")
public List<ProposalVersionResponse> getVersions(@PathVariable UUID id) {
    return proposalService.findVersions(ProposalId.of(id))
            .stream()
            .map(ProposalVersionResponse::from)
            .toList();
}
```

**Problema:** O endpoint `GET /proposals/{id}` verifica corretamente que a proposal pertence ao workspace do JWT antes de retornar. O endpoint `GET /proposals/{id}/versions` nao faz esta verificacao. Um usuario autenticado em qualquer workspace pode enumerar versoes de proposals de outros workspaces se souber (ou adivinhar) o UUID.

**Sugestao:** Adicionar a mesma verificacao de workspace isolation presente no `getById()` antes de retornar as versoes. O padrao ja existe no codigo — e so replicar.

---

### I3 — `GET /proposals` sem paginacao

**Arquivo:** `adapter/in/web/proposal/ProposalControllerV2.java:85-93`

```java
public List<ProposalResponse> list() {
    UUID workspaceId = SecurityUtil.getWorkspaceId();
    return proposalService.findByWorkspace(new WorkspaceId(workspaceId))
            .stream()
            .map(ProposalResponse::from)
            .toList();
}
```

**Problema:** `findByWorkspace` retorna todas as proposals sem limite. Um workspace com 500+ proposals carrega tudo em memoria, serializa tudo em JSON e envia ao cliente. O impacto e duplo: memoria JVM e latencia de rede.

**Sugestao:** Adicionar `Pageable` com `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`. O repositorio JPA ja tem infraestrutura para paginacao (ver `findByWorkspaceWithFilters` no briefing). O contrato de lista sem paginacao deve ser considerado inseguro para qualquer endpoint que retorna colecoes de tamanho indefinido.

---

### I4 — `BriefingControllerV1.listBriefings()` retorna pagina vazia hardcoded

**Arquivo:** `adapter/in/web/briefing/BriefingControllerV1.java:118-124`

```java
// TODO: Build Pageable with sort
// TODO: Apply filters (status, serviceType, createdAfter)
// For now, return empty page as placeholder until repository findByWorkspace is implemented
var content = java.util.List.<BriefingResponse>of();
var pageResponse = mapper.toPageResponse(content, 0, 0, size, page, true, true);
return ResponseEntity.ok(pageResponse);
```

**Problema:** O endpoint `GET /briefings` esta documentado publicamente via OpenAPI como funcional, mas sempre retorna lista vazia. O repositorio `JpaBriefingRepositoryAdapter.findByWorkspaceWithFilters()` ja esta implementado — so precisa ser conectado. Isso e diferente de C2 porque o dado nao e inventado, mas o endpoint e inutilizavel, quebrando o fluxo principal de listagem de briefings.

**Sugestao:** Conectar `JpaBriefingRepositoryAdapter.findByWorkspaceWithFilters()` ao controller, passando os filtros de query params ja mapeados na assinatura do metodo.

---

### I5 — `BriefingService` expoe repositorios via getters publicos para o controller

**Arquivo:** `core/domain/briefing/BriefingService.java:280-296`

```java
public BriefingSessionRepository sessionRepository() { return sessionRepository; }
public BriefingQuestionRepository questionRepository() { return questionRepository; }
public BriefingAnswerRepository answerRepository() { return answerRepository; }
```

**Problema:** O `BriefingControllerV1` acessa diretamente `briefingService.sessionRepository().findById()` e `briefingService.questionRepository().findBySession()` ao longo de varios metodos. Isso inverte a responsabilidade: o controller esta orquestrando queries de repositorio em vez de chamar casos de uso do servico de dominio. O dominio esta "vazando" para a camada de adapter. Um service de dominio nao deve expor seus repositorios.

**Sugestao:** Adicionar metodos de use case no `BriefingService` que encapsulem as queries compostas: ex. `getBriefingDetail(sessionId, workspaceId)` retornando um objeto que agrega sessao, questoes e respostas. O controller passa a chamar um unico metodo, e a logica de verificacao de workspace fica no service.

---

### I6 — Ausencia total de `@Transactional` nos adapters de escrita

**Arquivo:** `adapter/out/persistence/proposal/JpaProposalRepositoryAdapter.java:31-41` (e similares em user, workspace, briefing)

O padrao de upsert (find + conditional save) em `save()` dos adapters nao tem `@Transactional`. Entre o `findById()` e o `springRepo.save()` ha uma janela de race condition: dois requests concorrentes podem ambos encontrar `empty` e tentar inserir o mesmo ID, resultando em `DataIntegrityViolationException` nao tratada. O `@Version` protege o update, mas nao o insert.

**Sugestao:** Anotar todos os metodos `save()` dos adapters de persistencia com `@Transactional`. Para operacoes de leitura, adicionar `@Transactional(readOnly = true)` nos metodos `findBy*`.

---

## Sugestoes (nice to have)

### S1 — `WorkspaceControllerV2.requireOwnerOrAdmin()` nao verifica workspace do token vs workspace da URL

**Arquivo:** `adapter/in/web/workspace/WorkspaceControllerV2.java:181-187`

A verificacao de role e feita corretamente, mas nao verifica se o `workspaceId` do JWT bate com o `workspaceId` da URL. Um OWNER do workspace A poderia invocar `PUT /workspaces/{id-do-workspace-B}/members/{memberId}/role` e passaria na verificacao de role. A verificacao de workspace isolation esta nos endpoints de proposal, mas nao nos de workspace.

---

### S2 — `ApprovalControllerV2.approve()` retorna `UUID.randomUUID()` como ID da aprovacao

**Arquivo:** `adapter/in/web/proposal/ApprovalControllerV2.java:89-96`

O ID retornado no `ApprovalResponse` e gerado no controller com `UUID.randomUUID()`, independente do ID real salvo no banco via `ProposalService.recordApproval()`. O cliente recebe um ID que nao corresponde ao registro persistido, impossibilitando correlacao para rastreabilidade.

---

### S3 — `BriefingService.detectGaps()` usa constante magica `10` como expectedAnswers

**Arquivo:** `core/domain/briefing/BriefingService.java:185`

```java
int expectedAnswers = 10; // Placeholder
```

Esta constante deveria ser parametrizavel por `ServiceType` ou configuravel via `ServiceContextProfile`, nao hardcoded no servico de dominio. Afeta diretamente o calculo de completude.

---

### S4 — `JpaBriefingRepositoryAdapter.extractAiAnalysis()` serializa JSON manualmente com concatenacao de strings

**Arquivo:** `adapter/out/persistence/briefing/JpaBriefingRepositoryAdapter.java:115-122`

```java
return "{\"gaps\":[" + String.join(",", gaps.stream().map(g -> "\"" + g + "\"").toList()) + "]}";
```

Concatenacao manual de JSON e fragil: qualquer gap contendo aspas ou caracteres especiais gerara JSON invalido sem estouro de excecao. Usar `ObjectMapper.writeValueAsString()` que ja esta disponivel como dependencia do projeto.

---

### S5 — Ausencia de `correlationId` nos logs

Os logs registram `userId`, `workspaceId` e `proposalId`, mas nao ha `correlationId` / `traceId` para correlacionar requests entre servicos ou dentro de uma mesma request multi-step. Para debugging em producao, isso dificulta a rastreabilidade.

**Sugestao:** Adicionar `MDC.put("correlationId", UUID.randomUUID().toString())` no `JwtAuthenticationFilter` apos autenticacao bem-sucedida. Para endpoints publicos, usar o header `X-Request-ID` se presente, ou gerar um novo.

---

## Checklist Final

| Categoria | Status | Detalhe |
|-----------|--------|---------|
| Hexagonal isolation | OK | 0 violations em `core/domain` |
| Sealed classes + pattern matching | OK | Switch expression nos adapters |
| Records para DTOs | OK | Todos os DTOs sao records |
| Constructor injection | OK | Sem @Autowired em field |
| Repository pattern | OK | Ports no dominio, adapters na infraestrutura |
| JWT validation | OK | Signature, expiry, tipo (access vs refresh) |
| RBAC | Parcial | Role check presente; workspace cross-check ausente (S1) |
| Input validation | OK | @Valid em todos os controllers |
| Password security | OK | BCrypt 12, nunca logado |
| Error messages | OK | Genericas para credenciais, Problem Detail em todos |
| N+1 queries | OK | Sem relacionamentos JPA com eager loading; sem findAll() |
| Paginacao | Parcial | Ausente em /proposals (I3) e /briefings (I4) |
| Caching | Nao implementado | Sem @Cacheable; DB hit a cada request autenticado (C1) |
| @Transactional | Parcial | Ausente nos metodos save() dos adapters (I6) |
| Migrations Flyway | OK | V1-V4 aplicadas, testadas via Testcontainers |
| Optimistic locking | OK | @Version em JpaUser e JpaProposal |
| Testcontainers | OK | PostgreSQL real nos integration tests |
| Coverage estimada | ~75% | Criticos cobertos; listagem briefings sem cobertura real |
| OpenAPI annotations | OK | @Operation, @ApiResponse presentes |

---

## Veredicto

**Aprovado com ressalvas.**

Os tres problemas criticos (C1, C2, C3) devem ser resolvidos antes do QA iniciar os smoke tests:

- **C1** e principalmente um problema de design que se tornara um gargalo de performance em producao.
- **C2** e um endpoint que retorna dados silenciosamente invalidos — enganosa para quem testa.
- **C3** e um NPE garantido em producao no primeiro usuario com briefing incompleto que visualizar progresso.

Os problemas importantes (I1-I6) podem ser tratados em paralelo pelo QA como issues a rastrear, mas I2 (versoes sem isolamento de workspace) e I6 (ausencia de @Transactional) sao relevantes para seguranca e corretude respectivamente e deveriam entrar antes da primeira release publica.
