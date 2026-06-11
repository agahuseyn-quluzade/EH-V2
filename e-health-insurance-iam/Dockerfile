# syntax=docker/dockerfile:1.7
# Build context: parent directory (root of the e-health-insurance repo)
# ki, hem common-security, hem də servis qovluğu əlçatandır.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Ortaq modullar (includeBuild ../ehi-common-security, ../ehi-common-events)
COPY ehi-common-security/ ./ehi-common-security/
COPY ehi-common-events/ ./ehi-common-events/

# Servis: gradle wrapper + build script-ləri əvvəl köçür ki dependency layer cache olunsun
WORKDIR /workspace/service
COPY e-health-insurance-iam/gradlew \
     e-health-insurance-iam/settings.gradle \
     e-health-insurance-iam/build.gradle ./
COPY e-health-insurance-iam/gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Mənbə kodu — bunlar tez-tez dəyişdiyi üçün sonda
COPY e-health-insurance-iam/src ./src
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

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
