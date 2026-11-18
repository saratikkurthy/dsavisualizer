# DSA Visualizer backend — Dockerfile for Render (Language: Docker).
#
# Render has NO native Java runtime (its Language dropdown is Node,
# Python, Ruby, Go, Rust, Elixir, Docker). Java/Kotlin/Scala apps must
# deploy with Docker — Render's own docs say so. So this file builds
# the Spring Boot JAR with Maven and runs it on a small JRE image.
#
# Multi-stage build:
#   Stage 1 (build): Maven + JDK 17 — compiles and packages the JAR.
#   Stage 2 (run):   JRE 17 only — small image, runs the JAR.
# Render sets the PORT env var at runtime; the app reads it via
# server.port=${PORT:8080} (application.properties) and the -D flag
# below. -Xmx384m keeps the JVM inside Render's free-tier 512 MB RAM.

# ---- Stage 1: build the JAR ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests clean package

# ---- Stage 2: run the JAR ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/dsa-visualizer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Xmx384m -Dserver.port=${PORT:-8080} -jar app.jar"]
