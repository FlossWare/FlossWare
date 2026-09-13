---
title: Contributing Guide
---

# Contributing Guide

Thank you for your interest in contributing to the orchestration framework. This guide covers everything you need to know to get started, submit changes, and participate in the project.

## How to Contribute

Contributing follows four steps:

1. **Find or create an issue.** Check existing issues for something you want to work on, or open a new issue describing the change you want to make. Discuss the approach before writing code.

2. **Fork and branch.** Fork the repository, create a feature branch, and implement your changes following the [coding standards](coding_standards.md).

3. **Submit a pull request.** Push your branch and open a pull request against `main`. Fill out the PR template completely and ensure all checks pass.

4. **Respond to review.** Address feedback from the multi-AI review pipeline and human maintainers. Iterate until the change is approved.

## Code Review Process

All pull requests go through a 4-phase multi-AI review pipeline before human review. This ensures consistent quality and catches issues that single-reviewer processes miss.

### Phase 1: Automated Analysis

Static analysis, linting, and test execution run automatically when you open a PR. All checks must pass before the AI review begins.

### Phase 2: Multi-Model Review

Three to six AI models independently review the code changes. Each model examines correctness, style adherence, security implications, and performance characteristics. The models used for review are always different from those used in Phase 3.

### Phase 3: Meta-Review

A separate set of strong models (with zero overlap from Phase 2) synthesize the individual reviews. They identify consensus findings, resolve conflicting recommendations, and produce a unified review summary.

### Phase 4: Human Review

A human maintainer reviews the PR alongside the AI-generated summary. The maintainer makes the final accept/reject decision. AI review informs but does not replace human judgment.

## Branch Workflow

The project uses a simple branching model:

- **`main`** is the primary branch. Every commit to `main` is a release candidate. The branch is always kept in a deployable state.

- **Feature branches** are created from `main` for all changes. Use descriptive names like `add-openrouter-scraper` or `fix-consensus-timeout`.

- **Git worktrees** are encouraged for working on multiple features simultaneously without switching branches:

```bash
# Create a worktree for a new feature
git worktree add ../feature-new-scraper -b add-new-scraper

# Work in the worktree directory
cd ../feature-new-scraper

# Remove the worktree when done
git worktree remove ../feature-new-scraper
```

Do not create git tags. All versioning is handled by the maintainers.

## Pull Request Guidelines

Every pull request must use the following template:

```markdown
## Summary
- Brief description of what the change does
- Why this change is needed
- Any notable implementation decisions

## Test Plan
- [ ] Unit tests added or updated
- [ ] Integration tests pass locally
- [ ] Manual testing performed (describe steps)
- [ ] No regressions in existing functionality
```

Additional guidelines for pull requests:

- Keep PRs focused on a single change. Split unrelated work into separate PRs.
- Include code examples or screenshots where they help explain the change.
- Link the related issue in the PR description.
- Ensure the PR title is concise and under 70 characters.
- Use the description body for details, not the title.

## Areas for Contribution

Contributions are welcome in the following areas:

- **New scrapers.** Add scrapers for additional data sources. Subclass `BaseScraper` and implement the `scrape()` method. See the [getting started guide](getting_started.md) for an example.

- **GA optimizers.** Improve or add genetic algorithm optimization strategies for model routing and task distribution.

- **Consensus strategies.** Implement new consensus mechanisms beyond majority vote, such as weighted confidence scoring or Bayesian aggregation.

- **Documentation.** Improve existing documentation, add tutorials, write examples, or fix errors in the docs.

- **Performance.** Profile and optimize hot paths in the orchestrator, reduce API latency, or improve caching.

- **Integrations.** Add support for new AI model providers, monitoring backends, or external services.

## Development Setup

```bash
# Clone the repository
git clone https://github.com/FlossWare/claude-orchestrator.git
cd claude-orchestrator

# Install Node.js dependencies
npm install

# Install Python dependencies
pip3 install -r requirements.txt
```

Verify your setup by running the test suite:

```bash
npm test
```

See the [getting started guide](getting_started.md) for prerequisite details and first steps.

## Communication

- **Issues** are the primary channel for bug reports, feature requests, and design discussions. Search existing issues before opening a new one.

- **Pull requests** are for code review and implementation discussion. Keep PR conversations focused on the code changes.

Be clear and constructive in all communication. Provide context, include reproduction steps for bugs, and explain the rationale behind feature requests.

## Code of Conduct

All participants are expected to behave professionally and respectfully. Harassment, discrimination, and hostile behavior are not tolerated. Treat others the way you want to be treated. Maintainers reserve the right to remove anyone who violates these expectations.

## License

Contributions are accepted under the same license as the project. By submitting a pull request, you agree that your contributions will be licensed under the project's existing license terms. See the `LICENSE` file in the repository root for details.
