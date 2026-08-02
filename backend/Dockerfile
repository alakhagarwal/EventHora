# ==============================================================================
# Multi-stage Dockerfile for EventHora Spring Boot Backend (Java 21)
# Optimized for Render, Railway, or AWS ECS deployment
# ==============================================================================

# ─── Stage 1: Build JAR using Maven & Java 21 ─────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build production JAR (skipping tests for fast cloud builds)
RUN mvn clean package -DskipTests

# ─── Stage 2: Minimal Lightweight Runtime ─────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the compiled JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Render assigns a dynamic PORT environment variable (default: 8080)
EXPOSE 8080

# Launch Spring Boot using sh -c so $PORT env var is expanded correctly
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
