---
title: Web Scraping
---

# Web Scraping

Web scraping feeds the knowledge system with current, domain-specific content that pre-trained models lack. Scrapers extract text from websites, APIs, and documentation portals, then deliver it into the ingestion pipeline for chunking, embedding, and graph integration.

## Why We Scrape

Pre-trained language models have a fixed knowledge cutoff. They hallucinate when asked about topics beyond their training data or after their cutoff date. Scraping addresses this through six mechanisms:

**RAG (Retrieval-Augmented Generation).** Scraped content becomes the retrieval corpus. When a model receives a query, relevant chunks are retrieved and injected into the prompt as context. The model generates answers grounded in actual documents rather than parametric memory.

**Factual grounding.** Scraped documentation provides authoritative answers. Instead of relying on a model's probabilistic recall of a configuration syntax, the system retrieves the actual documentation page.

**Currency.** Documentation changes. A library's API might be deprecated, a configuration option renamed, a security advisory published. Scraping captures these changes within hours rather than waiting months for the next model training run.

**Domain depth.** General-purpose models have shallow knowledge of niche domains (firmware reverse engineering, Salesforce metadata API, OpenWrt internals). Scraping specialist documentation and forums builds deep domain coverage.

**Hallucination reduction.** When the retrieval corpus contains the answer, the model copies rather than invents. Empirically, RAG with high-quality scraped content reduces hallucination rates from ~15% to ~3% on domain-specific questions.

**Auditability.** Every answer can be traced back to a source URL and scrape timestamp. Users can verify claims against the original source. This is impossible with parametric-only generation.

## Overview

The scraping system comprises **116+ individual scrapers** covering **15+ domains**, with a total corpus of **381,000+ documents** and growing.

Scrapers are Python scripts that inherit from `BaseScraper` and implement domain-specific extraction logic. They run on fleet worker nodes, coordinated by the controller, and deliver extracted content to the store API for ingestion.

## Knowledge Domains

| Domain                    | Example Scrapers                              | Sources                                            |
|---------------------------|-----------------------------------------------|----------------------------------------------------|
| Networking                | `openwrt_wiki`, `mikrotik_wiki`, `pfsense_docs` | OpenWrt Wiki, MikroTik Wiki, pfSense Docs         |
| Linux                     | `arch_wiki`, `gentoo_wiki`, `kernel_docs`      | Arch Wiki, Gentoo Wiki, kernel.org                 |
| Firmware                  | `openwrt_firmware`, `ddwrt_forum`              | OpenWrt downloads, DD-WRT forums                   |
| Security                  | `cve_details`, `nvd_feeds`, `exploit_db`       | CVE databases, NVD, Exploit-DB                     |
| Cloud / DevOps            | `aws_docs`, `k8s_docs`, `terraform_registry`  | AWS documentation, Kubernetes docs, Terraform      |
| Programming Languages     | `python_docs`, `mdn_web`, `rust_book`          | Python docs, MDN Web Docs, Rust Book               |
| Salesforce                | `sf_developer_docs`, `sf_metadata_api`         | Salesforce Developer Docs, Metadata API Reference  |
| AI / ML                   | `arxiv_papers`, `huggingface_docs`             | arXiv (cs.AI, cs.LG), Hugging Face documentation  |
| Hardware                  | `cpu_specs`, `memory_specs`, `pci_ids`         | WikiChip, JEDEC specs, PCI ID repository           |
| Databases                 | `postgres_docs`, `redis_docs`, `orientdb_docs` | PostgreSQL docs, Redis docs, OrientDB docs         |
| Standards / RFCs          | `ietf_rfcs`, `ieee_standards`                  | IETF RFC repository, IEEE Xplore                   |
| Operating Systems         | `fedora_docs`, `rhel_docs`, `freebsd_handbook` | Fedora Docs, RHEL documentation, FreeBSD Handbook  |

## BaseScraper Pattern

All scrapers inherit from `BaseScraper`, which provides HTTP session management, rate limiting, error handling, and output formatting.

```
 +-------------------+
 |    BaseScraper     |
 +-------------------+
 | - session          |
 | - rate_limiter     |
 | - output_dir       |
 +-------------------+
 | + run()            |
 | + fetch_page(url)  |
 | + save_item(item)  |
 | + get_urls()       |   <-- abstract
 | + parse(response)  |   <-- abstract
 +-------------------+
         ^
         |
 +-------------------+      +-------------------+      +-------------------+
 | OpenWrtWikiScraper |      |  ArchWikiScraper  |      | PostgresDocScraper|
 +-------------------+      +-------------------+      +-------------------+
 | + get_urls()       |      | + get_urls()       |      | + get_urls()       |
 | + parse(response)  |      | + parse(response)  |      | + parse(response)  |
 +-------------------+      +-------------------+      +-------------------+
```

### Implementing a Scraper

A complete scraper requires implementing two methods: `get_urls()` to discover pages and `parse()` to extract content:

```python
import re
from base_scraper import BaseScraper


class OpenWrtWikiScraper(BaseScraper):
    """Scrapes the OpenWrt Wiki for networking documentation."""

    NAME = 'openwrt_wiki'
    BASE_URL = 'https://openwrt.org'
    DOMAIN = 'networking'

    def get_urls(self):
        """Yield URLs to scrape by crawling the sitemap."""
        sitemap_url = f'{self.BASE_URL}/sitemap.xml'
        response = self.fetch_page(sitemap_url)
        urls = re.findall(r'<loc>(.*?)</loc>', response.text)

        for url in urls:
            # Filter to documentation pages only
            if '/docs/' in url or '/toh/' in url:
                yield url

    def parse(self, response):
        """Extract structured content from a wiki page."""
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(response.text, 'html.parser')

        # Remove navigation, sidebars, footers
        for element in soup.select('nav, .sidebar, footer, .toc'):
            element.decompose()

        title = soup.find('h1')
        content = soup.find('div', class_='dokuwiki')

        if not content:
            return None

        return {
            'title': title.get_text(strip=True) if title else 'Untitled',
            'content': content.get_text(separator='\n', strip=True),
            'url': response.url,
            'domain': self.DOMAIN,
            'source': self.NAME,
            'content_type': 'wiki',
        }


if __name__ == '__main__':
    scraper = OpenWrtWikiScraper()
    scraper.run()
```

The `run()` method (inherited from `BaseScraper`) iterates through `get_urls()`, calls `fetch_page()` and `parse()` for each URL, and passes results to `save_item()`.

## What save_item Triggers

When a scraper calls `save_item(item)`, it triggers a two-step delivery process:

```
 scraper.save_item(item)
         |
         v
 +------------------+       +------------------+
 | Write to disk    |       | Enqueue in Redis  |
 | (JSON file in    | ----> | (RPUSH to         |
 |  output_dir)     |       |  scraper:queue)   |
 +------------------+       +------------------+
         |                           |
         v                           v
 Durable backup              Store workers
 (survives restart)           pick up and ingest
```

1. **Write to disk**: The item is serialized to JSON and written to the scraper's output directory. This provides a durable backup -- if Redis or the store workers are down, no data is lost.

2. **Enqueue in Redis**: The item is pushed to a Redis list (`scraper:queue`). Store workers monitor this queue and process items into the knowledge pipeline (document storage, chunking, embedding).

This dual-write design means scraper output survives any downstream failure. The disk files can be replayed into Redis if the queue is lost.

## MediaWiki API Enumeration

Several knowledge sources (Arch Wiki, Gentoo Wiki) run on MediaWiki. Rather than crawling HTML, these scrapers use the MediaWiki API's `allpages` endpoint for complete, structured enumeration:

```python
def get_urls(self):
    """Enumerate all pages via MediaWiki API with cursor pagination."""
    api_url = f'{self.BASE_URL}/api.php'
    params = {
        'action': 'query',
        'list': 'allpages',
        'aplimit': 50,       # Pages per request
        'apnamespace': 0,    # Main namespace only
        'format': 'json',
    }

    while True:
        response = self.fetch_page(api_url, params=params)
        data = response.json()

        for page in data['query']['allpages']:
            page_title = page['title'].replace(' ', '_')
            yield f'{self.BASE_URL}/wiki/{page_title}'

        # Follow continuation cursor
        if 'continue' in data:
            params['apcontinue'] = data['continue']['apcontinue']
        else:
            break  # No more pages
```

The `apcontinue` cursor ensures complete enumeration without missing pages or hitting offset limits. Each request returns up to 50 page titles, and the cursor advances until the entire wiki is enumerated.

## Rate Limiting

Scrapers must be respectful of source websites. `BaseScraper` enforces several rate-limiting mechanisms:

**User-Agent identification.** All requests include a descriptive User-Agent header that identifies the bot and provides a contact URL:

```python
USER_AGENT = 'FlossWare-KnowledgeBot/1.0 (+https://github.com/FlossWare)'
```

**Per-request delays.** A configurable delay is inserted between requests. The default is 1 second; scrapers targeting APIs with explicit rate limits adjust this value:

```python
class BaseScraper:
    REQUEST_DELAY = 1.0          # seconds between requests
    REQUEST_TIMEOUT = 30         # seconds before timeout
    MAX_CONTENT_LENGTH = 10_000_000  # 10 MB max response size
```

**Timeout.** Requests that take longer than `REQUEST_TIMEOUT` seconds are aborted. This prevents a single hung connection from stalling the entire scraper.

**Maximum content length.** Responses larger than `MAX_CONTENT_LENGTH` (10 MB) are discarded. This prevents binary files (PDFs with embedded images, archives) from consuming memory and storage.

```python
def fetch_page(self, url, **kwargs):
    """Fetch a URL with rate limiting, timeout, and size checks."""
    time.sleep(self.REQUEST_DELAY)

    response = self.session.get(
        url,
        timeout=self.REQUEST_TIMEOUT,
        headers={'User-Agent': self.USER_AGENT},
        stream=True,
        **kwargs
    )
    response.raise_for_status()

    # Check content length before reading body
    content_length = int(response.headers.get('Content-Length', 0))
    if content_length > self.MAX_CONTENT_LENGTH:
        raise ContentTooLargeError(f'{url}: {content_length} bytes')

    return response
```

## Fleet Distribution

Scrapers are distributed across fleet worker nodes by the controller. The controller maintains a registry of scrapers and dispatches them via SSH:

```
 +-------------------+
 |    Controller     |
 |    (aio-01)       |
 +-------------------+
         |
         | SSH dispatch
         |
    +----+----+----+----+
    |    |    |    |    |
    v    v    v    v    v
 +----+ +----+ +----+ +----+ +----+
 |srv1| |srv2| |srv3| |pi-3| |pi-4|
 +----+ +----+ +----+ +----+ +----+
  openwrt arch   k8s   cve   postgres
  mikrotik gentoo aws  nvd   redis
  pfsense  fedora tf   exploitdb orientdb
```

Each worker runs a subset of scrapers. Assignment considers:

- **Node capacity**: Servers with more memory run scrapers that process large pages.
- **Network locality**: Scrapers targeting internal documentation run on nodes with internal network access.
- **Failure isolation**: Related scrapers (e.g., all security scrapers) run on the same node so a network issue affects one domain, not all.

The controller dispatches a scraper via:

```bash
ssh worker-node "cd /mnt/aio-01/claude-orchestrator/tools && python3 scrapers/openwrt_wiki.py"
```

Or via the REST API:

```bash
POST /fleet/scrapers/start
{
    "scraper": "openwrt_wiki",
    "worker": "server-01"
}
```

## Performance: Decoupled Pipeline

The original scraper architecture was **monolithic**: each scraper fetched, parsed, stored, chunked, and embedded content in a single process. This created a bottleneck where the slowest stage (embedding) throttled the fastest stage (fetching).

```
MONOLITHIC (original):

  fetch -> parse -> store -> chunk -> embed
  ~100ms   ~50ms   ~200ms   ~10ms   ~500ms

  Throughput: limited by slowest stage = ~599 docs/hr
```

The current architecture **decouples** scraping from processing. Scrapers only fetch and parse, writing results to the queue. Separate store workers handle ingestion, chunking, and embedding:

```
DECOUPLED (current):

  Scraper processes (parallel):        Store workers (parallel):
  fetch -> parse -> enqueue            dequeue -> store -> chunk -> embed
  ~100ms   ~50ms    ~1ms               ~1ms      ~200ms   ~10ms   ~500ms

  Scraper throughput: ~4700 docs/hr (limited by fetch, 3 parallel scrapers)
  Store throughput:   ~5000 docs/hr (limited by embed, 4 parallel workers)
```

This decoupling achieved a **7.8x improvement** in end-to-end throughput (599 to 4,700 docs/hr). The key insight is that fetching and embedding are independently parallelizable and should not block each other.

## Store Workers

Store workers consume items from the Redis queue and process them through the knowledge pipeline. They use a **claim-process-complete** protocol to prevent duplicate processing:

```
 Store Worker lifecycle:

 1. CLAIM:    RPOPLPUSH scraper:queue scraper:processing
              (atomically move item from queue to processing list)

 2. PROCESS:  Parse JSON -> store document -> chunk -> embed
              (full pipeline for one item)

 3. COMPLETE: LREM scraper:processing 1 item
              (remove from processing list)

 On failure:
 4. RETURN:   RPOPLPUSH scraper:processing scraper:queue
              (return item to queue for retry)
```

The `RPOPLPUSH` command is atomic -- it pops from one list and pushes to another in a single operation. This prevents two workers from claiming the same item.

If a worker crashes during processing, items remain in `scraper:processing`. A monitor job checks for items that have been in `scraper:processing` for more than 5 minutes and returns them to `scraper:queue` for retry.

Multiple store workers run in parallel (default: 4). Each worker processes items independently, and the Redis queue provides natural load balancing -- faster workers consume more items.

## Error Handling

Scrapers operate in hostile environments (unreliable networks, changing website structures, rate limiting). The error handling philosophy is: **no single failure should stop the scraper**.

### Non-Fatal Pipeline Failures

Errors during individual page processing are logged and skipped. The scraper continues with the next URL:

```python
def run(self):
    """Main scraper loop with non-fatal error handling."""
    success_count = 0
    error_count = 0

    for url in self.get_urls():
        try:
            response = self.fetch_page(url)
            item = self.parse(response)
            if item:
                self.save_item(item)
                success_count += 1
        except (RequestException, ParseError, ContentTooLargeError) as e:
            self.logger.warning(f'Skipping {url}: {e}')
            error_count += 1
            continue
        except KeyboardInterrupt:
            self.logger.info('Interrupted by user')
            break

    self.logger.info(f'Complete: {success_count} succeeded, {error_count} failed')
```

### Graceful Shutdown

Scrapers handle `SIGTERM` and `SIGINT` gracefully, finishing the current page before exiting:

```python
import signal

class BaseScraper:
    def __init__(self):
        self._shutdown = False
        signal.signal(signal.SIGTERM, self._handle_shutdown)
        signal.signal(signal.SIGINT, self._handle_shutdown)

    def _handle_shutdown(self, signum, frame):
        self.logger.info(f'Received signal {signum}, finishing current page...')
        self._shutdown = True

    def run(self):
        for url in self.get_urls():
            if self._shutdown:
                break
            # ... process url ...
```

This ensures that the current `save_item()` call completes, so no partially-written items enter the queue.

### Deduplication

The same URL may appear in multiple scraper runs (daily re-scrapes, overlapping sitemaps). Deduplication happens at two levels:

**URL-level deduplication.** Before fetching, the scraper checks whether the URL was already scraped recently:

```python
def should_scrape(self, url):
    """Check if URL needs re-scraping."""
    last_scraped = self.redis.get(f'scraped:{url}')
    if last_scraped:
        age_hours = (time.time() - float(last_scraped)) / 3600
        if age_hours < 24:  # Skip if scraped within 24 hours
            return False
    return True
```

**Content-level deduplication.** After parsing, a SHA-256 hash of the content is compared against stored hashes. If the content has not changed since the last scrape, the item is skipped:

```python
def save_item(self, item):
    content_hash = hashlib.sha256(item['content'].encode()).hexdigest()

    # Skip if content unchanged
    existing_hash = self.redis.get(f'content_hash:{item["url"]}')
    if existing_hash and existing_hash.decode() == content_hash:
        self.logger.debug(f'Content unchanged: {item["url"]}')
        return

    # Save and update hash
    self._write_to_disk(item)
    self._enqueue(item)
    self.redis.set(f'content_hash:{item["url"]}', content_hash)
```

This two-level approach prevents both redundant network requests (URL dedup) and redundant pipeline processing (content dedup).
