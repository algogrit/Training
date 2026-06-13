---
marp: true
theme: base
paginate: true
size: 16:9
title: Temporal Fundamentals
description: A Java-first 24-hour Temporal training mapped to the course Agenda.
author: Gaurav Agarwal
footer: "@codermana"
---

<!-- _class: title -->

###### Fundamentals · 24 hours · 6 days

# Temporal Fundamentals

A Java-first 24-hour tour mapped to the course Agenda.

Gaurav Agarwal

<!--
6 days × 4 hours. Each day mirrors a day in lecture_notes/Day-XX.md.
Lab slides are marked - laptops out, fingers on keyboards.
Pace check: end of Day 1 should leave the room with one Workflow running.
-->

---

# Course Agenda

| Day | Topic | Lab focus |
| --- | --- | --- |
| 1 | Foundations - durable execution mental model | Hello Temporal, Event History |
| 2 | Reliability + interactions | Signals, Updates, Schedules |
| 3 | Kafka integration | End-to-end Kafka pipeline, DLQ |
| 4 | Production engineering | Replay tests, dashboards |
| 5 | Saga + Spring Boot + capstone | Capstone Workflow |
| 6 | AWS migration + containers | Glue, K8s, KEDA |

<!--
Quick orientation slide. Don't dwell - each Day cover slide opens the
detailed agenda for that block.
-->

---

<!-- _class: day -->

###### Day 1

# Foundations

Rethinking orchestration as durable application code.

<!--
4 hours: 2 morning + 2 afternoon. Morning is concepts; afternoon is the
first hands-on Workflow.
-->

---

<!-- _class: section -->

###### Day 1 · Morning

# Why Temporal exists

The failure modes of cron- and DAG-based orchestration.

---

# Every backend has these

- "Charge the card, ship the order, send the receipt."
- "Pull from S3, transform with Spark, write to Snowflake."
- "Wait for the human approval, then provision the tenant."
- "Retry the flaky API for an hour, then page the on-call."

These are **workflows**. They look easy until one step fails.

<!--
Read in different voices. Each shape will resonate with someone in the room.
-->

---

# What goes wrong

- The third call timed out. Did it succeed?
- The Lambda was killed at minute 14 of 15.
- The Kafka consumer crashed *between* the DB write and the publish.
- The cron didn't fire. Nobody noticed for two days.
- The retry loop never had a budget.

> Recovery is a **runbook**, not a button.

<!--
The slogan to repeat across the day: runbook, not a button.
-->

---

<!-- _class: dense -->

# Tools you've shipped with

| Stack | What it solves | What it leaves to you |
| --- | --- | --- |
| Cron + scripts | Triggering on a schedule | All state, retries, recovery |
| Airflow | Scheduling DAGs | Cross-system state, retries on top |
| Step Functions | State machines in JSON | Code review, Lambda 15-min cap |
| Kafka alone | Transport between systems | Per-key state, idempotency |

Drift across all of them = the 2 AM page.

<!--
Don't bash any tool. Each solves a real problem. The point is the seam each
leaves open, not that any is bad.
-->

---

<!-- _class: section -->

###### Day 1 · Morning

# Core concepts

Workflows, Activities, Workers, Task Queues.

---

<!-- _class: cards -->

# The four primitives

| Workflow | Activity | Worker | Task Queue |
| --- | --- | --- | --- |
| Durable function. State is the event history. Deterministic. | Arbitrary code with side effects. Retried independently. | Long-lived process polling one or more Task Queues. | A string name. Routes work to a Worker pool. |

<!--
Four cards, four primitives. Task Queue is JUST A STRING. Not Kafka. Not a
DB. It's a routing key.
-->

---

# Workflow

```java
@WorkflowInterface
public interface OrdersWorkflow {
  @WorkflowMethod
  void run(String batchDate);
}
```

- Annotated Java interface + impl.
- The impl is your **durable function**.
- State lives in event history; the impl is replayable.

<!--
This is just a Java interface. The SDK uses the @WorkflowInterface
annotation to identify it via reflection.
-->

---

# Activity

```java
@ActivityInterface
public interface OrdersActivities {
  String extract(String batchDate);
  String transform(String rawUri);
  void load(String cleanUri);
}
```

- Unrestricted code: HTTP, DB, files, anything.
- Retried independently of the Workflow.
- 95% of your real production code lives here.

---

# Worker + Task Queue

```java
WorkerFactory factory = WorkerFactory.newInstance(client);
Worker worker = factory.newWorker("orders");
worker.registerWorkflowImplementationTypes(OrdersWorkflowImpl.class);
worker.registerActivitiesImplementations(new OrdersActivitiesImpl());
factory.start();
```

- Worker = long-lived JVM polling `orders` Task Queue.
- The Task Queue **string** routes work to a Worker pool.

<!--
factory.start() kicks off the long-poll loop. Workers connect outbound.
-->

---

<!-- _class: dense -->

# Airflow → Temporal map

| Airflow | Temporal |
| --- | --- |
| DAG | Workflow |
| Operator / Task | Activity |
| Worker | Worker (long-lived JVM) |
| `default_queue` | Task Queue |
| XCom | A normal Java return value |
| `ExternalTaskSensor` | Signal / `signalWithStart` |
| `BranchPythonOperator` | `if` / `switch` in Java |
| Sensor poll loop | `Workflow.await(predicate)` |

> You stop describing shape. You start writing behavior.

<!--
For Airflow rooms, this slide is the moment of recognition. XCom-becomes-
a-return-value gets the biggest reaction.
-->

---

<!-- _class: section -->

###### Day 1 · Morning

# Event sourcing & deterministic replay

The single concept that breaks the most Airflow brains.

---

# The replay rule

When a Worker resumes a Workflow:

1. It re-runs the Workflow code from the start.
2. Replays recorded events to reconstruct local state.
3. Reaches the next undecided point.
4. Continues from there.

> Different decision than the recorded history = non-determinism error.

<!--
Whiteboard moment. Walk through with arrows. "Replay" doesn't re-execute
side effects - Activity results are READ from history.
-->

---

<!-- _class: dense -->

# Five families of non-determinism

```java
// 1. Time
long now = System.currentTimeMillis();         // NO
long now = Workflow.currentTimeMillis();       // YES

// 2. Random
int n = new Random().nextInt(10);              // NO
int n = Workflow.newRandom().nextInt(10);      // YES

// 3. I/O
Files.writeString(path, "x");                  // NO - move to Activity

// 4. Concurrency
Thread.sleep(60_000);                           // NO
Workflow.sleep(Duration.ofMinutes(1));         // YES

// 5. Iteration order
for (var e : hashMap.entrySet()) { ... }       // risky
```

<!--
Reference card. Family 4 is the biggest aha - Workflow.sleep records a
timer; the Worker FORGETS the workflow.
-->

---

# Durable sleep

```java
Workflow.sleep(Duration.ofDays(30));
```

- The Worker *forgets* this Workflow.
- The server fires a timer in 30 days.
- Some Worker - maybe a different one - picks it up and continues.

> No JVM stays alive. Survives every deploy in between.

<!--
Ask: "how would you wait 30 days for an email opt-in today?" Compare to
one line.
-->

---

<!-- _class: section -->

###### Day 1 · Morning

# Architecture

What's inside the box.

---

<!-- _class: code -->

## The cluster

```
                   ┌──────────────┐
   SDK / CLI  ───▶ │   Frontend   │   gRPC API
                   └──────┬───────┘
                          │
                   ┌──────▼───────┐
                   │   History    │   workflow state machine
                   └──────┬───────┘
                          │
                   ┌──────▼───────┐
                   │   Matching   │   Task Queue dispatch
                   └──────┬───────┘
                          │
                   ┌──────▼───────┐
                   │ Persistence  │   PostgreSQL / Cassandra
                   └──────────────┘
```

Your Workers connect **outbound** to Frontend on `:7233`.

<!--
Trace one Workflow start. SDK → Frontend → History (write
WorkflowExecutionStarted) → Matching → Worker polls.
-->

---

# What's on your laptop

`temporal server start-dev` bundles:

- Frontend + History + Matching + internal Worker
- PostgreSQL (or in-memory)
- Web UI on `:8233`
- Prometheus metrics on `:7234` with `--metrics-port`

> One binary today. Same gRPC contract as production.

---

<!-- _class: lab -->

###### Lab · Day 1 PM

# Local dev setup

```bash
make check          # verify required tools
make temporal       # start dev server (in this terminal)
```

In another terminal:

```bash
open http://127.0.0.1:8233
temporal operator namespace list
```

> Goal: every laptop shows the `default` namespace in the Web UI.

<!--
Wait until every laptop is green. Pair the stragglers. Don't proceed without
this; the rest of the day depends on it.
-->

---

<!-- _class: lab -->

###### Lab · Day 1 PM

# Hello Temporal

```bash
make run-hello
```

What to look for:

1. Workflow appears in the Web UI under `default` namespace.
2. Click into it; open the Event History tab.
3. Identify `WorkflowExecutionStarted`, `ActivityTaskScheduled`, `ActivityTaskCompleted`.

> Restart the Worker mid-run; the Workflow resumes. That's the lesson.

<!--
Have one person KILL the Worker mid-run on purpose. The Workflow completes
when the Worker restarts. This is the most important moment of Day 1.
-->

---

<!-- _class: lab -->

###### Lab · Day 1 PM

# Read the Event History

```bash
temporal workflow show \
  --workflow-id hello-temporal-demo --output json \
  | jq '.events[].eventType'
```

Identify in order:

- `WorkflowTaskScheduled` / `Started` / `Completed`  - the decision loop
- `ActivityTaskScheduled` / `Started` / `Completed`  - the work loop
- `WorkflowExecutionCompleted` - final outcome

<!--
This grep-able view is the production debugging starting point. Show it
now; it'll come back on Day 4 for replay tests.
-->

---

<!-- _class: takeaway -->

# Day 1 takeaways

- One model: **Workflow code re-executes on replay; Activity results are recorded.**
- One discipline: keep Workflow code deterministic; do all I/O in Activities.
- One habit: pick Workflow IDs from business identity. They're durable handles.

<!--
The first slogan to repeat. If only one thing sticks for Day 1, it's the
re-execution-vs-recorded-results distinction.
-->

---

<!-- _class: day -->

###### Day 2

# Building reliable Workflows

Async, retries, heartbeats - and the ways you interact with running executions.

<!--
4 hours: morning is reliability mechanics; afternoon is signals/queries/
updates/schedules/children. Lots of code.
-->

---

<!-- _class: section -->

###### Day 2 · Morning

# Async and parallel Activity execution

Promises, not threads.

---

# Sequential vs Async

```java
// Sequential - one Activity at a time
String rawUri = activities.extract(batchDate);
String cleanUri = activities.transform(rawUri);

// Async - two extracts run in parallel
Promise<String> rawUri   = Async.function(activities::extract, batchDate);
Promise<String> auditUri = Async.function(activities::extract, batchDate + "-audit");
String cleanUri      = activities.transform(rawUri.get());
String cleanAuditUri = activities.transform(auditUri.get());
```

> `Promise.get()` blocks the *Workflow loop*, not an OS thread.

---

<!-- _class: code -->

## Fan-out / fan-in

```java
List<Promise<Integer>> counts =
    partitions.stream()
        .map(p -> Async.function(activities::processPartition, p))
        .toList();

Promise.allOf(counts).get();
int total = counts.stream().mapToInt(Promise::get).sum();
```

All partitions run in parallel. The Workflow suspends across all of them.

<!--
One JVM hosts tens of thousands of suspended Workflows. Each one is just
heap state, not a parked thread.
-->

---

<!-- _class: lab -->

###### Lab · Day 2 AM

# Async + parallel activities

```bash
make run-async
```

In the Web UI:

1. Note that all `ActivityTaskScheduled` events appear with the *same* timestamp.
2. Compare to a sequential variant: events stagger.
3. Pair: predict what happens if one of three parallel Activities fails.

<!--
Have students sketch on paper before running. Then run and verify their
prediction was right (or wrong - even better).
-->

---

<!-- _class: section -->

###### Day 2 · Morning

# Retries, timeouts, heartbeats

Know what each setting controls or you'll misuse all of them.

---

<!-- _class: dense -->

# Three timeouts

| Setting | Controls |
| --- | --- |
| `startToCloseTimeout` | One attempt's wall-clock budget |
| `scheduleToCloseTimeout` | Total budget across **all** retry attempts |
| `scheduleToStartTimeout` | How long an Activity sits in the queue before pickup |
| `heartbeatTimeout` | Max gap between heartbeats; Worker death detect |

> If you can't say *why* a timeout is 5 minutes, it's wrong.

---

<!-- _class: code dense -->

## Setting them deliberately

```java
ActivityOptions.newBuilder()
    .setStartToCloseTimeout(Duration.ofMinutes(5))
    .setScheduleToCloseTimeout(Duration.ofMinutes(30))
    .setHeartbeatTimeout(Duration.ofSeconds(30))
    .setRetryOptions(
        RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(5))
            .setBackoffCoefficient(2.0)
            .setMaximumInterval(Duration.ofMinutes(1))
            .setMaximumAttempts(6)
            .build())
    .build();
```

6 × 5min attempts + 6 backoff waits ≈ 33min — set scheduleToClose to bound it.

<!--
The arithmetic is the lesson. Bring a calculator if you don't trust the
audience to do it on paper.
-->

---

<!-- _class: code -->

## Heartbeats

```java
public String exportLargeTable(String tableName) {
  for (int page = 0; page < 1000; page++) {
    try {
      exportPage(tableName, page);
      Activity.getExecutionContext().heartbeat(page);
    } catch (ActivityCanceledException stop) {
      cleanupPartialExport(tableName, page);
      throw stop;
    }
  }
  return "s3://exports/" + tableName;
}
```

> On retry, read the last heartbeat detail and *resume from page N*.

---

<!-- _class: section -->

###### Day 2 · Morning

# Determinism, reinforced

The rules that keep replay honest.

---

# Common traps

- `Map.Entry.getKey()` iteration over `HashMap` - JVM-version-dependent.
- `Instant.now()`, `LocalDateTime.now()`.
- `UUID.randomUUID()` → use `Workflow.randomUUID()`.
- `CompletableFuture`, `ExecutorService` → use `Async.function`, `Workflow.newPromise`.
- Throwing checked exceptions across the Workflow boundary - prefer `ApplicationFailure`.

> The replay tests on Day 4 catch all of these.

<!--
Reinforcement, not new content. The students saw the families yesterday;
this is the "what bites in production" list.
-->

---

<!-- _class: section -->

###### Day 2 · Afternoon

# Signals and Queries

Push data in. Pull data out.

---

<!-- _class: code -->

## Signals - push data in

```java
@WorkflowInterface
interface ApprovalWorkflow {
  @WorkflowMethod  String run(String requestId);
  @SignalMethod    void approve(String approver);
  @QueryMethod     String currentState();
}

@Override
public String run(String requestId) {
  Workflow.await(() -> state.startsWith("APPROVED"));
  return state;
}

@Override
public void approve(String approver) { state = "APPROVED by " + approver; }
```

<!--
Async, recorded in history, wakes any await predicate.
-->

---

# Queries - pull data out

```java
@Override
public String currentState() { return state; }
```

- Read-only function over **current in-memory state**.
- No history events. No Activities. No side effects.

> Synchronous and cheap. Routed to whichever Worker has the workflow cached.

---

<!-- _class: lab -->

###### Lab · Day 2 PM

# Signals + Queries

```bash
make run-approval
```

```bash
temporal workflow signal --workflow-id approval-demo \
  --name approve --input '"alice"'

temporal workflow query --workflow-id approval-demo \
  --type currentState
```

> Send the Signal before the Workflow starts; see what happens.

<!--
The "before workflow starts" twist: signalWithStart later in the day will
make this explicit.
-->

---

<!-- _class: section -->

###### Day 2 · Afternoon

# Updates

Synchronous, validated, write-capable RPC into a running Workflow.

---

<!-- _class: code -->

## @UpdateMethod + @UpdateValidatorMethod

```java
@WorkflowInterface
interface CartWorkflow {
  @WorkflowMethod  String checkout(String cartId);
  @UpdateMethod    int addItem(String sku, int quantity);

  @UpdateValidatorMethod(updateName = "addItem")
  void validateAddItem(String sku, int quantity);
}
```

- Caller blocks on `.getResult()`.
- Validator runs **before** the update is admitted to history.
- Reject cheaply; don't pollute the audit trail.

---

<!-- _class: code -->

## signalWithStart

```java
BatchRequest batch = client.newSignalWithStartRequest();
batch.add(workflow::run, orderId);
batch.add(workflow::orderEvent, event);
client.signalWithStart(batch);
```

- First event for a key: workflow starts.
- Later events: signal the existing execution.

> Bare `start()` throws `WorkflowExecutionAlreadyStarted` on event #2.

<!--
This is THE foot-gun. Every team copies bare WorkflowClient.start() from
a tutorial and crashes on the second Kafka message for the same key.
-->

---

<!-- _class: code -->

## startUpdateWithStart

```java
WithStartWorkflowOperation<String> start =
    WithStartWorkflowOperation.newBuilder(workflow::process)
        .setArguments(request.orderId()).build();

WorkflowUpdateHandle<String> update =
    client.startUpdateWithStart(
        start, "submit", WorkflowUpdateStage.COMPLETED, String.class, request);

return update.getResult();
```

> One round trip. Creates the Workflow if absent, applies the Update, returns.

---

<!-- _class: lab -->

###### Lab · Day 2 PM

# Updates

```bash
make run-approval     # same project; different test
```

```bash
# Sync update against a running workflow
temporal workflow update --workflow-id cart-1001 \
  --name addItem --input '"book"' --input 2
```

> Verify the response is the **new item count**, not a generic 202.

<!--
The blocking-call shape is what makes Updates the modern primitive.
Signal+Query is older and works against older clusters; Update is the
right tool when the caller wants the result.
-->

---

<!-- _class: section -->

###### Day 2 · Afternoon

# Schedules

Replacing Airflow's scheduler.

---

<!-- _class: code dense -->

## Hourly schedule

```java
Schedule schedule = Schedule.newBuilder()
    .setAction(ScheduleActionStartWorkflow.newBuilder()
        .setWorkflowType(OrdersWorkflow.class)
        .setOptions(WorkflowOptions.newBuilder().setTaskQueue("orders").build())
        .build())
    .setSpec(ScheduleSpec.newBuilder()
        .setIntervals(List.of(new ScheduleIntervalSpec(Duration.ofHours(1))))
        .setJitter(Duration.ofMinutes(5))
        .build())
    .setPolicy(SchedulePolicy.newBuilder()
        .setOverlap(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_BUFFER_ONE)
        .build())
    .build();

scheduleClient.createSchedule("hourly-orders", schedule, ScheduleOptions.newBuilder().build());
```

> Durable Temporal object. Survives redeploy. Overlap is *explicit*.

---

<!-- _class: lab -->

###### Lab · Day 2 PM

# Schedules

```bash
make run-schedules
temporal schedule list
temporal schedule describe --schedule-id daily-sales-report-schedule
```

Discuss:

- What does `SCHEDULE_OVERLAP_POLICY_SKIP` mean for a 90-minute job that fires hourly?
- Pause + resume from the CLI; observe what the schedule does.

<!--
Compare to "your DAG runs hourly but the 3 AM run takes 90 minutes" - in
Airflow you set max_active_runs. Here you set ScheduleOverlapPolicy.
-->

---

<!-- _class: section -->

###### Day 2 · Afternoon

# Child Workflows and timeouts

When to compose. How to bound.

---

<!-- _class: code -->

## Child Workflows

```java
FraudWorkflow fraud = Workflow.newChildWorkflowStub(
    FraudWorkflow.class,
    ChildWorkflowOptions.newBuilder().setTaskQueue("fraud").build());

ShippingWorkflow shipping = Workflow.newChildWorkflowStub(
    ShippingWorkflow.class,
    ChildWorkflowOptions.newBuilder().setTaskQueue("shipping").build());

Promise<String> fraudDecision = Async.function(fraud::check, orderId);
Promise<String> shippingPlan  = Async.function(shipping::plan, orderId);

Promise.allOf(fraudDecision, shippingPlan).get();
```

> Children get **independent identity, history, Task Queue, timeouts**.

---

# Workflow timeouts

```java
WorkflowOptions.newBuilder()
    .setWorkflowExecutionTimeout(Duration.ofDays(7))  // across continue-as-new
    .setWorkflowRunTimeout(Duration.ofHours(12))      // this run only
    .setTaskQueue("orders")
    .build();
```

- `WorkflowExecutionTimeout` - hard cap, all continuations.
- `WorkflowRunTimeout` - cap for this run; forces continuation.

<!--
Workflow timeouts ≠ Activity timeouts. These are top-level execution caps,
not per-attempt budgets.
-->

---

<!-- _class: takeaway -->

# Day 2 takeaways

- One async pattern: `Async.function` + `Promise.allOf`. Yields the Workflow loop, not threads.
- One Kafka/REST rule: **signalWithStart**, never bare start.
- One sync RPC: **startUpdateWithStart** for "POST and wait for result."

<!--
Three slogans for Day 2. Each one is a foot-gun saved.
-->

---

<!-- _class: day -->

###### Day 3

# Kafka integration

Kafka is the bus between teams. Temporal is the brain inside one team.

<!--
Day 3 is half conceptual (where does Kafka end and Temporal start?) and
half hands-on (full Kafka → Temporal → Kafka loop).
-->

---

<!-- _class: section -->

###### Day 3 · Morning

# Temporal + Kafka architecture

Different jobs. Used together.

---

<!-- _class: dense -->

# Who owns what

| Concern | Owner |
| --- | --- |
| Append-only event log, replayable by offset | Kafka |
| Fan-out to many independent consumers | Kafka |
| State of a single business transaction | Temporal |
| Retry / timeout / compensation logic | Temporal |
| Long-running human / external waits | Temporal |

> Kafka tells you *what happened*. Temporal tells you *where we are*.

---

<!-- _class: code -->

## Kafka consumer as Activity

```java
@Override
public List<String> pollBatch(String topic) {
  consumer.subscribe(List.of(topic));
  List<String> values = new ArrayList<>();
  while (values.size() < 100) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
    for (ConsumerRecord<String, String> record : records) {
      values.add(record.value());
      Activity.getExecutionContext()
          .heartbeat(record.topic() + ":" + record.partition() + ":" + record.offset());
    }
  }
  consumer.commitSync();   // commit ONLY after success
  return values;
}
```

<!--
Disable auto-commit. Always. Commit after the unit of work succeeds.
Heartbeat the topic:partition:offset so retries can resume.
-->

---

<!-- _class: code -->

## Producer Activity

```java
properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
properties.put(ProducerConfig.ACKS_CONFIG, "all");

@Override
public void publishOutcome(String orderId, String outcome) {
  producer.send(new ProducerRecord<>("order-outcomes", orderId, outcome)).join();
}
```

- Idempotent producer + stable key = at-least-once becomes effectively-once by key.
- Downstream still dedupes.

---

<!-- _class: code -->

## Outbox pattern

```java
transactionTemplate.execute(status -> {
  orderRepository.save(order);
  outboxRepository.save(new OutboxMessage(
      "order-events", order.id(),
      json.serialize(new OrderAccepted(order.id()))));
  return null;
});
```

- One DB transaction = atomic business row + outbox row.
- Publish to Kafka in a separate Activity / Debezium.

> Two side effects across systems can't be atomic without 2PC. Outbox sidesteps that.

---

<!-- _class: section -->

###### Day 3 · Morning

# Signal-driven Workflows

Replacing Kafka-triggered Airflow DAGs.

---

# The pattern

1. One Workflow per business entity (e.g. per orderId).
2. Workflow ID = `"order-" + orderId`.
3. Kafka consumer is a thin bridge: `signalWithStart` for every event.
4. Commit offsets after `signalWithStart` returns.

> Bare `start()` throws `WorkflowExecutionAlreadyStarted` on event #2.

---

<!-- _class: code -->

## The bridge

```java
BatchRequest batch = client.newSignalWithStartRequest();
batch.add(workflow::run, orderId);
batch.add(workflow::orderEvent, record.value());
client.signalWithStart(batch);
consumer.commitSync();
```

- First event for `orderId` starts the workflow.
- Subsequent events signal the existing execution.
- Offset commit happens *after* the signal lands.

---

<!-- _class: section -->

###### Day 3 · Afternoon

# End-to-end pipeline

Kafka → Temporal → Kafka.

---

<!-- _class: lab -->

###### Lab · Day 3 PM

# Run the pipeline

```bash
make stack-kafka      # KRaft broker on :9092
make run-kafka        # Worker + bridge
```

In another terminal:

```bash
kcat -b localhost:9092 -t orders -P -k "order-1" <<< 'NEW:line-item-A'
kcat -b localhost:9092 -t order-outcomes -C -o end -f 'key=%k value=%s\n'
```

> Send a second event for the same key. Watch it Signal the existing workflow.

<!--
Have students fire two events for one key. The second event should NOT
start a new workflow. If it does, they're using bare start - debug it.
-->

---

# Partition fan-out

Two strategies:

- **Outside the Workflow** - one Workflow per partition. Many small histories.
- **Inside the Workflow** - one Workflow processes a *range* of partitions in parallel Activities.

> Pick based on whether the partitions share business state.

---

<!-- _class: code -->

## Inside-Workflow fan-out

```java
List<Promise<Integer>> counts =
    ranges.stream()
        .map(range -> Async.function(activities::processRange, range))
        .toList();

Promise.allOf(counts).get();
int total = counts.stream().mapToInt(Promise::get).sum();
```

Bound the fan-out: don't open 1,000 partitions inside one history.

---

<!-- _class: lab -->

###### Lab · Day 3 PM

# Fan-out by partition

```bash
# Produce to multiple partitions (auto-create OR pre-create with 4)
make kafka-topic TOPIC=orders PARTITIONS=4

for i in 1 2 3 4; do
  echo "evt-$i" | kcat -b localhost:9092 -t orders -P -k "order-$i"
done
```

> In the Web UI, observe 4 separate Workflow executions, one per key.

---

# DLQ vs Temporal retry exhaustion

| Failure type | Belongs in |
| --- | --- |
| Transient (network) | Temporal retry (free) |
| Poison message (malformed) | DLQ topic for triage |
| Business rule rejection | DLQ or audit topic |
| Catch-all | DLQ after Temporal exhausts |

> Temporal retries solve transient. DLQ catches what retry can't fix.

---

<!-- _class: code -->

## DLQ Activity

```java
try {
  orders.validate(orderId);
} catch (ActivityFailure exhausted) {
  dlq.publish(orderId, exhausted.getMessage());
}
```

- Workflow catches `ActivityFailure` (retry exhausted).
- Publishes to DLQ; Workflow completes successfully.
- The *order* failed; the *Workflow* did its job.

---

<!-- _class: takeaway -->

# Day 3 takeaways

- `signalWithStart` is the only correct Kafka bridge primitive.
- Commit Kafka offsets only after the unit of work is durably accepted.
- DLQ catches what Temporal retries cannot fix. Different problems.

---

<!-- _class: day -->

###### Day 4

# Production engineering

Versioning, sizing, observability, replay tests, namespaces, Airflow migration.

<!--
Heaviest day on production rigour. Lots of ops content. Two big labs:
metrics dashboard and replay tests.
-->

---

<!-- _class: section -->

###### Day 4 · Morning

# Workflow versioning

Shipping new code without breaking in-flight Workflows.

---

# Why versioning exists

Day 1: deploy v1. Workflow runs against v1 history.

Day 30: deploy v2 that reorders two Activities.

In-flight Workflow resumes against **v2 code** with **v1 history** → non-determinism error.

> You need v2 code to behave like v1 *until past the change-point*.

---

<!-- _class: code -->

## `Workflow.getVersion`

```java
int v = Workflow.getVersion("charge-before-reserve", Workflow.DEFAULT_VERSION, 1);

if (v == Workflow.DEFAULT_VERSION) {
  payments.reserve(orderId);
  payments.charge(orderId);
} else {
  payments.charge(orderId);
  payments.reserve(orderId);
}
```

> The change-point name is *identity*. Treat like a migration filename. Never recycle.

---

<!-- _class: code -->

## Versioning behavior

```java
@WorkflowVersioningBehavior(VersioningBehavior.PINNED)
class ShortLivedCheckoutWorkflow implements CheckoutWorkflow { ... }

@WorkflowVersioningBehavior(VersioningBehavior.AUTO_UPGRADE)
class SubscriptionLifecycleWorkflow implements SubscriptionWorkflow { ... }
```

- **PINNED** - drain in-flight on old Workers, then deploy.
- **AUTO_UPGRADE** - long-runners pick up newer compatible code automatically.

---

<!-- _class: section -->

###### Day 4 · Morning

# Worker sizing & Task Queue design

Sized for resource profile, not business domain.

---

<!-- _class: dense -->

# The levers

| Setting | Controls |
| --- | --- |
| `maxConcurrentWorkflowTaskExecutionSize` | In-flight workflow decisions on this Worker |
| `maxConcurrentActivityExecutionSize` | In-flight Activity attempts |
| `ResourceBasedTuner` | Auto-scale Worker slots vs CPU / memory targets |
| `setUsingVirtualThreads(true)` (JDK 21+) | Threads = cheaper; more Activity concurrency |
| Number of Task Queues | One pool per resource profile |

---

<!-- _class: code -->

## Manual sizing

```java
Worker worker = factory.newWorker(
    "io-heavy",
    WorkerOptions.newBuilder()
        .setMaxConcurrentActivityExecutionSize(200)
        .setMaxConcurrentWorkflowTaskExecutionSize(20)
        .build());
```

> I/O-heavy workload: many concurrent Activities, few workflow tasks.

---

<!-- _class: code -->

## Resource-based tuner

```java
ResourceBasedTuner tuner =
    ResourceBasedTuner.newBuilder()
        .setControllerOptions(
            ResourceBasedControllerOptions.newBuilder()
                .setTargetMemoryUsage(0.75)
                .setTargetCpuUsage(0.80)
                .build())
        .build();

Worker worker = factory.newWorker(
    "payments", WorkerOptions.newBuilder().setWorkerTuner(tuner).build());
```

> Auto-fit Worker slot counts to host capacity. Best fit for mixed workloads.

---

<!-- _class: section -->

###### Day 4 · Morning

# Observability

Metrics on day one.

---

<!-- _class: dense -->

# Key SDK metrics

| Metric | Tells you |
| --- | --- |
| `temporal_workflow_task_schedule_to_start_latency` | Worker capacity vs demand |
| `temporal_workflow_completed_total` | Throughput |
| `temporal_workflow_failed_total` | Real failures |
| `temporal_activity_execution_failed_total` | Bad downstream / retry config |
| `temporal_sticky_cache_size` | Replay overhead / memory health |
| `temporal_activity_schedule_to_start_latency` | Activity backlog |

> Wire via Micrometer → Prometheus → your existing Grafana.

---

<!-- _class: code -->

## Micrometer wiring

```java
PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

Scope scope = new RootScopeBuilder()
    .reporter(new MicrometerClientStatsReporter(registry))
    .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));

WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
    WorkflowServiceStubsOptions.newBuilder().setMetricsScope(scope).build());
```

---

<!-- _class: lab -->

###### Lab · Day 4 AM

# Local dashboard

```bash
make stack-obs        # Prometheus + Grafana
make temporal         # dev server with --metrics-port 7234
open http://localhost:3000
make load-transform N=50
```

In Grafana:

1. Open the "Temporal Training - Overview" dashboard.
2. Watch `temporal_workflow_completed_total` climb.
3. Generate a failure; see `temporal_workflow_failed_total` increment.

---

<!-- _class: section -->

###### Day 4 · Morning

# Namespace strategy

Isolation boundary, not a routing primitive.

---

<!-- _class: dense -->

# When to split namespaces

| Scenario | Namespace shape |
| --- | --- |
| Dev / staging / prod | One namespace per environment |
| Regulated tenant isolation | One namespace per tenant |
| Shared SaaS tenants | One namespace per env; tenant ID in Search Attributes |
| Different retention SLAs | Separate namespace per retention class |

> Namespace ≠ Task Queue. Task Queue routes work; Namespace bounds it.

---

<!-- _class: section -->

###### Day 4 · Afternoon

# Testing

In-process Workflow tests with time skipping.

---

<!-- _class: code -->

## TestWorkflowEnvironment

```java
TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
Worker worker = env.newWorker("reminder");
worker.registerWorkflowImplementationTypes(ReminderWorkflowImpl.class);
worker.registerActivitiesImplementations(new ReminderActivitiesImpl());
env.start();

ReminderWorkflow stub = env.getWorkflowClient().newWorkflowStub(
    ReminderWorkflow.class,
    WorkflowOptions.newBuilder().setTaskQueue("reminder").build());

String result = stub.run("hello");
```

> No Docker. No network. *Time skipping* - a 30-day reminder completes in milliseconds.

---

<!-- _class: lab -->

###### Lab · Day 4 PM

# In-process tests

```bash
make run-testing
```

- The test uses `Workflow.sleep(Duration.ofDays(1))`.
- It still completes in <1 second.
- Try changing the sleep to 30 days; same test time.

---

<!-- _class: section -->

###### Day 4 · Afternoon

# Workflow replay testing

Catching determinism regressions before they reach production.

---

<!-- _class: code -->

## Capture & replay

```bash
# Capture
temporal workflow show --workflow-id order-1001 \
  --output json > histories/order-1001.json
```

```java
// Replay
@Test
void replaysProductionHistory() throws Exception {
  WorkflowReplayer.replayWorkflowExecutionFromResource(
      "histories/order-1001.json", OrderSagaWorkflowImpl.class);
}
```

> Refactor breaks an in-flight workflow → CI fails before you ship.

---

<!-- _class: lab -->

###### Lab · Day 4 PM

# Build a replay corpus

For a Workflow you wrote on Day 1-2:

1. Run 3 executions covering: happy path, retry, cancellation.
2. Capture each with `temporal workflow show ... --output json`.
3. Drop them into `src/test/resources/histories/`.
4. Add a `WorkflowReplayer` test per file.
5. Modify the Workflow to reorder Activities; watch the test fail.

<!--
This is the safety net for the rest of the year. Encourage students to
take this pattern back to their team and seed a corpus.
-->

---

<!-- _class: section -->

###### Day 4 · Afternoon

# Migrating Airflow DAGs

A decision framework.

---

<!-- _class: dense -->

# Migrate or not?

| DAG shape | Verdict |
| --- | --- |
| Simple ETL on a fixed schedule | Stay on Airflow, or move scheduler to Temporal Schedules |
| Cross-system orchestration with retries and human steps | Migrate (sweet spot) |
| Kafka-triggered, one execution per key | Migrate (Day 3 pattern) |
| Pure data transformation | Don't migrate. Spark / dbt territory |
| Long-running waits (hours, days, humans) | Migrate. Airflow handles this poorly |
| Tight Airflow operator coupling | Wrap in Activities; the operator is the unit |

---

# Migration order that works

1. **Pick one** DAG that hurts in production.
2. **Map operators → Activities** mechanically. Don't redesign.
3. **Run side by side** for a release cycle.
4. **Cut over** after the Temporal version is clean for two weeks.
5. **Redesign** only after stable. Now use Signals, Updates, Schedules.

> Don't migrate everything. Migrate where Temporal earns its keep.

---

<!-- _class: takeaway -->

# Day 4 takeaways

- Versioning is about preserving old histories, not just deploying new code.
- Size Workers for **resource profile**, not business domain.
- Replay tests are the single safety net for Workflow code changes.
- Not everything is a Workflow. Migrate where Temporal earns its keep.

---

<!-- _class: day -->

###### Day 5

# Saga, Spring Boot & capstone

Real-world Workflow walkthrough. Then build one.

<!--
4 hours. Morning is saga + Spring. Afternoon is capstone (75 min of build
time + 25 min review + 20 min Q&A).
-->

---

<!-- _class: section -->

###### Day 5 · Morning

# Order-processing saga

The canonical demo: payment → inventory → ship; compensate on failure.

---

<!-- _class: code dense -->

## The saga

```java
public String process(String orderId) {
  Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
  try {
    String paymentId = activities.authorizePayment(orderId);
    saga.addCompensation(activities::cancelPayment, paymentId);

    String reservationId = activities.reserveInventory(orderId);
    saga.addCompensation(activities::restoreInventory, reservationId);

    activities.ship(orderId);
    return "COMPLETED";
  } catch (RuntimeException failure) {
    saga.compensate();   // LIFO
    activities.sendFailureNotification(orderId, failure.getMessage());
    return "COMPENSATED";
  }
}
```

---

<!-- _class: lab -->

###### Lab · Day 5 AM

# Run the saga

```bash
make run-saga
```

Try two starts:

```bash
temporal workflow start --task-queue orders \
  --type OrderSagaWorkflow --workflow-id order-OK \
  --input '"order-1001"'

temporal workflow start --task-queue orders \
  --type OrderSagaWorkflow --workflow-id order-fail \
  --input '"fail-at-ship"'
```

> In the Web UI, watch the compensations fire in reverse order.

---

# Orchestration vs choreography

- **Orchestration** - one central Workflow coordinates all steps & compensations. Single audit trail. **Temporal's natural shape.**
- **Choreography** - each service reacts to events, emits its own. No central state.

> For cross-team flows from Airflow + Kafka, orchestration wins.

<!--
War story: when team #3 silently drops an event in a choreographed flow,
nobody notices for 36 hours. Temporal's log shows it immediately.
-->

---

# Compensation rules

1. Register compensation **immediately** after the forward step succeeds.
2. Compensations are **business logic**, not generic undo.
3. Compensations get their own retry policy. Test the failing case.
4. Idempotency on forward AND compensation steps.

---

<!-- _class: section -->

###### Day 5 · Morning

# Saga in Spring Boot

Wiring + interaction patterns.

---

<!-- _class: code -->

## Manual Spring config

```java
@Configuration
class TemporalConfig {
  @Bean WorkflowServiceStubs workflowServiceStubs() {
    return WorkflowServiceStubs.newLocalServiceStubs();
  }
  @Bean WorkflowClient workflowClient(WorkflowServiceStubs s) {
    return WorkflowClient.newInstance(s);
  }
  @Bean(initMethod = "start", destroyMethod = "shutdown")
  WorkerFactory workerFactory(WorkflowClient c, OrderActivities a) {
    WorkerFactory f = WorkerFactory.newInstance(c);
    Worker w = f.newWorker("orders");
    w.registerWorkflowImplementationTypes(OrderSagaWorkflowImpl.class);
    w.registerActivitiesImplementations(a);
    return f;
  }
}
```

<!--
This is the underlying wiring. In production, prefer the
temporal-spring-boot-starter and let it do this.
-->

---

<!-- _class: code -->

## Sync interaction (Update)

```java
WithStartWorkflowOperation<String> start =
    WithStartWorkflowOperation.newBuilder(workflow::process)
        .setArguments(request.orderId()).build();

WorkflowUpdateHandle<String> update = client.startUpdateWithStart(
    start, "submit", WorkflowUpdateStage.COMPLETED, String.class, request);

return update.getResult();
```

> POST endpoint blocks until the workflow returns. One round trip.

---

<!-- _class: code -->

## Async interaction (Signal)

```java
@KafkaListener(topics = "orders")
void onOrder(OrderRequest request) {
  OrderSagaWorkflow workflow = client.newWorkflowStub(
      OrderSagaWorkflow.class,
      WorkflowOptions.newBuilder()
          .setWorkflowId("order-" + request.orderId())
          .setTaskQueue("orders").build());

  BatchRequest batch = client.newSignalWithStartRequest();
  batch.add(workflow::process, request.orderId());
  batch.add(workflow::onUpdate, request);
  client.signalWithStart(batch);
}
```

---

<!-- _class: code -->

## Continue-as-new

```java
@Override
public void run(String subscriptionId, int eventCount) {
  while (true) {
    Workflow.await(this::hasNextEvent);
    handleNextEvent();
    eventCount++;
    if (eventCount >= 1000) {
      Workflow.continueAsNew(subscriptionId, 0);
    }
  }
}
```

> Continue-as-new is a *checkpoint*, not a memory dump. Carry only what's needed.

---

<!-- _class: section -->

###### Day 5 · Afternoon

# Capstone

Redesign a Kafka-triggered Airflow DAG as a Temporal Saga.

---

# The task

> A customer signup flow. Kafka event `customer-signup` arrives with `{userId, email, plan}`. The DAG runs four tasks: create user, charge first month, provision tenant, send welcome email. Failure handling today is ad hoc.

Redesign it as a Saga. Demonstrate one compensation path.

---

# Deliverable plan (75 min)

| Time | Deliverable |
| --- | --- |
| 0-10 | Sketch Workflow signature + Activity interface + compensation order on paper |
| 10-50 | Implement enough Java to run the happy path + one failure path |
| 50-65 | Wire the Kafka trigger with `signalWithStart` |
| 65-75 | Run end-to-end against the local stack; demo one compensation |

---

# Acceptance criteria

1. At least three forward steps.
2. Compensation registered immediately after each step.
3. `@KafkaListener` triggering via `signalWithStart`.
4. One demonstrated failure → compensation visible in the Web UI history.

---

<!-- _class: lab -->

###### Lab · Day 5 PM

# Capstone

```bash
make stack-kafka
make temporal
# Use 07-saga or scaffold your own
```

Go. 75 minutes. Walk the room every 15. Unstick people on Spring config -
the lesson is in the saga shape, not the wiring.

<!--
Hold the line on time. At 50 minutes, stop everyone and check in. If most
are stuck, slow down; if most are done, pull review forward.
-->

---

# Capstone review (25 min)

Two or three volunteer pairs share screen. The room critiques. Cover:

- How did they decide what was a Workflow vs an Activity?
- Where did they put idempotency keys?
- Orchestration or choreography? Why?
- What would they change for a 30-day saga?

<!--
Resist correcting code style. Focus on the four questions above. They are
what the cohort will face on real systems.
-->

---

# Q&A + open migration planning (20 min)

Anchor questions if the room is quiet:

- Pick one Airflow DAG. What's the first thing that would break in Temporal?
- What's your team's hardest distributed-transaction failure? Would a Saga have caught it?
- Where does "I think it ran but I'm not sure" happen in your stack? That's a Workflow.

---

<!-- _class: takeaway -->

# Day 5 takeaways

- Compensation is business logic, not generic undo. Design it on purpose.
- Sync Updates via `startUpdateWithStart` for sync APIs.
- Async Signals via `signalWithStart` for event-driven triggers.
- Continue-as-new is a checkpoint, not a memory dump.

---

<!-- _class: day -->

###### Day 6

# AWS migration & container workloads

Replacing Glue + Lambda + Step Functions. Running Workers in Kubernetes.

<!--
4 hours: morning is AWS migration; afternoon is containers + KEDA.
Two big labs: containerized Worker on kind, and KEDA autoscale.
-->

---

<!-- _class: section -->

###### Day 6 · Morning

# The AWS orchestration problem

Hidden complexity in Lambda + Glue + Step Functions.

---

# State scatters

- **EventBridge** rule fires.
- **Lambda** validates, transforms, sometimes orchestrates.
- **Step Functions** declares state transitions in JSON.
- **Glue** runs Spark / Python shell, writes results to S3.
- **S3** is the handoff medium.

> Every handoff = a chance for state to disagree. Recovery is a runbook.

---

<!-- _class: dense -->

# AWS → Temporal map

| AWS shape | Temporal shape |
| --- | --- |
| EventBridge → Lambda → Step Functions | Consumer (or thin Lambda) `signalWithStart`s a Workflow |
| Step Functions JSON states | Workflow branches through Java code |
| Glue Python writes S3 checkpoints | Activity returns result; history records the run |
| Glue Spark heavy transform | Activity starts Glue, heartbeats runId while polling |
| CloudWatch Lambda retry | `RetryOptions` with typed `ApplicationFailure` |
| S3 handoff between Lambdas | Activity returns S3 URI |
| DynamoDB checkpoint table | Workflow event history |

---

<!-- _class: dense -->

# When to keep AWS compute

| Service | Keep when | Replace when |
| --- | --- | --- |
| Lambda | <100ms, IAM-bound, one-shot | Multi-step coordination, retries, long waits |
| Glue Spark | Large distributed transforms (>10 GB) | Pure data movement; small batches |
| Glue Python | Tiny scripts (<1 min) with Glue catalog | Anything you'd write as a Java Activity |
| Step Functions | Already wired, low-change | Anything needing human steps or code review |

> Temporal supervises; AWS executes the heavy lift.

---

<!-- _class: section -->

###### Day 6 · Morning

# Glue Spark as an Activity

The canonical supervise-AWS-compute pattern.

---

<!-- _class: code dense -->

## Glue activity

```java
@Override
public String runGlueJob(String jobName, String inputS3Uri) {
  String runId = glue.startJobRun(
      StartJobRunRequest.builder()
          .jobName(jobName)
          .arguments(Map.of("--input", inputS3Uri))
          .build()).jobRunId();

  while (true) {
    Activity.getExecutionContext().heartbeat(runId);
    JobRun jobRun = glue.getJobRun(
        GetJobRunRequest.builder().jobName(jobName).runId(runId).build()).jobRun();
    if (jobRun.jobRunState() == JobRunState.SUCCEEDED) return runId;
    if (Set.of(FAILED, TIMEOUT, STOPPED).contains(jobRun.jobRunState()))
      throw ApplicationFailure.newFailure(jobRun.errorMessage(), "GlueJobFailed");
    Thread.sleep(Duration.ofSeconds(15).toMillis());   // back off
  }
}
```

---

<!-- _class: lab -->

###### Lab · Day 6 AM

# Glue Activity (LocalStack)

```bash
make stack-aws        # LocalStack on :4566
make aws-init         # create S3 buckets
make run-aws          # Import Worker
```

In another terminal:

```bash
awslocal s3 cp /tmp/test.csv s3://imports-incoming/test.csv
scripts/start-workflow.sh transform 1 ImportWorkflow \
  "s3://imports-incoming/test.csv"
```

> Watch the heartbeats in the Web UI as the polling loop runs.

---

# S3 reference payloads

```java
record TransformRequest(String inputS3Uri, String outputPrefix) {}
record TransformResult(String outputS3Uri, long rowCount) {}
```

- Workflow history holds **URIs + counts**.
- Activity owns the bytes.
- Soft limit ~50 KB per payload; large data via S3.

> Workflow history is small. URIs travel cheap.

---

<!-- _class: lab -->

###### Lab · Day 6 AM

# Replace S3 checkpoints

Take a hypothetical existing pipeline that writes a checkpoint S3 key after every step.

1. Identify which checkpoints are *resume points*. Those become Workflow state.
2. Identify which checkpoints are *handoffs*. Those become Activity return URIs.
3. Sketch the Workflow signature. What's input? What's output?

> No new code; redesign on paper. 15 minutes.

---

<!-- _class: code -->

## Step Functions → Temporal

```java
@Override
public void run(String inputS3Uri) {
  ValidatedFile file = activities.validate(inputS3Uri);
  String transformedUri = activities.transform(file.cleanInputUri());
  LoadResult loadResult = activities.load(transformedUri);
  activities.publishNotification(loadResult);
}
```

Compare to the equivalent ASL: ~30 lines of JSON state machine with `Resource` arns.

---

<!-- _class: section -->

###### Day 6 · Afternoon

# Workers as containers

No HTTP server. Process-level probes. Graceful shutdown.

---

# Mental model

- A Worker is a long-lived process polling Task Queues *outbound*.
- **No inbound traffic.** No Service, no Ingress.
- Health = "is the process polling?" Use exec or actuator probes.
- Graceful shutdown = drain in-flight Activities; SIGTERM, then heartbeat-cancel.

---

<!-- _class: code -->

## Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/worker.jar /app/worker.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/worker.jar"]
```

---

<!-- _class: lab -->

###### Lab · Day 6 PM

# Docker build

```bash
cd examples/runnable/08-aws-containers
docker build -t temporal-transform-worker:dev .

docker run --rm \
  -e TEMPORAL_ADDRESS=host.docker.internal:7233 \
  temporal-transform-worker:dev
```

> Confirm the Worker connects to the host's Temporal and starts polling.

---

<!-- _class: code dense -->

## Kubernetes Deployment

```yaml
spec:
  replicas: 2
  strategy:
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  template:
    spec:
      terminationGracePeriodSeconds: 120   # >= longest Activity timeout
      containers:
        - name: worker
          image: <ecr>/temporal-transform-worker:latest
          readinessProbe:
            exec: { command: ["sh", "-c", "pgrep -f worker.jar > /dev/null"] }
          livenessProbe:
            exec: { command: ["sh", "-c", "pgrep -f worker.jar > /dev/null"] }
```

> `terminationGracePeriodSeconds` ≥ longest `startToCloseTimeout`.

---

<!-- _class: lab -->

###### Lab · Day 6 PM

# K8s deploy on kind

```bash
make kind-up          # cluster + KEDA
make kind-load        # build + load Worker image
kubectl apply -f examples/runnable/08-aws-containers/k8s-worker-deployment.yaml
kubectl rollout status deployment/temporal-transform-worker
kubectl logs -l app=temporal-transform-worker --tail=20
```

> Confirm the Worker polls the cluster's Temporal address.

---

<!-- _class: code -->

## KEDA temporal scaler

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
spec:
  scaleTargetRef: { name: temporal-transform-worker }
  minReplicaCount: 1
  maxReplicaCount: 10
  triggers:
    - type: temporal
      metadata:
        endpoint: temporal-frontend.temporal.svc.cluster.local:7233
        namespace: production
        taskQueue: transform
        queueType: ActivityTaskQueue
        targetQueueSize: "20"
        activationTargetQueueSize: "5"
```

> Native scaler polls `DescribeTaskQueue`. No Prometheus exporter needed.

---

<!-- _class: lab -->

###### Lab · Day 6 PM

# KEDA autoscale

```bash
kubectl apply -f examples/runnable/08-aws-containers/keda-scaledobject.yaml
make load-transform N=200
kubectl get scaledobject,pods -l app=temporal-transform-worker -w
```

> Watch replica count climb from 1 → ~5 as backlog grows.

---

# Glue Activities in containers

- Worker pod runs Glue-orchestration Activities.
- **IRSA**, not access keys: `eks.amazonaws.com/role-arn` on the ServiceAccount.
- Outbound to Temporal frontend (Cloud or self-hosted ELB).
- Outbound to AWS APIs via VPC endpoints.

> No bundled access keys. IRSA + VPC endpoints is the production shape.

---

<!-- _class: dense -->

# Temporal Cloud vs EKS self-hosted

| Concern | Temporal Cloud | EKS self-hosted |
| --- | --- | --- |
| Setup time | Hours | Weeks |
| Persistence | Managed (Cassandra) | You run PostgreSQL / Cassandra |
| Upgrades | Automatic | You schedule |
| Multi-region | Built-in (premium) | You design replication |
| Cost shape | Per-action | Fixed infra |
| Audit / compliance | SOC2, HIPAA tiers | You provide evidence |

> Use Cloud unless you have a specific reason not to.

---

<!-- _class: lab -->

###### Lab · Day 6 PM

# End-to-end S3 → Temporal → S3

```bash
make stack-aws        # LocalStack
make temporal         # dev server
make run-aws          # Worker

awslocal s3 cp test-input.csv s3://imports-incoming/
scripts/start-workflow.sh transform end2end ImportWorkflow \
  "s3://imports-incoming/test-input.csv"
awslocal s3 ls s3://imports-output/
```

Verify:

- Three Activity completions in the Web UI.
- Workflow history holds URIs + `rowCount`, not file bytes.

---

<!-- _class: takeaway -->

# Day 6 takeaways

- Temporal replaces orchestration **state**, not all compute. Keep Glue Spark; replace Step Functions JSON.
- Workers have no inbound traffic. Use `exec` probes or add Actuator deliberately.
- KEDA's native Temporal scaler is the right one.
- Cloud is the default for new deployments. Self-host only with a clear reason.

---

<!-- _class: section -->

###### Course close

# What you have now

A complete Temporal mental model and the patterns to ship with.

---

# Where to go next

- Take the **capstone** from Day 5 back to your team. Ship it side-by-side with the existing implementation.
- Stand up the **observability stack** in your real env. Get the metrics flowing first.
- Start the **replay corpus**. One captured history per non-trivial Workflow.
- Pick one **Airflow DAG** to migrate using the framework.

---

<!-- _class: takeaway -->

# The four habits

1. When you'd write a runbook, write a Workflow instead.
2. Workflow code is deterministic; all I/O lives in Activities.
3. `signalWithStart` / `startUpdateWithStart` are the bridge primitives.
4. Capture histories; replay them in CI.

---

<!-- _class: quote -->

> Your hardest distributed-transaction bug today is a feature Temporal already solved.

The cost is learning a new model. The reward is fewer runbooks.

---

## Resources

Docs

https://docs.temporal.io

Java SDK

https://github.com/temporalio/sdk-java

Slides

https://temporal-training.slides.codermana.com/temporal-fundamentals/

Course repo

https://github.com/CoderMana/temporal-training
