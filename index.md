---
title: FlossWare
description: Free-first, modular infrastructure and AI-assisted engineering
---

<div class="fw-hero">
  <div class="fw-hero-copy">
    <img class="fw-logo" src="/assets/favicon.svg" alt="FlossWare" width="72" height="72">
    <p class="fw-kicker">OPEN SOURCE · AI · ENGINEERING</p>
    <h2>Software systems built to stay understandable.</h2>
    <p class="fw-lead">FlossWare is a free-first engineering ecosystem for modular infrastructure, AI-assisted systems, distributed services, and reusable open-source foundations.</p>
    <div class="fw-actions">
      <a class="fw-button fw-button-primary" href="https://github.com/FlossWare">Explore GitHub</a>
      <a class="fw-button fw-button-secondary" href="#architecture">Explore the architecture</a>
    </div>
  </div>
</div>

<div class="fw-stats">
  <div><strong>Open</strong><span>standards first</span></div>
  <div><strong>Modular</strong><span>loosely coupled systems</span></div>
  <div><strong>AI-first</strong><span>Loom execution substrate</span></div>
  <div><strong>Documented</strong><span>decisions and provenance preserved</span></div>
</div>

## What is FlossWare?

FlossWare builds reusable engineering foundations and reference implementations using open standards, explicit configuration, stable contracts, and loosely coupled architectures.

The goal is deliberately unfashionable: **make powerful systems understandable, replaceable, and maintainable.**

<div class="fw-cards">
  <a class="fw-card" href="docs/architecture.html">
    <span class="fw-card-icon">01</span>
    <h3>Architecture</h3>
    <p>Loom, Workers, Arbiters, model selection, capability boundaries, provenance, and the rules that keep the system composable.</p>
  </a>
  <a class="fw-card" href="docs/repositories.html">
    <span class="fw-card-icon">02</span>
    <h3>Repository boundaries</h3>
    <p>What belongs in each FlossWare repository and where integration should happen without copying implementation.</p>
  </a>
  <a class="fw-card" href="docs/knowledge.html">
    <span class="fw-card-icon">03</span>
    <h3>Knowledge</h3>
    <p>Acquisition, provenance, derived artifacts, retrieval, and the Git-backed human knowledge system.</p>
  </a>
</div>

## Architecture {#architecture}

<div class="fw-principles">
  <div><strong>Workers are fundamental.</strong><span>An Arbiter is a Worker that coordinates Workers.</span></div>
  <div><strong>Hard constraints first.</strong><span>Policy, capability, budget, quota, rate, and availability define feasibility.</span></div>
  <div><strong>Contracts over coupling.</strong><span>Capabilities evolve behind stable integration boundaries.</span></div>
  <div><strong>Evidence over diagrams.</strong><span>Executable dogfooding promotes ideas into durable architecture.</span></div>
</div>

```text
                         Intent
                           |
                        Arbiter
                       /      \
                  Worker    Worker
                       \      /
                        Arbiter
                           |
                        evidence
                           |
                       evaluation
                           |
                         result
```

### Model selection

```text
request
  -> candidates
  -> hard constraints
  -> feasible resources / models
  -> selection strategy
  -> invocation
  -> usage / cost / latency / outcome
  -> evaluation / reward
  -> strategy / knowledge update
```

### Architecture documentation

- [FlossWare Architecture](docs/architecture.html)
- [Repository Boundaries](docs/repositories.html)
- [Engineering Principles](docs/principles.html)
- [Dogfooding and Engineering Lessons](docs/dogfooding.html)
- [Knowledge Architecture](docs/knowledge.html)

## Loom

Loom is the AI-first execution substrate. It turns declarative intent into verified change while remaining deliberately small.

The core executable abstraction is the `Worker`. `Arbiter` coordination is ordinary Worker composition. External clients such as Crush, Claude Code, Codex, and Cursor remain outside Loom's internal execution model and consume Loom through explicit interfaces.

The canonical dogfood path is:

```text
loom-ai/scripts/dogfood.sh
```

The core gate exercises formatting, linting, tests, package build, Intent construction, Worker execution, Arbiter coordination, completion evaluation, and end-to-end core execution without requiring provider credentials, databases, Redis, OrientDB, containers, or a local model.

## Knowledge system

FlossWare keeps one human-readable knowledge corpus:

```text
Obsidian authoring
       |
       v
Git-backed Markdown
       |
       +------> GitHub source/history
       |
       +------> GitHub Pages presentation
```

Obsidian is the authoring interface. GitHub provides version history and public source. GitHub Pages presents the same Markdown as the public documentation site. There is no second manually synchronized copy of the knowledge base.

## Engineering principles

- **Modular by default** — components remain independently usable.
- **Explicit behavior** — infrastructure capabilities are enabled deliberately.
- **Contracts over coupling** — prefer stable interfaces and composition.
- **Hard constraints before optimization** — adaptive strategies operate only within the feasible set.
- **Provenance is part of the data** — derived artifacts remain traceable.
- **AI-assisted engineering is explicit** — automation operates through contracts and validation.
- **Knowledge is maintained** — architecture and decisions remain reviewable and versioned.

## Projects

<div class="fw-projects">
  <a href="https://github.com/FlossWare/loom-ai"><strong>loom-ai</strong><span>AI-first execution substrate built around Intent, Workers, Arbiters, evidence, and evaluation.</span></a>
  <a href="https://github.com/FlossWare/model-gateway"><strong>model-gateway</strong><span>Provider/model access, feasibility, routing, usage, cost, and provenance boundaries.</span></a>
  <a href="https://github.com/FlossWare/curses-tui"><strong>curses-tui</strong><span>Reusable terminal interaction primitives.</span></a>
</div>

## Historical material

Older documentation remains valuable as evidence of architectural evolution, but it is not current authority. In particular, older orchestration, fleet, provider, database, and learning documents may describe superseded systems.

**Current architecture wins. Historical documents explain how we got here.**

## About FlossWare

FlossWare is an open-source engineering effort led by **[Scot P. Floess (Flossy)](sfloess/)** spanning distributed systems, search, AI/ML, infrastructure automation, and open-source tooling.

For the professional résumé and background, visit [sfloess.github.io](https://sfloess.github.io).

---

<div class="fw-footer-cta">
  <strong>Build it. Document it. Keep it replaceable.</strong>
  <span>Free-first engineering with open interfaces and deliberate architecture.</span>
</div>
