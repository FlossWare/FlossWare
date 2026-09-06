---
title: Loom Architecture
---

# Loom Architecture

Loom is the primary AI orchestration substrate in FlossWare. It is designed as a **provider-neutral orchestration layer**, not as a monolithic agent framework and not as a requirement to use one model provider or one infrastructure stack.

The central architectural rule is simple:

> **Define the contract first. Keep implementations replaceable.**

## System view

```text
                         Applications / Agents
                                  |
                    SDK / CLI / REST / MCP adapters
                                  |
                           Loom Orchestrator
                                  |
                 +----------------+----------------+
                 |                |                |
             Execution         Routing         Consensus
                 |                |                |
                 +----------------+----------------+
                                  |
                         Contract / Protocol layer
                                  |
        +-------------------------+-------------------------+
        |                         |                         |
     Inference                  Data                  Capabilities
  cloud / local          storage / search / graph      tools / resources
        |                         |                         |
        +-------------------------+-------------------------+
                                  |
                       Replaceable implementations
```

## Architectural layers

### Application and integration layer

Applications, agents, SDK consumers, the CLI, REST clients, and MCP integrations live outside the core contract layer. They consume Loom capabilities without needing to know which backend implements them.

All application-facing adapters should enter through the Loom Orchestrator boundary. They must not create a second orchestration path by calling workers, execution engines, model providers, or infrastructure backends directly.

### Orchestrator layer

The **Orchestrator is a first-class Loom concept**. It is the authoritative coordinator for application work and owns the workflow lifecycle.

The Orchestrator is responsible for:

- accepting and validating work;
- planning and dependency-aware scheduling;
- delegating inference/provider selection to the model-routing contract;
- assigning work to local or distributed workers;
- coordinating worker lifecycle, leases, retries, deadlines, and recovery;
- coordinating consensus, verification, and arbiter decisions;
- collecting results and preserving provenance;
- persisting execution state through persistence contracts;
- exposing a stable application boundary to REST, MCP, CLI, TUI, and SDK adapters.

The Orchestrator is the brain. Workers are the execution substrate. An `ExecutionEngine` is an implementation component used by the Orchestrator, not a replacement for the application-facing Orchestrator contract.

### Worker fleet

Loom's target execution architecture includes a distributed worker fleet. Workers are independent execution units that can run on different machines or environments while implementing the same worker contract.

Workers must **initiate connectivity to the Orchestrator**. The Orchestrator must not depend on SSH into workers for registration, dispatch, health polling, or lifecycle management.

The fleet contract therefore includes, at minimum:

- worker registration and capability advertisement;
- authenticated outbound connectivity to the Orchestrator;
- persistent connectivity where practical, with automatic reconnect;
- heartbeats and health state;
- task leasing and lease renewal;
- progress and result reporting;
- graceful deregistration;
- lease expiration and recovery when a worker disappears;
- retry and idempotency semantics;
- correlation of every result with its originating task and execution.

Distributed fleet execution is a required target capability, not merely historical deployment documentation. The current implementation status is tracked separately from this architectural requirement.

### Orchestration capabilities

Loom coordinates higher-level behavior such as:

- task execution and dependency-aware scheduling;
- model routing and fallback;
- multi-model consensus and synthesis;
- context construction and related middleware;
- capability and tool dispatch;
- integration with external agent runtimes.

These capabilities should remain independently replaceable behind contracts where practical.

## Contract layer

Contracts are expressed primarily through small Python `Protocol` interfaces and stable data models. Structural typing is intentional: an implementation does not need to inherit from Loom to satisfy a contract.

This keeps the dependency direction one-way:

```text
contracts/models
      ^
      |
implementations
      ^
      |
configuration / orchestration
      ^
      |
applications / adapters
```

The contract layer should remain as independent as practical from third-party libraries.

## Backend and infrastructure layer

Loom defines provider-neutral contracts for capabilities such as:

- inference;
- persistence and storage;
- queues and messaging;
- secrets;
- embeddings;
- search;
- graph storage;
- tools and resources;
- execution backends;
- observability.

Concrete technologies such as **PostgreSQL, Redis, SQLite, pgvector, OrientDB, or a particular model provider are implementations of those contracts, not intrinsic Loom requirements**.

For example:

```text
Persistence contract  -> PostgreSQL / SQLite / other implementation
Queue contract        -> Redis / in-process queue / other implementation
Search contract       -> PostgreSQL+pgvector / dedicated search / other implementation
Graph contract        -> OrientDB / other graph implementation
Inference contract    -> hosted API / local model server / other implementation
```

A deployment selects the implementations it needs. Optional dependencies should be loaded only when the selected implementation requires them.

The default in-memory configuration is deliberately useful. It makes the core testable and usable without provisioning PostgreSQL, Redis, a graph database, a model server, or a cloud account.

### Required versus optional

Documentation must distinguish three things:

1. **Architectural contracts:** interfaces Loom requires in order to provide a capability.
2. **Supported implementations:** concrete backends that currently implement those contracts.
3. **Deployment requirements:** infrastructure that a particular production deployment chooses or is required to provision.

A supported backend does not become a mandatory dependency merely because it is documented or appears in `pyproject.toml`. A backend becomes mandatory only when an explicit architectural decision or capability requirement says so.

This prevents historical infrastructure choices from quietly turning into permanent architecture. Humanity has already produced enough accidental dependencies without Loom volunteering for another one.

## Provider and deployment independence

Loom does **not** require API-only inference.

A compatible inference implementation may point to a hosted provider, an OpenAI-compatible gateway, a local model server, or another implementation that satisfies the LLM contract. The same principle applies to storage, queues, graphs, search, embeddings, tools, and other capabilities.

This is an architectural change from the older FlossWare orchestration documentation, which described API-only inference as a hard decision. That is no longer the desired organization-wide default. Local inference is a supported architectural option when latency, privacy, cost, availability, offline operation, or other requirements justify it.

The architecture therefore separates:

- **what a capability must do** from
- **where and how that capability is implemented**.

## Execution model

Loom exposes execution at multiple levels:

```text
Orchestrator
  |
  +-- dependency-aware task plan / DAG
        |
        +-- ExecutionEngine
              |
              +-- execution pipeline
                    |
                    +-- TaskRunner / Worker
                          |
                          +-- concrete execution backend
```

A task runner is the leaf execution primitive. Pipelines provide operational lifecycle behavior such as cancellation and deadlines. The execution engine coordinates dependencies and can execute independent work concurrently. Distributed workers provide the same execution substrate across machines while preserving the worker contract.

This hierarchy keeps simple use cases simple without preventing more sophisticated orchestration.

## Consensus and verification

Consensus is a capability, not a requirement for every request.

Where useful, Loom can fan a task out to multiple models and synthesize or evaluate the results. Verification and evaluation should remain independently replaceable so that deterministic checks, model-based review, benchmark systems, or domain-specific validators can be introduced without changing the orchestration contract.

This also means the system should not confuse **model agreement** with **truth**. Agreement is evidence, not authority.

## External projects and adapters

FlossWare frequently evaluates external projects, coding agents, model providers, and infrastructure components. Loom treats these as interoperability targets unless an explicit architectural decision says otherwise.

Using a project does not mean incorporating it. Learning from an architectural idea does not mean taking a dependency on its implementation.

This distinction keeps Loom's core small and prevents the ecosystem from becoming a dependency graph assembled by enthusiasm and regret.

## Relationship to FlossWare repositories

- `loom-ai` owns the orchestration implementation and its contracts.
- `engineering-standards` owns organization-wide engineering rules and ADRs.
- Focused capability repositories can provide independent implementations or experiments.
- Private `knowledge` is not a public architectural dependency.
- Application repositories consume stable interfaces rather than reaching into unrelated capability implementations.

See the [repository map](../repositories.md) for the current repository roles and visibility boundaries.

## Capability traceability

The organization-level documentation is a capability inventory, not a promise that every historical implementation detail remains active.

For every documented capability, one of the following must be true:

- it is implemented and covered by tests;
- it is explicitly marked experimental or historical;
- it is tracked by an implementation issue or acceptance criterion.

Historical hostnames, fixed machine assignments, provider pools, database ports, SSH topology, model counts, and similar deployment details must not be treated as architectural requirements unless an explicit current decision reinstates them.

## Design consequences

1. **No provider is sacred.** Provider selection is an implementation choice governed by capability, reliability, cost, privacy, and other requirements.
2. **No backend is mandatory without an ADR.** Supported infrastructure should not silently become architectural coupling.
3. **The core should stay small.** Optional capabilities belong behind contracts and optional dependencies.
4. **The Orchestrator is first-class.** Application adapters enter through one orchestration boundary, while execution remains replaceable.
5. **Fleet workers are execution infrastructure.** Workers connect outward to the Orchestrator rather than being managed through SSH.
6. **Capabilities can mature independently.** Experimental repositories do not need to become core merely because a directory exists for them.
7. **Documentation must track decisions.** When the architecture changes, stale ADRs and site documentation are part of the change surface.
8. **Dogfooding is the validation path.** Loom should increasingly exercise its own contracts, tooling, documentation, and external integrations in real FlossWare engineering workflows.
