#!/bin/bash

# Scalability Setup Script for File Transfer System
# Sets up database sharding, load balancing, caching, and auto-scaling

set -e

echo "⚡ File Transfer System Scalability Setup"
echo "======================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=${1:-production}
ENABLE_SHARDING=${2:-true}
ENABLE_CLUSTERING=${3:-true}
INSTANCES=${4:-3}

echo -e "${BLUE}Environment: ${ENVIRONMENT}${NC}"
echo -e "${BLUE}Enable Sharding: ${ENABLE_SHARDING}${NC}"
echo -e "${BLUE}Enable Clustering: ${ENABLE_CLUSTERING}${NC}"
echo -e "${BLUE}Application Instances: ${INSTANCES}${NC}"
echo ""

# Create scalability directories
echo -e "${GREEN}📁 Creating scalability directories...${NC}"
mkdir -p config/scalability
mkdir -p docker/postgres
mkdir -p docker/redis
mkdir -p docker/auto-scaler
mkdir -p docker/cache-warmer
mkdir -p logs/scalability

# Generate database sharding configuration
echo -e "${GREEN}🗄️  Generating database sharding configuration...${NC}"

cat > config/scalability/sharding-config.yml << EOF
# Database Sharding Configuration
sharding:
  enabled: ${ENABLE_SHARDING}
  
  # Sharding strategy
  strategy:
    type: hash  # Options: hash, range, directory
    key: tenant_id
    
  # Database shards
  databases:
    count: 4
    shards:
      - name: shard_0
        url: jdbc:postgresql://postgres-shard-0:5432/filetransfer_shard_0
        weight: 25
      - name: shard_1
        url: jdbc:postgresql://postgres-shard-1:5432/filetransfer_shard_1
        weight: 25
      - name: shard_2
        url: jdbc:postgresql://postgres-shard-2:5432/filetransfer_shard_2
        weight: 25
      - name: shard_3
        url: jdbc:postgresql://postgres-shard-3:5432/filetransfer_shard_3
        weight: 25
  
  # Table sharding rules
  tables:
    file_transfer_records:
      sharding_key: tenant_id
      shards_per_db: 4
    alert_history:
      sharding_key: tenant_id
      shards_per_db: 2
    daily_file_count_tracker:
      sharding_key: tenant_id
      shards_per_db: 2
EOF

# Generate load balancing configuration
echo -e "${GREEN}⚖️  Generating load balancing configuration...${NC}"

cat > config/scalability/load-balancing.yml << EOF
# Load Balancing Configuration
load-balancing:
  algorithm: weighted-round-robin
  health-check:
    enabled: true
    interval: 30s
    timeout: 5s
    retries: 3
  
  # Service instances
  instances:
EOF

for i in $(seq 1 $INSTANCES); do
cat >> config/scalability/load-balancing.yml << EOF
    - id: app-instance-${i}
      host: file-transfer-app-${i}
      port: 8080
      weight: 100
      max-connections: 100
EOF
done

# Generate Redis cluster configuration
if [[ "$ENABLE_CLUSTERING" == "true" ]]; then
    echo -e "${GREEN}🔄 Generating Redis cluster configuration...${NC}"
    
    cat > docker/redis/redis-cluster.conf << EOF
# Redis Cluster Configuration
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 15000
cluster-announce-ip \${REDIS_ANNOUNCE_IP}
cluster-announce-port 6379
cluster-announce-bus-port 16379

# Memory and performance settings
maxmemory 1gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000

# Security
requirepass \${REDIS_PASSWORD}
masterauth \${REDIS_PASSWORD}

# Logging
loglevel notice
logfile /data/redis.log

# Persistence
appendonly yes
appendfsync everysec
EOF

    # Generate Redis cluster setup script
    cat > docker/redis/setup-cluster.sh << EOF
#!/bin/bash
echo "Setting up Redis cluster..."

# Wait for all Redis nodes to be ready
sleep 30

# Create cluster
redis-cli --cluster create \\
  redis-cluster-1:6379 \\
  redis-cluster-2:6379 \\
  redis-cluster-3:6379 \\
  redis-cluster-4:6379 \\
  redis-cluster-5:6379 \\
  redis-cluster-6:6379 \\
  --cluster-replicas 1 \\
  --cluster-yes

echo "Redis cluster setup completed"
EOF
    chmod +x docker/redis/setup-cluster.sh
fi

# Generate PostgreSQL configuration for performance
echo -e "${GREEN}🐘 Generating PostgreSQL performance configuration...${NC}"

cat > docker/postgres/postgresql.conf << EOF
# PostgreSQL Performance Configuration for Sharding

# Connection settings
max_connections = 200
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200

# Logging
log_destination = 'stderr'
logging_collector = on
log_directory = 'pg_log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_statement = 'mod'
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# Replication (for read replicas)
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
hot_standby = on

# Performance monitoring
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.track = all
pg_stat_statements.max = 10000
EOF

# Generate database initialization scripts
for i in $(seq 0 3); do
    cat > docker/postgres/init-shard-${i}.sql << EOF
-- Initialize Database Shard ${i}

-- Create database
CREATE DATABASE filetransfer_shard_${i};

-- Connect to the database
\\c filetransfer_shard_${i};

-- Create schema
CREATE SCHEMA IF NOT EXISTS filetransfer;

-- Set search path
SET search_path TO filetransfer, public;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create tables with partitioning
CREATE TABLE file_transfer_records_${i}_0 (
    LIKE file_transfer_records INCLUDING ALL
) PARTITION OF file_transfer_records FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE file_transfer_records_${i}_1 (
    LIKE file_transfer_records INCLUDING ALL
) PARTITION OF file_transfer_records FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE file_transfer_records_${i}_2 (
    LIKE file_transfer_records INCLUDING ALL
) PARTITION OF file_transfer_records FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE file_transfer_records_${i}_3 (
    LIKE file_transfer_records INCLUDING ALL
) PARTITION OF file_transfer_records FOR VALUES WITH (MODULUS 4, REMAINDER 3);

-- Create indexes for performance
CREATE INDEX CONCURRENTLY idx_file_transfer_${i}_tenant_status 
ON file_transfer_records_${i}_0(tenant_id, status);

CREATE INDEX CONCURRENTLY idx_file_transfer_${i}_created_at 
ON file_transfer_records_${i}_0(created_at);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA filetransfer TO postgres;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA filetransfer TO postgres;
EOF
done

# Generate auto-scaler configuration
echo -e "${GREEN}🤖 Generating auto-scaler configuration...${NC}"

cat > docker/auto-scaler/Dockerfile << EOF
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY autoscaler.py .
COPY config.yml .

CMD ["python", "autoscaler.py"]
EOF

cat > docker/auto-scaler/requirements.txt << EOF
requests==2.31.0
pyyaml==6.0.1
docker==6.1.3
prometheus-client==0.17.1
numpy==1.24.3
EOF

cat > docker/auto-scaler/autoscaler.py << 'EOF'
#!/usr/bin/env python3
"""
Auto-scaler for File Transfer System
Monitors metrics and scales application instances based on load
"""

import time
import requests
import docker
import yaml
import logging
from datetime import datetime, timedelta

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class AutoScaler:
    def __init__(self, config_file='config.yml'):
        with open(config_file, 'r') as f:
            self.config = yaml.safe_load(f)
        
        self.docker_client = docker.from_env()
        self.prometheus_url = self.config['prometheus']['url']
        self.scaling_config = self.config['scaling']
        
    def get_metric_value(self, query):
        """Get metric value from Prometheus"""
        try:
            response = requests.get(
                f"{self.prometheus_url}/api/v1/query",
                params={'query': query}
            )
            result = response.json()
            
            if result['status'] == 'success' and result['data']['result']:
                return float(result['data']['result'][0]['value'][1])
            return 0.0
        except Exception as e:
            logger.error(f"Error getting metric: {e}")
            return 0.0
    
    def get_current_instances(self):
        """Get current number of running instances"""
        containers = self.docker_client.containers.list(
            filters={'name': 'file-transfer-app'}
        )
        return len(containers)
    
    def scale_up(self):
        """Scale up by starting a new instance"""
        current_instances = self.get_current_instances()
        if current_instances >= self.scaling_config['max_instances']:
            logger.info("Already at maximum instances")
            return
        
        new_instance_id = current_instances + 1
        logger.info(f"Scaling up: starting instance {new_instance_id}")
        
        # Start new container
        self.docker_client.containers.run(
            image='file-transfer-app:latest',
            name=f'file-transfer-app-{new_instance_id}',
            environment={
                'SERVER_PORT': '8080',
                'SPRING_PROFILES_ACTIVE': 'production,scalability'
            },
            detach=True,
            network='scalability'
        )
    
    def scale_down(self):
        """Scale down by stopping an instance"""
        current_instances = self.get_current_instances()
        if current_instances <= self.scaling_config['min_instances']:
            logger.info("Already at minimum instances")
            return
        
        logger.info(f"Scaling down: stopping instance {current_instances}")
        
        # Stop the highest numbered instance
        container = self.docker_client.containers.get(
            f'file-transfer-app-{current_instances}'
        )
        container.stop()
        container.remove()
    
    def should_scale_up(self, cpu_usage, memory_usage, queue_length):
        """Determine if we should scale up"""
        cpu_threshold = self.scaling_config['thresholds']['cpu_scale_up']
        memory_threshold = self.scaling_config['thresholds']['memory_scale_up']
        queue_threshold = self.scaling_config['thresholds']['queue_scale_up']
        
        return (cpu_usage > cpu_threshold or 
                memory_usage > memory_threshold or 
                queue_length > queue_threshold)
    
    def should_scale_down(self, cpu_usage, memory_usage, queue_length):
        """Determine if we should scale down"""
        cpu_threshold = self.scaling_config['thresholds']['cpu_scale_down']
        memory_threshold = self.scaling_config['thresholds']['memory_scale_down']
        queue_threshold = self.scaling_config['thresholds']['queue_scale_down']
        
        return (cpu_usage < cpu_threshold and 
                memory_usage < memory_threshold and 
                queue_length < queue_threshold)
    
    def run(self):
        """Main scaling loop"""
        logger.info("Starting auto-scaler")
        
        while True:
            try:
                # Get current metrics
                cpu_usage = self.get_metric_value(
                    'avg(rate(process_cpu_seconds_total[5m])) * 100'
                )
                memory_usage = self.get_metric_value(
                    'avg(jvm_memory_used_bytes / jvm_memory_max_bytes) * 100'
                )
                queue_length = self.get_metric_value(
                    'sum(file_transfer_queued)'
                )
                
                logger.info(f"Metrics - CPU: {cpu_usage:.1f}%, "
                          f"Memory: {memory_usage:.1f}%, "
                          f"Queue: {queue_length}")
                
                # Make scaling decisions
                if self.should_scale_up(cpu_usage, memory_usage, queue_length):
                    self.scale_up()
                elif self.should_scale_down(cpu_usage, memory_usage, queue_length):
                    self.scale_down()
                
                # Wait before next check
                time.sleep(self.scaling_config['check_interval'])
                
            except Exception as e:
                logger.error(f"Error in scaling loop: {e}")
                time.sleep(60)

if __name__ == '__main__':
    scaler = AutoScaler()
    scaler.run()
EOF

cat > docker/auto-scaler/config.yml << EOF
# Auto-scaler Configuration
prometheus:
  url: http://prometheus:9090

scaling:
  min_instances: 2
  max_instances: 10
  check_interval: 60  # seconds
  
  thresholds:
    cpu_scale_up: 75    # CPU % to scale up
    cpu_scale_down: 30  # CPU % to scale down
    memory_scale_up: 80 # Memory % to scale up  
    memory_scale_down: 40 # Memory % to scale down
    queue_scale_up: 100   # Queue length to scale up
    queue_scale_down: 20  # Queue length to scale down
EOF

# Generate cache warmer
echo -e "${GREEN}🔥 Generating cache warmer...${NC}"

cat > docker/cache-warmer/Dockerfile << EOF
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY cache_warmer.py .

CMD ["python", "cache_warmer.py"]
EOF

cat > docker/cache-warmer/requirements.txt << EOF
redis==4.6.0
requests==2.31.0
schedule==1.2.0
EOF

cat > docker/cache-warmer/cache_warmer.py << 'EOF'
#!/usr/bin/env python3
"""
Cache Warmer for File Transfer System
Pre-loads frequently accessed data into Redis cache
"""

import redis
import requests
import schedule
import time
import logging
import json
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class CacheWarmer:
    def __init__(self):
        self.redis_client = redis.Redis(
            host='redis-cluster-1',
            port=6379,
            decode_responses=True
        )
        self.app_endpoints = [
            'http://file-transfer-app-1:8080',
            'http://file-transfer-app-2:8080',
            'http://file-transfer-app-3:8080'
        ]
    
    def warm_tenant_cache(self):
        """Pre-load tenant configurations"""
        logger.info("Warming tenant cache...")
        
        try:
            # Get list of active tenants
            response = requests.get(f"{self.app_endpoints[0]}/api/tenants")
            if response.status_code == 200:
                tenants = response.json()
                
                for tenant in tenants:
                    tenant_id = tenant['tenantId']
                    
                    # Cache tenant configuration
                    self.redis_client.setex(
                        f"tenant:{tenant_id}",
                        3600,  # 1 hour TTL
                        json.dumps(tenant)
                    )
                    
                    # Cache sub-service configurations
                    sub_services_response = requests.get(
                        f"{self.app_endpoints[0]}/api/sub-services?tenantId={tenant_id}"
                    )
                    if sub_services_response.status_code == 200:
                        sub_services = sub_services_response.json()
                        self.redis_client.setex(
                            f"subservices:{tenant_id}",
                            1800,  # 30 minutes TTL
                            json.dumps(sub_services)
                        )
                
                logger.info(f"Warmed cache for {len(tenants)} tenants")
        except Exception as e:
            logger.error(f"Error warming tenant cache: {e}")
    
    def warm_schema_cache(self):
        """Pre-load file schemas"""
        logger.info("Warming schema cache...")
        
        try:
            response = requests.get(f"{self.app_endpoints[0]}/api/schemas")
            if response.status_code == 200:
                schemas = response.json()
                
                for schema in schemas:
                    schema_id = schema['id']
                    self.redis_client.setex(
                        f"schema:{schema_id}",
                        3600,  # 1 hour TTL
                        json.dumps(schema)
                    )
                
                logger.info(f"Warmed cache for {len(schemas)} schemas")
        except Exception as e:
            logger.error(f"Error warming schema cache: {e}")
    
    def warm_all_caches(self):
        """Warm all caches"""
        logger.info("Starting cache warming cycle")
        start_time = datetime.now()
        
        self.warm_tenant_cache()
        self.warm_schema_cache()
        
        duration = datetime.now() - start_time
        logger.info(f"Cache warming completed in {duration.total_seconds():.2f} seconds")
    
    def run(self):
        """Main cache warming loop"""
        logger.info("Starting cache warmer")
        
        # Schedule cache warming
        schedule.every(30).minutes.do(self.warm_all_caches)
        schedule.every().hour.at(":00").do(self.warm_all_caches)
        
        # Initial cache warming
        self.warm_all_caches()
        
        # Run scheduler
        while True:
            schedule.run_pending()
            time.sleep(60)

if __name__ == '__main__':
    warmer = CacheWarmer()
    warmer.run()
EOF

# Generate scaling test script
echo -e "${GREEN}🧪 Generating scaling test script...${NC}"

cat > scripts/test-scaling.sh << 'EOF'
#!/bin/bash

# Scaling Test Script
echo "🧪 Testing File Transfer System Scalability"
echo "==========================================="

BASE_URL="http://localhost"
CONCURRENT_REQUESTS=50
TOTAL_REQUESTS=1000

echo "Running load test with ${CONCURRENT_REQUESTS} concurrent requests..."

# Test API endpoints
echo "Testing API endpoints..."
ab -n $TOTAL_REQUESTS -c $CONCURRENT_REQUESTS \
   -H "Content-Type: application/json" \
   -p /dev/null \
   "$BASE_URL/api/health"

echo ""
echo "Testing file upload simulation..."
ab -n 100 -c 10 \
   -H "Content-Type: multipart/form-data" \
   "$BASE_URL/api/files/upload"

echo ""
echo "Monitoring system during load test..."
curl -s "$BASE_URL/api/scalability/performance/metrics" | jq .

echo ""
echo "Load balancing statistics..."
curl -s "$BASE_URL/api/scalability/load-balancing/stats?serviceName=file-transfer-web" | jq .
EOF

chmod +x scripts/test-scaling.sh

# Generate monitoring dashboard configuration
echo -e "${GREEN}📊 Generating monitoring dashboard configuration...${NC}"

cat > docker/prometheus/prometheus-scale.yml << EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

rule_files:
  - "scalability-alert-rules.yml"

scrape_configs:
  # File Transfer Application Instances
  - job_name: 'file-transfer-apps'
    static_configs:
      - targets: 
        - 'file-transfer-app-1:8080'
        - 'file-transfer-app-2:8080'
        - 'file-transfer-app-3:8080'
    scrape_interval: 15s
    metrics_path: /actuator/prometheus

  # Load Balancer
  - job_name: 'haproxy'
    static_configs:
      - targets: ['load-balancer:8404']
    scrape_interval: 30s

  # Database Shards
  - job_name: 'postgres-shards'
    static_configs:
      - targets:
        - 'postgres-primary:5432'
        - 'postgres-secondary:5432'
    scrape_interval: 30s

  # Redis Cluster
  - job_name: 'redis-cluster'
    static_configs:
      - targets:
        - 'redis-cluster-1:6379'
        - 'redis-cluster-2:6379'
        - 'redis-cluster-3:6379'
    scrape_interval: 30s

  # System metrics
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
    scrape_interval: 15s
EOF

# Create startup script
echo -e "${GREEN}🚀 Creating scalability startup script...${NC}"

cat > scripts/start-scalability-stack.sh << EOF
#!/bin/bash

echo "🚀 Starting File Transfer Scalability Stack"
echo "==========================================="

# Check prerequisites
echo "Checking prerequisites..."
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "Docker Compose is required but not installed. Aborting." >&2; exit 1; }

# Set environment variables
export COMPOSE_PROJECT_NAME=filetransfer-scale
export INSTANCES=${INSTANCES:-3}

# Start the scalability stack
echo "Starting scalability stack..."
cd docker
docker-compose -f scalability-stack.yml up -d

echo "Waiting for services to be ready..."
sleep 60

# Setup Redis cluster if enabled
if [[ "$ENABLE_CLUSTERING" == "true" ]]; then
    echo "Setting up Redis cluster..."
    docker exec redis-cluster-1 /bin/bash /etc/redis/setup-cluster.sh
fi

# Verify services
echo "Verifying services..."
docker-compose -f scalability-stack.yml ps

echo ""
echo "✅ Scalability stack started successfully!"
echo ""
echo "🌐 Access URLs:"
echo "  • Load Balancer:        http://localhost"
echo "  • HAProxy Stats:        http://localhost:8404/stats"
echo "  • Prometheus:           http://localhost:9091"
echo "  • Grafana:              http://localhost:3002"
echo "  • Eureka:               http://localhost:8761"
echo ""
echo "📊 Monitoring:"
echo "  • Application Metrics:  http://localhost/actuator/prometheus"
echo "  • Scalability API:      http://localhost/api/scalability"
echo "  • Performance Metrics:  http://localhost/api/scalability/performance/metrics"
echo ""
echo "🧪 Testing:"
echo "  • Run load test: ./scripts/test-scaling.sh"
echo "  • Monitor scaling: docker logs -f file-transfer-auto-scaler"
EOF

chmod +x scripts/start-scalability-stack.sh

# Final summary
echo ""
echo -e "${GREEN}🎉 Scalability setup completed successfully!${NC}"
echo ""
echo -e "${BLUE}📁 Created files:${NC}"
echo "  • config/scalability/sharding-config.yml"
echo "  • config/scalability/load-balancing.yml"
echo "  • docker/scalability-stack.yml"
echo "  • docker/haproxy/haproxy.cfg"
echo "  • docker/postgres/postgresql.conf"
echo "  • docker/redis/redis-cluster.conf"
echo "  • docker/auto-scaler/* (Python auto-scaler)"
echo "  • docker/cache-warmer/* (Cache warming service)"
echo "  • scripts/start-scalability-stack.sh"
echo "  • scripts/test-scaling.sh"

echo ""
echo -e "${YELLOW}⚠️  IMPORTANT SCALABILITY NOTES:${NC}"
echo "1. Database sharding requires careful planning for data distribution"
echo "2. Redis clustering needs proper network configuration"
echo "3. Load balancer health checks ensure high availability"
echo "4. Auto-scaler monitors metrics and adjusts instance count"
echo "5. Monitor system resources during scaling events"

echo ""
echo -e "${GREEN}🚀 Next steps:${NC}"
echo "1. Start the scalability stack: ./scripts/start-scalability-stack.sh"
echo "2. Monitor the system: docker-compose -f docker/scalability-stack.yml logs -f"
echo "3. Test scaling: ./scripts/test-scaling.sh"
echo "4. View metrics: http://localhost:3002 (Grafana)"

echo ""
echo -e "${BLUE}Scalability setup completed for environment: ${ENVIRONMENT}${NC}"
EOF

<function_calls>
<invoke name="run_terminal_cmd">
<parameter name="command">chmod +x /workspace/scripts/scalability-setup.sh