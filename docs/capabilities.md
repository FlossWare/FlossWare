---
title: FlossWare Capability Boundary Inventory
---

# FlossWare Capability Boundary Inventory

> **Status: Initial design inventory · 2026-10-23**
>
> This document classifies capabilities and proposes contract boundaries. It is not a claim that every listed capability has been implemented, has a stable contract, or runs as an independently deployed service. Those facts must be established from the relevant repository and executable evidence.

## Purpose

FlossWare should expose reusable behavior through clear contracts without turning every algorithm or helper into a network service. The design separates four things:

1. **Responsibility:** what behavior a capability owns.
2. **Contract:** the stable semantics consumers depend on.
3. **Implementation:** the algorithm, library, backend, or composition that realizes the contract.
4. **Deployment:** where and how that implementation runs.

A contract can be invoked in-process today and exposed through REST or MCP later. A service boundary does not automatically imply a separate process or repository. See proposed [ADR-0034](https://github.com/FlossWare/standards/blob/main/adr/ADR-0034-capability-contracts-and-deployment-boundaries.md) and the existing REST boundary and capability/protocol separation decisions in the standards repository.

## Classification vocabulary

| Classification | Meaning |
|---|---|
| **Domain service** | Owns a coherent domain responsibility, policy, lifecycle, or data boundary. |
| **Reusable capability** | Has independently meaningful behavior and a substitutable contract; may run in-process or be remotely exposed. |
| **Implementation component** | Algorithm, transformation, helper, or data structure owned by a parent capability; no separate public contract by default. |
| **Adapter** | Exposes a capability through a protocol or integration surface without duplicating domain logic. |
| **Decorator** | Wraps an implementation to add or alter a documented aspect of behavior while preserving the contract. |

The classifications below are proposed boundaries. “Candidate” means the contract and ownership need validation before implementation or repository creation.

## 1. Execution and coordination

| Capability | Classification | Contract responsibility | Initial boundary guidance |
|---|---|---|---|
| Loom execution substrate | Foundational contract / implementation family | Invocation, Worker semantics, registration/discovery, bindings, results, evidence, lifecycle | Keep language-neutral semantics separate from language implementations |
| Worker | Core execution abstraction | Execute one defined unit of work and return a contract-defined result | Primitive in Loom; not a separate microservice by default |
| Arbiter | Composable execution capability | Coordinate Workers, evaluate completion, and produce an aggregate outcome | An Arbiter is a Worker; recursive composition should remain ordinary |
| Scheduling / dispatch | Reusable capability candidate | Select and dispatch eligible work under explicit policy | Separate only if ownership or operational requirements justify it |
| Worker registry / discovery | Reusable capability candidate | Describe available workers, capabilities, and endpoint bindings | Can be in-process initially; remote exposure depends on fleet and security needs |
| Lease / heartbeat management | Implementation component or reusable capability | Track ownership and liveness for distributed execution | Keep distinct from basic Worker semantics; elevate when shared across runtimes |
| Retry / requeue | Resilience capability | Define retryability, attempt policy, backoff, and requeue semantics | Shared policy may be a capability; transport/runtime mechanics are implementation details |
| Cancellation / deadlines | Contract semantics plus runtime support | Define cancellation, deadline propagation, and terminal outcomes | Must be specified in contracts; not automatically a standalone service |
| Result / evidence provenance | Shared contract capability | Correlate outputs with source, execution, and derivation evidence | Preserve across services; central storage is a separate decision |

## 2. Model and provider invocation

| Capability | Classification | Contract responsibility | Initial boundary guidance |
|---|---|---|---|
| Model Gateway | Domain/platform service | Provider invocation, credential isolation, resource/account selection, limits, usage/cost, invocation provenance | Existing documented repository boundary; provider adapters remain implementations |
| Provider adapter | Adapter / implementation component | Translate the gateway contract to one provider API | Separate package/module where useful; not necessarily a separate service |
| Model/resource inventory | Reusable capability candidate | Describe eligible models, resources, capabilities, and availability | Keep inventory distinct from selection strategy |
| Hard feasibility and policy | Reusable capability | Enforce authorization, capability, budget, quota, rate, and availability constraints | Authoritative constraints must be applied before adaptive selection |
| Selection strategy | Reusable capability | Choose among candidates that passed hard constraints | Thompson sampling or other strategies are implementations of a strategy contract, not necessarily services |
| Usage and cost accounting | Reusable capability candidate | Normalize usage, cost, latency, and provider outcomes | Separate if multiple consumers need consistent accounting or budgets |
| Prompt cache | Decorator / reusable capability | Reuse eligible invocation results under explicit cache semantics | Explicitly enabled; correctness, isolation, and invalidation rules matter |
| Retry / rate limiting | Decorator or resilience capability | Apply configured provider-call resilience and limits | Do not duplicate policy across providers and callers |
| Structured-output validation | Reusable capability | Validate and normalize outputs against a declared schema | Keep schema semantics separate from provider-specific constrained-output support |
| Consensus | Domain capability | Combine multiple model results under an explicit consensus contract | Independent boundary where multi-model coordination is meaningful |
| Evaluation / reward | Reusable capability | Evaluate outcomes and attribute feedback/reward | Keep evaluator implementations replaceable and evaluation evidence traceable |
| Budget enforcement | Domain/platform capability | Apply budgets and account-level spending rules | Hard enforcement is separate from optimization preferences |

## 3. Knowledge acquisition and corpus preparation

| Capability | Classification | Contract responsibility | Initial boundary guidance |
|---|---|---|---|
| Discovery | Reusable capability | Discover candidate source URIs or resources | Separate from acquisition and processing |
| Acquisition / fetching | Reusable capability | Fetch source content and preserve acquisition metadata | Independent of Loom execution and downstream retrieval |
| Raw artifact preservation | Storage capability | Retain original source artifacts and content-addressed identity | Persistence backend is replaceable behind a storage contract |
| Extraction | Reusable capability | Extract text and structured content from source formats | Format-specific extractors are implementations/adapters |
| Normalization / canonicalization | Reusable capability when shared | Produce normalized content without losing source linkage | Internal pipeline stage when only one consumer needs it |
| Chunking | Reusable capability | Derive chunks while preserving source provenance and structured metadata | Define stable chunk semantics; multiple strategies may implement the contract |
| Token counting | Implementation component initially | Count tokens under a declared tokenizer/model vocabulary | Promote to a reusable capability when shared token accounting or quota semantics justify it |
| Embedding generation | Reusable capability | Convert content/query inputs to declared vector representations | Model/provider-specific implementations behind a contract; include dimensions and compatibility metadata |
| Content identity / hashing | Implementation component or reusable capability | Identify identical content under explicit canonicalization rules | Keep internal unless shared identity semantics are needed across systems |
| Metadata extraction | Reusable capability candidate | Produce typed metadata and provenance from source content | Separate from canonical text and chunk generation when independently consumed |
| Ingestion orchestration | Reusable capability or domain service | Coordinate extraction, normalization, chunking, embedding, and index updates | Orchestrator should compose capabilities, not absorb their implementations |

## 4. Retrieval and search

| Capability | Classification | Contract responsibility | Initial boundary guidance |
|---|---|---|---|
| Lexical retrieval | Reusable capability | Retrieve lexical candidates from a corpus/index | Own candidate retrieval and result semantics, not every scoring algorithm |
| Lexical index | Storage/index implementation | Maintain the data structures required for lexical retrieval | Expose through an index contract if multiple retrieval implementations need it |
| Corpus statistics | Reusable capability candidate | Provide document frequencies, corpus size, average lengths, and related statistics | Distinct contract if multiple retrieval/scoring consumers need consistent statistics |
| Index metadata / schema | Reusable capability candidate | Describe index version, analyzers, fields, vector dimensions, and compatibility | Keep consistency and versioning rules explicit |
| BM25 | Algorithm implementation | Score lexical candidates using query, document, and corpus statistics | Not a service by default; can implement a scoring strategy contract |
| Query parsing | Reusable capability when shared | Convert user/query text into a declared query representation | Internal component if used only by one retrieval implementation |
| Query rewriting / expansion | Reusable capability candidate | Derive alternate terms or query forms with provenance | Keep optional and policy-controlled |
| Vector retrieval | Reusable capability | Retrieve candidates by vector similarity under declared metric and filtering semantics | Separate from embedding generation and vector storage |
| Vector index / store | Storage/index implementation | Store vectors and provide backend-specific search primitives | Backend implementations behind stable storage/search contracts |
| Filtering | Reusable capability when shared | Apply metadata, authorization, tenant, and other filters | Security filters must be authoritative; do not treat them as optional ranking hints |
| Hybrid retrieval | Reusable capability | Combine candidate sets from lexical, vector, and other retrieval paths | Composition/orchestration contract; may run in-process |
| Ranking | Reusable capability | Order candidates under explicit features and ranking semantics | Shared contract can support lexical, vector, and hybrid retrieval |
| Score normalization | Implementation component initially | Transform scores to a defined comparable representation | Promote only if independently reused or governed |
| Reranking | Reusable capability | Reorder candidates using a replaceable reranking strategy/model | Contract should define candidate limits, evidence, and failure behavior |
| Deduplication | Reusable capability when shared | Identify or collapse duplicate/equivalent candidates under declared rules | May be an in-process stage in a pipeline |
| Result fusion | Algorithm implementation or reusable capability | Combine ranked lists using a declared fusion strategy | RRF and alternatives are algorithms, not automatically services |
| Retrieval evaluation | Reusable capability | Measure retrieval quality using declared datasets and metrics | Separate from online retrieval path; preserve evaluation provenance |
| Query / corpus telemetry | Observability capability | Record configured measurements and diagnostic evidence | Explicit opt-in; not hidden side effects of retrieval contracts |

### Retrieval boundary example

A reasonable initial composition is:

```text
Query
  -> Query Parsing (optional shared capability)
  -> Retrieval Coordinator
       -> Lexical Retrieval
            -> Lexical Index
            -> Corpus Statistics
            -> Scoring Strategy (e.g., BM25)
       -> Vector Retrieval
            -> Embedding Generation
            -> Vector Index
  -> Candidate Filtering / Deduplication
  -> Ranking / Fusion
  -> Optional Reranking
  -> Results + provenance
```

This is a logical composition, not a deployment diagram. Components can execute in one process while retaining contracts and conformance tests. Extract a component to a remote service only when a concrete consumer, ownership, security, scaling, or lifecycle requirement justifies it.

## 5. Memory, graph, and knowledge

| Capability | Classification | Contract responsibility | Initial boundary guidance |
|---|---|---|---|
| Memory | Domain service | Store, retrieve, update, expire, and rank memories under explicit memory semantics | Foundational service boundary; storage backends are implementations |
| Memory retrieval policy | Reusable capability | Apply source/lineage/recency/relevance priority rules | Keep policy separate from storage mechanics |
| Memory persistence backend | Implementation | Persist memory records and indexes | RAM and filesystem implementations can satisfy the same contract; other backends may follow |
| Memory decorators | Decorator | Add caching, metrics, tracing, or explicitly enabled behavior | Same public contract; explicit configuration and order |
| Graph | Domain service / contract family | Represent entities, relations, traversal, and graph operations | Keep graph semantics separate from a particular graph database |
| Graph storage adapter | Adapter / implementation | Map graph contract to a chosen backend | Backend choice must not redefine graph semantics |
| Knowledge runtime | Domain service candidate | Manage runtime knowledge objects, sources, relationships, and lifecycle | Distinguish runtime knowledge operations from the human-readable FlossWare documentation vault |
| Knowledge acquisition | Reusable capability | Bring external/local source material into a knowledge corpus with provenance | Compose discovery, acquisition, extraction, chunking, embedding, and indexing |
| Knowledge retrieval | Reusable capability | Retrieve relevant knowledge with evidence and provenance | Compose lexical/vector/hybrid retrieval; do not duplicate ranking implementations |
| Lineage / provenance | Shared contract capability | Trace source-to-derived artifact and execution relationships | May be embedded in records initially; use a shared contract where multiple domains consume it |

Memory, Graph, and Knowledge are domain-level concerns. Their sub-capabilities should not automatically become additional deployments. Their contracts should allow composition and backend substitution.

## 6. Learning and optimization

| Capability | Classification | Contract responsibility | Initial boundary guidance |
|---|---|---|---|
| Learning | Domain service / capability family | Update learned state from explicitly supplied evidence and outcomes | Separate learned state and policy from execution mechanics |
| Thompson sampling | Strategy implementation | Select actions using posterior state and declared reward semantics | Algorithm implementation behind a strategy contract, not automatically a service |
| Reward attribution | Reusable capability | Associate outcomes/rewards with the decision and evidence that produced them | Preserve provenance and delayed-feedback semantics |
| Evaluation | Reusable capability | Judge results against explicit criteria | May be shared by learning and orchestration without being owned by either |
| Parameter optimization | Reusable capability | Search a defined parameter space against an objective | Optimization algorithms remain replaceable implementations |
| Genetic optimization | Strategy implementation / capability | Apply genetic search to a declared optimization problem | Separate contract only if independently reusable; algorithm name alone is insufficient |
| Feedback ingestion | Reusable capability candidate | Validate and normalize feedback before updating learned state | Keep external sources behind adapters |
| Experiment tracking | Observability / evaluation capability | Associate configuration, data, outcome, and provenance for comparisons | Explicitly enabled; avoid hidden persistence |

## 7. Cross-cutting capabilities

| Capability | Classification | Boundary guidance |
|---|---|---|
| Storage contracts | Reusable capability family | Define semantics independent of RAM, filesystem, PostgreSQL, Redis, or other backend |
| Cache | Decorator / reusable capability | Explicitly enabled; specify invalidation and consistency |
| Resilience | Decorator / reusable capability | Retry, timeout, circuit-breaker, and fallback behavior must respect contract semantics |
| Observability | Decorator / adapter | Metrics, tracing, and logging must be opt-in where they introduce side effects |
| Security / authorization | Cross-cutting contract capability | Enforce at the capability boundary; do not rely on client behavior |
| Configuration | Cross-cutting capability | Configuration is the source of truth; defaults remain minimal |
| REST adapter | Adapter | Maps a capability contract to HTTP semantics without duplicating business logic |
| MCP adapter | Adapter | Exposes appropriate capabilities to agent clients with explicit authorization |
| Event adapter / publisher | Adapter | Supports asynchronous integration where needed; event publication remains explicit |
| Streaming | Adapter / reusable capability | Separate streaming protocol mechanics from domain operation semantics |
| Conversation state | Domain capability candidate | Keep distinct from durable memory and execution evidence unless semantics explicitly unify them |
| Structured output | Reusable capability | Validate declared schema and report contract-defined failures |
| TUI schema / terminal UI | Adapter and presentation capability | Keep presentation separate from domain services |
| Setup / bootstrap | Delivery capability | Installation and client setup do not belong in runtime domain contracts |

## Promotion rules

A component should be promoted from implementation detail to reusable capability when there is evidence of one or more of the following:

- Multiple independent consumers need the same behavior.
- The behavior has a coherent contract, invariants, and conformance tests.
- It has an independent owner, lifecycle, security boundary, or configuration.
- Multiple implementations would be valuable without changing consumers.
- Independent operation, scaling, availability, or resource isolation is required.

A reusable capability should be promoted to an independently deployed service only when remote access or operational isolation is justified. A service does not automatically need its own repository. Repository separation follows contract governance, language implementation boundaries, and release ownership.

## Required fields for future inventory entries

For every significant capability, record:

- **Name and classification**
- **Responsibility and explicit non-responsibilities**
- **Contract repository / contract status**
- **Consumers and dependencies**
- **Owned data, policy, and invariants**
- **Known implementations and conformance tests**
- **Protocol adapters (REST, MCP, events, or none)**
- **Current deployment placement**
- **Evidence and open questions**

Do not infer implementation status from an architecture label. Update the inventory when a contract or implementation lands, and distinguish accepted decisions from proposals.
