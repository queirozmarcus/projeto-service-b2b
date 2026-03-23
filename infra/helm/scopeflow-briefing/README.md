# ScopeFlow Briefing Service Helm Chart

Kubernetes Helm chart para o ScopeFlow Briefing Service - API de briefing e alinhamento de escopo powered by AI.

## TL;DR

```bash
# Create namespace
kubectl create namespace production

# Create secrets
kubectl create secret generic scopeflow-briefing-secrets \
  --from-literal=database-url=jdbc:postgresql://postgres:5432/scopeflow \
  --from-literal=database-username=scopeflow \
  --from-literal=database-password=<password> \
  --from-literal=jwt-secret=<jwt-secret> \
  --from-literal=rabbitmq-host=rabbitmq \
  --from-literal=rabbitmq-username=scopeflow \
  --from-literal=rabbitmq-password=<password> \
  --from-literal=redis-host=redis \
  --from-literal=aws-access-key-id=<key> \
  --from-literal=aws-secret-access-key=<secret> \
  --from-literal=aws-s3-bucket=scopeflow-prod \
  --from-literal=openai-api-key=<key> \
  -n production

# Install chart
helm upgrade --install scopeflow-briefing . \
  --namespace production \
  --values values-production.yaml \
  --set image.tag=latest \
  --wait
```

## Introduction

This chart bootstraps a ScopeFlow Briefing Service deployment on a Kubernetes cluster using the Helm package manager.

## Prerequisites

- Kubernetes 1.28+
- Helm 3.14+
- PV provisioner support in the underlying infrastructure (for persistent volumes)
- Ingress controller (nginx recommended)
- Cert-manager (for TLS certificates)

## Installing the Chart

```bash
helm upgrade --install [RELEASE_NAME] . \
  --namespace [NAMESPACE] \
  --create-namespace \
  --values [VALUES_FILE] \
  --set image.tag=[IMAGE_TAG]
```

## Uninstalling the Chart

```bash
helm uninstall [RELEASE_NAME] --namespace [NAMESPACE]
```

## Parameters

### Global Parameters

| Name | Description | Value |
|------|-------------|-------|
| `replicaCount` | Number of replicas | `3` |
| `image.repository` | Image repository | `ghcr.io/scopeflow/scopeflow-api` |
| `image.tag` | Image tag | `""` (defaults to chart appVersion) |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |

### Service Parameters

| Name | Description | Value |
|------|-------------|-------|
| `service.type` | Service type | `ClusterIP` |
| `service.port` | Service port | `8080` |
| `service.targetPort` | Container port | `8080` |

### Ingress Parameters

| Name | Description | Value |
|------|-------------|-------|
| `ingress.enabled` | Enable ingress | `true` |
| `ingress.className` | Ingress class name | `nginx` |
| `ingress.hosts[0].host` | Hostname | `api.scopeflow.com` |
| `ingress.tls[0].secretName` | TLS secret name | `scopeflow-api-tls` |

### Resource Limits

| Name | Description | Value |
|------|-------------|-------|
| `resources.limits.cpu` | CPU limit | `1000m` |
| `resources.limits.memory` | Memory limit | `1Gi` |
| `resources.requests.cpu` | CPU request | `250m` |
| `resources.requests.memory` | Memory request | `512Mi` |

### Autoscaling

| Name | Description | Value |
|------|-------------|-------|
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `3` |
| `autoscaling.maxReplicas` | Maximum replicas | `10` |
| `autoscaling.targetCPUUtilizationPercentage` | Target CPU % | `75` |
| `autoscaling.targetMemoryUtilizationPercentage` | Target Memory % | `80` |

### Probes

| Name | Description | Value |
|------|-------------|-------|
| `livenessProbe.httpGet.path` | Liveness probe path | `/actuator/health/liveness` |
| `livenessProbe.initialDelaySeconds` | Initial delay | `30` |
| `readinessProbe.httpGet.path` | Readiness probe path | `/actuator/health/readiness` |
| `readinessProbe.initialDelaySeconds` | Initial delay | `10` |
| `startupProbe.httpGet.path` | Startup probe path | `/actuator/health/startup` |
| `startupProbe.failureThreshold` | Failure threshold | `30` |

### Pod Disruption Budget

| Name | Description | Value |
|------|-------------|-------|
| `podDisruptionBudget.enabled` | Enable PDB | `true` |
| `podDisruptionBudget.minAvailable` | Minimum available pods | `1` |

## Configuration Files

### values.yaml
Default configuration for all environments.

### values-staging.yaml
Staging-specific overrides:
- 2 replicas
- Reduced resources (100m CPU, 256Mi RAM)
- Autoscaling disabled
- Debug logging enabled

### values-production.yaml
Production-specific overrides:
- 3 replicas (HPA: 3-10)
- Full resources (250m-1000m CPU, 512Mi-1Gi RAM)
- Autoscaling enabled
- WARN logging level
- Hard pod anti-affinity
- PDB minAvailable: 2

## Secrets

Create a Kubernetes Secret named `scopeflow-briefing-secrets` with the following keys:

```bash
kubectl create secret generic scopeflow-briefing-secrets \
  --from-literal=database-url=jdbc:postgresql://host:5432/db \
  --from-literal=database-username=user \
  --from-literal=database-password=pass \
  --from-literal=jwt-secret=secret \
  --from-literal=rabbitmq-host=host \
  --from-literal=rabbitmq-username=user \
  --from-literal=rabbitmq-password=pass \
  --from-literal=redis-host=host \
  --from-literal=aws-access-key-id=key \
  --from-literal=aws-secret-access-key=secret \
  --from-literal=aws-s3-bucket=bucket \
  --from-literal=openai-api-key=key \
  --namespace [NAMESPACE]
```

## Monitoring

The chart exposes Prometheus metrics at `/actuator/prometheus` (port 8080).

Annotations for Prometheus scraping are configured by default:
```yaml
prometheus.io/scrape: "true"
prometheus.io/port: "8080"
prometheus.io/path: "/actuator/prometheus"
```

## Troubleshooting

### Pods not starting

```bash
kubectl get pods -n [NAMESPACE]
kubectl describe pod [POD_NAME] -n [NAMESPACE]
kubectl logs [POD_NAME] -n [NAMESPACE]
```

### Database connection issues

Verify secrets:
```bash
kubectl get secret scopeflow-briefing-secrets -n [NAMESPACE] -o yaml
```

### TLS certificate issues

Check cert-manager:
```bash
kubectl get certificate -n [NAMESPACE]
kubectl describe certificate scopeflow-api-tls -n [NAMESPACE]
```

## Support

- Documentation: `/docs/deployment/DEPLOYMENT-GUIDE.md`
- Issues: https://github.com/scopeflow/projeto-service-b2b/issues
- Email: devops@scopeflow.com
