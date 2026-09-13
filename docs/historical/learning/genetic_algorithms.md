---
title: Genetic Algorithm Evolution
---

# Genetic Algorithm Evolution

The orchestration framework uses **genetic algorithms (GA)** to evolve optimal configurations for routing, retrieval, team composition, and more. Rather than hand-tuning parameters, the system breeds populations of candidate configurations, selects the fittest, and iterates until convergence.

## Core Concept

```
                    +-----------+
                    | Population|
                    | (N=20-30) |
                    +-----+-----+
                          |
                    +-----v-----+
                    | Evaluate  |
                    | Fitness   |
                    +-----+-----+
                          |
              +-----------+-----------+
              |                       |
        +-----v-----+          +-----v-----+
        |   Elite   |          | Selected  |
        | (top 10%) |          | Parents   |
        +-----+-----+          +-----+-----+
              |                       |
              |                 +-----v-----+
              |                 | Crossover |
              |                 +-----+-----+
              |                       |
              |                 +-----v-----+
              |                 |  Mutate   |
              |                 +-----+-----+
              |                       |
              +-----------+-----------+
                          |
                    +-----v-----+
                    |    New    |
                    |Population |
                    +-----------+
```

Elite individuals pass through unchanged. The rest are bred through crossover and mutation to produce the next generation.

## The GA Loop

Every genetic algorithm in the framework follows the same seven-step cycle:

1. **Initialize** -- Generate a random population of N chromosomes, each encoding a candidate configuration as a set of genes.
2. **Evaluate** -- Score every chromosome using a domain-specific fitness function. Fitness is computed from real execution data stored in PostgreSQL.
3. **Select** -- Choose parents for the next generation via tournament selection (k=3). Higher-fitness individuals are more likely to be selected.
4. **Crossover** -- Combine pairs of parents gene-by-gene (uniform crossover) to produce offspring that inherit traits from both.
5. **Mutate** -- With a small per-gene probability (10-15%), randomly perturb individual genes to introduce diversity.
6. **Elitism** -- Copy the top 10-50% of the current generation directly into the next, ensuring the best solutions are never lost.
7. **Repeat** -- Go to step 2. Continue until a convergence criterion is met (fitness plateau or generation limit).

```
Initialize -> Evaluate -> Select -> Crossover -> Mutate -> Elitism -> Evaluate -> ...
     |                                                                     |
     +--- generation 0                                    generation N ----+
```

## FleetChromosome

Every optimizable configuration is encoded as a `FleetChromosome` dataclass with six genes:

```python
@dataclass
class FleetChromosome:
    """A single candidate configuration in the GA population."""
    
    model_weights: dict[str, float]     # Per-model routing weights (0.0-1.0)
    retry_strategy: str                 # "exponential", "linear", "fixed"
    timeout_ms: int                     # Request timeout in milliseconds (500-10000)
    parallelism: int                    # Max concurrent workers (1-9)
    temperature: float                  # LLM temperature for this config (0.0-2.0)
    chunk_strategy: dict                # RAG chunking params (size, overlap, method)
    
    fitness: float = 0.0               # Computed fitness score
    generation: int = 0                 # Which generation this was created in
    parent_ids: list[str] = field(      # Lineage tracking
        default_factory=list
    )
```

Each gene has a defined range and mutation operator. The `fitness` field is populated during evaluation and used for selection.

## The Seven Optimizers

### 1. Model Routing Optimizer

Evolves the optimal mapping of task types to AI models.

**Fitness function:**

```
fitness = quality * 0.6 - cost * 0.2 - latency * 0.2
```

- **quality** -- Average response quality score (0.0-1.0) from arbiter evaluations stored in `workflow.arbiter_decisions`.
- **cost** -- Normalized API cost per request from `costs.entries`.
- **latency** -- Normalized response time from `monitoring.execution_summary`.

The weights (0.6, 0.2, 0.2) reflect the system's bias toward quality over cost or speed. A chromosome that routes coding tasks to Opus and simple classification to Haiku would score higher than one that uses Opus for everything (quality similar, cost much lower).

**Genes:**

| Gene | Type | Range | Description |
|------|------|-------|-------------|
| `task_model_map` | dict | model pool | Maps task categories to preferred models |
| `fallback_chain` | list | model pool | Ordered fallback when primary model fails |
| `quality_threshold` | float | 0.5-0.95 | Minimum quality to accept a response |
| `cost_ceiling` | float | 0.0-0.01 | Maximum cost per request in USD |
| `retry_count` | int | 1-5 | Number of retries before fallback |
| `timeout_ms` | int | 1000-30000 | Per-request timeout |

### 2. RAG Retrieval Optimizer

Evolves chunking, embedding, and retrieval parameters for the search pipeline.

**Genes:**

| Gene | Type | Range | Description |
|------|------|-------|-------------|
| `chunk_size` | int | 128-2048 | Token count per chunk |
| `chunk_overlap` | int | 0-512 | Overlap between adjacent chunks |
| `top_k` | int | 3-50 | Number of chunks to retrieve |
| `similarity_threshold` | float | 0.3-0.9 | Minimum cosine similarity |
| `reranking_model` | str | model pool | Model used for reranking results |

**Fitness** is computed from retrieval precision and recall measured against known-good query-answer pairs stored in the knowledge base.

### 3. Team Composition Optimizer

Evolves which models should work together on multi-AI consensus tasks.

**Zero-overlap constraint:** No two team members may be the same model. This is enforced as a hard constraint -- chromosomes violating it receive a fitness of 0.0 regardless of other metrics.

```python
def evaluate_team(chromosome):
    team = chromosome.team_members  # e.g., ["opus", "sonnet", "gemini-pro", "llama-70b"]
    
    # Hard constraint: no duplicates
    if len(team) != len(set(team)):
        return 0.0
    
    # Fitness: diversity bonus + quality score
    diversity = model_family_diversity(team)   # Different providers = higher
    quality = avg_consensus_quality(team)       # From workflow.arbiter_decisions
    cost = avg_team_cost(team)                  # From costs.entries
    
    return quality * 0.5 + diversity * 0.3 - cost * 0.2
```

### 4. Training Data Curation Optimizer

Evolves selection criteria for which stored experiences are most valuable for future reference.

**Genes encode:** recency weight, diversity weight, quality threshold, deduplication similarity cutoff, category balance ratios.

**Fitness** is measured by the relevance of curated datasets to subsequent tasks -- does retrieving from the curated set produce better outcomes than retrieving from the uncurated archive?

### 5. Prompt Template Optimizer

Evolves the structure and parameters of prompt templates used across the system.

**Genes:**

| Gene | Type | Range | Description |
|------|------|-------|-------------|
| `system_prompt_style` | str | ["concise", "detailed", "structured", "minimal"] | System prompt verbosity |
| `example_count` | int | 0-5 | Number of few-shot examples to include |
| `chain_of_thought` | bool | true/false | Whether to request step-by-step reasoning |
| `output_format` | str | ["json", "markdown", "plain", "xml"] | Requested output structure |
| `temperature` | float | 0.0-1.5 | Sampling temperature |

**Fitness** is the average quality score of responses generated using the template configuration, as rated by arbiter consensus.

### 6. Adversarial Verification Optimizer

Evolves the parameters of the adversarial review layer that checks outputs for errors, hallucinations, and bias.

**Genes:**

| Gene | Type | Range | Description |
|------|------|-------|-------------|
| `reviewer_count` | int | 2-6 | Number of independent reviewers |
| `agreement_threshold` | float | 0.5-1.0 | Fraction that must agree to pass |
| `challenge_depth` | str | ["surface", "moderate", "deep"] | How aggressively to challenge claims |
| `bias_check_enabled` | bool | true/false | Whether to run explicit bias detection |

**Fitness** is based on the True Positive Rate (TPR) and False Positive Rate (FPR) of the verification layer:

```
fitness = TPR * 0.7 - FPR * 0.3
```

A high TPR means the layer catches real errors. A low FPR means it does not flag correct outputs as wrong. The 0.7/0.3 weighting reflects that missing errors is worse than false alarms.

### 7. Workflow Configuration Optimizer

Evolves end-to-end workflow parameters: how tasks are decomposed, parallelized, retried, and assembled.

**Genes:**

| Gene | Type | Range | Description |
|------|------|-------|-------------|
| `max_parallel_workers` | int | 1-9 | Concurrent worker limit |
| `decomposition_strategy` | str | ["atomic", "phased", "hierarchical"] | How to break down tasks |
| `retry_policy` | str | ["exponential", "linear", "fixed"] | Backoff strategy |
| `max_retries` | int | 1-10 | Retry limit per subtask |
| `consensus_quorum` | float | 0.5-1.0 | Fraction of workers that must agree |
| `timeout_multiplier` | float | 1.0-5.0 | Timeout scaling factor |

**Fitness** is a weighted combination:

```
fitness = success_rate * 0.4 + quality * 0.3 - duration * 0.2 - cost * 0.1
```

## Evolutionary Operators

### Tournament Selection (k=3)

To select a parent, pick k=3 individuals at random from the population and return the one with the highest fitness.

```
Population: [A(0.72), B(0.85), C(0.61), D(0.93), E(0.78), F(0.54)]

Tournament 1: randomly pick B(0.85), D(0.93), F(0.54) -> winner: D(0.93)
Tournament 2: randomly pick A(0.72), C(0.61), E(0.78) -> winner: E(0.78)

Parents for crossover: D and E
```

Tournament selection provides selection pressure (fitter individuals win more often) without requiring the population to be sorted. The parameter k controls pressure intensity -- higher k means stronger preference for the best.

### Uniform Crossover

Each gene is independently inherited from one parent or the other with equal probability.

```
Parent D: [opus,    exponential, 3000, 6, 0.7, {size:512}]
Parent E: [sonnet,  linear,      5000, 4, 0.3, {size:1024}]
Coin:     [heads,   tails,       heads, tails, heads, tails]
Child:    [opus,    linear,      3000, 4, 0.7, {size:1024}]
```

This gene-by-gene mixing allows offspring to combine the best traits of both parents. It is more disruptive than single-point crossover, which helps maintain diversity in small populations.

### Mutation

Each gene has an independent probability of being randomly perturbed.

```
Before mutation: [opus, linear, 3000, 4, 0.7, {size:1024}]
Mutation mask:   [no,   no,     YES,  no, YES, no          ]  (p=0.12 per gene)
After mutation:  [opus, linear, 4200, 4, 0.4, {size:1024}]
                                ^^^^     ^^^
                         3000 + random   0.7 + random
                         delta (-500     delta (-0.3)
                         to +2000)
```

- **Numeric genes** are perturbed by adding a random delta within a defined range.
- **Categorical genes** (e.g., retry_strategy) are replaced with a random valid value.
- **Boolean genes** are flipped.
- **Dict genes** have individual sub-keys mutated independently.

## Hyperparameters

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Population size | 20-30 | Large enough for diversity, small enough for fast evaluation |
| Mutation rate | 10-15% per gene | Balances exploration (new configs) with exploitation (refining good ones) |
| Elitism rate | 10-50% | Higher for stable domains, lower for rapidly changing ones |
| Tournament size (k) | 3 | Moderate selection pressure |
| Max generations | 50-100 | Convergence typically occurs by generation 30-50 |
| Crossover rate | 80-90% | Most offspring are crossovers; remainder are clones |
| Convergence threshold | 0.01 | Stop if best fitness improves less than this for 10 consecutive generations |
| Fitness evaluation | Real data | All fitness scores computed from production execution metrics |

## Real Data, Not Simulation

Every fitness evaluation queries real execution data from PostgreSQL. There are no synthetic benchmarks or simulated scores.

```
+-------------+       +------------------+       +----------------+
| GA Engine   | ----> | REST API         | ----> | PostgreSQL     |
| (evolve     |       | POST /ga/evolve  |       | execution_summary |
|  population)|       | GET /ga/fitness  |       | arbiter_decisions |
+-------------+       +------------------+       | costs.entries  |
                                                  +----------------+
                                                         |
                                                  +------v---------+
                                                  | Real execution |
                                                  | history from   |
                                                  | production     |
                                                  | workflows      |
                                                  +----------------+
```

When a chromosome is evaluated:

1. The GA engine sends the candidate configuration to the REST API.
2. The API queries PostgreSQL for recent execution results matching the chromosome's parameter space.
3. Quality, cost, latency, and success rates are aggregated from actual workflow runs.
4. The fitness score is returned to the GA engine.

This means GA evolution is grounded in reality -- configurations that perform well in production are the ones that survive.

## GA + Thompson Sampling Integration

Genetic algorithms and Thompson Sampling serve complementary roles:

- **GA (offline)** evolves populations of configurations over generations. It is computationally expensive but explores the parameter space thoroughly.
- **Thompson Sampling (online)** selects among a set of known strategies in real time using Bayesian probability. It is fast but only chooses from existing options.

The integration works as a cycle:

```
+------------------+        +---------------------+        +------------------+
| Genetic Algorithm|        | Thompson Sampling   |        | Execution Engine |
| (offline, batch) |        | (online, per-request|        | (production)     |
+--------+---------+        +----------+----------+        +--------+---------+
         |                             |                            |
         | 1. Evolve new              | 3. Select best             |
         |    configurations          |    strategy per            |
         |                            |    request                 |
         v                            v                            |
   +-----+------+              +------+-------+                    |
   | New elite  | -- 2. -----> | Strategy     | --- 4. route ----> |
   | configs    |   Register   | Pool (arms)  |    request         |
   +------------+   as new     +--------------+                    |
                    arms              ^                            |
                                      |                            |
                                      +---- 5. Record outcome ----+
                                             (success/failure,
                                              quality, cost)
```

1. The GA evolves populations offline, producing elite configurations.
2. Elite configurations are registered as new Thompson Sampling arms.
3. For each incoming request, Thompson Sampling selects the best strategy by sampling from Beta distributions.
4. The selected strategy's parameters are used to route and execute the request.
5. The execution outcome (quality, cost, latency) is recorded, updating the Beta distribution for that arm.
6. Periodically, the GA runs again using the accumulated execution data, potentially producing even better configurations.

## Convergence Tracking

The API exposes convergence metrics for each optimizer:

```bash
curl http://aio-01:5000/ga/convergence
```

```json
{
  "optimizers": {
    "model_routing": {
      "current_generation": 34,
      "best_fitness": 0.847,
      "fitness_history": [0.312, 0.445, 0.521, 0.634, 0.712, 0.789, 0.831, 0.847],
      "converged": false,
      "improvement_rate": 0.004,
      "population_diversity": 0.23,
      "elite_count": 5
    },
    "rag_retrieval": {
      "current_generation": 28,
      "best_fitness": 0.791,
      "fitness_history": [0.198, 0.334, 0.502, 0.651, 0.743, 0.788, 0.791],
      "converged": true,
      "improvement_rate": 0.001,
      "population_diversity": 0.08,
      "elite_count": 8
    }
  },
  "timestamp": "2026-08-05T12:00:00Z"
}
```

Key indicators:

- **improvement_rate** below the convergence threshold (0.01) for 10+ generations signals convergence.
- **population_diversity** approaching 0 means the population has homogenized -- consider increasing mutation rate or injecting random individuals.
- **converged: true** means the optimizer has stopped evolving and is using its best-known configuration.

## Why GA Over Grid/Random Search

| Criterion | Grid Search | Random Search | Genetic Algorithm |
|-----------|-------------|---------------|-------------------|
| Dimensions | Exponential blowup (curse of dimensionality) | Scales linearly but misses interactions | Handles high-dimensional spaces via crossover |
| Adaptivity | Static -- must predefine grid | Static -- no learning between samples | Adaptive -- each generation builds on the last |
| Interactions | Cannot discover gene interactions | Unlikely to find combinations | Crossover explicitly combines successful traits |
| Convergence | Evaluates every point (wasteful) | No convergence guarantee | Converges via selection pressure |
| Parallelism | Embarrassingly parallel but wasteful | Embarrassingly parallel but random | Population evaluation is parallel; selection is sequential |
| Prior knowledge | Requires knowing good ranges | Requires knowing good ranges | Can start with random ranges and discover good regions |
| Continuous improvement | One-shot | One-shot | Iterative -- gets better with more data |

For the orchestration framework, GA is the right choice because:

1. **High dimensionality** -- Each optimizer has 4-6 genes with continuous, discrete, and categorical types. Grid search over 6 dimensions with 10 values each requires 1,000,000 evaluations.
2. **Gene interactions** -- The best chunk_size depends on the similarity_threshold, which depends on the reranking_model. GA discovers these interactions through crossover.
3. **Changing landscape** -- As new models are added to the pool and usage patterns shift, the fitness landscape changes. GA continuously adapts; grid search would need to be re-run from scratch.
4. **Real-data evaluation cost** -- Each fitness evaluation queries production data. GA minimizes total evaluations needed by focusing search on promising regions.
