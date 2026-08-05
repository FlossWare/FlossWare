---
title: Getting Started
---

# Getting Started

This guide walks you through setting up the orchestration framework, making your first API calls, and understanding the core concepts.

## Prerequisites

Ensure the following tools are installed before proceeding:

| Tool | Minimum Version | Purpose |
|------|----------------|---------|
| Node.js | 18+ | Runtime for orchestrator and workflows |
| Python | 3.10+ | Scrapers, ML components, analysis tools |
| Git | 2.30+ | Version control and worktree support |
| SSH | Any | Secure access to fleet nodes |
| curl | Any | API testing and health checks |

Verify your installations:

```bash
node --version    # v18.0.0 or higher
python3 --version # 3.10.0 or higher
git --version     # 2.30.0 or higher
ssh -V            # any version
curl --version    # any version
```

## Quick Start

Clone the repository and install dependencies:

```bash
git clone https://github.com/FlossWare/claude-orchestrator.git
cd claude-orchestrator
npm install
```

Verify the orchestrator is reachable:

```bash
curl -s http://aio-01:5000/health | jq .
```

Expected response:

```json
{
  "status": "healthy",
  "uptime": 86400,
  "workers": 8,
  "models_available": 213
}
```

## Making Your First Consensus Request

The consensus endpoint sends a prompt to multiple AI models and returns a synthesized result based on majority agreement.

```bash
curl -s -X POST http://aio-01:5000/consensus \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What are the three most important principles of distributed systems?",
    "num_models": 5,
    "strategy": "quality-first"
  }' | jq .
```

Response:

```json
{
  "consensus": "The three most important principles are: (1) fault tolerance...",
  "confidence": 0.87,
  "models_used": [
    "claude-sonnet-4",
    "gemini-2.5-flash",
    "llama-3.3-70b",
    "qwen-2.5-72b",
    "deepseek-chat"
  ],
  "agreement_ratio": 0.8,
  "duration_ms": 4200
}
```

The `strategy` field controls how models are selected. Use `quality-first` for important decisions and `cost-optimized` for routine tasks.

## Running a Workflow

Workflows define multi-step AI pipelines. Run a workflow with Node.js:

```bash
node workflows/code-review.mjs --file src/router.mjs
```

Workflows follow a standard structure:

```javascript
export const meta = {
  name: 'code-review',
  description: 'Multi-model code review with adversarial meta-review',
  version: '1.0'
};

export default async function run({ parallel, pipeline, agent, task }) {
  // Phase 1: Parallel review by 3 models
  const reviews = await parallel([
    agent('reviewer-1', { model: 'gemini-2.5-flash', task }),
    agent('reviewer-2', { model: 'llama-3.3-70b', task }),
    agent('reviewer-3', { model: 'qwen-2.5-72b', task })
  ]);

  // Phase 2: Meta-review by a different model (no overlap)
  const synthesis = await agent('meta-reviewer', {
    model: 'claude-sonnet-4',
    task: `Synthesize these reviews: ${JSON.stringify(reviews)}`
  });

  return synthesis;
}
```

Always define the `meta` block first. Use `parallel()` for concurrent execution and `pipeline()` for sequential steps.

## Adding a New Scraper

Scrapers collect data from external sources and store it through the orchestrator API. Create a new scraper by subclassing `BaseScraper`:

```python
from base_scraper import BaseScraper
from typing import List, Dict, Any

class GitHubReleaseScraper(BaseScraper):
    """Scrape release notes from GitHub repositories."""

    name = "github-releases"
    schedule = "daily"

    async def scrape(self) -> List[Dict[str, Any]]:
        """Fetch latest releases from configured repositories.

        Query the GitHub API for each tracked repository and return
        structured release data for storage.
        """
        results = []
        for repo in self.config["repositories"]:
            releases = await self.fetch_json(
                f"https://api.github.com/repos/{repo}/releases?per_page=5"
            )
            for release in releases:
                results.append({
                    "source": f"github:{repo}",
                    "title": release["name"],
                    "body": release["body"],
                    "published_at": release["published_at"],
                    "url": release["html_url"]
                })
        return results

    async def store(self, results: List[Dict[str, Any]]) -> int:
        """Store scraped results via the orchestrator API."""
        stored = 0
        for item in results:
            await self.post_to_api("/store", item)
            stored += 1
        return stored
```

Place scraper files in the `tools/` directory. They are invoked through the fleet API:

```bash
curl -s -X POST http://aio-01:5000/fleet/scrapers/start \
  -H "Content-Type: application/json" \
  -d '{"scraper": "github-releases"}'
```

## Exploring the Knowledge Base

The intelligent search API queries stored knowledge across multiple backends (PostgreSQL, vector similarity, and graph).

Search for relevant experiences:

```bash
curl -s -X POST http://aio-01:5000/search/intelligent \
  -H "Content-Type: application/json" \
  -d '{
    "query": "consensus timeout handling",
    "limit": 5
  }' | jq .
```

Search with filters:

```bash
curl -s -X POST http://aio-01:5000/search/intelligent \
  -H "Content-Type: application/json" \
  -d '{
    "query": "scraper failure recovery",
    "limit": 10,
    "source": "workflow",
    "min_confidence": 0.7
  }' | jq .
```

## Understanding Model Routing

The orchestrator uses Thompson Sampling to route requests to models. This is a multi-armed bandit algorithm that balances exploration (trying less-used models) with exploitation (favoring models that perform well).

Each model maintains a Beta distribution based on past successes and failures. The router samples from each distribution and picks the highest value, naturally shifting traffic toward better-performing models while still exploring alternatives.

Query available routing strategies:

```bash
curl -s http://aio-01:5000/api/strategies | jq .
```

The response includes all configured strategies and their current model rankings:

| Field | Description |
|-------|-------------|
| `strategy_name` | Name of the routing strategy (e.g., `quality-first`, `cost-optimized`) |
| `model_rankings` | Ordered list of models by current performance score |
| `alpha` | Success count (Beta distribution parameter) |
| `beta` | Failure count (Beta distribution parameter) |
| `total_requests` | Number of requests routed to this model |
| `avg_latency_ms` | Average response time in milliseconds |
| `success_rate` | Proportion of successful completions |

## Development Environment Setup

Install all dependencies for both Node.js and Python:

```bash
# Node.js dependencies
npm install

# Python dependencies
pip3 install -r requirements.txt

# Verify sentence-transformers for embeddings (optional)
python3 -c "from sentence_transformers import SentenceTransformer; print('OK')"
```

No environment variables are required for local development. The orchestrator endpoint (`aio-01:5000`) is the default and is resolved via the local network. API keys are managed centrally by the orchestrator and do not need to be configured on development machines.

## Key API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Orchestrator health check and uptime |
| `/consensus` | POST | Multi-model consensus on a prompt |
| `/fleet/status` | GET | Status of all fleet workers and available models |
| `/fleet/scrapers/start` | POST | Start a scraper by name |
| `/search/intelligent` | POST | Adaptive multi-source knowledge search |
| `/learning/experiences` | POST | Store a learning experience with embedding |
| `/api/strategies` | GET | List available routing strategies and rankings |
| `/store` | POST | Store arbitrary data (scraped content, results) |
| `/fetch` | POST | Extract clean text from a URL |
| `/fetch/batch` | POST | Extract clean text from multiple URLs |

## Next Steps

With your environment set up and the basics understood, explore these areas next:

- [Coding Standards](coding_standards.md) -- conventions for JavaScript, Python, and workflow patterns.
- [Contributing Guide](contributing.md) -- how to submit changes and participate in the project.
- [API Reference](/docs/api/) -- full documentation for all orchestrator endpoints.
- [Workflow Examples](/docs/workflows/) -- production workflow patterns and templates.
- [Architecture Overview](/docs/architecture/) -- how the fleet, orchestrator, and routing layer fit together.
