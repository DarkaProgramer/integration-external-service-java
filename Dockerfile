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
# Render asigna el puerto en la variable PORT (10000 por defecto; server.port=${PORT:8081})
EXPOSE 10000
# Plan free de Render: 0.1 CPU y 512 MB. Estas opciones acortan el arranque y bajan el consumo de memoria:
# - preferIPv4Stack: escucha en 0.0.0.0 (IPv4) para que el escáner de puertos de Render detecte el servicio
# - TieredStopAtLevel=1 + SerialGC: menos trabajo del compilador JIT y del GC con un solo núcleo
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
