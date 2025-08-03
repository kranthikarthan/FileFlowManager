#!/bin/bash

# File Transfer Application Deployment Script
echo "Starting File Transfer Application..."

# Check if Docker and Docker Compose are installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create Maven wrapper files if they don't exist
create_maven_wrapper() {
    local project_dir=$1
    if [ ! -f "$project_dir/mvnw" ]; then
        echo "Creating Maven wrapper for $project_dir..."
        cd "$project_dir"
        if command -v mvn &> /dev/null; then
            mvn wrapper:wrapper
        else
            echo "Maven is not installed. Creating minimal wrapper files..."
            mkdir -p .mvn/wrapper
            cat > .mvn/wrapper/maven-wrapper.properties << 'EOF'
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.5/apache-maven-3.9.5-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
EOF
            
            cat > mvnw << 'EOF'
#!/bin/sh
exec java -classpath .mvn/wrapper/maven-wrapper.jar "-Dmaven.multiModuleProjectDirectory=$PWD" org.apache.maven.wrapper.MavenWrapperMain "$@"
EOF
            chmod +x mvnw
            
            cat > mvnw.cmd << 'EOF'
@echo off
java -classpath .mvn/wrapper/maven-wrapper.jar "-Dmaven.multiModuleProjectDirectory=%CD%" org.apache.maven.wrapper.MavenWrapperMain %*
EOF
        fi
        cd - > /dev/null
    fi
}

# Create Maven wrappers for both Spring Boot projects
create_maven_wrapper "file-transfer-batch"
create_maven_wrapper "file-transfer-web"

# Create package-lock.json for React app if it doesn't exist
if [ ! -f "file-transfer-frontend/package-lock.json" ]; then
    echo "Creating package-lock.json for React frontend..."
    cd file-transfer-frontend
    npm install --package-lock-only
    cd - > /dev/null
fi

# Build and start all services
echo "Building and starting all services..."
docker-compose up --build -d

# Wait for services to start
echo "Waiting for services to start..."
sleep 30

# Check service health
echo "Checking service health..."
echo "MySQL: $(docker-compose ps mysql | grep -o 'healthy\|unhealthy' | head -1)"
echo "Batch App: $(docker-compose ps file-transfer-batch | grep -o 'Up\|Exited' | head -1)"
echo "Web App: $(docker-compose ps file-transfer-web | grep -o 'Up\|Exited' | head -1)"
echo "Frontend: $(docker-compose ps file-transfer-frontend | grep -o 'Up\|Exited' | head -1)"

echo ""
echo "Application URLs:"
echo "Frontend: http://localhost:3000"
echo "Web API: http://localhost:8080"
echo "Batch App: http://localhost:8081"
echo "MySQL: localhost:3306"
echo ""
echo "To view logs: docker-compose logs -f [service-name]"
echo "To stop: docker-compose down"