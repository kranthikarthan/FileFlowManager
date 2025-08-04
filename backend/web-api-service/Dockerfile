# Multi-stage build for Spring Boot Web API Service
FROM openjdk:17-jdk-slim as build

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy shared library first (for better caching)
COPY ../shared /shared
WORKDIR /shared
RUN mvn clean install -DskipTests

# Copy and build web API service
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM openjdk:17-jre-slim

# Add non-root user for security
RUN groupadd -r fileflow && useradd -r -g fileflow fileflow

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the jar file
COPY --from=build /app/target/*.jar app.jar

# Change ownership to fileflow user
RUN chown -R fileflow:fileflow /app

# Switch to non-root user
USER fileflow

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]