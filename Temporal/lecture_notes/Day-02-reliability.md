# Day 2 - Building Reliable Workflows

## Objectives

- Run Activities asynchronously, in parallel, and survive partial failure.
- Choose retries, timeouts, and heartbeats deliberately - not by default.
- Reinforce the determinism rules through replay-safe time and cancellation.
- Interact with running Workflows via Signals, Queries, and Updates.
- Replace Airflow's scheduler with Temporal Schedules; compose Workflows with Child Workflows.

## Prerequisites Check (5 min)

Everyone should have completed Day 1's Hello Temporal. Confirm:

- Their local Temporal stack restarts cleanly (`scripts/start-temporal.sh`).
- They can read an Event History in the Web UI without prompting.

If anyone is still red on either, the morning's async examples will look like magic. Take 5 minutes at the top to backfill.

---

## Morning - 2 hrs

### 1. Async and parallel Activity execution (35 min) `[lab]`

**Talking point.** A synchronous Workflow that calls three Activities back-to-back wastes time the same way synchronous Java does. The fix is `Async.function(...)`, which returns a `Promise<T>` instead of blocking the Workflow. The Workflow loop yields control after recording `ActivityTaskScheduled` and resumes only when results come back.

The key word is **Workflow loop**. `Promise.get()` does *not* block an OS thread; it deschedules the Workflow until the awaited event lands. A single Worker JVM can host thousands of suspended Workflows because each one is just heap state, not a parked thread.

**Landing examples.**

```bash
scripts/show-example.sh 02-reliability/async_activity.java
scripts/show-example.sh 02-reliability/parallel_fanout_allof.java
```

Walk through `Async.function(activities::extract, batchDate)` and contrast with the synchronous version from Day 1. Then show `Promise.allOf(counts).get()` and stress that the Workflow is suspended across all of them in parallel - not three threads.

**Lab.**

```bash
scripts/run-example.sh async
```

In the Web UI, point out that all three `ActivityTaskScheduled` events appear before any `ActivityTaskCompleted`. That's the visible signature of parallel fan-out.

**Discussion prompt.** "If you fan out 10,000 partitions at once, what breaks?" Lead to Workflow history size (next-day topic) and Worker capacity. Set the hook for Day 4's sizing discussion.

### 2. Retries, timeouts, and heartbeating (30 min)

**Talking point.** Three timeouts, one retry policy, one heartbeat - know what each controls or you will misuse all of them.

| Setting                      | Controls                                                              |
| ---------------------------- | --------------------------------------------------------------------- |
| `startToCloseTimeout`        | One attempt's wall-clock budget                                       |
| `scheduleToCloseTimeout`     | Total budget across **all** retry attempts                            |
| `scheduleToStartTimeout`     | How long an Activity can wait in the Task Queue before a Worker picks it up |
| `heartbeatTimeout`           | Max gap between heartbeats; Worker death detection                    |
| `RetryOptions`               | Backoff schedule and attempt cap                                      |

**Landing examples.**

```bash
scripts/show-example.sh 02-reliability/retry_and_timeouts.java
scripts/show-example.sh 02-reliability/heartbeat_long_activity.java
```

In the retry example, show that 6 attempts with 5s init * 2.0 backoff fits inside the 30-minute schedule-to-close budget. In the heartbeat example, point out the try/catch for `ActivityCanceledException` - cleanup before re-throwing.

**Discussion prompt.** "Your Activity takes 4 hours. The Worker host gets rebooted at hour 3. What happens?" Answer with heartbeat: the heartbeat timeout fires, the Activity is rescheduled, and if you recorded heartbeat details you can resume from page N. Without heartbeats: silence until `startToCloseTimeout` fires, then a clean retry from page 0. Cost of forgetting heartbeats = wasted compute.

**Common student errors.**

- Setting `startToCloseTimeout` shorter than the Activity's real worst case. Activity attempts get killed mid-flight.
- Setting `scheduleToCloseTimeout` shorter than `startToCloseTimeout * maxAttempts * backoff`. Retries are silently truncated.
- Heartbeating in a tight loop without doing real work between - the heartbeat throttles server-side, so it doesn't hurt Temporal, but it does hammer any external API in the same loop.

### 3. Partial failure and cancellation (25 min)

**Talking point.** Fan-out plus failure = partial success. The Workflow needs to decide: abort all, compensate the succeeded ones, or accept partial completion. Each is a business choice, not a framework default.

Separately, `CancellationScope` is how you say "this work might need to stop." Inside a scope, you can call `.cancel(reason)` to propagate cancellation into running Activities. The Activity sees `ActivityCanceledException` at its next heartbeat and gets to clean up.

**Landing examples.**

```bash
scripts/show-example.sh 02-reliability/partial_failure.java
scripts/show-example.sh 02-reliability/cancellation_scope.java
```

`partial_failure.java` shows accept-partial-completion: catch `ActivityFailure` per Promise, record the outcome, continue. `cancellation_scope.java` shows a deadline race - start a long-running export, race it against a deadline, cancel on timeout, let the Activity tear down via heartbeat.

**Discussion prompt.** "If you cancel an Activity, is the Workflow notified?" Yes - `Promise::get` throws `CanceledFailure`. You decide whether that's a failure (re-raise) or expected (return a sentinel). Cancellation is cooperative; the Activity must be heartbeating or sleeping in a cancellable way for the cancel to land.

### 4. Determinism constraints - the rules that keep replay honest (30 min)

**Talking point.** Day 1 introduced determinism. Today's reliability work makes the rules concrete. There are five families of non-determinism that will burn Workflows:

1. **Time** - `System.currentTimeMillis`, `Instant.now`, `LocalDateTime.now`. Use `Workflow.currentTimeMillis()`.
2. **Random** - `Math.random`, `new Random()`, `UUID.randomUUID()`. Use `Workflow.newRandom()` and `Workflow.randomUUID()`.
3. **I/O** - file, network, database. Move into Activities.
4. **Concurrency** - `Thread.sleep`, `CompletableFuture`, `ExecutorService`. Use `Workflow.sleep`, `Async.function`, `Workflow.newPromise`.
5. **Iteration order over collections** - `HashMap` iteration is JVM-version-dependent. Use `LinkedHashMap` or sort the keys.

**Landing example.**

```bash
scripts/show-example.sh 02-reliability/workflow_time.java
```

Show `Workflow.sleep(Duration.ofHours(6))` and call out: this records `TimerStarted`, the Worker forgets the Workflow, the server fires a `TimerFired` six hours later, a Worker picks up the WorkflowTask, and execution resumes. **No JVM thread parks for six hours.** This is durable sleep, and it's why Temporal Workflows can have human-timescale lifetimes.

**Discussion prompt.** "What's the longest `Workflow.sleep` you'd write in production?" There's no upper bound from Temporal; the practical limit is Workflow execution timeout and history-event count. Bridge to Day 5 (continue-as-new).

**Common student error.** Using `Thread.sleep` "just for a quick test." The Worker thread parks, blocking other Workflows on the same thread pool, and the sleep doesn't survive a restart. Caught by tests on Day 4.

---

## Break (15 min)

---

## Afternoon - 2 hrs

### 5. Signals and Queries - interacting with running Workflows (30 min) `[lab]`

**Talking point.** Signals push data **in**; Queries pull data **out**. Both are async-friendly bridges between the outside world and a running Workflow.

- A **Signal** is a fire-and-forget message. It's recorded in history and delivered to a `@SignalMethod`. Signals can arrive while the Workflow is sleeping; the Workflow wakes when its `await` predicate becomes true.
- A **Query** is a read-only function. It runs against the Workflow's *current in-memory state*, doesn't write to history, and must not call Activities or modify state.

**Landing example.**

```bash
scripts/show-example.sh 03-interactions/signals_queries.java
```

Trace: the Workflow blocks on `Workflow.await(() -> state.startsWith("APPROVED"))`; a Signal sets `state`; the await re-evaluates and unblocks. A Query reads `state` without disturbing anything.

**Lab.**

```bash
scripts/run-example.sh signals
```

Have students start the approval Workflow, send a Signal via CLI:

```bash
temporal workflow signal --workflow-id approval-demo --name approve --input '"alice"'
temporal workflow query  --workflow-id approval-demo --type currentState
```

**Discussion prompt.** "Why isn't a Query just a method call?" Because the Workflow may be on any Worker in the fleet; the Query is routed through the server to wherever the Workflow's state lives.

### 6. Updates - synchronous request/response against a running Workflow (25 min) `[lab]`

**Talking point.** Signals are async; Queries are read-only. Updates are the missing third option: a synchronous, validated, write-capable RPC into a running Workflow. The caller blocks until the Workflow's `@UpdateMethod` returns.

Crucially, `@UpdateValidatorMethod` runs *before* the update is admitted to history - so rejection is cheap and doesn't pollute the audit trail.

**Landing examples.**

```bash
scripts/show-example.sh 03-interactions/update_completed.java
scripts/show-example.sh 03-interactions/update_with_start.java
```

The first shows the basic shape: `startUpdate(name, COMPLETED, ResultType, args).getResult()`. The second shows `startUpdateWithStart` - the Workflow is created on the first call if it doesn't exist. This is the pattern for "POST /orders" where the caller wants the order ID back synchronously.

**Discussion prompt.** "When would you use Signal+Query instead of Update?" Answer: when you don't need synchronous completion (Signal+Query is older and works against older Temporal versions). Update is the right primitive for sync APIs going forward.

### 7. Schedules - replacing Airflow's scheduler (25 min) `[airflow]` `[lab]`

**Talking point.** Airflow's scheduler decides when DAGs run. In Temporal, that responsibility moves to the **Schedule** primitive - a server-side trigger that starts Workflows on a calendar or interval, with policies for catch-up, overlap, and jitter.

Key differences from Airflow:

- Schedules are durable Temporal objects, not Python decorators. They survive deploys.
- Overlap policy (`SKIP`, `BUFFER_ONE`, `ALLOW_ALL`, etc.) is explicit, not the default-on `depends_on_past=True` ergonomic trap.
- Catch-up is bounded - you choose whether a missed window backfills.

**Landing example.**

```bash
scripts/show-example.sh 03-interactions/schedule_interval.java
```

Walk the builder: `ScheduleSpec` (when), `ScheduleActionStartWorkflow` (what), `SchedulePolicy` (how to handle overlap), `ScheduleState` (paused, notes).

**Lab.**

```bash
scripts/run-example.sh schedules
temporal schedule list
temporal schedule describe --schedule-id daily-sales-report-schedule
```

**Discussion prompt.** "Your Airflow DAG runs every hour. The 03:00 run takes 90 minutes. What does the 04:00 run do?" In Airflow: depends on `max_active_runs`. In Temporal: explicit `ScheduleOverlapPolicy` - and you write it down, you don't inherit it.

### 8. Child Workflows and workflow timeouts (25 min) `[airflow]`

**Talking point.** A Child Workflow is a Workflow started by another Workflow. Use children when you want **independent identity, retries, and lifetime** for a subprocess - not just to avoid a long function. Each child has its own history, its own timeouts, its own Workflow ID.

Two workflow-level timeouts:

- `WorkflowExecutionTimeout` - the maximum lifetime across *all* continue-as-new continuations. The hard upper bound.
- `WorkflowRunTimeout` - the maximum lifetime of *this individual run*. Forces a fresh continuation.

These aren't Activity timeouts. They protect against stuck Workflows, not stuck Activities.

**Landing examples.**

```bash
scripts/show-example.sh 03-interactions/child_workflow.java
scripts/show-example.sh 03-interactions/workflow_and_run_timeouts.java
```

In the child example, point out the two children on *different* Task Queues - this is how you route fraud work to one Worker pool and shipping work to another. In the timeout example, contrast the two values: 7-day execution cap, 12-hour per-run cap.

**Discussion prompt.** "Could you do this with one big Workflow plus Activities?" Yes. The trade-off: Child Workflow gives you a separately addressable execution (you can Query/Signal/cancel it). Activities are simpler when you don't need that.

**Mapping note `[airflow]`.** Airflow's `SubDagOperator` is the closest analogue but is deprecated for good reason. Child Workflows are the proper, isolated version.

---

## Windows PowerShell shortcuts

```powershell
scripts/show-example.ps1 02-reliability/async_activity.java
scripts/show-example.ps1 02-reliability/parallel_fanout_allof.java
scripts/show-example.ps1 02-reliability/heartbeat_long_activity.java
scripts/show-example.ps1 02-reliability/cancellation_scope.java
scripts/show-example.ps1 03-interactions/signals_queries.java
scripts/show-example.ps1 03-interactions/update_with_start.java
scripts/show-example.ps1 03-interactions/schedule_interval.java
scripts/show-example.ps1 03-interactions/child_workflow.java
scripts/run-example.ps1 async
scripts/run-example.ps1 signals
scripts/run-example.ps1 schedules
```

---

## Instructor Notes

- The async point that lands hardest: **`Promise.get()` blocks the Workflow loop, not an OS thread.** A single JVM can host tens of thousands of suspended Workflows. Say this out loud.
- Heartbeats are the recovery checkpoint for long Activities. Cheap to add, expensive to forget.
- Timeouts should describe the real operational contract, not just an arbitrary default. If you can't say *why* the timeout is 5 minutes, it's wrong.
- For Updates vs Signal+Query: Updates are the modern primitive. Use them for any RPC where the caller wants the result. Keep Signal+Query for older clusters or genuinely async flows.
- Schedules replace the *scheduler*, not the entire orchestration story. The Workflow it starts is still where the work lives.
- Child Workflows give you separately addressable executions. If you don't need to Signal/Query/cancel them independently, an Activity is simpler.
