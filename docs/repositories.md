---
title: Repository Map
---

# FlossWare Repository Map

This document describes the current role of the principal FlossWare repositories. It is intentionally architectural rather than a raw inventory: a repository's existence does not imply that it is production-ready, independently supported, or an architectural dependency.

## Primary repositories

| Repository | Visibility | Role |
|---|---|---|
| [loom-ai](https://github.com/FlossWare/loom-ai) | Public | Primary AI orchestration substrate and pluggable contract surface |
| [engineering-standards](https://github.com/FlossWare/engineering-standards) | Public | Canonical engineering standards and ADRs |
| [commons-java](https://github.com/FlossWare/commons-java) | Public | Shared Java foundation libraries |
| [FlossWare](https://github.com/FlossWare/FlossWare) | Public | Organization documentation and architecture overview |
| [knowledge](https://github.com/FlossWare/knowledge) | Private | Private knowledge and supporting material |

## Loom capability family

The current AI repositories include focused capability areas such as model routing, RAG, consensus, evaluation, resilience, observability, structured output, workflow, streaming, conversation, caching, strategy, budget, and learning.

These repositories should be understood as **capability boundaries and integration targets**. They are not automatically required dependencies of Loom. Where a capability becomes mature, Loom should consume it through a stable contract or adapter rather than importing project-specific implementation details into the core.

Current capability repositories include:

- [model-router-ai](https://github.com/FlossWare/model-router-ai)
- [rag-ai](https://github.com/FlossWare/rag-ai)
- [consensus-ai](https://github.com/FlossWare/consensus-ai)
- [evaluation-ai](https://github.com/FlossWare/evaluation-ai)
- [resilience-ai](https://github.com/FlossWare/resilience-ai)
- [observability-ai](https://github.com/FlossWare/observability-ai)
- [structured-output-ai](https://github.com/FlossWare/structured-output-ai)
- [security-ai](https://github.com/FlossWare/security-ai)
- [genetic-optimizer-ai](https://github.com/FlossWare/genetic-optimizer-ai)
- [strategy-ai](https://github.com/FlossWare/strategy-ai)
- [budget-ai](https://github.com/FlossWare/budget-ai)
- [learning-ai](https://github.com/FlossWare/learning-ai)
- [workflow-ai](https://github.com/FlossWare/workflow-ai)
- [streaming-ai](https://github.com/FlossWare/streaming-ai)
- [conversation-ai](https://github.com/FlossWare/conversation-ai)
- [cache-ai](https://github.com/FlossWare/cache-ai)

Some of these repositories are intentionally early-stage. Documentation should describe their intended boundary without presenting an empty or experimental repository as a finished product.

## Other open-source projects

FlossWare also maintains infrastructure, platform, utility, and Java projects, including:

- [diskwipe-java](https://github.com/FlossWare/diskwipe-java)
- [nexus-java](https://github.com/FlossWare/nexus-java)
- [de-converter](https://github.com/FlossWare/de-converter)
- [samsung-galaxy-j7](https://github.com/FlossWare/samsung-galaxy-j7)
- [cobbler](https://github.com/FlossWare/cobbler)
- [ddwrt-bootstrap](https://github.com/FlossWare/ddwrt-bootstrap)
- [notion2config](https://github.com/FlossWare/notion2config)
- [curses-themes](https://github.com/FlossWare/curses-themes)

These projects remain separate when their problem domain, release cadence, or audience benefits from independent ownership.

## Visibility and knowledge boundaries

Public documentation should explain public architecture and reusable engineering knowledge. Private repositories are not merely hidden versions of public repositories: they may contain operational data, private knowledge, credentials or other material that should not be reproduced in public documentation.

In particular, the private `knowledge` repository should be treated as a source of internal knowledge, not as a public project dependency or documentation corpus.

## Naming direction

The organization is moving away from treating `*-ai` as the architecture itself. The important boundary is the **capability**, not the suffix. Loom is the integration substrate; capability repositories can evolve independently and may eventually be renamed, consolidated, archived, or exposed through Loom contracts as their maturity becomes clear.

## Source of truth

- Repository role and architecture: this document and the FlossWare architecture documentation.
- Engineering rules and accepted decisions: `engineering-standards`.
- Loom implementation contracts and behavior: `loom-ai`.
- Private knowledge: `knowledge`.

When these sources disagree, an architectural decision should be recorded or updated explicitly rather than allowing several documents to become competing versions of reality.
