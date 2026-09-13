---
title: Dogfooding and Engineering Lessons
---

# Dogfooding and Engineering Lessons

> **Current validation contract · 2026-09-13**

Architecture is not promoted because it looks good in a diagram. FlossWare uses executable dogfooding as evidence.

## Loom dogfood

The canonical path is:

```text
loom-ai/scripts/dogfood.sh
```

The current core gate validates the executable path through:

1. formatting
2. linting
3. tests
4. package build
5. Intent construction
6. Worker execution
7. Arbiter coordination
8. completion evaluation
9. successful end-to-end core execution

The core smoke path deliberately does **not** require provider credentials, databases, Redis, OrientDB, containers, or a local model.

## Engineering lesson

Prefer a real executable path over another architecture document.

A proposed abstraction earns durable architectural status when implementation and dogfood evidence demonstrate that the boundary is useful, stable, and composable.

## What dogfooding protects against

- architecture documents describing systems that no longer exist
- abstractions created only because another product has the same word
- hidden coupling between capabilities
- infrastructure requirements leaking into the core execution gate
- optimization preceding hard feasibility constraints

## Current principle

**Build the smallest useful boundary, exercise it through the real path, then document what the evidence teaches us.**
