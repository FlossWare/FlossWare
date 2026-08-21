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
                         Loom orchestration
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

### Orchestration layer

Loom coordinates higher-level behavior such as:

- task execution and dependency-aware scheduling;
- model routing and fallback;
- multi-model consensus and synthesis;
- context construction and related middleware;
- capability and tool dispatch;
- integration with external agent runtimes.

### Contract layer

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

### Backend layer

Backends provide concrete implementations for inference, storage, queues, secrets, embeddings, search, graphs, tools, resources, and execution. Optional dependencies are loaded only when the selected implementation requires them.

The default in-memory configuration is deliberately useful. It makes the core testable and usable without provisioning a database, queue, model server, or cloud account.

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
ExecutionEngine
  |
  +-- dependency-aware task plan / DAG
        |
        +-- execution pipeline
              |
              +-- TaskRunner
                    |
                    +-- concrete execution backend
```

A task runner is the leaf execution primitive. Pipelines provide operational lifecycle behavior such as cancellation and deadlines. The execution engine coordinates dependencies and can execute independent work concurrently.

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

## Design consequences

1. **No provider is sacred.** Provider selection is an implementation choice governed by capability, reliability, cost, privacy, and other requirements.
2. **No backend is mandatory without an ADR.** Supported infrastructure should not silently become architectural coupling.
3. **The core should stay small.** Optional capabilities belong behind contracts and optional dependencies.
4. **Capabilities can mature independently.** Experimental repositories do not need to become core merely because a directory exists for them.
5. **Documentation must track decisions.** When the architecture changes, stale ADRs and site documentation are part of the change surface.
6. **Dogfooding is the validation path.** Loom should increasingly exercise its own contracts, tooling, documentation, and external integrations in real FlossWare engineering workflows.
