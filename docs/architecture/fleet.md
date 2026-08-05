---
title: Distributed Fleet
---

# Distributed Fleet

The fleet is a heterogeneous cluster of 9 machines (1 controller + 8 workers) that distributes LLM API requests, consensus evaluations, and background tasks across physical hardware. Every machine runs a lightweight agent process that accepts work over SSH. There are no containers, no Kubernetes, and no cloud dependencies -- just SSH, systemd, and Ansible.

---

## Fleet Topology

```
                            FLEET TOPOLOGY

                        +-------------------+
                        |     laptop-01     |
                        |  (Dev Workstation) |
                        |  NOT a worker     |
                        +--------+----------+
                                 |
                                 | SSH (dev access only)
                                 v
                        +-------------------+
                        |      aio-01       |
                        |   (Controller)    |
                        |  REST API :5000   |
                        |  PostgreSQL :5433 |
                        |  Redis :6379      |
                        +--------+----------+
                                 |
                 +---------------+---------------+
                 |               |               |
          +------+------+ +-----+-------+ +-----+-------+
          |             | |             | |             |
     +----+----+   +----+----+   +----+----+   +----+----+
     |server-01|   |server-02|   |server-03|   | pi-01   |
     | Tier 1  |   | Tier 1  |   | Tier 1  |   | Tier 3  |
     | 64GB    |   | 23GB*   |   | 64GB    |   | 8GB     |
     +---------+   +---------+   +---------+   +---------+

     +----+----+   +----+----+   +----+----+   +----+----+
     | pi-02   |   | pi-03   |   | pi-04   |   | pi-05   |
     | Tier 2  |   | Tier 3  |   | Tier 3  |   | Tier 3  |
     | 8GB     |   | 4GB     |   | 4GB     |   | 4GB     |
     | Grafana |   |         |   |         |   |         |
     +---------+   +---------+   +---------+   +---------+

  * server-02: 32GB installed, 23GB usable (DIMM issue)
```

---

## Node Roles

### Controller: aio-01

The controller is the single point of coordination. It runs:

- **REST API** (port 5000): Accepts task submissions, routes to workers, returns results.
- **PostgreSQL** (port 5433): Stores execution logs, Thompson Sampling state, workflow records, experience memory with pgvector embeddings.
- **Redis** (port 6379): Consensus caching, rate limiting counters, ephemeral state.
- **Scheduler**: Cron-driven background tasks (model discovery, feedback loop analysis, database backups).

The controller never executes LLM API calls itself. It dispatches to workers and synthesizes their responses.

### Workers: 8 Nodes

Workers receive tasks from the controller over SSH, execute LLM API calls against external providers (OpenRouter, Anthropic, Google, Groq, Cerebras, DeepSeek), and return results. Each worker runs a systemd-managed agent process that:

1. Listens for incoming SSH commands from the controller.
2. Executes the task (typically a single LLM API call with retry logic).
3. POSTs results back to the controller at `aio-01:5000`.
4. Reports health metrics on demand.

Workers never access PostgreSQL directly. All persistence goes through the controller's REST API.

---

## Worker Tiers

Workers are grouped into three tiers based on hardware capability. Tier assignment affects task routing: compute-heavy tasks (long-context synthesis, large batch processing) go to Tier 1 first.

| Tier | Nodes | RAM | CPU | Primary Use |
|------|-------|-----|-----|-------------|
| Tier 1 | server-01, server-02, server-03 | 23-64 GB | x86_64, multi-core | Heavy API workloads, batch processing, long-context tasks |
| Tier 2 | pi-02 | 8 GB | ARM (RPi 4/5) | Monitoring (Grafana/Prometheus), light API tasks, orchestrator |
| Tier 3 | pi-01, pi-03, pi-04, pi-05 | 4-8 GB | ARM (RPi 4/5) | Lightweight API relay, health checks, scraping |

### Tier-Specific Notes

- **server-01**: Requires `powersave` CPU governor or the cooling fan runs excessively loud. Ansible enforces this on deploy.
- **server-02**: Has a DIMM issue -- 32 GB installed but only 23 GB usable by the OS. Sufficient for API relay but limits concurrent large-context requests.
- **server-03**: Full 64 GB, no hardware issues. Preferred for the heaviest workloads.
- **pi-02**: Runs Grafana (port 3000) and Prometheus (port 9090) in addition to worker duties. Monitoring takes priority over task execution.
- **pi-01**: Acts as SSH jump proxy to reach aio-01 from laptop-01. All SSH connections to the controller route through pi-01.

---

## SSH-Based Distribution

### Why SSH Over Kubernetes

The fleet uses SSH for task distribution instead of Kubernetes, Docker Swarm, or any container orchestration platform. The reasons are practical:

1. **Hardware heterogeneity**: The fleet spans x86_64 servers and ARM Raspberry Pis. A unified container runtime adds complexity without benefit when every node runs the same Python/Node.js agent.
2. **Simplicity**: SSH is already configured, authenticated, and tested. Adding a container layer introduces failure modes (image pulls, registry auth, overlay networking) with no corresponding gain for API-relay workloads.
3. **Resource efficiency**: Raspberry Pis with 4 GB RAM cannot afford the overhead of a kubelet or container runtime. The agent process uses under 50 MB.
4. **Security**: SSH key authentication with no password fallback. Keys are distributed via Ansible. No exposed ports beyond SSH (22) and the controller API (5000).

### SSH Configuration

All inter-node SSH uses a dedicated `claude-fleet` key pair. The controller's SSH config maps hostnames to IPs with connection pooling:

```
# ~/.ssh/config on aio-01 (controller)

Host server-01
  HostName 192.168.1.10
  User fleet
  IdentityFile ~/.ssh/claude-fleet
  ControlMaster auto
  ControlPath /tmp/ssh-%r@%h:%p
  ControlPersist 600
  ConnectTimeout 5
  ServerAliveInterval 15
  ServerAliveCountMax 3

Host server-02
  HostName 192.168.1.11
  User fleet
  IdentityFile ~/.ssh/claude-fleet
  ControlMaster auto
  ControlPath /tmp/ssh-%r@%h:%p
  ControlPersist 600

# ... repeated for all 8 workers
```

Connection multiplexing (`ControlMaster auto`) keeps a persistent SSH connection per worker, eliminating handshake overhead for rapid task dispatch. `ControlPersist 600` keeps idle connections alive for 10 minutes.

### SSH Jump Proxy

From the development workstation (laptop-01), the controller is reached through pi-01 as a jump proxy:

```
# ~/.ssh/config on laptop-01

Host aio-01
  HostName 192.168.1.5
  User fleet
  ProxyJump pi-01
  IdentityFile ~/.ssh/claude-fleet
```

Direct SSH or curl from laptop-01 to aio-01 is blocked at the network level. All traffic must transit pi-01.

---

## Health Monitoring

The controller polls each worker every 30 seconds with a lightweight health check. The check verifies:

1. **SSH connectivity**: Can the controller open a channel to the worker?
2. **Agent process**: Is the agent process running (checked via `pgrep`)?
3. **Disk space**: Is the worker above 10% free disk?
4. **Load average**: Is the 1-minute load average below 2x CPU count?
5. **API reachability**: Can the worker reach at least one external API provider?

```
              HEALTH CHECK STATE MACHINE

  +----------+     health check passes     +----------+
  |          | <-------------------------> |          |
  | HEALTHY  |                             | HEALTHY  |
  |          | --------------------------> |          |
  +----------+                             +----------+
       |
       | 1 consecutive failure
       v
  +----------+
  | DEGRADED |   Worker still receives tasks but
  |          |   is deprioritized in routing
  +----------+
       |
       | 3 consecutive failures
       v
  +----------+
  |  OFFLINE |   Worker removed from dispatch pool
  |          |   Alert logged to monitoring.health_events
  +----------+
       |
       | health check passes again
       v
  +----------+
  | RECOVERY |   Worker receives 1 test task; if it
  |          |   succeeds, transitions back to HEALTHY
  +----------+
```

Health status is stored in Redis and exposed via the `/fleet/health` REST endpoint. Grafana dashboards on pi-02:3000 visualize fleet health over time.

---

## Per-Node Circuit Breakers

Each worker has an independent circuit breaker that trips when the worker's error rate exceeds a threshold. This prevents a failing worker from consuming dispatch slots that could go to healthy workers.

| Parameter | Value | Description |
|-----------|-------|-------------|
| Failure threshold | 5 errors in 60 seconds | Trips the circuit breaker to OPEN |
| Open duration | 30 seconds | Time before attempting a probe request |
| Half-open probes | 1 request | Number of test requests before closing |
| Success threshold | 2 consecutive successes | Required to transition from HALF-OPEN to CLOSED |
| Tracked errors | Timeout, SSH failure, 5xx from API | Errors that increment the failure counter |
| Ignored errors | 4xx from API, rate limit (429) | Errors that do NOT increment the counter |

Circuit breaker state is independent from health check state. A worker can be HEALTHY (SSH is fine, agent is running) but have an OPEN circuit breaker (the APIs it is calling are returning errors).

---

## Ansible Deployment

The fleet is managed entirely through Ansible. There is no manual SSH-and-configure workflow. All configuration, package installation, service management, and key distribution is codified in playbooks.

### Roles (11 total)

| Role | Applied To | Purpose |
|------|-----------|---------|
| `common` | All nodes | Base packages, NTP, timezone, locale, SSH hardening |
| `fleet-agent` | All workers | Install and configure the worker agent process |
| `controller` | aio-01 | REST API, scheduler, Redis, dispatcher |
| `postgresql` | aio-01 | PostgreSQL 16 with pgvector extension |
| `redis` | aio-01 | Redis 7 with persistence and maxmemory policy |
| `monitoring` | pi-02 | Grafana, Prometheus, node_exporter |
| `node-exporter` | All nodes | Prometheus node_exporter for hardware metrics |
| `ssh-keys` | All nodes | Distribute fleet SSH keys, configure authorized_keys |
| `cpu-governor` | server-01 | Set powersave governor to control fan noise |
| `nfs-client` | All workers | Mount shared NFS volume from NAS |
| `scraper` | Workers with scraper duty | Install and configure scraping tools |

### Deployment Playbook (10 phases)

The `site.yml` playbook runs all roles in a defined order with rolling updates to avoid fleet-wide downtime:

```yaml
# site.yml - 10-phase deployment

# Phase 1: Pre-flight checks
- hosts: all
  tasks:
    - name: Verify SSH connectivity
    - name: Check disk space (>10% free)
    - name: Verify NTP synchronization

# Phase 2: Common baseline
- hosts: all
  roles:
    - common
    - node-exporter
    - ssh-keys

# Phase 3: Controller infrastructure
- hosts: controller
  roles:
    - postgresql
    - redis
    - controller

# Phase 4: Monitoring stack
- hosts: monitoring
  roles:
    - monitoring

# Phase 5: Worker agents (rolling, 2 at a time)
- hosts: workers
  serial: 2
  roles:
    - fleet-agent
    - nfs-client

# Phase 6: Hardware-specific overrides
- hosts: server-01
  roles:
    - cpu-governor

# Phase 7: Scraper deployment
- hosts: scraper_nodes
  roles:
    - scraper

# Phase 8: Database migrations
- hosts: controller
  tasks:
    - name: Run pending PostgreSQL migrations

# Phase 9: Health verification
- hosts: all
  tasks:
    - name: Verify agent process running
    - name: Verify controller API responding
    - name: Verify Grafana dashboards loading

# Phase 10: Post-deploy notification
- hosts: controller
  tasks:
    - name: Log deployment to monitoring.deployments table
    - name: Update fleet version in /fleet/status endpoint
```

Workers are deployed in rolling batches of 2 (`serial: 2`) so that at least 6 workers remain available during deployment. If a batch fails, Ansible halts before proceeding to the next batch.

---

## Scaling: Adding a Worker

Adding a new worker to the fleet is a 5-step process:

1. **Hardware setup**: Install the OS (Fedora for x86_64, Raspberry Pi OS for ARM), ensure SSH access with password authentication temporarily.
2. **Inventory update**: Add the new host to the Ansible inventory file under the appropriate tier group (`tier1_workers`, `tier2_workers`, or `tier3_workers`).
3. **Run Ansible**: Execute `ansible-playbook site.yml --limit new-worker-hostname`. This installs all required packages, distributes SSH keys, configures the agent, and mounts NFS.
4. **Controller registration**: The controller auto-discovers new workers on the next health check cycle (within 30 seconds) or can be prompted via `POST /fleet/workers/refresh`.
5. **Verification**: Check `GET /fleet/health` to confirm the new worker appears as HEALTHY. Run a test consensus request and verify the new worker receives dispatches.

### Removing a Worker

1. **Drain**: `POST /fleet/workers/{hostname}/drain` -- the controller stops dispatching new tasks but allows in-flight tasks to complete.
2. **Wait**: Monitor `/fleet/workers/{hostname}/status` until `in_flight_tasks: 0`.
3. **Remove from inventory**: Delete the host from the Ansible inventory.
4. **Controller cleanup**: `DELETE /fleet/workers/{hostname}` removes the worker from the dispatch pool and archives its historical metrics.

---

## Graceful Degradation

The fleet is designed to continue operating with reduced capacity as workers go offline. There is no single worker whose failure causes a system-wide outage (the controller is a single point of failure, but it runs on the most reliable hardware with daily backups).

| Workers Available | Impact | Automatic Response |
|-------------------|--------|--------------------|
| 8 of 8 | None -- full capacity | Normal operation |
| 6-7 of 8 | Minimal -- slightly longer queue times | Reduce max parallel consensus workers from 6 to available count |
| 4-5 of 8 | Moderate -- consensus uses fewer models | Reduce consensus pool size, prioritize Tier 1 workers |
| 2-3 of 8 | Significant -- single-model fallback likely | Disable batch operations, use single-model with retry |
| 1 of 8 | Severe -- no consensus possible | All requests go to single worker, confidence always "low" |
| 0 of 8 | Outage -- controller returns 503 | Alert via monitoring, wait for recovery |

---

## Operational Constraints

- **Max parallel workers per request**: 3 (prevents a single large request from monopolizing the fleet).
- **Health check timeout**: 2000 ms. If a worker does not respond within 2 seconds, it counts as a failure.
- **NFS required**: Workers expect the shared NFS mount at `/mnt/aio-01/`. If NFS is unavailable, the worker falls back to local storage but logs a warning.
- **API keys**: Stored in the controller's secrets store and injected into worker environments at dispatch time. Workers never persist API keys to disk.
- **Clock synchronization**: All nodes must be within 5 seconds of each other (enforced by NTP via the `common` Ansible role). Token budget timestamps and execution logs depend on synchronized clocks.
- **Deployment window**: Rolling deploys take approximately 8 minutes for the full fleet. During this window, capacity is reduced by 2 workers (the current rolling batch).
- **cabin-laptop-02**: Exists on the network but is exclusively for scraping workloads. It is never added to the general worker pool and never receives consensus dispatch.
