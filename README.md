# FlossWare

The FlossWare knowledge vault and public documentation site.

This repository is intentionally **Markdown-first** so the same knowledge can be authored locally in Obsidian, versioned with Git, reviewed on GitHub, and published through GitHub Pages.

## Repository roles

- **Markdown** is the human-readable knowledge corpus.
- **Obsidian** is the local authoring interface.
- **GitHub** provides version history, collaboration, and public source.
- **GitHub Pages** provides the formatted public presentation.
- **FlossWare software repositories** remain the executable source of truth for implementation, contracts, and tests.

## Documentation

- [Architecture](docs/architecture.html)
- [Repository boundaries](docs/repositories.html)
- [Engineering principles](docs/principles.html)
- [Knowledge architecture](docs/knowledge.html)
- [Dogfooding and engineering lessons](docs/dogfooding.html)

## Publishing

The site uses GitHub Pages with Jekyll and the existing Cayman-based FlossWare presentation layer. The Markdown is the content; Jekyll/CSS provide the presentation.

Public site: [flossware.org](https://flossware.org)

Historical documentation remains in the repository where it can be inspected, but current documentation is authoritative over superseded material.
