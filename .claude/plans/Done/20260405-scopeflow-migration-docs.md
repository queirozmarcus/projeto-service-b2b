──────────────────────────────────
Plano: Revisão dos Extraction Cards + Atualização do README
Projeto: ScopeFlow AI (projeto-service-b2b)
Data: 2026-04-05
Status: CONCLUÍDO ✅

[Contexto]
docs/migration/ tem 8 arquivos em 3 subdiretórios. Os extraction cards
02-workspace, 03-proposal e 04-briefing foram criados nesta sessão sem
revisão formal. O README atual não indexa nenhum dos documentos existentes
(ADRs, coupling-matrix, extraction-cards).

[Etapas]
1. Revisão dos Extraction Cards (02, 03, 04)
   → Agent: tech-lead (Migration Pack — Opus)
   → Input: extraction-cards/02-workspace.md, 03-proposal.md, 04-briefing.md
   → Entregável: revisão crítica — gaps, inconsistências, pré-requisitos
                 faltando, ordem de extração, riscos não mapeados

2. Atualizar README
   → Agent: domain-analyst (Migration Pack — Sonnet)
   → Depende de: Etapa 1 (incorporar correções antes de indexar)
   → Entregável: README como índice completo e navegável
     - Seção "Índice de Documentos" (ADRs + coupling-matrix + extraction cards)
     - Tabela de extraction cards: prioridade, risco, status
     - Próximos Passos atualizado
     - Caminho de leitura recomendado (novo)
   → O que NÃO muda: resumo executivo, contextos, estratégia de split,
                      comandos úteis, referências

[Riscos]
- tech-lead pode identificar inconsistências que exijam edição nos cards
  antes de indexar no README

[Decisões]
- tech-lead (Opus) para revisão estratégica — valida Strangler Fig e sequência
- domain-analyst (Sonnet) para documentação — indexação e estrutura
──────────────────────────────────
