# ADR-006: RFC 9457 Problem Details para Erros REST

**Status:** Aceito
**Data:** 2026-03-24

## Contexto

A API REST do ScopeFlow precisa de um formato padronizado para erros. Sem padrao, cada endpoint retorna erros em formatos diferentes, dificultando tratamento no frontend e debugging em producao.

## Decisao

Usar **RFC 9457 Problem Details for HTTP APIs** como formato unico para todas as respostas de erro. Spring Boot 3.2 tem suporte nativo via `ProblemDetail` class.

Formato padrao:

```json
{
  "type": "https://api.scopeflow.com/errors/{error-slug}",
  "title": "Human Readable Title",
  "status": 409,
  "detail": "Specific error message with context",
  "instance": "/api/v1/proposals/550e8400-...",
  "error_code": "PROPOSAL-001",
  "error_id": "7c9e6679-...",
  "timestamp": "2026-03-24T10:30:45Z"
}
```

Extensoes customizadas:
- `error_code`: codigo estavel para tratamento programatico no frontend (ex: `USER-001`)
- `error_id`: UUID unico por ocorrencia (para correlacao com logs)
- `timestamp`: momento exato do erro
- `violations`: array de field errors (apenas em 400 Validation Error)

## Alternativas Consideradas

### Opcao A: Formato customizado (ErrorResponse DTO)

- Pros: Total controle; pode incluir qualquer campo
- Contras: Nao padronizado; frontend precisa de parser customizado; nao interoperavel

### Opcao B: RFC 9457 Problem Details (escolhida)

- Pros: Padrao HTTP (RFC publicado); Spring Boot 3.2 suporta nativamente; Content-Type `application/problem+json`; extensivel com campos customizados; clientes HTTP ja reconhecem
- Contras: Formato fixo (type, title, status, detail obrigatorios); campos extras precisam de `setProperty()`

### Opcao C: Spring Boot default error handling

- Pros: Zero configuracao; funciona out-of-the-box
- Contras: Formato inconsistente entre tipos de erro; stack traces vazam em dev; sem error codes estaveis

## Consequencias

- `GlobalExceptionHandler` centraliza toda conversao exception -> ProblemDetail
- Cada domain exception tem error_code estavel (convenção `{DOMAIN}-{NNN}`)
- Frontend pode tratar erros por `error_code` (programatico) ou `status` (HTTP standard)
- Stack traces NUNCA incluidos na resposta (apenas no log server-side)
- Content-Type das respostas de erro: `application/problem+json`
- Ja implementado para Briefing context (Sprint 1). Estender para Auth, Workspace, Proposal
- O `type` URI e informativo (nao precisa resolver para pagina real no MVP, mas pode futuramente)
