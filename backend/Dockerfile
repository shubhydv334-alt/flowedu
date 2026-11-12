# syntax=docker/dockerfile:1

# ---- build stage: compile and package the executable jar (tests run in CI)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime stage: small JRE image, non-root user
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S flowedu && adduser -S flowedu -G flowedu
WORKDIR /app
COPY --from=build /workspace/target/flowedu-*.jar app.jar
USER flowedu
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
