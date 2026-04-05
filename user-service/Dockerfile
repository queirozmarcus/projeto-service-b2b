# Dockerfile — User Service (Strangler Fig extraction)
#
# Multi-stage build otimizado para microsserviço Java 21 + Spring Boot.
# Baseado no Dockerfile.prod do monólito, adaptado para user-service.
#
# Features:
#   - Layer caching de dependências Maven
#   - Extração de layers do Spring Boot (dependencies, snapshot-dependencies, spring-boot-loader, application)
#   - Runtime mínimo (JRE 21 alpine)
#   - Non-root user (UID 1001, GID 1001)
#   - Graceful shutdown configurado via env vars
#   - Health check via Spring Actuator
#
# Build:
#   docker build -t user-service:latest .
#
# Run:
#   docker run -p 8081:8081 -e DATABASE_URL=... user-service:latest

# ================================
# Stage 1: Build
# ================================
FROM eclipse-temurin:21-jdk-alpine AS builder
LABEL stage=builder

WORKDIR /build

# Copiar apenas arquivos de configuração Maven primeiro (cache de dependências)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixar dependências (camada cacheável — só refaz se pom.xml mudar)
RUN ./mvnw dependency:go-offline -B -q

# Copiar código-fonte e compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests -B -q && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ScopeFlow <support@scopeflow.com>" \
      description="User Service — Authentication & Authorization microservice" \
      org.opencontainers.image.source="https://github.com/scopeflow/user-service"

# Instalar curl para health checks
RUN apk add --no-cache curl

# Criar usuário non-root (UID 1001, GID 1001)
RUN addgroup -g 1001 userservice && \
    adduser -D -u 1001 -G userservice userservice

WORKDIR /app

# Copiar layers do Spring Boot na ordem correta (otimiza rebuild ao mudar código)
# 1. dependencies: bibliotecas externas (muda raramente)
# 2. spring-boot-loader: carregador do Spring Boot (muda raramente)
# 3. snapshot-dependencies: dependências SNAPSHOT (muda moderadamente)
# 4. application: código da aplicação (muda frequentemente)
COPY --from=builder --chown=userservice:userservice /build/target/extracted/dependencies/ ./
COPY --from=builder --chown=userservice:userservice /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=userservice:userservice /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=userservice:userservice /build/target/extracted/application/ ./

# Mudar para usuário non-root
USER userservice:userservice

# Porta do user-service (diferente do monólito 8080)
EXPOSE 8081

# Configurar sinal de parada (SIGTERM para graceful shutdown)
STOPSIGNAL SIGTERM

# Health check via Spring Actuator
# - interval: 30s (frequência de verificação)
# - timeout: 3s (tempo máximo de espera por resposta)
# - start-period: 40s (período de inicialização antes de falhar)
# - retries: 3 (tentativas antes de marcar como unhealthy)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health/liveness || exit 1

# JVM options para containers:
#   -XX:+UseContainerSupport: detecta limites de CPU/memória do container
#   -XX:MaxRAMPercentage=75.0: usa até 75% da memória do container
#   -XX:+UseG1GC: Garbage Collector G1 (padrão no Java 21)
#   -XX:MaxGCPauseMillis=200: pausa máxima de GC de 200ms
#   -XX:+HeapDumpOnOutOfMemoryError: gera dump se OOM ocorrer
#   -XX:HeapDumpPath=/tmp/: local do dump (montado via volume para debug)
#   -Dspring.threads.virtual.enabled=true: ativa virtual threads (Java 21+)
#   -Dspring.lifecycle.timeout-per-shutdown-phase: timeout de graceful shutdown (override via env var)
#
# JAVA_TOOL_OPTIONS pode ser injetado via env var no docker-compose para debug:
#   JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
CMD ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/", \
    "-Dspring.threads.virtual.enabled=true", \
    "-Dspring.lifecycle.timeout-per-shutdown-phase=30s", \
    "org.springframework.boot.loader.launch.JarLauncher"]
