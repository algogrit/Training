# Day 3 - Kafka Integration & Event-Driven Patterns

## Objectives

- Draw a clean line between Kafka's job (event log) and Temporal's job (durable execution).
- Bridge Kafka to Temporal correctly - using `signalWithStart`, not bare `start`.
- Build Kafka consumer Activities and idempotent producer Activities.
- Recognize the outbox pattern and when it's the right tool.
- Fan out with Kafka partitions; route to DLQ after Temporal retries exhaust.

## Prerequisites Check (5 min)

- Local Kafka is up: `scripts/start-stack.sh kafka` (broker on `localhost:9092`).
- `kcat` installed (`kcat -V`) - useful for poking the broker by hand.
- Day 2's signal example still works locally.

If Kafka isn't up, the afternoon lab will stall. Fix this at the top.

---

## Morning - 2 hrs

### 1. Temporal + Kafka architecture patterns (25 min) `[kafka]`

**Talking point.** Kafka and Temporal both look like "durable middleware" from a distance, but they own different concerns:

| Concern                                     | Owner    |
| ------------------------------------------- | -------- |
| Append-only event log, replayable by offset | Kafka    |
| Fan-out to many independent consumers       | Kafka    |
| State of a single business transaction      | Temporal |
| Retry/timeout/compensation logic            | Temporal |
| Long-running human/external waits           | Temporal |

The healthy pattern: **Kafka is the transport between systems; Temporal is the brain inside one system.** A Kafka event triggers a Temporal Workflow; the Workflow does the work and may produce more Kafka events when complete.

The anti-pattern: using Kafka topics as a queue between Workflow steps inside one system. Temporal already owns step state; you're rebuilding it badly.

**Discussion prompt.** "Today, what happens in your stack when a Kafka consumer crashes mid-batch?" Most teams will describe redelivery + manual recovery. Set the hook: Temporal makes that automatic.

### 2. Replacing Kafka-triggered Airflow DAGs with Signal-driven Workflows (25 min) `[airflow]` `[kafka]`

**Talking point.** The common Airflow setup: a sensor task polls Kafka, an `ExternalTaskSensor` waits for the right event, then the DAG fires. The polling is slow, the sensor is brittle, and the DAG knows nothing about prior events for the same key.

In Temporal: one Workflow per business entity (e.g. one per orderId), kept alive by Signals. The Kafka consumer is a thin process that translates `ConsumerRecord` -> `signalWithStart` and commits offsets.

The `signalWithStart` part is the critical detail. Bare `WorkflowClient.start(...)` throws `WorkflowExecutionAlreadyStarted` on the second event for the same key. `signalWithStart` creates the Workflow on the first event and signals the existing execution on every event after. This is the foot-gun every student hits.

**Landing example.**

```bash
scripts/show-example.sh 04-kafka/signal_bridge.java
```

Walk through `client.newSignalWithStartRequest()`, the two `batch.add(...)` calls (one for the start args, one for the signal payload), and the single `client.signalWithStart(batch)` RPC.

**Discussion prompt.** "What happens to Kafka offset commits if the Workflow crashes after the Signal is recorded?" Answer: commits should happen *after* `signalWithStart` returns successfully. If the bridge dies before commit, redelivery is safe because Temporal deduplicates by Workflow ID + signal idempotency (if you use a signal name + ID strategy).

### 3. Kafka consumer as a Temporal Activity (30 min) `[kafka]`

**Talking point.** Two architectural choices for "Workflow pulls from Kafka":

- **Bridge process pattern** (preferred for triggers): a standalone consumer translates events to Signals. The Workflow doesn't know Kafka exists.
- **Activity-driven poll pattern**: a Workflow calls a `pollBatch` Activity that subscribes, polls, and returns a batch. Useful when polling is itself part of orchestrated work (e.g. "drain this topic of pending events, then run reconciliation").

Use the bridge for triggers. Use the Activity when polling is deliberate, scheduled, bounded work.

**Landing example.**

```bash
scripts/show-example.sh 04-kafka/kafka_consumer_activity.java
```

Walk:

- `consumer.subscribe(...)` inside an Activity (Activities are full Java code; there are no determinism rules here).
- Heartbeat per record with `topic:partition:offset` as the heartbeat detail - this is your resume cursor on retry.
- `consumer.commitSync()` after the loop completes - **never before**. Commit only after the unit of work is durably accepted upstream.

**Discussion prompt.** "If the Activity is killed at record 50 of 100, what happens?" Without resume-from-heartbeat: the retry replays all 100 (Kafka redelivers from the last commit). With resume-from-heartbeat: the retry reads the last heartbeat detail and skips ahead. Show both as design choices.

**Common student error.** Using auto-commit (`enable.auto.commit=true`) and then wondering why retries silently skip records. Disable auto-commit. Always.

### 4. Producing to Kafka from Activities + the outbox pattern (40 min) `[kafka]`

**Talking point.** Producing to Kafka from a Workflow happens inside an Activity (Workflow code does no I/O). Two correctness requirements:

1. **Idempotence** - `enable.idempotence=true`, `acks=all`. The producer handles deduplication on retry within a single producer session.
2. **At-least-once with a stable key** - on Activity retry, the same record is sent again. Downstream consumers must tolerate duplicates by key.

The outbox pattern is the next level: when you need a database write and a Kafka publish to be atomic, write both into the same DB transaction and publish later from an outbox table. Temporal makes this easier - the Activity owns the transaction; a separate Workflow drains the outbox.

**Landing examples.**

```bash
scripts/show-example.sh 04-kafka/producer_activity_idempotent.java
scripts/show-example.sh 04-kafka/outbox_activity.java
```

In the producer example, point at `ENABLE_IDEMPOTENCE_CONFIG=true` and `ACKS_CONFIG=all`. In the outbox example, the key line is `orderRepository.save(order)` and `outboxRepository.save(...)` inside one `transactionTemplate.execute`. The Kafka publish is a downstream concern - either a separate Workflow that polls the outbox, or Debezium CDC.

**Discussion prompt.** "Why not just `producer.send(...)` and `db.save(...)` in the same Activity?" Answer: the Activity is atomic from the Workflow's perspective (it succeeded or didn't), but the two side effects can't be made atomic across systems without 2PC. Outbox sidesteps that by putting both side effects in one DB transaction.

---

## Break (15 min)

---

## Afternoon - 2 hrs

### 5. End-to-end Kafka -> Temporal -> Kafka pipeline (50 min) `[lab]` `[kafka]`

**Lab goal.** Build the round-trip: a producer sends an order; the bridge `signalWithStart`s a Workflow; the Workflow decides; an outcome producer publishes the result. Use the runnable project.

```bash
scripts/run-example.sh kafka
```

While that runs, in two terminals:

```bash
# Terminal 1 - produce a synthetic order
kcat -b localhost:9092 -t orders -P -k "order-1" <<< 'NEW:line-item-A'

# Terminal 2 - consume the outcome topic
kcat -b localhost:9092 -t order-outcomes -C -o end -f 'key=%k value=%s\n'
```

In the Web UI, find the resulting `order-order-1` Workflow and walk its history. Then produce a second event for the same key and watch the existing Workflow receive a Signal (no new execution, no `WorkflowExecutionAlreadyStarted` error - that's the `signalWithStart` payoff).

**Common student errors.**

- Topic doesn't exist - the broker auto-creates if `auto.create.topics.enable=true` in the Compose stack; if not, create explicitly.
- Wrong Task Queue string between bridge and Worker - Workflow stuck in `Running` with zero events past `Started`.
- Bridge committing offset before `signalWithStart` returns - redelivery becomes lossy if the bridge crashes between commit and Signal.

### 6. Fan-out with Kafka partitions (30 min) `[lab]` `[kafka]`

**Talking point.** Kafka partitions parallelize processing by key. Two ways to fan out in Temporal:

- **Outside the Workflow** - one Workflow per partition, started by the bridge. Independent histories; easy to reason about; many Workflows running.
- **Inside the Workflow** - one Workflow that processes a *range* of partitions in parallel Activities. Single history; centralized state; bounded by Workflow history size limit.

Pick based on whether the partitions share business state. Orders by customer: outside. Batch reconciliation: inside.

**Landing example.**

```bash
scripts/show-example.sh 04-kafka/partition_fanout.java
```

Show `Promise.allOf(counts).get()` for inside-Workflow fan-out. Mention the implicit cap: don't fan out 1,000 partitions inside one Workflow - that's 2,000+ history events just for scheduling.

**Discussion prompt.** "Your topic has 24 partitions. You want to drain it daily. Inside or outside?" Lead them to a hybrid: one Workflow per range of 4 partitions (six Workflows total), each fans out 4 in parallel inside. Concrete numbers force concrete reasoning.

### 7. Dead-letter handling - DLQ vs Temporal retry exhaustion (35 min) `[kafka]`

**Talking point.** Two different failure modes look the same from Kafka's perspective:

| Failure type                          | Where it belongs                     |
| ------------------------------------- | ------------------------------------ |
| Transient (network, throttling)       | Temporal retry (already free)        |
| Poison message (malformed payload)    | DLQ topic for human triage           |
| Business rule rejection (invalid order) | DLQ or audit topic; not retried     |
| Unknown / catch-all                   | DLQ after Temporal retry exhausts    |

Temporal retries solve transient failures for free. DLQs solve everything that no amount of retrying will fix. A Workflow that hits Temporal retry exhaustion should fall through to a DLQ Activity - not crash, not infinitely loop.

**Landing example.**

```bash
scripts/show-example.sh 04-kafka/dlq_after_retry_exhaustion.java
```

Walk: the validate Activity has `maxAttempts=5`. After 5 failures it throws `ActivityFailure`. The Workflow catches that, calls the `dlq.publish` Activity, and exits cleanly. The Workflow itself completes successfully - the *order* failed, but the *Workflow* did its job.

**Discussion prompt.** "Why not just let the Workflow fail?" Two reasons: (1) a failed Workflow needs operator action to dismiss; a completed one with a DLQ side effect is self-service. (2) The DLQ has the routing for downstream triage; a failed Workflow doesn't route anywhere.

**Common student error.** Catching the wrong exception type and accidentally treating real `ApplicationFailure` (business rule) the same as `ActivityFailure` (retry exhaustion). Use typed `ApplicationFailure` and match on `getType()` for business cases.

---

## Windows PowerShell shortcuts

```powershell
scripts/show-example.ps1 04-kafka/signal_bridge.java
scripts/show-example.ps1 04-kafka/kafka_consumer_activity.java
scripts/show-example.ps1 04-kafka/producer_activity_idempotent.java
scripts/show-example.ps1 04-kafka/outbox_activity.java
scripts/show-example.ps1 04-kafka/partition_fanout.java
scripts/show-example.ps1 04-kafka/dlq_after_retry_exhaustion.java
scripts/run-example.ps1 kafka
```

---

## Instructor Notes

- Kafka retains events; Temporal owns durable execution state. Different problems, used together.
- **`signalWithStart` is the only correct bridge primitive.** Bare `start` is a foot-gun students will copy from the internet. Say "signalWithStart" out loud at every opportunity.
- Prefer Signals when Kafka events are commands for a specific Workflow instance. Prefer Activity-pull when the Workflow is driving the polling cadence.
- Commit Kafka offsets only after the unit of work has been accepted upstream. Auto-commit is off, always.
- Temporal retry exhaustion and Kafka DLQs solve different problems. Use DLQ routing when an event needs operator triage, not as a generic retry mechanism.
- Idempotent producers + stable keys = at-least-once becomes effectively-once-by-key downstream. That's the deal; downstream must dedupe.
