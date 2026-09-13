# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Build definition first, sources after. This layer only rebuilds when the build files
# change, so an ordinary code edit reuses the dependency cache instead of re-downloading
# the world on every deploy.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies

COPY config config
COPY src src
# Tests are excluded deliberately. They need a real PostgreSQL and RabbitMQ through
# Testcontainers, which means a Docker daemon inside the image build; CI already runs
# them against real containers before anything reaches here.
RUN ./gradlew --no-daemon bootJar -x test


FROM eclipse-temurin:17-jre-jammy AS runtime

# curl is here for the container healthcheck and nothing else — the JRE image ships
# without it, and a compose healthcheck needs something inside the container to run.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 lumen

WORKDIR /app
COPY --from=build --chown=lumen:lumen /workspace/build/libs/*.jar app.jar
USER lumen

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

# MaxRAMPercentage rather than a fixed -Xmx: the heap should be sized from the memory
# limit the platform actually enforces, not from a number guessed at build time.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
