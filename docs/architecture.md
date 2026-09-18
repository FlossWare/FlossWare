---
title: FlossWare Architecture
---

# FlossWare Architecture

> **Current baseline · 2026-09-17**
>
> FlossWare is a collection of composable capabilities with **Loom as a language-neutral execution substrate**. AI is a domain built on Loom rather than the definition of Loom itself. Older architecture material remains in Git history and explicitly historical documentation.

## Contract and implementation layers

FlossWare separates foundational contracts, domain contracts, and language implementations:

    {contract}
         |
         +-- {contract}-{language}
         |
         +-- {contract}-{domain}
                   |
                   +-- {contract}-{domain}-{language}

For Loom this becomes:

    loom
      |
      +-- loom-python
      |
      +-- loom-ai
             |
             +-- loom-ai-python

The reusable repository naming and layering rule is defined by the FlossWare engineering standard ADR-0024.

## The core Loom model

Loom provides the language-neutral machinery for:

    Contract
      -> Requirement / Capability
      -> Registration
      -> Discovery
      -> Provider / Builder
      -> Endpoint / Binding
      -> Invocation
      -> Result / Evidence

A Loom implementation realizes these semantics. The implementation language is not part of the Loom contract.

## AI domain model

The AI domain builds on Loom:

    Loom contracts
         |
         v
    AI-domain contracts
         |
         v
    AI implementation
         |
       Intent
         -> Arbiter
             -> Worker
             -> Worker
         -> evidence
         -> result

loom-ai owns the AI-domain contracts and semantics. loom-ai-python owns their Python implementation. Future implementations such as loom-ai-java are peers.

External clients such as Crush, Claude Code, Codex, and Cursor consume Loom through external interfaces. They are not Loom implementations merely because they can invoke Loom.

## Current boundaries

| Repository | Responsibility |
|---|---|
| loom | Language-neutral Loom protocol, semantic model, binding model, and conformance |
| loom-python | Python implementation of the Loom contract |
| loom-ai | AI-domain contracts and semantics built on Loom |
| loom-ai-python | Python implementation of AI-domain contracts |
| loom-setup | Installation and runtime setup |
| loom-client-setup | External-client configuration and integration |
| model-gateway | Provider/model invocation, resources, accounts, credentials, feasibility, selection, usage/cost, provenance, prompt caching |
| evaluation | Evaluation and reward implementations |
| strategy | Interchangeable decision and optimization strategies |
| consensus | Multi-model consensus |
| budget | Budget and cost controls |
| storage, retrieval, rag | Storage and retrieval capabilities |
| scraping | Discovery and resource acquisition |
| chunking | Provenance-preserving chunk derivation |
| structured-output, cache, conversation, streaming | Dedicated cross-cutting capabilities |
| observability, resilience, security | Operational and safety boundaries |
| curses-tui, tui-schema | Reusable terminal interaction and canonical TUI schema |
| genetic-optimizer | Genetic optimization capability |

Repository boundaries are architectural constraints. A capability belongs behind its contract rather than being absorbed into the repository that happens to call it.

## Knowledge and acquisition

Knowledge acquisition is separate from Loom execution:

    Discovery -> URI set -> Acquisition -> local corpus
                                      -> extraction / normalization
                                      -> chunking
                                      -> embedding / indexing / retrieval

Raw acquisition artifacts remain preserved. Content is content-addressed and source provenance is retained. Chunks are derived artifacts whose provenance and structured metadata remain separate from canonical chunk text.

## Architectural rules

1. Keep foundational contracts language-neutral.
2. Keep domain contracts separate from their language implementations.
3. Treat implementations in different languages as peers.
4. Do not make a reference implementation the de facto contract specification.
5. Keep capability implementations behind stable contracts.
6. Preserve provenance across acquisition, derivation, execution, evaluation, and retrieval.
7. Keep external clients outside Loom's internal execution model.
8. Promote ideas into durable decisions through implementation and executable dogfood evidence.

## Where the knowledge lives

The FlossWare knowledge base is Markdown and Git-backed so it can be authored locally in Obsidian and published through GitHub Pages. The public site is a presentation of that same knowledge, not a second manually maintained documentation corpus.

GitHub remains the executable source of truth for software, contracts, tests, and implementation details. The knowledge vault provides the human-readable architectural layer.

## Evidence

- Repository boundaries
- Engineering principles
- Dogfooding
- FlossWare GitHub organization
