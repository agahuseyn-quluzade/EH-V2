# syntax=docker/dockerfile:1.7
# Build context: repository root (parent directory) — vahid pattern, common-security istifadə etmir.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace/service

COPY e-health-insurance-gw/gradlew \
     e-health-insurance-gw/settings.gradle \
     e-health-insurance-gw/build.gradle ./
COPY e-health-insurance-gw/gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY e-health-insurance-gw/src ./src
RUN ./gradlew --no-daemon clean bootJar -x test && \
    cp build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

# Healthcheck üçün curl (compose actuator readiness/liveness probe-larında istifadə olunur)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
