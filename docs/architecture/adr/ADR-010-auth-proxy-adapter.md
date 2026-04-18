# ADR-010: AuthProxyAdapter para proxy HTTP ao user-service

**Status:** Aceito  
**Data:** 2026-04-17  
**Contexto:** Strangler Fig — user-service extraído, monólito como proxy

---

## Contexto

Após a extração do user-service (ADR-008), o monólito passou a fazer proxy das
requisições de `/api/v1/auth/*` e `/api/v1/users/*` para o user-service.
Dois controllers (`AuthControllerV2` e `UserController`) implementavam esse proxy
usando `RestTemplate` diretamente:

```java
} catch (Exception e) {
    throw new RuntimeException("User service unavailable", e); // ← problema
}
```

Isso criava dois problemas:

1. **HTTP 500 em vez de 503**: `RuntimeException` cai no catch-all do
   `GlobalExceptionHandler`, retornando HTTP 500 genérico sem `Retry-After`.
   O handler correto (HTTP 503 com `Retry-After: 30`) só é acionado para
   `ServiceUnavailableException`.

2. **Sem circuit breaker**: chamadas diretas via `RestTemplate` sem Resilience4j
   bloqueiam threads do Tomcat por tempo indeterminado quando o user-service cai.
   Com o `UserServiceRestAdapter` (caminho tipado para workspace invites) já
   protegido pelo circuit breaker "user-service", apenas o caminho de auth/proxy
   ficava desprotegido — inconsistência que se tornaria visível em produção.

---

## Decisão

Criar `AuthProxyAdapter` em `adapter/out/userservice/`, dedicado ao proxy passthrough:

- Anotado com `@CircuitBreaker(name = "user-service")` e `@Retry(name = "user-service")`
- **Compartilha a mesma instância CB** do `UserServiceRestAdapter` — qualquer
  instabilidade do user-service abre o circuito para ambos os caminhos simultaneamente
- Propaga status code, body e headers relevantes (Set-Cookie, Content-Type, etc.),
  filtrando hop-by-hop headers (RFC 2616 §13.5.1)
- Trata `HttpClientErrorException` (4xx) localmente — não abre circuit, não faz retry
- Converte falhas de rede/5xx em `ServiceUnavailableException` → HTTP 503 com `Retry-After: 30`
- `AuthControllerV2` e `UserController` injetam `AuthProxyAdapter` via construtor
  e removem dependência direta de `RestTemplate`

---

## Alternativas consideradas

### Opção A — Apenas trocar RuntimeException por ServiceUnavailableException

- Pros: trivial (5 linhas), risco zero
- Contras: sem circuit breaker, sem retry, threads continuam bloqueadas, violação
  arquitetural (controller dependendo de RestTemplate) persiste

### Opção B — Adicionar métodos proxy no UserServiceRestAdapter existente

- Pros: reutiliza adapter existente, menos arquivos
- Contras: viola SRP — `UserServiceRestAdapter` tem contrato tipado
  (`Optional<UserResponse>`, `UserResponse`) incompatível com passthrough
  (`ResponseEntity<String>`); polui o port `UserServiceClient`

### Opção C — AuthProxyAdapter dedicado (escolhida)

- Pros: SRP, hexagonal, circuit breaker + retry, sem poluição de contrato
- Contras: um arquivo a mais — custo aceitável para a separação de responsabilidades

---

## Consequências

**Positivas:**
- Todas as chamadas ao user-service (tipadas e proxy) passam pelo mesmo circuit breaker
- Thread starvation mitigado: circuit aberto → fast-fail em < 1ms
- `AuthControllerV2` e `UserController` sem dependência de infraestrutura HTTP
- Comportamento de falha consistente: HTTP 503 + `Retry-After: 30` para todos os caminhos

**Negativas / trade-offs:**
- Dois adapters de saída para o mesmo serviço externo (`UserServiceRestAdapter` tipado +
  `AuthProxyAdapter` passthrough) — intencional pela diferença de contrato
- `AuthProxyAdapter` não implementa nenhum port de domínio (não há port para proxy puro)
  — aceitável como adaptação estrutural de Strangler Fig

---

## Arquivos modificados

| Arquivo | Mudança |
|---------|---------|
| `adapter/out/userservice/AuthProxyAdapter.java` | Criado — adapter com CB + retry |
| `adapter/in/web/auth/AuthControllerV2.java` | Refatorado — injeta `AuthProxyAdapter` |
| `adapter/in/web/user/UserController.java` | Refatorado — injeta `AuthProxyAdapter` |

## Relacionado a

- ADR-008: Extração do user-service via Strangler Fig
- ADR-009: Decommission do módulo User no monólito
