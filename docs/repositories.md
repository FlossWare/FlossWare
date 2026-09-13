---
title: Repository Boundaries
---

# Repository Boundaries

> **Current boundary map · 2026-09-13**

FlossWare uses multiple repositories deliberately. Repository boundaries are architectural constraints, not suggestions.

## Active boundaries

| Repository | Owns |
|---|---|
| `loom-ai` | Intent, Worker/Arbiter execution, execution state, evidence, task orchestration, stable integration points |
| `loom-client-setup` | External-client integration and client-facing setup |
| `loom-setup` | Machine/runtime installation and bootstrap |
| `model-gateway` | Provider access, routing, resources/accounts, credential isolation, retries, limits, caching, usage/cost, provenance |
| `evaluation` | Evaluators, feedback signals, reward attribution, verification semantics |
| `strategy` | Optimization and selection strategies, including interchangeable adaptive strategies |
| `consensus` | Multi-model consensus |
| `budget` | Budget and cost |
| `storage`, `retrieval`, `rag` | Named storage and retrieval capabilities |
| `scraping` | Discovery and resource acquisition |
| `chunking` | Provenance-preserving derivation of chunks |
| `structured-output`, `cache`, `conversation`, `streaming` | Dedicated capability boundaries |
| `observability`, `resilience`, `security` | Cross-cutting operational boundaries |
| `curses-tui`, `tui-schema` | Terminal interaction and canonical TUI schema |
| `genetic-optimizer` | Genetic optimization |

## Retiring or historical boundaries

These names may still exist in Git history or older repositories, but they are not the default place for new architecture:

- `learning`
- `workflow`
- `crush-demo`
- `knowledge` as an active development repository

Durable human-readable knowledge belongs in the Git-backed FlossWare knowledge vault rather than in a dedicated `knowledge` software repository.

## Boundary rule

When functionality crosses a repository boundary, prefer:

- a stable contract
- an adapter
- composition
- decoration
- an explicit integration point

Do **not** copy implementation into another repository merely to make an integration convenient.
