---
title: Thompson Sampling
---

# Thompson Sampling

Thompson Sampling is the online decision-making algorithm that selects which strategy to use for each incoming request. While [genetic algorithms](genetic_algorithms.md) evolve configurations offline, Thompson Sampling makes real-time choices by balancing exploration of uncertain options with exploitation of known good ones.

## The Explore/Exploit Tradeoff

Every time the system receives a request, it must choose a strategy (model routing, prompt template, team composition, etc.). The fundamental tension is:

- **Exploit** -- Use the strategy that has performed best so far. This maximizes short-term quality but risks missing better alternatives.
- **Explore** -- Try a less-proven strategy to gather more information about it. This sacrifices short-term quality for long-term improvement.

Thompson Sampling resolves this tradeoff elegantly: strategies with uncertain performance are naturally explored (because their sampled values vary widely), while strategies with proven performance are naturally exploited (because their sampled values are consistently high).

## How It Works

### Bayesian State: The Beta Distribution

Each strategy arm maintains a Beta distribution parameterized by two values:

- **alpha** -- Count of successes (quality score above threshold) plus 1
- **beta** -- Count of failures (quality score below threshold) plus 1

The Beta distribution represents the system's belief about the true success probability of each strategy. Initially, alpha=1 and beta=1, which is a uniform distribution (complete uncertainty). As outcomes are observed, the distribution narrows around the true success rate.

```python
# Each strategy arm maintains:
{
    "strategy_id": "model-routing-v3",
    "alpha": 47,          # 46 successes + 1 prior
    "beta": 12,           # 11 failures + 1 prior
    "total_trials": 57,
    "estimated_success_rate": 0.80   # alpha / (alpha + beta)
}
```

### Selection Process

For each request, Thompson Sampling works in three steps:

1. **Sample** -- Draw a random value from each strategy's Beta distribution.
2. **Select** -- Pick the strategy whose sampled value is highest.
3. **Update** -- After execution, observe the outcome and increment alpha (success) or beta (failure).

**Example with 3 strategies:**

```
Strategy A: Beta(alpha=47, beta=12)  -- well-tested, ~80% success
Strategy B: Beta(alpha=8,  beta=3)   -- less tested, ~73% success
Strategy C: Beta(alpha=2,  beta=2)   -- barely tested, ~50% success

Round 1 samples:
  A draws: 0.82   <-- consistent (narrow distribution)
  B draws: 0.69   <-- moderate variance
  C draws: 0.91   <-- high variance (wide distribution)
  Winner: C (0.91) -- exploration! C is uncertain, so its sample swings high

Round 2 samples:
  A draws: 0.78
  B draws: 0.81
  C draws: 0.35   <-- C swung low this time (still uncertain)
  Winner: B (0.81) -- mild exploration of B

Round 3 samples:
  A draws: 0.83
  B draws: 0.58
  C draws: 0.44
  Winner: A (0.83) -- exploitation of the proven strategy
```

### Convergence Behavior

Over time, successful strategies accumulate alpha, and their distributions narrow. Failed strategies accumulate beta. The visual effect:

```
Early (few observations):

Strategy A  Beta(2,2)     ___/\___        Wide -- explores often
                         /        \
                        /          \
                     --/            \--
                     0.0    0.5    1.0

Strategy B  Beta(3,2)       __/\_         Slightly right-shifted
                          /      \
                         /        \
                      --/          \--
                     0.0    0.5    1.0


Late (many observations):

Strategy A  Beta(47,12)         |/\|      Narrow peak at ~0.80
                                / \       Rarely samples below 0.70
                             --/   \--    Almost always exploited
                     0.0    0.5    1.0

Strategy B  Beta(8,22)    |/\|            Narrow peak at ~0.27
                          / \             Clearly inferior
                       --/   \--          Rarely selected
                     0.0    0.5    1.0
```

The key insight: Thompson Sampling **automatically** transitions from exploration to exploitation as evidence accumulates. No manual tuning of exploration parameters is needed.

## Why Thompson Sampling

| Algorithm | Mechanism | Exploration Control | Adapts to Change | Bayesian | Drawback |
|-----------|-----------|-------------------|------------------|----------|----------|
| Epsilon-Greedy | Exploit (1-e) of the time, random e of the time | Manual epsilon parameter | Only if epsilon > 0 | No | Wastes exploration budget on clearly bad arms |
| UCB1 | Pick arm with highest upper confidence bound | Automatic via confidence interval | Slowly (bound shrinks with all observations) | No | Overly optimistic; slow to abandon bad arms |
| Exp3 | Maintain probability weights, mix with uniform | Manual gamma parameter | Yes (weights decay) | No | Designed for adversarial settings; higher regret in stochastic ones |
| **Thompson Sampling** | **Sample from posterior, pick highest** | **Automatic via posterior width** | **Yes (can reset priors)** | **Yes** | **Requires conjugate prior (Beta for Bernoulli)** |

Thompson Sampling is the best fit because:

1. **No tuning** -- Epsilon-greedy requires choosing epsilon. UCB1 requires a confidence parameter. Thompson Sampling has no hyperparameters.
2. **Bayesian uncertainty** -- The posterior naturally encodes how much we know about each arm. New arms get explored; proven arms get exploited.
3. **Adaptivity** -- When the environment changes (new models added, existing models degraded), priors can be decayed or reset to re-enable exploration.
4. **Theoretical guarantees** -- Thompson Sampling achieves near-optimal regret bounds in practice, competitive with UCB1 and better than epsilon-greedy.

## Strategy Arms

Each arm in the Thompson Sampling bandit represents a complete strategy configuration. Arms have five dimensions:

| Dimension | Example Values | Source |
|-----------|---------------|--------|
| Model routing | "opus-primary-sonnet-fallback", "gemini-primary-haiku-fallback" | GA Model Routing Optimizer |
| Prompt template | "structured-cot-3shot", "minimal-0shot-json" | GA Prompt Template Optimizer |
| Team composition | ["opus","gemini","llama-70b","sonnet"] | GA Team Composition Optimizer |
| Retry policy | "exponential-3x-1000ms", "linear-5x-500ms" | GA Workflow Configuration Optimizer |
| RAG parameters | {chunk_size:512, top_k:10, threshold:0.6} | GA RAG Retrieval Optimizer |

### Manual Registration

New strategies can be registered manually via the API:

```bash
curl -X POST http://aio-01:5000/strategies/register \
  -H "Content-Type: application/json" \
  -d '{
    "strategy_id": "custom-high-quality",
    "description": "Opus-only with deep CoT and 5-shot examples",
    "model_routing": "opus-only",
    "prompt_template": "detailed-cot-5shot",
    "team_composition": ["opus", "opus", "opus"],
    "retry_policy": "exponential-3x-2000ms",
    "rag_params": {"chunk_size": 1024, "top_k": 20, "threshold": 0.7},
    "initial_alpha": 1,
    "initial_beta": 1
  }'
```

The strategy starts with a uniform prior (alpha=1, beta=1) and will be explored naturally by Thompson Sampling.

### GA Evolution Channel

The more common path is automatic registration from the [genetic algorithm](genetic_algorithms.md) pipeline:

1. The GA evolves a population of configurations offline.
2. Elite chromosomes (top 10-50%) from the final generation are extracted.
3. Each elite is registered as a new Thompson Sampling arm with a uniform prior.
4. Thompson Sampling explores the new arms alongside existing ones.
5. Arms that perform well accumulate alpha; poorly performing ones accumulate beta and are eventually abandoned.

## Integration with Genetic Algorithms

Thompson Sampling and GA are complementary systems:

| Aspect | Thompson Sampling | Genetic Algorithm |
|--------|-------------------|-------------------|
| Operates | Online, per-request | Offline, batch |
| Timescale | Milliseconds (selection) | Hours (evolution) |
| Purpose | Choose best known strategy | Discover new strategies |
| Input | Current strategy pool | Historical execution data |
| Output | Strategy selection for this request | New elite configurations |
| Exploration | Bayesian (posterior sampling) | Mutation + crossover |
| Persistence | Beta parameters in PostgreSQL | Population state in PostgreSQL |

The integration flow:

```
+----------------+     register new     +-------------------+
| GA Optimizer   | ----- arms --------> | Thompson Sampling |
| (runs every    |                      | (runs per request)|
|  few hours)    |                      |                   |
+-------+--------+                      +--------+----------+
        ^                                        |
        |                                        | select strategy
        |                                        v
        |                               +--------+----------+
        +--- reads execution data ------|  Execution Engine |
             from PostgreSQL            | (production runs) |
                                        +-------------------+
                                                 |
                                                 v
                                        +--------+----------+
                                        | PostgreSQL        |
                                        | - execution_summary|
                                        | - strategy_perf   |
                                        | - costs.entries   |
                                        +-------------------+
```

## Performance Tracking

Strategy performance is stored in the `learning.strategy_performance` table:

```sql
-- View current strategy rankings
SELECT
    strategy_id,
    alpha,
    beta,
    total_trials,
    ROUND(alpha::numeric / (alpha + beta), 3) AS success_rate,
    ROUND(avg_quality, 3) AS avg_quality,
    ROUND(avg_cost, 4) AS avg_cost,
    last_selected_at
FROM learning.strategy_performance
ORDER BY alpha::numeric / (alpha + beta) DESC
LIMIT 10;
```

```
 strategy_id              | alpha | beta | trials | success_rate | avg_quality | avg_cost | last_selected_at
--------------------------+-------+------+--------+--------------+-------------+----------+---------------------
 model-routing-v7-elite   |   142 |   28 |    169 |        0.835 |       0.871 |   0.0023 | 2026-08-05 11:42:00
 hybrid-opus-gemini-v3    |    98 |   23 |    120 |        0.810 |       0.845 |   0.0031 | 2026-08-05 11:41:55
 balanced-cost-quality-v2 |    67 |   19 |     85 |        0.779 |       0.812 |   0.0012 | 2026-08-05 11:40:30
 custom-high-quality      |    12 |    5 |     16 |        0.706 |       0.890 |   0.0089 | 2026-08-05 11:38:00
 fast-haiku-only          |    34 |   41 |     74 |        0.453 |       0.624 |   0.0004 | 2026-08-05 10:15:00
```

```sql
-- Track performance over time for a specific strategy
SELECT
    date_trunc('hour', recorded_at) AS hour,
    SUM(CASE WHEN outcome = 'success' THEN 1 ELSE 0 END) AS successes,
    SUM(CASE WHEN outcome = 'failure' THEN 1 ELSE 0 END) AS failures,
    ROUND(AVG(quality_score), 3) AS avg_quality
FROM learning.strategy_outcomes
WHERE strategy_id = 'model-routing-v7-elite'
  AND recorded_at > NOW() - INTERVAL '24 hours'
GROUP BY hour
ORDER BY hour;
```

```sql
-- Find strategies that may need retirement (high beta, low selection)
SELECT
    strategy_id,
    alpha,
    beta,
    total_trials,
    ROUND(alpha::numeric / (alpha + beta), 3) AS success_rate,
    last_selected_at,
    AGE(NOW(), last_selected_at) AS time_since_last_use
FROM learning.strategy_performance
WHERE alpha::numeric / (alpha + beta) < 0.3
  AND total_trials > 20
ORDER BY success_rate ASC;
```

## Lifecycle of a Strategy

Every strategy arm passes through five stages:

### 1. Created

A new strategy is registered, either manually via the API or automatically from a GA elite chromosome. It starts with a uniform prior: `Beta(alpha=1, beta=1)`.

```
State: alpha=1, beta=1, trials=0
Behavior: Will be selected frequently due to high uncertainty
Duration: 1-10 trials
```

### 2. Exploring

The strategy has been selected a handful of times. Its posterior is beginning to take shape but is still wide. Thompson Sampling will select it with moderate frequency.

```
State: alpha=5-15, beta=2-10, trials=10-25
Behavior: Selected often enough to gather meaningful data
Duration: 10-50 trials
Transition: Once posterior narrows sufficiently (variance < threshold)
```

### 3. Converging

The strategy's performance is becoming clear. The posterior distribution is narrowing, and the success rate estimate is stabilizing. The strategy is either proving itself or being outcompeted.

```
State: alpha=20-50, beta=5-30, trials=30-80
Behavior: Selection frequency reflects relative quality
Duration: 20-100 trials
Transition: Success rate stabilizes within +/- 0.05 for 20+ trials
```

### 4a. Exploited (if successful)

The strategy has proven itself with a high success rate. Its posterior is narrow and peaked to the right. Thompson Sampling selects it frequently -- it is the workhorse.

```
State: alpha=50+, beta=10-20, trials=60+
Behavior: Selected for majority of requests
Duration: Indefinite (until environment changes)
Risk: May dominate and reduce diversity -- monitor with feedback loop optimizer
```

### 4b. Abandoned (if unsuccessful)

The strategy has accumulated too many failures. Its posterior is narrow and peaked to the left. Thompson Sampling almost never selects it.

```
State: alpha=5-15, beta=30+, trials=40+
Behavior: Almost never selected (sampled values consistently low)
Duration: Until retired or environment changes
Recovery: Prior decay can re-enable exploration if conditions change
```

### 5. Retired

A strategy is explicitly removed from the active pool. This happens when:

- It has been abandoned for an extended period (no selections in 7+ days).
- A GA optimizer produces a strictly superior replacement.
- The strategy's parameters reference a model or configuration that is no longer available.

```sql
-- Retire a strategy
UPDATE learning.strategy_performance
SET status = 'retired', retired_at = NOW(), retired_reason = 'superseded by v8-elite'
WHERE strategy_id = 'model-routing-v3-elite';
```

Retired strategies are not deleted -- they remain in the database for historical analysis and can be reactivated if needed.
