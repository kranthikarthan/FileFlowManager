FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy Maven wrapper and pom.xml first for better caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create directories for file transfer
RUN mkdir -p /app/data/inbound/service1 \
             /app/data/inbound/service2 \
             /app/data/outbound/service1 \
             /app/data/outbound/service2 \
             /app/logs

# Expose port
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "target/file-transfer-batch-1.0.0.jar"]