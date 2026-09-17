FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests
FROM docker:27-cli AS dockercli
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends git ca-certificates curl && rm -rf /var/lib/apt/lists/* && useradd --uid 10001 --create-home app
COPY --from=dockercli /usr/local/bin/docker /usr/local/bin/docker
WORKDIR /app
RUN mkdir -p /app/data && chown -R app:app /app/data
COPY --from=build /build/target/*.jar app.jar
RUN chmod 0444 /app/app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java","-jar","/app/app.jar"]
