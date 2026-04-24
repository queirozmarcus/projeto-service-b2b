# Plano: Estratégia de Prompts IA & Contratos de API

**Data:** 2026-03-22
**Relacionado:** 001-fine-planning-mvp.md
**Status:** DETALHAMENTO TÉCNICO

---

## 1. Estratégia Completa de Prompts

### 1.1 Filosofia & Princípios

**O que a IA NÃO faz:**
- Não toma decisões finais (sugestões apenas)
- Não expõe conteúdo sensível em logs
- Não substitui revisão humana
- Não tenta ser "chat genérico"

**O que a IA FAZ:**
- Adapta perguntas ao nicho e serviço
- Aprofunda respostas vagas
- Consolida informações em estrutura legível
- Sugere escopo, exclusões, premissas
- Simplifica linguagem pra aprovação
- Gera resumo de kickoff

**Critério de sucesso:**
- Reduz tempo de discovery em 60%
- Reduz ambiguidade em 80%
- Taxa de aprovação > 85%
- Confiança da IA sempre medida

---

### 1.2 Os 6 Prompts — Versão Final v1

#### PROMPT 1: Geração de Perguntas de Briefing

**Nome:** `briefing_questions_v1.md`
**Uso:** Inicial, quando cliente começa a responder
**Modelo:** GPT-4 Turbo (temperature: 0.7)
**Output:** JSON com array de perguntas

**Template:**

```markdown
# Geração de Perguntas de Briefing

Você é um consultor experiente em {{service}} para {{niche}} brasileiros.
Sua missão: gerar perguntas que descobrem necessidade real, não apenas "o que".

## Contexto
- Nicho: {{niche}}
- Serviço: {{service}}
- Ton de voz da agência: {{tone}}
- Entregáveis padrão: {{deliverables}}
- Exclusões padrão: {{exclusions}}
- Experiência: especialista em evitar escopo mal definido

## Restrição de Linguagem
- Sem jargão técnico (cliente é pequeno negócio)
- Linguagem clara, direta, PT-BR natural
- Perguntas devem instigar reflexão, não ser superficiais
- Expectativa de resposta: 50-200 palavras

## Ordem das Perguntas
1. Básico: objetivo e público-alvo (fácil, aquecimento)
2. Profundo: dores e expectativas (coloca cliente pra pensar)
3. Prático: detalhes operacionais (timing, recursos)
4. Referência: exemplos e preferências (inspiração)
5. Confirmação: tem algo que esquecemos? (fechamento)

## Formato de Resposta

Retorna JSON exatamente assim (sem markdown, sem explicação):

```json
{
  "questions": [
    {
      "id": "q1",
      "order": 1,
      "text": "Qual é o principal objetivo desta [{{service}}]?",
      "type": "open",
      "hint": "Ex: aumentar reconhecimento de marca, gerar leads, vender produto",
      "why_asking": "Entender o norte do projeto antes de tudo"
    },
    {
      "id": "q2",
      "order": 2,
      "text": "Quem é o seu cliente ideal? Descreve em poucas palavras.",
      "type": "open",
      "hint": "Ex: mulher 25-40 anos, empreendedora, online",
      "why_asking": "Escopo muda muito dependendo do público"
    },
    {
      "id": "q3",
      "order": 3,
      "text": "Qual é a maior dor que você sente hoje?",
      "type": "open",
      "hint": "Ex: falta de leads, marca confusa, concorrência, falta de tempo",
      "why_asking": "Entender a raiz do problema para escopo correto"
    },
    {
      "id": "q4",
      "order": 4,
      "text": "Você tem referência de marca / conta / site que você gosta?",
      "type": "open",
      "hint": "Ex: XYZ Company faz assim, gosto desse estilo",
      "why_asking": "Inspiração visual/tonal para o trabalho"
    },
    {
      "id": "q5",
      "order": 5,
      "text": "Tem algo importante que a gente não perguntou?",
      "type": "open",
      "hint": "Ex: sim, temos restrição de budget, urgência, etc",
      "why_asking": "Catch-all para questões não previstas"
    }
  ]
}
```

## Regras de Geração
- Sempre 5 perguntas
- Começar fácil, terminar profundo
- Sem jargão, sem números desnecessários
- Se não souber o serviço, gera genérica + adapta depois
- JSON válido SEMPRE, mesmo se criativo
```

**Exemplo de saída esperada:**

```json
{
  "questions": [
    {
      "id": "q1",
      "order": 1,
      "text": "Qual é o principal objetivo desta campanha de social media?",
      "type": "open",
      "hint": "Ex: aumentar seguidores, gerar leads, construir comunidade",
      "why_asking": "Entender o norte do projeto"
    },
    {
      "id": "q2",
      "order": 2,
      "text": "Descreve seu cliente ideal em poucas palavras.",
      "type": "open",
      "hint": "Ex: mulher 25-40, empreendedora, lifestyle",
      "why_asking": "Público muda tudo no conteúdo"
    },
    {
      "id": "q3",
      "order": 3,
      "text": "Qual é a maior dificuldade que você enfrenta hoje?",
      "type": "open",
      "hint": "Ex: falta de leads, marca confusa, concorrência",
      "why_asking": "Problema real é que o projeto deve resolver"
    },
    {
      "id": "q4",
      "order": 4,
      "text": "Tem alguma conta ou marca que você admira?",
      "type": "open",
      "hint": "Ex: @marca_x tem jeito que gosto",
      "why_asking": "Inspiração pra direção do trabalho"
    },
    {
      "id": "q5",
      "order": 5,
      "text": "Tem algo importante que a gente não perguntou?",
      "type": "open",
      "hint": "Ex: urgência, restrição, preferência",
      "why_asking": "Catch-all para questões importantes"
    }
  ]
}
```

---

#### PROMPT 2: Detecção de Respostas Vagas & Pergunta Complementar

**Nome:** `deep_question_v1.md`
**Uso:** Quando resposta tem < 20 chars ou parece incompleta
**Modelo:** GPT-4 Turbo (temperature: 0.8)
**Output:** JSON com pergunta complementar e justificativa

**Template:**

```markdown
# Pergunta Complementar para Resposta Vaga

Você é consultor experiente. Cliente respondeu algo superficial.
Seu trabalho: fazer pergunta que aprofunda SEM assustar.

## Contexto
- Serviço: {{service}}
- Pergunta original: "{{original_question}}"
- Resposta do cliente: "{{vague_answer}}"
- Histórico de respostas: {{previous_answers}}

## Tarefa
Gera pergunta que aprofunda a resposta vaga.
Deixa claro qual é a ambiguidade sem ser agressivo.

## Formato

```json
{
  "follow_up_question": "Você mencionou X, mas como isso se relaciona com Y?",
  "type": "open",
  "hint": "Ex: ...",
  "why_asking": "Entender melhor X porque disso depende o escopo"
}
```
```

---

#### PROMPT 3: Consolidação de Briefing

**Nome:** `briefing_consolidation_v1.md`
**Uso:** Após 5 respostas, antes de gerar escopo
**Modelo:** GPT-4 Turbo (temperature: 0.5)
**Output:** JSON estruturado + confidence_score

**Template:**

```markdown
# Consolidação de Briefing

Você é analisador de requisitos. Cliente respondeu 5 perguntas (+ possivelmente follow-ups).
Tarefa: consolidar respostas em briefing estruturado.
Alertar se informação crítica falta.

## Contexto
- Serviço: {{service}}
- Nicho: {{niche}}
- Respostas: {{all_answers}}

## Tarefa
Consolida em: objetivo, público-alvo, dores, expectativas, constraints.
Marca se confiança é alta (> 80%) ou se precisa revisão manual.

## Formato

```json
{
  "objective": "Aumentar reconhecimento da marca entre mulheres 25-40 no Instagram",
  "target_audience": "Mulheres 25-40 anos, empreendedoras, foco lifestyle e bem-estar",
  "pains": [
    "Falta de leads qualificados",
    "Concorrência forte no nicho",
    "Falta de tempo para criar conteúdo"
  ],
  "expectations": "Crescimento de 50% em seguidores + 20 leads/mês em 3 meses",
  "constraints": {
    "timeline": "3 meses",
    "budget_approx": "2-3k/mês",
    "content_per_week": 3
  },
  "missing_info": [
    "Não mencionou preferência de plataforma secundária (TikTok?)",
    "Budget confirmado?"
  ],
  "confidence_score": 0.82,
  "alerts": [
    "Expectativa de 50% crescimento em 3 meses é agressiva, rever",
    "Budget não foi confirmado exatamente"
  ]
}
```

Se confidence < 70%:
```json
{
  ...
  "confidence_score": 0.65,
  "alerts": [
    "BAIXA CONFIANÇA: Muita ambiguidade, recomenda revisão manual",
    "Cliente não deixou clara qual é a dor principal"
  ]
}
```
```

---

#### PROMPT 4: Geração de Escopo

**Nome:** `scope_generation_v1.md`
**Uso:** Após briefing consolidado, prestador quer gerar escopo
**Modelo:** GPT-4 Turbo (temperature: 0.6)
**Output:** JSON com escopo detalhado

**Template:**

```markdown
# Geração de Escopo com IA

Você é especialista em {{service}} com 10 anos de experiência.
Cliente foi entrevistado. Agora gera escopo detalhado, realista e lucrativo.

## Contexto
- Briefing consolidado: {{briefing}}
- Serviço: {{service}}
- Entregáveis padrão: {{default_deliverables}}
- Exclusões padrão: {{default_exclusions}}
- Workspace tone: {{tone}}

## Tarefa
Gera escopo: objetivo, entregáveis, exclusões, premissas, dependências.
Adiciona notas pra prestador revisar.

## Formato

```json
{
  "objective": "Aumentar reconhecimento de marca e gerar leads via social media",
  "deliverables": [
    "Definição de tom de voz da marca",
    "Calendário editorial de 12 semanas",
    "3 posts por semana (Instagram + LinkedIn)",
    "1 Carrossel semanal com insights",
    "Resposta a comentários (até 2x dia)",
    "Relatório mensal de performance",
    "1 Reunião de alinhamento por mês"
  ],
  "exclusions": [
    "Produção de fotos ou vídeos (cliente fornece)",
    "Gestão de tráfego pago (PPC em separado)",
    "Design de peças (usa templates)",
    "Análise de mercado (fora do escopo)"
  ],
  "assumptions": [
    "Assumo que cliente fornecerá fotos/vídeos prontos",
    "Internet e acesso às contas em dia",
    "Decisões rápidas do cliente (max 24h)"
  ],
  "dependencies": [
    "Brief claro com tom de voz",
    "Acesso às contas de social media",
    "Cliente responde em max 24h"
  ],
  "suggested_timeline": "Kick-off semana 1, primeiros posts semana 2, medição mensal",
  "notes_for_review": [
    "Cliente quer 50% crescimento em 3 meses: possível mas agressivo, requer conteúdo viral",
    "Sem orçamento pago, escopo é orgânico puro",
    "Sugiro aumentar pra 5 posts/semana se quer performance"
  ]
}
```
```

---

#### PROMPT 5: Resumo Amigável pra Aprovação

**Nome:** `approval_summary_v1.md`
**Uso:** Proposta final antes de cliente aprovar
**Modelo:** GPT-4 Turbo (temperature: 0.7)
**Output:** JSON com resumo em linguagem simples

**Template:**

```markdown
# Resumo Amigável para Aprovação

Cliente vai ver isso. TUDO deve ser claro em 5 minutos de leitura.
Sem jargão. Sem confusão. SIM ou NÃO, nada entre.

## Contexto
- Proposta técnica: {{proposal_scope}}
- Cliente: {{client_name}}
- Ton desejado: {{tone}}

## Tarefa
Simplifica proposta em: o que, quanto, quando, quem é responsável.

## Formato

```json
{
  "friendly_summary": "Vamos gerenciar suas redes sociais (Instagram e LinkedIn) criando conteúdo e comunidade. Você aumenta presença, visibilidade e gera leads qualificados.",

  "what_included": [
    "3 posts por semana (Instagram + LinkedIn)",
    "Resposta a comentários e mensagens",
    "Relatório mensal com números reais (engajamento, leads)",
    "1 reunião por mês pra alinhamento"
  ],

  "what_excluded": [
    "Fotos e vídeos você fornece (ou paga extra)",
    "Publicidade paga (anúncios no Instagram, Google, etc)",
    "Análise de concorrência aprofundada"
  ],

  "timeline_simple": "Começa 15 de Abril, primeiros posts aparecem na semana 1.",

  "your_responsibilities": [
    "Fornecer fotos/vídeos de qualidade",
    "Responder nossa mensagens em até 24h",
    "Aprovar posts antes de publicar (opcional)",
    "Estar com acesso às contas liberado"
  ],

  "investment": "R$ 2.500/mês",

  "expected_results": "50% mais seguidores, 20+ leads/mês, maior reconhecimento da marca",

  "next_steps": "Se tudo ok, assina aqui. Marcamos kick-off pra próxima segunda."
}
```
```

---

#### PROMPT 6: Resumo de Kickoff

**Nome:** `kickoff_summary_v1.md`
**Uso:** Após cliente aprovar, prestador inicia projeto
**Modelo:** GPT-4 Turbo (temperature: 0.5)
**Output:** JSON com checklist + resumo

**Template:**

```markdown
# Resumo Executivo de Kickoff

Projeto foi aprovado. Cliente quer começar.
Gera resumo executivo + checklist prático.

## Contexto
- Projeto: {{project_name}}
- Cliente: {{client_name}}
- Escopo: {{scope}}
- Timeline: {{timeline}}

## Tarefa
Checklist de kick-off: o que fazer, quem faz, quando.

## Formato

```json
{
  "executive_summary": "Social Media Management da [Cliente] começa em 15/04. Objetivo: 50% crescimento em seguidores + 20 leads/mês via Instagram e LinkedIn em 3 meses.",

  "kickoff_checklist": [
    {
      "item": "Reunião inicial com cliente (1h)",
      "owner": "Prestador",
      "deadline": "15/04 (Dia 1)"
    },
    {
      "item": "Receber fotos e vídeos iniciais",
      "owner": "Cliente",
      "deadline": "15/04"
    },
    {
      "item": "Definir tom de voz + guidelines de conteúdo",
      "owner": "Prestador (com feedback cliente)",
      "deadline": "17/04"
    },
    {
      "item": "Criar calendário editorial de 12 semanas",
      "owner": "Prestador",
      "deadline": "19/04"
    },
    {
      "item": "Publicar primeiros 3 posts",
      "owner": "Prestador",
      "deadline": "22/04 (Semana 2)"
    }
  ],

  "client_responsibilities": [
    "Fornecer fotos/vídeos de qualidade",
    "Responder em 24h quando houver dúvida",
    "Aprovar posts antes de publicar (se preferir)",
    "Disponibilizar acesso às contas"
  ],

  "prestador_responsibilities": [
    "3 posts/semana com qualidade consistente",
    "Responder comentários 2x ao dia",
    "Gerar relatório mensal",
    "Fazer 1 reunião de alinhamento por mês"
  ],

  "risk_flags": [
    "Se expectativa de crescimento 50% em 3 meses com escopo atual: improvável. Sugerir aumento de posts ou investimento em tráfego pago."
  ],

  "next_steps": [
    "Primeira reunião: confirmar dados da conta, tom, guia de conteúdo",
    "Segunda semana: primeiros posts ao vivo, medir performance",
    "Quarta semana: primeiro relatório, ajustar estratégia se necessário"
  ]
}
```
```

---

### 1.3 Prompt Versioning Strategy

**Como evoluciona:**

1. **v1** (MVP): Base solid, 5 prompts core
2. **v2** (Validação): Aperfeiçoa linguagem baseado em uso real
3. **v3+** (Expansão): Adiciona nova habilidade, novo serviço

**Quando criar nova versão:**
- Performance < 70% (IA gera genérico)
- Feedback de clientes (perguntas não fazem sentido)
- Novo serviço (landing page precisa de prompt diferente)

**Como manter histórico:**
- Arquivo imutável: `prompts/{name}_v{n}.md`
- Campo `prompt_version` em `ai_generations` table
- Se quer rodar com v2, muda lógica do orquestrador
- Antigo fica em git, nunca deleta

---

### 1.4 Monitoramento de Qualidade de IA

**Métricas:**

| Métrica | Target | Ação se falhar |
|---------|--------|---|
| Confidence score médio | > 80% | Revisar prompt, aumentar temperatura |
| Taxa de respostas vagas | < 5% | Prompt não profunda bem, loop infinito |
| Usuário edita escopo IA | < 30% edições | IA tá acertando, bom |
| Tempo IA generação | < 5s | Se > 10s, verificar cota OpenAI |
| Taxa de aprovação (cliente) | > 85% | Se < 70%, proposta genérica |

---

## 2. Contratos de API — Detalhado

### 2.1 Convenções Globais

**Base URL:** `https://api.scopeflow.local/api/v1` (local) ou `https://app.scopeflow.com/api/v1` (prod)

**Autenticação:**
- Header: `Authorization: Bearer {jwt_access_token}`
- Refresh endpoint: `POST /auth/refresh` com `refresh_token` no body

**Rate Limiting:**
- Autenticado: 1000 req/min
- Público (approval link): 50 req/min por IP

**Resposta padrão (sucesso):**
```json
{
  "data": { /* payload */ },
  "meta": { "timestamp": "2025-01-15T10:30:00Z" },
  "error": null
}
```

**Resposta padrão (erro):**
```json
{
  "data": null,
  "meta": { "timestamp": "2025-01-15T10:30:00Z" },
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Email is required",
    "details": [
      { "field": "email", "reason": "required" }
    ]
  }
}
```

---

### 2.2 Auth Endpoints

#### `POST /auth/register`

**Descrição:** Criar usuário + workspace inicial

**Request:**
```json
{
  "name": "Marcus Silva",
  "email": "marcus@agencia.com",
  "password": "SenhaForte123!",
  "workspaceName": "Minha Agência",
  "nichePrimary": "marketing"
}
```

**Response 201:**
```json
{
  "data": {
    "user": {
      "id": "user_abc123",
      "name": "Marcus Silva",
      "email": "marcus@agencia.com"
    },
    "workspace": {
      "id": "ws_abc123",
      "name": "Minha Agência",
      "slug": "minha-agencia",
      "nichePrimary": "marketing"
    },
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 900
  },
  "meta": {},
  "error": null
}
```

---

#### `POST /auth/login`

**Request:**
```json
{
  "email": "marcus@agencia.com",
  "password": "SenhaForte123!"
}
```

**Response 200:**
```json
{
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 900,
    "user": {
      "id": "user_abc123",
      "name": "Marcus Silva",
      "email": "marcus@agencia.com"
    }
  },
  "meta": {},
  "error": null
}
```

---

### 2.3 Briefing Endpoints

#### `POST /briefing/{proposalId}/start`

**Descrição:** Inicia fluxo de briefing, gera perguntas com IA

**Requires:** `Authorization` header, proposalId owned by workspace

**Response 200:**
```json
{
  "data": {
    "briefingSessionId": "bs_abc123",
    "publicToken": "eyJhbGc...",
    "publicUrl": "https://app.scopeflow.com/public/briefing/bs_abc123?token=eyJhbGc...",
    "questions": [
      {
        "id": "q1",
        "order": 1,
        "text": "Qual é o principal objetivo?",
        "type": "open",
        "hint": "Ex: aumentar reconhecimento"
      },
      ...
    ],
    "totalQuestions": 5
  },
  "meta": {},
  "error": null
}
```

---

#### `POST /public/briefing/{sessionId}/answers`

**Descrição:** Cliente submete respostas (endpoint público, token-validated)

**Auth:** `?token={publicToken}` em query string

**Request:**
```json
{
  "answers": [
    {
      "questionId": "q1",
      "answer": "Aumentar presença na Instagram e gerar 20 leads por mês"
    },
    {
      "questionId": "q2",
      "answer": "Mulheres entre 25-40 anos que trabalham com bem-estar"
    },
    ...
  ]
}
```

**Response 200:**
```json
{
  "data": {
    "briefingSessionId": "bs_abc123",
    "answersReceived": 5,
    "completionStatus": "complete",
    "nextQuestion": null,
    "message": "Briefing completo! Agora vamos consolidar."
  },
  "meta": {},
  "error": null
}
```

Ou se resposta vaga:
```json
{
  "data": {
    "briefingSessionId": "bs_abc123",
    "answersReceived": 4,
    "completionStatus": "asking_follow_up",
    "nextQuestion": {
      "id": "follow_1",
      "text": "Você mencionou '20 leads', como você mede isso? Email, telefone, forma de contato?"
    }
  },
  "meta": {},
  "error": null
}
```

---

#### `GET /briefing/{sessionId}/summary`

**Descrição:** Prestador vê resumo consolidado de briefing

**Requires:** `Authorization`, sessionId owned by workspace

**Response 200:**
```json
{
  "data": {
    "briefingSessionId": "bs_abc123",
    "status": "complete",
    "consolidatedBrief": {
      "objective": "Aumentar reconhecimento de marca via social media",
      "target_audience": "Mulheres 25-40 anos, bem-estar",
      "pains": ["Falta de leads", "Concorrência forte"],
      "expectations": "50% crescimento em 3 meses",
      "constraints": {
        "timeline": "3 meses",
        "budget": "2-3k/mês"
      },
      "missing_info": [],
      "confidence_score": 0.89
    },
    "aiGeneration": {
      "id": "ai_gen_123",
      "type": "briefing_consolidation",
      "promptVersion": "v1",
      "generatedAt": "2025-01-15T10:30:00Z"
    }
  },
  "meta": {},
  "error": null
}
```

---

### 2.4 Proposal Endpoints

#### `POST /proposals/{briefingSessionId}/generate-scope`

**Descrição:** IA gera escopo sugerido a partir de briefing

**Request:**
```json
{}
```

**Response 202 (Accepted — async job):**
```json
{
  "data": {
    "proposalId": "prop_abc123",
    "scopeJobId": "job_123",
    "status": "generating",
    "estimatedSeconds": 5
  },
  "meta": {},
  "error": null
}
```

Cliente pooling em `/proposals/{proposalId}/scope-status?jobId=job_123`

**Response 200 (quando pronto):**
```json
{
  "data": {
    "proposalId": "prop_abc123",
    "scope": {
      "objective": "Aumentar reconhecimento e leads via social media",
      "deliverables": [
        "Calendário editorial 12 semanas",
        "3 posts/semana",
        "Resposta a comentários 2x/dia",
        "Relatório mensal"
      ],
      "exclusions": [
        "Produção de fotos/vídeos",
        "Tráfego pago"
      ],
      "assumptions": [
        "Cliente fornece fotos/vídeos"
      ],
      "dependencies": [
        "Acesso às contas"
      ]
    },
    "status": "draft",
    "notes": [
      "Expectativa de 50% crescimento é agressiva",
      "Considerar aumentar para 5 posts/semana"
    ]
  },
  "meta": {},
  "error": null
}
```

---

#### `PUT /proposals/{proposalId}/scope`

**Descrição:** Prestador edita escopo antes de gerar proposta

**Request:**
```json
{
  "objective": "Aumentar reconhecimento e gerar 20 leads/mês via social media",
  "deliverables": [
    "Calendário editorial 12 semanas",
    "5 posts/semana (Instagram + LinkedIn)",
    "Resposta a comentários 2x/dia",
    "Relatório mensal",
    "1 reunião de alinhamento"
  ],
  "exclusions": [
    "Produção de fotos/vídeos",
    "Tráfego pago",
    "Análise aprofundada de concorrência"
  ]
}
```

**Response 200:**
```json
{
  "data": {
    "proposalId": "prop_abc123",
    "versionNumber": 2,
    "scope": { /* edited scope */ },
    "status": "draft"
  },
  "meta": {},
  "error": null
}
```

---

#### `POST /proposals/{proposalId}/render`

**Descrição:** Renderiza proposta em HTML a partir do template

**Request:**
```json
{
  "templateId": "tpl_default_social_media",
  "scopeVersionId": 2,
  "pricing": 2500,
  "timeline": "15 de Abril - 15 de Julho"
}
```

**Response 202 (Async):**
```json
{
  "data": {
    "proposalId": "prop_abc123",
    "renderJobId": "job_456",
    "status": "rendering",
    "estimatedSeconds": 3
  },
  "meta": {},
  "error": null
}
```

**Response 200 (quando pronto):**
```json
{
  "data": {
    "proposalId": "prop_abc123",
    "htmlContent": "<html>...</html>",
    "versionNumber": 2,
    "status": "ready_for_approval",
    "previewUrl": "https://app.scopeflow.com/proposals/prop_abc123/preview"
  },
  "meta": {},
  "error": null
}
```

---

### 2.5 Approval Endpoints

#### `GET /public/proposals/{proposalId}/approve`

**Descrição:** Cliente vê proposta pra aprovar (endpoint público)

**Query:** `?token={publicToken}`

**Response 200:**
```json
{
  "data": {
    "proposalId": "prop_abc123",
    "proposal": {
      "scope": { /* objetivo, entregáveis, exclusões */ },
      "htmlContent": "<html>...</html>"
    },
    "friendlySummary": {
      "friendly_summary": "Vamos gerenciar suas redes sociais...",
      "what_included": ["3 posts/semana", ...],
      "what_excluded": ["Fotos/vídeos", ...],
      "timeline_simple": "Começa 15/04",
      "investment": "R$ 2.500/mês"
    }
  },
  "meta": {},
  "error": null
}
```

---

#### `POST /public/proposals/{proposalId}/approve`

**Descrição:** Cliente aprova proposta, rastreia metadados

**Request:**
```json
{
  "approverName": "João Silva",
  "approverEmail": "joao@empresa.com",
  "token": "eyJhbGc..."
}
```

**Response 201:**
```json
{
  "data": {
    "approvalId": "appr_abc123",
    "proposalId": "prop_abc123",
    "approvedAt": "2025-01-15T10:45:00Z",
    "approverName": "João Silva",
    "ipAddress": "192.168.1.1",
    "message": "Proposta aprovada com sucesso!"
  },
  "meta": {},
  "error": null
}
```

Backend neste ponto:
1. Salva Approval record (com IP, UA)
2. Cria ProposalEvent type=approved
3. Enqueue: PDF generation, kickoff summary, email notification
4. Retorna sucesso

---

### 2.6 Dashboard & Metrics

#### `GET /dashboard/metrics`

**Descrição:** Prestador vê dashboard de propostas

**Response 200:**
```json
{
  "data": {
    "metrics": {
      "total_proposals": 12,
      "approved": 10,
      "pending": 2,
      "rejected": 0,
      "approval_rate": 0.833,
      "avg_time_to_approval_days": 4.2,
      "briefing_completion_rate": 0.95
    },
    "recent_proposals": [
      {
        "id": "prop_abc123",
        "client": "João Silva",
        "service": "Social Media",
        "status": "approved",
        "approvedAt": "2025-01-15T10:45:00Z"
      }
    ]
  },
  "meta": {},
  "error": null
}
```

---

## 3. Próximos Passos

Este documento define:
✅ Todos os 6 prompts IA com exemplos
✅ Estratégia de versioning
✅ Monitoramento de qualidade
✅ Todos os endpoints principais
✅ Request/response contracts

**Quando aprovado, vai pra Execução em Sprint 1.**

