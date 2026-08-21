---
title: Design Philosophy
---

# Design Philosophy

This document captures the current architectural reasoning behind FlossWare's engineering ecosystem. Historical decisions remain useful evidence, but they are not automatically permanent policy. Current behavior and accepted architectural decisions are maintained in `engineering-standards` and its ADRs.

## 1. Contracts before implementations

**Problem:** Infrastructure and AI systems change quickly. Hard-coding providers, databases, queues, model runtimes, or agent frameworks turns experimentation into architectural coupling.

**Decision:** Define stable contracts and keep implementations behind them. Loom uses small provider-neutral protocols and structural typing so external implementations can participate without inheriting from Loom or importing its internals.

**Tradeoff:** An abstraction layer adds design and testing work, and some provider-specific features will not fit cleanly into a common interface.

**Reconsider if:** The abstraction demonstrably prevents important capabilities from being exposed without a compensating extension mechanism.

## 2. Provider independence

**Problem:** A single AI provider creates operational and architectural concentration risk. Providers change models, pricing, quotas, APIs, and availability.

**Decision:** FlossWare treats model providers as interchangeable implementations. Capability and routing decisions should not assume a single vendor.

**Tradeoff:** Provider-neutral interfaces cannot expose every provider-specific feature without extensions, and multiple providers increase integration and evaluation work.

**Reconsider if:** A capability has no meaningful alternative and the dependency is explicitly accepted through an ADR.

## 3. Local and cloud inference are both valid

**Problem:** The older FlossWare architecture treated API-only inference as the default because local model hosting added operational cost and often produced lower quality.

**Decision:** That is no longer a universal architectural constraint. Loom supports inference implementations that can target hosted APIs, gateways, local model servers, or other compatible runtimes.

The right choice depends on requirements such as:

- privacy and data locality;
- latency;
- offline or degraded-mode operation;
- cost;
- hardware availability;
- model capability;
- operational complexity.

**Tradeoff:** Supporting both local and cloud implementations increases testing and deployment combinations.

**Reconsider if:** The supported contract cannot represent the operational guarantees required by a deployment class.

## 4. Minimal core, optional capabilities

**Problem:** AI systems accumulate dependencies rapidly. Making every feature mandatory produces a fragile installation and makes simple use cases unnecessarily expensive.

**Decision:** Loom's core remains usable with minimal dependencies. Storage, queues, embeddings, search, graphs, tools, resources, inference, and other capabilities are selected through optional implementations.

**Tradeoff:** Users must choose or configure the implementations appropriate to their deployment.

**Reconsider if:** A capability becomes genuinely foundational to every supported deployment and its inclusion no longer creates meaningful coupling.

## 5. Orchestration instead of monolithic agents

**Problem:** Putting planning, tool use, memory, execution, model selection, and verification into one agent implementation makes behavior difficult to test and replace.

**Decision:** Loom is an orchestration substrate. Execution, routing, consensus, context, tools, resources, and evaluation are separable capabilities connected through contracts.

The model is a reasoning component. The harness remains responsible for workflow state, execution policy, deadlines, retries, verification, and coordination.

**Tradeoff:** More explicit architecture means more interfaces and lifecycle decisions.

**Reconsider if:** A simpler architecture provides equivalent control, observability, replaceability, and reliability without sacrificing extensibility.

## 6. Verification is separate from generation

**Problem:** A model checking its own output tends to reproduce its own assumptions.

**Decision:** Generation and verification are separate concerns. Verification may use deterministic tests, an independent model, multiple reviewers, benchmarks, or domain-specific validators.

Consensus can improve evidence, but agreement is not proof. High-stakes changes still require appropriate human or deterministic validation.

**Tradeoff:** Verification consumes additional compute and engineering effort.

**Reconsider if:** A task has a deterministic correctness mechanism that is demonstrably stronger and cheaper than model-based verification.

## 7. Evidence-driven adaptation

**Problem:** Model quality, provider behavior, and configuration effectiveness change over time. Static routing and hand-tuned configuration become stale.

**Decision:** Where measurable outcomes exist, FlossWare may use adaptive methods such as Thompson Sampling or genetic optimization. These are capabilities, not mandatory architecture.

Selection mechanisms must preserve enough evidence to explain why a configuration or model was preferred and must not turn exploration into uncontrolled production risk.

**Tradeoff:** Adaptive systems require telemetry, evaluation criteria, state management, and safeguards against noisy feedback.

## 8. Documentation and decisions are part of the architecture

**Problem:** AI-assisted engineering produces large volumes of code, configuration, issues, reviews, and generated documentation. Without an authoritative decision trail, the organization accumulates contradictory descriptions of the same system.

**Decision:** Architectural decisions belong in ADRs. Public project documentation describes current behavior and boundaries. Historical decisions should be explicitly marked as superseded when they no longer apply.

The FlossWare documentation hierarchy is:

```text
engineering-standards
        |
        +-- principles / standards / ADRs
        |
        +-- repository and workflow rules
        |
        v
FlossWare organization documentation
        |
        +-- current architecture
        +-- repository map
        +-- public engineering concepts
        |
        v
Project documentation
        |
        +-- loom-ai implementation contracts
        +-- capability-specific behavior
        +-- operational details
```

Private knowledge remains private. Public documentation should describe architecture without reproducing private operational or knowledge content.

## 9. Dogfooding

**Problem:** An orchestration framework can look excellent in documentation while failing when used to build and maintain itself.

**Decision:** FlossWare should increasingly dogfood Loom for repository analysis, issue triage, documentation maintenance, coding-agent integration, verification, and other engineering workflows.

Dogfooding is not merely a demo. It is an architectural test of whether the contracts, adapters, context handling, execution model, and verification system work together under real engineering load.

**Tradeoff:** Dogfooding exposes rough edges earlier and therefore creates more visible work. That is a feature, not a defect.

## 10. Git worktrees and parallel engineering

**Problem:** Multiple agents or developers modifying the same working tree create conflicts and make attribution difficult.

**Decision:** Parallel agent work should use isolated branches and worktrees, with explicit integration and verification boundaries.

The exact workflow belongs in engineering standards rather than in Loom's runtime architecture.

## Current architectural summary

FlossWare's current direction can be summarized as:

```text
                 Capability requirements
                          |
                          v
                    Stable contracts
                          |
          +---------------+---------------+
          |               |               |
      Implementations  Orchestration   Verification
          |               |               |
    local / cloud     Loom execution    tests / review
    pluggable data    routing / context  evidence
    tools / services  consensus          human judgment
          |               |               |
          +---------------+---------------+
                          |
                    Measured outcomes
                          |
                          v
                    Better decisions
```

The architectural goal is not to predict which model, provider, database, or agent runtime will win. It is to make changing that answer inexpensive.
