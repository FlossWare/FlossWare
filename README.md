# FlossWare

**Public documentation site and knowledge presentation for the FlossWare ecosystem.**

This repository contains Markdown-backed documentation published through GitHub Pages. It documents architecture, repository boundaries, engineering principles, and project knowledge for human readers.

## Repository roles

- **This repository (`FlossWare`)**: public documentation source and GitHub Pages site.
- **`kbase`**: private Obsidian knowledge corpus and canonical authoring source for private working notes.
- **`standards`**: normative engineering standards and architecture decisions.
- **`research`**: investigations and experiments that have not yet become standards or decisions.
- **Software repositories**: executable implementation, contracts, tests, and release artifacts.

The public site must not accidentally publish private `kbase` content. Only reviewed material intended for publication belongs here.

## Documentation

- [Architecture](docs/architecture.html)
- [Repository boundaries](docs/repositories.html)
- [Engineering principles](docs/principles.html)
- [Knowledge architecture](docs/knowledge.html)
- [Dogfooding and engineering lessons](docs/dogfooding.html)

## Publishing

The site uses GitHub Pages with Jekyll and the existing Cayman-based FlossWare presentation layer. Markdown is the content; Jekyll and CSS provide the presentation.

Public site: [flossware.org](https://flossware.org)

Historical documentation remains available for inspection, but current documentation is authoritative over superseded material.
