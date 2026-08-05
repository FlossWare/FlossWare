---
title: Monitoring and Observability
---

# Monitoring and Observability

The orchestration framework provides comprehensive monitoring across fleet health, API providers, execution logging, queue depth, network latency, model diversity, feedback loops, cost tracking, and resource usage.

## Fleet Health Monitoring

Each fleet worker reports its status to the controller. The controller maintains a real-time view of all workers:

```bash
curl -s http://aio-01:5000/fleet/status | python3 -m json.tool
```

```json
{
  "workers": [
    {
      "node_id": "server-01",
      "status": "busy",
      "circuit_state": "closed",
      "current_task": "code-review-4821",
      "last_heartbeat": "2026-08-05T12:00:02Z",
      "tasks_completed": 1847,
      "tasks_failed": 23,
      "avg_response_ms": 3420,
      "memory_usage_mb": 1024,
      "cpu_percent": 45.2
    },
    {
      "node_id": "server-02",
      "status": "idle",
      "circuit_state": "closed",
      "current_task": null,
      "last_heartbeat": "2026-08-05T12:00:01Z",
      "tasks_completed": 1523,
      "tasks_failed": 41,
      "avg_response_ms": 4100,
      "memory_usage_mb": 768,
      "cpu_percent": 12.1
    },
    {
      "node_id": "server-03",
      "status": "unreachable",
      "circuit_state": "open",
      "current_task": null,
      "last_heartbeat": "2026-08-05T11:45:00Z",
      "tasks_completed": 892,
      "tasks_failed": 156,
      "avg_response_ms": null,
      "memory_usage_mb": null,
      "cpu_percent": null
    }
  ],
  "summary": {
    "total_workers": 7,
    "healthy": 5,
    "busy": 3,
    "idle": 2,
    "unreachable": 2,
    "circuit_breakers_open": 2
  }
}
```

### Worker Status Table

| Status | Meaning | Action Required |
|--------|---------|-----------------|
| `idle` | Worker is available and waiting for tasks | None |
| `busy` | Worker is currently executing a task | None |
| `draining` | Worker is finishing current task then stopping | None (graceful shutdown) |
| `unreachable` | No heartbeat received within timeout | Investigate connectivity |
| `error` | Worker reported an internal error | Check worker logs |

### Circuit Breaker States

| State | Meaning | Behavior |
|-------|---------|----------|
| `closed` | Normal operation | Tasks are routed to this worker |
| `open` | Worker has exceeded failure threshold | No tasks routed; waiting for reset timeout |
| `half-open` | Reset timeout expired; testing with limited traffic | 1-2 probe tasks sent; success closes, failure re-opens |

The health check timeout is **2000ms**. If a worker does not respond to a heartbeat within this window, it is marked `unreachable`.

## API Provider Health

External API providers (OpenRouter, Anthropic, Google, Groq, Cerebras, DeepSeek) are monitored for availability and performance. Each provider can be in one of three states:

| State | Meaning | Behavior |
|-------|---------|----------|
| `healthy` | API responding normally, latency within bounds | Full traffic routed |
| `degraded` | API responding but with elevated latency or error rate | Reduced traffic, fallback providers preferred |
| `disabled` | API not responding or rate-limited | No traffic routed; periodic probe to detect recovery |

```bash
curl -s http://aio-01:5000/providers/health | python3 -m json.tool
```

```json
{
  "providers": {
    "openrouter": {"state": "healthy", "latency_ms": 340, "error_rate": 0.02},
    "anthropic": {"state": "healthy", "latency_ms": 520, "error_rate": 0.01},
    "google": {"state": "degraded", "latency_ms": 2100, "error_rate": 0.15},
    "groq": {"state": "healthy", "latency_ms": 180, "error_rate": 0.03},
    "cerebras": {"state": "disabled", "latency_ms": null, "error_rate": 1.0},
    "deepseek": {"state": "healthy", "latency_ms": 890, "error_rate": 0.04}
  }
}
```

## Execution Logging

Every task execution is logged to two PostgreSQL tables:

### monitoring.execution_summary

High-level execution records:

| Field | Type | Description |
|-------|------|-------------|
| `execution_id` | UUID | Unique execution identifier |
| `task_type` | TEXT | Category of task (code-review, research, etc.) |
| `model_used` | TEXT | Primary model that handled the request |
| `strategy_id` | TEXT | Thompson Sampling strategy that was selected |
| `duration_ms` | INT | Total execution time |
| `quality_score` | FLOAT | Arbiter-assigned quality (0.0-1.0) |
| `cost_usd` | FLOAT | API cost for this execution |
| `outcome` | TEXT | "success", "failure", "timeout", "error" |
| `worker_node` | TEXT | Which fleet node executed this |
| `created_at` | TIMESTAMP | When execution started |

### workflow.worker_results

Per-worker results for multi-AI consensus tasks:

| Field | Type | Description |
|-------|------|-------------|
| `result_id` | UUID | Unique result identifier |
| `execution_id` | UUID | Parent execution |
| `worker_model` | TEXT | Model used by this worker |
| `worker_node` | TEXT | Fleet node that ran this worker |
| `response` | JSONB | Full worker response |
| `quality_score` | FLOAT | Individual quality score |
| `duration_ms` | INT | Worker-level duration |
| `token_count` | INT | Tokens consumed |

### Querying Execution Logs

```bash
# Recent executions via API
curl -s "http://aio-01:5000/monitoring/executions?limit=10&outcome=failure" \
  | python3 -m json.tool
```

```json
{
  "executions": [
    {
      "execution_id": "a1b2c3d4-...",
      "task_type": "code-review",
      "model_used": "claude-3-5-sonnet",
      "duration_ms": 45200,
      "quality_score": 0.32,
      "outcome": "failure",
      "error": "Model returned incomplete response",
      "created_at": "2026-08-05T11:58:00Z"
    }
  ],
  "total": 1,
  "page": 1
}
```

## Queue Monitoring

The system uses Redis-backed queues for task distribution. Queue depth indicates backlog and can signal bottlenecks.

```bash
curl -s http://aio-01:5000/monitoring/queues | python3 -m json.tool
```

```json
{
  "queues": {
    "task_routing": {"pending": 3, "processing": 2, "completed_1h": 145},
    "embedding": {"pending": 1247, "processing": 64, "completed_1h": 892},
    "scraping": {"pending": 0, "processing": 5, "completed_1h": 312},
    "store": {"pending": 18, "processing": 4, "completed_1h": 1023}
  }
}
```

### Prometheus-Compatible Metrics

The API exposes metrics in Prometheus exposition format for integration with Grafana:

```bash
curl -s http://aio-01:5000/metrics
```

```
# HELP fleet_queue_depth Number of pending items per queue
# TYPE fleet_queue_depth gauge
fleet_queue_depth{queue="task_routing"} 3
fleet_queue_depth{queue="embedding"} 1247
fleet_queue_depth{queue="scraping"} 0
fleet_queue_depth{queue="store"} 18

# HELP fleet_worker_status Current status of each worker
# TYPE fleet_worker_status gauge
fleet_worker_status{node="server-01",status="busy"} 1
fleet_worker_status{node="server-02",status="idle"} 1
fleet_worker_status{node="server-03",status="unreachable"} 1

# HELP fleet_execution_duration_seconds Execution duration histogram
# TYPE fleet_execution_duration_seconds histogram
fleet_execution_duration_seconds_bucket{le="1.0"} 234
fleet_execution_duration_seconds_bucket{le="5.0"} 1892
fleet_execution_duration_seconds_bucket{le="10.0"} 2341
fleet_execution_duration_seconds_bucket{le="30.0"} 2567
fleet_execution_duration_seconds_bucket{le="60.0"} 2589
fleet_execution_duration_seconds_bucket{le="+Inf"} 2601
fleet_execution_duration_seconds_sum 18432.7
fleet_execution_duration_seconds_count 2601

# HELP fleet_api_cost_usd_total Total API cost in USD
# TYPE fleet_api_cost_usd_total counter
fleet_api_cost_usd_total{provider="openrouter"} 0.0342
fleet_api_cost_usd_total{provider="anthropic"} 0.0
fleet_api_cost_usd_total{provider="google"} 0.0
fleet_api_cost_usd_total{provider="groq"} 0.0

# HELP fleet_model_selection_total Model selection count
# TYPE fleet_model_selection_total counter
fleet_model_selection_total{model="claude-3-5-sonnet"} 892
fleet_model_selection_total{model="llama-3.3-70b"} 743
fleet_model_selection_total{model="gemini-2.0-flash"} 634
fleet_model_selection_total{model="claude-3-5-haiku"} 521
```

Prometheus scrape config:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'claude-orchestrator'
    scrape_interval: 15s
    static_configs:
      - targets: ['aio-01:5000']
```

## Network Latency Matrix

The controller periodically measures network latency between itself and all fleet nodes:

```bash
curl -s http://aio-01:5000/monitoring/latency | python3 -m json.tool
```

```json
{
  "matrix": {
    "aio-01 -> server-01": {"avg_ms": 1.2, "p99_ms": 3.8},
    "aio-01 -> server-02": {"avg_ms": 1.4, "p99_ms": 4.1},
    "aio-01 -> server-03": {"avg_ms": 1.1, "p99_ms": 3.2},
    "aio-01 -> pi-01": {"avg_ms": 2.8, "p99_ms": 8.4},
    "aio-01 -> pi-02": {"avg_ms": 2.6, "p99_ms": 7.9},
    "aio-01 -> pi-03": {"avg_ms": 3.1, "p99_ms": 9.2},
    "aio-01 -> pi-04": {"avg_ms": 3.0, "p99_ms": 8.8}
  },
  "measured_at": "2026-08-05T12:00:00Z"
}
```

High latency (>10ms avg on a LAN) may indicate network congestion, NFS contention, or hardware issues.

## Model Diversity Monitoring

The system tracks which models are being selected to prevent any single model from dominating. The threshold for a diversity alert is **70%** -- if any one model handles more than 70% of requests, an alert is generated.

```bash
curl -s http://aio-01:5000/monitoring/diversity | python3 -m json.tool
```

```json
{
  "model_distribution": {
    "claude-3-5-sonnet": 0.28,
    "llama-3.3-70b": 0.24,
    "gemini-2.0-flash": 0.19,
    "claude-3-5-haiku": 0.15,
    "deepseek-v3": 0.08,
    "qwen-2.5-72b": 0.06
  },
  "dominant_model": "claude-3-5-sonnet",
  "dominant_share": 0.28,
  "diversity_healthy": true,
  "alert_threshold": 0.70,
  "window": "24h"
}
```

When `diversity_healthy` is `false`, the alert is stored in `monitoring.diversity_alerts` and the Thompson Sampling priors for the dominant model are decayed to re-enable exploration of alternatives.

## Feedback Loop Monitoring

The feedback loop optimizer detects self-referential patterns that could degrade system quality. It analyzes four layers:

| Layer | What It Detects | Threshold | Severity |
|-------|----------------|-----------|----------|
| Model Dominance | One model handles >70% of requests | 70% share | Medium (0.4-0.6) |
| Evaluator-Generator Coupling | A model evaluates its own outputs | 40% self-evaluation rate | High (0.6-0.8) |
| Reward Hacking | Quality scores rising while diversity falls | Quality up >10% AND diversity down >15% | Critical (0.8-1.0) |
| Concept Collapse | Output embeddings converging | Cosine similarity >0.90 between outputs | Critical (0.8-1.0) |

### Automated Schedule

Feedback loop analysis runs every 6 hours via cron:

```
0 */6 * * * /home/sfloess/bin/monitor-feedback-loops.sh
```

Reports are written to `~/.claude/reports/feedback-loops/latest.json`.

### Manual Analysis

**Python CLI:**

```bash
# Full analysis with 7-day window
python3 tools/feedback_loop_optimizer.py

# Custom window and output
python3 tools/feedback_loop_optimizer.py --window 30 --output /tmp/report.json
```

Exit codes:
- `0` -- No critical or high risks detected.
- `1` -- High risks found (severity 0.6-0.8). Investigation recommended.
- `2` -- Critical risks found (severity >0.8). Immediate action required.

**JavaScript API:**

```javascript
const { analyzeFeedbackLoops, isSystemHealthy } = require('./shared/feedback-loop-adapter.cjs');

// Quick health check
const healthy = await isSystemHealthy(7);  // 7-day window
console.log(healthy);  // true or false

// Full analysis
const analysis = await analyzeFeedbackLoops({ windowDays: 7 });
console.log(JSON.stringify(analysis, null, 2));
```

## Cost Tracking

API costs are tracked per-provider, per-model, and per-task-type:

```bash
curl -s "http://aio-01:5000/monitoring/costs?window=24h" | python3 -m json.tool
```

```json
{
  "window": "24h",
  "total_cost_usd": 0.0342,
  "by_provider": {
    "openrouter": 0.0342,
    "anthropic": 0.0,
    "google": 0.0,
    "groq": 0.0,
    "cerebras": 0.0,
    "deepseek": 0.0
  },
  "by_model": {
    "claude-3-5-sonnet": 0.0198,
    "llama-3.3-70b": 0.0089,
    "gemini-2.0-flash": 0.0055
  },
  "by_task_type": {
    "code-review": 0.0156,
    "research": 0.0112,
    "consensus": 0.0074
  }
}
```

Cost data is stored in the `costs.entries` table and used by the GA Model Routing Optimizer to penalize expensive configurations.

## Resource Usage

System resources are monitored per-node:

```bash
curl -s http://aio-01:5000/monitoring/resources | python3 -m json.tool
```

```json
{
  "nodes": {
    "aio-01": {
      "cpu_percent": 23.4,
      "memory_used_mb": 4096,
      "memory_total_mb": 16384,
      "disk_used_percent": 62.1,
      "load_avg_1m": 1.23
    },
    "server-01": {
      "cpu_percent": 45.2,
      "memory_used_mb": 8192,
      "memory_total_mb": 32768,
      "disk_used_percent": 48.3,
      "load_avg_1m": 2.56
    }
  }
}
```

## Alerting

The framework does not include a dedicated alerting daemon. Instead, the following conditions should be periodically checked (manually or via cron):

1. **Circuit breakers open** -- Any worker with `circuit_state: "open"` for more than 10 minutes indicates a persistent failure. Check the worker's logs and network connectivity.

2. **Queue depth growing** -- If the `embedding` queue has `pending > 5000` and is not decreasing, the embedding workers may be down or overwhelmed. Add more embedding workers or investigate the bottleneck.

3. **Cost spike** -- If `total_cost_usd` for a 24h window exceeds the expected budget, check which model and task type are responsible. The GA optimizer may be exploring expensive configurations.

4. **Diversity alert** -- If `diversity_healthy` is `false`, the Thompson Sampling system has converged too aggressively on one model. Review the feedback loop report and consider decaying priors.

5. **Feedback loop critical** -- If the feedback loop optimizer exits with code 2, there is a critical self-referential pattern. Review the report at `~/.claude/reports/feedback-loops/latest.json` and take corrective action (e.g., force model rotation, reset Thompson Sampling priors, inject diversity via GA mutation rate increase).
