FROM eclipse-temurin:21-jdk-jammy AS infra-build
WORKDIR /infra
COPY --from=infra . .
RUN rm -f gradle.properties && chmod +x gradlew && ./gradlew publishToMavenLocal --no-daemon --console=plain

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY --from=infra-build /root/.m2 /root/.m2
COPY . .
RUN rm -f gradle.properties && chmod +x gradlew && ./gradlew bootJar -x test --no-daemon --console=plain

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
