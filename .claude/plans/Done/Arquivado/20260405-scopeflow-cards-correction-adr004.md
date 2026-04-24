──────────────────────────────────
Plano: ADR-004 + Correção dos 4 Extraction Cards
Projeto: ScopeFlow AI (projeto-service-b2b)
Data: 2026-04-05
Status: CONCLUÍDO ✅
Duração: ~10 min
5 etapas concluídas (2 paralelas + 3 sequenciais)

[Contexto]
Relatório do tech-lead identificou 3 bloqueadores críticos, 15 gaps e 17 recomendações
nos extraction cards 01–04. ADR-004 pendente resolve ownership ambíguo de
service_context_profiles/questions (3 fontes conflitantes). Cards ainda não executados
— custo zero de corrigir agora.

[Etapas]
1. Criar ADR-004 [paralelo com Etapa 2]
   → Agent: tech-lead (Opus)
   → Entregável: docs/migration/adr/ADR-004-ownership-service-context.md
   → Decisão: Briefing vs Workspace com trade-offs documentados

2. Corrigir card 01 (User) [paralelo com Etapa 1]
   → Agent: backend-engineer
   → Correção: P-01 — adicionar endpoints de gestão de usuário
     (GET /users/by-email/{email}, POST /users/invited)
   → API Gateway como pré-requisito explícito (infra base)

3. Corrigir card 02 (Workspace) [após Etapa 1]
   → Agent: backend-engineer
   → Correções: G-02.1 a G-02.5

4. Corrigir card 03 (Proposal) [após Etapa 1]
   → Agent: backend-engineer
   → Correções: G-03.1 a G-03.5 + R-01 + R-02 + R-03

5. Corrigir card 04 (Briefing) [após Etapa 1]
   → Agent: backend-engineer
   → Correções: G-04.1 a G-04.5 + R-04 + R-05

[Riscos]
- ADR-004 pode definir ownership em Workspace — exigiria ajuste adicional no card 04

[Decisões]
- Aplicar todas as 17 recomendações (não só bloqueadores) — cards não executados
- Etapas 1+2 paralelas; 3/4/5 aguardam ADR-004
──────────────────────────────────
