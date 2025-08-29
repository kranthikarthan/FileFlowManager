#!/bin/bash

# Start File Transfer System Monitoring Stack
# This script starts Prometheus, Grafana, Zipkin, and ELK stack for comprehensive monitoring

set -e

echo "🚀 Starting File Transfer System Monitoring Stack..."

# Create necessary directories
echo "📁 Creating monitoring directories..."
mkdir -p logs
mkdir -p docker/prometheus/data
mkdir -p docker/grafana/data
mkdir -p docker/elasticsearch/data
mkdir -p docker/logstash/data

# Set proper permissions
echo "🔒 Setting permissions..."
chmod 755 logs
chmod 755 docker/prometheus/data
chmod 755 docker/grafana/data
sudo chown -R 1000:1000 docker/elasticsearch/data
sudo chown -R 1000:1000 docker/grafana/data

# Start the monitoring stack
echo "🏗️  Starting monitoring containers..."
cd docker
docker-compose -f monitoring-stack.yml up -d

echo "⏳ Waiting for services to start up..."
sleep 30

# Check service health
echo "🏥 Checking service health..."

# Check Prometheus
if curl -s http://localhost:9090/-/healthy > /dev/null; then
    echo "✅ Prometheus is healthy"
else
    echo "❌ Prometheus is not responding"
fi

# Check Grafana
if curl -s http://localhost:3001/api/health > /dev/null; then
    echo "✅ Grafana is healthy"
else
    echo "❌ Grafana is not responding"
fi

# Check Zipkin
if curl -s http://localhost:9411/health > /dev/null; then
    echo "✅ Zipkin is healthy"
else
    echo "❌ Zipkin is not responding"
fi

# Check Elasticsearch
if curl -s http://localhost:9200/_health > /dev/null; then
    echo "✅ Elasticsearch is healthy"
else
    echo "❌ Elasticsearch is not responding"
fi

# Check Kibana
if curl -s http://localhost:5601/api/status > /dev/null; then
    echo "✅ Kibana is healthy"
else
    echo "❌ Kibana is not responding"
fi

echo ""
echo "🎉 Monitoring stack startup complete!"
echo ""
echo "📊 Access URLs:"
echo "  • Prometheus:     http://localhost:9090"
echo "  • Grafana:        http://localhost:3001 (admin/admin123)"
echo "  • Zipkin:         http://localhost:9411"
echo "  • Elasticsearch:  http://localhost:9200"
echo "  • Kibana:         http://localhost:5601"
echo "  • AlertManager:   http://localhost:9093"
echo ""
echo "📈 Application Metrics:"
echo "  • Spring Actuator: http://localhost:8080/actuator"
echo "  • Prometheus Metrics: http://localhost:8080/actuator/prometheus"
echo "  • Health Check: http://localhost:8080/actuator/health"
echo ""
echo "🔍 To view logs:"
echo "  docker-compose -f monitoring-stack.yml logs -f [service-name]"
echo ""
echo "🛑 To stop monitoring:"
echo "  docker-compose -f monitoring-stack.yml down"
echo ""

# Wait for user input
read -p "Press Enter to continue or Ctrl+C to exit..."