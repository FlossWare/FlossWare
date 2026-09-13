---
title: Orchestration Layer
---

# Orchestration Layer

The orchestration layer is the control plane that sits between callers (the CLI, workflows, cron jobs) and the distributed fleet. It accepts task requests, classifies them, selects models, dispatches work to workers, collects results, runs consensus, and returns the final answer. Every decision the orchestration layer makes is logged for post-hoc analysis.

---

## Request Lifecycle

A request enters the system via the REST API on `aio-01:5000` and passes through a 10-step pipeline before a response is returned.

```
                         REQUEST LIFECYCLE

  Caller (CLI / Workflow / Cron)
    |
    | POST /route-thompson
    v
  +------------------------------------------------------------------+
  |                    ORCHESTRATION PIPELINE                         |
  |                                                                  |
  |  Step 1: RECEIVE                                                 |
  |    Parse request body, validate schema, assign request_id        |
  |                                                                  |
  |  Step 2: CLASSIFY                                                |
  |    Determine task category (code_review, research, etc.)         |
  |    Extract metadata: language, complexity, context_length         |
  |                                                                  |
  |  Step 3: BLUEPRINT SELECTION                                     |
  |    Match task category to execution blueprint                    |
  |    Blueprint defines: worker count, timeout, consensus strategy  |
  |                                                                  |
  |  Step 4: MODEL SELECTION                                         |
  |    Thompson Sampling selects N models from 200+ candidates       |
  |    Filtered by: provider availability, rate limits, capability   |
  |                                                                  |
  |  Step 5: WORKER ASSIGNMENT                                       |
  |    Map selected models to available workers                      |
  |    Respect tier preferences and circuit breaker state             |
  |                                                                  |
  |  Step 6: DISPATCH                                                |
  |    Send tasks to workers in parallel via SSH                     |
  |    Start timeout timer per worker                                |
  |                                                                  |
  |  Step 7: COLLECTION                                              |
  |    Await worker responses, handle timeouts and errors            |
  |    Minimum response threshold: 3 of N (configurable)             |
  |                                                                  |
  |  Step 8: CONSENSUS                                               |
  |    Run selected consensus strategy (rotating, majority, etc.)    |
  |    Arbiter synthesizes worker responses into unified answer      |
  |                                                                  |
  |  Step 9: FEEDBACK                                                |
  |    Update Thompson Sampling: reward models that contributed      |
  |    to consensus, penalize timeouts and low-scoring responses     |
  |                                                                  |
  |  Step 10: RESPOND                                                |
  |    Return consensus response to caller                           |
  |    Store execution record in PostgreSQL                          |
  |                                                                  |
  +------------------------------------------------------------------+
    |
    v
  Response returned to caller
```

### Step-by-Step Detail

**Step 1: Receive.** The REST API accepts POST requests with a JSON body containing at minimum a `task` field (string) and optionally `category`, `models`, `strategy`, `timeout`, and `metadata`. A UUID `request_id` is assigned and returned in the response headers for tracing.

**Step 2: Classify.** If the caller did not provide a `category`, the system classifies the task using keyword matching and a lightweight classifier. Classification determines which blueprint governs execution. Misclassification is the most common source of suboptimal routing -- the classifier is intentionally conservative and falls back to `general` when uncertain.

**Step 3: Blueprint Selection.** Each task category maps to an execution blueprint that defines operational parameters. Blueprints are stored as JSON files and can be overridden per-request. See the Blueprint Organization section below.

**Step 4: Model Selection.** Thompson Sampling draws from Beta distributions to select models. Each model maintains a Beta(alpha, beta) distribution where alpha tracks successes and beta tracks failures. The system samples from each distribution and selects the top N models by sampled value. See the [Model Routing](routing.html) page for details.

**Step 5: Worker Assignment.** Selected models are mapped to available workers. The assignment algorithm prefers Tier 1 workers for compute-heavy tasks and distributes evenly across tiers for light tasks. Workers with OPEN circuit breakers are skipped.

**Step 6: Dispatch.** Tasks are dispatched to all assigned workers simultaneously via SSH. Each dispatch includes the task prompt, model identifier, API key (injected from the secrets store), timeout value, and response format specification.

**Step 7: Collection.** The controller waits for worker responses up to the blueprint-defined timeout. Workers that respond after the timeout are logged but their responses are discarded. If fewer than the minimum response threshold respond, the system retries with different workers before falling back.

**Step 8: Consensus.** The collected responses are passed to the consensus engine. The consensus strategy is determined by the blueprint. See the [Consensus Engine](consensus.html) page for details on each strategy.

**Step 9: Feedback.** After consensus is computed, the system updates Thompson Sampling parameters for each model that participated. Models whose responses scored highly in the consensus receive alpha increments (successes). Models that timed out or scored poorly receive beta increments (failures). This closes the feedback loop: future routing decisions reflect past performance.

**Step 10: Respond.** The final consensus response is returned to the caller. The full execution record (request, classification, model selection, worker assignments, individual responses, consensus result, timing) is stored in `workflow.executions` and related tables in PostgreSQL.

---

## Blueprint Organization

Blueprints define the operational parameters for each task category. There are 22 blueprints covering the full range of tasks the system handles.

| Blueprint | Workers | Timeout | Strategy | Min Responses | Description |
|-----------|---------|---------|----------|---------------|-------------|
| `code_review` | 6 | 90s | rotating | 3 | Review code for bugs, style, and correctness |
| `security_audit` | 6 | 120s | weighted | 4 | Security-focused review with higher consensus bar |
| `architecture` | 6 | 120s | rotating | 4 | Architectural design review and recommendations |
| `implementation` | 4 | 180s | rotating | 2 | Code generation and implementation |
| `refactoring` | 4 | 90s | rotating | 2 | Code restructuring suggestions |
| `debugging` | 6 | 90s | weighted | 3 | Root cause analysis and fix suggestions |
| `testing` | 4 | 90s | rotating | 2 | Test generation and test review |
| `documentation` | 3 | 60s | majority | 2 | Documentation writing and review |
| `research` | 6 | 300s | rotating | 3 | Deep research across multiple sources |
| `classification` | 4 | 30s | majority | 3 | Binary or categorical classification |
| `summarization` | 3 | 60s | majority | 2 | Text summarization |
| `translation` | 3 | 60s | majority | 2 | Natural language translation |
| `data_analysis` | 4 | 120s | weighted | 2 | Analyze datasets and produce insights |
| `planning` | 6 | 120s | rotating | 3 | Project planning and task decomposition |
| `evaluation` | 6 | 90s | pairwise | 4 | Evaluate and rank candidate solutions |
| `brainstorming` | 6 | 60s | rotating | 3 | Divergent idea generation |
| `explanation` | 3 | 60s | majority | 2 | Explain concepts clearly |
| `comparison` | 4 | 90s | pairwise | 3 | Compare alternatives with pros/cons |
| `troubleshooting` | 6 | 90s | weighted | 3 | Diagnose infrastructure or system issues |
| `configuration` | 3 | 60s | majority | 2 | Generate or review configuration files |
| `migration` | 4 | 120s | rotating | 3 | Plan and execute code/data migrations |
| `general` | 4 | 90s | rotating | 2 | Catch-all for unclassified tasks |

Blueprints are stored in `/mnt/aio-01/claude-orchestrator/blueprints/` as individual JSON files. Callers can override any blueprint parameter in their request body.

---

## Task Classification

The classifier assigns incoming tasks to one of 15 primary categories. Classification drives blueprint selection, model selection, and timeout behavior.

| Category | Keywords / Signals | Default Priority |
|----------|-------------------|------------------|
| `code_review` | review, check, audit, lint, bugs | high |
| `security_audit` | security, vulnerability, CVE, injection, XSS | critical |
| `architecture` | design, architecture, system, scalability | high |
| `implementation` | implement, build, create, write code, function | medium |
| `refactoring` | refactor, clean up, simplify, extract | medium |
| `debugging` | debug, error, stack trace, exception, fix | high |
| `testing` | test, spec, coverage, assert, mock | medium |
| `documentation` | document, README, docstring, comment, explain code | low |
| `research` | research, investigate, find, compare options | medium |
| `classification` | classify, categorize, is this, which type | low |
| `summarization` | summarize, TLDR, brief, overview, digest | low |
| `planning` | plan, roadmap, decompose, break down, steps | medium |
| `evaluation` | evaluate, rank, score, which is better | medium |
| `troubleshooting` | troubleshoot, diagnose, not working, broken | high |
| `general` | (fallback when no strong signal) | medium |

Classification confidence is included in the execution record. When confidence is below 0.6, the system logs a `low_confidence_classification` event for later review and tuning.

---

## Weighted Voting

When the consensus strategy is `weighted`, each worker response is scored across four dimensions before synthesis.

### Dimensions

| Dimension | Weight | Source | Description |
|-----------|--------|--------|-------------|
| Capability Score | 0.35 | Thompson Sampling | Model's historical success rate for this task category |
| Recency Bonus | 0.20 | EWMA | How well the model has performed in the last 50 requests |
| Provider Reliability | 0.25 | Uptime tracking | Provider's API availability over the last 24 hours |
| Response Quality | 0.20 | Arbiter scoring | Arbiter's per-response relevance/correctness/completeness score |

### Formula

```
weighted_score = (capability_score * 0.35)
              + (recency_bonus * 0.20)
              + (provider_reliability * 0.25)
              + (response_quality * 0.20)
```

The weighted score is normalized to [0, 1] and used by the arbiter to weight disagreements. When two workers disagree, the worker with the higher weighted score has more influence on the synthesized answer.

### Example

```
  Worker 1 (Opus):     capability=0.92, recency=0.88, reliability=0.99, quality=0.90
    weighted_score = (0.92*0.35) + (0.88*0.20) + (0.99*0.25) + (0.90*0.20)
                   = 0.322 + 0.176 + 0.2475 + 0.180
                   = 0.9255

  Worker 2 (Qwen):     capability=0.71, recency=0.65, reliability=0.94, quality=0.74
    weighted_score = (0.71*0.35) + (0.65*0.20) + (0.94*0.25) + (0.74*0.20)
                   = 0.2485 + 0.130 + 0.235 + 0.148
                   = 0.7615

  --> Opus response weighted 22% higher than Qwen in synthesis
```

---

## Circuit Breaker Pattern

Each external API provider has a circuit breaker that prevents the system from repeatedly calling a provider that is experiencing an outage. This is separate from the per-worker circuit breakers described in the fleet documentation.

```
               PROVIDER CIRCUIT BREAKER STATE MACHINE

                    +--------+
                    | CLOSED |  Normal operation
                    |        |  All requests flow through
                    +---+----+
                        |
                        | error_count >= 5 in 60s window
                        v
                    +--------+
                    |  OPEN  |  Provider blocked
                    |        |  All requests fail-fast with 503
                    +---+----+
                        |
                        | 30 seconds elapsed
                        v
                  +-----------+
                  | HALF-OPEN |  Probe mode
                  |           |  1 request allowed through
                  +-----+-----+
                       / \
                      /   \
                 success   failure
                    /         \
                   v           v
              +--------+   +--------+
              | CLOSED |   |  OPEN  |
              +--------+   +--------+
```

### Provider Circuit Breaker Parameters

| Provider | Failure Threshold | Open Duration | Notes |
|----------|------------------|---------------|-------|
| OpenRouter | 5 errors / 60s | 30s | Most models route through here |
| Anthropic | 3 errors / 60s | 60s | Higher bar due to limited free tier |
| Google (Gemini) | 5 errors / 60s | 30s | Standard parameters |
| Groq | 5 errors / 60s | 15s | Short open time; recovers fast |
| Cerebras | 5 errors / 60s | 30s | Standard parameters |
| DeepSeek | 5 errors / 60s | 30s | Standard parameters |

When a provider's circuit breaker is OPEN, the model selection step skips all models from that provider. This can reduce the available model pool significantly if multiple providers are down simultaneously.

---

## Rate Limiting

The orchestration layer enforces per-provider rate limits to stay within API quotas and avoid 429 responses. Rate limits are tracked in Redis using sliding window counters.

| Provider | Requests/min | Tokens/min | Concurrent | Notes |
|----------|-------------|------------|------------|-------|
| OpenRouter | 60 | 100,000 | 10 | Free tier limits |
| Anthropic | 50 | 80,000 | 5 | Personal API key |
| Google (Gemini) | 60 | 120,000 | 10 | Free tier |
| Groq | 30 | 30,000 | 5 | Free tier, very fast |
| Cerebras | 30 | 60,000 | 5 | Free tier |
| DeepSeek | 60 | 100,000 | 10 | Generous free tier |

When a rate limit is approaching (>80% consumed), the model selection step deprioritizes models from that provider. When the limit is hit (100%), the provider is temporarily blocked until the window slides.

### Rate Limit Headers

Worker responses include rate limit headers from the upstream provider. The controller parses these headers and adjusts its internal counters to stay synchronized with the provider's actual state:

```
X-RateLimit-Remaining: 12
X-RateLimit-Reset: 1720000000
X-RateLimit-Limit: 60
```

---

## Error Handling

Errors are classified as recoverable or non-recoverable. Recoverable errors trigger automatic retry with backoff. Non-recoverable errors are returned to the caller immediately.

### Recoverable Errors

| Error | Retry Strategy | Max Retries | Backoff |
|-------|---------------|-------------|---------|
| Worker timeout | Retry with different worker | 3 | 1s, 2s, 4s |
| Provider 429 (rate limit) | Wait for reset, retry | 3 | Use `Retry-After` header |
| Provider 500/502/503 | Retry with same provider | 2 | 1s, 2s |
| SSH connection refused | Retry with different worker | 3 | 1s, 2s, 4s |
| Network timeout | Retry with different worker | 3 | 2s, 4s, 8s |
| Redis connection lost | Retry with local fallback | 2 | 1s, 2s |
| NFS mount unavailable | Retry with local storage | 1 | 0s |

### Non-Recoverable Errors

| Error | Response | Action |
|-------|----------|--------|
| Invalid API key | 401 to caller | Log and alert; key may be expired |
| Model not found | 404 to caller | Remove model from pool, update discovery |
| Request too large | 413 to caller | Suggest chunking or model with larger context |
| All workers offline | 503 to caller | Alert via monitoring; wait for recovery |
| All providers down | 503 to caller | All circuit breakers OPEN; critical alert |
| Database unreachable | 500 to caller | Controller cannot log; degrade to in-memory |
| Malformed request | 400 to caller | Return validation errors in response body |

### Retry with Exponential Backoff

All retries use exponential backoff with jitter to prevent thundering herd:

```
delay = base_delay * (2 ^ attempt) + random(0, base_delay * 0.5)

Example (base_delay = 1s):
  Attempt 1: 1s + random(0, 0.5s) = ~1.0-1.5s
  Attempt 2: 2s + random(0, 0.5s) = ~2.0-2.5s
  Attempt 3: 4s + random(0, 0.5s) = ~4.0-4.5s
```

After all retries are exhausted, the error is returned to the caller with the full retry history included in the response body for debugging.

---

## Execution Records

Every request produces an execution record stored in PostgreSQL across multiple tables:

- **`workflow.executions`**: Top-level record with request_id, task, category, timing, outcome.
- **`workflow.worker_results`**: One row per worker response with model, latency, raw response, score.
- **`workflow.arbiter_decisions`**: The arbiter's synthesis, scores, agreement/disagreement points.
- **`workflow.phases`**: Timing breakdown per pipeline step (classify, select, dispatch, collect, consensus).
- **`learning.strategy_performance`**: Updated Thompson Sampling parameters post-execution.

These records enable:

1. **Performance analysis**: Which models are fastest/most accurate for which task types?
2. **Cost tracking**: How many tokens were consumed per request, per provider?
3. **Debugging**: When a consensus result is wrong, trace back to which worker responses led to it.
4. **Optimization**: Identify bottlenecks in the pipeline (e.g., classification taking too long, one provider always timing out).
