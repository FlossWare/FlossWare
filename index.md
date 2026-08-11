---
title: FlossWare
---

<div class="fw-hero">
  <div class="fw-hero-banner">
    <img src="/images/flossware-banner.jpg" alt="FlossWare">
  </div>
  <div class="fw-hero-copy">
    <p class="fw-kicker">OPEN SOURCE · AI · ENGINEERING</p>
    <h2>Software systems built to stay understandable.</h2>
    <p class="fw-lead">FlossWare is a free-first engineering ecosystem for modular infrastructure, AI-assisted systems, distributed services, and reusable open-source foundations.</p>
    <div class="fw-actions">
      <a class="fw-button fw-button-primary" href="https://github.com/FlossWare">Explore GitHub</a>
      <a class="fw-button fw-button-secondary" href="#architecture">Explore the architecture</a>
    </div>
  </div>
</div>

<div class="fw-stats">
  <div><strong>Open</strong><span>standards first</span></div>
  <div><strong>Modular</strong><span>loosely coupled systems</span></div>
  <div><strong>AI-ready</strong><span>REST + MCP + events</span></div>
  <div><strong>Documented</strong><span>decisions preserved</span></div>
</div>

## What is FlossWare?

FlossWare builds reusable engineering foundations and reference implementations using open standards, explicit configuration, and loosely coupled architectures. The goal is deliberately unfashionable: **make powerful systems understandable, replaceable, and maintainable.**

<div class="fw-cards">
  <a class="fw-card" href="#architecture">
    <span class="fw-card-icon">01</span>
    <h3>Architecture</h3>
    <p>Service boundaries, orchestration, routing, distributed fleets, consensus, and stable integration contracts.</p>
  </a>
  <a class="fw-card" href="#knowledge">
    <span class="fw-card-icon">02</span>
    <h3>Knowledge systems</h3>
    <p>Scraping, chunking, embeddings, vector search, graphs, and the pipelines connecting them.</p>
  </a>
  <a class="fw-card" href="#projects">
    <span class="fw-card-icon">03</span>
    <h3>Open-source projects</h3>
    <p>Reusable libraries, infrastructure tooling, engineering standards, and reference implementations.</p>
  </a>
</div>

## Architecture {#architecture}

<div class="fw-principles">
  <div><strong>Configuration is the source of truth.</strong><span>Behavior should be explicit rather than hidden in code.</span></div>
  <div><strong>Minimal by default.</strong><span>Capabilities are enabled deliberately.</span></div>
  <div><strong>Contracts over coupling.</strong><span>Stable interfaces let components evolve independently.</span></div>
  <div><strong>Open standards.</strong><span>Prefer interoperable designs over vendor lock-in.</span></div>
</div>

```text
                         Clients / Agents
                                |
                   REST APIs / MCP Tool Interfaces
                                |
                         Service Boundaries
                         /               \\
                        /                 \\
              Stored Procedures       Message Bus
                    |                       |
                    v                       v
                Databases             Event Consumers
```

- **REST** provides synchronous service contracts.
- **MCP** provides AI agent and tool integration contracts.
- **Message buses** provide asynchronous workflows and event-driven integration.
- **Stored procedures** provide database abstraction where they add value.
- **ADRs** preserve architectural decisions.

### Architecture documentation

- [Orchestration Layer](docs/architecture/orchestration.html) — REST API, task classification, request lifecycle
- [Distributed Fleet](docs/architecture/fleet.html) — heterogeneous cluster topology and SSH-based distribution
- [Consensus Engine](docs/architecture/consensus.html) — multi-model synthesis and arbiter patterns
- [Model Routing](docs/architecture/routing.html) — Thompson Sampling, capability matrix, fallback chains

## Knowledge systems {#knowledge}

<div class="fw-cards fw-cards-compact">
  <a class="fw-card" href="docs/knowledge/scraping.html"><h3>Ingest</h3><p>Web scraping and document acquisition from diverse knowledge domains.</p></a>
  <a class="fw-card" href="docs/knowledge/chunking.html"><h3>Transform</h3><p>Semantic chunking and preparation for downstream retrieval.</p></a>
  <a class="fw-card" href="docs/knowledge/embeddings.html"><h3>Retrieve</h3><p>Vector embeddings, HNSW indexing, and similarity search.</p></a>
  <a class="fw-card" href="docs/knowledge/graph.html"><h3>Connect</h3><p>Knowledge graphs and relationship traversal across stored knowledge.</p></a>
</div>

### Knowledge documentation

- [Web Scraping](docs/knowledge/scraping.html)
- [Document Chunking](docs/knowledge/chunking.html)
- [Vector Embeddings](docs/knowledge/embeddings.html)
- [Knowledge Graph](docs/knowledge/graph.html)

## Data and infrastructure

| System | Role |
|---|---|
| [PostgreSQL + pgvector](docs/databases/postgres.html) | Relational storage and vector similarity |
| [Redis](docs/databases/redis.html) | Pipeline queues, caching, and rate limiting |
| [OrientDB](docs/databases/orientdb.html) | Graph storage and relationship traversal |

## Learning and optimization

The engineering work also includes experimental systems for adaptive decision-making and configuration evolution.

- [Thompson Sampling](docs/learning/thompson_sampling.html) — Bayesian multi-armed bandit for strategy selection
- [Genetic Algorithms](docs/learning/genetic_algorithms.html) — configuration evolution across multiple domains

## Operations

- [Deployment](docs/operations/deployment.html) — Ansible-automated fleet deployment
- [Monitoring](docs/operations/monitoring.html) — fleet health and provider tracking
- [Scaling](docs/operations/scaling.html) — horizontal and vertical scaling and bottleneck identification

## Projects {#projects}

<div class="fw-projects">
  <a href="https://github.com/FlossWare/engineering-standards"><strong>engineering-standards</strong><span>Architecture decisions, engineering principles, and development standards.</span></a>
  <a href="https://github.com/FlossWare/commons-java"><strong>commons-java</strong><span>Shared Java foundation libraries.</span></a>
  <a href="https://github.com/FlossWare/tftp-os"><strong>tftp-os</strong><span>Reference implementation for infrastructure provisioning and automation.</span></a>
</div>

## Development

- [Getting Started](docs/development/getting_started.html)
- [Contributing](docs/development/contributing.html)
- [Coding Standards](docs/development/coding_standards.html)
- [Design Philosophy](docs/philosophy.html)

## About FlossWare

FlossWare is an open-source engineering effort led by **[Scot P. Floess (Flossy)](sfloess/)**. The work spans distributed systems, enterprise Java, search, AI/ML, infrastructure automation, and open-source tooling.

For the professional résumé and background, visit [sfloess.github.io](https://sfloess.github.io). For the Salesforce deployment project, visit [Solenopsis](https://solenopsis.github.io).

---

<div class="fw-footer-cta">
  <strong>Build it. Document it. Keep it replaceable.</strong>
  <span>Free-first engineering with open interfaces and deliberate architecture.</span>
</div>
