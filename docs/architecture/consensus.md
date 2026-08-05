---
title: Consensus Engine
---

# Consensus Engine

The consensus engine queries multiple LLMs for the same task and synthesizes their responses into a single, higher-quality answer. It is the core differentiator of the orchestration framework: instead of trusting one model, the system triangulates across models with different training data, architectures, and failure modes.

---

## How Consensus Works

A consensus request proceeds through four stages: dispatch, collection, scoring, and synthesis.

```
                          CONSENSUS FLOW

  Task: "Review this function for off-by-one errors"
    |
    v
  +------------------+
  | Model Selection   |    Thompson Sampling selects 6 models
  | (Thompson Sampling)|    from 200+ candidates based on
  +------------------+    task type: code_review
    |
    |  Dispatch in parallel
    v
  +----------+  +----------+  +----------+  +----------+  +----------+  +----------+
  | Worker 1 |  | Worker 2 |  | Worker 3 |  | Worker 4 |  | Worker 5 |  | Worker 6 |
  | Sonnet   |  | Opus     |  | GPT-4o   |  | Gemini   |  | DeepSeek |  | Qwen     |
  +----------+  +----------+  +----------+  +----------+  +----------+  +----------+
    |  resp A     |  resp B     |  resp C     |  timeout    |  resp E     |  resp F
    |             |             |             |  (skip)     |             |
    v             v             v                           v             v
  +------------------------------------------------------------------------+
  |                         ARBITER (Fable)                                 |
  |                                                                        |
  |  Inputs:                                                               |
  |    - 5 worker responses (A, B, C, E, F)                                |
  |    - Weighted scores per response                                      |
  |    - Original task description                                         |
  |                                                                        |
  |  Process:                                                              |
  |    1. Score each response (relevance, correctness, completeness)        |
  |    2. Identify agreement points (mentioned by 3+ responses)            |
  |    3. Identify disagreement points (strong contradiction)              |
  |    4. Weight by capability score and historical accuracy                |
  |    5. Synthesize unified answer                                        |
  |    6. Flag unresolved disagreements                                    |
  |                                                                        |
  |  Output:                                                               |
  |    - Synthesized consensus response                                    |
  |    - Per-response scores                                               |
  |    - Agreement/disagreement summary                                    |
  |    - Confidence level (high/medium/low)                                |
  +------------------------------------------------------------------------+
    |
    v
  Consensus Response returned to caller
```

---

## Consensus Strategies

The system supports five consensus strategies, selectable per request or per task category.

### Rotating (default)

The arbiter model rotates through a predefined list across consecutive requests. This prevents arbiter bias from accumulating. Rotation order is deterministic (round-robin keyed by request count mod pool size). The rotation pool defaults to: Opus, Sonnet, Fable, Haiku.

### Single

A fixed arbiter model handles all synthesis. Useful when you want consistent synthesis behavior.

### Majority

No arbiter. The system counts agreement among worker responses using semantic similarity. If 4 of 6 workers agree on a finding, it is included. Best for classification tasks and binary decisions.

### Weighted

Similar to majority but responses are weighted by capability score, historical accuracy, and model tier before counting.

### Pairwise

Each pair of worker responses is compared head-to-head by a judge model. The most expensive strategy but produces the most defensible ranking.

---

## The Arbiter

The arbiter receives a structured prompt containing the original task, all worker responses labeled by model, and computed weights. It produces structured output:

```json
{
  "consensus": "The synthesized answer...",
  "confidence": "high",
  "agreement_points": [
    "All models agree the loop terminates one iteration early"
  ],
  "disagreement_points": [
    "Models disagree on whether the fix should use <= or < + 1"
  ],
  "minority_insights": [
    "Only DeepSeek noted the potential integer overflow on line 42"
  ],
  "per_model_scores": {
    "sonnet": 0.85,
    "opus": 0.92,
    "gpt-4o": 0.78,
    "deepseek": 0.88,
    "qwen": 0.74
  }
}
```

---

## Consensus Caching

Identical consensus queries are cached in Redis with configurable TTL (default: 5 minutes for code, 30 minutes for research, 60 minutes for documentation). Cache key is SHA-256 of normalized task description, model list, and strategy.

---

## Disagreement Detection

When models strongly disagree (pairwise cosine similarity below 0.3), the system flags the disagreement rather than silently picking a winner. For high-stakes categories (security_audit, architecture), disagreements below 0.2 trigger automatic escalation with a larger worker pool.

---

## Default Worker Pool

| Model | Provider | Strength | Typical Role |
|-------|----------|----------|--------------|
| claude-sonnet | Anthropic | Balanced reasoning, strong code | Primary worker |
| claude-opus | Anthropic | Deep analysis, nuanced reasoning | Primary worker |
| claude-haiku | Anthropic | Fast, cost-effective, good at classification | Fast-path worker |
| gpt-4o | OpenRouter | Strong structured output, broad knowledge | Primary worker |
| gemini-2.0-flash | Google | Long context, fast inference | Long-context worker |
| llama-3.3-70b | Groq | Fast inference via LPU, open-weight reasoning | Speed worker |

Worker pool composition is subject to GA optimization, which evolves team compositions based on consensus quality metrics.

---

## When Consensus Fails

```
                     CONSENSUS FAILURE FALLBACK CHAIN

  Normal:       6 workers --> arbiter synthesis --> consensus response
                                    |
                              arbiter fails?
                                    |
  Fallback 1:   6 workers --> highest-weighted response (no synthesis)
                    |
              only 1 response?
                    |
  Fallback 2:   1 worker --> single response, confidence: low
                    |
              0 responses?
                    |
  Fallback 3:   retry with 2x timeout, different workers
                    |
              retry also fails?
                    |
  Error:        return error to caller
```

---

## Observability

Every consensus execution produces telemetry stored in PostgreSQL: execution records, per-worker results, arbiter decisions, and Thompson Sampling updates. This enables post-hoc analysis of consensus quality and identification of systematically underperforming models.
