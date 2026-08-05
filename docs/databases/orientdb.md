---
title: OrientDB Graph Database
---

# OrientDB Graph Database

OrientDB 3.2 provides the graph layer for the orchestration framework, storing relationships between infrastructure nodes, AI models, workflows, documents, and concepts. It runs in a Docker container on the controller node (aio-01).

## Connection Details

| Parameter | Value |
|-----------|-------|
| Host | aio-01 |
| Port | 2424 (binary), 2480 (HTTP) |
| Version | OrientDB 3.2 (Community) |
| Runtime | Docker container |
| Database | orchestrator |
| Storage engine | plocal (disk-backed) |

All access from workers goes through the REST API gateway (aio-01:5000). Workers never connect to OrientDB directly.

---

## Data Model

### Vertex Classes

OrientDB organizes data into vertex classes (nodes) and edge classes (relationships). Five vertex classes form the core of the graph.

| Vertex Class | Count | Description | Key Properties |
|-------------|-------|-------------|---------------|
| `Infrastructure` | 9 | Fleet nodes (controller + workers) | hostname, ip, role, status, cpu_cores, ram_gb |
| `Workflow` | 170+ | Workflow execution records | workflow_id, name, outcome, duration_ms, created_at |
| `Model` | 200+ | AI models from all providers | model_id, provider, context_window, cost_tier, capabilities |
| `Document` | 315K+ | Ingested documents and content | doc_id, url, title, source, chunk_count, ingested_at |
| `Concept` | 48K+ | Extracted concepts and entities | concept_id, name, type, frequency, first_seen |

### Edge Types

Seven edge types capture the relationships between vertices.

| Edge Type | From | To | Count | Description |
|-----------|------|----|-------|-------------|
| `EXECUTED_ON` | Workflow | Infrastructure | 500+ | Which node ran the workflow |
| `USED_MODEL` | Workflow | Model | 1.2K+ | Which models participated |
| `MENTIONS` | Document | Concept | 3.5M+ | Concept occurrence in a document |
| `RELATED_TO` | Concept | Concept | 190K+ | Semantic relationship between concepts |
| `HOSTED_BY` | Model | Infrastructure | 200+ | Which provider/node hosts a model |
| `DERIVED_FROM` | Document | Document | 45K+ | Document lineage and citations |
| `SIMILAR_TO` | Concept | Concept | 85K+ | Embedding-based similarity (cosine > 0.85) |

---

## Schema Diagram

```
                    +----------------+
                    | Infrastructure |
                    |    (9 nodes)   |
                    +-------+--------+
                       ^         ^
                       |         |
               EXECUTED_ON   HOSTED_BY
                       |         |
              +--------+--+  +--+--------+
              |  Workflow  |  |   Model   |
              |   (170+)   |  |   (200+)  |
              +-----+------+  +-----------+
                    |
                USED_MODEL
                    |
                    v
              +-----------+
              |   Model   |
              |   (200+)  |
              +-----------+

              +------------+         +------------+
              |  Document  |-------->|  Document  |
              |  (315K+)   | DERIVED |  (315K+)   |
              +-----+------+ _FROM   +------------+
                    |
                 MENTIONS
                    |
                    v
              +------------+         +------------+
              |  Concept   |-------->|  Concept   |
              |   (48K+)   | RELATED |   (48K+)   |
              +------------+ _TO /   +------------+
                             SIMILAR
                             _TO
```

---

## Query Patterns

OrientDB excels at traversal queries that would require expensive recursive CTEs or self-joins in SQL. The following patterns are used throughout the system.

### Multi-Hop Traversal

Find all concepts within 3 hops of a starting concept, revealing the knowledge neighborhood.

**OrientDB:**

```sql
SELECT expand(out('RELATED_TO').out('RELATED_TO').out('RELATED_TO'))
FROM Concept
WHERE name = 'firmware'
```

**Equivalent SQL (PostgreSQL recursive CTE):**

```sql
WITH RECURSIVE hops AS (
    -- Base case: direct relationships
    SELECT r.target_id AS concept_id, 1 AS depth
    FROM knowledge.relationships r
    JOIN knowledge.concepts c ON c.id = r.source_id
    WHERE c.name = 'firmware'

    UNION ALL

    -- Recursive case: follow edges
    SELECT r.target_id, h.depth + 1
    FROM knowledge.relationships r
    JOIN hops h ON h.concept_id = r.source_id
    WHERE h.depth < 3
)
SELECT DISTINCT c.name, h.depth
FROM hops h
JOIN knowledge.concepts c ON c.id = h.concept_id
ORDER BY h.depth;
```

The OrientDB query is 2 lines. The SQL equivalent is 15 lines and requires careful depth management to avoid infinite loops on cyclic graphs.

### Shortest Path

Find the shortest conceptual path between two concepts.

```sql
SELECT shortestPath(
    (SELECT FROM Concept WHERE name = 'UEFI'),
    (SELECT FROM Concept WHERE name = 'secure boot'),
    'BOTH', 'RELATED_TO'
)
```

This returns the vertex chain: `UEFI -> boot_loader -> trusted_platform -> secure_boot`. SQL has no native shortest-path operator.

### Neighborhood Discovery

Find all models that participated in workflows executed on a specific infrastructure node.

```sql
SELECT expand(in('EXECUTED_ON').out('USED_MODEL'))
FROM Infrastructure
WHERE hostname = 'server-01'
```

This traverses two edge types in a single query: from infrastructure back through workflows, then forward through model usage.

### Concept Co-Occurrence

Find concepts that frequently appear in the same documents as a target concept, ranked by co-occurrence count.

```sql
SELECT in('MENTIONS').out('MENTIONS').name AS co_concept,
       count(*) AS frequency
FROM Concept
WHERE name = 'PXE'
GROUP BY co_concept
ORDER BY frequency DESC
LIMIT 20
```

This query starts at the "PXE" concept, traverses backward through MENTIONS edges to find documents, then forward through MENTIONS edges to find sibling concepts.

---

## REST API Access

Workers access the graph through two REST API endpoints on aio-01:5000. These endpoints translate HTTP requests into OrientDB queries.

### /graph/query

Execute an arbitrary OrientDB SQL query.

```bash
curl -X POST http://aio-01:5000/graph/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT name, type, frequency FROM Concept WHERE frequency > 100 ORDER BY frequency DESC LIMIT 10"
  }'
```

Response:

```json
{
  "results": [
    {"name": "firmware", "type": "technology", "frequency": 847},
    {"name": "bootloader", "type": "technology", "frequency": 623},
    {"name": "UEFI", "type": "standard", "frequency": 412}
  ],
  "elapsed_ms": 12
}
```

### /graph/traverse

Execute a traversal query with depth control and filtering.

```bash
curl -X POST http://aio-01:5000/graph/traverse \
  -H "Content-Type: application/json" \
  -d '{
    "start_class": "Concept",
    "start_filter": {"name": "firmware"},
    "edge_type": "RELATED_TO",
    "direction": "out",
    "max_depth": 3,
    "limit": 50
  }'
```

Response:

```json
{
  "vertices": [
    {"name": "firmware", "depth": 0},
    {"name": "flash_memory", "depth": 1},
    {"name": "NAND", "depth": 2},
    {"name": "wear_leveling", "depth": 3}
  ],
  "edges": [
    {"from": "firmware", "to": "flash_memory", "type": "RELATED_TO"},
    {"from": "flash_memory", "to": "NAND", "type": "RELATED_TO"},
    {"from": "NAND", "to": "wear_leveling", "type": "RELATED_TO"}
  ],
  "elapsed_ms": 8
}
```

---

## Why Not PostgreSQL for Graphs

PostgreSQL can model graphs using junction tables and recursive CTEs, but performance degrades significantly as traversal depth increases.

### Query Depth Performance Comparison

Benchmark: traversing RELATED_TO edges from a single concept vertex, measuring p50 latency. Both databases on the same hardware (aio-01), same dataset (48K concepts, 190K edges).

| Traversal Depth | OrientDB (ms) | PostgreSQL Recursive CTE (ms) | OrientDB Advantage |
|----------------|---------------|-------------------------------|-------------------|
| 1 hop | 0.8 | 1.2 | 1.5x |
| 2 hops | 2.1 | 8.5 | 4.0x |
| 3 hops | 4.7 | 45.0 | 9.6x |
| 4 hops | 9.2 | 280.0 | 30.4x |
| 5 hops | 18.0 | 1,800.0 | 100.0x |
| 6 hops | 34.0 | timeout (>10s) | -- |

**Why the exponential divergence:**

- **OrientDB** stores edges as direct pointers between vertex records. Traversing an edge is a pointer dereference, O(1) per hop regardless of graph size.
- **PostgreSQL** resolves each hop with a JOIN against the relationships table. At depth N, the query planner must manage N nested hash joins, and the intermediate result set grows combinatorially.

At 3 hops (the most common query depth in the system), OrientDB is nearly 10x faster. At 5 hops, the difference is two orders of magnitude.

---

## Resilience

OrientDB is treated as a non-critical enhancement layer. The system is designed to function without it.

### Non-Blocking Synchronization

Graph data is synchronized from PostgreSQL (the source of truth) via an asynchronous sync process. The sync is:

- **Unidirectional:** PostgreSQL -> OrientDB (never the reverse)
- **Eventual:** Sync lag of up to 60 seconds during normal operation
- **Non-blocking:** Sync failures do not affect the main API pipeline

### Graceful Fallback

When OrientDB is unavailable (container restart, maintenance, crash), the system degrades gracefully:

| Feature | With OrientDB | Without OrientDB |
|---------|--------------|-----------------|
| `/graph/query` | Full graph queries | Returns 503 with retry-after header |
| `/graph/traverse` | Native traversal | Returns 503 with retry-after header |
| `/search/intelligent` | PostgreSQL + Vector + Graph | PostgreSQL + Vector only (graph layer skipped) |
| Document ingestion | Full pipeline (store->chunk->embed->graph) | 3-stage pipeline (store->chunk->embed), graph stage queued |
| Concept relationships | Real-time traversal | Flat list from PostgreSQL (no traversal) |

The `/search/intelligent` endpoint automatically detects OrientDB availability and adjusts its search strategy. When the graph layer is down, results may be less contextually rich but remain functional.

### Recovery

When OrientDB comes back online:

1. The health check detects the restored connection (checked every 30 seconds)
2. The graph sync process replays any missed changes from the `graph_sync.pending` PostgreSQL table
3. Queued graph-stage pipeline items are released from Redis and processed
4. Full search capability is restored

No manual intervention is required.

---

## Why OrientDB Over Neo4j

The choice of OrientDB over Neo4j was driven by two factors: data persistence reliability and licensing.

### Data Persistence

Neo4j Community Edition exhibited data persistence bugs during evaluation:

- Transaction log corruption after unclean shutdown (Docker stop without graceful drain)
- Index rebuilds required after container restarts, adding 30-60 seconds to startup
- Write-ahead log replay occasionally dropped committed transactions

OrientDB's plocal storage engine has been reliable through hundreds of container restarts with zero data loss incidents.

### Licensing

| Aspect | Neo4j Community | Neo4j Enterprise | OrientDB Community |
|--------|----------------|-----------------|-------------------|
| License | GPL v3 | Commercial ($$$) | Apache 2.0 |
| Clustering | Not available | Enterprise only | Available |
| Hot backup | Not available | Enterprise only | Available |
| Role-based access | Not available | Enterprise only | Available |
| Stored procedures | Available | Available | Available |
| Graph query language | Cypher | Cypher | Extended SQL + Gremlin |
| Multi-model (doc + graph) | No | No | Yes (native) |

Neo4j gates critical operational features (clustering, hot backup, RBAC) behind the Enterprise license. OrientDB Community includes all of these under the Apache 2.0 license, which is also more permissive for integration into larger systems.

OrientDB's multi-model capability (document + graph in a single engine) is an additional advantage. Vertex properties are stored as embedded documents, eliminating the need for a separate document store for rich vertex metadata.
