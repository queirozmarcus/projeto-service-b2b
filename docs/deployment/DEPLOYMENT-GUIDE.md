# ScopeFlow Briefing Service - Deployment Guide

## Overview

This guide covers deployment procedures for the ScopeFlow Briefing Service across different environments:
- **Local Development**: Docker Compose
- **Staging**: Kubernetes (automated via GitHub Actions)
- **Production**: Kubernetes (manual approval required)

---

## Prerequisites

### Local Development
- Docker 24+ with Docker Compose v2
- Java 21 (Eclipse Temurin)
- Maven 3.8+
- PostgreSQL 16 (via Docker)
- RabbitMQ 3.13 (via Docker)
- Redis 7 (via Docker)

### Kubernetes Environments
- Kubernetes 1.28+
- Helm 3.14+
- kubectl configured with cluster access
- Secrets created (see Secrets Management below)

---

## Local Development Deployment

### 1. Environment Setup

Create `.env` file in project root:

```bash
# Database
POSTGRES_DB=scopeflow
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# RabbitMQ
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

# OpenAI (optional for local dev)
OPENAI_API_KEY=sk-your-key-here
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, RabbitMQ, Redis
docker compose up -d postgres rabbitmq redis

# Verify health
docker compose ps
```

### 3. Run Database Migrations

```bash
cd backend
./mvnw flyway:migrate
```

### 4. Start Application (Option A: Maven)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 5. Start Application (Option B: Docker)

```bash
# Build and start all services (including app)
docker compose up -d

# View logs
docker compose logs -f app
```

### 6. Verify Deployment

```bash
# Health check
curl http://localhost:8080/actuator/health

# Briefing API (requires auth)
curl http://localhost:8080/api/v1/briefing/sessions
```

### 7. Stop Services

```bash
# Stop all services
docker compose down

# Stop and remove volumes (clean slate)
docker compose down -v
```

---

## Staging Deployment (Kubernetes)

### Prerequisites

1. **Kubernetes cluster** with:
   - Namespace: `staging`
   - Ingress controller (nginx)
   - Cert-manager (Let's Encrypt staging issuer)

2. **GitHub Secrets** configured:
   - `STAGING_KUBECONFIG`: Base64-encoded kubeconfig

### Automated Deployment

Staging deployment is **fully automated** via GitHub Actions:

```bash
# Push to feature branch
git push origin feature/sprint-1b-briefing-domain

# GitHub Actions will:
# 1. Run tests
# 2. Build Docker image
# 3. Deploy to staging
# 4. Run smoke tests
```

### Manual Deployment

```bash
# 1. Build Docker image locally
cd backend
docker build -t ghcr.io/scopeflow/scopeflow-api:$(git rev-parse --short HEAD) -f Dockerfile.prod .

# 2. Push to registry
docker push ghcr.io/scopeflow/scopeflow-api:$(git rev-parse --short HEAD)

# 3. Create namespace
kubectl create namespace staging

# 4. Create secrets (see Secrets Management below)
kubectl create secret generic scopeflow-briefing-secrets \
  --from-literal=database-url=jdbc:postgresql://postgres-staging:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=<staging-password> \
  --from-literal=jwt-secret=<random-32-chars> \
  --from-literal=rabbitmq-host=rabbitmq-staging \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=<staging-password> \
  --from-literal=redis-host=redis-staging \
  --from-literal=aws-access-key-id=<aws-key> \
  --from-literal=aws-secret-access-key=<aws-secret> \
  --from-literal=aws-s3-bucket=scopeflow-staging \
  --from-literal=openai-api-key=<openai-key> \
  -n staging

# 5. Deploy via Helm
helm upgrade --install scopeflow-briefing ./infra/helm/scopeflow-briefing \
  --namespace staging \
  --values ./infra/helm/scopeflow-briefing/values-staging.yaml \
  --set image.tag=$(git rev-parse --short HEAD) \
  --wait --timeout 5m

# 6. Verify deployment
kubectl rollout status deployment/scopeflow-briefing -n staging
kubectl get pods -n staging
kubectl logs -l app.kubernetes.io/name=scopeflow-briefing -n staging --tail=50

# 7. Test health endpoint
kubectl run curl-test --image=curlimages/curl:latest --rm -i --restart=Never -n staging -- \
  curl -s http://scopeflow-briefing:8080/actuator/health
```

---

## Production Deployment (Kubernetes)

### Prerequisites

1. **Kubernetes cluster** with:
   - Namespace: `production`
   - Ingress controller (nginx)
   - Cert-manager (Let's Encrypt production issuer)
   - Resource quotas and limits configured

2. **GitHub Secrets** configured:
   - `PRODUCTION_KUBECONFIG`: Base64-encoded kubeconfig
   - `SLACK_WEBHOOK_URL`: Slack notifications

### Deployment Process

Production deployment requires **manual approval** via GitHub Actions:

```bash
# 1. Merge feature branch to main
git checkout main
git merge feature/sprint-1b-briefing-domain
git push origin main

# 2. GitHub Actions will:
#    - Run all tests
#    - Build Docker image
#    - Deploy to staging (auto)
#    - Wait for manual approval
#    - Deploy to production (after approval)

# 3. Manual approval required in GitHub UI:
#    https://github.com/scopeflow/projeto-service-b2b/actions
```

### Manual Deployment (Emergency Rollout)

```bash
# 1. Create namespace
kubectl create namespace production

# 2. Create secrets (see Secrets Management below)
kubectl create secret generic scopeflow-briefing-secrets \
  --from-literal=database-url=jdbc:postgresql://postgres-prod:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=<strong-password> \
  --from-literal=jwt-secret=<random-32-chars-min> \
  --from-literal=rabbitmq-host=rabbitmq-prod \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=<strong-password> \
  --from-literal=redis-host=redis-prod \
  --from-literal=aws-access-key-id=<aws-key> \
  --from-literal=aws-secret-access-key=<aws-secret> \
  --from-literal=aws-s3-bucket=scopeflow-production \
  --from-literal=openai-api-key=<openai-key> \
  -n production

# 3. Deploy via Helm
helm upgrade --install scopeflow-briefing ./infra/helm/scopeflow-briefing \
  --namespace production \
  --values ./infra/helm/scopeflow-briefing/values-production.yaml \
  --set image.tag=$(git rev-parse --short HEAD) \
  --wait --timeout 10m --atomic

# 4. Verify deployment
kubectl rollout status deployment/scopeflow-briefing -n production
kubectl get pods -n production -l app.kubernetes.io/name=scopeflow-briefing
kubectl get hpa -n production

# 5. Test health endpoint
for i in {1..10}; do
  kubectl run curl-test-$i --image=curlimages/curl:latest --rm -i --restart=Never -n production -- \
    curl -s http://scopeflow-briefing:8080/actuator/health | grep '"status":"UP"' && break
  sleep 5
done
```

---

## Secrets Management

### Generate Strong Secrets

```bash
# JWT Secret (32+ characters)
openssl rand -hex 32

# Database Password
openssl rand -base64 24

# RabbitMQ Password
openssl rand -base64 24
```

### Create Kubernetes Secrets

**Staging:**
```bash
kubectl create secret generic scopeflow-briefing-secrets \
  --from-literal=database-url=jdbc:postgresql://postgres-staging.example.com:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=$(openssl rand -base64 24) \
  --from-literal=jwt-secret=$(openssl rand -hex 32) \
  --from-literal=rabbitmq-host=rabbitmq-staging.example.com \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=$(openssl rand -base64 24) \
  --from-literal=redis-host=redis-staging.example.com \
  --from-literal=aws-access-key-id=AKIAIOSFODNN7EXAMPLE \
  --from-literal=aws-secret-access-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY \
  --from-literal=aws-s3-bucket=scopeflow-staging \
  --from-literal=openai-api-key=sk-proj-... \
  --namespace staging
```

**Production:**
```bash
kubectl create secret generic scopeflow-briefing-secrets \
  --from-literal=database-url=jdbc:postgresql://postgres-prod.example.com:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=<PRODUCTION_PASSWORD> \
  --from-literal=jwt-secret=<PRODUCTION_JWT_SECRET> \
  --from-literal=rabbitmq-host=rabbitmq-prod.example.com \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=<PRODUCTION_RABBITMQ_PASSWORD> \
  --from-literal=redis-host=redis-prod.example.com \
  --from-literal=aws-access-key-id=<PRODUCTION_AWS_KEY> \
  --from-literal=aws-secret-access-key=<PRODUCTION_AWS_SECRET> \
  --from-literal=aws-s3-bucket=scopeflow-production \
  --from-literal=openai-api-key=<PRODUCTION_OPENAI_KEY> \
  --namespace production
```

### Verify Secrets

```bash
# List secrets
kubectl get secrets -n staging
kubectl get secrets -n production

# Describe secret (values are base64-encoded)
kubectl describe secret scopeflow-briefing-secrets -n staging

# Decode secret value (for verification only)
kubectl get secret scopeflow-briefing-secrets -n staging -o jsonpath='{.data.jwt-secret}' | base64 -d
```

---

## Rollback Procedures

### Helm Rollback

```bash
# List Helm releases
helm list -n staging
helm list -n production

# View release history
helm history scopeflow-briefing -n production

# Rollback to previous version
helm rollback scopeflow-briefing -n production

# Rollback to specific revision
helm rollback scopeflow-briefing 3 -n production
```

### Kubernetes Rollback

```bash
# View deployment history
kubectl rollout history deployment/scopeflow-briefing -n production

# Rollback to previous deployment
kubectl rollout undo deployment/scopeflow-briefing -n production

# Rollback to specific revision
kubectl rollout undo deployment/scopeflow-briefing --to-revision=5 -n production

# Verify rollback
kubectl rollout status deployment/scopeflow-briefing -n production
```

---

## Monitoring & Observability

### Health Checks

```bash
# Kubernetes probes
kubectl get pods -n production -l app.kubernetes.io/name=scopeflow-briefing
kubectl describe pod <pod-name> -n production

# Application health
curl https://api.scopeflow.com/actuator/health

# Detailed health (requires auth)
curl -H "Authorization: Bearer <token>" https://api.scopeflow.com/actuator/health/liveness
curl -H "Authorization: Bearer <token>" https://api.scopeflow.com/actuator/health/readiness
```

### Metrics

```bash
# Prometheus metrics endpoint
curl https://api.scopeflow.com/actuator/prometheus

# HPA status
kubectl get hpa scopeflow-briefing -n production

# Pod resource usage
kubectl top pods -n production -l app.kubernetes.io/name=scopeflow-briefing
```

### Logs

```bash
# Stream logs from all pods
kubectl logs -f -l app.kubernetes.io/name=scopeflow-briefing -n production

# Logs from specific pod
kubectl logs <pod-name> -n production --tail=100

# Logs from previous container (after crash)
kubectl logs <pod-name> -n production --previous
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n production

# Describe pod (check events)
kubectl describe pod <pod-name> -n production

# Check logs
kubectl logs <pod-name> -n production

# Common issues:
# 1. Image pull error: verify image tag and registry credentials
# 2. CrashLoopBackOff: check application logs
# 3. Pending: check resource quotas and node capacity
```

### Database Connection Issues

```bash
# Verify database secret
kubectl get secret scopeflow-briefing-secrets -n production -o yaml

# Test database connectivity from pod
kubectl exec -it <pod-name> -n production -- sh
# Inside pod:
wget -qO- https://api.scopeflow.com/actuator/health | grep db
```

### High Latency / Performance Issues

```bash
# Check resource usage
kubectl top pods -n production -l app.kubernetes.io/name=scopeflow-briefing

# Check HPA scaling
kubectl get hpa scopeflow-briefing -n production

# Force scale up (temporary)
kubectl scale deployment scopeflow-briefing --replicas=5 -n production

# Check application metrics
curl https://api.scopeflow.com/actuator/metrics
```

### Ingress / SSL Issues

```bash
# Check ingress
kubectl get ingress -n production
kubectl describe ingress scopeflow-briefing -n production

# Check cert-manager certificate
kubectl get certificate -n production
kubectl describe certificate scopeflow-api-tls -n production

# Check cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager
```

---

## Maintenance Operations

### Database Migrations

```bash
# Migrations run automatically on startup via Flyway
# Check migration status:
kubectl logs <pod-name> -n production | grep Flyway
```

### Scaling

```bash
# Manual scaling (overrides HPA)
kubectl scale deployment scopeflow-briefing --replicas=5 -n production

# Re-enable HPA
kubectl autoscale deployment scopeflow-briefing --min=3 --max=10 --cpu-percent=75 -n production
```

### Zero-Downtime Updates

```bash
# Update with rolling deployment
helm upgrade scopeflow-briefing ./infra/helm/scopeflow-briefing \
  --namespace production \
  --values ./infra/helm/scopeflow-briefing/values-production.yaml \
  --set image.tag=<new-tag> \
  --wait --atomic

# Monitor rollout
kubectl rollout status deployment/scopeflow-briefing -n production
```

---

## Security Checklist

- [ ] Secrets created with strong, random values
- [ ] Non-root user enforced in pod security context
- [ ] Read-only root filesystem enabled
- [ ] Resource limits configured
- [ ] Network policies applied (if cluster supports)
- [ ] TLS/SSL enabled for all ingress
- [ ] RBAC roles configured for service account
- [ ] Pod disruption budget configured
- [ ] Image scanning enabled in CI/CD
- [ ] No secrets in Git repository

---

## Contact & Support

- **DevOps Team**: devops@scopeflow.com
- **Slack Channel**: #devops-alerts
- **Runbooks**: `/docs/deployment/runbooks/`
- **Incident Management**: PagerDuty

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03-22 | Initial deployment guide for Briefing domain |
