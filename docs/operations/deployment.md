---
title: Deployment Guide
---

# Deployment Guide

This guide covers deploying the orchestration framework across the fleet. The system consists of a controller node (aio-01), fleet workers, scrapers, store workers, embedding workers, and supporting databases.

## Automated Deployment with Ansible

The primary deployment method uses Ansible playbooks that automate the full setup across all nodes.

```bash
# Full fleet deployment (all components)
ansible-playbook -i inventory/fleet.ini playbooks/deploy-all.yml

# Controller only
ansible-playbook -i inventory/fleet.ini playbooks/deploy-controller.yml

# Workers only
ansible-playbook -i inventory/fleet.ini playbooks/deploy-workers.yml

# Database setup only
ansible-playbook -i inventory/fleet.ini playbooks/deploy-databases.yml
```

The `deploy-all.yml` playbook executes 10 phases in order:

1. **Pre-flight checks** -- Verify SSH connectivity, disk space, Python/Node versions on all nodes.
2. **Database deployment** -- Install/configure PostgreSQL with pgvector, Redis, OrientDB.
3. **Schema migrations** -- Apply all pending migrations to PostgreSQL schemas (`learning.*`, `monitoring.*`, `costs.*`, `knowledge.*`, `workflow.*`).
4. **Controller deployment** -- Deploy the Flask API to aio-01, configure systemd service, start and verify.
5. **Worker deployment** -- Deploy worker scripts to all fleet nodes, register each with the controller.
6. **Scraper deployment** -- Deploy scraper scripts to designated scraper nodes.
7. **Store worker deployment** -- Deploy store workers with persistent storage paths.
8. **Embedding worker deployment** -- Deploy embedding workers to compute-capable nodes.
9. **Monitoring setup** -- Configure Prometheus exporters, Grafana dashboards, alerting rules.
10. **Health verification** -- Run health checks against all deployed components, report status.

## Prerequisites

Before deploying, ensure the following are available:

- **Ansible** 2.14+ on the control machine (laptop-01)
- **SSH access** to all fleet nodes with key-based authentication
- **PostgreSQL client** (`psql`) for database setup and migration
- **Node.js** 18+ on all nodes running JavaScript components
- **Python** 3.10+ on all nodes running Python components
- **Docker** on nodes running OrientDB (containerized deployment)

## Controller Node Setup

The controller runs on **aio-01** and serves the REST API that coordinates all fleet operations.

### Deploy the Flask API

```bash
# Copy application code to aio-01
scp -r ./orchestrator/ aio-01:/opt/claude-orchestrator/

# SSH to aio-01 and install dependencies
ssh aio-01 "cd /opt/claude-orchestrator && pip3 install -r requirements.txt"
```

### Configure systemd Service

Create the service file on aio-01:

```ini
# /etc/systemd/system/claude-orchestrator.service
[Unit]
Description=Claude Orchestration API
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=claude
WorkingDirectory=/opt/claude-orchestrator
ExecStart=/usr/bin/python3 -m flask run --host=0.0.0.0 --port=5000
Restart=always
RestartSec=5
Environment=FLASK_APP=app.py
Environment=FLASK_ENV=production
Environment=DATABASE_URL=postgresql://claude:password@localhost:5433/learning

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start the service
ssh aio-01 "sudo systemctl daemon-reload && sudo systemctl enable claude-orchestrator && sudo systemctl start claude-orchestrator"
```

### Verify Controller Health

```bash
curl http://aio-01:5000/health
```

Expected response:

```json
{
  "status": "healthy",
  "uptime_seconds": 42,
  "version": "1.0.0",
  "database": "connected",
  "workers_registered": 0
}
```

## Fleet Worker Deployment

Each worker node runs a lightweight agent that accepts tasks from the controller and returns results.

### Deploy a Worker

```bash
# 1. Ensure SSH key access
ssh-copy-id user@worker-node

# 2. Copy worker scripts
scp -r ./worker/ worker-node:/opt/claude-worker/

# 3. Install dependencies
ssh worker-node "cd /opt/claude-worker && npm install"

# 4. Register the worker with the controller
curl -X POST http://aio-01:5000/fleet/register \
  -H "Content-Type: application/json" \
  -d '{
    "node_id": "worker-node",
    "hostname": "worker-node",
    "capabilities": ["llm-routing", "code-review", "scraping"],
    "max_concurrent": 3
  }'

# 5. Configure circuit breaker via API
curl -X POST http://aio-01:5000/fleet/circuit-breaker \
  -H "Content-Type: application/json" \
  -d '{
    "node_id": "worker-node",
    "failure_threshold": 5,
    "reset_timeout_ms": 30000,
    "half_open_max": 2
  }'
```

### Verify Worker Registration

```bash
curl http://aio-01:5000/fleet/status | python3 -m json.tool
```

Expected response includes the new worker:

```json
{
  "workers": [
    {
      "node_id": "worker-node",
      "status": "idle",
      "circuit_state": "closed",
      "last_heartbeat": "2026-08-05T12:00:00Z",
      "tasks_completed": 0,
      "capabilities": ["llm-routing", "code-review", "scraping"]
    }
  ]
}
```

## Scraper Deployment

Scrapers collect data from external sources and submit it to the store via the API.

```bash
# Copy scraper scripts to the scraper node
scp -r ./tools/scrapers/ scraper-node:/opt/claude-scrapers/

# Install dependencies
ssh scraper-node "cd /opt/claude-scrapers && pip3 install -r requirements.txt"

# Start a scraper (runs in background)
ssh scraper-node "cd /opt/claude-scrapers && nohup python3 scraper_runner.py \
  --config config/scraper-config.json \
  --api-endpoint http://aio-01:5000 \
  > /var/log/claude-scraper.log 2>&1 &"
```

Scrapers post their collected data to the controller API:

```bash
# Scraper submits data via /store endpoint
curl -X POST http://aio-01:5000/store \
  -H "Content-Type: application/json" \
  -d '{
    "source": "github-trending",
    "content": "...",
    "metadata": {"scraped_at": "2026-08-05T12:00:00Z"}
  }'
```

The `nohup` pattern is used for scrapers because they are long-running background processes that should survive SSH disconnection.

## Store Worker Deployment

Store workers handle data ingestion, indexing, and persistence. They must use persistent storage paths.

**CRITICAL: Never use /tmp for store worker data.** The `/tmp` directory is a tmpfs (RAM-backed) filesystem that runs out of inodes and loses data on reboot. Always use paths under `/home` or `/opt`.

```bash
# Create persistent directories
ssh store-node "mkdir -p /opt/claude-store/{data,index,logs}"

# Deploy store worker
scp -r ./store-worker/ store-node:/opt/claude-store/worker/

# Configure persistent paths
ssh store-node "cat > /opt/claude-store/worker/config.json << 'EOF'
{
  \"data_dir\": \"/opt/claude-store/data\",
  \"index_dir\": \"/opt/claude-store/index\",
  \"log_dir\": \"/opt/claude-store/logs\",
  \"api_endpoint\": \"http://aio-01:5000\",
  \"batch_size\": 100,
  \"flush_interval_ms\": 5000
}
EOF"

# Start store worker
ssh store-node "cd /opt/claude-store/worker && nohup node store-worker.js \
  --config /opt/claude-store/worker/config.json \
  > /opt/claude-store/logs/store-worker.log 2>&1 &"
```

## Embedding Worker Deployment

Embedding workers generate vector embeddings for documents and chunks. They require significant CPU or GPU resources and should only be deployed on dedicated compute nodes.

```bash
# Deploy to a compute-capable node
scp -r ./embedding-worker/ compute-node:/opt/claude-embeddings/

# Install sentence-transformers (required for embeddings)
ssh compute-node "cd /opt/claude-embeddings && pip3 install sentence-transformers torch"

# Start embedding worker
ssh compute-node "cd /opt/claude-embeddings && nohup python3 embedding_worker.py \
  --model all-MiniLM-L6-v2 \
  --batch-size 64 \
  --api-endpoint http://aio-01:5000 \
  > /opt/claude-embeddings/logs/embedding-worker.log 2>&1 &"
```

The embedding worker pulls unembedded chunks from the API, generates embeddings, and posts them back:

```
Embedding Worker Loop:
  1. GET /embeddings/pending?limit=64     --> fetch unembedded chunks
  2. Generate embeddings locally           --> sentence-transformers model
  3. POST /embeddings/store               --> submit vectors to API
  4. Sleep 1s if no pending chunks        --> backpressure
```

## Database Setup

### PostgreSQL with pgvector

PostgreSQL is the primary data store. It runs on aio-01 (port 5433) with the pgvector extension for vector similarity search.

```bash
# Install pgvector extension (on aio-01)
ssh aio-01 "sudo apt-get install postgresql-16-pgvector"

# Connect and enable extension
psql -h aio-01 -p 5433 -U claude -d learning -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

#### Schema Migrations

Apply all schema migrations in order:

```bash
# Run migrations
psql -h aio-01 -p 5433 -U claude -d learning -f migrations/001_learning_schema.sql
psql -h aio-01 -p 5433 -U claude -d learning -f migrations/002_monitoring_schema.sql
psql -h aio-01 -p 5433 -U claude -d learning -f migrations/003_costs_schema.sql
psql -h aio-01 -p 5433 -U claude -d learning -f migrations/004_knowledge_schema.sql
psql -h aio-01 -p 5433 -U claude -d learning -f migrations/005_workflow_schema.sql
```

Key schemas and their purposes:

| Schema | Purpose | Key Tables |
|--------|---------|------------|
| `learning` | Experience storage, strategy performance | `experiences`, `strategy_performance` |
| `monitoring` | Execution logs, diversity alerts | `execution_summary`, `diversity_alerts` |
| `costs` | API cost tracking | `entries` |
| `knowledge` | Document and chunk storage | `documents`, `chunks` |
| `workflow` | Multi-AI workflow tracking | `executions`, `worker_results`, `arbiter_decisions` |

### Redis

Redis is used for caching, queue management, and real-time counters.

```bash
# Install Redis on the controller node
ssh aio-01 "sudo apt-get install redis-server"

# Configure Redis for persistence
ssh aio-01 "sudo sed -i 's/^# save 3600 1/save 3600 1/' /etc/redis/redis.conf"
ssh aio-01 "sudo systemctl restart redis-server"

# Verify Redis
ssh aio-01 "redis-cli ping"
# Expected: PONG
```

### OrientDB

OrientDB stores the knowledge graph (relationships between documents, concepts, and entities). It runs as a Docker container.

```bash
# Start OrientDB container on aio-01
ssh aio-01 "docker run -d \
  --name orientdb \
  --restart unless-stopped \
  -p 2424:2424 \
  -p 2480:2480 \
  -v /opt/orientdb/databases:/orientdb/databases \
  -v /opt/orientdb/backup:/orientdb/backup \
  -e ORIENTDB_ROOT_PASSWORD=rootpass \
  orientdb:3.2"

# Verify OrientDB
curl http://aio-01:2480/listDatabases
```

## Health Verification

After deployment, verify all components are healthy:

```bash
# 1. Controller API
curl -s http://aio-01:5000/health | python3 -m json.tool

# 2. Fleet workers
curl -s http://aio-01:5000/fleet/status | python3 -m json.tool

# 3. Database connectivity
curl -s http://aio-01:5000/health/database | python3 -m json.tool

# 4. Scraper status
curl -s http://aio-01:5000/fleet/scrapers/status | python3 -m json.tool
```

All four checks should return `"status": "healthy"`. If any component reports unhealthy:

1. Check the component's log files for error messages.
2. Verify network connectivity between nodes (`ping`, `ssh`).
3. Confirm the controller API is reachable from the failing node.
4. Check circuit breaker state -- a tripped breaker will show `"circuit_state": "open"`.

```bash
# Reset a tripped circuit breaker
curl -X POST http://aio-01:5000/fleet/circuit-breaker/reset \
  -H "Content-Type: application/json" \
  -d '{"node_id": "worker-node"}'
```
