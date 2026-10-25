# Multi-stage Java 21 image suitable for Docker-based low-cost hosting.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 portfolio
COPY --from=build /build/target/portfolio-*.jar /app/portfolio.jar
USER portfolio
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:ActiveProcessorCount=2"
ENTRYPOINT ["java", "-jar", "/app/portfolio.jar"]
