──────────────────────────────────
Plano: Migration Pack Fase 3 — Cut-over em Produção (User Service)
Data: 2026-04-12
Status: APROVADO

[Contexto]
User Service extraído e validado em staging desde 11/04/2026 (~24h).
Staging estável — usuário confirmou. Avançando para produção.
Feature flag: AUTH_SERVICE_EXTRACTED (boolean) — rollback sem redeploy.
Mecanismo: helm upgrade --set config.authServiceExtracted=true dispara
rolling restart via checksum/config annotation já no deployment template.

[Etapas]
1. k8s/helm/user-service/ → kubernetes-engineer
   → Helm chart completo para user-service: Chart.yaml, deployment, service,
     ingress, hpa, pdb, configmap, serviceaccount, _helpers.tpl
   → values.yaml (base), values-staging.yaml, values-prod.yaml

2. Monólito: AUTH_SERVICE_EXTRACTED no Helm → kubernetes-engineer
   → Adicionar AUTH_SERVICE_EXTRACTED e AUTH_SERVICE_URL ao ConfigMap do monólito
   → values.yaml: defaults (authServiceExtracted: "false")
   → values-prod.yaml: authServiceExtracted: "false" (flip via --set no cut-over)
   → values-staging.yaml: authServiceExtracted: "true"

3. application-production.yml no monólito → backend-dev
   → auth.service.use-extracted=true
   → Cookie secure=true, requires-https=true
   → Logging INFO, security WARN

4. application-production.yml no user-service → backend-dev
   → Porta 8081, logging INFO
   → Flyway desabilitado (banco compartilhado — monólito gerencia migrations)

[Decisões]
- Roll-out gradual via helm upgrade --set (sem redeploy de imagem)
- checksum/config annotation no deployment garante rolling restart automático
- user-service usa SHARED DB por ora (DB-per-service é próxima fase)
- JWT_SECRET compartilhado via externalSecrets (mesmo secret name)

[Riscos]
- Banco ainda compartilhado: sem isolamento de dados nesta fase (intencional)
- JWT_SECRET divergência: mitigado por secret compartilhado no K8s
──────────────────────────────────
