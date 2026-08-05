---
title: Coding Standards
---

# Coding Standards

This document defines the coding standards and conventions used across the orchestration framework. All contributions must follow these guidelines to maintain consistency and reliability.

## JavaScript

### ES Modules

All JavaScript files use ES Modules with the `.mjs` extension. Use `import`/`export` syntax exclusively.

```javascript
// Correct - ES Module (.mjs)
import { getDB } from './postgres-adapter.mjs';
import { routeRequest } from './multi-model-router.mjs';

export async function processTask(task) {
  // ...
}
```

```javascript
// Avoid - CommonJS
const { getDB } = require('./postgres-adapter.js');
module.exports = { processTask };
```

The exception is adapter files that must interoperate with legacy tooling, which use `.cjs` with explicit CommonJS syntax.

### Async/Await

Use `async`/`await` for all asynchronous operations. Never use raw `.then()` chains.

```javascript
// Correct
async function fetchModels() {
  const response = await fetch('http://aio-01:5000/fleet/status');
  const data = await response.json();
  return data.models;
}
```

```javascript
// Avoid
function fetchModels() {
  return fetch('http://aio-01:5000/fleet/status')
    .then(response => response.json())
    .then(data => data.models);
}
```

### Error Handling

Wrap all external calls in `try`/`catch` blocks. Always log the error before rethrowing or returning a fallback.

```javascript
async function queryFleet(endpoint) {
  try {
    const response = await fetch(`http://aio-01:5000${endpoint}`);
    if (!response.ok) {
      throw new Error(`Fleet returned ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`[queryFleet] Failed for ${endpoint}:`, error.message);
    throw error;
  }
}
```

## Python

### Type Hints

All function signatures must include type hints. Use `typing` imports for complex types.

```python
from typing import Optional, Dict, List, Any

def analyze_results(
    responses: List[Dict[str, Any]],
    threshold: float = 0.7,
    model_filter: Optional[str] = None
) -> Dict[str, float]:
    """Analyze consensus results and return confidence scores."""
    scores: Dict[str, float] = {}
    for response in responses:
        model = response["model"]
        if model_filter and model != model_filter:
            continue
        scores[model] = response.get("confidence", 0.0)
    return scores
```

### Docstrings

Write docstrings in imperative mood. Describe what the function does, not what it returns.

```python
def route_to_workers(task: str, worker_count: int) -> List[str]:
    """Distribute a task across available fleet workers.

    Split the task into subtasks and assign each to a worker node.
    Fall back to sequential execution if fewer than 2 workers are
    available.

    Args:
        task: The task description to distribute.
        worker_count: Number of workers to use.

    Returns:
        List of worker node identifiers that received subtasks.

    Raises:
        FleetUnavailableError: If no workers respond to health checks.
    """
```

### Exception Handling

Catch specific exception types. Never use bare `except`.

```python
# Correct - specific exception types
try:
    response = requests.post(api_url, json=payload, timeout=10)
    response.raise_for_status()
except requests.exceptions.Timeout:
    logger.warning("API call timed out after 10s: %s", api_url)
    return fallback_response()
except requests.exceptions.HTTPError as e:
    logger.error("HTTP error %d from %s", e.response.status_code, api_url)
    raise
except requests.exceptions.ConnectionError:
    logger.error("Cannot connect to %s", api_url)
    raise FleetUnavailableError(api_url)
```

```python
# Wrong - bare except swallows everything
try:
    response = requests.post(api_url, json=payload)
except:
    pass
```

## Database Access

### REST API Only

All database interactions go through the orchestrator REST API. Components never connect directly to PostgreSQL.

```javascript
// Correct - use REST API
const response = await fetch('http://aio-01:5000/learning/experiences', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ task, outcome, embedding })
});
```

```javascript
// Wrong - direct database connection
const { Client } = require('pg');
const client = new Client({ host: 'aio-01', port: 5433, database: 'learning' });
await client.connect();
```

### Shared Adapters

Use the provided adapter libraries for common database operations. Never write raw queries outside of adapters.

| Adapter | File | Purpose |
|---------|------|---------|
| PostgreSQL | `postgres-adapter.js` | Core database operations, experience storage, strategy queries |
| Workflow Storage | `workflow-storage-adapter.js` | Workflow execution tracking, worker results, arbiter decisions |
| Feedback Loop | `feedback-loop-adapter.cjs` | Feedback loop detection, system health checks, diversity monitoring |

### Track All Workflows

Every workflow execution must be stored. Record the workflow ID, worker assignments, duration, and outcome.

```javascript
import { getWorkflowStorage } from './workflow-storage-adapter.js';

const db = getWorkflowStorage();
const execId = await db.storeExecution({
  workflow_id: 'wf-' + Date.now(),
  workflow_name: 'code-review',
  task_description: description,
  total_workers: workers.length,
  total_duration_ms: elapsed,
  outcome: result.passed ? 'success' : 'failure'
});
```

## Workflow Patterns

### Zero Model Overlap Between Review and Meta-Review

The models that perform a review must never also perform the meta-review of that same review. This prevents self-referential evaluation.

```javascript
// Correct - no overlap between reviewers and meta-reviewers
const reviewModels = ['gemini-2.5-flash', 'llama-3.3-70b', 'qwen-2.5-72b'];
const metaReviewModels = ['claude-sonnet-4', 'gpt-4o', 'deepseek-chat'];
```

```javascript
// Wrong - models reviewing their own output
const reviewModels = ['gemini-2.5-flash', 'claude-sonnet-4', 'qwen-2.5-72b'];
const metaReviewModels = ['claude-sonnet-4', 'gpt-4o', 'gemini-2.5-flash'];
//                        ^^^^^^^^^^^^^^^^              ^^^^^^^^^^^^^^^^^
//                        Also in reviewModels!         Also in reviewModels!
```

### Different Arbiter Per Phase

Each workflow phase must use a different arbiter model to prevent single-model bias from propagating through the pipeline.

```javascript
const phases = [
  { name: 'design',   arbiter: 'claude-sonnet-4' },
  { name: 'implement', arbiter: 'gemini-2.5-pro' },
  { name: 'review',   arbiter: 'gpt-4o' },
  { name: 'finalize', arbiter: 'deepseek-chat' }
];
```

### Strong Models for Meta-Review

Meta-reviewers synthesize and judge the output of other models. They must be strong, capable models.

```javascript
// Wrong - weak models as meta-reviewers
const metaReviewModels = ['llama-3.1-8b', 'phi-3-mini', 'gemma-2-2b'];

// Correct - strong models as meta-reviewers
const metaReviewModels = ['claude-sonnet-4', 'gpt-4o', 'gemini-2.5-pro'];
```

## General Conventions

### No Hardcoded Model Names

Model names must come from configuration, environment, or the routing API. Never hardcode model identifiers in business logic.

```javascript
// Correct - model from config/routing
const model = await routeRequest(task, { strategy: 'quality-first' });

// Wrong - hardcoded model name
const model = 'claude-sonnet-4';
```

### Rate-Limit External Calls

All external API calls must include rate limiting with exponential backoff. Use 3 retries with 1s, 2s, and 4s delays.

```javascript
async function callWithRetry(fn, maxRetries = 3) {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      if (attempt === maxRetries - 1) throw error;
      const delay = Math.pow(2, attempt) * 1000;
      console.warn(`Retry ${attempt + 1}/${maxRetries} after ${delay}ms`);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
}
```

### Use Persistent Paths

Never write to `/tmp`. The `/tmp` directory is a tmpfs (RAM-backed) filesystem that runs out of inodes. Use `/home` or project-relative paths for all working data.

```javascript
// Correct
const outputPath = '/home/sfloess/data/results/output.json';

// Wrong - /tmp is RAM-backed, will run out of inodes
const outputPath = '/tmp/output.json';
```

### Idempotent Operations

All operations must be safe to retry. Use upsert semantics for database writes and check-before-create for resources.

```javascript
// Idempotent - safe to call multiple times
await db.query(
  `INSERT INTO learning.experiences (id, task, outcome)
   VALUES ($1, $2, $3)
   ON CONFLICT (id) DO UPDATE SET outcome = $3`,
  [id, task, outcome]
);
```

### Store Execution Data

Every significant operation must record its execution metadata: timestamps, models used, duration, input hash, and outcome.

```javascript
const execution = {
  started_at: new Date().toISOString(),
  models_used: selectedModels,
  duration_ms: Date.now() - startTime,
  input_hash: hashInput(task),
  outcome: result.success ? 'success' : 'failure',
  error: result.error || null
};
await storeExecution(execution);
```
