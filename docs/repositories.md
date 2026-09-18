---
title: Repository Boundaries
---

# Repository Boundaries

> **Current boundary map · 2026-09-17**
>
> FlossWare uses multiple repositories deliberately. Repository boundaries are architectural constraints, not suggestions.

## Contract families

FlossWare uses a reusable repository-layering convention:

    {contract}
    {contract}-{language}
    {contract}-{domain}
    {contract}-{domain}-{language}

The names communicate architectural role:

- {contract}: language-neutral foundational contract.
- {contract}-{language}: implementation of that contract in a specific language.
- {contract}-{domain}: domain-specific contract and semantics.
- {contract}-{domain}-{language}: implementation of the domain contract in a specific language.

The normative rule is FlossWare engineering standard ADR-0024.

## Loom family

| Repository | Owns |
|---|---|
| loom | Language-neutral Loom protocol, semantic model, bindings, and conformance |
| loom-python | Python implementation of Loom |
| loom-ai | AI-domain contracts and semantics built on Loom |
| loom-ai-python | Python implementation of the AI-domain contracts |

Future implementations such as loom-java, loom-erlang, loom-ai-java, and loom-ai-erlang are peer implementations.

## Other active boundaries

| Repository | Owns |
|---|---|
| loom-client-setup | External-client integration and client-facing setup |
| loom-setup | Machine/runtime installation and bootstrap |
| model-gateway | Provider access, routing, resources/accounts, credential isolation, retries, limits, caching, usage/cost, provenance |
| evaluation | Evaluators, feedback signals, reward attribution, verification semantics |
| strategy | Optimization and selection strategies, including interchangeable adaptive strategies |
| consensus | Multi-model consensus |
| budget | Budget and cost |
| storage, retrieval, rag | Named storage and retrieval capabilities |
| scraping | Discovery and resource acquisition |
| chunking | Provenance-preserving derivation of chunks |
| structured-output, cache, conversation, streaming | Dedicated capability boundaries |
| observability, resilience, security | Cross-cutting operational boundaries |
| curses-tui, tui-schema | Terminal interaction and canonical TUI schema |
| genetic-optimizer | Genetic optimization |

## Retiring or historical boundaries

These names may still exist in Git history or older repositories, but they are not the default place for new architecture:

- learning
- workflow
- crush-demo
- knowledge as an active development repository

Durable human-readable knowledge belongs in the Git-backed FlossWare knowledge vault rather than in a dedicated knowledge software repository.

## Boundary rule

When functionality crosses a repository boundary, prefer:

- a stable contract
- an adapter
- composition
- decoration
- an explicit integration point

Do not copy implementation into another repository merely to make an integration convenient.
