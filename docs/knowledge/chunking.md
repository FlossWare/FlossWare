---
title: Document Chunking
---

# Document Chunking

Document chunking is the process of splitting large documents into smaller, semantically meaningful pieces before they are embedded and stored. It sits between raw ingestion and vector embedding in the knowledge pipeline, and its quality directly determines retrieval accuracy.

## Why Chunking Is Necessary

Large language models and embedding models both operate within fixed **context windows**. A document that exceeds the window must be split, but naive splitting (e.g., every N characters) destroys meaning. Two specific problems make chunking essential:

**Context window limits.** Embedding models like `all-mpnet-base-v2` accept a maximum of 512 tokens per input. Documents routinely exceed this. Passing a truncated document silently discards everything after the cutoff.

**Diluted embeddings.** Even when a document fits within the window, embedding an entire page produces a single vector that represents the *average* meaning of all its content. A page about both "network configuration" and "disk partitioning" yields a vector that is close to neither topic. Retrieval queries for either topic score poorly against this diluted vector. Chunking isolates each topic into its own vector, making similarity search precise.

## Semantic Splitting

Chunking strategies differ between prose and code because their structural boundaries differ.

### Prose Documents

Prose is split at **paragraph boundaries** first, then at sentence boundaries if a paragraph exceeds the maximum chunk size.

The split regex identifies paragraph breaks as two or more consecutive newlines:

```python
PARAGRAPH_SPLIT = re.compile(r'\n{2,}')
```

The algorithm:

1. Split the document on `PARAGRAPH_SPLIT`.
2. Accumulate consecutive paragraphs into a chunk until adding the next paragraph would exceed `max_chunk_size`.
3. If a single paragraph exceeds `max_chunk_size`, split it further at sentence boundaries (`. `, `? `, `! `).
4. Emit each chunk with metadata recording its character offset within the source document.

```python
def chunk_prose(text, max_size=1500, min_size=500):
    paragraphs = PARAGRAPH_SPLIT.split(text)
    current_chunk = []
    current_length = 0

    for para in paragraphs:
        para = para.strip()
        if not para:
            continue

        if current_length + len(para) > max_size and current_length >= min_size:
            yield ''.join(current_chunk)
            current_chunk = []
            current_length = 0

        current_chunk.append(para + '\n\n')
        current_length += len(para)

    if current_chunk:
        yield ''.join(current_chunk)
```

### Code Documents

Code is split at **function and class boundaries**. Splitting mid-function destroys the semantic unit that developers search for.

Boundary detection uses language-aware regex patterns:

```python
CODE_BOUNDARIES = {
    'python': re.compile(r'^(class |def |async def )', re.MULTILINE),
    'javascript': re.compile(
        r'^(function |class |const \w+ = |export (default )?(function|class) )',
        re.MULTILINE
    ),
    'java': re.compile(
        r'^(\s*(public|private|protected)\s+(static\s+)?(class|void|int|String)\s)',
        re.MULTILINE
    ),
}
```

Example: given a Python file with three functions, the chunker produces three chunks:

```
Source file:
+--------------------------+
| def connect(host, port): |    --> Chunk 1
|     ...                  |
+--------------------------+
| def query(sql):          |    --> Chunk 2
|     ...                  |
+--------------------------+
| class ConnectionPool:    |    --> Chunk 3
|     def __init__(self):  |
|     def acquire(self):   |
|     ...                  |
+--------------------------+
```

If a single function exceeds `max_chunk_size`, it is split at logical sub-boundaries (blank lines within the function body), preserving the function signature as a prefix on each sub-chunk.

## Chunk Size Control

Three parameters govern chunk dimensions:

| Parameter       | Default | Unit       | Purpose                                      |
|-----------------|---------|------------|----------------------------------------------|
| `min_chunk_size`| 500     | characters | Prevents fragments too small to embed usefully |
| `max_chunk_size`| 1500    | characters | Keeps chunks within embedding model limits     |
| `overlap`       | 100     | characters | Preserves context across chunk boundaries      |

These defaults are tuned for `all-mpnet-base-v2` (512-token window, roughly 4 characters per token). Larger embedding models can use larger chunks.

## Overlap

Adjacent chunks share an **overlap region** so that concepts spanning a boundary are captured in both chunks. Without overlap, a sentence split across two chunks would be incomplete in both, causing retrieval misses.

```
Document:
|<------------- Chunk 1 ------------->|
                              |<-- overlap -->|
                              |<------------- Chunk 2 ------------->|
                                                            |<-- overlap -->|
                                                            |<------------- Chunk 3 ------------->|

Character positions:
0          500        1000       1500       2000       2500       3000
|-----------|-----------|-----------|-----------|-----------|-----------|
|<---- Chunk 1 (0-1500) ---->|
                      |<---- Chunk 2 (1400-2900) ---->|
                                            |<---- Chunk 3 (2800-4300) ---->|
                      ^                     ^
                      |                     |
                 overlap=100           overlap=100
```

The overlap is always taken from the **end** of the preceding chunk. This means the last 100 characters of Chunk 1 are identical to the first 100 characters of Chunk 2. During retrieval, deduplication logic detects overlapping chunks from the same source document and merges them.

## Chunk Size Tradeoffs

| Size    | Characters | Pros                                           | Cons                                              |
|---------|------------|------------------------------------------------|---------------------------------------------------|
| Small   | 200-500    | Precise retrieval; each chunk is tightly focused | More chunks to store and search; higher storage cost; loses broader context |
| Medium  | 500-1500   | Good balance of precision and context; fits embedding model windows well | May split some multi-paragraph arguments across chunks |
| Large   | 1500-3000  | Preserves long-form reasoning; fewer chunks     | Diluted embeddings; may exceed model token limits; retrieval returns irrelevant padding |

The system defaults to **medium** (500-1500) as the best tradeoff for general-purpose knowledge retrieval.

## Memory Efficiency

Chunking operates on documents that can be arbitrarily large. Loading an entire document into memory before chunking wastes resources and risks out-of-memory errors on constrained worker nodes.

### Generator-Based Streaming

The chunker exposes both a **batch interface** (returns a list) and a **streaming interface** (returns a generator). The streaming interface is preferred:

```python
# Batch: loads all chunks into memory at once
chunks = list(chunk_document(text))

# Streaming: yields one chunk at a time (preferred)
for chunk in chunk_document(text):
    process(chunk)
```

The streaming interface keeps at most two chunks in memory at any time (the current chunk being built and the previous chunk for overlap extraction).

### 10 MB Document Limit

Documents larger than 10 MB are rejected before chunking begins. This limit exists because:

1. Documents above 10 MB are almost always binary files (PDFs with embedded images, archives) that were misclassified as text.
2. Chunking a 10 MB text document would produce thousands of chunks, overwhelming the embedding pipeline.
3. Worker nodes have limited memory; a 10 MB string plus its chunks could consume 30+ MB of heap.

```python
MAX_DOCUMENT_SIZE = 10 * 1024 * 1024  # 10 MB

def chunk_document(text, **kwargs):
    if len(text) > MAX_DOCUMENT_SIZE:
        raise DocumentTooLargeError(
            f"Document size {len(text)} exceeds limit {MAX_DOCUMENT_SIZE}"
        )
    # ... proceed with chunking
```

## Storage Schema

Chunks are stored in the `knowledge.chunks` table in PostgreSQL:

```sql
CREATE TABLE knowledge.chunks (
    id              SERIAL PRIMARY KEY,
    document_id     INTEGER NOT NULL REFERENCES knowledge.documents(id) ON DELETE CASCADE,
    chunk_index     INTEGER NOT NULL,
    content         TEXT NOT NULL,
    char_offset     INTEGER NOT NULL,
    char_length     INTEGER NOT NULL,
    token_count     INTEGER,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE (document_id, chunk_index)
);

-- Full-text search index on chunk content
CREATE INDEX idx_chunks_content_gin
    ON knowledge.chunks
    USING GIN (to_tsvector('english', content));

-- Fast lookup by document
CREATE INDEX idx_chunks_document_id
    ON knowledge.chunks (document_id);
```

The `GIN` index on `content` enables PostgreSQL full-text search as a complement to vector similarity search. Queries that include exact keywords (e.g., error codes, function names) use the GIN index for precise matching, while semantic queries use the vector index on the associated embeddings table.

The `metadata` JSONB column stores chunk-specific attributes:

```json
{
  "language": "python",
  "chunk_type": "function",
  "function_name": "connect",
  "source_file": "lib/db.py",
  "split_reason": "code_boundary"
}
```

## Pipeline Integration

Chunking sits in the middle of the linear ingestion pipeline:

```
 +----------+      +----------+      +----------+
 |  Store   | ---> |  Chunk   | ---> |  Embed   |
 +----------+      +----------+      +----------+
   Raw doc           Split into         Generate
   ingested          semantic           vector for
   to disk           pieces             each chunk
```

1. **Store** -- The raw document arrives (via scraper, upload, or API) and is persisted to the `knowledge.documents` table with its full text and source metadata.

2. **Chunk** -- The chunker reads the document text, determines its type (prose vs. code, with language detection for code), and splits it into chunks. Each chunk is written to `knowledge.chunks` with its offset, length, and metadata.

3. **Embed** -- The embedding pipeline reads chunks that lack embeddings (joining `knowledge.chunks` against `knowledge.embeddings` to find gaps) and generates a vector for each. Vectors are stored in the `knowledge.embeddings` table and indexed for similarity search.

Each stage is **idempotent**. Re-running the chunker on an already-chunked document detects existing chunks (via the `UNIQUE(document_id, chunk_index)` constraint) and skips them. This allows safe retries after partial failures.
