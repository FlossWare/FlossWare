# FlossWare

FlossWare is an open-source engineering ecosystem for modular infrastructure, AI orchestration, reusable libraries, and engineering standards.

The organization is centered on **replaceable components, explicit contracts, provider independence, and documentation that preserves architectural intent**.

## Core projects

- **[loom-ai](https://github.com/FlossWare/loom-ai)** — the primary AI orchestration substrate. Loom provides provider-neutral contracts and pluggable implementations for models, storage, queues, search, graphs, tools, resources, execution, and related capabilities.
- **[engineering-standards](https://github.com/FlossWare/engineering-standards)** — canonical engineering principles, standards, and architecture decision records.
- **[knowledge](https://github.com/FlossWare/knowledge)** — private knowledge and supporting material that should not be treated as public project documentation.
- **[commons-java](https://github.com/FlossWare/commons-java)** — shared Java foundations used by FlossWare projects.

## Architecture direction

FlossWare no longer assumes that AI must be API-only or that a single provider, model, runtime, database, or deployment topology is the answer.

Instead, the architecture favors **capability contracts with swappable implementations**. Cloud inference, local inference, hosted services, and external tools can participate when they satisfy the relevant contract and engineering requirements.

Loom is the clearest expression of this direction. Its core remains deliberately small while integrations live behind protocols and optional backends.

```text
                         Applications / Agents
                                  |
                    SDK / CLI / REST / MCP adapters
                                  |
                         Loom orchestration
                    /       |       |        \
              execution  routing  consensus  context
                    \       |       |        /
                         Contracts / Protocols
                                  |
                  +---------------+----------------+
                  |               |                |
               Models          Data           Capabilities
            cloud/local     storage/search     tools/resources
                  |               |                |
                  +--------- replaceable ---------+
```

## Documentation

- [FlossWare architecture](docs/architecture/loom.md)
- [Repository map](docs/repositories.md)
- [Design philosophy](docs/philosophy.md)
- [Engineering standards](https://github.com/FlossWare/engineering-standards)

## Principles

1. **Contracts over coupling.** Stable interfaces are more valuable than implementation-specific assumptions.
2. **Replaceability by design.** Providers and infrastructure components should be swappable where practical.
3. **Capability over branding.** A component is selected for what it can reliably do, not for which vendor produced it.
4. **Minimal core, optional integrations.** Core functionality should not require every backend or service.
5. **Evidence over assertion.** Architecture and operational claims should be backed by tests, measurements, or explicit ADRs.
6. **Documentation is part of the system.** Architectural intent, boundaries, and tradeoffs are maintained alongside code.

## Related projects

FlossWare also maintains open-source infrastructure, utilities, Java libraries, and focused AI capability repositories. Some repositories are intentionally small or experimental and should not be mistaken for mature standalone products.

See the [repository map](docs/repositories.md) for current roles, visibility, and architectural relationships.
