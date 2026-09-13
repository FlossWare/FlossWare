---
title: Knowledge Graph
---

# Knowledge Graph

The knowledge graph captures relationships between documents, chunks, workflows, and concepts that cannot be expressed as simple vector proximity. It answers questions like "what workflows used this document?" and "what concepts are related to this topic through a chain of references?"

## Triple-Store Architecture

Knowledge is stored across three databases, each serving a distinct role:

```
 +-------------------+      +-------------------+      +-------------------+
 |    PostgreSQL      |      |      Redis        |      |    OrientDB       |
 |   (primary store)  |      |   (hot cache)     |      |  (graph store)    |
 +-------------------+      +-------------------+      +-------------------+
 | knowledge.documents|      | Recent queries    |      | Nodes: Document,  |
 | knowledge.chunks   |      | Frequent edges    |      |   Chunk, Concept, |
 | knowledge.embeddings      | Session state     |      |   Workflow        |
 | workflow.*         |      | Pub/sub channels  |      | Edges: CONTAINS,  |
 | learning.*         |      |                   |      |   REFERENCES,     |
 +-------------------+      +-------------------+      |   DERIVED_FROM,   |
         |                           |                  |   RELATED_TO, ... |
         |                           |                  +-------------------+
         +---------------------------+---------------------------+
                                     |
                              Application Layer
                         (reads/writes all three)
```

Data flows **from PostgreSQL to OrientDB** (not the reverse). PostgreSQL is the source of truth for all structured data. OrientDB mirrors relationship data as a graph for efficient traversal queries. Redis caches hot paths for both.

## Why Three Databases

Each database was chosen for what it does best. No single database satisfies all three requirements simultaneously:

| Requirement                  | PostgreSQL           | Redis               | OrientDB            |
|------------------------------|----------------------|---------------------|---------------------|
| ACID transactions            | Yes                  | No                  | Partial             |
| Vector similarity search     | Yes (pgvector)       | No                  | No                  |
| Full-text search             | Yes (GIN indexes)    | No                  | Yes (Lucene)        |
| Graph traversal (multi-hop)  | Poor (recursive CTE) | No                  | **Native O(1) hops**|
| Sub-millisecond reads        | No (disk-bound)      | **Yes (in-memory)** | No                  |
| Complex joins                | **Yes**              | No                  | Partial             |
| Horizontal write scaling     | Moderate             | **Yes**             | Moderate            |

The critical distinction is **graph traversal**. PostgreSQL can express graph queries using recursive CTEs, but performance degrades exponentially with hop depth. OrientDB traverses edges in O(1) per hop regardless of graph size.

## OrientDB Node Types

The graph contains four node types (vertex classes):

| Node Type    | Properties                                       | Source                          |
|--------------|--------------------------------------------------|---------------------------------|
| `Document`   | `doc_id`, `title`, `source_url`, `source_type`   | `knowledge.documents`           |
| `Chunk`      | `chunk_id`, `doc_id`, `chunk_index`, `token_count`| `knowledge.chunks`             |
| `Concept`    | `name`, `description`, `category`                 | Extracted from chunk content    |
| `Workflow`   | `workflow_id`, `name`, `outcome`, `duration_ms`   | `workflow.executions`           |

## OrientDB Edge Types

Seven edge types capture relationships:

| Edge Type      | From        | To          | Properties              | Meaning                                    |
|----------------|-------------|-------------|-------------------------|--------------------------------------------|
| `CONTAINS`     | Document    | Chunk       | `chunk_index`           | Document contains this chunk               |
| `REFERENCES`   | Chunk       | Chunk       | `ref_type`, `confidence`| One chunk cites or links to another        |
| `DERIVED_FROM` | Document    | Document    | `derivation_type`       | Document was derived from another (summary, translation) |
| `RELATED_TO`   | Chunk       | Chunk       | `similarity`, `method`  | Chunks are semantically similar (from embeddings) |
| `MENTIONS`     | Chunk       | Concept     | `count`, `positions`    | Chunk mentions this concept                |
| `USED_BY`      | Document    | Workflow    | `role`, `phase`         | Document was used in this workflow          |
| `PRODUCED_BY`  | Document    | Workflow    | `output_type`           | Document was produced by this workflow      |

## Graph Queries That SQL Cannot Efficiently Express

### Example 1: Concept Neighborhood (Multi-Hop Traversal)

"Find all concepts related to 'network timeout' within 3 hops, and the documents that mention them."

**OrientDB (native graph, ~5 ms):**

```sql
SELECT expand(both('RELATED_TO', 'MENTIONS', 'CONTAINS'))
FROM (
    SELECT FROM Concept WHERE name = 'network timeout'
)
WHILE $depth <= 3
```

This traverses from the "network timeout" concept node outward along any combination of `RELATED_TO`, `MENTIONS`, and `CONTAINS` edges, up to 3 hops deep. Each hop is O(1) -- it follows pointers, not table scans.

**PostgreSQL equivalent (recursive CTE, ~850 ms on 100K nodes):**

```sql
WITH RECURSIVE graph AS (
    -- Base case: start node
    SELECT c.id, c.name, 0 AS depth
    FROM concepts c
    WHERE c.name = 'network timeout'

    UNION ALL

    -- Recursive case: follow edges
    SELECT c2.id, c2.name, g.depth + 1
    FROM graph g
    JOIN concept_edges ce ON ce.from_id = g.id
    JOIN concepts c2 ON c2.id = ce.to_id
    WHERE g.depth < 3
)
SELECT DISTINCT g.name, d.title
FROM graph g
JOIN chunk_concepts cc ON cc.concept_id = g.id
JOIN chunks ch ON ch.id = cc.chunk_id
JOIN documents d ON d.id = ch.document_id;
```

The PostgreSQL version requires 4 joins per hop level, and the recursive CTE materializes all intermediate results. At 3 hops with a branching factor of 10, this touches 1,000+ rows per query.

### Example 2: Provenance Chain

"Trace the full lineage of a document back to its original sources."

**OrientDB (~2 ms):**

```sql
TRAVERSE in('DERIVED_FROM') FROM (
    SELECT FROM Document WHERE doc_id = 42
)
```

This follows `DERIVED_FROM` edges backward (inbound) until it reaches nodes with no inbound `DERIVED_FROM` edges -- the original sources.

**PostgreSQL equivalent (~400 ms):**

```sql
WITH RECURSIVE lineage AS (
    SELECT id, title, NULL::integer AS parent_id, 0 AS depth
    FROM knowledge.documents
    WHERE id = 42

    UNION ALL

    SELECT d.id, d.title, dd.source_id, l.depth + 1
    FROM lineage l
    JOIN document_derivations dd ON dd.derived_id = l.id
    JOIN knowledge.documents d ON d.id = dd.source_id
    WHERE l.depth < 20  -- safety limit
)
SELECT * FROM lineage ORDER BY depth;
```

## Graph Sync

OrientDB is populated from PostgreSQL, not the reverse. The sync process is **non-blocking** -- it runs in a background worker and does not hold locks on PostgreSQL tables.

### WorkflowGraphSync MERGE Pattern

When a workflow completes, its results are synced to the graph using OrientDB's `MERGE` operation, which is an upsert:

```javascript
class WorkflowGraphSync {
    async syncWorkflow(execution) {
        // MERGE creates or updates the vertex
        await this.orientdb.command(
            `MERGE INTO Workflow
             SET workflow_id = :wfId,
                 name = :name,
                 outcome = :outcome,
                 duration_ms = :duration
             UPSERT
             WHERE workflow_id = :wfId`,
            {
                wfId: execution.workflow_id,
                name: execution.workflow_name,
                outcome: execution.outcome,
                duration: execution.total_duration_ms
            }
        );

        // Sync edges for documents used by this workflow
        for (const doc of execution.input_documents) {
            await this.orientdb.command(
                `LET $wf = (SELECT FROM Workflow WHERE workflow_id = :wfId);
                 LET $doc = (SELECT FROM Document WHERE doc_id = :docId);
                 CREATE EDGE USED_BY FROM $doc TO $wf
                 SET role = :role, phase = :phase`,
                {
                    wfId: execution.workflow_id,
                    docId: doc.id,
                    role: doc.role,
                    phase: doc.phase
                }
            );
        }
    }
}
```

### PostgreSQL CTE Fallback

When OrientDB is unavailable, the system falls back to PostgreSQL recursive CTEs for graph queries. This fallback is **automatic and transparent** to callers:

```javascript
async function queryGraph(query) {
    try {
        return await orientdb.command(query.orientql);
    } catch (err) {
        if (err.code === 'ECONNREFUSED') {
            console.warn('OrientDB unavailable, falling back to PostgreSQL CTE');
            return await postgres.query(query.fallbackSQL);
        }
        throw err;
    }
}
```

Every graph query is defined with both an OrientDB query and a PostgreSQL CTE fallback. The OrientDB version is faster for traversals; the PostgreSQL version is correct but slower.

## Similarity Links

When new embeddings are generated, a background job creates `RELATED_TO` edges between chunks whose cosine similarity exceeds a threshold:

**Threshold: 0.85**

```sql
-- Find highly similar chunk pairs (PostgreSQL)
SELECT
    a.chunk_id AS chunk_a,
    b.chunk_id AS chunk_b,
    1 - (a.embedding <=> b.embedding) AS similarity
FROM knowledge.embeddings a
CROSS JOIN LATERAL (
    SELECT chunk_id, embedding
    FROM knowledge.embeddings
    WHERE chunk_id != a.chunk_id
    ORDER BY embedding <=> a.embedding
    LIMIT 5
) b
WHERE 1 - (a.embedding <=> b.embedding) >= 0.85;
```

For each qualifying pair, a `RELATED_TO` edge is created in OrientDB:

```javascript
await orientdb.command(
    `LET $a = (SELECT FROM Chunk WHERE chunk_id = :idA);
     LET $b = (SELECT FROM Chunk WHERE chunk_id = :idB);
     CREATE EDGE RELATED_TO FROM $a TO $b
     SET similarity = :sim, method = 'cosine'`,
    { idA: pair.chunk_a, idB: pair.chunk_b, sim: pair.similarity }
);
```

The 0.85 threshold was empirically tuned: lower values produce too many edges (noisy graph), higher values miss valid relationships. At 0.85, approximately 3% of chunk pairs are linked.

## REST API

The graph is exposed via a REST endpoint on the orchestrator:

**Endpoint:** `POST /graph/query`

**Request body:**

```json
{
    "query_type": "neighborhood",
    "start_node": { "type": "Concept", "name": "network timeout" },
    "max_depth": 3,
    "edge_types": ["RELATED_TO", "MENTIONS"],
    "limit": 50
}
```

**Response:**

```json
{
    "nodes": [
        { "type": "Concept", "name": "network timeout", "depth": 0 },
        { "type": "Concept", "name": "connection reset", "depth": 1 },
        { "type": "Chunk", "chunk_id": 1842, "depth": 2 }
    ],
    "edges": [
        { "from": "network timeout", "to": "connection reset", "type": "RELATED_TO", "similarity": 0.91 }
    ],
    "query_time_ms": 5
}
```

### Query Types

| Query Type      | Description                                          | Parameters                               |
|-----------------|------------------------------------------------------|------------------------------------------|
| `neighborhood`  | All nodes within N hops of a start node              | `start_node`, `max_depth`, `edge_types`  |
| `path`          | Shortest path between two nodes                      | `start_node`, `end_node`, `edge_types`   |
| `provenance`    | Full lineage of a document back to original sources   | `document_id`                            |
| `subgraph`      | All nodes connected to a set of start nodes           | `start_nodes`, `max_depth`               |
| `concepts`      | Concepts mentioned by a document's chunks             | `document_id`                            |

## Why OrientDB Over Neo4j

The system uses OrientDB instead of Neo4j for two reasons:

**1. Persistence reliability.** During evaluation, Neo4j Community Edition exhibited data loss on unclean shutdown (e.g., power failure, OOM kill). Write-ahead log recovery failed to restore the last several seconds of transactions. OrientDB's append-only storage format proved more resilient to unclean shutdown in testing on the same hardware.

**2. Native REST API.** OrientDB exposes a built-in HTTP/REST API that accepts queries and returns JSON. No driver library is needed -- any HTTP client can query the graph:

```bash
curl -X POST http://orientdb-host:2480/command/knowledge/sql \
     -u admin:admin \
     -H "Content-Type: application/json" \
     -d '{"command": "SELECT FROM Concept WHERE name = '\''network timeout'\''"}'
```

Neo4j requires the Bolt protocol and a language-specific driver library. In a polyglot fleet (JavaScript orchestrator, Python scrapers, Bash scripts), adding a Neo4j driver to each component introduces dependency overhead. OrientDB's REST API is callable from all of them with `curl` or any HTTP library.
