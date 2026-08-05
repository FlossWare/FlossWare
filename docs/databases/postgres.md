---
title: PostgreSQL Architecture
---

# PostgreSQL Architecture

PostgreSQL 16 with the pgvector extension serves as the primary persistent data store for the orchestration framework. It runs on the controller node (aio-01) on port 5433, with all data consolidated in the `learning` database.

## Connection Details

| Parameter | Value |
|-----------|-------|
| Host | aio-01 |
| Port | 5433 |
| Database | learning |
| Version | PostgreSQL 16 |
| Extensions | pgvector, pg_trgm, pg_stat_statements |

Workers never connect to PostgreSQL directly. All access is mediated through the REST API gateway on aio-01:5000.

---

## Schema Overview

The database contains 226 tables organized across 25 schemas. Each schema owns a distinct functional domain:

```
learning (database)
|
+-- knowledge.*          (20 tables)  -- Extracted knowledge, concepts, relationships
+-- learning.*           (85 tables)  -- Experience memory, strategy performance, bandit state
+-- monitoring.*         (30 tables)  -- Execution logs, diversity alerts, health checks
+-- workflow.*           (17 tables)  -- Multi-AI workflow persistence
+-- costs.*              (12 tables)  -- Token usage, cost tracking, budget enforcement
+-- embeddings.*         (10 tables)  -- Vector storage, HNSW indexes, similarity search
+-- secrets.*            ( 5 tables)  -- Encrypted API keys, rotation tracking
+-- scraping.*           ( 8 tables)  -- URL queue, content cache, extraction logs
+-- graph_sync.*         ( 6 tables)  -- OrientDB synchronization state
+-- routing.*            ( 7 tables)  -- Thompson Sampling state, model rankings
+-- feedback.*           ( 4 tables)  -- User feedback, quality signals
+-- config.*             ( 3 tables)  -- Runtime configuration, feature flags
+-- audit.*              ( 5 tables)  -- Change tracking, decision logs
+-- models.*             ( 6 tables)  -- Model registry, capability tags, provider mapping
+-- providers.*          ( 4 tables)  -- API provider health, rate limit state
+-- public               ( 4 tables)  -- Shared lookup tables, enums
+-- (10 additional)      (remaining)  -- Specialized operational schemas
```

---

## Key Schemas

### knowledge.* (20 tables)

The knowledge schema stores extracted facts, concepts, and their relationships. Documents ingested by the scraping pipeline land here after chunking and embedding.

| Table | Purpose | Row Estimate |
|-------|---------|-------------|
| `knowledge.documents` | Source documents with metadata | 315K+ |
| `knowledge.chunks` | Text chunks with position tracking | 1.2M+ |
| `knowledge.concepts` | Extracted named concepts | 48K+ |
| `knowledge.concept_embeddings` | 768-dim vectors for concepts | 48K+ |
| `knowledge.relationships` | Concept-to-concept links | 190K+ |
| `knowledge.taxonomies` | Hierarchical concept trees | 2K+ |
| `knowledge.sources` | Origin URLs and provenance | 12K+ |
| `knowledge.extraction_runs` | Batch extraction job logs | 8K+ |
| `knowledge.quality_scores` | Per-chunk quality assessments | 800K+ |
| `knowledge.dedup_hashes` | SHA-256 deduplication index | 1.2M+ |
| `knowledge.entity_mentions` | Entity occurrence tracking | 3.5M+ |
| `knowledge.summaries` | Multi-level document summaries | 45K+ |
| `knowledge.keywords` | TF-IDF keyword extraction | 280K+ |
| `knowledge.clusters` | Topic clusters (k-means) | 1.5K+ |
| `knowledge.cluster_members` | Chunk-to-cluster assignments | 900K+ |
| `knowledge.search_log` | Search query history | 25K+ |
| `knowledge.feedback` | Relevance feedback on results | 6K+ |
| `knowledge.stale_markers` | Content freshness tracking | 40K+ |
| `knowledge.merge_log` | Concept merge/split audit | 3K+ |
| `knowledge.export_batches` | Bulk export tracking | 500+ |

### learning.* (85+ tables)

The largest schema. Stores experience memory, Thompson Sampling bandit state, strategy performance metrics, and the continual learning feedback loop. Key tables include:

- `learning.experiences` -- Experience memory with 128-dim embedding vectors
- `learning.strategy_performance` -- Thompson Sampling alpha/beta parameters per strategy
- `learning.model_outcomes` -- Per-model success/failure tracking
- `learning.task_classifications` -- Task type taxonomy and routing rules
- `learning.reward_signals` -- Delayed reward propagation records

### monitoring.* (30+ tables)

Execution telemetry and operational health:

- `monitoring.execution_summary` -- Model execution logs (latency, tokens, status)
- `monitoring.diversity_alerts` -- Feedback loop optimizer alerts
- `monitoring.health_checks` -- Fleet node health snapshots
- `monitoring.error_log` -- Categorized error tracking
- `monitoring.rate_limit_events` -- Provider rate limit encounters

### workflow.* (17 tables)

Multi-AI workflow persistence with task embeddings:

- `workflow.executions` -- Main workflow records with 384-dim task embeddings
- `workflow.worker_results` -- Individual worker outputs
- `workflow.arbiter_decisions` -- Arbiter synthesis and final verdicts
- `workflow.phases` -- Phase-level tracking (design, implement, review)
- `workflow.feedback` -- Quality feedback per workflow
- `workflow.learnings` -- Extracted learnings from completed workflows

---

## Vector Dimensions

pgvector stores embeddings at three dimensionalities, each tuned for a different use case. HNSW indexes accelerate approximate nearest-neighbor search.

| Dimension | Use Case | HNSW m | HNSW ef_construction | Index Type | Tables |
|-----------|----------|--------|----------------------|------------|--------|
| 128-dim | Experience similarity | 16 | 64 | hnsw (L2) | `learning.experiences` |
| 384-dim | Task/workflow matching | 16 | 128 | hnsw (cosine) | `workflow.executions`, `routing.task_vectors` |
| 768-dim | Document/concept search | 32 | 200 | hnsw (cosine) | `knowledge.concept_embeddings`, `knowledge.chunks` |

Embedding generation uses `sentence-transformers` (all-MiniLM-L6-v2 for 384-dim, all-mpnet-base-v2 for 768-dim). The 128-dim vectors are PCA-reduced from 384-dim for compact experience storage.

---

## Performance Benchmarks vs ChromaDB

Direct comparison on the same hardware (aio-01, 32 GB RAM, NVMe SSD):

| Operation | ChromaDB | PostgreSQL + pgvector | Speedup |
|-----------|----------|----------------------|---------|
| Simple similarity search (128-dim, top-10) | 0.9 ms | 0.44 ms | 2.0x |
| Filtered similarity (metadata + vector) | 2.3 ms | 0.44 ms | 5.2x |
| High-dimensional search (768-dim, top-10) | 1.2 ms | 0.97 ms | 1.2x |
| Batch insert (1000 vectors, 384-dim) | 45 ms | 32 ms | 1.4x |
| Concurrent queries (10 parallel) | 12 ms p99 | 6.8 ms p99 | 1.8x |
| Cold start time | 2.1 s | 0 s (always running) | -- |

PostgreSQL is consistently faster, and the advantage grows with filtered queries because pgvector leverages standard B-tree indexes alongside HNSW for combined predicate + vector search.

---

## Why PostgreSQL Over Dedicated Vector Databases

| Capability | PostgreSQL + pgvector | ChromaDB | Pinecone | Weaviate |
|------------|----------------------|----------|----------|----------|
| Vector similarity search | Yes (HNSW, IVFFlat) | Yes | Yes | Yes |
| SQL joins across tables | Yes | No | No | No |
| ACID transactions | Yes | No | No | Partial |
| Materialized views | Yes | No | No | No |
| Triggers and functions | Yes | No | No | No |
| Row-level security | Yes | No | Yes | No |
| Backup with pg_dump | Yes | Manual | Managed | Manual |
| Combined vector + relational queries | Yes | No | No | Limited |
| Existing operational expertise | Yes | New stack | New stack | New stack |
| Cost | Free | Free | Paid | Free/Paid |

The decisive factor is **combined vector + relational queries**. A typical workflow query joins `workflow.executions` (vector similarity on task embedding) with `workflow.worker_results` (relational filter on model and outcome) and `monitoring.execution_summary` (latency percentiles). Doing this across separate systems would require application-level joins with significant latency and complexity.

---

## REST API Gateway Access Pattern

Workers never connect to PostgreSQL directly. All database access flows through the REST API on aio-01:5000, which owns the connection pool and enforces access control.

```
+-------------+     +-------------+     +-------------+
|  worker-01  |     |  worker-02  |     |  worker-03  |
|  (server-01)|     |  (server-02)|     |  (server-03)|
+------+------+     +------+------+     +------+------+
       |                   |                   |
       +-------------------+-------------------+
                           |
                    HTTP POST/GET
                           |
                    +------v------+
                    |  REST API   |
                    | aio-01:5000 |
                    +------+------+
                           |
                   Connection Pool
                     (max: 20)
                           |
                    +------v------+
                    | PostgreSQL  |
                    | aio-01:5433 |
                    |  db:learning|
                    +-------------+
```

**Why this pattern:**

1. **Connection pooling** -- PostgreSQL has a hard limit on concurrent connections. The API maintains a pool of 20 connections shared across all workers.
2. **Access control** -- The API enforces schema-level permissions. Workers cannot run arbitrary SQL.
3. **Query optimization** -- The API layer applies caching, query batching, and prepared statements.
4. **Network simplicity** -- Workers only need HTTP access to one endpoint, not database credentials.

### Key API Endpoints for Database Operations

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/learning/experiences` | GET/POST | Read/write experience memory |
| `/learning/strategy` | GET/POST | Thompson Sampling state |
| `/store` | POST | Ingest documents into knowledge schema |
| `/search/intelligent` | POST | Adaptive multi-source search (PostgreSQL, vector, graph) |
| `/monitoring/log` | POST | Write execution telemetry |
| `/workflow/store` | POST | Persist workflow results |
| `/secrets` | GET | Retrieve API keys (encrypted at rest) |

---

## Schema Migrations

Database schema changes are managed through 22 numbered migration files applied sequentially. Each migration is idempotent (safe to re-run).

```
migrations/
  001_initial_schema.sql
  002_add_pgvector.sql
  003_learning_experiences.sql
  004_monitoring_tables.sql
  005_workflow_schema.sql
  006_costs_tracking.sql
  007_knowledge_base.sql
  008_hnsw_indexes.sql
  009_materialized_views.sql
  010_scraping_pipeline.sql
  011_graph_sync.sql
  012_routing_thompson.sql
  013_add_384dim_vectors.sql
  014_feedback_tables.sql
  015_audit_logging.sql
  016_model_registry.sql
  017_provider_health.sql
  018_secrets_encryption.sql
  019_dedup_indexes.sql
  020_concept_clustering.sql
  021_export_batches.sql
  022_config_feature_flags.sql
```

Migrations are applied at API startup. The current version is tracked in `public.schema_version`.

---

## Materialized Views

Three materialized views provide pre-aggregated data for dashboards and decision-making. They refresh automatically every 5 minutes via `pg_cron`.

| View | Source Tables | Purpose | Refresh Interval |
|------|--------------|---------|-----------------|
| `workflow.workflow_summary` | `workflow.executions`, `workflow.worker_results` | Aggregated workflow stats (success rate, avg duration, model distribution) | 5 min |
| `workflow.model_performance` | `monitoring.execution_summary`, `learning.model_outcomes` | Per-model metrics (latency p50/p95/p99, success rate, token efficiency) | 5 min |
| `workflow.cost_analysis` | `costs.entries`, `workflow.executions` | Cost breakdowns by provider, model, and workflow type | 5 min |

Manual refresh:

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY workflow.workflow_summary;
REFRESH MATERIALIZED VIEW CONCURRENTLY workflow.model_performance;
REFRESH MATERIALIZED VIEW CONCURRENTLY workflow.cost_analysis;
```

The `CONCURRENTLY` keyword allows queries to continue reading the old data while the refresh runs.

---

## Backup and Recovery

### Automated Backups

| Parameter | Value |
|-----------|-------|
| Schedule | Daily at 2:00 AM |
| Method | `pg_dump` (custom format) |
| Destination | `server-ap:/exports/backups/laptop-01-learning/` |
| Retention | 30 days |
| Script | `~/bin/backup-learning-db.sh` |
| Compression | gzip (level 6) |
| Average dump size | ~1.2 GB compressed |

### Recovery Procedure

```bash
# List available backups
ls -lt /exports/backups/laptop-01-learning/

# Restore from a specific backup
pg_restore -h aio-01 -p 5433 -d learning \
  --clean --if-exists --no-owner \
  /exports/backups/laptop-01-learning/learning_2026-08-04.dump
```

### Point-in-Time Recovery

WAL archiving is enabled for point-in-time recovery. WAL segments are shipped to the same backup destination. Recovery to any point within the 30-day retention window is supported.

### Monitoring Backup Health

The backup script reports success/failure to `monitoring.backup_log`. A Grafana alert fires if no successful backup is recorded within 26 hours (allowing for 2-hour variance).
