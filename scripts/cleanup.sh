#!/bin/bash

echo "Cleaning up File Transfer Application..."

# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Remove all images related to the project
docker images | grep file-transfer | awk '{print $3}' | xargs -r docker rmi

# Clean up any dangling images
docker system prune -f

echo "Cleanup completed successfully."