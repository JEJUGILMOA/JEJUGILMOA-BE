# syntax=docker/dockerfile:1

# ==================================================
# Build stage
# ==================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY . .

RUN chmod +x gradlew

# 테스트는 GitHub Actions의 PostGIS 환경에서 수행
RUN ./gradlew clean bootJar -x test --no-daemon

# -plain.jar가 아닌 Spring Boot 실행 JAR만 선택
RUN JAR_FILE="$(find build/libs \
      -maxdepth 1 \
      -type f \
      -name '*.jar' \
      ! -name '*-plain.jar' \
      -print \
      -quit)" \
    && test -n "${JAR_FILE}" \
    && cp "${JAR_FILE}" /workspace/app.jar


# ==================================================
# Runtime stage
# ==================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --no-create-home spring

COPY --from=builder \
     --chown=spring:spring \
     /workspace/app.jar \
     /app/app.jar

USER spring

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/health | grep -q "ok" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
