# syntax=docker/dockerfile:1.7
# Build context: repository root (parent directory)

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY ehi-common-security/ ./ehi-common-security/
COPY ehi-common-events/ ./ehi-common-events/

WORKDIR /workspace/service
COPY e-health-insurance-notification/gradlew \
     e-health-insurance-notification/settings.gradle \
     e-health-insurance-notification/build.gradle ./
COPY e-health-insurance-notification/gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY e-health-insurance-notification/src ./src
RUN ./gradlew --no-daemon clean bootJar -x test && \
    cp build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

# Healthcheck üçün curl (compose actuator readiness/liveness probe-larında istifadə olunur)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring spring
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

USER spring:spring

COPY --from=build /workspace/app.jar ./app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
