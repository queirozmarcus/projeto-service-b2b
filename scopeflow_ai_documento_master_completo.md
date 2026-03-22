# ScopeFlow AI — Documento Master Completo

## 1. Resumo executivo

O **ScopeFlow AI** é um SaaS para pequenos prestadores de serviço B2B no qual a **IA conduz o alinhamento de escopo, estrutura o briefing com base em nicho e serviço, gera uma proposta mais clara e facilita uma aprovação rastreável com menos desgaste**.

A tese central do produto é simples: o problema não está apenas em “fazer proposta”, mas em transformar uma conversa comercial confusa em um escopo claro, aprovado e pronto para kickoff.

O produto foi pensado para um nicho inicial de **microagências e freelancers de marketing, design, social media, landing pages e web**, com possibilidade de expansão posterior para consultoria, dev freelancers, produtoras e outros prestadores de serviço B2B.

Este documento consolida:
- visão e posicionamento do produto;
- papel da IA no fluxo;
- MVP refinado;
- requisitos funcionais e não funcionais;
- backlog priorizado;
- arquitetura técnica inicial;
- roadmap e recomendações de execução;
- wireframes textuais;
- banco de dados inicial em SQL;
- contratos de API do MVP.

---

## 2. Visão do produto

### 2.1 Nome provisório
**ScopeFlow AI**

### 2.2 Subtítulo
**A IA organiza o briefing, alinha o escopo e deixa a aprovação mais clara.**

### 2.3 Elevator pitch
Uma plataforma SaaS para pequenos prestadores de serviço B2B em que a IA entrevista, organiza, clarifica e transforma o contexto do cliente em briefing estruturado, escopo claro, proposta compreensível e aprovação rastreável.

### 2.4 O problema real
Pequenos prestadores costumam vender serviços com um processo improvisado:
- conversa em WhatsApp;
- áudio solto;
- PDF genérico;
- briefing superficial;
- escopo mal delimitado;
- aprovação informal.

Isso leva a:
- retrabalho;
- desalinhamento;
- conflito sobre o que está incluso;
- cliente inseguro;
- execução mal iniciada;
- desgaste relacional.

### 2.5 A solução
O produto cria um fluxo guiado em que a IA:
- entende o contexto do prestador;
- adapta o briefing ao nicho e ao serviço;
- identifica ambiguidades;
- pede esclarecimentos;
- sugere escopo, premissas e exclusões;
- ajuda a produzir uma proposta objetiva;
- facilita uma aprovação rastreável e mais leve;
- gera um resumo de kickoff.

---

## 3. Posicionamento

### 3.1 O que o produto é
- copiloto comercial-operacional com IA;
- plataforma de alinhamento de escopo;
- estruturador de briefing;
- gerador de proposta contextualizada;
- ponte entre venda e kickoff.

### 3.2 O que o produto não é
- CRM completo;
- ERP;
- sistema financeiro;
- plataforma jurídica completa;
- gestão avançada de projetos;
- simples editor bonito de proposta.

### 3.3 Tese de diferenciação
O diferencial não é apenas “fazer proposta”.  
O diferencial é usar **IA especializada para reduzir ambiguidade comercial antes do projeto começar**.

---

## 4. Público-alvo

### 4.1 Usuário pagante
- freelancer;
- microagência;
- consultor;
- pequeno estúdio;
- prestador de serviços digitais B2B.

### 4.2 Usuário indireto
- cliente do prestador;
- decisor de pequena empresa;
- gestor;
- dono de negócio;
- responsável comercial ou de marketing.

### 4.3 Nicho inicial recomendado
**Microagências e freelancers de marketing, design, social media, landing page e web.**

### 4.4 Expansões futuras
- consultoria;
- tráfego pago;
- desenvolvimento sob demanda;
- produtoras de vídeo;
- arquitetos;
- software houses pequenas.

---

## 5. Proposta de valor

### 5.1 Valor central
Transformar uma venda subjetiva e desgastante em um fluxo guiado, inteligente e rastreável:

**contexto do prestador + serviço + IA + briefing estruturado + escopo claro + aprovação segura**

### 5.2 Benefícios para o prestador
- economiza tempo;
- reduz retrabalho;
- reduz conflito de escopo;
- melhora a imagem profissional;
- organiza a transição da venda para execução;
- aumenta a clareza da proposta.

### 5.3 Benefícios para o cliente
- entende melhor o que está contratando;
- responde briefing de forma mais fácil;
- aprova com mais segurança;
- tem menos dúvida sobre entregáveis e limites;
- recebe um resumo mais claro do combinado.

---

## 6. Papel da IA no produto

### 6.1 O que a IA faz
A IA será usada para:
- adaptar perguntas ao nicho e ao serviço;
- aprofundar respostas vagas;
- organizar briefing;
- detectar lacunas de informação;
- sugerir objetivo, entregáveis, premissas e exclusões;
- simplificar a linguagem da proposta;
- produzir um resumo claro para aprovação;
- gerar um resumo de kickoff após aceite.

### 6.2 O que a IA não faz
- não substitui a revisão do prestador;
- não aprova proposta em nome do prestador;
- não assume responsabilidade comercial;
- não vira “chat genérico” sem contexto;
- não toma decisões finais automaticamente em fluxos críticos.

### 6.3 Fontes de contexto da IA
- nicho do prestador;
- tipo de serviço;
- entregáveis padrão;
- exclusões padrão;
- tom de comunicação;
- riscos recorrentes;
- respostas do cliente;
- restrições de prazo, orçamento e escopo.

### 6.4 Saídas da IA
- briefing adaptativo;
- perguntas complementares;
- resumo estruturado;
- escopo inicial;
- exclusões sugeridas;
- proposta mais clara;
- resumo explicativo de aprovação;
- resumo executivo de kickoff.

---

## 7. Fluxo macro do produto

1. prestador configura nicho e serviços;
2. IA monta o fluxo de descoberta;
3. cliente responde perguntas guiadas;
4. IA detecta lacunas e complementa;
5. IA consolida o briefing;
6. IA sugere escopo inicial;
7. prestador revisa;
8. sistema gera proposta;
9. cliente visualiza e aprova;
10. sistema registra o aceite;
11. IA gera resumo de kickoff.

---

## 8. Fluxo detalhado com IA

### 8.1 Configuração do prestador
O prestador informa:
- nicho;
- serviços;
- entregáveis comuns;
- exclusões recorrentes;
- linguagem;
- riscos típicos;
- perguntas frequentes.

A IA usa isso para construir um **perfil operacional do prestador**.

### 8.2 Seleção do serviço
O prestador escolhe um serviço, como:
- social media;
- landing page;
- identidade visual;
- consultoria;
- diagnóstico;
- site institucional.

A IA troca o roteiro conforme o serviço.

### 8.3 Coleta guiada com o cliente
O cliente recebe uma experiência orientada, não apenas um formulário:
- perguntas sequenciais;
- linguagem simples;
- pequenos esclarecimentos;
- aprofundamento quando necessário.

### 8.4 Briefing estruturado
A IA organiza as respostas em blocos como:
- objetivo;
- contexto;
- dores;
- público-alvo;
- entregáveis esperados;
- restrições;
- prazo;
- dependências;
- riscos percebidos.

### 8.5 Alinhamento de escopo
A IA propõe:
- escopo base;
- entregáveis;
- exclusões;
- premissas;
- dependências;
- pontos de possível divergência futura.

### 8.6 Revisão humana
O prestador revisa:
- briefing;
- escopo;
- linguagem;
- preço;
- prazo;
- limitações.

### 8.7 Proposta
A proposta final inclui:
- problema/objetivo;
- escopo;
- entregáveis;
- exclusões;
- responsabilidades;
- prazo;
- investimento;
- aceite.

### 8.8 Aprovação
O cliente visualiza uma proposta clara, com resumo amigável do que está sendo contratado, e aprova com rastreabilidade:
- nome;
- e-mail;
- data/hora;
- IP;
- versão aprovada.

### 8.9 Kickoff
Após aprovação, a IA gera:
- resumo executivo do projeto;
- checklist de kickoff;
- pendências do cliente;
- resumo de escopo aprovado.

---

## 9. MVP refinado

### 9.1 Objetivo do MVP
Validar se pequenos prestadores pagam por uma ferramenta que usa IA para:
- melhorar o briefing;
- alinhar escopo;
- gerar proposta clara;
- reduzir atrito na aprovação;
- organizar o início do projeto.

### 9.2 O que entra no MVP

#### Fundação
- autenticação;
- workspace;
- membros;
- clientes;
- catálogo de serviços;
- templates de proposta.

#### Núcleo IA
- perfil do prestador;
- contexto por serviço;
- briefing adaptativo;
- perguntas complementares;
- resumo estruturado;
- geração de escopo;
- exclusões e premissas sugeridas;
- proposta a partir do escopo revisado.

#### Aprovação
- link público;
- resumo amigável;
- aceite rastreável;
- histórico de versões;
- eventos.

#### Pós-aprovação
- resumo executivo;
- checklist básico de kickoff;
- exportação PDF.

### 9.3 O que fica fora do MVP
- CRM completo;
- cobrança;
- assinatura eletrônica avançada;
- gestão completa de projeto;
- integração WhatsApp oficial;
- automações enterprise;
- analytics avançado;
- machine learning próprio;
- negociação automática de change request.

---

## 10. Objetivos do produto

### 10.1 Objetivos de negócio
- validar demanda real;
- conseguir primeiros clientes pagantes;
- provar valor da IA no alinhamento de escopo;
- criar base de templates e contexto por nicho;
- preparar escalabilidade gradual.

### 10.2 Objetivos do usuário
#### Prestador
- ganhar tempo;
- vender com mais clareza;
- reduzir retrabalho;
- reduzir atrito com cliente;
- iniciar melhor os projetos.

#### Cliente
- entender claramente o que está contratando;
- responder briefing sem fricção;
- aprovar com segurança;
- saber suas responsabilidades.

---

## 11. Hipóteses do produto

### 11.1 Hipótese principal
Se a IA usar nicho, serviço e contexto do cliente para conduzir briefing e escopo, então pequenos prestadores perceberão maior clareza, menos retrabalho e mais facilidade de aprovação, aumentando a propensão a pagar pelo produto.

### 11.2 Hipóteses secundárias
- briefing guiado gera menor abandono;
- proposta derivada de briefing assistido reduz ambiguidade;
- resumo amigável aumenta compreensão e confiança;
- nichos de marketing/design/web terão boa adesão inicial.

---

## 12. Requisitos funcionais

### RF01 — Cadastro e autenticação
Permitir cadastro, login, logout e recuperação de senha.

### RF02 — Workspace
Permitir criação e administração de workspace.

### RF03 — Membros e papéis
Permitir papéis básicos:
- owner;
- admin;
- member.

### RF04 — Clientes
Permitir CRUD de clientes.

### RF05 — Catálogo de serviços
Permitir cadastrar serviços/pacotes com descrição, entregáveis e observações.

### RF06 — Perfil do prestador
Permitir cadastrar:
- nicho;
- linguagem;
- riscos recorrentes;
- exclusões;
- perguntas frequentes.

### RF07 — Contexto por serviço
Permitir associar a cada serviço:
- entregáveis padrão;
- exclusões padrão;
- premissas;
- riscos;
- perguntas iniciais.

### RF08 — Templates de proposta
Permitir criar, editar, duplicar e reutilizar templates.

### RF09 — Briefing adaptativo
Gerar e adaptar o briefing com base em nicho, serviço e respostas.

### RF10 — Perguntas complementares
A IA deve detectar respostas vagas ou insuficientes e sugerir aprofundamentos.

### RF11 — Resumo estruturado
Consolidar briefing em estrutura clara.

### RF12 — Geração de escopo
Gerar escopo inicial contendo:
- objetivo;
- entregáveis;
- exclusões;
- premissas;
- dependências;
- riscos.

### RF13 — Revisão humana
Permitir revisão e edição obrigatória da saída da IA.

### RF14 — Geração de proposta
Gerar proposta a partir do escopo revisado.

### RF15 — Link público
Gerar link público para visualização da proposta.

### RF16 — Resumo amigável para aprovação
Exibir versão mais clara e objetiva da proposta para o cliente.

### RF17 — Aprovação rastreável
Registrar aprovação com metadados.

### RF18 — Histórico de versões
Guardar as versões de briefing, escopo e proposta.

### RF19 — Eventos
Registrar eventos relevantes do fluxo.

### RF20 — Resumo de kickoff
Gerar resumo executivo do projeto após aprovação.

### RF21 — Exportação PDF
Permitir exportação da proposta e do resumo final.

### RF22 — Dashboard simples
Permitir acompanhar status das propostas.

---

## 13. Requisitos não funcionais

### RNF01 — Usabilidade
Interface simples para usuários não técnicos.

### RNF02 — Performance
Páginas principais rápidas e tempos aceitáveis para geração de IA.

### RNF03 — Segurança
HTTPS, autenticação segura, RBAC, segregação por workspace.

### RNF04 — Privacidade
Tratamento adequado de dados e adequação mínima à LGPD.

### RNF05 — Escalabilidade
Arquitetura que permita crescer gradualmente.

### RNF06 — Observabilidade
Logs, métricas e rastreamento de erros.

### RNF07 — Editabilidade
Toda saída da IA deve ser editável.

### RNF08 — Confiabilidade
IA assistiva, nunca definitiva por padrão.

### RNF09 — Explicabilidade mínima
Permitir que o prestador entenda a lógica geral das sugestões da IA.

### RNF10 — Disponibilidade
Meta inicial compatível com SaaS pequeno com backups e monitoramento básico.

---

## 14. Regras de negócio

### RN01
Toda proposta pertence a um workspace.

### RN02
Toda proposta deve estar associada a um cliente e a um serviço.

### RN03
Toda saída da IA deve poder ser revisada manualmente.

### RN04
A versão enviada ao cliente deve ser congelada.

### RN05
A aprovação deve estar associada à versão congelada.

### RN06
Mudanças materiais após aprovação exigem nova validação.

### RN07
A IA não pode aprovar proposta em nome do prestador.

### RN08
Somente usuários autorizados podem editar serviços, templates e propostas.

---

## 15. Backlog priorizado

### 15.1 Priorização MoSCoW

#### Must Have
- autenticação;
- workspace;
- clientes;
- serviços/pacotes;
- perfil do prestador;
- briefing adaptativo;
- resumo estruturado;
- geração de escopo;
- revisão humana;
- proposta final;
- link público;
- aprovação rastreável;
- histórico de versões;
- resumo de kickoff;
- PDF.

#### Should Have
- templates por nicho;
- dashboard básico;
- notificações por e-mail;
- duplicação de proposta;
- checklist configurável.

#### Could Have
- score de risco de escopo;
- sugestões por histórico;
- branding avançado;
- comentários internos;
- lógica condicional mais sofisticada.

#### Won't Have no MVP
- CRM completo;
- cobrança;
- assinatura eletrônica avançada;
- gestão completa do projeto;
- integrações enterprise;
- WhatsApp oficial.

---

## 16. Épicos e histórias

### Épico 1 — Fundação
#### Histórias
- Como usuário, quero criar uma conta para acessar o sistema.
- Como owner, quero configurar meu workspace.
- Como owner, quero convidar membros.

### Épico 2 — Clientes e serviços
#### Histórias
- Como prestador, quero cadastrar clientes.
- Como prestador, quero cadastrar serviços.
- Como prestador, quero definir entregáveis e exclusões padrão.

### Épico 3 — Perfil operacional
#### Histórias
- Como prestador, quero registrar meu nicho.
- Como prestador, quero registrar tom de comunicação.
- Como prestador, quero registrar riscos recorrentes.

### Épico 4 — Briefing com IA
#### Histórias
- Como prestador, quero que o sistema gere perguntas adequadas ao serviço.
- Como cliente, quero responder um briefing simples e guiado.
- Como sistema, quero aprofundar respostas vagas.
- Como prestador, quero ver o resumo consolidado.

### Épico 5 — Escopo com IA
#### Histórias
- Como prestador, quero receber um escopo sugerido.
- Como prestador, quero ver entregáveis, exclusões e premissas.
- Como prestador, quero editar tudo antes de enviar.

### Épico 6 — Proposta
#### Histórias
- Como prestador, quero gerar uma proposta a partir do escopo.
- Como prestador, quero usar templates.
- Como prestador, quero duplicar propostas.

### Épico 7 — Aprovação
#### Histórias
- Como cliente, quero ver a proposta por link simples.
- Como cliente, quero ver um resumo claro do combinado.
- Como cliente, quero aprovar com poucos cliques.
- Como prestador, quero registrar o aceite com rastreabilidade.

### Épico 8 — Kickoff
#### Histórias
- Como prestador, quero receber um resumo executivo do projeto aprovado.
- Como prestador, quero um checklist de kickoff.
- Como equipe, quero exportar proposta e resumo em PDF.

### Épico 9 — Métricas
#### Histórias
- Como prestador, quero acompanhar status das propostas.
- Como prestador, quero ver taxa de aprovação.
- Como prestador, quero identificar pendências.

---

## 17. Planejamento por sprint

### Sprint 1 — Base
- cadastro/login;
- recuperação de senha;
- workspace;
- papéis;
- clientes.

### Sprint 2 — Serviços e contexto
- serviços/pacotes;
- perfil do prestador;
- contexto por serviço;
- templates.

### Sprint 3 — Briefing IA
- briefing público;
- perguntas adaptativas;
- armazenamento de respostas;
- resumo consolidado.

### Sprint 4 — Escopo e proposta
- geração de escopo;
- revisão humana;
- proposta;
- versionamento.

### Sprint 5 — Aprovação
- página pública da proposta;
- resumo amigável;
- aceite rastreável;
- histórico de eventos.

### Sprint 6 — Pós-aprovação
- resumo de kickoff;
- PDF;
- dashboard simples;
- notificações.

---

## 18. Critérios de aceite principais

### Bloco Briefing IA
- sistema adapta perguntas por serviço;
- sistema faz aprofundamento quando necessário;
- respostas ficam consolidadas em estrutura legível.

### Bloco Escopo
- IA gera objetivo, entregáveis, exclusões, premissas e dependências;
- usuário consegue editar tudo;
- sistema salva a versão revisada.

### Bloco Aprovação
- cliente visualiza a proposta por link;
- cliente aprova com nome e e-mail;
- sistema registra data/hora, IP e versão.

### Bloco Kickoff
- sistema gera resumo executivo;
- sistema gera checklist inicial;
- sistema permite exportação em PDF.

---

## 19. Arquitetura técnica do MVP

### 19.1 Princípios
- começar simples;
- monólito modular;
- IA como capacidade do produto, não sistema separado;
- forte versionamento dos artefatos;
- editabilidade total;
- baixo custo operacional no início.

### 19.2 Componentes principais
1. Frontend Web  
2. Backend API  
3. Módulo de IA/orquestração  
4. Banco relacional  
5. Storage de arquivos  
6. Fila assíncrona leve  
7. Observabilidade

### 19.3 Stack sugerida
#### Recomendação para MVP rápido
- Frontend: Next.js + TypeScript
- Backend: NestJS + TypeScript
- Banco: PostgreSQL
- ORM: Prisma
- Storage: S3
- Auth: JWT + refresh token
- Queue: BullMQ/Redis
- PDF: geração server-side
- IA: integração via API com LLM

#### Alternativa
- Backend em Spring Boot, mantendo o restante próximo.

### 19.4 Módulos do backend
- auth
- workspace
- clients
- services
- briefing
- ai
- proposals
- approvals
- pdf
- notifications
- analytics

### 19.5 Modelo lógico principal
#### Entidades
- users
- workspaces
- workspace_members
- clients
- service_catalog
- service_context_profiles
- proposal_templates
- proposals
- proposal_versions
- briefing_sessions
- briefing_answers
- ai_generations
- approvals
- proposal_events
- files

---

## 20. Fluxo técnico da IA

### 20.1 Briefing
1. usuário escolhe serviço;
2. backend carrega contexto;
3. módulo de IA gera perguntas;
4. cliente responde;
5. sistema mede completude;
6. IA aprofunda se necessário;
7. backend salva briefing consolidado.

### 20.2 Escopo
1. briefing consolidado é enviado à IA;
2. contexto do serviço é agregado;
3. IA gera escopo inicial;
4. sistema salva rascunho;
5. usuário revisa;
6. versão revisada vira base da proposta.

### 20.3 Aprovação
1. proposta revisada é publicada;
2. link com token é gerado;
3. cliente visualiza resumo + proposta;
4. cliente aprova;
5. backend registra metadados;
6. pós-processamento gera resumo de kickoff e PDFs.

---

## 21. Estratégia de prompts

### 21.1 Fontes de contexto
- nicho;
- tipo de serviço;
- entregáveis padrão;
- exclusões padrão;
- tom;
- briefing;
- objetivo do projeto.

### 21.2 Tipos de prompt
- geração de perguntas;
- aprofundamento;
- consolidação de briefing;
- geração de escopo;
- simplificação de linguagem para aprovação;
- resumo de kickoff.

### 21.3 Regras de implementação
- prompts versionados;
- saída estruturada em JSON sempre que possível;
- fallback para falha ou baixa confiança;
- logs técnicos sem exposição desnecessária de conteúdo sensível.

---

## 22. Segurança, privacidade e governança

### 22.1 Segurança
- autenticação segura;
- RBAC;
- rate limit em páginas públicas;
- proteção de tokens;
- segregação por workspace;
- criptografia em trânsito.

### 22.2 Privacidade
- coleta mínima necessária;
- política de privacidade clara;
- tratamento de briefing e aprovações com cautela;
- alinhamento mínimo com LGPD.

### 22.3 Governança das saídas de IA
- revisão humana obrigatória;
- histórico das gerações;
- rastreabilidade por versão;
- IA tratada como assistência.

---

## 23. Observabilidade

### 23.1 Logs
- autenticação;
- geração de briefing;
- geração de escopo;
- envio de proposta;
- aprovação;
- PDF;
- falhas.

### 23.2 Métricas
- tempo de geração de IA;
- taxa de falha;
- taxa de conclusão do briefing;
- taxa de aprovação;
- tempo até aprovação;
- uso por workspace.

### 23.3 Alertas
- falha de IA;
- falha de PDF;
- pico de erros em endpoints públicos.

---

## 24. Métricas de sucesso

### 24.1 Produto
- taxa de conclusão do briefing;
- taxa de aprovação;
- tempo médio até aprovação;
- número médio de edições após geração da IA;
- frequência de uso por workspace.

### 24.2 Negócio
- conversão trial → pago;
- retenção mensal;
- receita recorrente;
- CAC por canal;
- feedback qualitativo.

### 24.3 Valor percebido
- redução de retrabalho;
- redução de conflito de escopo;
- aumento de clareza;
- melhora do kickoff.

---

## 25. Riscos e mitigação

### Risco 1
IA gerar respostas genéricas.  
**Mitigação:** nicho inicial bem definido, contexto por serviço, revisão humana.

### Risco 2
Usuário achar que PDF/Docs já resolvem.  
**Mitigação:** enfatizar clareza de escopo, briefing guiado e rastreabilidade.

### Risco 3
Produto ficar amplo demais.  
**Mitigação:** manter foco em pré-venda e pré-kickoff.

### Risco 4
Custo de inferência alto.  
**Mitigação:** usar IA apenas em pontos críticos, contexto resumido e limites por plano.

### Risco 5
Baixa retenção.  
**Mitigação:** templates por nicho, reaproveitamento, histórico e valor claro na operação.

---

## 26. Estratégia de entrada no mercado

### 26.1 Melhor recorte inicial
**Microagências e freelancers de social media, design e landing pages.**

### 26.2 Melhor mensagem de lançamento
**“A IA organiza o briefing, alinha o escopo e deixa a aprovação mais clara.”**

### 26.3 Canais
- SEO;
- conteúdo com intenção alta;
- templates gratuitos;
- creators do nicho;
- comunidades de freelancers/agências.

### 26.4 Conteúdos com potencial de aquisição
- modelo de proposta comercial;
- briefing para social media;
- como evitar retrabalho com cliente;
- como aprovar escopo sem confusão;
- checklist de kickoff.

---

## 27. Roadmap

### Fase 1 — MVP
- fundação;
- briefing adaptativo;
- escopo com IA;
- proposta;
- aprovação;
- kickoff;
- PDF.

### Fase 2 — Validação expandida
- templates por nicho mais ricos;
- score de risco de escopo;
- melhores dashboards;
- branding;
- notificações mais inteligentes.

### Fase 3 — Expansão
- memória por cliente;
- sugestões por histórico;
- integrações externas;
- suporte a change requests;
- recursos colaborativos de kickoff.

---

## 28. Recomendação final de execução

A melhor forma de começar é:
1. fechar o nicho inicial;
2. mapear 3 a 5 serviços muito bem;
3. construir um monólito modular;
4. lançar com poucos clientes reais;
5. medir onde a IA gera mais valor;
6. expandir apenas depois de validar retenção.

O produto fica mais forte quando a IA não tenta fazer tudo.  
Ela deve atuar exatamente onde gera vantagem real:
- discovery;
- clareza;
- alinhamento;
- estruturação;
- síntese.

---

## 29. Wireframes textuais do MVP

## 29.1 Tela — Login
**Objetivo:** autenticar usuário.

**Blocos:**
- logo do produto
- campo e-mail
- campo senha
- botão entrar
- link esqueci minha senha
- link criar conta

**Ações:**
- login
- recuperação de senha
- navegação para cadastro

---

## 29.2 Tela — Cadastro / Onboarding inicial
**Objetivo:** criar conta e iniciar workspace.

**Blocos:**
- nome do usuário
- e-mail
- senha
- nome do workspace
- nicho principal
- botão continuar

**Saídas:**
- usuário criado
- workspace criado
- perfil inicial do prestador registrado

---

## 29.3 Tela — Dashboard
**Objetivo:** visão geral.

**Blocos:**
- header com workspace e usuário
- cards:
  - propostas em rascunho
  - enviadas
  - aprovadas
  - expiradas
- lista de propostas recentes
- atalho para:
  - novo cliente
  - novo serviço
  - nova proposta
- seção de atividades recentes

**Ações:**
- abrir proposta
- criar nova proposta
- filtrar por status

---

## 29.4 Tela — Clientes
**Objetivo:** gerenciar clientes.

**Blocos:**
- barra de busca
- botão novo cliente
- tabela/lista de clientes
- nome
- empresa
- e-mail
- telefone
- quantidade de propostas
- ações: editar, arquivar, abrir

**Tela de detalhe do cliente:**
- dados do cliente
- histórico de propostas
- observações

---

## 29.5 Tela — Serviços
**Objetivo:** gerenciar catálogo de serviços.

**Blocos:**
- lista de serviços
- botão novo serviço
- nome do serviço
- tipo/categoria
- preço base opcional
- status ativo/inativo

**Detalhe do serviço:**
- descrição
- entregáveis padrão
- exclusões padrão
- riscos comuns
- perguntas iniciais
- tom de linguagem sugerido

---

## 29.6 Tela — Perfil do prestador
**Objetivo:** fornecer contexto para IA.

**Blocos:**
- nicho principal
- nichos secundários
- tom de comunicação
- riscos recorrentes
- exclusões recorrentes
- perguntas frequentes
- diferenciais do prestador
- botão salvar

---

## 29.7 Tela — Criar proposta
**Objetivo:** iniciar fluxo comercial.

**Passo 1**
- escolher cliente
- escolher serviço
- escolher template
- botão continuar

**Passo 2**
- contexto inicial opcional
- objetivo do projeto
- observações adicionais
- botão iniciar briefing

---

## 29.8 Tela — Briefing guiado
**Objetivo:** coletar informações do cliente.

**Versão cliente pública**

**Blocos:**
- cabeçalho com nome do projeto/serviço
- texto curto explicando o processo
- pergunta atual
- campo de resposta
- botão próximo
- barra de progresso

**Comportamentos:**
- IA gera pergunta seguinte
- IA aprofunda quando necessário
- cliente pode voltar
- rascunho salvo automaticamente

---

## 29.9 Tela — Resumo do briefing
**Objetivo:** consolidar informações.

**Blocos:**
- objetivo do projeto
- contexto atual
- dores
- público-alvo
- entregáveis esperados
- restrições
- prazo
- dependências
- riscos percebidos

**Ações:**
- editar
- pedir nova sugestão da IA
- seguir para escopo

---

## 29.10 Tela — Escopo sugerido pela IA
**Objetivo:** revisar escopo.

**Blocos:**
- objetivo
- escopo proposto
- entregáveis
- exclusões
- premissas
- dependências
- riscos
- observações da IA

**Ações:**
- editar campo a campo
- regenerar seção específica
- aprovar escopo para proposta

---

## 29.11 Tela — Proposta
**Objetivo:** montar proposta final.

**Blocos:**
- título da proposta
- resumo do problema
- objetivo
- escopo
- entregáveis
- exclusões
- responsabilidades do cliente
- prazo
- investimento
- validade
- observações finais

**Ações:**
- salvar rascunho
- versionar
- visualizar versão pública
- publicar

---

## 29.12 Tela — Visualização pública da proposta
**Objetivo:** permitir entendimento claro pelo cliente.

**Blocos:**
- logo/nome do prestador
- título
- resumo amigável do projeto
- o que está incluso
- o que não está incluso
- prazo
- investimento
- responsabilidades do cliente
- botão aprovar

**Ações:**
- aprovar
- baixar PDF
- voltar ao resumo

---

## 29.13 Tela — Aprovação
**Objetivo:** registrar aceite.

**Blocos:**
- resumo final
- confirmação de entendimento
- nome
- e-mail
- checkbox de aceite
- botão aprovar

**Resultado:**
- aceite registrado
- tela de confirmação

---

## 29.14 Tela — Kickoff
**Objetivo:** consolidar base operacional.

**Blocos:**
- resumo executivo do projeto
- checklist inicial
- pendências do cliente
- entregáveis aprovados
- riscos observados
- botão exportar PDF

---

## 30. Banco de dados inicial em SQL

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL UNIQUE,
    logo_url TEXT,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    niche_primary VARCHAR(120),
    tone_of_voice VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL CHECK (role IN ('owner', 'admin', 'member')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, user_id)
);

CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    company_name VARCHAR(150),
    email VARCHAR(255),
    phone VARCHAR(40),
    notes TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE service_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(80),
    description TEXT,
    base_price NUMERIC(12,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE service_context_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES service_catalog(id) ON DELETE CASCADE,
    deliverables_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    exclusions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    risks_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    questions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    assumptions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE proposal_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    template_json JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    service_id UUID NOT NULL REFERENCES service_catalog(id) ON DELETE RESTRICT,
    template_id UUID REFERENCES proposal_templates(id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('draft', 'sent', 'viewed', 'approved', 'expired', 'archived')),
    public_token VARCHAR(100) UNIQUE,
    total_amount NUMERIC(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    valid_until DATE,
    current_version_number INT NOT NULL DEFAULT 1,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE proposal_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    briefing_summary_json JSONB,
    scope_json JSONB,
    proposal_json JSONB NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (proposal_id, version_number)
);

CREATE TABLE briefing_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('not_started', 'in_progress', 'completed')),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE briefing_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    briefing_session_id UUID NOT NULL REFERENCES briefing_sessions(id) ON DELETE CASCADE,
    question_key VARCHAR(120) NOT NULL,
    question_text TEXT NOT NULL,
    answer_text TEXT,
    answer_json JSONB,
    sequence_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    proposal_id UUID REFERENCES proposals(id) ON DELETE CASCADE,
    generation_type VARCHAR(40) NOT NULL CHECK (
        generation_type IN ('briefing_questions', 'briefing_summary', 'scope_generation', 'approval_summary', 'kickoff_summary')
    ),
    input_json JSONB NOT NULL,
    output_json JSONB NOT NULL,
    prompt_version VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    proposal_version_id UUID NOT NULL REFERENCES proposal_versions(id) ON DELETE RESTRICT,
    approved_name VARCHAR(120) NOT NULL,
    approved_email VARCHAR(255) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    approved_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE proposal_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_payload JSONB,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    proposal_id UUID REFERENCES proposals(id) ON DELETE CASCADE,
    file_type VARCHAR(40) NOT NULL CHECK (file_type IN ('logo', 'attachment', 'proposal_pdf', 'kickoff_pdf')),
    file_name VARCHAR(255) NOT NULL,
    storage_key TEXT NOT NULL,
    content_type VARCHAR(120),
    file_size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clients_workspace ON clients(workspace_id);
CREATE INDEX idx_services_workspace ON service_catalog(workspace_id);
CREATE INDEX idx_proposals_workspace ON proposals(workspace_id);
CREATE INDEX idx_proposals_client ON proposals(client_id);
CREATE INDEX idx_proposal_versions_proposal ON proposal_versions(proposal_id);
CREATE INDEX idx_briefing_sessions_proposal ON briefing_sessions(proposal_id);
CREATE INDEX idx_ai_generations_proposal ON ai_generations(proposal_id);
CREATE INDEX idx_proposal_events_proposal ON proposal_events(proposal_id);
```

---

## 31. Contratos de API do MVP

### Convenções
- Base URL: `/api/v1`
- Autenticação interna: `Authorization: Bearer <token>`
- Páginas públicas usam `public_token`
- Formato de resposta padrão:

```json
{
  "data": {},
  "meta": {},
  "error": null
}
```

- Formato de erro padrão:

```json
{
  "data": null,
  "meta": {},
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid payload",
    "details": []
  }
}
```

---

### 31.1 Auth

#### POST `/api/v1/auth/register`
**Descrição:** criar usuário e workspace inicial.

**Request**
```json
{
  "name": "Marcus",
  "email": "marcus@email.com",
  "password": "SenhaForte123",
  "workspaceName": "Minha Agência",
  "nichePrimary": "marketing"
}
```

**Response 201**
```json
{
  "data": {
    "user": {
      "id": "uuid",
      "name": "Marcus",
      "email": "marcus@email.com"
    },
    "workspace": {
      "id": "uuid",
      "name": "Minha Agência",
      "slug": "minha-agencia"
    },
    "accessToken": "jwt",
    "refreshToken": "jwt"
  },
  "meta": {},
  "error": null
}
```

#### POST `/api/v1/auth/login`
**Request**
```json
{
  "email": "marcus@email.com",
  "password": "SenhaForte123"
}
```

**Response 200**
```json
{
  "data": {
    "accessToken": "jwt",
    "refreshToken": "jwt",
    "user": {
      "id": "uuid",
      "name": "Marcus",
      "email": "marcus@email.com"
    }
  },
  "meta": {},
  "error": null
}
```

#### POST `/api/v1/auth/forgot-password`
**Request**
```json
{
  "email": "marcus@email.com"
}
```

**Response 200**
```json
{
  "data": {
    "message": "Recovery instructions sent if the account exists."
  },
  "meta": {},
  "error": null
}
```

---

### 31.2 Workspace

#### GET `/api/v1/workspace/me`
**Response 200**
```json
{
  "data": {
    "id": "uuid",
    "name": "Minha Agência",
    "slug": "minha-agencia",
    "nichePrimary": "marketing",
    "toneOfVoice": "consultivo"
  },
  "meta": {},
  "error": null
}
```

#### PUT `/api/v1/workspace/me`
**Request**
```json
{
  "name": "Minha Agência",
  "nichePrimary": "marketing",
  "toneOfVoice": "consultivo"
}
```

---

### 31.3 Clients

#### GET `/api/v1/clients`
**Query params**
- `search`
- `page`
- `pageSize`

**Response 200**
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "Ana",
      "companyName": "Clínica Ana",
      "email": "ana@clinica.com"
    }
  ],
  "meta": {
    "page": 1,
    "pageSize": 20,
    "total": 1
  },
  "error": null
}
```

#### POST `/api/v1/clients`
**Request**
```json
{
  "name": "Ana",
  "companyName": "Clínica Ana",
  "email": "ana@clinica.com",
  "phone": "81999999999",
  "notes": "Cliente prospect"
}
```

#### GET `/api/v1/clients/{clientId}`
#### PUT `/api/v1/clients/{clientId}`
#### DELETE `/api/v1/clients/{clientId}`

---

### 31.4 Services

#### GET `/api/v1/services`
#### POST `/api/v1/services`

**Request**
```json
{
  "name": "Gestão de Instagram",
  "category": "social-media",
  "description": "Pacote mensal de gestão",
  "basePrice": 1500.00
}
```

#### GET `/api/v1/services/{serviceId}`
#### PUT `/api/v1/services/{serviceId}`

#### PUT `/api/v1/services/{serviceId}/context`
**Descrição:** atualizar contexto que alimenta a IA.

**Request**
```json
{
  "deliverables": ["12 posts", "8 stories"],
  "exclusions": ["tráfego pago", "captação de vídeo"],
  "risks": ["cliente sem material visual", "aprovação lenta"],
  "questions": [
    "Qual o objetivo principal da presença digital?",
    "Quem aprova o conteúdo?"
  ],
  "assumptions": ["cliente fornecerá identidade visual"]
}
```

---

### 31.5 Proposal templates

#### GET `/api/v1/proposal-templates`
#### POST `/api/v1/proposal-templates`

**Request**
```json
{
  "name": "Template Social Media",
  "templateJson": {
    "sections": ["problem", "scope", "deliverables", "pricing"]
  }
}
```

#### PUT `/api/v1/proposal-templates/{templateId}`
#### POST `/api/v1/proposal-templates/{templateId}/duplicate`

---

### 31.6 Proposals

#### POST `/api/v1/proposals`
**Descrição:** criar proposta base.

**Request**
```json
{
  "clientId": "uuid",
  "serviceId": "uuid",
  "templateId": "uuid",
  "title": "Gestão de Instagram - Clínica Ana",
  "validUntil": "2026-04-10"
}
```

**Response 201**
```json
{
  "data": {
    "id": "uuid",
    "status": "draft",
    "currentVersionNumber": 1
  },
  "meta": {},
  "error": null
}
```

#### GET `/api/v1/proposals`
#### GET `/api/v1/proposals/{proposalId}`
#### PUT `/api/v1/proposals/{proposalId}`

#### POST `/api/v1/proposals/{proposalId}/duplicate`
#### POST `/api/v1/proposals/{proposalId}/publish`

**Response 200**
```json
{
  "data": {
    "id": "uuid",
    "status": "sent",
    "publicToken": "public-token-123"
  },
  "meta": {},
  "error": null
}
```

---

### 31.7 Briefing IA

#### POST `/api/v1/proposals/{proposalId}/briefing/generate`
**Descrição:** gerar perguntas iniciais do briefing.

**Request**
```json
{
  "initialContext": "Cliente quer melhorar presença digital e gerar mais leads."
}
```

**Response 200**
```json
{
  "data": {
    "questions": [
      {
        "key": "project_goal",
        "text": "Qual é o principal objetivo do projeto?",
        "type": "text"
      },
      {
        "key": "approval_owner",
        "text": "Quem será responsável por aprovar os conteúdos?",
        "type": "text"
      }
    ]
  },
  "meta": {
    "promptVersion": "briefing_questions_v1"
  },
  "error": null
}
```

#### POST `/api/v1/proposals/{proposalId}/briefing/answers`
**Descrição:** salvar respostas e pedir aprofundamento, se necessário.

**Request**
```json
{
  "answers": [
    {
      "questionKey": "project_goal",
      "questionText": "Qual é o principal objetivo do projeto?",
      "answerText": "Quero vender mais."
    }
  ]
}
```

**Response 200**
```json
{
  "data": {
    "status": "in_progress",
    "followUpQuestions": [
      {
        "key": "sales_target_detail",
        "text": "Quando você diz vender mais, quer aumentar leads, agendamentos ou vendas diretas?",
        "type": "text"
      }
    ]
  },
  "meta": {},
  "error": null
}
```

#### POST `/api/v1/proposals/{proposalId}/briefing/complete`
**Descrição:** consolidar briefing.

**Response 200**
```json
{
  "data": {
    "summary": {
      "goal": "Aumentar agendamentos",
      "context": "Clínica com presença irregular no Instagram",
      "constraints": ["Pouco material em vídeo"],
      "dependencies": ["Cliente precisa aprovar conteúdos semanalmente"]
    }
  },
  "meta": {
    "promptVersion": "briefing_summary_v1"
  },
  "error": null
}
```

---

### 31.8 Scope IA

#### POST `/api/v1/proposals/{proposalId}/scope/generate`
**Descrição:** gerar escopo inicial com IA.

**Response 200**
```json
{
  "data": {
    "scope": {
      "objective": "Aumentar agendamentos via Instagram",
      "deliverables": ["12 posts", "8 stories"],
      "exclusions": ["Tráfego pago", "Captação de vídeo"],
      "assumptions": ["Cliente fornecerá materiais base"],
      "dependencies": ["Aprovação em até 48h"],
      "risks": ["Atraso em aprovações pode afetar calendário"]
    }
  },
  "meta": {
    "promptVersion": "scope_generation_v1"
  },
  "error": null
}
```

#### PUT `/api/v1/proposals/{proposalId}/scope`
**Descrição:** salvar versão revisada manualmente.

**Request**
```json
{
  "scope": {
    "objective": "Aumentar agendamentos via Instagram",
    "deliverables": ["12 posts", "8 stories", "1 reunião mensal"],
    "exclusions": ["Tráfego pago", "Captação de vídeo"],
    "assumptions": ["Cliente fornecerá materiais base"],
    "dependencies": ["Aprovação em até 48h"],
    "risks": ["Atraso em aprovações pode afetar calendário"]
  }
}
```

---

### 31.9 Proposal generation

#### POST `/api/v1/proposals/{proposalId}/proposal/generate`
**Descrição:** gerar proposta final a partir do escopo revisado.

**Response 200**
```json
{
  "data": {
    "proposal": {
      "title": "Gestão de Instagram - Clínica Ana",
      "summary": "Projeto mensal para melhorar presença digital e aumentar agendamentos.",
      "scope": "...",
      "pricing": {
        "amount": 1500.00,
        "currency": "BRL"
      }
    }
  },
  "meta": {
    "promptVersion": "proposal_generation_v1"
  },
  "error": null
}
```

#### PUT `/api/v1/proposals/{proposalId}/proposal`
**Descrição:** salvar versão final editada pelo usuário.

---

### 31.10 Public proposal

#### GET `/api/v1/public/proposals/{publicToken}`
**Descrição:** visualizar proposta pública.

**Response 200**
```json
{
  "data": {
    "title": "Gestão de Instagram - Clínica Ana",
    "friendlySummary": "Você está contratando uma gestão mensal de Instagram com 12 posts e 8 stories.",
    "included": ["12 posts", "8 stories", "1 reunião mensal"],
    "excluded": ["Tráfego pago", "Captação de vídeo"],
    "price": {
      "amount": 1500.00,
      "currency": "BRL"
    },
    "validUntil": "2026-04-10"
  },
  "meta": {},
  "error": null
}
```

---

### 31.11 Approval

#### POST `/api/v1/public/proposals/{publicToken}/approve`
**Request**
```json
{
  "approvedName": "Ana Souza",
  "approvedEmail": "ana@clinica.com",
  "acceptTerms": true
}
```

**Response 200**
```json
{
  "data": {
    "status": "approved",
    "approvedAt": "2026-03-22T15:30:00Z"
  },
  "meta": {},
  "error": null
}
```

**Erros possíveis**
- `PROPOSAL_EXPIRED`
- `PROPOSAL_ALREADY_APPROVED`
- `INVALID_PUBLIC_TOKEN`
- `VALIDATION_ERROR`

---

### 31.12 Kickoff

#### POST `/api/v1/proposals/{proposalId}/kickoff/generate`
**Descrição:** gerar resumo de kickoff.

**Response 200**
```json
{
  "data": {
    "kickoffSummary": {
      "projectObjective": "Aumentar agendamentos via Instagram",
      "approvedDeliverables": ["12 posts", "8 stories", "1 reunião mensal"],
      "clientPendingItems": ["Enviar identidade visual", "Enviar fotos institucionais"],
      "executionRisks": ["Atraso na aprovação de conteúdo"]
    }
  },
  "meta": {
    "promptVersion": "kickoff_summary_v1"
  },
  "error": null
}
```

---

### 31.13 PDFs

#### GET `/api/v1/proposals/{proposalId}/pdf`
#### GET `/api/v1/proposals/{proposalId}/kickoff-pdf`

**Response 200**
```json
{
  "data": {
    "url": "https://storage.example.com/proposal.pdf"
  },
  "meta": {},
  "error": null
}
```

---

## 32. Próximos artefatos recomendados

Depois deste documento, os próximos entregáveis ideais são:
- wireframes visuais de baixa fidelidade;
- collection Postman/Insomnia;
- esquema Prisma ou JPA;
- prompts-base por nicho;
- plano de go-to-market de 30 dias.

---

## 33. Conclusão

O **ScopeFlow AI** tem potencial porque ataca uma dor frequente, subatendida e economicamente relevante: a bagunça entre conversa comercial, briefing, escopo e aprovação. A IA entra de forma útil e defensável ao reduzir ambiguidade antes da execução, em vez de virar apenas um chatbot genérico.

Se o produto mantiver foco no recorte inicial, com IA assistiva, revisão humana forte e proposta de valor clara, ele tem boa chance de validar rapidamente e evoluir com consistência.
