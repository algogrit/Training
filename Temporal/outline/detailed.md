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

- **First Workflow in Java — "Hello Temporal"** `[lab]`
  - SDK setup: `io.temporal:temporal-sdk` Gradle/Maven dependency
  - `@WorkflowInterface`, `@WorkflowMethod`, `@ActivityInterface`, `@ActivityMethod`
  - `WorkflowClient`, `WorkflowOptions`, `WorkflowStubs`
  - Side-by-side with equivalent Airflow DAG

- **Reading the Event History — understanding what replay actually does** `[lab]`
  - Use Temporal CLI to dump history; trace execution step by step

---

## Day 2 — Building reliable workflows

### Morning · 2 hrs

- **Calling Activities asynchronously in Java** `[lab]`
  - `Async.function()` and `Async.procedure()` for non-blocking activity invocation
  - Activity stubs and typed activity handles
  - Cancellation scopes and `CancellationScope`

- **Executing parallel Activities in Java** `[lab]`
  - `Promise.allOf()` and `Promise.anyOf()`
  - Fan-out/fan-in pattern; aggregating results with `Promise.get()`
  - Handling partial failures in parallel branches

- **Activity retries, timeouts, and heartbeating**
  - `RetryOptions` vs. Airflow's `retry` + `retry_delay`
  - `scheduleToCloseTimeout` vs. `startToCloseTimeout`
  - `Activity.getExecutionContext().heartbeat()` — not optional for long-running work
  - Handling `ActivityPausedException` and `ActivityCanceledException` on heartbeat

- **Determinism constraints — the rules that keep replay honest**
  - No `Math.random()`, no `System.currentTimeMillis()`, no direct I/O in Workflow code
  - Using `Workflow.currentTimeMillis()` and `Workflow.newRandom()`
  - How to detect and test for violations

### Afternoon · 2 hrs

- **Signals and Queries — external interaction with running Workflows** `[lab]`
  - `@SignalMethod`, `@QueryMethod` annotations
  - Build a workflow that waits for an approval signal using `Workflow.await()`
  - Query its state without interrupting it

- **Updates (Workflow Update API) — synchronous request/response against a running Workflow** `[lab]`
  - `@UpdateMethod` and `@UpdateValidatorMethod`
  - `UpdateOptions.setWaitForStage(WorkflowUpdateStage)` — required; pass `COMPLETED` or `ACCEPTED` explicitly
  - `WorkflowClient.startUpdate` for starting an update without immediately blocking on the result
  - `WorkflowClient.startUpdateWithStart` with `WithStartWorkflowOperation` for atomic start + update

- **CRON Jobs and Schedules — replacing Airflow's scheduler** `[airflow]` `[lab]`
  - Cron expressions, interval schedules, jitter, catchup policy
  - Migrating Airflow `schedule_interval` and `catchup` patterns directly
  - Overlap policies: `Skip`, `BufferOne`, `AllowAll`, `Terminate`
  - `ScheduleClient` and `ScheduleOptions` in the Java SDK

- **Workflow timeouts and Child Workflows** `[airflow]`
  - `workflowExecutionTimeout` vs. `workflowRunTimeout`; replacing `dagrun_timeout` and SLA misses
  - Child Workflows via `Workflow.newChildWorkflowStub()`; replacing `SubDagOperator` and `TaskGroup`

---

## Day 3 — Kafka integration & event-driven patterns

### Morning · 2 hrs

- **Temporal + Kafka architecture patterns — two event systems, one platform** `[kafka]`
  - Where Temporal ends and Kafka begins
  - When to use Signals vs. consuming directly from a topic

- **Kafka consumer as a Temporal Activity — reliable at-least-once consumption** `[kafka]`
  - Java `KafkaConsumer` inside an Activity; commit offsets only after Activity succeeds
  - `Activity.getExecutionContext().heartbeat()` on long polls; idempotency keys
  - Handling `ActivityPausedException` and `ActivityCanceledException` on heartbeat

- **Producing to Kafka from Activities — transactional guarantees and outbox patterns** `[kafka]`
  - Java `KafkaProducer` with idempotent producer config
  - Outbox pattern: write to DB + produce in the same Activity

- **Replacing Kafka-triggered Airflow DAGs with Signal-driven Workflows** `[airflow]` `[kafka]`
  - A Kafka consumer bridge (plain Java thread) that sends Signals via `WorkflowStub`
  - Eliminating the REST API trigger layer

### Afternoon · 2 hrs

- **Lab — end-to-end Kafka → Temporal → Kafka pipeline** `[lab]` `[kafka]`
  - Order processing example: consume order event → run approval workflow → produce outcome event
  - Full Java implementation: `KafkaConsumer` bridge, Workflow, Activities, `KafkaProducer`

- **Fan-out with Kafka partitions — parallelism inside a Workflow** `[lab]` `[kafka]`
  - `Promise.allOf()` across parallel activity invocations per partition range (capped at 4–6)
  - Aggregating results back into the parent workflow

- **Dead-letter handling — Kafka DLQ vs. Temporal's built-in retry exhaustion** `[kafka]`
  - When to let Temporal own the retry vs. routing to a DLQ
  - Compensating transactions as a precursor to Day 5 Saga pattern

---

## Day 4 — Production engineering

### Morning · 2 hrs

- **Workflow versioning — patching, the versioning API, and safe deploy** `[airflow]`
  - The equivalent of Airflow's DAG versioning problem
  - Using `Workflow.getVersion()` safely during rolling deploys
  - Patching strategies: additive changes vs. breaking changes
  - `@WorkflowVersioningBehavior` — higher-level alternative to patching
    - `Pinned` — short-lived workflows; never affected by new worker deployments
    - `AutoUpgrade` — long-running workflows; upgrade automatically to the latest worker
  - When to use `@WorkflowVersioningBehavior` vs. `Workflow.getVersion()` — decision guide

- **Worker sizing, Task Queue design, and auto-tuning**
  - Sticky execution; separate queues for high-latency vs. fast activities
  - Manual tuning: `WorkerOptions.maxConcurrentActivityExecutionSize`,
    `maxConcurrentWorkflowTaskExecutionSize`
  - `ResourceBasedTuner` — preferred for production; set memory and CPU thresholds and the
    worker auto-adjusts slot counts based on available host resources
  - `CompositeTuner` for mixing strategies across workflow and activity slots
  - Virtual threads (`WorkerFactoryOptions.setUsingVirtualWorkflowThreads`,
    `WorkerOptions.setUsingVirtualThreads`) for high-concurrency activity workers on JVM 21+
  - Autoscaling Worker deployments in Kubernetes

- **Observability — Prometheus, Grafana, and OpenTelemetry** `[lab]`
  - Docker Compose: add Prometheus + Grafana alongside Temporal
  - Micrometer + Prometheus registry wired into the Java SDK's `MicrometerClientStatsReporter`
  - OpenTelemetry span propagation into Activities via `OpenTracingClientInterceptor`
  - Writing a custom Activity metric (counter, histogram) with Micrometer

- **Namespace strategy — multi-tenancy, isolation, and data retention**

### Afternoon · 2 hrs

- **Testing Workflows — the Temporal Java testing framework** `[lab]`
  - `TestWorkflowEnvironment` with JUnit 5 and `@RegisterExtension TestWorkflowExtension`
  - Mocking Activities with `Mockito`; time-skipping with `TestWorkflowEnvironment.sleep()`
  - `TestWorkflowExtension` for Spring Boot integration tests
  - Runs fully in-process — no Docker required

- **Workflow replay testing — catching determinism regressions** `[lab]`
  - Replaying production history against new code before deploying
  - The `WorkflowReplayer` API in Java

- **Migrating Airflow DAGs — a decision framework** `[airflow]`
  - Which DAGs to migrate first (complexity × risk matrix)
  - Strangler fig pattern; running both systems in parallel
  - Common pitfalls and anti-patterns

---

## Day 5 — Saga pattern, Spring Boot & capstone

### Morning · 2 hrs

- **Real-world Workflow walkthrough — order processing saga** `[lab]`
  - Structured Java code walkthrough: payment → inventory → notification pipeline
  - Covers retries, signals, child workflows, error handling, and versioning — all together
  - Establishes the running example used throughout the Saga and Spring Boot sessions

- **Saga pattern in Spring Boot — distributed transaction consistency with Temporal** `[lab]`
  - Saga orchestration vs. choreography — why orchestration fits Temporal naturally
  - Defining compensating Activities: `cancelPayment()`, `restoreInventory()`, `sendFailureNotification()`
  - Implementing rollback sequencing in the Workflow: try/catch with explicit compensation calls
  - Sync sagas: `UpdateOptions.setWaitForStage(WorkflowUpdateStage.COMPLETED)` blocks the
    caller until the saga completes
  - Async sagas: fire-and-forget with Signal-based callbacks; polling with Query
  - `@WorkflowInterface` and `@ActivityInterface` as Spring `@Component` beans
  - Wiring `WorkflowClient` as a Spring `@Bean`; `Worker` registration on `ApplicationContext` start
  - Spring Kafka `@KafkaListener` as the event-driven saga trigger
  - Graceful shutdown: `Worker.shutdown()` on `ApplicationContext` close
  - Lab: inject a failure at each saga step; verify compensation executes correctly

- **Continue-as-New — long-running sagas and history size limits**
  - When a saga runs indefinitely (e.g. subscription lifecycle); using `Workflow.continueAsNew()`

### Afternoon · 2 hrs

- **Capstone — design and implement a transactional saga** `[lab]` `[airflow]` `[kafka]`
  - Teams receive a multi-step Airflow DAG with Kafka triggers representing a business transaction
  - Redesign as a Temporal Saga in Spring Boot end-to-end
  - Requirements: compensation logic for every step, Kafka trigger via Spring Kafka listener,
    at least one sync and one async interaction pattern

- **Capstone review — diff the two implementations, identify trade-offs**
  - What got simpler? What required more thought?
  - Consistency guarantees: what Temporal gives you vs. what you still own
  - Observability and auditability of compensation steps

- **Q&A and open migration planning — bring your own workflow**
  - Teams present their real migration candidates
  - Group discussion on saga boundaries, compensation design, and phasing

---

## Day 6 — AWS migration & container workloads *(new)*

### Morning · 2 hrs — Replacing AWS Glue + Lambda + S3 with Temporal

- **The AWS orchestration problem — why glue code becomes glue workflows** `[aws]`
  - How Lambda + Glue + Step Functions accumulates hidden orchestration complexity
  - Where state lives in the AWS model: S3 checkpoints, DynamoDB locks, SSM params
  - The cost of distributed state: debugging a failed pipeline across CloudWatch, S3, and Glue history
  - Temporal's value proposition: durable execution replaces the S3-checkpoint pattern entirely

- **Mapping AWS primitives to Temporal** `[aws]`

  | AWS Primitive | Temporal Equivalent | Notes |
  |---|---|---|
  | Glue Job (ETL logic) | Activity (Java) | Your code; no proprietary DSL |
  | Lambda (trigger / orchestrator) | Workflow starter or Signal handler | Code, not YAML |
  | S3 (checkpoint between steps) | Temporal durable state | Eliminated entirely |
  | Step Functions state machine | Temporal Workflow | Branching, loops, waits in code |
  | Glue Workflow / trigger | Temporal Schedule or parent Workflow | First-class, not bolted on |
  | CloudWatch retry + DLQ | Temporal RetryOptions + compensation | Per-activity granularity |
  | EventBridge rule → Lambda | Kafka/SQS consumer bridge → Signal | Or direct SDK trigger |

- **When to keep AWS compute vs. replace it** `[aws]`
  - Keep Glue Spark for terabyte-scale distributed joins — Temporal orchestrates *around* it, not instead of it
  - Replace Lambda orchestration glue: event routing, step chaining, state passing via S3
  - Replace Glue Python Shell jobs with Temporal Activities in Java; same business logic, better retry semantics
  - Decision matrix: job duration × statefulness × branching complexity

- **Wrapping a Glue Spark job as a Temporal Activity** `[aws]` `[lab]`
  - Submitting a Glue job run via `GlueClient` (AWS SDK v2) inside an Activity
  - Polling job status with `Activity.getExecutionContext().heartbeat(jobRunId)`
  - Handling `FAILED`, `TIMEOUT`, and `STOPPED` states cleanly
  - Surfacing Glue error details in `ApplicationFailure` for Temporal UI visibility
  - Timeout alignment: `startToCloseTimeout` must exceed worst-case Glue job duration

- **Replacing S3-based checkpointing** `[aws]` `[lab]`
  - Why pipelines write intermediate results to S3: crash survival, hand-off between Lambdas
  - Temporal's durable state means workflow-local data survives worker restarts automatically
  - Pattern: pass S3 URIs as Activity outputs (for large payloads) but let Temporal own *whether* each step ran
  - Payload size limits: keep Workflow history lean; use S3 references for blobs > ~2 MB
  - Codec server pattern for encrypting S3 URI payloads at rest in Temporal history

- **Migrating a Lambda + Step Functions pipeline end-to-end** `[aws]` `[lab]`
  - Take a real Step Functions state machine (JSON): validate → transform → load → notify
  - Rewrite as a Temporal Workflow + four Activities in Java
  - Compare: lines of code, error surfaces, retry configuration, debuggability
  - Strangler fig approach: run Step Functions and Temporal in parallel; route by feature flag

### Afternoon · 2 hrs — Running Temporal Workers as container workloads

- **Temporal Worker as a containerised service — the mental model** `[containers]`
  - Workers are stateless long-running processes: they poll Task Queues, execute, repeat
  - No listener ports required; Workers initiate all connections outbound to the Temporal Frontend
  - Implication: Workers fit naturally into Kubernetes Deployments without Ingress or Service objects
  - Container image strategy: one image per Worker pool, or a shared image with Task Queue env var

- **Dockerfile for a Java Temporal Worker** `[containers]` `[lab]`
  - Multi-stage build: Maven/Gradle compile stage + minimal JRE runtime stage
  - JVM flags for containers: `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage`
  - Graceful shutdown: `SIGTERM` → `Worker.shutdown()` → drain in-flight Activity executions
  - Health check: expose a simple HTTP `/health` endpoint (Spring Actuator or a plain HttpServer)
    that returns `200` only after `WorkerFactory.start()` succeeds

- **Kubernetes Deployment for Temporal Workers** `[containers]` `[lab]`
  - `Deployment` with `replicas`, `resources.requests/limits`, and `terminationGracePeriodSeconds`
  - Aligning `terminationGracePeriodSeconds` with `startToCloseTimeout` to avoid mid-activity kills
  - ConfigMap + Secrets for `TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`, and AWS credentials
  - Liveness vs. readiness probes: readiness gates on Task Queue poll success; liveness on JVM health
  - Rolling update strategy: `maxUnavailable: 0` to preserve sticky execution during deploys

- **Autoscaling Workers with KEDA** `[containers]` `[lab]`
  - Why HPA on CPU/memory lags for Temporal Workers (queue depth is the right signal, not CPU)
  - KEDA `ScaledObject` targeting Temporal Task Queue backlog via the Temporal metrics endpoint
  - `minReplicaCount: 1` to keep at least one poll active; `maxReplicaCount` tuned per workload
  - Scaling Activity workers independently from Workflow workers using separate Deployments
  - Cost optimisation: scale-to-zero for batch/overnight workloads; keep workflow workers at min 1

- **Running Glue-replacement Activities in containers — the Temporal Worker as compute** `[aws]` `[containers]`
  - Replacing Glue jobs with a containerised Java Temporal Worker: same business logic, better retry model
  - Resource isolation: separate Deployments for CPU-heavy transform activities vs. lightweight I/O activities
  - Sidecars and init containers: AWS credential injection via IRSA (IAM Roles for Service Accounts)

- **Temporal Cloud vs. self-hosted on EKS — operational trade-offs** `[aws]` `[containers]`
  - Self-hosted: Temporal server on EKS (Helm chart), RDS PostgreSQL, ElasticSearch for visibility
  - Temporal Cloud: managed Frontend + History + Matching; you only run Workers
  - Cost model comparison: EC2/RDS for self-hosted vs. Temporal Cloud action-based pricing
  - Migration path: start with Temporal Cloud to de-risk the platform; move self-hosted if cost justifies
  - Network topology: Workers on EKS calling Temporal Cloud over mTLS; AWS PrivateLink for self-hosted

- **Lab — containerised end-to-end: S3 trigger → Temporal Worker → S3 output** `[aws]` `[containers]` `[lab]`
  - Build and push a Worker Docker image to ECR
  - Deploy to local `kind` cluster (simulating EKS); configure KEDA ScaledObject
  - Trigger a workflow via an S3 event bridge → SQS → Signal bridge pattern
  - Observe autoscaling as workflow backlog grows; verify graceful drain on pod termination

---

## Docker Compose stack reference

All labs run on a laptop with Docker. Minimum recommended RAM: 8 GB.

| Service | Used from | Notes |
|---|---|---|
| `temporalio/auto-setup` | Day 1 | Temporal server + Web UI bundled |
| PostgreSQL | Day 1 | Temporal persistence backend |
| Kafka (KRaft, single broker) | Day 3 | Bitnami or Confluent image; no Zookeeper needed |
| Prometheus | Day 4 | Scrapes Worker metrics endpoint via Micrometer |
| Grafana | Day 4 | Pre-loaded Temporal dashboard |
| LocalStack | Day 6 | Mocks Glue, S3, SQS, and EventBridge for AWS labs |
| `kind` (Kubernetes in Docker) | Day 6 | Local EKS-equivalent for container labs |
| KEDA | Day 6 | Installed into `kind` cluster via Helm |

> Day 4 testing labs use `TestWorkflowEnvironment` (in-process) — no server or Docker required.
> Cap Kafka partition count at **4–6** in the Day 3 fan-out lab to avoid CPU spikes on lower-spec machines.
> Day 6 AWS labs use LocalStack to avoid requiring real AWS credentials on training machines.

---

## Java SDK dependency reference

<!-- > **Note:** `temporal-spring-boot-starter-alpha` is end-of-life as of SDK 1.24.0. Use
> `temporal-spring-boot-starter` (no suffix). Supports Spring Boot 2.x, 3.x, and 4.x.
> Pin the version explicitly in the training repo. -->

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

<!-- Day 6: AWS SDK v2 for Glue/S3/SQS Activities -->
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
