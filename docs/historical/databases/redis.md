---
title: Redis Architecture
---

# Redis Architecture

Redis 8.0 serves as the high-throughput transient data layer for the orchestration framework. It handles pipeline queues, worker coordination, API response caching, and rate limiting. Redis runs on the controller node (aio-01) on port 6379.

## Connection Details

| Parameter | Value |
|-----------|-------|
| Host | aio-01 |
| Port | 6379 |
| Version | Redis 8.0 |
| Persistence | AOF (appendonly, everysec fsync) |
| Max memory | 4 GB |
| Eviction policy | allkeys-lru (cache keys only; queue keys are protected) |

---

## Pipeline Queues

The scraping and ingestion pipeline uses Redis sorted sets as priority queues. Each document flows through a 4-stage pipeline, with Redis managing the handoff between stages.

### 4-Stage Pipeline

```
  +----------+     +----------+     +----------+     +----------+
  |  STORE   | --> |  CHUNK   | --> |  EMBED   | --> |  GRAPH   |
  | (ingest) |     | (split)  |     | (vector) |     | (orient) |
  +----------+     +----------+     +----------+     +----------+
       |                |                |                |
  queue:store      queue:chunk      queue:embed      queue:graph
  (sorted set)    (sorted set)    (sorted set)    (sorted set)
       |                |                |                |
  processing:     processing:     processing:     processing:
    store            chunk            embed            graph
   (hash)           (hash)           (hash)           (hash)
```

Each stage has two Redis structures:

- **`queue:<stage>`** -- A sorted set where the score is the priority. Workers pop the highest-priority item with `ZPOPMIN`.
- **`processing:<stage>`** -- A hash tracking items currently being processed, keyed by item ID with a timestamp value for timeout detection.

### Priority Scoring

Priority scores determine processing order within each queue. Lower scores are processed first (ZPOPMIN returns the minimum).

**Formula:**

```
score = base_priority - (age_seconds * 0.001) - type_bonus
```

| Factor | Value | Effect |
|--------|-------|--------|
| `base_priority` (normal) | 100 | Default for standard documents |
| `base_priority` (high) | 50 | User-requested or referenced documents |
| `base_priority` (critical) | 10 | Error reprocessing, stale content refresh |
| `age_seconds * 0.001` | variable | Older items drift toward higher priority |
| `type_bonus` (API docs) | 5 | Technical documentation gets a small boost |
| `type_bonus` (code) | 3 | Source code files |

**Examples:**

```
Normal document, just queued:    100 - (0 * 0.001) - 0 = 100.000
Normal document, 1 hour old:     100 - (3600 * 0.001) - 0 = 96.400
High-priority, just queued:       50 - (0 * 0.001) - 0 = 50.000
Critical reprocess, 30 min old:   10 - (1800 * 0.001) - 0 =  8.200
API docs, high priority:          50 - (0 * 0.001) - 5 = 45.000
```

---

## Lua Scripts

Six Lua scripts execute atomically on Redis to prevent race conditions in queue operations. Each script is loaded once at API startup via `SCRIPT LOAD` and invoked by SHA hash.

| Script | Operation | Keys Touched | Purpose |
|--------|-----------|-------------|---------|
| `ADD` | Add item to queue | `queue:<stage>`, `dedup:seen` | Enqueue with priority score, skip if duplicate |
| `FETCH` | Pop and track item | `queue:<stage>`, `processing:<stage>` | Atomically move from queue to processing set |
| `COMPLETE` | Mark success | `processing:<stage>`, `queue:<next>` | Remove from processing, enqueue in next stage |
| `FAIL` | Mark failure | `processing:<stage>`, `queue:<stage>`, `failed:counts` | Return to queue with backoff, increment failure count |
| `RECLAIM` | Recover stuck items | `processing:<stage>`, `queue:<stage>` | Reclaim items stuck in processing beyond timeout |
| `HEARTBEAT` | Extend processing lease | `processing:<stage>` | Update timestamp to prevent premature reclaim |

### FETCH Script Example

The FETCH script is the most critical -- it atomically pops an item from the queue and registers it in the processing set, ensuring no two workers process the same item.

```lua
-- FETCH: Atomically pop highest-priority item and track it
-- KEYS[1] = queue:<stage>  (sorted set)
-- KEYS[2] = processing:<stage>  (hash)
-- ARGV[1] = worker_id
-- ARGV[2] = current_timestamp

-- Pop the lowest-score (highest-priority) item
local items = redis.call('ZPOPMIN', KEYS[1], 1)
if #items == 0 then
    return nil
end

local item_id = items[1]
local score = items[2]

-- Register in processing set with worker ID and timestamp
redis.call('HSET', KEYS[2], item_id,
    cjson.encode({
        worker = ARGV[1],
        started = ARGV[2],
        score = score
    })
)

-- Return the item ID and its score
return {item_id, score}
```

---

## Automatic Pipeline Chaining

When a stage completes successfully, the COMPLETE Lua script automatically enqueues the item into the next stage's queue. The chain is defined in the API configuration:

```
store --> chunk --> embed --> graph
```

The COMPLETE script for the `chunk` stage:

1. Removes the item from `processing:chunk`
2. Adds the item to `queue:embed` with a fresh priority score
3. Increments `stats:chunk:completed`

If a stage has no successor (graph is the final stage), COMPLETE simply removes the item from processing and records the final status.

---

## Idempotency and Deduplication

Every item entering the pipeline is checked against a deduplication set before enqueuing.

```
dedup:seen  (Redis SET)
```

- **Key format:** SHA-256 hash of the canonical URL or content identifier
- **TTL:** 7 days (configurable)
- **Check:** The ADD Lua script calls `SISMEMBER dedup:seen <hash>` before adding to the queue
- **On duplicate:** Returns a status indicating the item was skipped, with no error

This prevents the same URL from being re-scraped and re-processed when multiple sources reference it. The 7-day TTL allows intentional re-ingestion of content that may have changed.

---

## Worker Heartbeats

Workers send periodic heartbeats to extend their processing lease. Without heartbeats, stuck items would remain in `processing:<stage>` indefinitely.

| Parameter | Value |
|-----------|-------|
| Heartbeat interval | 30 seconds |
| Processing timeout | 90 seconds (3 missed heartbeats) |
| Reclaim check interval | 60 seconds |
| Max reclaim attempts | 3 (then moved to dead letter) |

### Heartbeat Flow

```
Worker                          Redis
  |                               |
  |-- HEARTBEAT (item_id, ts) -->|  Update timestamp in processing:<stage>
  |                               |
  |        ... 30 seconds ...     |
  |                               |
  |-- HEARTBEAT (item_id, ts) -->|  Update timestamp again
  |                               |
  |    ... worker crashes ...     |
  |                               |
  |                               |  (no heartbeat for 90s)
  |                               |
  |                  RECLAIM runs |  Move item back to queue:<stage>
  |                               |  Increment reclaim counter
```

The RECLAIM script runs every 60 seconds (triggered by the API process). It scans `processing:<stage>` for entries where `current_time - started > 90` and moves them back to the queue with an elevated priority score (base_priority = 10) so they are retried promptly.

After 3 reclaim attempts, the item is moved to `deadletter:<stage>` (a Redis list) for manual inspection.

---

## API Response Caching

Expensive API responses are cached in Redis to avoid redundant computation on repeated queries.

### Cache Key Structure

```
cache:<endpoint_hash>:<params_hash>
```

- **`endpoint_hash`:** SHA-256 of the API endpoint path
- **`params_hash`:** SHA-256 of the sorted, canonicalized request parameters
- **TTL:** 15 minutes (900 seconds)
- **Value:** JSON-serialized response body

### Cache Flow

```
Client Request
      |
      v
  Compute SHA-256 of endpoint + params
      |
      v
  GET cache:<hash>
      |
  +---+---+
  |       |
 HIT    MISS
  |       |
  |       v
  |   Execute query
  |       |
  |       v
  |   SET cache:<hash> <response> EX 900
  |       |
  +---+---+
      |
      v
  Return response
```

### Cached Endpoints

| Endpoint | Cache Rationale |
|----------|----------------|
| `/search/intelligent` | Vector similarity search is compute-intensive |
| `/learning/strategy` | Thompson Sampling state changes slowly |
| `/workflow/summary` | Materialized view data refreshes every 5 min |
| `/monitoring/health` | Fleet status polled frequently by dashboard |

Cache is invalidated explicitly when write operations occur on the underlying data. The API clears relevant cache keys using pattern-based `SCAN` + `DEL`.

---

## Rate Limiting

Per-provider rate limiting prevents API key exhaustion and respects upstream provider quotas. Redis counters track request counts within sliding windows.

### Rate Limit Key Structure

```
ratelimit:<provider>:<window>
```

- **`provider`:** Provider name (e.g., `openrouter`, `anthropic`, `google`, `groq`, `cerebras`)
- **`window`:** Unix timestamp truncated to the window boundary (e.g., minute boundary)
- **TTL:** 2x the window duration (auto-cleanup)

### Example: OpenRouter Rate Limiting

```
Provider: openrouter
Limit: 200 requests per minute
Window: 60 seconds

Key: ratelimit:openrouter:1722873600   (minute boundary timestamp)
Value: 147                              (requests so far this minute)
TTL: 120 seconds                        (auto-expire after 2 windows)
```

**Check-and-increment (atomic):**

```
MULTI
  INCR ratelimit:openrouter:1722873600
  EXPIRE ratelimit:openrouter:1722873600 120
EXEC
```

If the returned count exceeds the limit (200), the request is queued for retry after the window rolls over.

### Provider Rate Limits

| Provider | Requests/Minute | Tokens/Minute | Concurrent |
|----------|----------------|---------------|------------|
| OpenRouter | 200 | 100K | 10 |
| Anthropic | 60 | 80K | 5 |
| Google | 60 | 60K | 5 |
| Groq | 30 | 30K | 3 |
| Cerebras | 30 | 20K | 3 |
| DeepSeek | 60 | 50K | 5 |

---

## Why Redis Over PostgreSQL for Queues

| Characteristic | Redis | PostgreSQL |
|---------------|-------|------------|
| Pop latency (ZPOPMIN) | < 0.1 ms | 2-5 ms (SELECT FOR UPDATE SKIP LOCKED) |
| Throughput (ops/sec) | 100K+ | 5-10K |
| Atomic multi-key operations | Lua scripts (single-threaded, no locks) | Requires explicit transactions and row locks |
| TTL on keys | Native (per-key EXPIRE) | Requires cron job or trigger cleanup |
| Pub/Sub for stage notifications | Built-in | LISTEN/NOTIFY (limited payload) |
| Memory overhead per queue item | ~200 bytes | ~1 KB (row overhead, MVCC tuples) |
| Persistence needs | Transient (loss = re-enqueue) | Overkill for ephemeral queue data |
| Connection overhead | Single long-lived connection | Connection pool management |

**The deciding factors:**

1. **Latency** -- Queue operations happen on every pipeline step. Sub-millisecond pops are essential when processing thousands of documents.
2. **Atomicity without locks** -- Lua scripts execute atomically without row-level locking, eliminating deadlock risk entirely.
3. **Transient data** -- Queue items are ephemeral. If Redis restarts, items are re-discovered from PostgreSQL source tables and re-enqueued. Full ACID durability is unnecessary overhead.
4. **Separation of concerns** -- PostgreSQL handles durable storage and complex queries. Redis handles coordination and throughput-sensitive operations. Each tool does what it does best.
