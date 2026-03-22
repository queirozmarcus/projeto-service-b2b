# Plano: Decisões Arquiteturais (ADRs) & 3 Serviços MVP

**Data:** 2026-03-22
**Relacionado:** 001-fine-planning-mvp.md, 002-prompts-and-api-contracts.md
**Status:** DECISÕES CRÍTICAS

---

## 1. Architecture Decision Records (ADRs)

### ADR-001: Monólito Modular vs Microserviços

**Decisão:** Monólito modular no MVP

**Context:**
- MVP precisa validar produto rápido (12 semanas)
- Custo de microserviços (deploy, network, consistency) é overkill
- Equipe pequena, foco em entrega

**Decision:**
- Backend = NestJS monólito com módulos isolados
- Frontend = Next.js 15 com componentes reutilizáveis
- Banco = PostgreSQL centralizado
- Queue = BullMQ para async (não precisa de microserviço separado)

**Consequences:**
- ✅ Deploy simples (1 contêiner)
- ✅ Dev rápido, sem overhead de comunicação
- ✅ Transações ACID sem saga complexity
- ❌ Escala: se crescer, precisará split depois
- ❌ Independência de deploy: mudança em auth afeta tudo

**Mitigação:**
- Arquitetura hexagonal desde o início (pronta pra split)
- Limites de módulo claros (sem acoplamento)
- Se performance cair, primeiro scale vertical (DB replica)

**Status:** ✅ APROVADO

---

### ADR-002: Versionamento de Prompts vs Feedback em Tempo Real

**Decisão:** Versionamento de prompts em arquivo + feedback via UI

**Context:**
- Prompts v1 são "boas estimativas", não são finais
- Clientes darão feedback: "perguntas muito técnicas", "muito longo"
- Queremos iterar rapidamente
- Não queremos recriar histórico (auditabilidade)

**Decision:**
- Prompts salvos em `src/adapter/out/ai/prompts/{name}_v{n}.md`
- Campo `prompt_version` em `ai_generations` table
- Feedback coletado em dashboard (sim/não, opções pré-definidas)
- Quando pronto pra v2: copy v1 → v2, edita, testa, promove

**Consequences:**
- ✅ Histórico completo de prompts
- ✅ Fácil revert se nova versão pior
- ✅ Auditável: qual prompt gerou qual resultado
- ❌ Exige processo de versionamento rigoroso
- ❌ Sem experimentação em tempo real (A/B testing complexo)

**Mitigação:**
- Script CLI `npm run prompt:version {name}` cria nova versão automaticamente
- Docs claras sobre quando criar v2
- Testes de prompt com fixtures antes de promover

**Status:** ✅ APROVADO

---

### ADR-003: IA no Backend vs Frontend

**Decisão:** IA 100% no backend

**Context:**
- Chamadas IA têm latência (5-30s)
- Precisam ser idempotentes (retry, timeout)
- Precisam ser auditáveis (logs, versioning)
- Custos devem ser controlados

**Decision:**
- Todas as chamadas IA via backend
- Frontend faz POST → backend, espera resposta
- Async jobs (PDF, kickoff) via BullMQ + worker
- Clients poolam status ou recebem webhook

**Consequences:**
- ✅ Segurança (API key no backend)
- ✅ Auditabilidade (logs, monitoring)
- ✅ Controle de custo (rate limit, throttling)
- ❌ UX: esperar 5-10s é lento (usar spinner)
- ❌ Mais requisições HTTP

**Mitigação:**
- Mostrar progresso real: "gerando perguntas... 40%"
- Async jobs: PDF, kickoff gerados em background, notifica via email
- Cache alguns resultados (mesma proposta = mesmas perguntas)

**Status:** ✅ APROVADO

---

### ADR-004: Autenticação: JWT vs Session vs OAuth

**Decisão:** JWT com refresh tokens

**Context:**
- Sem login de terceiros no MVP
- Frontend e backend estão no mesmo domínio
- Precisa de workspace segregation

**Decision:**
- Access token (JWT): 15 min, bearer header
- Refresh token (JWT): 7 dias, secure httponly cookie
- Workspace_id em token (rápido pra validar)
- Logout: blacklist refresh token em Redis (curto TTL)

**Consequences:**
- ✅ Stateless: sem sessão storage
- ✅ Workspace segregation no token
- ✅ Refresh natural sem re-login
- ❌ Logout é async (delay até expiração)
- ❌ Token revocation complexa

**Mitigação:**
- Refresh token em secure httponly cookie (evita XSS)
- Logout blacklist com TTL curto (15 min)
- Monitorar se logout é problema real

**Status:** ✅ APROVADO

---

### ADR-005: PDF Generation: Server-Side vs Client-Side

**Decisão:** Server-side async + S3 storage

**Context:**
- PDF precisa ser idêntico sempre
- Cliente tira screenshot → diferente em cada browser
- PDF vai ser enviado por email (async)
- Tamanho pode ser grande (+ imagens)

**Decision:**
- Backend: puppeteer (headless Chrome) ou pdfkit
- Input: HTML proposta + metadata
- Output: S3 file, Presigned URL (7 dias)
- Async job via BullMQ

**Consequences:**
- ✅ PDF consistente
- ✅ Enviável por email
- ✅ Auditável (arquivo salvo)
- ❌ Custo extra (puppeteer + S3)
- ❌ Complexidade de geração

**Mitigação:**
- Use puppeteer-extra (slim, ~50MB)
- Cache PDF se houver regeneração
- Fallback: HTML em browser (se PDF falhar)

**Status:** ✅ APROVADO

---

### ADR-006: Entidade de Cliente (CRM Light)

**Decisão:** Modelo simples: name, email, phone

**Context:**
- Não é CRM full, é só contato
- Não precisa de histórico completo
- MVP não tem gestão de múltiplos contatos por projeto

**Decision:**
- Tabela `clients` simples: id, workspace_id, name, email, phone
- 1 cliente = 1+ propostas
- Sem custom fields, histórico, tags
- Expandível em v2 (Fase 2)

**Consequences:**
- ✅ Simples, rápido de implementar
- ✅ Sem over-engineering
- ❌ Se cliente quiser CRM, terá que integrar terceiro
- ❌ Sem histórico de contato

**Mitigação:**
- Docs dizem: "Para CRM completo, integre Hubspot/Pipedrive"
- Pré-reservar design pra future integração

**Status:** ✅ APROVADO

---

### ADR-007: Workspace Segregation: Row-Level vs Tenant-Level

**Decisão:** Row-level segregation via workspace_id

**Context:**
- Multi-tenant SaaS: múltiplos prestadores
- Segurança crítica: não expor dados de outro workspace

**Decision:**
- Todas as queries filtram por workspace_id
- Middleware valida: usuário logado pertence a workspace?
- Prisma policy: não salvamos sem workspace_id
- Índices: (workspace_id, x) em todas as tabelas principais

**Consequences:**
- ✅ Segurança: impossível expor dados cruzados
- ✅ Multi-tenancy real
- ✅ Performance: índices pequenos
- ❌ Desenvolvimento mais cuidadoso (não esquecer workspace_id)
- ❌ Queries sempre com where clause

**Mitigação:**
- Middleware automático: injeta workspace_id do token
- Testes: sempre testar com 2+ workspaces
- Code review: verificar se workspace_id presente

**Status:** ✅ APROVADO

---

### ADR-008: Observabilidade: Logs vs Metrics vs Traces

**Decisão:** Logs estruturados (v1), métricas após validação

**Context:**
- MVP não precisa de observabilidade enterprise
- Logs são críticos (debug, audita)
- Métricas usadas após validar demanda

**Decision:**
- Logs: Winston + JSON estruturado
- Stored: arquivo local (dev), CloudWatch (prod)
- Métricas: Prometheus opcional (Sprint 6 if needed)
- Traces: não no MVP (OpenTelemetry later)

**Consequences:**
- ✅ Debug fácil
- ✅ Auditable (quem fez o quê)
- ✅ Simples de implementar
- ❌ Sem correlação entre requests
- ❌ Sem análise de performance real-time

**Mitigação:**
- Logs incluem request_id (trace)
- Dashboard simples: grep logs, count errors
- Se escalar, adiciona Prometheus

**Status:** ✅ APROVADO

---

## 2. Os 3 Serviços MVP

### Critério de Seleção
1. **Demanda validada:** mercado de microagências/freelancers alto
2. **Escopo claro:** sem ambiguidade de entregáveis
3. **Margin alto:** vale a pena vender
4. **Retenção potencial:** cliente usa todo mês
5. **Contexto bem definido:** IA consegue gerar perguntas boas

---

### Serviço 1: Social Media Management

**Slug:** `social_media_management`

**Descrição:** Gerenciar presença em redes sociais (Instagram, LinkedIn, TikTok) com conteúdo consistente.

**Público-alvo do cliente:**
- Pequeno negócio / freelancer / microagência
- Quer mais visibilidade e leads via redes
- Não tem tempo pra postar todo dia

**Entregáveis padrão:**
- Diagnóstico inicial (1 hora)
- Calendário editorial (12 semanas)
- 3 posts por semana (Instagram + LinkedIn)
- Resposta a comentários (2x por dia)
- Relatório mensal com métricas
- 1 reunião de alinhamento por mês

**Exclusões padrão:**
- Produção de fotos ou vídeos (cliente fornece ou paga extra)
- Gestão de tráfego pago / anúncios
- Análise aprofundada de concorrência
- Criação de contas novas (assume já existe)

**Timeline típica:** 3 meses mínimo, 6+ recomendado

**Preço esperado:** R$ 1.500 - R$ 3.500/mês

**Questões de Descoberta (5):**
1. Qual é o principal objetivo desta campanha? (objetivo)
2. Descreve seu cliente ideal em poucas palavras (público-alvo)
3. Qual é a maior dificuldade que você enfrenta hoje? (dores)
4. Tem alguma conta ou marca que você admira no seu nicho? (referência)
5. Tem algo importante que a gente não perguntou? (catch-all)

**Contexto para IA:**
```json
{
  "service_id": "social_media_management",
  "service_name": "Social Media Management",
  "niche": "marketing",
  "default_deliverables": [
    "Diagnóstico inicial",
    "Calendário editorial 12 semanas",
    "3 posts/semana (Instagram + LinkedIn)",
    "Resposta comentários 2x/dia",
    "Relatório mensal",
    "1 reunião alinhamento"
  ],
  "default_exclusions": [
    "Produção de fotos/vídeos",
    "Tráfego pago",
    "Análise concorrência aprofundada"
  ],
  "tone_override": null,
  "questions_template": [
    {
      "id": "q1",
      "text": "Qual é o principal objetivo desta campanha de social media?",
      "type": "open"
    },
    {
      "id": "q2",
      "text": "Descreve seu cliente ideal em poucas palavras.",
      "type": "open"
    },
    {
      "id": "q3",
      "text": "Qual é a maior dificuldade que você enfrenta hoje?",
      "type": "open"
    },
    {
      "id": "q4",
      "text": "Tem alguma conta ou marca que você admira?",
      "type": "open"
    },
    {
      "id": "q5",
      "text": "Tem algo importante que a gente não perguntou?",
      "type": "open"
    }
  ]
}
```

**Risco de Escopo:**
- Expectativa de crescimento muito agressivo (100% em 3 meses) → educate
- Budget não realista (R$ 300/mês pra 5 posts/semana) → pedir realidade

**Tipagem SQL (seed):**
```sql
INSERT INTO service_catalog (id, workspace_id, name, description)
VALUES ('svc_social_1', 'ws_default', 'Social Media Management', 'Gestão de presença em redes sociais');

INSERT INTO service_context_profiles (
  id, workspace_id, service_id,
  questions_template,
  default_deliverables,
  default_exclusions,
  tone_override,
  prompt_version
) VALUES (
  'scp_social_1',
  'ws_default',
  'svc_social_1',
  '[{"id": "q1", ...}]'::jsonb,
  '["Diagnóstico", ...]'::jsonb,
  '["Fotos/vídeos", ...]'::jsonb,
  null,
  'v1'
);
```

---

### Serviço 2: Landing Page Design

**Slug:** `landing_page_design`

**Descrição:** Criar landing page otimizada para conversão (lead capture ou venda).

**Público-alvo do cliente:**
- Startup / ecommerce / prestador de serviço
- Quer capturar leads ou vender algo específico
- Tem conteúdo mas não sabe design

**Entregáveis padrão:**
- Briefing + pesquisa (4 horas)
- Design de landing (desktop + mobile)
- Copywriting otimizado pra conversão
- Integração com email/CRM (Zapier, Make, etc)
- 2 rodadas de revisão
- Deploy em hospedagem

**Exclusões padrão:**
- Fotografia/videografia profissional
- Tráfego pago (ads)
- Manutenção contínua (apenas deploy)
- SEO avançado
- Registrar domínio (cliente faz)

**Timeline típica:** 2-3 semanas

**Preço esperado:** R$ 2.000 - R$ 5.000 (one-shot)

**Questões de Descoberta (5):**
1. Qual é o objetivo principal desta landing page? (o que vender/capturar)
2. Quem é seu cliente ideal? (público-alvo)
3. Qual é a maior dificuldade que você tem hoje? (problema que página resolve)
4. O que você quer que o visitante FAÇA quando chegar? (CTA)
5. Tem alguma referência de design que você gosta? (inspiração)

**Contexto para IA:**
```json
{
  "service_id": "landing_page_design",
  "service_name": "Landing Page Design",
  "niche": "design",
  "default_deliverables": [
    "Briefing + pesquisa",
    "Design em Figma (desktop + mobile)",
    "Copywriting otimizado",
    "Integração email/CRM",
    "2 rodadas revisão",
    "Deploy e testes"
  ],
  "default_exclusions": [
    "Fotografia/videografia profissional",
    "Tráfego pago (PPC)",
    "Manutenção contínua",
    "SEO avançado",
    "Registrar domínio"
  ],
  "tone_override": null,
  "questions_template": [...]
}
```

---

### Serviço 3: Brand Identity Design (Logo + Guidelines)

**Slug:** `brand_identity`

**Descrição:** Criar identidade visual da marca: logo, paleta, tipografia, tone of voice.

**Público-alvo do cliente:**
- Novo negócio / freelancer em crescimento
- Quer marca profissional
- Não tem recursos pra agência grande

**Entregáveis padrão:**
- Sessão discovery (2 horas)
- 3 conceitos de logo (em preto e branco)
- Paleta de cores + tipografia
- Manual de marca (1 página)
- Todos os arquivos (PNG, SVG, PDF)
- 1 revisão

**Exclusões padrão:**
- Aplicações da marca (cartão, papel, etc)
- Fotografia de marca
- Website/social media design
- Animações ou design em movimento
- Gestão de marca contínua

**Timeline típica:** 2-3 semanas

**Preço esperado:** R$ 1.500 - R$ 3.500

**Questões de Descoberta (5):**
1. Qual é a missão / razão de existir da sua marca? (propósito)
2. Quem é seu cliente ideal? (público-alvo)
3. O que diferencia você dos concorrentes? (diferencial)
4. Qual é o tom de voz que você quer passar? (personalidade)
5. Você tem cores ou estilos que você prefere? (preferência visual)

**Contexto para IA:**
```json
{
  "service_id": "brand_identity",
  "service_name": "Brand Identity",
  "niche": "design",
  "default_deliverables": [
    "Sessão discovery",
    "3 conceitos de logo",
    "Paleta de cores + tipografia",
    "Manual de marca (1 página)",
    "Arquivos (PNG, SVG, PDF)",
    "1 revisão"
  ],
  "default_exclusions": [
    "Aplicações (cartão, papel, etc)",
    "Fotografia",
    "Website/social media design",
    "Animações",
    "Gestão contínua"
  ],
  "tone_override": null,
  "questions_template": [...]
}
```

---

## 3. Matriz de Decisão: Por que estes 3?

| Critério | Social Media | Landing Page | Brand Identity |
|----------|------|------|------|
| **Demanda** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Escopo claro** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Margin** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Retenção** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ |
| **IA context** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Total** | 19 | 19 | 17 |

**Retenção baixa em Landing Page & Brand:** one-shot. Mas:
- Landing Page: cliente pode iterar (v2, v3)
- Brand: pode expandir pra social media depois (cross-sell)

**Decisão:** Começar com estes 3, validar retenção em 3 meses, depois decidir expansão.

---

## 4. Seed Data (SQL)

```sql
-- 3 Serviços + Contextos iniciais

INSERT INTO service_catalog (id, workspace_id, name, description) VALUES
  ('svc_sm_1', 'ws_demo', 'Social Media Management', 'Gerenciar presença em redes sociais'),
  ('svc_lp_1', 'ws_demo', 'Landing Page Design', 'Criar landing page otimizada'),
  ('svc_bi_1', 'ws_demo', 'Brand Identity', 'Logo, cores, guidelines');

INSERT INTO service_context_profiles (id, workspace_id, service_id, questions_template, default_deliverables, default_exclusions, prompt_version) VALUES
  (
    'scp_sm_1',
    'ws_demo',
    'svc_sm_1',
    '[
      {"id": "q1", "text": "Qual é o principal objetivo desta campanha de social media?", "type": "open"},
      {"id": "q2", "text": "Descreve seu cliente ideal em poucas palavras.", "type": "open"},
      {"id": "q3", "text": "Qual é a maior dificuldade que você enfrenta hoje?", "type": "open"},
      {"id": "q4", "text": "Tem alguma conta ou marca que você admira?", "type": "open"},
      {"id": "q5", "text": "Tem algo importante que a gente não perguntou?", "type": "open"}
    ]'::jsonb,
    '["Diagnóstico inicial", "Calendário editorial 12 semanas", "3 posts/semana", "Resposta comentários 2x/dia", "Relatório mensal", "1 reunião alinhamento"]'::jsonb,
    '["Fotos/vídeos", "Tráfego pago", "Análise concorrência aprofundada"]'::jsonb,
    'v1'
  );

-- Similar para Landing Page e Brand Identity...
```

---

## 5. Próximos Passos

**Quando aprovado:**
1. Validar os 3 serviços (podem customizar conforme nicho)
2. Seed SQL criado e migrado
3. Prompts v1 prontos com estes 3 contextos
4. Testes de exemplo usando Social Media

**Decisão:**
- Adicionar mais serviços depois de validar retenção
- Não adicionar 4º serviço até terminar Sprint 3 (briefing IA)

---

**Status:** ⏳ Aguardando aprovação dos 3 serviços MVP

