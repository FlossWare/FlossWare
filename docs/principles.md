---
title: Engineering Principles
---

# Engineering Principles

FlossWare favors systems that remain understandable, replaceable, and maintainable as they grow.

## Modular by default

Components should remain independently usable. A capability belongs behind a boundary rather than being absorbed into whichever component happens to call it first.

## Explicit behavior

The default behavior is nothing. Components should not implicitly publish events, create audit records, emit metrics, persist data, or trigger external actions. Infrastructure capabilities are enabled deliberately.

## Contracts over coupling

Stable interfaces, adapters, composition, decoration, and explicit bridges are preferred to copying implementation across repository boundaries.

## Contract before implementation

A language-neutral contract is authoritative for its semantics. Language implementations are realizations of that contract and are peers rather than competing authorities.

Where a contract family is split across repositories, use the FlossWare repository-layering convention:

    {contract}
    {contract}-{language}
    {contract}-{domain}
    {contract}-{domain}-{language}

The detailed normative contract-family naming rule is [FlossWare/standards ADR-0024](https://github.com/FlossWare/standards/blob/main/adr/ADR-0024-contract-centric-repository-layering-and-naming.md).

## Hard constraints before optimization

Authorization, policy, capability, budget, quota, rate, and availability constraints define the feasible set. Learned or adaptive strategies may optimize within that set, never around it.

## Provenance is part of the data

Acquisition, derivation, execution, evaluation, and retrieval should retain enough provenance to explain where an artifact or result came from.

## AI-assisted engineering is explicit

AI participates through defined contracts, workflows, and validation rather than uncontrolled automation. Architectural decisions remain explicit and reviewable.

## Knowledge is maintained

Architecture, decisions, lessons, research, and provenance are maintained as durable human-readable Markdown in [FlossWare/axium](https://github.com/FlossWare/axium). Obsidian is an authoring interface, not the repository's purpose.

## Current authority

- Normative standards: [FlossWare/standards](https://github.com/FlossWare/standards)
- Canonical knowledge corpus: [FlossWare/axium](https://github.com/FlossWare/axium)
- Public presentation: the FlossWare GitHub Pages repository
- Executable implementation: the appropriate FlossWare software repository
- Historical material: Git history and explicitly marked historical documentation

There should be one knowledge corpus, not parallel manually synchronized copies.
