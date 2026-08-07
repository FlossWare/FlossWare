---
title: FlossWare
---

# FlossWare

**Free-first, modular infrastructure and AI-assisted engineering.**

FlossWare builds reusable engineering foundations and reference implementations using open standards, explicit configuration, and loosely coupled architectures.

---

## Architecture Principles

- **Configuration is the source of truth**
- **Minimal behavior by default; capabilities are explicitly enabled**
- **Components are modular and composable**
- **Open standards over vendor lock-in**
- **Loose coupling through stable contracts**

## Reference Architecture

```
                 Clients / Agents
                       |
          REST APIs / MCP Tool Interfaces
                       |
              Service Boundaries
                       |
        +--------------+--------------+
        |                             |
  Stored Procedures              Message Bus
        |                             |
        v                             v
    Databases                 Event Consumers
```

## Integration Model

- **REST** provides synchronous service contracts.
- **MCP** provides AI agent and tool integration contracts.
- **Message buses** provide asynchronous workflows and event-driven integration.
- **Stored procedures** provide database abstraction where they add value.
- **ADRs** preserve architectural decisions.

---

## Documentation

### Architecture

- [Orchestration Layer](docs/architecture/orchestration.md) -- REST API, task classification, request lifecycle
- [Distributed Fleet](docs/architecture/fleet.md) -- Heterogeneous cluster topology and SSH-based distribution
- [Consensus Engine](docs/architecture/consensus.md) -- Multi-model synthesis and arbiter patterns
- [Model Routing](docs/architecture/routing.md) -- Thompson Sampling, capability matrix, fallback chains

### Databases

- [PostgreSQL + pgvector](docs/databases/postgres.md) -- Relational storage, vector similarity, schema overview
- [Redis](docs/databases/redis.md) -- Pipeline queues, caching, rate limiting
- [OrientDB](docs/databases/orientdb.md) -- Graph database for relationship traversal

### Knowledge Pipeline

- [Web Scraping](docs/knowledge/scraping.md) -- Document ingestion from 116+ scrapers across 15+ domains
- [Document Chunking](docs/knowledge/chunking.md) -- Semantic splitting for embedding
- [Vector Embeddings](docs/knowledge/embeddings.md) -- HNSW indexing and similarity search
- [Knowledge Graph](docs/knowledge/graph.md) -- Triple-store architecture

### Learning and Optimization

- [Thompson Sampling](docs/learning/thompson_sampling.md) -- Bayesian multi-armed bandit for strategy selection
- [Genetic Algorithms](docs/learning/genetic_algorithms.md) -- Configuration evolution across seven domains

### Operations

- [Deployment](docs/operations/deployment.md) -- Ansible-automated fleet deployment
- [Monitoring](docs/operations/monitoring.md) -- Fleet health, provider tracking, feedback loop analysis
- [Scaling](docs/operations/scaling.md) -- Horizontal and vertical scaling, bottleneck identification

### Development

- [Getting Started](docs/development/getting_started.md) -- Development environment setup
- [Contributing](docs/development/contributing.md) -- How to contribute
- [Coding Standards](docs/development/coding_standards.md) -- Code conventions and patterns

### Philosophy

- [Design Philosophy](docs/philosophy.md) -- Architectural decisions, tradeoffs, and rationale

---

## Featured Projects

| Repository | Purpose |
|---|---|
| [engineering-standards](https://github.com/FlossWare/engineering-standards) | Architecture decisions, engineering principles, and development standards |
| [commons-java](https://github.com/FlossWare/commons-java) | Shared Java foundation libraries |
| [tftp-os](https://github.com/FlossWare/tftp-os) | Reference implementation for infrastructure provisioning and automation |

---

## Meet the Team

- **[Scot Floess](sfloess/)** -- Senior Principal Software Engineer at Red Hat with 30+ years building distributed systems, enterprise Java, and open source tooling. Architect of [FlossWare](https://github.com/FlossWare), co-architect of [Solenopsis](https://github.com/solenopsis), and holder of [U.S. Patent 6,442,565](https://patents.google.com/patent/US6442565) for distributed computing technologies.

---

## Contributing

Contributions should align with the [FlossWare](https://github.com/FlossWare) [engineering standards](https://github.com/FlossWare/engineering-standards). Projects should favor clarity, composability, and maintainable open designs over unnecessary complexity.

See the [Contributing Guide](docs/development/contributing.md) for details.
