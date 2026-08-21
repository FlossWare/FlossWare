---
title: FlossWare
---

<div class="fw-hero">
  <div class="fw-hero-copy">
    <img class="fw-logo" src="/assets/favicon.svg" alt="FlossWare" width="72" height="72">
    <p class="fw-kicker">OPEN SOURCE · AI · ENGINEERING</p>
    <h2>Software systems built to stay understandable.</h2>
    <p class="fw-lead">FlossWare is an open-source engineering ecosystem for modular infrastructure, AI orchestration, reusable libraries, and standards-driven engineering.</p>
    <div class="fw-actions">
      <a class="fw-button fw-button-primary" href="https://github.com/FlossWare">Explore GitHub</a>
      <a class="fw-button fw-button-secondary" href="#architecture">Explore the architecture</a>
    </div>
  </div>
</div>

<div class="fw-stats">
  <div><strong>Open</strong><span>standards first</span></div>
  <div><strong>Modular</strong><span>replaceable components</span></div>
  <div><strong>AI-ready</strong><span>REST + MCP + SDKs</span></div>
  <div><strong>Documented</strong><span>decisions preserved</span></div>
</div>

## What is FlossWare?

FlossWare builds reusable engineering foundations and reference implementations using explicit contracts, open interfaces, and loosely coupled architectures. The goal is deliberately unfashionable: **make powerful systems understandable, replaceable, and maintainable.**

<div class="fw-cards">
  <a class="fw-card" href="#architecture">
    <span class="fw-card-icon">01</span>
    <h3>Architecture</h3>
    <p>Contracts, orchestration, execution, routing, distributed systems, and stable integration boundaries.</p>
  </a>
  <a class="fw-card" href="#loom">
    <span class="fw-card-icon">02</span>
    <h3>Loom</h3>
    <p>A provider-neutral AI orchestration substrate with pluggable model, execution, context, data, and capability backends.</p>
  </a>
  <a class="fw-card" href="#projects">
    <span class="fw-card-icon">03</span>
    <h3>Open-source projects</h3>
    <p>Libraries, infrastructure tooling, engineering standards, and focused capability implementations.</p>
  </a>
</div>

## Architecture {#architecture}

FlossWare favors **contracts over coupling** and **replaceability over centralized assumptions**. Components should expose stable interfaces while allowing implementations to evolve independently.

```text
                         Applications / Agents
                                  |
                    SDK / CLI / REST / MCP adapters
                                  |
                         Loom orchestration
                    /       |       |        \\
              execution  routing  consensus  context
                    \\       |       |        /
                         Contracts / Protocols
                                  |
                  +---------------+----------------+
                  |               |                |
               Models          Data           Capabilities
            cloud/local     storage/search     tools/resources
                  |               |                |
                  +--------- replaceable ---------+
```

- **Contracts** define stable behavior without forcing implementation inheritance.
- **Loom** composes those contracts into orchestration and execution capabilities.
- **REST, SDKs, CLI, and MCP** provide integration surfaces appropriate to the consumer.
- **Backends** remain independently replaceable where practical.
- **ADRs** preserve architectural decisions and their tradeoffs.

### Architecture documentation

- [Loom architecture](docs/architecture/loom.md) — current AI orchestration architecture and boundaries
- [Consensus](docs/architecture/consensus.md) — multi-model synthesis and verification patterns
- [Distributed Fleet](docs/architecture/fleet.md) — fleet topology and distribution
- [Model Routing](docs/architecture/routing.md) — adaptive model selection and fallback concepts

## Loom {#loom}

[loom-ai](https://github.com/FlossWare/loom-ai) is the primary AI infrastructure project in the organization.

Loom is an **orchestration substrate, not an agent framework**. Its core defines provider-neutral protocols and uses optional implementations for inference, storage, queues, embeddings, search, graphs, tools, resources, and execution.

Key properties include:

- Local or cloud model implementations can participate through the same contracts.
- In-memory defaults keep the core usable with minimal dependencies.
- PostgreSQL, Redis, graph, embedding, search, and other integrations are optional backends.
- Execution is explicit, with task runners, pipelines, and DAG-based orchestration.
- MCP and coding-agent adapters provide integration without making those tools core dependencies.
- External projects are interoperability targets and implementations, not automatically architectural dependencies.

See the [Loom repository](https://github.com/FlossWare/loom-ai) and its [architecture guide](https://github.com/FlossWare/loom-ai/blob/main/docs/architecture.md) for implementation-level detail.

## Knowledge systems {#knowledge}

FlossWare maintains knowledge-oriented components for ingestion, transformation, retrieval, and graph relationships. These capabilities can be consumed independently or supplied to Loom through appropriate contracts.

- [Web Scraping](docs/knowledge/scraping.md)
- [Document Chunking](docs/knowledge/chunking.md)
- [Vector Embeddings](docs/knowledge/embeddings.md)
- [Knowledge Graph](docs/knowledge/graph.md)

Project knowledge that is private or operationally sensitive is intentionally kept outside the public organization documentation.

## Data and infrastructure

| System | Role |
|---|---|
| [PostgreSQL + pgvector](docs/databases/postgres.md) | Relational and vector storage where appropriate |
| [Redis](docs/databases/redis.md) | Queues, caching, and rate limiting where appropriate |
| [OrientDB](docs/databases/orientdb.md) | Graph storage and relationship traversal where appropriate |

These are **supported implementations, not mandatory architectural dependencies**. The distinction matters now that Loom is explicitly pluggable.

## Learning and optimization

FlossWare also explores adaptive engineering techniques that can be used where they provide measurable value:

- [Thompson Sampling](docs/learning/thompson_sampling.md) — adaptive selection under uncertainty
- [Genetic Algorithms](docs/learning/genetic_algorithms.md) — configuration and strategy optimization

These techniques are capabilities, not universal requirements.

## Projects {#projects}

<div class="fw-projects">
  <a href="https://github.com/FlossWare/loom-ai"><strong>loom-ai</strong><span>Provider-neutral AI orchestration substrate with pluggable contracts and backends.</span></a>
  <a href="https://github.com/FlossWare/engineering-standards"><strong>engineering-standards</strong><span>Canonical engineering principles, standards, and architecture decision records.</span></a>
  <a href="https://github.com/FlossWare/commons-java"><strong>commons-java</strong><span>Shared Java foundation libraries.</span></a>
  <a href="https://github.com/FlossWare/knowledge"><strong>knowledge</strong><span>Private knowledge repository and supporting material.</span></a>
</div>

For the complete current repository roles and visibility, see the [repository map](docs/repositories.md).

## Development

- [Getting Started](docs/development/getting_started.md)
- [Contributing](docs/development/contributing.md)
- [Coding Standards](docs/development/coding_standards.md)
- [Design Philosophy](docs/philosophy.md)
- [Engineering Standards](https://github.com/FlossWare/engineering-standards)

## About FlossWare

FlossWare is an open-source engineering effort spanning distributed systems, enterprise Java, search, AI/ML, infrastructure automation, and reusable engineering tooling.

---

<div class="fw-footer-cta">
  <strong>Build it. Document it. Keep it replaceable.</strong>
  <span>Open interfaces, deliberate architecture, and evidence-driven engineering.</span>
</div>
