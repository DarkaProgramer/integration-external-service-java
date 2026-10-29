# ── Etapa 1: compilar ──────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
# Las pruebas se ejecutan antes del push (./gradlew test); aquí solo se empaqueta
RUN ./gradlew --no-daemon bootJar -x test && cp build/libs/*-1.0.jar app.jar

# ── Etapa 2: ejecutar (solo JRE, usuario sin privilegios) ──────────
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/app.jar app.jar
USER app
# Render asigna el puerto en la variable PORT (server.port=${PORT:8081})
EXPOSE 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
