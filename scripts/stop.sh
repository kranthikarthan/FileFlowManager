#!/bin/bash

echo "Stopping File Transfer Application..."

# Stop all services
docker-compose down

# Optional: Remove volumes (uncomment if you want to reset data)
# echo "Removing volumes..."
# docker-compose down -v

echo "Application stopped successfully."