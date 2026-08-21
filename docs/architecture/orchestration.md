---
title: Orchestration Layer
---

# Orchestration Layer

FlossWare's current orchestration architecture is centered on [Loom](https://github.com/FlossWare/loom-ai). Loom provides the control plane for coordinating models, execution, context, tools, resources, and optional infrastructure backends.

Loom is deliberately **not** tied to a single host, worker fleet, model provider, or deployment topology. A deployment may be embedded in a process, exposed through REST, integrated through MCP, or connected to external agents and services.

## Request lifecycle

A typical orchestration request follows this conceptual flow:

```text
Caller / Agent / CLI / REST / MCP
              |
              v
        Request intake
              |
              v
     Context + task preparation
              |
              v
       Execution planning
              |
        +-----+------+
        |            |
     Routing      Direct task
        |            |
        v            |
   Model/tool       |
   selection        |
        |            |
        +-----+------+
              |
              v
       Task execution
              |
              v
      Verification /
      evaluation
              |
              v
        Result / evidence
              |
              v
          Caller
```

The exact path depends on the configured capabilities. Consensus, adaptive routing, external tools, persistent storage, and graph or search services are optional rather than mandatory steps.

## Execution hierarchy

Loom separates execution concerns into explicit layers:

```text
ExecutionEngine
  |
  +-- dependency-aware execution plan
        |
        +-- ExecutionPipeline
              |
              +-- TaskRunner
                    |
                    +-- backend implementation
```

This allows a simple task to use a single runner while larger workflows can express dependencies and concurrent execution.

## Model coordination

Model selection is an implementation concern behind Loom's inference and routing contracts. A deployment may use:

- a fixed model chosen by the caller;
- capability-aware routing;
- adaptive selection such as Thompson Sampling;
- provider fallback chains;
- local and cloud inference together;
- a custom routing implementation.

No single routing algorithm is required by the architecture.

## Verification

Generation and verification are separate concerns. Depending on the workflow, verification can be deterministic tests, an independent model, multiple model votes, benchmark evaluation, or human review.

The system should preserve evidence about what was executed and how a result was accepted, rejected, or escalated.

## Integration boundaries

Loom exposes several integration surfaces:

- Python APIs and the client SDK for programmatic use;
- REST for language-neutral service integration;
- CLI for operators and developers;
- MCP for AI agent and tool integration;
- adapters for external coding agents and other execution environments.

These surfaces should consume stable contracts rather than internal backend implementations.

## Historical fleet architecture

Older FlossWare documentation described a centralized REST endpoint on `aio-01`, SSH dispatch to a fixed worker fleet, and a 200+ model pool as the primary orchestration architecture. Those details describe a historical deployment and are no longer organization-wide architectural requirements.

The fleet and infrastructure can still be useful implementations or dogfooding environments, but Loom's current contract-first architecture intentionally allows different deployment topologies.

## Related documentation

- [Loom architecture](loom.md)
- [Model routing](routing.md)
- [Consensus](consensus.md)
- [Repository map](../repositories.md)
- [Engineering standards](https://github.com/FlossWare/engineering-standards)
