---
title: FlossWare Architecture
---

# FlossWare Architecture

> **Current baseline · 2026-09-13**
>
> FlossWare is a collection of composable capabilities with **Loom as the AI-first execution substrate**. This page describes the current architecture. Older architecture material remains in Git history and the legacy documentation tree as historical evidence.

## The core model

Loom turns declarative intent into verified change while deliberately keeping its core small.

```text
Intent
  -> Arbiter
      -> Worker
      -> Worker
      -> Arbiter
  -> evidence
  -> evaluation
  -> result
```

`Worker` is the fundamental executable abstraction. An `Arbiter` is a Worker that coordinates Workers, so recursive composition is ordinary rather than a special workflow primitive.

External clients such as Crush, Claude Code, Codex, and Cursor consume Loom through external interfaces. They are **not** Loom Workers merely because they can invoke Loom.

## Current boundaries

| Repository / capability | Responsibility |
|---|---|
| `loom-ai` | Intent, Worker/Arbiter execution, explicit execution state, evidence, task-level orchestration, stable integration points |
| `loom-setup` | Installation and runtime setup |
| `loom-client-setup` | External-client configuration and integration |
| `model-gateway` | Provider/model invocation, resources, accounts, credentials, feasibility, selection, usage/cost, provenance, prompt caching |
| `evaluation` | Evaluation and reward implementations |
| `strategy` | Interchangeable decision and optimization strategies |
| `consensus` | Multi-model consensus |
| `budget` | Budget and cost controls |
| `storage`, `retrieval`, `rag` | Storage and retrieval capabilities |
| `scraping` | Discovery and resource acquisition |
| `chunking` | Provenance-preserving chunk derivation |
| `structured-output`, `cache`, `conversation`, `streaming` | Dedicated cross-cutting capabilities |
| `observability`, `resilience`, `security` | Operational and safety boundaries |
| `curses-tui`, `tui-schema` | Reusable terminal interaction and canonical TUI schema |
| `genetic-optimizer` | Genetic optimization capability |

Loom composes these capabilities. It does not absorb their implementations merely because it can call them.

## Model selection

```text
request
  -> candidates
  -> hard constraints
  -> feasible resources / models
  -> selection strategy
  -> invocation
  -> usage / cost / latency / outcome
  -> evaluation / reward
  -> strategy / knowledge update
```

Hard authorization, policy, capability, budget, quota, rate, and availability constraints are authoritative. Adaptive strategies operate only inside the feasible set.

## Knowledge and acquisition

Knowledge acquisition is separate from Loom execution:

```text
Discovery -> URI set -> Acquisition -> local corpus
                                  -> extraction / normalization
                                  -> chunking
                                  -> embedding / indexing / retrieval
```

Raw acquisition artifacts remain preserved. Content is content-addressed and source provenance is retained. Chunks are derived artifacts whose provenance and structured metadata remain separate from canonical chunk text.

## Architectural rules

1. Do not create a new primitive merely because an external product or algorithm uses one.
2. First locate behavior in an existing Worker, Arbiter, Strategy, Evaluator, Gateway, capability, or persistence boundary.
3. Keep capability implementations behind stable contracts.
4. Apply hard constraints before learned optimization.
5. Preserve provenance across acquisition, derivation, execution, evaluation, and retrieval.
6. Keep external clients outside Loom's internal execution model.
7. Preserve historical repositories as evidence without treating them as current architecture.
8. Promote ideas into durable decisions through implementation and executable dogfood evidence.

## Where the knowledge lives

The FlossWare knowledge base is Markdown and Git-backed so it can be authored locally in Obsidian and published through GitHub Pages. The public site is a presentation of that same knowledge, not a second manually maintained documentation corpus.

GitHub remains the executable source of truth for software, contracts, tests, and implementation details. The knowledge vault provides the human-readable architectural layer.

## Evidence

- [Repository boundaries](repositories.html)
- [Engineering principles](principles.html)
- [Dogfooding](dogfooding.html)
- [FlossWare GitHub organization](https://github.com/FlossWare)
