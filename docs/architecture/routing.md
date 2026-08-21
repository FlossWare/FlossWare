---
title: Model Routing
---

# Model Routing

Model routing determines which inference implementation and model should handle a task. In the current FlossWare architecture, routing is a **capability behind Loom contracts**, not a requirement that every deployment use the same model pool or algorithm.

## Routing flow

```text
                       Incoming Task
                            |
                            v
                  +--------------------+
                  | Task requirements   |
                  | context / latency   |
                  | tools / quality     |
                  +----------+---------+
                             |
                             v
                  +--------------------+
                  | Capability filter   |
                  | available models    |
                  | local/cloud         |
                  | provider health     |
                  +----------+---------+
                             |
                             v
                  +--------------------+
                  | Routing policy      |
                  | fixed / heuristic   |
                  | adaptive / custom   |
                  +----------+---------+
                             |
                             v
                    Selected inference
                             |
                             v
                       Execution /
                       verification
```

## Routing policies

Loom does not require one routing strategy. A deployment may choose:

- **Explicit selection** when the caller already knows the appropriate model.
- **Capability routing** when requirements map naturally to model capabilities.
- **Fallback routing** when availability and resilience are the primary concern.
- **Adaptive routing** when enough outcome data exists to learn which choices work best.
- **Custom routing** when a domain has specialized requirements.

## Thompson Sampling

FlossWare has explored Thompson Sampling as an adaptive selection mechanism. It maintains a probability distribution for each candidate choice and balances exploration with exploitation as evidence accumulates.

Thompson Sampling is useful when:

- there are multiple viable models or providers;
- outcomes can be measured consistently;
- performance changes over time;
- exploration can be bounded safely.

It is not automatically appropriate when the evaluation signal is weak, the candidate set is tiny, or the cost of experimentation is unacceptable.

## Local and cloud inference

Routing may select among hosted APIs, OpenAI-compatible gateways, local model servers, or other implementations satisfying the inference contract. Privacy, latency, cost, model quality, availability, and operational burden are routing inputs rather than reasons to impose a universal deployment model.

## Provider resilience

A routing implementation may incorporate provider health, rate limits, timeouts, circuit breakers, and fallback chains. These concerns belong to the routing and resilience implementation rather than to the core contract itself.

## Evidence and feedback

Adaptive routing should record enough evidence to answer:

1. What candidates were available?
2. Which policy selected the candidate?
3. What constraints or filters were applied?
4. What happened during execution?
5. How was the outcome evaluated?
6. Did the result update future routing decisions?

Without those answers, an adaptive router becomes a black box with a suspicious amount of confidence.

## Related documentation

- [Loom architecture](loom.md)
- [Orchestration layer](orchestration.md)
- [Consensus](consensus.md)
- [Thompson Sampling](../learning/thompson_sampling.md)
