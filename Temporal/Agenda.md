# Temporal Training Outline

**24 hours · 6 days × 4 hours**
Audience: Software engineers transitioning from Airflow · Java · Kafka-heavy stacks · Spring Boot

---

> **Tags used below**
> `[airflow]` Migration/comparison note · `[kafka]` Kafka integration · `[lab]` Hands-on exercise · `[aws]` AWS migration note · `[containers]` Container/Kubernetes pattern

---

## Day 1 — Foundations: Rethinking orchestration

### Morning · 2 hrs

- **Why Temporal exists — the failure modes of cron-based orchestration** `[airflow]`
- **Core concepts — Workflows, Activities, Workers, Task Queues**
  - Mapping Airflow primitives to Temporal equivalents
- **Event sourcing and deterministic replay**
- **Temporal architecture overview**

### Afternoon · 2 hrs

- **Local dev setup — Temporal CLI, Docker Compose, Temporal Web UI** `[lab]`
- **First Workflow in Java — "Hello Temporal"** `[lab]`
- **Reading the Event History** `[lab]`

---

## Day 2 — Building reliable workflows

### Morning · 2 hrs

- **Async and parallel Activity execution** `[lab]`
- **Retries, timeouts, and heartbeating**
- **Determinism constraints — the rules that keep replay honest**

### Afternoon · 2 hrs

- **Signals and Queries — interacting with running Workflows** `[lab]`
- **Updates — synchronous request/response against a running Workflow** `[lab]`
- **Schedules — replacing Airflow's scheduler** `[airflow]` `[lab]`
- **Child Workflows and workflow timeouts** `[airflow]`

---

## Day 3 — Kafka integration & event-driven patterns

### Morning · 2 hrs

- **Temporal + Kafka architecture patterns** `[kafka]`
- **Kafka consumer as a Temporal Activity** `[kafka]`
- **Producing to Kafka from Activities** `[kafka]`
- **Replacing Kafka-triggered Airflow DAGs with Signal-driven Workflows** `[airflow]` `[kafka]`

### Afternoon · 2 hrs

- **Lab — end-to-end Kafka → Temporal → Kafka pipeline** `[lab]` `[kafka]`
- **Fan-out with Kafka partitions** `[lab]` `[kafka]`
- **Dead-letter handling — DLQ vs. Temporal retry exhaustion** `[kafka]`

---

## Day 4 — Production engineering

### Morning · 2 hrs

- **Workflow versioning — patching, the versioning API, and safe deploys** `[airflow]`
- **Worker sizing, Task Queue design, and auto-tuning**
- **Observability — Prometheus, Grafana, and OpenTelemetry** `[lab]`
- **Namespace strategy — multi-tenancy, isolation, and data retention**

### Afternoon · 2 hrs

- **Testing Workflows — the Temporal Java testing framework** `[lab]`
- **Workflow replay testing — catching determinism regressions** `[lab]`
- **Migrating Airflow DAGs — a decision framework** `[airflow]`

---

## Day 5 — Saga pattern, Spring Boot & capstone

### Morning · 2 hrs

- **Real-world Workflow walkthrough — order processing saga** `[lab]`
- **Saga pattern in Spring Boot** `[lab]`
  - Orchestration vs. choreography; compensation logic; sync and async interaction patterns
  - Spring Boot wiring: `WorkflowClient`, Worker registration, Kafka trigger, graceful shutdown
- **Continue-as-New — long-running sagas and history size limits**

### Afternoon · 2 hrs

- **Capstone — design and implement a transactional saga** `[lab]` `[airflow]` `[kafka]`
  - Redesign a Kafka-triggered Airflow DAG as a Temporal Saga in Spring Boot
- **Capstone review — compare implementations, discuss trade-offs**
- **Q&A and open migration planning — bring your own workflow**

---

## Day 6 — AWS migration & container workloads

### Morning · 2 hrs — Replacing AWS Glue + Lambda + S3 with Temporal

- **The AWS orchestration problem** `[aws]`
  - Hidden complexity in Lambda + Glue + Step Functions; the cost of distributed state
- **Mapping AWS primitives to Temporal** `[aws]`
- **When to keep AWS compute vs. replace it** `[aws]`
- **Wrapping a Glue Spark job as a Temporal Activity** `[aws]` `[lab]`
- **Replacing S3-based checkpointing** `[aws]` `[lab]`
- **Migrating a Lambda + Step Functions pipeline end-to-end** `[aws]` `[lab]`

### Afternoon · 2 hrs — Running Temporal Workers as container workloads

- **Temporal Worker as a containerised service — the mental model** `[containers]`
- **Dockerfile for a Java Temporal Worker** `[containers]` `[lab]`
  - Multi-stage build, JVM container flags, graceful shutdown, health check
- **Kubernetes Deployment for Temporal Workers** `[containers]` `[lab]`
  - Resource limits, probe strategy, rolling update alignment with activity timeouts
- **Autoscaling Workers with KEDA** `[containers]` `[lab]`
  - Task Queue backlog as the scaling signal; Activity vs. Workflow worker pools
- **Running Glue-replacement Activities in containers** `[aws]` `[containers]`
- **Temporal Cloud vs. self-hosted on EKS — operational trade-offs** `[aws]` `[containers]`
- **Lab — containerised end-to-end: S3 trigger → Temporal Worker → S3 output** `[aws]` `[containers]` `[lab]`

---

## Docker Compose stack reference

All labs run on a laptop with Docker. Minimum recommended RAM: 8 GB.

| Service | Used from | Notes |
|---|---|---|
| `temporalio/auto-setup` | Day 1 | Temporal server + Web UI bundled |
| PostgreSQL | Day 1 | Temporal persistence backend |
| Kafka (KRaft, single broker) | Day 3 | No Zookeeper needed |
| Prometheus + Grafana | Day 4 | Pre-loaded Temporal dashboard |
| LocalStack | Day 6 | Mocks Glue, S3, SQS locally |
| `kind` + KEDA | Day 6 | Local Kubernetes for container labs |

> Day 4 testing uses `TestWorkflowEnvironment` (in-process) — no Docker required.
> Day 6 AWS labs use LocalStack — no real AWS credentials needed.

---

## Java SDK dependency reference

<!-- > `temporal-spring-boot-starter-alpha` is end-of-life as of SDK 1.24.0. Use `temporal-spring-boot-starter` (no suffix). Supports Spring Boot 2.x, 3.x, and 4.x. -->

```xml
<!-- Maven -->
<dependency>
  <groupId>io.temporal</groupId>
  <artifactId>temporal-sdk</artifactId>
  <version>1.32.1</version>
</dependency>
<dependency>
  <groupId>io.temporal</groupId>
  <artifactId>temporal-spring-boot-starter</artifactId>
  <version>1.32.1</version>
</dependency>
<dependency>
  <groupId>io.temporal</groupId>
  <artifactId>temporal-testing</artifactId>
  <version>1.32.1</version>
  <scope>test</scope>
</dependency>
<!-- Day 6: AWS SDK v2 -->
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>glue</artifactId>
  <version>2.25.0</version>
</dependency>
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
  <version>2.25.0</version>
</dependency>
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>sqs</artifactId>
  <version>2.25.0</version>
</dependency>
```

```groovy
// Gradle
implementation 'io.temporal:temporal-sdk:1.32.1'
implementation 'io.temporal:temporal-spring-boot-starter:1.32.1'
testImplementation 'io.temporal:temporal-testing:1.32.1'
// Day 6: AWS SDK v2
implementation 'software.amazon.awssdk:glue:2.25.0'
implementation 'software.amazon.awssdk:s3:2.25.0'
implementation 'software.amazon.awssdk:sqs:2.25.0'
```
