---
title: Consensus Engine
---

# Consensus Engine

Consensus is a **Loom capability**, not the definition of the FlossWare architecture and not a requirement for every request.

Loom can fan a task out to multiple compatible inference implementations, collect independent results, and synthesize or evaluate them. The exact number of models, providers, workers, arbiter, strategy, and storage backend are deployment choices behind contracts.

## Conceptual flow

```text
                         Task
                          |
                          v
                 Capability / policy
                          |
                          v
                   Model routing
                    /    |    \
                   /     |     \
              Model A  Model B  Model C ...
                   \     |     /
                    \    |    /
                     v   v   v
                   Results / evidence
                          |
                          v
              Synthesis / verification
                          |
                          v
                       Result
```

Consensus is useful when independent evidence can improve the result. It is unnecessary overhead when a deterministic check, a single capable model, or another verification mechanism is stronger and cheaper.

## Strategies

A consensus implementation may provide strategies such as:

- **Rotating arbiter** for distributing synthesis across compatible models.
- **Single arbiter** for consistent synthesis.
- **Majority** for tasks where semantic agreement is meaningful.
- **Weighted** when historical capability evidence should influence aggregation.
- **Pairwise** when explicit comparison between candidate responses is valuable.
- **Custom** strategies for domain-specific verification.

These are implementations of the consensus capability, not organization-wide defaults.

## Verification and disagreement

Consensus should preserve disagreement rather than silently converting disagreement into confidence. Useful outputs may include:

- synthesized response;
- per-response evidence or scores;
- agreement points;
- disagreement points;
- minority insights;
- confidence or escalation state.

Agreement is evidence, not proof. Multiple models can share the same blind spot, especially when they share provider, training-data, architecture, or prompt biases.

For high-stakes work, deterministic tests, independent verification, or human review may be more appropriate than model consensus alone.

## Routing and adaptive selection

A consensus implementation may use fixed selection, capability-aware routing, fallback policies, or adaptive selection such as Thompson Sampling. Genetic optimization may also be used to evaluate team composition or strategy configurations when a reliable fitness signal exists.

The architecture does **not** require a pool of 200+ models, a six-model panel, a particular provider mix, or a particular worker count.

## Caching

Consensus results may be cached when requests are safely reusable. Cache behavior belongs to the selected storage/cache implementation and should be governed by task semantics, model configuration, and freshness requirements.

## Historical implementation

Earlier FlossWare deployments used a six-worker consensus pattern with a large provider/model pool and a fixed physical fleet. That design is historical. It should not be copied into new documentation as a universal Loom requirement.

The historical implementation remains useful as an example of one possible deployment and as evidence for operational lessons. Current implementations should depend on Loom contracts instead of the old fleet topology.

## Current source of truth

- [Loom Architecture](loom.md)
- [Orchestration Layer](orchestration.md)
- [Model Routing](routing.md)
- [Design Philosophy](../philosophy.md)
- [Engineering Standards](https://github.com/FlossWare/engineering-standards)
