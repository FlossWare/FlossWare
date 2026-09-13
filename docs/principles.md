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

## Hard constraints before optimization

Authorization, policy, capability, budget, quota, rate, and availability constraints define the feasible set. Learned or adaptive strategies may optimize within that set, never around it.

## Provenance is part of the data

Acquisition, derivation, execution, evaluation, and retrieval should retain enough provenance to explain where an artifact or result came from.

## AI-assisted engineering is explicit

AI participates through defined contracts, workflows, and validation rather than uncontrolled automation. Architectural decisions remain explicit and reviewable.

## Knowledge is maintained

Architecture, decisions, lessons, research, and provenance are maintained as durable human-readable Markdown. They are versioned with Git and can be authored in Obsidian.

## Current authority

- **Knowledge:** the Git-backed FlossWare Markdown vault
- **Public presentation:** the FlossWare GitHub Pages site
- **Executable implementation:** the appropriate FlossWare software repository
- **Historical material:** Git history and explicitly marked historical documentation

There should be one knowledge corpus, not parallel manually synchronized copies.
