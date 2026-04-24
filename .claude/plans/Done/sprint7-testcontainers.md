# Sprint 7 — Validação Testcontainers

## Classes encontradas

| Módulo | Classe | Database name | Usa DynamicPropertySource | Referência hardcoded? |
|--------|--------|--------------|--------------------------|----------------------|
| backend | `MessagingIntegrationTestBase` | `scopeflow_test` | Sim | Não |
| backend | `BriefingIntegrationTestBase` | `scopeflow_test` | Sim | Não |
| backend | `UserServiceContractTest` | `scopeflow_test` | Sim | Não |
| user-service | `InvalidEmailValidationIntegrationTest` | `scopeflow_test` | Sim | Não |
| user-service | `AuthControllerIntegrationTest` | `scopeflow_test` | Sim | Não |
| user-service | `UserControllerIntegrationTest` | `scopeflow_test` | Sim | Não |
| user-service | `RegisterInvalidEmailE2ETest` | `scopeflow_test` | Sim | Não |

## Dependência Testcontainers

- `backend/pom.xml`: `1.19.6`
- `user-service/pom.xml`: `1.19.6`

## Conclusão

Nenhuma alteração necessária.

Todas as 7 classes usam `@DynamicPropertySource` para injetar URL, username e password via `postgres::getJdbcUrl`, `postgres::getUsername`, `postgres::getPassword`. Nenhuma referência hardcoded a `user-db`, porta `5433` ou `scopeflow_users` foi encontrada. O database name `scopeflow_test` é usado uniformemente em todos os módulos — é um nome arbitrário criado pelo próprio Testcontainers em container efêmero, sem relação com os bancos do docker-compose (`scopeflow` na porta 5432 e `scopeflow_users` na porta 5433).

Testcontainers sobem containers isolados em portas aleatórias para cada execução de teste; a unificação de servidores PostgreSQL no ambiente Docker não afeta esses containers em nenhum aspecto.

## Alterações realizadas

Nenhuma alteração necessária.
