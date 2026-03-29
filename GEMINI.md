# ScopeFlow AI - Gemini Instructions

Este arquivo fornece as diretrizes operacionais e o contexto fundamental para o desenvolvimento e manutenção do ecossistema **ScopeFlow AI** através do Gemini CLI.

## 🚀 Visão Geral do Projeto

O **ScopeFlow AI** é uma plataforma SaaS B2B impulsionada por IA, projetada para otimizar o processo de vendas de prestadores de serviços (freelancers, agências). A plataforma transforma conversas comerciais em escopos claros e aprovados através de um fluxo de descoberta assistido.

### Core Business Domains
1. **User & Workspace:** Gestão multi-tenant e controle de acesso (RBAC).
2. **Briefing & Discovery:** Fluxo sequencial de perguntas → Respostas → Detecção de lacunas via IA.
3. **Proposals:** Geração de propostas e contratos baseados nos briefings concluídos.
4. **Client Access:** Interface pública segura para preenchimento de briefings por clientes externos.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Detalhes |
| :--- | :--- | :--- |
| **Backend** | Spring Boot 3.2+ | Java 21, Hexagonal Architecture, DDD |
| **Frontend** | Next.js 15+ | React 19, TypeScript, Tailwind CSS 4, Zustand |
| **Banco de Dados** | PostgreSQL 16 | Flyway para migrações, Hibernate 6.x |
| **Mensageria** | RabbitMQ 3.13 | Outbox Pattern para eventos assíncronos |
| **Segurança** | Spring Security 6 | JWT (Stateless), Rate Limiting (Bucket4j) |
| **IA** | OpenAI SDK | Geração de perguntas de follow-up e análise de lacunas |
| **Infra/DevOps** | Docker / K8s | Helm, Docker Compose (Local/Staging), GitHub Actions |
| **Testes** | JUnit 5 / Playwright | Testcontainers, AssertJ, Vitest |

---

## 🏗️ Arquitetura e Convenções

### Backend (Hexagonal/Ports & Adapters)
- **Domain (`core/domain`):** Lógica pura, sem dependências de frameworks. Uso de `sealed classes` para tipos seguros e `records` para Value Objects.
- **Application (`core/usecase`):** Casos de uso que orquestram a lógica de domínio.
- **Adapters (`adapter/in` e `adapter/out`):**
  - **IN:** Controllers REST (`web`), Mappers de DTO (records).
  - **OUT:** Repositórios JPA (`persistence`), Mensageria (`messaging`).
- **Padrões:** RFC 9457 (Problem Details) para erros, Imutabilidade por padrão.

### Frontend (Next.js App Router)
- **Estrutura:** `src/app` (pages), `src/components`, `src/hooks`, `src/lib`.
- **Estilo:** Tailwind CSS 4 com componentes Radix UI.
- **Estado:** Zustand para estado global leve.

### Convenções de Código
- **Idioma:** Código em Inglês; Documentação e Commits em Português (PT-BR).
- **Commits:** Padrão Conventional Commits (feat, fix, docs, refactor, chore).
- **Qualidade:** Cobertura de testes alvo > 80% (Backend e Frontend).

---

## ⚙️ Comandos de Desenvolvimento

### Backend (Java/Spring)
```bash
cd backend
./mvnw spring-boot:run          # Iniciar em dev
./mvnw clean package            # Build (Uber JAR)
./mvnw verify                   # Rodar testes (Unit + Integration com Testcontainers)
./mvnw flyway:migrate           # Aplicar migrações
```

### Frontend (Next.js)
```bash
cd frontend
npm install                     # Instalar dependências
npm run dev                     # Iniciar servidor dev
npm run build                   # Build de produção
npm run test:e2e                # Rodar testes Playwright
```

### Infraestrutura Local
```bash
docker compose up -d            # PostgreSQL, RabbitMQ, Redis
```

---

## 📋 Guia de Validação

Antes de considerar uma tarefa concluída, o Gemini deve validar:
1. **Compilação:** Backend e Frontend buildando sem erros.
2. **Testes:** Novos casos de teste adicionados e todos os testes existentes passando.
3. **Padrão Hexagonal:** Garantir que lógica de negócio não vazou para os adapters.
4. **Segurança:** Verificar isolamento de workspace (Tenant Isolation) em novos endpoints.
5. **Migrações:** Garantir que novas tabelas/campos tenham scripts Flyway correspondentes.

---
*Este arquivo é gerado e mantido como bússola para interações inteligentes no projeto ScopeFlow AI.*
