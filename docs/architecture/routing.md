---
title: Model Routing
---

# Model Routing

Model routing determines which LLMs handle a given task. The system maintains a pool of 200+ models across multiple providers and selects the best subset for each request using Thompson Sampling, capability scores, and task-aware heuristics. Routing is not random and not static -- it adapts continuously based on observed performance.

---

## Task-Aware Routing Flow

When a task arrives, the router considers the task category, available models, provider health, and historical performance to select the optimal set of workers.

```
                        TASK-AWARE ROUTING FLOW

  Incoming Task
    |
    v
  +------------------+
  | Task Classifier   |    Assigns category (code_review, research, etc.)
  +--------+---------+
           |
           v
  +------------------+
  | Provider Filter   |    Remove providers with:
  |                  |      - OPEN circuit breaker
  |                  |      - Rate limit >80% consumed
  |                  |      - Known outage (health check failed)
  +--------+---------+
           |
           v
  +------------------+
  | Capability Filter |    Remove models with:
  |                  |      - capability_score < 0.3 for this category
  |                  |      - context_length < task requirement
  |                  |      - Missing required features (e.g., tool use)
  +--------+---------+
           |
           v
  +------------------+
  | Thompson Sampling |    Sample from Beta(alpha, beta) for each
  |                  |    remaining model. Select top N by sample value.
  |                  |    N determined by blueprint (typically 4-6).
  +--------+---------+
           |
           v
  +------------------+
  | Diversity Check   |    Ensure selected set spans:
  |                  |      - At least 2 different providers
  |                  |      - At least 2 different model families
  |                  |      - No single provider >50% of selections
  +--------+---------+
           |
           v
  +------------------+
  | Worker Assignment |    Map models to healthy workers
  |                  |    Tier 1 for heavy tasks, distribute evenly
  +--------+---------+
           |
           v
  Selected Models dispatched to Workers
```

---

## Dynamic Capability Scores

Every model maintains a per-category capability score that reflects its historical performance. Scores are updated after every consensus execution using Exponentially Weighted Moving Average (EWMA).

### EWMA Formula

```
score_new = alpha * observation + (1 - alpha) * score_old

Where:
  alpha = 0.1 (smoothing factor, configurable)
  observation = arbiter's quality score for this model's response [0, 1]
  score_old = model's current capability score for this category
```

### Properties

- **alpha = 0.1**: Recent observations have moderate influence. The score adapts to changes over approximately 10-20 observations.
- **Initialization**: New models start with `score = 0.5` (neutral). This gives them a fair chance to prove themselves without dominating routing.
- **Per-category**: A model can have high capability for `code_review` (0.88) and low capability for `research` (0.45). Scores are independent across categories.
- **Decay**: Models that are not selected (due to provider outage, rate limits, etc.) do not have their scores decay. Scores only change when the model participates and is evaluated.

### Example

```
  Model: claude-sonnet, Category: code_review

  Current score: 0.82
  Consensus evaluation scores sonnet's response at 0.91

  score_new = 0.1 * 0.91 + 0.9 * 0.82
            = 0.091 + 0.738
            = 0.829

  Score moves from 0.82 to 0.829 (slight improvement)
```

Over many observations, the score converges to the model's true average quality for that category.

---

## Thompson Sampling

Thompson Sampling is a Bayesian approach to the multi-armed bandit problem. Each model is a "bandit arm" and the system must balance exploitation (selecting models known to be good) with exploration (trying less-tested models that might be better).

### Beta Distribution

Each model maintains a Beta(alpha, beta) distribution per task category.

```
  Beta Distribution for Model Selection

  alpha = number of successes + 1 (prior)
  beta  = number of failures + 1 (prior)

  Mean  = alpha / (alpha + beta)
  Var   = (alpha * beta) / ((alpha + beta)^2 * (alpha + beta + 1))
```

- **High alpha, low beta**: The model has succeeded often. The distribution is concentrated near 1.0. The model is likely to be selected.
- **Low alpha, high beta**: The model has failed often. The distribution is concentrated near 0.0. The model is unlikely to be selected.
- **Low alpha, low beta**: The model is undertested. The distribution is wide (high variance). Thompson Sampling will occasionally select it to gather more data.

### Selection Process

```
                    THOMPSON SAMPLING SELECTION

  For each candidate model M_i:
    1. Look up Beta(alpha_i, beta_i) for the task category
    2. Draw a random sample: theta_i ~ Beta(alpha_i, beta_i)

  Sort all models by theta_i (descending)
  Select top N models

  Example (selecting 6 from 10 candidates):

    Model         alpha  beta   Sample (theta)   Selected?
    -----         -----  ----   --------------   ---------
    claude-opus     45    8      0.87             Yes (1)
    gpt-4o          38   12      0.82             Yes (2)
    claude-sonnet   42   10      0.79             Yes (3)
    deepseek-v3     28    9      0.77             Yes (4)
    gemini-flash    22    7      0.71             Yes (5)
    llama-3.3-70b   18    6      0.69             Yes (6)
    qwen-2.5-72b   15   11      0.62             No
    mistral-large   12    8      0.58             No
    phi-4           5     3      0.55             No
    command-r+      8    14      0.41             No

  Note: phi-4 has low alpha+beta (undertested). Its high
  variance means it will occasionally sample high and get
  selected -- this is exploration working as intended.
```

### Convergence

Over hundreds of executions, Thompson Sampling converges toward the optimal model set for each task category. The convergence rate depends on the variance in model quality:

- **Clear winners**: When one model is significantly better, convergence happens in 20-30 executions.
- **Close competition**: When multiple models are similarly capable, the system continues to explore among them indefinitely (which is desirable -- it maintains diversity).
- **Seasonal shifts**: When a provider updates their model (e.g., GPT-4o gets an update), the new performance characteristics are absorbed within 10-20 executions as the EWMA and Thompson Sampling parameters adjust.

### Feedback Update Rules

After each consensus execution:

```
For each participating model:
  If arbiter_score >= 0.7:     alpha += 1  (success)
  If arbiter_score < 0.4:      beta += 1   (failure)
  If 0.4 <= arbiter_score < 0.7: no update (neutral)
  If timeout:                   beta += 2   (strong failure)
  If error (non-timeout):       beta += 1   (failure)
```

The threshold values (0.7, 0.4) and increment sizes are configurable. The double penalty for timeouts reflects the operational cost: a timeout consumes a worker slot and delays the response without contributing to consensus.

---

## Fallback Chains

When the primary model selection fails (provider down, rate limited, all models filtered out), the router follows a fallback chain specific to each task category.

| Category | Primary Provider | Fallback 1 | Fallback 2 | Fallback 3 |
|----------|-----------------|------------|------------|------------|
| code_review | Anthropic (Sonnet) | OpenRouter (GPT-4o) | Google (Gemini) | Groq (Llama) |
| security_audit | Anthropic (Opus) | OpenRouter (GPT-4o) | Anthropic (Sonnet) | Google (Gemini) |
| architecture | Anthropic (Opus) | OpenRouter (GPT-4o) | Google (Gemini) | DeepSeek (V3) |
| implementation | Anthropic (Sonnet) | DeepSeek (V3) | OpenRouter (GPT-4o) | Groq (Llama) |
| research | Google (Gemini) | Anthropic (Opus) | OpenRouter (GPT-4o) | DeepSeek (V3) |
| debugging | Anthropic (Sonnet) | OpenRouter (GPT-4o) | DeepSeek (V3) | Groq (Llama) |
| documentation | Anthropic (Haiku) | Groq (Llama) | Google (Gemini) | OpenRouter (Mistral) |
| general | Anthropic (Sonnet) | OpenRouter (GPT-4o) | Google (Gemini) | Groq (Llama) |

Fallback is provider-level, not model-level. If Anthropic is down, all Anthropic models are skipped and the next provider in the chain is tried.

---

## Model Capability Matrix

Each model has a capability profile that the router uses for task-aware selection. Capabilities are initialized from known model specifications and refined by observed performance.

| Model | Code | Reasoning | Long Context | Speed | Tool Use | Structured Output |
|-------|------|-----------|-------------|-------|----------|-------------------|
| claude-opus | 0.95 | 0.97 | 0.85 | 0.40 | 0.95 | 0.90 |
| claude-sonnet | 0.92 | 0.88 | 0.80 | 0.70 | 0.92 | 0.88 |
| claude-haiku | 0.75 | 0.70 | 0.75 | 0.95 | 0.85 | 0.85 |
| claude-fable | 0.90 | 0.92 | 0.80 | 0.65 | 0.90 | 0.88 |
| gpt-4o | 0.90 | 0.90 | 0.80 | 0.65 | 0.90 | 0.95 |
| gemini-2.0-flash | 0.80 | 0.82 | 0.95 | 0.90 | 0.80 | 0.82 |
| llama-3.3-70b | 0.82 | 0.80 | 0.70 | 0.95 | 0.60 | 0.70 |
| deepseek-v3 | 0.88 | 0.85 | 0.80 | 0.70 | 0.75 | 0.80 |
| qwen-2.5-72b | 0.85 | 0.83 | 0.75 | 0.65 | 0.70 | 0.78 |
| mistral-large | 0.82 | 0.80 | 0.70 | 0.70 | 0.75 | 0.80 |
| command-r+ | 0.75 | 0.78 | 0.85 | 0.60 | 0.80 | 0.75 |

Scores range from 0 to 1. These are initial values; observed performance overwrites them via EWMA within 20-30 executions.

### How Capabilities Affect Routing

The router uses capabilities as hard filters before Thompson Sampling:

- **Code tasks** (code_review, implementation, debugging): Require `code >= 0.7`.
- **Research tasks**: Require `long_context >= 0.7` and `reasoning >= 0.7`.
- **Fast-path tasks** (classification, summarization): Require `speed >= 0.8`.
- **Tool-using tasks**: Require `tool_use >= 0.8`.
- **Structured output tasks** (data_analysis, configuration): Require `structured_output >= 0.8`.

Models that fail any hard filter for a task category are excluded from Thompson Sampling entirely. This prevents a model with high general capability but low code capability from being selected for code review.

---

## How New Models Enter the Pool

When a new model is discovered (via daily automated provider scans or manual addition), it enters the routing pool through a 5-step onboarding process.

### Step 1: Discovery

The daily model discovery job scans all configured providers for newly available models. It queries provider-specific endpoints:

- **OpenRouter**: `GET /api/v1/models` -- lists all available models with pricing, context length, and capabilities.
- **Google**: Vertex AI model catalog.
- **Groq**: Model listing API.
- **Cerebras**: Model listing API.

New models are logged to `monitoring.model_discovery` with their specifications.

### Step 2: Initialization

The new model is added to the routing pool with neutral parameters:

```
capability_score = 0.5 (per category)
thompson_alpha = 1 (prior)
thompson_beta = 1 (prior)
status = "probationary"
```

The neutral initialization ensures the model gets a fair chance: Beta(1, 1) is a uniform distribution, meaning Thompson Sampling will select it with moderate frequency.

### Step 3: Probationary Period

During the first 20 consensus executions involving this model, it is flagged as `probationary`. Its responses are included in consensus but its arbiter scores are logged separately for review. If its average score falls below 0.3 during probation, it is automatically deprioritized (see below).

### Step 4: Graduation

After 20 successful executions (arbiter_score >= 0.4), the model graduates to `active` status. Its Thompson Sampling parameters now reflect real performance data rather than priors.

### Step 5: Ongoing Evaluation

Active models are continuously evaluated through Thompson Sampling feedback. There is no fixed re-evaluation schedule -- every consensus execution in which the model participates updates its parameters. Models can improve or degrade over time as providers update their models or as the task mix changes.

```
             MODEL LIFECYCLE

  Discovery --> Initialization --> Probation (20 execs)
                                      |
                              avg score >= 0.4?
                                     / \
                                   yes   no
                                   /       \
                                  v         v
                              Active    Deprioritized
                                |           |
                          Ongoing eval  Periodic retry
                                |       (every 50 execs)
                          score drops       |
                          below 0.3?    score >= 0.4?
                                |           |
                                v           v
                          Deprioritized  Active
```

---

## How Underperforming Models Get Deprioritized

Models are deprioritized (not removed) when they consistently underperform. Deprioritization is a soft mechanism: the model remains in the pool but is unlikely to be selected by Thompson Sampling.

### Deprioritization Triggers

A model is deprioritized when any of the following conditions are met:

1. **Low capability score**: EWMA capability score drops below 0.3 for a task category. The model is deprioritized for that specific category only.
2. **High timeout rate**: More than 30% of the model's last 20 executions resulted in timeouts. This suggests the provider is unreliable for this model.
3. **Probation failure**: Average arbiter score below 0.3 during the 20-execution probationary period.
4. **Provider instability**: The provider's circuit breaker has been OPEN more than 50% of the time in the last 24 hours.

### What Deprioritization Means

- The model's Thompson Sampling parameters are set to Beta(1, 10), which concentrates the distribution near 0 and makes selection extremely unlikely.
- The model is NOT removed from the pool. It can still be selected on rare occasions when Thompson Sampling draws a high sample from Beta(1, 10) (approximately 1 in 50 draws). This preserves the ability to detect recovery.
- Every 50 consensus executions, one slot is reserved for a deprioritized model to give it a chance to re-prove itself. If it scores above 0.4, its parameters are reset to Beta(2, 2) and it re-enters active evaluation.

### Model Removal (Rare)

Models are permanently removed from the pool only when:

- The provider discontinues the model (404 on model endpoint).
- The model's API key is revoked or expired.
- Manual removal by the operator.

Automatic deprioritization is strongly preferred over removal because providers frequently update their models, and a model that underperforms today may improve tomorrow.

---

## Quality-First Routing

The routing system prioritizes quality over cost and speed. This is a deliberate design decision that reflects the system's purpose: producing higher-quality answers than any single model could provide alone.

### What Quality-First Means in Practice

1. **No cost-based routing by default**: The router does not prefer cheaper models. A free model on OpenRouter and a paid model on Anthropic are evaluated purely on capability score and Thompson Sampling parameters. Cost optimization is available as an explicit strategy (`CostOptimized`) but is not the default.

2. **No speed-based routing by default**: Faster models do not get routing preference unless the task category requires speed (e.g., `classification`). A 30-second response from Opus that scores 0.92 is preferred over a 2-second response from Haiku that scores 0.71.

3. **Diversity over convergence**: The diversity check in the routing flow prevents Thompson Sampling from converging on a single "best" model. Even if Opus dominates, the router ensures at least 2 providers and 2 model families are represented. This catches cases where the "best" model has systematic blind spots.

4. **Exploration budget**: Approximately 10-15% of routing decisions are exploratory (Thompson Sampling selecting a less-proven model). This exploration is not waste -- it is the mechanism by which the system discovers that a new model outperforms the current favorites.

### Strategy Override

Callers can override quality-first routing by specifying a strategy in the request:

| Strategy | Behavior |
|----------|----------|
| `QualityFirst` (default) | Select by capability score and Thompson Sampling, ignore cost |
| `CostOptimized` | Prefer free-tier models; only use paid models when free alternatives score below 0.5 |
| `Balanced` | Weight capability (60%), cost (20%), speed (20%) |
| `SpeedFirst` | Select by speed capability, minimum quality threshold of 0.6 |
| `MaxDiversity` | Maximize provider and model family diversity, minimum 4 different providers |

All strategies still enforce the minimum capability filter (capability_score >= 0.3). No strategy will route to a model with a capability score below this threshold.
