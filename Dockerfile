# ============================================================
# Doctor Queue SaaS — Multi-Stage Dockerfile
# ============================================================

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Cache Maven dependencies first
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

COPY src ./src
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -B

# Stage 2: Runtime (minimal JRE image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser
EXPOSE 8080

# Graceful shutdown support
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
