---
title: Knowledge Architecture
---

# Knowledge Architecture

FlossWare separates **knowledge acquisition** from **AI execution**.

## Acquisition pipeline

```text
Discovery
   -> URI set
   -> Acquisition
   -> local corpus
   -> extraction / normalization
   -> chunking
   -> embedding / indexing
   -> retrieval
```

The acquisition pipeline preserves raw artifacts and source provenance. Derived artifacts such as chunks remain traceable to their source material.

## Execution is separate

Loom consumes capabilities such as retrieval, storage, scraping, and chunking through stable boundaries. Those implementations do not become Loom core merely because Loom orchestrates them.

## Human knowledge

Human-readable knowledge includes:

- architecture
- architectural decisions
- engineering principles
- research and experiments
- dogfooding lessons
- provenance and historical context

This material belongs in [FlossWare/axium](https://github.com/FlossWare/axium), the canonical Git-backed knowledge corpus. Obsidian is an authoring interface and Git provides version history. [FlossWare/standards](https://github.com/FlossWare/standards) owns normative rules. The FlossWare GitHub Pages repository publishes selected public-facing documentation, not a competing canonical corpus.

## Historical knowledge

Older scraping, routing, orchestration, database, and learning documentation may describe systems that no longer match the current architecture. Historical material is useful evidence, but it is not normative merely because it is detailed.

Current architecture takes precedence over historical documentation.
