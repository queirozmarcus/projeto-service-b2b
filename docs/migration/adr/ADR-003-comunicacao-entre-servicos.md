# ADR-003: Comunicacao entre servicos — sincrona + assincrona

**Status:** Proposto
**Data:** 2026-04-05
**Contexto:**

Com a extracao de bounded contexts em servicos separados, e necessario definir como
os servicos se comunicam. O monolito atual usa chamadas diretas (injecao de dependencia)
para comunicacao sincrona e Outbox Pattern + RabbitMQ para eventos assincronos.

**Decisao:**

Usar comunicacao hibrida com criterio claro:

| Tipo | Quando | Mecanismo |
|------|--------|-----------|
| **Sincrona (REST)** | Queries que bloqueiam o fluxo do usuario | REST + circuit breaker (Resilience4j) |
| **Assincrona (eventos)** | Side effects, notificacoes, reacoes | Outbox Pattern + RabbitMQ (ja implementado) |
| **Token validation** | Todo request autenticado | JWT local (sem chamada REST ao User service) |

### Padroes especificos por fluxo

1. **Validar usuario autenticado**: JWT validation local (shared secret ou JWKS).
   Nenhuma chamada REST ao User service por request. Latencia zero.

2. **Workspace invite (buscar user por email)**: REST sincrono ao User service.
   Circuit breaker com fallback: "usuario nao encontrado, tente novamente".

3. **BriefingCompleted -> gerar proposta**: Evento assincrono via RabbitMQ.
   Ja funciona com Outbox Pattern. Nenhuma mudanca.

4. **ProposalApproved -> gerar PDF + email**: Evento assincrono via RabbitMQ.
   Ja funciona. Listeners permanecem no servico que possui o side effect.

**Alternativas consideradas:**

1. *Somente REST (sincrono)*: Descartado. Cria acoplamento temporal forte.
   Se User service estiver fora, Workspace nao consegue fazer invite.

2. *Somente eventos (assincrono)*: Descartado. Queries que bloqueiam o usuario
   (como login, refresh token) nao funcionam com eventual consistency.

3. *gRPC em vez de REST*: Considerado para comunicacao inter-servico de alta frequencia.
   Descartado por agora — REST e suficiente para o volume atual. Pode ser adotado depois.

**Trade-offs:**

| Ganha | Perde |
|-------|-------|
| Simplicidade: REST para queries, eventos para side effects | Dois mecanismos para manter (REST clients + event consumers) |
| Resiliencia: circuit breaker protege contra cascading failures | Complexidade de debugging distribuido (tracing) |
| Outbox Pattern ja implementado — reuso imediato | Eventos podem chegar fora de ordem (idempotency necessaria) |

**Impacto:**

- Cada servico que faz chamada REST a outro servico deve usar Resilience4j (circuit breaker + retry + timeout)
- Outbox table compartilhada continua funcionando na fase shared-DB
- Quando separar DBs, cada servico tera seu proprio outbox
- Distributed tracing (OpenTelemetry) sera necessario a partir do 2o servico extraido
