# syntax=docker/dockerfile:1.7
# ---------------------------------------------------------------------------
# BACK-JOBS-API — imagen multi-stage
#   deps    -> descarga dependencias Maven (capa cacheada)
#   test    -> corre los tests (unitarios + integración)   docker build --target test .
#   build   -> genera el .jar sin tests
#   runtime -> imagen liviana para ejecutar la API (default)
# ---------------------------------------------------------------------------

ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

FROM ${MAVEN_IMAGE} AS deps
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline -DexcludeReactor=true || true

FROM deps AS test
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B test

FROM deps AS build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package \
 && cp target/*.jar /app/app.jar

FROM ${RUNTIME_IMAGE} AS runtime
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=build /app/app.jar app.jar
USER spring
EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
