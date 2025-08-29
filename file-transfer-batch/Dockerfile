# Dockerfile for Scalable Batch Processing Application

FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Install useful tools for debugging and monitoring
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    htop \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN useradd -r -s /bin/false batchuser

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (this layer will be cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create directory for application
RUN mkdir -p /app/batch-app

# Copy the built JAR
RUN cp target/*.jar /app/batch-app/app.jar

# Change ownership
RUN chown -R batchuser:batchuser /app

# Switch to application user
USER batchuser

# Set JVM options for scalability
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+PrintGCDetails -Djava.security.egd=file:/dev/./urandom"

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/batch-app/app.jar"]