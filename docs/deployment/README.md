# Deployment Guide

Procedures for deploying ScopeFlow across environments: Local, Staging, and Production.

## Quick Start

### Local Development

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Verify health
./scripts/check-stack-health.sh

# 3. Run tests
./scripts/validate-qa-full.sh
```

API available at http://localhost:8080

### Staging/Production (Kubernetes)

```bash
# Deploy via GitHub Actions (recommended)
git push origin main  # Auto-deploys to staging → manual approval → production

# Manual deploy
helm upgrade --install scopeflow-briefing ./infra/helm/scopeflow-briefing \
  --namespace <staging|production> \
  --values ./infra/helm/scopeflow-briefing/values-<env>.yaml \
  --set image.tag=$(git rev-parse --short HEAD)
```

---

## Prerequisites

### Local
- Docker 24+ with Compose v2
- Java 21 (Eclipse Temurin)
- Maven 3.8+

### Kubernetes
- Kubernetes 1.28+
- Helm 3.14+
- kubectl with cluster access
- Secrets configured (see below)

---

## Environments

### Local (Docker Compose)

**Stack:** PostgreSQL, User-DB, RabbitMQ, Redis, Traefik, Backend, User Service

```bash
# Start all services
docker compose up -d

# Check health
./scripts/check-stack-health.sh

# View logs
docker compose logs -f <service-name>

# Stop
docker compose down
```

**Environment Variables:** See `.env.example`

### Staging (Kubernetes)

**Automated via GitHub Actions:**
- Push to any branch → deploy to staging
- Runs tests → builds image → deploys → smoke tests

**Manual deploy:**
```bash
kubectl create namespace staging

# Create secrets (see Secrets section)
kubectl create secret generic scopeflow-briefing-secrets -n staging \
  --from-literal=database-url=jdbc:postgresql://postgres:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=$(openssl rand -base64 24) \
  --from-literal=jwt-secret=$(openssl rand -hex 32) \
  --from-literal=rabbitmq-host=rabbitmq \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=$(openssl rand -base64 24) \
  --from-literal=redis-host=redis \
  --from-literal=aws-access-key-id=<key> \
  --from-literal=aws-secret-access-key=<secret> \
  --from-literal=aws-s3-bucket=scopeflow-staging \
  --from-literal=openai-api-key=<key>

helm upgrade --install scopeflow-briefing ./infra/helm/scopeflow-briefing \
  --namespace staging \
  --values ./infra/helm/scopeflow-briefing/values-staging.yaml \
  --set image.tag=$(git rev-parse --short HEAD) \
  --wait --timeout 5m
```

### Production (Kubernetes)

**Automated via GitHub Actions:**
- Merge to main → staging → **manual approval** → production

**Manual deploy:**
```bash
kubectl create namespace production

# Create secrets with STRONG values
kubectl create secret generic scopeflow-briefing-secrets -n production \
  --from-literal=database-url=jdbc:postgresql://postgres-prod:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=<PRODUCTION_PASSWORD> \
  --from-literal=jwt-secret=<PRODUCTION_JWT_SECRET> \
  --from-literal=rabbitmq-host=rabbitmq-prod \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=<PRODUCTION_RABBITMQ_PASSWORD> \
  --from-literal=redis-host=redis-prod \
  --from-literal=aws-access-key-id=<PRODUCTION_AWS_KEY> \
  --from-literal=aws-secret-access-key=<PRODUCTION_AWS_SECRET> \
  --from-literal=aws-s3-bucket=scopeflow-production \
  --from-literal=openai-api-key=<PRODUCTION_OPENAI_KEY>

helm upgrade --install scopeflow-briefing ./infra/helm/scopeflow-briefing \
  --namespace production \
  --values ./infra/helm/scopeflow-briefing/values-production.yaml \
  --set image.tag=$(git rev-parse --short HEAD) \
  --wait --timeout 10m --atomic
```

---

## Secrets Management

### Generate Strong Secrets

```bash
# JWT Secret (32+ characters)
openssl rand -hex 32

# Passwords (24+ characters)
openssl rand -base64 24
```

### Verify Secrets

```bash
# List secrets
kubectl get secrets -n <namespace>

# Decode secret value
kubectl get secret scopeflow-briefing-secrets -n <namespace> \
  -o jsonpath='{.data.jwt-secret}' | base64 -d
```

---

## Operations

### Verify Deployment

```bash
# Kubernetes
kubectl rollout status deployment/scopeflow-briefing -n <namespace>
kubectl get pods -n <namespace>

# Health check
curl https://api.scopeflow.com/actuator/health
```

### View Logs

```bash
# Local
docker compose logs -f <service-name>

# Kubernetes
kubectl logs -f -l app.kubernetes.io/name=scopeflow-briefing -n <namespace>
```

### Rollback

```bash
# Helm rollback
helm rollback scopeflow-briefing -n <namespace>

# Kubernetes rollback
kubectl rollout undo deployment/scopeflow-briefing -n <namespace>
```

### Scaling

```bash
# Manual scale
kubectl scale deployment scopeflow-briefing --replicas=5 -n <namespace>

# HPA status
kubectl get hpa scopeflow-briefing -n <namespace>
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Pod not starting | `kubectl describe pod <pod-name> -n <namespace>` → check events + logs |
| Database connection failed | Verify secret values: `kubectl get secret scopeflow-briefing-secrets -o yaml -n <namespace>` |
| High latency | Check resources: `kubectl top pods -n <namespace>` + HPA status |
| Ingress/SSL issues | Check ingress: `kubectl describe ingress -n <namespace>` + cert-manager logs |
| Local stack unhealthy | `./scripts/check-stack-health.sh` → restart failed services |

**Full diagnostics:** See `scripts/README.md` for validation scripts.

**Documentation:** See [CLAUDE.md](../../CLAUDE.md) for development guidelines and architecture details.

---

## Security Checklist

- [ ] Strong secrets (32+ chars JWT, 24+ chars passwords)
- [ ] Non-root user in pods
- [ ] Read-only root filesystem
- [ ] Resource limits configured
- [ ] TLS enabled for ingress
- [ ] No secrets in Git
- [ ] Image scanning in CI/CD

---

## Support

- **Scripts:** `scripts/README.md` — validation and health check scripts
- **API Docs:** `docs/api/README.md`
- **Architecture:** `CLAUDE.md` — hexagonal architecture, patterns, conventions
- **Migration:** `docs/migration/` — user service extraction, DB strategy
