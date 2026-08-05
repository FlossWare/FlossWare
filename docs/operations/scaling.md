---
title: Scaling Guide
---

# Scaling Guide

This guide covers horizontal scaling (adding more components), vertical scaling (making existing components more powerful), bottleneck identification, and capacity planning.

## Horizontal Scaling

### Adding Scrapers

Scrapers are the simplest component to scale. Each scraper is an independent process that collects data and submits it to the controller API. Adding a new scraper requires no changes to the existing system.

```bash
# Deploy a new scraper to any node with network access
scp scraper_new_source.py scraper-node:/opt/claude-scrapers/
ssh scraper-node "cd /opt/claude-scrapers && nohup python3 scraper_new_source.py \
  --api-endpoint http://aio-01:5000 \
  > /opt/claude-scrapers/logs/new-source.log 2>&1 &"
```

Scrapers are stateless -- they do not coordinate with each other. The controller deduplicates incoming data, so multiple scrapers targeting the same source are safe (though wasteful).

### Adding Models

New AI models are discovered and integrated automatically through Thompson Sampling.

When a new free model becomes available on a provider (e.g., a new model on OpenRouter), the system discovers it through the daily model discovery process. The model is registered as a new Thompson Sampling arm with a uniform prior `Beta(alpha=1, beta=1)` and is automatically explored alongside existing models.

No manual configuration is required unless the model needs special handling (e.g., unusual token limits, non-standard API format).

```bash
# Manual model registration (if auto-discovery misses it)
curl -X POST http://aio-01:5000/models/register \
  -H "Content-Type: application/json" \
  -d '{
    "model_id": "new-model-name",
    "provider": "openrouter",
    "max_tokens": 8192,
    "supports_tools": true,
    "cost_per_1k_input": 0.0,
    "cost_per_1k_output": 0.0
  }'
```

### Adding Fleet Nodes

Adding a new worker node to the fleet takes three steps:

```bash
# Step 1: Ensure SSH key access from controller
ssh-copy-id user@new-worker

# Step 2: Deploy worker software
scp -r ./worker/ new-worker:/opt/claude-worker/
ssh new-worker "cd /opt/claude-worker && npm install"

# Step 3: Register with the controller
curl -X POST http://aio-01:5000/fleet/register \
  -H "Content-Type: application/json" \
  -d '{
    "node_id": "new-worker",
    "hostname": "new-worker.local",
    "capabilities": ["llm-routing", "code-review"],
    "max_concurrent": 3
  }'
```

After registration, the controller immediately begins routing tasks to the new worker. The circuit breaker starts in the `closed` state, meaning the worker receives full traffic.

### Adding GA Optimizers

The genetic algorithm framework supports multiple independent optimizers. Each optimizer evolves configurations for a different aspect of the system.

Currently deployed optimizers:

| Optimizer | What It Evolves | Genes | Fitness Function |
|-----------|----------------|-------|-----------------|
| Model Routing | Task-to-model mapping | 6 | quality*0.6 - cost*0.2 - latency*0.2 |
| RAG Retrieval | Chunk/embed/retrieve params | 5 | Retrieval precision + recall |
| Team Composition | Multi-AI consensus teams | 4 | quality*0.5 + diversity*0.3 - cost*0.2 |
| Workflow Configuration | End-to-end workflow params | 6 | success*0.4 + quality*0.3 - duration*0.2 - cost*0.1 |

To add a new optimizer:

1. Define the chromosome (genes, types, ranges).
2. Implement the fitness function using data from PostgreSQL.
3. Register the optimizer with the GA engine via the API.
4. Elite chromosomes from the new optimizer are automatically registered as Thompson Sampling arms.

### Adding Review Layers

Additional adversarial review layers can be stacked to increase verification rigor:

```
Output -> Layer 1 (fast consensus) -> Layer 2 (adversarial) -> Layer 3 (domain expert)
```

Each layer is an independent evaluator with its own set of reviewer models, agreement threshold, and pass/fail criteria. Adding a layer increases latency but decreases the probability of undetected errors.

## Vertical Scaling

### PostgreSQL

PostgreSQL is the system's primary bottleneck for write-heavy workloads (execution logging, embedding storage).

| Concern | Current | Scaling Strategy |
|---------|---------|-----------------|
| Write throughput | Single writer on aio-01 | Batch inserts (100 rows per transaction), reduce fsync frequency for non-critical tables |
| Read throughput | Single reader on aio-01 | Add read replicas for monitoring queries; keep writes on primary |
| Storage | ~50 GB on local SSD | Move to NAS for cold data (>90 days), keep hot data on SSD |
| Vector search | pgvector HNSW index | Increase `ef_construction` for better recall, partition by date for smaller index segments |

```sql
-- Check current database size
SELECT pg_size_pretty(pg_database_size('learning'));

-- Check index sizes (vector indexes are often the largest)
SELECT
    indexrelname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 10;
```

### Redis

Redis is used for queues, caching, and counters. It is rarely the bottleneck but can become one with very high queue depths.

| Concern | Current | Scaling Strategy |
|---------|---------|-----------------|
| Memory usage | ~200 MB | Monitor with `redis-cli info memory`; evict stale cache entries |
| Queue depth | <1000 typical | If embedding queue >5000, add embedding workers rather than scaling Redis |
| Persistence | RDB snapshots every 3600s | Enable AOF for durability if queue data is critical |

```bash
# Check Redis memory usage
ssh aio-01 "redis-cli info memory | grep used_memory_human"

# Check queue depths
ssh aio-01 "redis-cli llen queue:embedding"
ssh aio-01 "redis-cli llen queue:task_routing"
```

### Embedding Throughput

Embedding generation is CPU-bound (no GPU in the fleet). Throughput scales with:

1. **Batch size** -- Larger batches amortize model loading overhead. Current default: 64 chunks per batch.
2. **More embedding workers** -- Each worker is independent. Deploy to any compute-capable node.
3. **Smaller embedding model** -- `all-MiniLM-L6-v2` (384-dim) is faster than `all-mpnet-base-v2` (768-dim) with acceptable quality loss.
4. **Dimensionality reduction** -- Store 128-dim embeddings instead of 384-dim. Requires reindexing.

## Pipeline Bottleneck Identification

Data flows through a four-stage pipeline. Each stage has different characteristics and bottlenecks:

```
+----------+     +-----------+     +-----------+     +----------+
| Scraping | --> | Storing   | --> | Embedding | --> | Indexing  |
| (ingest) |     | (persist) |     | (vectorize|     | (search)  |
+----------+     +-----------+     +-----------+     +----------+
     |                |                 |                  |
   Network          Disk I/O           CPU              PostgreSQL
   bound             bound            bound              bound
```

### Stage Characteristics

| Stage | Bound By | Scaling Strategy | Bottleneck Indicator |
|-------|----------|-----------------|---------------------|
| Scraping | Network I/O, rate limits | Add more scrapers, respect rate limits, rotate IP/user-agent | `scraping` queue always empty (scrapers are idle) |
| Storing | Disk I/O, PostgreSQL write throughput | Batch inserts, larger transactions, SSD storage | `store` queue depth growing steadily |
| Embedding | CPU (no GPU) | Add embedding workers, use smaller model, increase batch size | `embedding` queue depth >1000 and growing |
| Indexing | PostgreSQL vector index maintenance | Partition tables by date, rebuild HNSW indexes during low traffic | Search latency increasing over time |

### Diagnosing Bottlenecks

```bash
# Check queue depths to find the bottleneck stage
curl -s http://aio-01:5000/monitoring/queues | python3 -m json.tool
```

**Interpretation:**

1. **All queues near zero** -- System is keeping up. No bottleneck.
2. **`embedding` queue growing, others stable** -- Embedding workers cannot keep up with the ingestion rate. Add more embedding workers or reduce ingestion rate.
3. **`store` queue growing, `embedding` queue zero** -- Store workers are the bottleneck. Check PostgreSQL write performance and disk I/O.
4. **`scraping` queue empty, `store` and `embedding` queues empty** -- Scrapers are the bottleneck (or there is nothing to scrape). Add more scrapers or new data sources.

## Current Scale

| Metric | Value | Notes |
|--------|-------|-------|
| Documents indexed | 381,000+ | Across all knowledge domains |
| Chunks stored | 3,900,000+ | Average ~10 chunks per document |
| Embeddings generated | 487,000+ | 384-dimensional vectors (all-MiniLM-L6-v2) |
| URLs tracked | 119,000+ | Deduplicated source URLs |
| Fleet nodes | 9 | 1 controller (aio-01) + 7 workers + 1 dev (laptop-01) |
| AI models available | 200+ | Free tier across OpenRouter, Anthropic, Google, Groq, Cerebras, DeepSeek |
| Scrapers deployed | 116+ | Independent scraper processes |
| PostgreSQL tables | 30+ | Across 5 schemas (learning, monitoring, costs, knowledge, workflow) |
| Daily API requests | Varies | Depends on workload; cost tracked in `costs.entries` |

## Capacity Planning Considerations

### Worker Memory

Each worker node needs sufficient memory for:

- Node.js runtime: ~100 MB base
- Request/response buffers: ~50-200 MB depending on task complexity
- Concurrent tasks: multiply by `max_concurrent` setting (default 3)

**Rule of thumb:** Allocate 500 MB per concurrent task slot. A node with `max_concurrent: 3` needs at least 1.5 GB free memory.

### Parallel Workers

The controller limits parallel workers per request to avoid overwhelming the fleet. The current maximum is **3 parallel workers** per multi-AI consensus task.

Increasing this requires:
- More fleet nodes (to avoid contention)
- Higher `max_concurrent` per node (if nodes have spare capacity)
- Larger Redis queue buffers (to handle burst traffic)

### Database Connections

PostgreSQL has a maximum connection limit (default: 100). Each component that connects to PostgreSQL uses connections:

| Component | Connections Used |
|-----------|-----------------|
| Controller API | 10-20 (connection pool) |
| Each worker | 1-2 (when querying execution history) |
| Embedding workers | 1 per worker |
| Store workers | 2-3 per worker |
| Monitoring queries | 1-2 |
| Backup process | 1 |

With 9 fleet nodes plus workers, the system uses ~40-50 connections. Monitor with:

```sql
SELECT count(*) FROM pg_stat_activity WHERE datname = 'learning';
```

If approaching the limit, increase `max_connections` in `postgresql.conf` or use PgBouncer for connection pooling.

### NFS Dependency

Some fleet nodes share data via NFS mounts. NFS introduces:

- **Latency** -- File operations are slower than local disk (5-20ms vs <1ms).
- **Single point of failure** -- If the NFS server goes down, all dependent nodes are affected.
- **Locking** -- NFS file locks can cause contention under heavy concurrent writes.

**Mitigation:** The system falls back to local storage if NFS is unavailable. Critical data (PostgreSQL, Redis) does not depend on NFS. Only shared configuration and scraper scripts use NFS paths.

When scaling, prefer local storage for data-intensive operations and reserve NFS for shared configuration and read-heavy reference data.
