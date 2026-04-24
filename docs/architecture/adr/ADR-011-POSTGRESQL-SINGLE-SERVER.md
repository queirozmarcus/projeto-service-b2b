# ADR-011: Unificação PostgreSQL — Single Server, Dual Database

**Status:** Accepted
**Data:** 2026-04-23
**Autores:** Architect, DevOps
**Relaciona-se a:** ADR-002 (migration — shared DB inicial), ADR-008 (User service extraction), ADR-009 (User module decommission), ADR-010 (Auth proxy adapter)

> **Nota de numeração:** este ADR foi solicitado como "ADR-010" mas o número
> 010 já estava em uso (`ADR-010-auth-proxy-adapter.md`). Para preservar a
> unicidade da numeração, o documento foi registrado como **ADR-011**.

---

## Contexto

Após a extração do User Service via Strangler Fig (ADR-008) e o decomissionamento
do módulo User no monólito (ADR-009), a topologia de banco em dev/staging
manteve **dois containers PostgreSQL separados**:

| Container         | Porta host | Database          | Serviço consumidor |
|-------------------|-----------:|-------------------|--------------------|
| `postgres`        |       5432 | `scopeflow`       | monólito           |
| `scopeflow-user-db` |       5433 | `scopeflow_users` | user-service       |

Essa configuração nasceu como consequência imediata da Fase 3 da migration
(ADR-002) — *database-per-service* para o User. Em produção ela faz sentido
(isolamento físico real, blast radius reduzido, scaling independente). Em
**dev e staging**, porém, ela trouxe custo sem ganho proporcional:

### Problemas observados

1. **Overhead operacional em dev local**
   - Dois containers Postgres competindo por memória (4GB mínimo do host WSL2)
   - Duas configurações de healthcheck, duas inicializações de volume, dois
     backups, dois pontos de falha para subir a stack.
   - `./scripts/validate-qa-full.sh --with-stack` demora mais esperando dois
     bancos ficarem `healthy`.

2. **Duplicação de config sem valor real de isolamento**
   - Em dev/staging os dois containers rodam no mesmo host, na mesma rede
     Docker, com os mesmos secrets no `.env`. O "isolamento físico" existe
     apenas no papel — um `docker compose down` derruba os dois igualmente.

3. **Fricção para onboarding e troubleshooting**
   - Novo dev precisa entender por que há dois Postgres, quando conectar em
     qual, por que as migrations Flyway estão em dois lugares, por que
     `psql` direto exige trocar porta.
   - Troubleshooting de testes Testcontainers ficou mais complexo — cada
     serviço sobe seu próprio container efêmero.

4. **Divergência dev ↔ prod aceitável**
   - Em produção a premissa de DB-per-service segue válida (instâncias
     RDS/Aurora separadas, backups independentes, IAM distinto). A questão
     é se dev/staging precisam replicar isso fielmente.
   - Decisão: **não**. Dev/staging priorizam velocidade e simplicidade; o
     isolamento lógico (dois databases) é suficiente para detectar
     vazamentos de schema cross-context.

### Trigger da decisão (Sprint 9)

A Sprint 9 (validação do ecossistema Docker) expôs que a duplicação
operacional estava atrasando o ciclo de feedback dos testes locais e gerando
instabilidade em staging (falhas intermitentes de startup de um dos Postgres).
A equipe decidiu unificar.

---

## Decisão

Adotar **um único servidor PostgreSQL** hospedando **dois databases lógicos
independentes** em dev e staging:

```
postgres:5432
├── scopeflow         (owner: scopeflow, usado pelo monólito)
└── scopeflow_users   (owner: scopeflow, usado pelo user-service)
```

### O que muda

- `docker-compose.yml` e `docker-compose.staging.yml`: remoção do container
  `scopeflow-user-db`. O container `postgres` passa a criar ambos os
  databases no bootstrap (via script `init-db.sql` montado em
  `/docker-entrypoint-initdb.d/`).
- `user-service` aponta para `postgres:5432/scopeflow_users` em vez de
  `user-db:5433/scopeflow_users`.
- Healthcheck único, volume único, porta única (5432).
- Scripts de validação (`validate-qa-full.sh`, `check-stack-health.sh`) passam
  a checar um serviço de banco apenas.

### O que NÃO muda

- **Isolamento lógico preservado:** os dois databases continuam fisicamente
  separados dentro do servidor. Nenhum JOIN cross-database, nenhum schema
  compartilhado. O padrão **DB-per-service permanece conceitualmente intacto**.
- **Flyway por serviço:** cada serviço continua dono das suas migrations
  (`backend/src/main/resources/db/migration/` para o monólito;
  `user-service/src/main/resources/db/migration/` para o user-service).
- **Ownership de dados:** monólito NUNCA lê `scopeflow_users`; user-service
  NUNCA lê `scopeflow`. Comunicação cross-context segue via REST
  (ADR-010 — Auth proxy adapter).
- **Produção:** em produção a topologia continua sendo *database-per-server*
  (duas instâncias físicas separadas). Esta decisão vale apenas para
  dev/staging.

### O que NÃO foi adotado

- Database compartilhado (uma única base com schemas) — violaria o isolamento
  lógico conquistado em ADR-008.
- Migração imediata para RDS/Aurora gerenciado em dev — custo desproporcional
  para ambiente de desenvolvimento local.
- Postgres embarcado (H2, Testcontainers only) — conflita com a premissa de
  rodar a stack completa em dev para smoke tests end-to-end.

---

## Alternativas consideradas

### Opção A — Manter dois containers PostgreSQL

- **Prós:**
  - Paridade fiel com a produção (dois servidores físicos).
  - Teste de resiliência cross-DB (um pode cair sem o outro).
- **Contras:**
  - Overhead de memória, CPU e tempo de startup em dev.
  - Duplicação de config sem ganho real de isolamento (mesmo host, mesma rede).
  - Fricção para onboarding e debugging.

### Opção B — Single server, dual database (ESCOLHIDA)

- **Prós:**
  - Stack dev mais leve (1 container Postgres em vez de 2).
  - Isolamento lógico preservado (dois databases, dois Flyway).
  - Scripts de validação simplificados.
  - Ownership de dados inalterado — nenhuma mudança no código dos serviços
    além da connection string.
- **Contras:**
  - SPOF em dev/staging (queda do Postgres derruba os dois serviços).
  - Divergência controlada entre dev/staging e produção — exige um
    `docker-compose.prod.yml` (ou IaC) que replique a topologia real.
  - Risco de equipe esquecer que em prod são duas instâncias — mitigado
    documentando neste ADR e no `README.md`.

### Opção C — Database compartilhado (uma base, schemas separados)

- **Prós:**
  - Ainda mais simples (uma connection string).
  - JOINs cross-context viáveis (se alguma vez fossem desejáveis — não são).
- **Contras:**
  - **Regressão arquitetural:** desfaz o isolamento do ADR-008.
  - Risco alto de vazamento de ownership (dev tenta um JOIN por preguiça).
  - Migrations Flyway passariam a competir pelo mesmo schema default.
  - Descartada.

### Opção D — Migrar dev/staging para managed DB (RDS/Aurora, Neon, Supabase)

- **Prós:**
  - Paridade de infra com produção.
  - Backups, upgrades e alta disponibilidade gerenciados.
- **Contras:**
  - Custo recorrente em dev/staging (US$ 30–80/mês/instância mínima).
  - Dependência de rede para desenvolvimento local.
  - Tempo de provisionamento incompatível com ciclo de feedback rápido.
  - Descartada **para dev/staging**; permanece como candidata para produção.

---

## Consequências

### Positivas

- **Stack dev mais rápida:** `docker compose up` bota 1 Postgres em vez de 2;
  memória livre para outros serviços (RabbitMQ, Redis, frontend).
- **Scripts de validação simplificados:** `check-stack-health.sh` reduz de
  4 checks de banco para 3.
- **Onboarding direto:** um único Postgres, uma única porta, uma única
  credencial em dev.
- **Testes locais mais estáveis:** menos race conditions de startup entre
  containers; `Testcontainers` continua intocado (spawn próprio por teste).
- **Custo de memória reduzido** em WSL2 (~200MB economizados pelo segundo
  Postgres removido).

### Negativas e riscos

- **SPOF em dev/staging:** um incidente no Postgres derruba monólito **e**
  user-service simultaneamente. Aceitável: dev/staging não têm SLO; em
  produção a topologia real é outra.
- **Divergência dev ↔ prod:** dev roda single-server, prod roda dual-server.
  Requer que **todo** teste de resiliência cross-DB (ex: o que acontece
  quando `scopeflow_users` fica indisponível e o monólito chama auth?) seja
  feito em staging com override ou em produção canário.
- **Risco de regressão conceitual:** com um único Postgres, um dev desatento
  pode tentar um JOIN cross-database ou apontar o monólito para
  `scopeflow_users`. Mitigação:
  - Usuários de banco distintos por serviço (`scopeflow_app` vs
    `scopeflow_user_app`) com GRANTs limitados ao database próprio.
  - Code review e linter de config para bloquear connection strings
    cross-database.
- **Split futuro em produção:** quando/se decidirmos que dev/staging também
  precisam de dual-server (ex: para testar failover), exige retrabalho de
  compose. Custo estimado baixo (reverter o `init-db.sql` e reintroduzir
  o segundo serviço) — **decisão reversível**.
- **Observabilidade unificada:** um único dashboard de Postgres em dev (bom
  para simplicidade, ruim se quisermos comparar latência por database —
  resolver via label `datname` nas métricas).

---

## Plano de implementação (referência, Sprint 5 Docker Compose)

1. Criar `docker/postgres/init-db.sql` com `CREATE DATABASE scopeflow_users`
   e grants mínimos para o usuário do user-service.
2. Atualizar `docker-compose.yml`:
   - Remover serviço `scopeflow-user-db` e seu volume.
   - Montar `init-db.sql` em `/docker-entrypoint-initdb.d/` do container
     `postgres`.
   - Ajustar `user-service.environment.SPRING_DATASOURCE_URL` para
     `jdbc:postgresql://postgres:5432/scopeflow_users`.
   - Remover `depends_on: scopeflow-user-db` do user-service; manter
     `depends_on: postgres`.
3. Atualizar `docker-compose.staging.yml` na mesma linha.
4. Ajustar `scripts/check-stack-health.sh` e `scripts/validate-qa-full.sh`
   para checar apenas `postgres` (removendo bloco de `user-db`).
5. Atualizar `README.md` e `CLAUDE.md` (tabela de serviços e portas).
6. Smoke test: `./scripts/validate-qa-full.sh --with-stack` deve passar em
   ambos os ambientes.

---

## Plano de rollback

Reversão é **stateless e local ao docker-compose**:

1. Restaurar serviço `scopeflow-user-db` no `docker-compose.yml` a partir do
   git (commit anterior à Sprint 5).
2. Reverter `SPRING_DATASOURCE_URL` do user-service para `user-db:5433`.
3. Re-rodar Flyway do user-service contra o banco dedicado (migrations já
   estão idempotentes).
4. Não há perda de dados: em dev, basta recriar; em staging, pode-se
   `pg_dump` do `scopeflow_users` no postgres unificado e restaurar no
   `user-db` separado.

Critérios de acionamento:
- Incidente operacional em staging onde o SPOF do Postgres causou indisponibilidade
  simultânea que não teria ocorrido com dois containers.
- Necessidade repentina de testar failover cross-DB antes do cut-over de
  produção.

---

## Próximos passos

- **Produção permanece com dual-server:** nenhuma ação nesta ADR muda a
  topologia de produção. Manter `docs/migration/DB-MIGRATION-USER-SERVICE.md`
  como referência de cut-over.
- **Topology drift guard:** incluir no pipeline de CI um step que valida que
  `docker-compose.prod.yml` (ou equivalente IaC/Helm) mantém duas instâncias
  Postgres separadas. Evita que a simplificação de dev sangre para produção.
- **Users de banco distintos por serviço:** criar `scopeflow_user_app` no
  `init-db.sql` com GRANT apenas no database `scopeflow_users`. Fecha a
  porta para vazamentos cross-database por acidente.
- **Avaliar managed DB para staging:** quando staging passar a ter carga
  representativa, reavaliar Opção D deste ADR.
- **Observabilidade:** adicionar label `datname` nos painéis de métricas
  Postgres para permitir visão por database.

---

## Referências

- ADR-002 (migration) — Database strategy: shared DB inicial
- ADR-008 — Extração do User Service (conclusão)
- ADR-009 — Decommission do módulo User no monólito
- ADR-010 — Auth proxy adapter
- `.claude/plans/Done/sprint1-diagnostico.md` — inventário da situação anterior
- `.claude/plans/Done/sprint5-docker-compose.md` — execução da unificação
- `docs/migration/DB-MIGRATION-USER-SERVICE.md` — cut-over de produção
- `docker-compose.yml`, `docker-compose.staging.yml` — topologia pós-decisão
- `scripts/check-stack-health.sh`, `scripts/validate-qa-full.sh` — validação
