# Temporal Training Outline

**20 hours · 5 days × 4 hours**
Audience: Software engineers transitioning from Airflow · Python & Java · Kafka-heavy stacks

---

> **Tags used below**
> `[airflow]` Migration/comparison note · `[kafka]` Kafka integration · `[lab]` Hands-on exercise

---

## Day 1 — Foundations: Rethinking orchestration

### Morning · 2 hrs

- **Why Temporal exists — the failure modes of cron-based orchestration** `[airflow]`
  - DAG mental model vs. code-as-workflow
  - What Airflow solves and where it breaks down at scale

- **Core Temporal concepts — Workflows, Activities, Workers, Task Queues**
  - Mapping Airflow primitives: DAG → Workflow, Operator → Activity, Executor → Worker

- **Event sourcing and deterministic replay — the foundation everything else rests on**
  - How Temporal's history log differs from Airflow's XCom + task state database

- **Temporal architecture overview**
  - Server components: Frontend, History, Matching, Worker services

### Afternoon · 2 hrs

- **Local dev setup — Temporal CLI, Docker Compose, Temporal Web UI** `[lab]`
  - Run a local cluster; inspect the Web UI; compare to Airflow's UI mental model

- **First Workflow in Python (and Java stub) — "Hello Temporal"** `[lab]`
  - SDK setup, `@workflow.defn`, `@activity.defn`, `WorkflowClient`
  - Side-by-side with equivalent Airflow DAG

- **Reading the Event History — understanding what replay actually does** `[lab]`
  - Use Temporal CLI to dump history; trace execution step by step

---

## Day 2 — Building reliable workflows

### Morning · 2 hrs

- **Calling Activities asynchronously — explicit focus** `[lab]`
  - `execute_activity` vs. `start_activity`; Activity handles; cancellation
  - Python: `asyncio`-based execution model
  - Java: `Promise`-based execution model

- **Executing parallel Activities** `[lab]`
  - Python: `asyncio.gather` across multiple activity handles
  - Java: `Promise.allOf` / `Promise.anyOf`
  - Fan-out/fan-in pattern; aggregating results

- **Activity retries, timeouts, and heartbeating**
  - `RetryPolicy` vs. Airflow's `retry` + `retry_delay`
  - `Schedule-to-Close` vs. `Start-to-Close`; why heartbeating is not optional for long-running work

- **Determinism constraints — the rules that keep replay honest**
  - No `random`, no `time.now()`, no external calls in Workflow code
  - How to test for violations

### Afternoon · 2 hrs

- **Signals and Queries — external interaction with running Workflows** `[lab]`
  - Build a workflow that waits for an approval signal
  - Query its state without interrupting it

- **Updates (Workflow Update API) — synchronous request/response against a running Workflow** `[lab]`

- **CRON Jobs and Schedules — replacing Airflow's scheduler** `[airflow]` `[lab]`
  - Cron expressions, interval schedules, jitter, catchup policy
  - Migrating Airflow `schedule_interval` and `catchup` patterns directly
  - Overlap policies: `Skip`, `BufferOne`, `AllowAll`, `Terminate`

- **Workflow timeouts and Child Workflows** `[airflow]`
  - `ExecutionTimeout` vs. `RunTimeout`; replacing `dagrun_timeout` and SLA misses
  - Child Workflows vs. Activities; replacing `SubDagOperator` and `TaskGroup`

---

## Day 3 — Kafka integration & event-driven patterns

### Morning · 2 hrs

- **Temporal + Kafka architecture patterns — two event systems, one platform** `[kafka]`
  - Where Temporal ends and Kafka begins
  - When to use Signals vs. consuming directly from a topic

- **Kafka consumer as a Temporal Activity — reliable at-least-once consumption** `[kafka]`
  - Commit offsets only after Activity succeeds
  - Heartbeat on long polls; idempotency keys

- **Producing to Kafka from Activities — transactional guarantees and outbox patterns** `[kafka]`

- **Replacing Kafka-triggered Airflow DAGs with Signal-driven Workflows** `[airflow]` `[kafka]`
  - A Kafka consumer bridge that sends Signals
  - Eliminating the REST API trigger layer

### Afternoon · 2 hrs

- **Lab — end-to-end Kafka → Temporal → Kafka pipeline** `[lab]` `[kafka]`
  - Order processing example: consume order event → run approval workflow → produce outcome event

- **Fan-out with Kafka partitions — parallelism inside a Workflow** `[lab]` `[kafka]`
  - `execute_activity` in parallel across a partition range (capped at 4–6 partitions)
  - Aggregating results back into the parent workflow

- **Dead-letter handling — Kafka DLQ vs. Temporal's built-in retry exhaustion** `[kafka]`
  - When to let Temporal own the retry vs. routing to a DLQ
  - Compensating transactions

---

## Day 4 — Production engineering

### Morning · 2 hrs

- **Workflow versioning — patching and the versioning API** `[airflow]`
  - The equivalent of Airflow's DAG versioning problem
  - Using `workflow.get_version()` and `patch()` safely during rolling deploys

- **Worker sizing and Task Queue design — throughput vs. isolation**
  - Sticky execution; separate queues for high-latency vs. fast activities
  - Autoscaling Worker deployments

- **Observability — Prometheus, Grafana, and OpenTelemetry** `[lab]`
  - Docker Compose: add Prometheus + Grafana alongside Temporal
  - Scraping the Worker metrics endpoint; key production SLO metrics
  - OpenTelemetry span propagation into Activities
  - Writing a custom Activity metric (counter, histogram)

- **Namespace strategy — multi-tenancy, isolation, and data retention**

### Afternoon · 2 hrs

- **Testing Workflows — the Temporal testing framework** `[lab]`
  - `TestWorkflowEnvironment`, mocking Activities, time-skipping for scheduled workflows
  - Runs fully in-process — no Docker required

- **Workflow replay testing — catching determinism regressions** `[lab]`
  - Replaying production history against new code before deploying
  - The `Replayer` API

- **Migrating Airflow DAGs — a decision framework** `[airflow]`
  - Which DAGs to migrate first (complexity × risk matrix)
  - Strangler fig pattern; running both systems in parallel
  - Common pitfalls and anti-patterns

---

## Day 5 — Advanced patterns & capstone

### Morning · 2 hrs

- **Real-world Workflow walkthrough — a non-trivial production example**
  - Structured code walkthrough of a multi-step, multi-activity workflow
  - Covers: retries, signals, child workflows, error handling, versioning — all together
  - Discussion: how would this have been built in Airflow, and what's different?

- **Advanced patterns**
  - Saga pattern: distributed transactions without two-phase commit; compensating Activities
  - Continue-as-New: handling very long-running or high-event-count Workflows
  - Infinite loop workflows; history size limits in practice

- **Spring Boot integration** `[lab]`
  - `temporalio/sdk-java` with Spring Boot autoconfiguration
  - Registering Workers and Activities as Spring beans
  - Wiring Temporal with Spring Kafka listeners for event-driven triggers
  - Configuration, health checks, graceful shutdown

### Afternoon · 2 hrs

- **Capstone — migrate a real Airflow DAG** `[lab]` `[airflow]` `[kafka]`
  - Teams receive a multi-step Airflow DAG with Kafka triggers
  - Redesign and implement it in Temporal end-to-end
  - Must include: schedules or signals, parallel activities, at least one retry policy

- **Capstone review — diff the two implementations, identify trade-offs**
  - What got simpler? What required more thought?
  - Code volume, error handling surface, observability

- **Q&A and open migration planning — bring your own DAG**
  - Teams present their real migration candidates
  - Group discussion on approach, phasing, and timeline

---

## Docker Compose stack reference

All labs can run on a laptop with Docker or pre-configured on VMs. Minimum recommended RAM: 8 GB.

| Service | Used from | Notes |
|---|---|---|
| `temporalio/auto-setup` | Day 1 | Temporal server + Web UI bundled |
| PostgreSQL | Day 1 | Temporal persistence backend |
| Kafka (KRaft, single broker) | Day 3 | Bitnami or Confluent image; no Zookeeper needed |
| Prometheus | Day 4 | Scrapes Worker metrics endpoint |
| Grafana | Day 4 | Pre-loaded Temporal dashboard |

> Day 4 testing labs use `TestWorkflowEnvironment` (in-process) — no server or Docker required.
> Cap Kafka partition count at **4–6** in the Day 3 fan-out lab to avoid CPU spikes on lower-spec machines.
