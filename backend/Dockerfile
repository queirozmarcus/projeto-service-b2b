# Multi-stage build: JDK 21 para build, JRE mínimo para runtime
# Stage 1: Build — usa Maven wrapper para garantir versão controlada
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copia o wrapper primeiro para cache de dependências funcionar
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download de dependências em camada separada (melhora cache)
RUN ./mvnw dependency:go-offline -B -q

# Copia código-fonte e compila
COPY src ./src
RUN ./mvnw clean package -DskipTests -B -q && \
    java -Djarmode=tools -jar target/*.jar extract --layers --launcher

# Stage 2: Runtime — imagem mínima com JRE apenas
FROM eclipse-temurin:21-jre-alpine

# Cria usuário não-root antes de qualquer COPY (boa prática de segurança)
RUN addgroup -g 1000 scopeflow && \
    adduser -D -u 1000 -G scopeflow scopeflow

WORKDIR /app

# Copia camadas extraídas em ordem crescente de volatilidade
# (dependências → loader → snapshot-deps → application)
# Essa ordem maximiza o cache de camadas entre deploys
COPY --from=builder --chown=scopeflow:scopeflow /build/target/extracted/dependencies/ ./
COPY --from=builder --chown=scopeflow:scopeflow /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=scopeflow:scopeflow /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=scopeflow:scopeflow /build/target/extracted/application/ ./

USER scopeflow

EXPOSE 8080

# STOPSIGNAL SIGTERM garante graceful shutdown via Kubernetes preStop + terminationGracePeriodSeconds
STOPSIGNAL SIGTERM

# Healthcheck para runtime local (Kubernetes usa as probes do Deployment)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

# Flags JVM obrigatórias para containers:
#   -XX:+UseContainerSupport      → respeita cgroup memory/cpu limits do Kubernetes
#   -XX:MaxRAMPercentage=75.0     → heap = 75% do limit do container (sobra para metaspace + threads)
#   -XX:+UseG1GC                  → GC padrão Java 21, bom para workloads de serviço
#   -XX:MaxGCPauseMillis=200      → target de pause para GC
#   -XX:+HeapDumpOnOutOfMemoryError → diagnóstico de OOM
#   -XX:HeapDumpPath=/tmp/        → diretório gravável por usuário não-root
#   -Dspring.threads.virtual.enabled=true → habilita virtual threads (Project Loom, Java 21 + SB 3.2)
#
# REMOVIDO do ENTRYPOINT padrão: flags JMX (-Dcom.sun.management.jmxremote*)
#   JMX exposto sem autenticação/SSL em produção é risco de segurança crítico.
#   Para monitoramento, use Micrometer + Prometheus (/actuator/prometheus).
#   Se precisar de JMX pontualmente, injete via JAVA_TOOL_OPTIONS em modo debug local.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/", \
    "-Dspring.threads.virtual.enabled=true", \
    "-Dspring.lifecycle.timeout-per-shutdown-phase=${SHUTDOWN_TIMEOUT:-20s}", \
    "org.springframework.boot.loader.launch.JarLauncher"]
