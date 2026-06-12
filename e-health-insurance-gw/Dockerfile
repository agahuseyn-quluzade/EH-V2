FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN rm -f gradle.properties && chmod +x gradlew && ./gradlew bootJar -x test --no-daemon --console=plain

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
