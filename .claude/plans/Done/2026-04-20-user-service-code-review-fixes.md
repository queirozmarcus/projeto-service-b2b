---
Plano: user-service — Correção de Code Review (10 sprints)
Data: 2026-04-20
Status: CONCLUÍDO ✅
Duração: ~1 sessão (2026-04-20)
10 sprints + /qa-generate + estabilização de testes. 117 testes, 0 falhas.
Pack: Dev
Modelo: opusplan (S6, S7) / Sonnet default (demais)
---

## Contexto

Code review completo do user-service (ScopeFlow AI) revelou 15 achados:
- 3 Críticos (segurança/autorização)
- 7 Importantes (arquitetura + bugs)
- 5 Sugestões (polish)

Projeto: ~/iGitHub/projeto-service-b2b/user-service/src
Stack: Java 21, Spring Boot 3.x, hexagonal, PostgreSQL DB-per-service

## Decisões do Brainstorm

- Críticos primeiro (segurança não espera sprint)
- Arquiteturais depois (S6/S7 — extração de use cases)
- Sugestões por último
- S10 fecha com /qa-generate para validar

## Etapas

| Sprint | Escopo | Itens | Status |
|--------|--------|-------|--------|
| S1 | 🔴 Token sem role/workspaceId | Crítico #1 | ✅ CONCLUÍDO |
| S2 | 🔴 IP Spoofing no rate limiter | Crítico #2 | ✅ CONCLUÍDO |
| S3 | 🔴 Email como path variable | Crítico #3 | ✅ CONCLUÍDO |
| S4 | 🟡 Normalização de email + índice JPA | Imp #4 + #7 | ✅ CONCLUÍDO |
| S5 | 🟡 deactivateUser silencioso + race condition | Imp #3 + #6 | ✅ CONCLUÍDO |
| S6 | 🟡 Extrair AuthenticateUserUseCase + RegisterUserUseCase | Imp #1a | ✅ CONCLUÍDO |
| S7 | 🟡 Extrair InviteUserUseCase + JwtService como port TokenIssuer | Imp #1b + #2 | ✅ CONCLUÍDO |
| S8 | 🟡 Remover DB hit por request → blocklist Redis | Imp #5 | ✅ CONCLUÍDO |
| S9 | 💡 @NotBlank DTOs + PasswordHasher port + consolidar UserResponse | Sug #1 #2 #3 | ✅ CONCLUÍDO |
| S10 | 💡 Pattern matching + mensagem PT-BR + /qa-generate | Sug #4 #5 | ✅ CONCLUÍDO |

## Dependências

- S6 → depende de S1 (token com role correto antes de refatorar auth)
- S7 → depende de S6 (InviteUseCase vem depois de Auth/Register)
- S8 → depende de S1 (token com userId correto para blocklist)
- S9/S10 → independentes

## Riscos

- S6/S7: extração de use cases pode quebrar testes — rodar ./mvnw verify após cada sprint
- S8: Redis já disponível no docker-compose ✅
- S4: migration Flyway V2 irreversível — revisar antes de aplicar
