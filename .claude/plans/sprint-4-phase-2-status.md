# Sprint 4 — Phase 2 Status: Event Listeners + Idempotency

**Data:** 2026-03-24
**Status:** ✅ CONCLUÍDO

---

## O que foi implementado

### Idempotency Infrastructure (D9)
- ✅ **V6 Migration:** Idempotency table com unique constraint (listener_id, idempotency_key)
- ✅ **IdempotencyRecord Entity:** Rastreamento de eventos processados com resultado
- ✅ **IdempotencyRepository:** Queries para polling e limpeza
- ✅ **IdempotencyService:** API simples (isProcessed, markAsProcessed)

Padrão Idempotent Consumer:
```
Evento chega (ex: UserRegisteredEvent)
  ↓
Listener checa: já processei? (query idempotency_record)
  ├─ SIM: skip (don't send email again)
  └─ NÃO: processa (envia email) → insere record (marca como processado)
```

### Event Listeners (3 total)

#### 1. UserRegistrationListener
- Queue: `user.registered`
- Side Effect: enviar welcome email (implementação em Phase 4)
- Idempotency Key: `user-{userId}`

#### 2. ProposalApprovalListener
- Queue: `proposal.approved`
- Side Effects:
  1. Gerar PDF (Phase 3 — PdfService + iText)
  2. Upload S3
  3. Enviar email aprovação (Phase 4 — EmailService + SES)
- Idempotency Key: `proposal-{proposalId}:{timestamp}`
- Armazena resultado (PDF URL) em idempotency_record para auditoria

#### 3. BriefingCompletedListener
- Queue: `briefing.completed`
- Side Effect: Fallback question generation (se não geradas in-session)
- Idempotency Key: `briefing-{sessionId}`

### Padrões & Decisões

**Transactional Behavior:**
- Listener marca como processado ANTES de completar (idempotency first)
- Se listener falha depois: RabbitMQ retenta, listener vê record, pula
- Resultado: side effects podem ser executados múltiplas vezes (idempotentes) ou parcialmente (graceful degradation)

**Error Handling:**
- Exception em listener → RabbitMQ retry (3x, backoff exponencial: 1s → 2s → 4s)
- After 3 retries → DLQ (Dead Letter Queue)
- Observação: user registration já foi salva (domínio persistiu), evento só falha se email service está indisponível

**Virtual Threads:**
- Listeners rodam em threads virtuais (Java 21)
- Non-blocking: múltiplos listeners processam em paralelo sem thread pool exhaustion

---

## Próximas Fases

### Phase 3: PDF Generation (2 dias)
- Implementar `PdfService` com iText 8
- Gerar proposal PDFs
- Upload to S3 (presigned URLs)
- Timeout handling (30s)

### Phase 4: Email Service (1.5 dias)
- Implementar `EmailService` com AWS SES
- 3 templates: welcome, proposal-approved, briefing-reminder
- Template rendering (Thymeleaf or Mustache)
- Rate limiting (SES sandbox limits)

### Phase 5: Outbox Poller (1 dia)
- OutboxEventPublisher scheduler ✅ (já implementado em Phase 1)
- Integração completa com listeners

### Phase 6: Integration Tests (1.5 dias)
- Testcontainers RabbitMQ
- Idempotency dedup validation
- Retry mechanism simulation
- DLQ routing tests

---

## Decisões Arquiteturais (Resumo)

| Decisão | O quê | Por quê |
|---------|-------|---------|
| **D7** | RabbitMQ + Spring AMQP | Durability, retry, DLQ out-of-box |
| **D8** | Outbox Pattern | Atomicity: TX = aggregate + event |
| **D9** | Idempotent Listeners | Prevent duplicate side effects (2x email) |
| **D10** | Retry 3x + Exponential Backoff | Resilience, graceful degradation |

---

## Status por Componente

| Componente | Estrutura | Lógica | Testes |
|-----------|-----------|--------|--------|
| OutboxEvent | ✅ | ✅ | ⏳ (Phase 6) |
| RabbitMQConfig | ✅ | ✅ | ⏳ |
| Listeners (3x) | ✅ | ⏳ (Phase 3-4) | ⏳ |
| IdempotencyService | ✅ | ✅ | ⏳ |
| PdfService | ⏳ | ⏳ | ⏳ |
| EmailService | ⏳ | ⏳ | ⏳ |

---

## Commits

1. **6b14081:** Phase 1 — Outbox + RabbitMQ infrastructure
2. **65edad2:** Type mismatch fixes (Sprint 2 adapter layer)
3. **b981b31:** Phase 2 — Event listeners + idempotency

---

## Próximo Passo

**Phase 3: PDF Generation Service**
- Adicionar `spring-boot-starter-mail` e iText 8 ao pom.xml
- Implementar `PdfService` interface
- Criar iText template loader
- Integrar com S3 (presigned URLs)
- Atualizar `ProposalApprovalListener` para chamar `pdfService.generateProposalPdf()`

**Tempo estimado:** ~2 dias
