# ==============================================================================
# Dockerfile: share-service
# WHAT: Multi-stage container build for the SecureFileShare share management service.
# WHY: Builds a minimal, secure, non-root runtime image based on Eclipse Temurin 21 JRE Alpine,
#      optimized for linux/arm64 and linux/amd64 container environments.
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Maven Builder
# WHAT: Compiles common module and share-service into an executable fat JAR.
# WHY: Multi-stage build keeps build tools, Maven cache, and source code out of the
#      final runtime image, minimizing final image attack surface and size.
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy root POM and submodule POM definitions first
# Why: Leverages Docker layer caching so dependencies are re-downloaded only when pom.xml changes
COPY pom.xml ./
COPY common/pom.xml ./common/
COPY auth-service/pom.xml ./auth-service/
COPY file-service/pom.xml ./file-service/
COPY share-service/pom.xml ./share-service/
COPY audit-service/pom.xml ./audit-service/
COPY client-app/pom.xml ./client-app/

# Download dependencies offline for common and share-service modules
# Why: Speeds up subsequent rebuilds by keeping dependency downloads cached in a separate layer
RUN mvn dependency:go-offline -B -pl share-service -am --no-transfer-progress || true

# Copy source code for common dependency and share-service
COPY common/src ./common/src
COPY share-service/src ./share-service/src

# Package share-service fat JAR while skipping unit tests (tests are enforced during CI pipeline)
# Why: Prevents redundant test execution during image compilation while packaging required dependencies
RUN mvn clean package -pl share-service -am -DskipTests --no-transfer-progress

# ------------------------------------------------------------------------------
# Stage 2: Runtime Image
# WHAT: Production runtime running Eclipse Temurin 21 JRE on Alpine Linux.
# WHY: Alpine provides an ultra-compact footprint (~160MB) with strict non-root isolation.
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21.0.5_11-jre-alpine AS runner

# Install curl for container healthcheck probe
# Why: Alpine JRE image does not bundle curl by default; curl is required for HTTP /health probing
RUN apk add --no-cache curl=8.11.1-r0 || apk add --no-cache curl

# Create dedicated unprivileged system group and user
# Why: Principle of least privilege - running as non-root mitigates container breakout vulnerabilities
RUN addgroup -S sfsgroup -g 10001 && \
    adduser -S sfsuser -u 10001 -G sfsgroup -s /sbin/nologin

WORKDIR /app

# Copy packaged JAR artifact from builder stage with proper ownership
# Why: Preserves non-root file ownership to prevent write tampering by the runtime user
COPY --from=builder --chown=sfsuser:sfsgroup /build/share-service/target/share-service-*.jar /app/app.jar

# Enforce non-root execution
USER sfsuser:sfsgroup

# Configure container-aware JVM flags
# Why: UseContainerSupport ensures JVM respects cgroup limits; MaxRAMPercentage bounds heap to 75%
#      of container memory allocation (leaving headroom for Metaspace/OS); urandom prevents entropy blocking
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Expose internal service port
EXPOSE 8083

# Docker healthcheck probe against share-service /api/v1/health endpoint
# Why: Enables Docker daemon and orchestrators to detect deadlocks, crash loops, and startup failures
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8083/api/v1/health || exit 1

# Execute Java application with exec to guarantee PID 1 signal forwarding for graceful shutdown
# Why: Shell traps are avoided so SIGTERM propagates directly to JVM for clean connection draining
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
