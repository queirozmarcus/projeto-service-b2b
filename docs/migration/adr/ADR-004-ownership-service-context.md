# ADR-004: Ownership de service_context_profiles e service_context_questions

**Status:** Proposto
**Data:** 2026-04-05
**Contexto:**

As tabelas `service_context_profiles` e `service_context_questions` possuem tres fontes
documentais conflitantes sobre ownership:

1. **data-ownership.md** classifica ambas como **Briefing**, argumentando que sao
   "consumidas exclusivamente pelo Briefing context para popular perguntas".
2. **coupling-matrix.md** (zona de ambiguidade #1) recomenda **Workspace**, por serem
   "configuracao do workspace exposta para Briefing via API ou evento".
3. **Migration V8 SQL** (comentario no schema) declara **Workspace**: "Ownership:
   Workspace context (created/managed by workspace owner/admin)".

A ambiguidade existe porque essas tabelas vivem na **fronteira** entre dois contextos:
- **Escrita:** administrador do workspace configura tipos de servico e templates de perguntas
- **Leitura:** Briefing context consome ao iniciar uma sessao para carregar perguntas corretas
- **FK estrutural:** `service_context_profiles.workspace_id` -> `workspaces.id` (CASCADE)
- **Nao ha API publica** para gerenciar esses templates — a logica esta embutida no Briefing service atual

A decisao impacta diretamente a ordem de extracao (ADR-001) e a estrategia de comunicacao
entre servicos (ADR-003), pois define quem carrega essas tabelas quando os contextos
forem separados em microsservicos.

**Decisao:**

Ownership atribuido ao **Workspace context**.

Justificativa:

1. **Semantica de dominio:** `service_context_profiles` sao **configuracao do workspace** —
   definem quais tipos de servico um workspace oferece, com que tom, precos e entregas.
   Configuracao de tenant e responsabilidade inequivoca do contexto que gerencia o tenant.

2. **Ciclo de vida da escrita:** Quem cria, edita e desativa profiles e o administrador
   do workspace. O Briefing context e **consumidor read-only** desses dados. O principio
   "quem escreve, e dono" e mais robusto que "quem le, e dono" para definir ownership.

3. **Acoplamento na extracao:** Quando Briefing for extraido como microsservico (passo 4
   da ordem definida no ADR-001), com ownership no Workspace:
   - Briefing service consome templates via **API sincrona** (`GET /api/v1/workspaces/{id}/service-profiles`)
     ou **evento** (`ServiceProfileUpdatedEvent`), ambos mecanismos ja previstos no ADR-003.
   - As tabelas **nao migram** junto com Briefing — permanecem no Workspace service que
     ja foi extraido no passo 2.
   - Workspace service ja estara estavel e operacional quando Briefing precisar consumir.

   Se o ownership fosse Briefing:
   - Na extracao do Workspace (passo 2), seria necessario expor as tabelas que
     pertencem a outro servico ainda nao extraido, criando dependencia circular.
   - Ou as tabelas ficariam "orfas" no monolito ate a extracao do Briefing (passo 4),
     violando o principio de que cada extracao entrega ownership completo.

4. **Coerencia com o schema:** A FK `workspace_id -> workspaces.id` com CASCADE indica
   que o ciclo de vida dos profiles esta acoplado ao workspace, nao ao briefing.
   A migration V8 corrobora com o comentario explicito de ownership.

**Alternativas consideradas:**

1. *Ownership no Briefing (status quo do data-ownership.md)*:
   Descartado. Embora Briefing seja o unico consumidor **hoje**, ownership deve refletir
   quem **administra** o dado, nao quem o consome. Atribuir ownership ao consumidor cria
   um anti-pattern: quando um segundo consumidor surgir (ex: Proposal precisa saber quais
   servicos o workspace oferece para sugerir precos), o ownership ficaria ambiguo novamente.
   Alem disso, na extracao sequencial (ADR-001), Workspace e extraido antes de Briefing —
   se as tabelas pertencem a Briefing, elas ficam "presas" no monolito ate o passo 4.

2. *Novo bounded context "ServiceCatalog"*:
   Descartado. Over-engineering para 2 tabelas com ~6 colunas cada. Um contexto proprio
   so se justificaria se houvesse logica de dominio complexa (precificacao dinamica,
   marketplace de servicos). No estagio atual do ScopeFlow, essas tabelas sao configuracao
   CRUD simples do workspace.

3. *Ownership compartilhado (co-ownership)*:
   Descartado por principio. Co-ownership viola o fundamento de single-owner tables
   estabelecido no data-ownership.md. Dois contextos acessando a mesma tabela diretamente
   cria acoplamento de dados que inviabiliza split de banco no futuro.

**Trade-offs:**

| Ganha | Perde |
|-------|-------|
| Ownership alinhado com semantica de dominio (configuracao = Workspace) | Briefing perde acesso direto (JOIN local) aos templates na extracao |
| Extracao do Workspace (passo 2) leva todas as suas tabelas de configuracao | Necessidade de API ou evento para Briefing consumir templates |
| Segundo consumidor futuro nao requer rediscussao de ownership | Latencia adicional na criacao de briefing session (REST call ou cache) |
| Coerencia entre schema (V8), coupling-matrix e decisao formal | data-ownership.md precisa ser atualizado (correcao de inconsistencia) |
| Principio "quem escreve, e dono" aplicado de forma consistente | API de gerenciamento de profiles precisa ser criada no Workspace service |

**Impacto:**

1. **Documentacao:** Atualizar `data-ownership.md` — mover `service_context_profiles` e
   `service_context_questions` de Briefing para Workspace. Marcar zona de ambiguidade #1
   do `coupling-matrix.md` como resolvida.

2. **Extraction card do Workspace (passo 2):** Incluir `service_context_profiles` e
   `service_context_questions` no escopo de extracao. O Workspace service precisa expor:
   - `GET /api/v1/workspaces/{id}/service-profiles` (lista profiles ativos)
   - `GET /api/v1/service-profiles/{id}/questions` (lista templates de perguntas)
   - CRUD completo para administradores do workspace

3. **Extraction card do Briefing (passo 4):** Substituir acesso direto as tabelas por
   chamada REST ao Workspace service (com circuit breaker, conforme ADR-003) ou cache
   local populado via evento `ServiceProfileUpdatedEvent`. A estrategia preferida e
   **cache local via evento** para evitar chamada sincrona no hot path de criacao de sessao.

4. **Codigo atual:** Refatorar o Briefing service no monolito para acessar
   `service_context_profiles` via uma interface (port) em vez de acesso direto ao
   repository. Isso prepara a substituicao futura por REST client sem mudar o domain.

5. **Workspace context:** Ownership passa de 2 para 4 tabelas: `workspaces`,
   `workspace_members`, `service_context_profiles`, `service_context_questions`.

