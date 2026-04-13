# ADR-008: Extração do User Service — Conclusão via Strangler Fig

**Status:** Accepted
**Data:** 2026-04-11
**Autores:** Tech Lead, Backend Engineer
**Relaciona-se a:** ADR-001 (Ordem de extração), ADR-002 (Shared DB inicial), ADR-003 (Comunicação entre serviços), ADR-004 (Ownership por contexto)

---

## Contexto

O ScopeFlow AI iniciou a jornada de decomposicao do monolito Spring Boot 3.4 / Java 21 em
microsservicos usando o padrao Strangler Fig. Conforme definido no ADR-001 (`docs/migration/adr/`),
o **User (Auth)** foi escolhido como o primeiro bounded context a ser extraido, pelos seguintes
motivos:

1. **Zero dependencias de saida** — User nao chama outros contextos, apenas e chamado
2. **Valor para validar a infra** — primeiro servico real sob Strangler Fig sem risco de dominio complexo
3. **Baixo acoplamento de dominio** — tabela `users` e seus value objects sao auto-contidos
4. **Pre-requisito dos outros contextos** — Workspace, Briefing e Proposal referenciam `user_id`

A extracao foi executada em 20 sprints agrupados em 5 blocos:

| Bloco | Sprints | Entrega |
|-------|---------|---------|
| A     | 1-4     | Estrutura hexagonal, domain model com sealed classes (`UserActive`, `UserInactive`), value objects (`Email`, `PasswordHash`, `UserId`), RFC 9457 |
| B     | 5-8     | JPA mapping na tabela `users` compartilhada, port `UserRepository` + adapter, `flyway.enabled=false` |
| C     | 9-12    | `JwtService` com segredo compartilhado, `JwtAuthenticationFilter`, `SecurityFilterChain` stateless, BCrypt alinhado ao monolito |
| D     | 13-16   | `RegisterUseCase`, `LoginUseCase` (access + refresh), `AuthController` (`/api/v1/auth/*`), `UserController` (`/api/v1/users/me`, `/users/by-email`) |
| E     | 17-20   | `AuthControllerV2` no monolito com proxy condicional, 8 contract tests Spring Cloud Contract, cut-over + cleanup |

O Sprint 20 marca o **cut-over efetivo** do trafego de autenticacao do monolito para o user-service
em producao, via feature toggle `auth.service.use-extracted`. Este ADR registra formalmente a
decisao de conclusao, o estado final da extracao e as dividas tecnicas assumidas.

---

## Decisao

Concluir a extracao do **User Service** adotando as seguintes posturas tecnicas:

### 1. Shared database (PostgreSQL compartilhado) — transicao, nao final

O user-service acessa a **mesma instancia PostgreSQL** do monolito via JDBC direto, lendo e
escrevendo na tabela `users`. Nao ha replicacao, nao ha event sourcing, nao ha CDC. Esta decisao
esta alinhada ao ADR-002 (migration) que define a Fase 1 como shared DB + servicos separados.

Justificativa:
- Elimina o custo de sync de dados durante a primeira extracao
- Garante consistencia forte (uma unica fonte da verdade para `users`)
- Reduz blast radius de bugs de integracao no primeiro servico extraido
- Permite rollback instantaneo (sem reconciliacao de dados)

### 2. Feature toggle para cut-over com zero downtime

O monolito expoe `AuthControllerV2` que, baseado na flag `auth.service.use-extracted`:
- **Flag OFF:** trata a requisicao localmente (comportamento legado)
- **Flag ON:** faz proxy REST para o user-service e retorna a resposta transparente ao cliente

A flag e configuravel por ambiente (staging primeiro, depois producao) e permite rollback
imediato sem novo deploy.

### 3. JWT_SECRET compartilhado via variavel de ambiente

Tanto o monolito quanto o user-service validam e assinam tokens usando o mesmo `JWT_SECRET`,
mesma string de algoritmo (HS256), mesmo tempo de vida. Isso garante que tokens emitidos por
qualquer um dos servicos sao aceitos pelo outro durante a transicao.

### 4. Contract tests como gate de deploy

8 contratos Spring Cloud Contract foram escritos no user-service e executados no pipeline do
monolito como consumer. Qualquer quebra de contrato impede o deploy do user-service. Isso
estabelece o **guarda-chuva de compatibilidade** durante toda a fase de coexistencia.

### 5. Nao foi adotado

- Event sourcing (descartado: complexidade desproporcional para o primeiro passo)
- Database-per-service imediato (descartado: ver ADR-002 de migration)
- API Gateway dedicado (descartado: proxy via monolito e suficiente no curto prazo)
- CDC / Debezium (descartado: shared DB elimina a necessidade)

---

## Consequencias

### Positivas

- **Escalabilidade independente:** user-service escala horizontalmente sem acoplamento ao
  ciclo de vida do monolito. Picos de login/register nao competem por recursos com Briefing ou IA.
- **Ciclo de deploy separado:** mudancas em auth (politicas de senha, novos hashes, MFA) podem ser
  entregues sem redeploy do monolito.
- **Primeiro servico real em producao:** valida infra de observabilidade, CI/CD, secrets, contract
  tests. Aprendizado direto aplicavel aos proximos contextos.
- **Preparacao para Workspace:** com User isolado, a extracao do Workspace (proximo contexto, ADR-001
  migration) passa a ter apenas uma dependencia externa real — e ela ja e um servico.
- **Disciplina de bounded context:** a equipe agora tem uma tabela fisica fora do alcance direto do
  monolito, o que forca uso de API em vez de joins cross-context em novo codigo.
- **Rollback trivial:** desligar a flag restaura comportamento legado em segundos, sem migracoes
  reversas.

### Negativas e riscos

- **Acoplamento de dado persiste:** banco compartilhado significa que alteracoes de schema em `users`
  exigem coordenacao entre os dois deploys. Nao ha isolamento fisico real ainda.
- **Risco de divergencia de JWT_SECRET:** se uma das instancias receber um segredo diferente por
  erro de config, todos os tokens quebram. Mitigacao: validacao no boot + alerta.
- **Coupling via FKs cross-context:** `workspaces.owner_id -> users.id` ainda existe fisicamente.
  Migracao de banco exigira remocao dessas FKs (planejada, ver proximos passos).
- **Duplicacao temporaria de codigo de auth:** o monolito ainda contem as classes legadas ate o
  cleanup completo do Sprint 20. Risco: drift se alguem tocar o codigo legado sem atualizar o
  user-service.
- **Observabilidade fragmentada:** logs de uma sessao de login agora atravessam dois servicos.
  Requer correlation ID propagado (implementado, mas com cobertura a validar).
- **Custo operacional:** mais um deploy, mais um pipeline, mais um alvo de monitoring, mais um
  job de on-call. Aceitavel para o primeiro servico, mas acumulativo.

---

## Estrategia de Cut-over

O cut-over segue um pipeline de validacao com gates explicitos:

### Fase 1 — Staging (T+0 a T+48h)

1. Deploy do user-service em staging com a mesma versao de codigo do monolito
2. Ativacao da flag `auth.service.use-extracted=true` no monolito em staging
3. Execucao da suite E2E completa (`./RUN-BRIEFING-TESTS.sh`, smoke tests, suite de auth)
4. Observacao por 48h de:
   - Latencia p50/p95/p99 de `/auth/login` e `/auth/register`
   - Taxa de erro 5xx
   - Logs de `JwtAuthenticationFilter` (tokens invalidos, expirados, malformed)
   - Metricas de contract tests no pipeline

### Fase 2 — Producao (T+48h a T+72h)

5. Ativacao gradual da flag em producao com janela de observacao reduzida:
   - 10% de trafego via user-service por 30 minutos
   - 50% por 1 hora
   - 100% se todos os SLOs estiverem verdes
6. Gates de SLO:
   - **p95 < 200ms** em `/auth/login`
   - **Taxa de erro < 0.1%** em qualquer endpoint de auth
   - **Zero** rejeicoes de JWT por divergencia de segredo
7. Durante 24h adicionais, monitoring reforcado com alertas em tempo real

### Fase 3 — Cleanup (T+72h em diante)

8. Remocao do codigo legado de auth no monolito (`AuthController` v1, `UserControllerV1`,
   services e repositories duplicados) em PR dedicado
9. Manutencao do `AuthControllerV2` como proxy permanente ate desativar a porta publica do monolito
10. Atualizacao do OpenAPI spec do monolito para apontar auth ao user-service

---

## Rollback

O rollback e **stateless, instantaneo e sem perda de dados** porque:

1. O banco e o mesmo. Nao ha reconciliacao.
2. A flag `auth.service.use-extracted=false` no monolito faz com que as requisicoes voltem a
   ser processadas localmente pelo `AuthControllerV2` (modo legado).
3. Tokens ja emitidos permanecem validos (mesmo `JWT_SECRET`).
4. Nao ha migracao de schema reversa a aplicar.

### Criterios de acionamento do rollback

- p95 > 200ms em `/auth/login` por mais de 5 minutos consecutivos
- Taxa de erro 5xx > 1% em qualquer endpoint de auth
- Qualquer rejeicao sistematica de JWT validos (indicando divergencia de segredo ou clock skew)
- Perda de conectividade user-service -> PostgreSQL por mais de 60 segundos
- Qualquer comportamento inesperado relatado por cliente em menos de 15 minutos apos ativacao

### Procedimento

1. Alterar a flag via config server / variavel de ambiente no monolito
2. Rollout da mudanca (reload em hot, sem redeploy)
3. Validar com smoke test de `/auth/login`
4. Abrir post-mortem em `docs/devops/runbooks/postmortem-YYYY-MM-DD-user-service.md`

---

## Proximos passos

Com a extracao do User Service concluida, os seguintes trabalhos estao explicitamente **fora do
escopo** deste sprint e devem ser planejados em backlog dedicado:

1. **Database-per-service para User (ADR-002 migration Fase 3):**
   - Criar instancia PostgreSQL propria para o user-service
   - Migrar a tabela `users` para o novo banco
   - Substituir FKs `workspaces.owner_id -> users.id` por referencia logica (UUID opaco)
   - Expor leitura de usuario cross-context apenas via API do user-service
2. **Substituir JDBC compartilhado por REST/gRPC:** qualquer leitura/escrita de `users` feita
   pelo monolito deve passar pela API publica do user-service, nao pelo banco.
3. **Secrets management:** mover `JWT_SECRET` para Vault/Secrets Manager com rotacao controlada e
   validacao de igualdade no boot dos dois servicos.
4. **Extracao do Workspace:** proximo bounded context conforme ADR-001 migration. Agora com User
   ja como servico, a dependencia Workspace -> User vira uma chamada HTTP.
5. **Cleanup do codigo legado:** remover controllers, services e repositories de auth do monolito
   apos 14 dias de estabilidade em producao.
6. **Observabilidade:** garantir correlation ID ponta a ponta, dashboards de auth dedicados,
   alertas de divergencia de JWT.

---

## Referencias

- ADR-001 (migration) — Ordem de extracao dos bounded contexts
- ADR-002 (migration) — Database strategy: shared DB inicial
- ADR-003 (migration) — Comunicacao entre servicos
- ADR-004 (migration) — Ownership por servico e contexto
- ADR-006 (architecture) — RFC 9457 Problem Details
- `.claude/plans/USER-SERVICE-EXTRACTION-20-SPRINTS.md` — plano detalhado dos 20 sprints
- Contract tests: `user-service/src/test/java/com/scopeflow/user/contract/`
- Feature toggle: `backend/src/main/java/com/scopeflow/adapter/in/web/auth/AuthControllerV2.java`
