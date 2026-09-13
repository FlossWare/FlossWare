---
title: Vector Embeddings
---

# Vector Embeddings

Vector embeddings convert text chunks into fixed-length numerical vectors that capture semantic meaning. Two chunks about the same concept produce vectors that are close together in vector space, enabling similarity search without keyword matching.

## What Embeddings Are

An embedding model reads a text string and outputs a vector (a list of floating-point numbers). The position of this vector in high-dimensional space encodes the text's meaning. Semantically similar texts cluster together; dissimilar texts are far apart.

**Cosine similarity** measures how close two vectors are, returning a value between -1 (opposite) and 1 (identical):

```
cosine_similarity(A, B) = (A . B) / (|A| * |B|)
```

Examples with `all-mpnet-base-v2`:

| Text A                        | Text B                          | Cosine Similarity |
|-------------------------------|---------------------------------|-------------------|
| "database connection timeout" | "DB connection timed out"       | 0.94              |
| "database connection timeout" | "how to connect to PostgreSQL"  | 0.71              |
| "database connection timeout" | "chocolate cake recipe"         | 0.08              |

The first pair is a paraphrase (high similarity). The second pair shares the topic of databases (moderate similarity). The third pair is unrelated (near zero).

## Embedding Model

The system uses **`all-mpnet-base-v2`** from the `sentence-transformers` library as its primary embedding model.

| Property             | Value                    |
|----------------------|--------------------------|
| Model name           | `all-mpnet-base-v2`      |
| Architecture         | MPNet (Microsoft)        |
| Output dimensions    | 768                      |
| Max input tokens     | 512                      |
| Training data        | 1B+ sentence pairs       |
| Model size           | 420 MB                   |
| Inference speed (CPU)| ~50 ms per chunk         |
| Inference speed (GPU)| ~5 ms per chunk          |
| Similarity metric    | Cosine similarity        |

This model was chosen for its balance of quality and speed. It outperforms smaller models (MiniLM) on semantic benchmarks while remaining small enough to run on CPU worker nodes.

## Multiple Embedding Dimensions

Not all data types require full 768-dimensional embeddings. The system uses three dimensionalities, trading precision for storage and search speed:

| Dimensions | Bytes per Vector | Use Case                          | Data Types                     |
|------------|------------------|-----------------------------------|--------------------------------|
| 128        | 512              | Coarse similarity, deduplication  | Log entries, status messages   |
| 384        | 1,536            | Workflow matching, task routing   | Task descriptions, summaries   |
| 768        | 3,072            | Full semantic search              | Documentation, code, research  |

Dimensionality reduction from 768 to lower dimensions uses PCA (Principal Component Analysis) fitted on a representative corpus. The PCA transform matrix is stored alongside the model and applied at embedding time.

```python
from sklearn.decomposition import PCA

# Fit once on representative data
pca_384 = PCA(n_components=384)
pca_384.fit(representative_embeddings)

# Apply at embedding time
full_embedding = model.encode(text)       # 768-dim
reduced_embedding = pca_384.transform([full_embedding])[0]  # 384-dim
```

## HNSW Indexing

Brute-force similarity search (comparing a query vector against every stored vector) is O(n). The system uses **HNSW** (Hierarchical Navigable Small World) indexing via `pgvector` to achieve approximate nearest-neighbor search in O(log n).

### How HNSW Works

HNSW builds a multi-layer graph of vectors. The top layer contains a sparse subset of nodes with long-range connections. Each successive layer adds more nodes with shorter-range connections. Search starts at the top layer and descends, narrowing the candidate set at each level.

```
Layer 3 (sparse):    A ---------> D ----------------------> H
                     |                                      |
Layer 2:             A ----> C ----> D ---------> F ----> H
                     |       |       |            |       |
Layer 1:             A -> B -> C -> D -> E -> F -> G -> H
                     |    |    |    |    |    |    |    |
Layer 0 (dense):     A  B  C  D  E  F  G  H  I  J  K  L  M  N

Query: "network timeout"
  1. Enter at Layer 3, jump A -> D -> H (3 comparisons)
  2. Drop to Layer 2, refine around H: F -> H (1 comparison)
  3. Drop to Layer 1, refine: F -> G -> H (2 comparisons)
  4. Drop to Layer 0, scan neighbors of G: F, G, H, I (4 comparisons)
  Result: G (10 total comparisons vs 14 for brute force)
```

### Performance Benchmarks

Measured on 100,000 768-dimensional vectors, 10 concurrent queries:

| Metric                    | ChromaDB (in-process) | PostgreSQL + pgvector (HNSW) |
|---------------------------|-----------------------|------------------------------|
| Simple similarity (768-d) | 1.2 ms                | 0.97 ms                      |
| Simple similarity (128-d) | 0.9 ms                | 0.44 ms (**2x faster**)      |
| Filtered similarity       | 2.3 ms                | 0.44 ms (**5x faster**)      |
| Index build time          | 4.2 s                 | 6.8 s                        |
| Memory usage              | 890 MB                | 620 MB                       |
| Concurrent query scaling  | Degrades at 5+        | Linear to 50+                |

PostgreSQL + pgvector wins on query performance and concurrent scaling. ChromaDB builds indexes faster but does not scale under concurrent load because it runs in-process with a global lock.

The filtered similarity advantage (5x) is critical: most real queries include metadata filters (e.g., "find chunks similar to X where source_type = 'documentation'"). ChromaDB applies filters after similarity search; pgvector applies them during search via its query planner.

## Compute Isolation Architecture

Embedding generation is CPU-intensive (or GPU-intensive). Running it on the same nodes that serve API requests causes latency spikes and resource contention. The system separates concerns:

```
 +-------------------+      +-------------------+
 |   Fleet Workers   |      |  Compute Nodes    |
 |  (aio-01, pi-*)   |      |  (server-01/02)   |
 +-------------------+      +-------------------+
 | API serving        |      | Embedding gen     |
 | Request routing    |      | PCA reduction     |
 | Chunk orchestration|      | Model inference   |
 | Result aggregation |      | Batch processing  |
 +-------------------+      +-------------------+
         |                           |
         |   POST /embed             |
         +-------------------------->|
         |                           |
         |   { vectors: [...] }      |
         |<--------------------------+
         |                           |
```

**Fleet workers** handle all request routing, chunking, and storage. They never load the embedding model.

**Compute nodes** load the embedding model once at startup and serve embedding requests over HTTP. They have no knowledge of the storage layer.

This separation means:

1. Fleet workers remain responsive during heavy embedding workloads.
2. Compute nodes can be scaled independently (add more servers for throughput).
3. The embedding model is loaded once per compute node, not once per worker.

## Embedding Workers

When a large batch of chunks needs embedding (e.g., after a scraping run produces thousands of documents), the work is distributed across compute nodes using **range-based partitioning**.

### 5-Step Process

1. **Collect** -- Query `knowledge.chunks` for all chunks lacking embeddings:
   ```sql
   SELECT c.id, c.content
   FROM knowledge.chunks c
   LEFT JOIN knowledge.embeddings e ON e.chunk_id = c.id
   WHERE e.id IS NULL
   ORDER BY c.id;
   ```

2. **Partition** -- Divide the chunk IDs into contiguous ranges, one per compute node:
   ```
   Node server-01: chunk IDs 1 - 5000
   Node server-02: chunk IDs 5001 - 10000
   ```

3. **Dispatch** -- Send each range to its compute node via HTTP:
   ```
   POST http://server-01:8080/embed/batch
   { "chunk_ids": [1, 5000], "dimensions": 768 }
   ```

4. **Embed** -- Each compute node reads the chunk text (via a shared database connection), runs the embedding model, and returns vectors.

5. **Store** -- Vectors are written to `knowledge.embeddings` in bulk:
   ```sql
   INSERT INTO knowledge.embeddings (chunk_id, embedding, dimensions)
   VALUES ($1, $2, $3)
   ON CONFLICT (chunk_id, dimensions) DO NOTHING;
   ```

Range-based partitioning ensures no overlap between nodes and no coordination overhead. If a node fails, its range can be re-dispatched to another node.

## Cascading Fallback

Embedding generation has three fallback levels to ensure chunks are never permanently stuck without embeddings:

```
 +----------------+     fail     +----------------+     fail     +----------+
 |  Local Model   | ----------> |   API Model    | ----------> |   Skip   |
 |  (server-01/02)|             |  (OpenAI API)  |             |  (NULL)  |
 +----------------+             +----------------+             +----------+
   sentence-transformers          text-embedding-3-small         Mark as
   all-mpnet-base-v2              1536-dim -> PCA to 768        skipped,
   ~50ms / chunk                  ~200ms / chunk                retry later
```

1. **Local model** (preferred): The `all-mpnet-base-v2` model running on compute nodes. Fastest, no API cost, full control.

2. **API model** (fallback): If all compute nodes are down, the system falls back to an external embedding API. The API returns higher-dimensional vectors that are reduced to 768 dimensions via PCA to maintain index compatibility.

3. **Skip** (last resort): If the API is also unavailable, the chunk is stored with a NULL embedding and a `retry_after` timestamp. A background job retries skipped chunks every 30 minutes.

## Embedding Pool

Concurrent embedding requests are managed by an **EmbeddingPool** that limits parallelism to avoid overwhelming compute nodes:

```javascript
class EmbeddingPool {
    constructor({
        maxConcurrent = 4,
        timeout = 30000,       // 30 seconds per request
        retries = 3,
        backoff = [1000, 2000, 4000]  // Exponential backoff
    }) {
        this.semaphore = new Semaphore(maxConcurrent);
        this.timeout = timeout;
        this.retries = retries;
        this.backoff = backoff;
    }

    async embed(texts) {
        return this.semaphore.acquire(async () => {
            for (let attempt = 0; attempt <= this.retries; attempt++) {
                try {
                    return await this._doEmbed(texts, this.timeout);
                } catch (err) {
                    if (attempt < this.retries) {
                        await sleep(this.backoff[attempt]);
                        continue;
                    }
                    throw err;
                }
            }
        });
    }
}
```

`maxConcurrent = 4` means at most 4 embedding requests are in flight simultaneously across the entire system. This prevents a burst of scraper output from saturating compute nodes with embedding work.

## LRU Cache

Identical text chunks produce identical embeddings. The system maintains a **1000-entry LRU cache** to avoid redundant computation:

```
 Input text
     |
     v
 SHA-256 hash of text
     |
     v
 Cache lookup (hash -> embedding)
     |
     +-- HIT: return cached embedding (0 ms)
     |
     +-- MISS: compute embedding, store in cache, return
```

Cache keys are **SHA-256 hashes** of the input text. This keeps keys a fixed 64 characters regardless of text length, and avoids storing full text strings in the cache.

```python
import hashlib
from functools import lru_cache

def text_to_key(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

class EmbeddingCache:
    def __init__(self, max_size=1000):
        self.cache = OrderedDict()
        self.max_size = max_size

    def get(self, text):
        key = text_to_key(text)
        if key in self.cache:
            self.cache.move_to_end(key)
            return self.cache[key]
        return None

    def put(self, text, embedding):
        key = text_to_key(text)
        self.cache[key] = embedding
        self.cache.move_to_end(key)
        if len(self.cache) > self.max_size:
            self.cache.popitem(last=False)
```

When the cache is unavailable (e.g., after a process restart), the system falls back to **hash-based deduplication** in the database: before computing an embedding, it checks whether `knowledge.embeddings` already contains an entry for the chunk ID.

## Storage Schema

Embeddings are stored in the `knowledge.embeddings` table, which uses `pgvector` for native vector operations:

```sql
CREATE TABLE knowledge.embeddings (
    id          SERIAL PRIMARY KEY,
    chunk_id    INTEGER NOT NULL REFERENCES knowledge.chunks(id) ON DELETE CASCADE,
    embedding   vector(768) NOT NULL,
    dimensions  INTEGER NOT NULL DEFAULT 768,
    model_name  VARCHAR(100) NOT NULL DEFAULT 'all-mpnet-base-v2',
    created_at  TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE (chunk_id, dimensions)
);

-- HNSW index for cosine distance similarity search
CREATE INDEX idx_embeddings_hnsw
    ON knowledge.embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
```

The HNSW index parameters:

- `m = 16`: Each node connects to 16 neighbors. Higher values improve recall at the cost of index size.
- `ef_construction = 200`: Number of candidates considered during index build. Higher values produce a better index at the cost of build time.

### Cosine Distance Query

To find the 10 chunks most similar to a query:

```sql
-- Generate query embedding externally, then:
SELECT
    c.id AS chunk_id,
    c.content,
    c.metadata,
    1 - (e.embedding <=> $1::vector) AS similarity
FROM knowledge.embeddings e
JOIN knowledge.chunks c ON c.id = e.chunk_id
WHERE e.dimensions = 768
ORDER BY e.embedding <=> $1::vector
LIMIT 10;
```

The `<=>` operator computes cosine distance (1 - cosine similarity). The `ORDER BY` clause uses the HNSW index for approximate nearest-neighbor search, avoiding a full table scan.

Adding metadata filters narrows results without sacrificing index usage:

```sql
SELECT
    c.id AS chunk_id,
    c.content,
    1 - (e.embedding <=> $1::vector) AS similarity
FROM knowledge.embeddings e
JOIN knowledge.chunks c ON c.id = e.chunk_id
WHERE e.dimensions = 768
  AND c.metadata->>'source_type' = 'documentation'
  AND c.metadata->>'language' = 'python'
ORDER BY e.embedding <=> $1::vector
LIMIT 10;
```
