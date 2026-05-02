# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache Maven dependencies separately from source code
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /build/target/api-gateway-*.jar app.jar

# Expose the gateway port
EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport   — respect cgroup CPU/memory limits
#   -XX:MaxRAMPercentage=75.0  — leave headroom for OS and off-heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
