# 🚀 Plan: User Service Extraction (20 Sprints)

Este plano detalha a extração cirúrgica do bounded context **User (Auth)** do monólito para um microserviço independente usando o padrão **Strangler Fig**.

---

## 🏗️ Bloco A: Fundação e Domínio (Sprints 1-4)
*Foco: Criar o "coração" do novo serviço sem dependências externas.*

1.  **Sprint 01 - Estrutura Hexagonal:** Criar a estrutura de pacotes `com.scopeflow.user` (domain, application, adapter, config) e o `pom.xml` final com dependências mínimas.
2.  **Sprint 02 - Core Domain Model:** Implementar a entidade `User` (agregado) e Value Objects (`Email`, `PasswordHash`, `UserId`) usando `records` e `sealed classes` do Java 21.
3.  **Sprint 03 - Domain Rules & Enums:** Definir as regras de estado (`UserStatus`) e permissões (`UserRole`) puras, sem frameworks.
4.  **Sprint 04 - RFC 9457 Error Handling:** Criar o `GlobalExceptionHandler` e as exceções de domínio (`UserNotFound`, `InvalidCredentials`) seguindo o padrão de Problem Details.

## 🗄️ Bloco B: Persistência e Adapters (Sprints 5-8)
*Foco: Conectar o domínio ao banco de dados compartilhado.*

5.  **Sprint 05 - JPA Entity Mapping:** Criar a `JpaUserEntity` no `adapter.out.persistence` mapeando para a tabela `users` já existente no monólito.
6.  **Sprint 06 - Outbound Port & Repository:** Implementar o `UserRepository` (Port) e o `JpaUserRepositoryAdapter` (Adapter) com Spring Data JPA.
7.  **Sprint 07 - Shared DB Configuration:** Configurar o `application.yml` para conectar ao PostgreSQL compartilhado com `flyway.enabled=false` (evitar conflito de migrações).
8.  **Sprint 08 - Integration Test (Persistence):** Criar testes de integração usando **Testcontainers** para validar que o microsserviço lê/escreve corretamente na tabela do monólito.

## 🔑 Bloco C: Autenticação e JWT (Sprints 9-12)
*Foco: Implementar a segurança de forma idêntica ao monólito.*

9.  **Sprint 09 - JWT Secret Sync:** Implementar o `JwtService` que consome o `JWT_SECRET` compartilhado via variáveis de ambiente.
10. **Sprint 10 - JwtAuthenticationFilter:** Criar o filtro de segurança que intercepta as requisições e extrai o contexto do usuário do token.
11. **Sprint 11 - Spring Security 6 Config:** Configurar o `SecurityFilterChain` como *stateless* e definir as permissões básicas dos endpoints de Auth.
12. **Sprint 12 - Password Encoding Paridade:** Garantir que o `BCryptPasswordEncoder` use a mesma força de hash do monólito para não invalidar senhas existentes.

## 🌐 Bloco D: Endpoints e Use Cases (Sprints 13-16)
*Foco: Expor as funcionalidades para o mundo externo.*

13. **Sprint 13 - Use Case: User Registration:** Implementar o serviço de aplicação para criar novos usuários (`POST /api/v1/auth/register`).
14. **Sprint 14 - Use Case: Authentication:** Implementar o fluxo de login (`POST /api/v1/auth/login`) gerando o par Access/Refresh Token.
15. **Sprint 15 - REST AuthController:** Criar o controller que expõe os endpoints de Auth seguindo o contrato exato do monólito.
16. **Sprint 16 - Use Case: Me & Profile:** Implementar o endpoint `GET /api/v1/users/me` para retornar os dados do usuário autenticado.

## Bridge Bridge E: A "Ponte" e Cut-over (Sprints 17-20)
*Foco: O estrangulamento final e a troca de tráfego.*

17. **Sprint 17 - Monolith Feature Toggle:** Implementar no monólito a flag `auth.service.use-extracted` via `application.yml` ou System Property.
18. **Sprint 18 - Monolith Proxy (AuthV2):** Criar o controller no monólito que, se a flag estiver ativa, redireciona as chamadas de Auth para o `user-service` via REST.
19. **Sprint 19 - Contract Validation:** Rodar os testes de contrato (Spring Cloud Contract) para garantir que o monólito aceita o JWT gerado pelo microsserviço.
20. **Sprint 20 - Cut-over & Cleanup:** Ativar a flag em Staging, validar os logs e atualizar o ADR de arquitetura oficializando a extração.

---
**Status:** ✅ CONCLUÍDO (20/20 sprints)
**Data início:** 2026-04-07
**Data conclusão:** 2026-04-11
