---
title: Distributed Fleet
---

# Distributed Fleet

> **Status: Historical / deployment-specific.** This document describes a former FlossWare fleet deployment. It is retained as operational history and must not be interpreted as the current Loom architecture.

## Historical context

The former deployment used a physical fleet centered on `aio-01`, with SSH-dispatched workers, Ansible-managed hosts, PostgreSQL/Redis services, and a fixed model-provider pool. That deployment was useful for early FlossWare orchestration and dogfooding, but those topology choices are not required by Loom.

The current architecture is contract-first and deployment-neutral. See [Loom Architecture](loom.md), [Orchestration](orchestration.md), and the [Repository Map](../repositories.md).

## What remains useful

The historical fleet demonstrates one possible deployment of orchestration capabilities across heterogeneous machines. Its operational lessons remain useful for evaluating:

- distributed execution;
- health monitoring and graceful degradation;
- infrastructure automation;
- model-provider resilience;
- resource-aware scheduling.

Those concerns should be implemented behind Loom contracts rather than encoded as universal assumptions about machine names, IP addresses, SSH keys, or a fixed worker count.

## Historical topology

The original deployment consisted of a controller and heterogeneous worker machines. The exact host inventory, addresses, hardware characteristics, SSH configuration, Ansible roles, and operational procedures are intentionally not reproduced here as current architecture. Git history preserves those details if historical investigation requires them.

## Migration guidance

New documentation must not state that:

- Loom requires `aio-01`;
- orchestration requires SSH dispatch;
- a fixed physical worker fleet is mandatory;
- a particular network topology is required;
- workers must use a specific provider pool;
- local or cloud inference is prohibited.

A deployment may still choose any of those mechanisms when they satisfy its requirements. They are implementation and operations decisions, not Loom's architecture.

## Current source of truth

- [Loom Architecture](loom.md)
- [Orchestration Layer](orchestration.md)
- [Model Routing](routing.md)
- [Repository Map](../repositories.md)
- [Engineering Standards](https://github.com/FlossWare/engineering-standards)
