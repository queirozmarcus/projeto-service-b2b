# DevOps-Engineer Delegation — Step 5: Briefing Deployment

**To:** devops-engineer (Claude Sonnet)
**From:** Marcus (Orchestrator)
**Date:** 2026-03-22
**Mode:** Fork isolated — Docker, Kubernetes, CI/CD
**Task ID:** TERMINAL2-STEP5-DEVOPS-ENGINEER
**Dependency:** All previous steps (1-4)

---

## Mission

Design and implement **production-ready deployment infrastructure** for the Briefing service. Create Docker multi-stage build, Kubernetes Helm chart, and GitHub Actions CI/CD pipeline.

**Input:**
- All previous steps: Domain (2), Schema (3), API (4)
- Reference: `backend/Dockerfile.prod` (Terminal 1 example)
- Reference: `k8s/helm/values.yaml` (Terminal 1 Helm example)
- Project CLAUDE.md: `./CLAUDE.md` (infrastructure preferences)

**Output:**
- `backend/Dockerfile.prod` (multi-stage, JDK 21 → JRE 21 Alpine)
- `k8s/helm/values-briefing.yaml` (Kubernetes values override)
- `.github/workflows/briefing-ci-cd.yml` (GitHub Actions pipeline)
- Infrastructure documentation: `.claude/plans/DEVOPS-ENGINEER-OUTPUT-Step5-Briefing.md`

**Constraints:**
- Multi-stage Docker (minimize image size)
- Non-root user (security)
- Kubernetes probes (liveness + readiness)
- Auto-scaling: 2-10 replicas
- Pod Disruption Budget (availability)
- Security context (read-only FS, no capabilities)
- CI/CD: Build → Test → Quality Gate → Image → Deploy

**Timeline:** ~1 day

---

## Docker Build (Dockerfile.prod)

```dockerfile
## Multi-stage Dockerfile for ScopeFlow Backend (Production)
## Uses: Eclipse Temurin JDK 21 → JRE 21 Alpine
## Image size: ~300MB (optimized)

# Stage 1: Build with Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
LABEL stage=builder

WORKDIR /build

# Copy Maven wrapper and pom.xml
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Download Maven dependencies (cached layer)
RUN ./mvnw dependency:resolve-plugins dependency:resolve -q

# Copy source code
COPY src ./src

# Build application (skip tests for faster build)
RUN ./mvnw clean package -DskipTests -q && \
    mv target/*.jar application.jar

# Stage 2: Runtime with JRE only (minimal image)
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ScopeFlow <support@scopeflow.com>" \
      description="ScopeFlow Briefing API - AI-powered B2B SaaS" \
      version="1.0.0-sprint1"

# Install dumb-init (graceful shutdown handler)
RUN apk add --no-cache dumb-init

# Create non-root user for security
RUN addgroup -g 1000 scopeflow && \
    adduser -D -u 1000 -G scopeflow scopeflow

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/application.jar .
RUN chown -R scopeflow:scopeflow /app

# Switch to non-root user
USER scopeflow:scopeflow

# JVM tuning for virtual threads + low-latency
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:G1NewCollectionHeapPercent=30 \
    -XX:G1ReservePercent=20 \
    -XX:InitiatingHeapOccupancyPercent=35 \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof"

# Expose ports
EXPOSE 8080
EXPOSE 9010

# Health check (Kubernetes readiness probe)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/v1/health/ready || exit 1

# Use dumb-init as entrypoint (handles signals properly)
ENTRYPOINT ["dumb-init", "--"]

# Start application
CMD ["java", "-jar", "application.jar"]
```

---

## Kubernetes Helm Chart (values-briefing.yaml)

Create: `k8s/helm/values-briefing.yaml`

```yaml
# ScopeFlow Briefing Service — Helm Chart Values
# Deployment-specific overrides for briefing-service

replicaCount: 2

image:
  repository: scopeflow/briefing-service
  tag: "1.0.0-sprint1"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8080
  targetPort: 8080
  name: briefing-service

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
  hosts:
    - host: api.scopeflow.com
      paths:
        - path: /api/v1/briefing
          pathType: Prefix
  tls:
    - secretName: scopeflow-api-tls
      hosts:
        - api.scopeflow.com

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

podDisruptionBudget:
  enabled: true
  minAvailable: 1

env:
  - name: JAVA_OPTS
    value: "-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
  - name: SPRING_PROFILES_ACTIVE
    value: "kubernetes"
  - name: BRIEFING_SERVICE_NAME
    value: "briefing-service"

secrets:
  database:
    host: postgres.default.svc.cluster.local
    port: 5432
    name: scopeflow_prod
  jwt:
    secretKey: ""  # Set via Sealed Secret
  openai:
    apiKey: ""     # Set via Sealed Secret

livenessProbe:
  httpGet:
    path: /api/v1/health/live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /api/v1/health/ready
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3

terminationGracePeriodSeconds: 30

affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app
                operator: In
                values:
                  - briefing-service
          topologyKey: kubernetes.io/hostname

securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsReadOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
```

---

## GitHub Actions CI/CD Pipeline

Create: `.github/workflows/briefing-ci-cd.yml`

```yaml
name: Briefing Service CI/CD

on:
  push:
    branches:
      - main
      - feature/sprint-1b-briefing-domain
    paths:
      - 'backend/**'
      - '.github/workflows/briefing-ci-cd.yml'
  pull_request:
    branches:
      - main
    paths:
      - 'backend/**'

jobs:
  build-and-test:
    name: Build & Test
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: scopeflow_test
          POSTGRES_USER: scopeflow
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: |
          cd backend
          ./mvnw clean compile

      - name: Run Unit Tests
        run: |
          cd backend
          ./mvnw test

      - name: Run Integration Tests
        run: |
          cd backend
          ./mvnw verify
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/scopeflow_test
          SPRING_DATASOURCE_USERNAME: scopeflow
          SPRING_DATASOURCE_PASSWORD: test

      - name: Code Quality (Checkstyle)
        run: |
          cd backend
          ./mvnw checkstyle:check

      - name: Code Coverage
        run: |
          cd backend
          ./mvnw package jacoco:report
          echo "Coverage report available at: target/site/jacoco/index.html"

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./backend/target/site/jacoco/jacoco.xml
          flags: briefing-service
          name: briefing-coverage

  build-image:
    name: Build Docker Image
    runs-on: ubuntu-latest
    needs: build-and-test
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v5
        with:
          context: backend
          file: ./backend/Dockerfile.prod
          push: true
          tags: |
            scopeflow/briefing-service:${{ github.sha }}
            scopeflow/briefing-service:1.0.0-sprint1
            scopeflow/briefing-service:latest
          cache-from: type=registry,ref=scopeflow/briefing-service:buildcache
          cache-to: type=registry,ref=scopeflow/briefing-service:buildcache,mode=max

  security-scan:
    name: Security Scanning
    runs-on: ubuntu-latest
    needs: build-and-test

    steps:
      - uses: actions/checkout@v4

      - name: Run Trivy Vulnerability Scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: 'backend'
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy Results to GitHub Security
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Check for CVEs
        run: |
          if grep -q '"Severity": "HIGH"' trivy-results.sarif; then
            echo "HIGH severity vulnerabilities found!"
            exit 1
          fi

  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    needs: [build-and-test, build-image, security-scan]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/setup-kubectl@v3
        with:
          version: 'v1.28.0'

      - name: Deploy with Helm
        run: |
          helm upgrade --install briefing-service ./k8s/helm \
            -f ./k8s/helm/values-briefing.yaml \
            --set image.tag=${{ github.sha }} \
            --namespace staging \
            --create-namespace \
            --wait \
            --timeout 5m
        env:
          KUBECONFIG: ${{ secrets.KUBECONFIG }}

      - name: Verify Deployment
        run: |
          kubectl rollout status deployment/briefing-service -n staging --timeout=5m
          kubectl get pods -n staging -l app=briefing-service

  notify-slack:
    name: Notify Slack
    runs-on: ubuntu-latest
    needs: [build-and-test, build-image, deploy-staging]
    if: always()

    steps:
      - name: Send Slack Notification
        uses: slackapi/slack-github-action@v1.24.0
        with:
          payload: |
            {
              "text": "Briefing Service CI/CD Pipeline",
              "blocks": [
                {
                  "type": "header",
                  "text": {
                    "type": "plain_text",
                    "text": "🚀 Briefing Service Build"
                  }
                },
                {
                  "type": "section",
                  "fields": [
                    {
                      "type": "mrkdwn",
                      "text": "*Commit:*\n${{ github.sha }}"
                    },
                    {
                      "type": "mrkdwn",
                      "text": "*Branch:*\n${{ github.ref_name }}"
                    },
                    {
                      "type": "mrkdwn",
                      "text": "*Status:*\n${{ job.status }}"
                    },
                    {
                      "type": "mrkdwn",
                      "text": "*Author:*\n${{ github.actor }}"
                    }
                  ]
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
          SLACK_WEBHOOK_TYPE: INCOMING_WEBHOOK
```

---

## Deliverables Checklist

When done, Step 5 is complete if:

- [ ] File created: `backend/Dockerfile.prod` (multi-stage, optimized)
- [ ] File created: `k8s/helm/values-briefing.yaml` (Kubernetes values)
- [ ] File created: `.github/workflows/briefing-ci-cd.yml` (CI/CD pipeline)
- [ ] Docker image builds without errors: `docker build -f backend/Dockerfile.prod -t briefing:test .`
- [ ] Dockerfile uses non-root user (scopeflow:1000)
- [ ] Dockerfile uses JRE-only in final stage (~300MB)
- [ ] Kubernetes probes configured (liveness + readiness)
- [ ] Auto-scaling enabled (2-10 replicas)
- [ ] Pod Disruption Budget configured
- [ ] Security context enforced (read-only FS)
- [ ] CI/CD pipeline includes: Build → Test → Quality Gate → Image → Deploy
- [ ] GitHub Actions YAML is valid
- [ ] Kubernetes manifests are valid: `helm lint k8s/helm -f k8s/helm/values-briefing.yaml`
- [ ] Infrastructure documentation complete
- [ ] Committed: `feat(devops): briefing-docker-kubernetes-cicd`
- [ ] Output summary: `.claude/plans/DEVOPS-ENGINEER-OUTPUT-Step5-Briefing.md`

---

## Reference Materials

- **Terminal 1 Docker:** `backend/Dockerfile.prod`
- **Terminal 1 Helm:** `k8s/helm/values.yaml`
- **Terminal 1 CI/CD:** `.github/workflows/*.yml`
- **Docker Multi-stage:** https://docs.docker.com/build/building/multi-stage/
- **Kubernetes Best Practices:** https://kubernetes.io/docs/concepts/configuration/overview/
- **GitHub Actions:** https://docs.github.com/en/actions

---

## Git Workflow

1. Branch: `feature/sprint-1b-briefing-domain`
2. Create/Update: `backend/Dockerfile.prod`, `k8s/helm/values-briefing.yaml`, `.github/workflows/briefing-ci-cd.yml`
3. Validate Docker: `docker build -f backend/Dockerfile.prod -t briefing:test .`
4. Validate Helm: `helm lint k8s/helm -f k8s/helm/values-briefing.yaml`
5. Commit: `feat(devops): briefing-docker-kubernetes-cicd`
6. Push to origin

---

## Timeline

**Start:** After API-Designer Step 4 ✅
**Duration:** ~1 day
**Next:** Consolidation (Step 6)

---

**Ready. Build production deployment infrastructure for Briefing.** 🚀
