# Day 4 - Production Engineering

## Objectives

- Version Workflows safely: `getVersion` patching and the versioning behavior API.
- Size Workers and Task Queues deliberately; understand resource-based tuning.
- Wire observability (Prometheus, Grafana, OpenTelemetry, Micrometer).
- Design a namespace strategy for tenants, environments, and retention.
- Test Workflows in-process and replay against captured production histories.
- Apply a decision framework for migrating Airflow DAGs.

## Prerequisites Check (5 min)

- Hello Temporal still runs (Day 1 sanity).
- `mvn` works offline-ish (we'll compile multiple test projects today).
- Prometheus + Grafana stack is up: `scripts/start-stack.sh obs`
  (Prometheus on :9091, Grafana on :3000).
- `scripts/start-temporal.sh` was started after today's update - it now
  enables `--metrics-port 7234` so Prometheus can scrape the dev server.

---

## Morning - 2 hrs

### 1. Why changing Workflow code can break replay (15 min)

**Talking point.** A running Workflow's identity is its **history of events**. When a Worker picks it up after a deploy, the Worker re-executes Workflow code against that history. If the new code makes a *different* sequence of decisions than the recorded history, Temporal raises a non-determinism error.

Three classes of change:

| Change                                  | Safe?                                       |
| --------------------------------------- | ------------------------------------------- |
| Add a new branch never reached by old runs | Safe                                     |
| Reorder Activities in an existing path  | Breaks replay                               |
| Change an Activity argument shape       | Breaks replay if shape was already recorded |
| Add a new Workflow type                 | Safe (it's a different identity)            |
| Change a Workflow timeout setting       | Safe (timeouts are on start, recorded once) |

The fix for the unsafe cases is **versioning**. Two flavors: `getVersion` patching (per change-point) and Workflow Versioning Behavior (per Workflow type).

**Discussion prompt.** "Today, how do you deploy an Airflow DAG that changes step order?" Most answers: wait for in-flight runs to finish, then deploy. Temporal makes you choose explicitly: keep the old runs on old code (pinned) or let them upgrade (auto). The deploy doesn't block.

### 2. `Workflow.getVersion` patching (20 min)

**Talking point.** `getVersion` introduces a versioned branch *at one point in the code*. New runs take the new branch; in-flight runs that already passed the change-point take the old branch (recorded in history). This lets you reorder Activities, change behavior, or fix logic without breaking active executions.

**Landing example.**

```bash
scripts/show-example.sh 05-production/get_version_patch.java
```

Walk: `int version = Workflow.getVersion("charge-before-reserve", DEFAULT_VERSION, 1)`. The string is a stable change-point name. `DEFAULT_VERSION` is the pre-patch code path; `1` is the new path. Once all old executions are done, you can remove the branch (next step: deprecation).

**Common student error.** Reusing the same change-point name for unrelated changes. The change-point name is *identity*; treat it like a database migration name. Never recycle.

### 3. Versioning behavior annotations (15 min)

**Talking point.** Workflow Versioning (the newer API) lets you mark a Workflow type with a behavior policy:

- `PINNED` - in-flight executions stay on compatible Workers during a rollout. Use for short-lived Workflows where you can afford to drain.
- `AUTO_UPGRADE` - long-running executions can move to newer compatible Worker code as soon as it's available. Use for years-long subscriptions, monitoring Workflows.

Combined with Worker deployment versioning, this gives you per-Workflow control without sprinkling `getVersion` everywhere.

**Landing example.**

```bash
scripts/show-example.sh 05-production/versioning_behavior.java
```

Two classes with `@WorkflowVersioningBehavior(PINNED)` and `@WorkflowVersioningBehavior(AUTO_UPGRADE)`. Stress: this is a Workflow-type-level declaration, not a per-execution choice.

**Discussion prompt.** "Your subscription Workflow runs for a year and you ship code monthly. Pinned or auto-upgrade?" Auto-upgrade - pinned would mean a year of legacy Workers running. Be honest: this requires backwards-compatible code changes (the determinism rules still apply).

### 4. Worker sizing, Task Queue design, and tuning (35 min)

**Talking point.** A Worker is a long-lived JVM polling Task Queues. Three sizing levers:

- `maxConcurrentWorkflowTaskExecutionSize` - how many Workflow decisions can be in-flight on this Worker. Workflow tasks are CPU-bound (your code) but very short.
- `maxConcurrentActivityExecutionSize` - how many Activity attempts can be in-flight. Activities are typically I/O-bound and can be very long.
- Task Queue count - one Worker pool per *resource profile*, not per business domain.

Two strategies:

| Strategy                | Use when                                     | Example                                       |
| ----------------------- | -------------------------------------------- | --------------------------------------------- |
| Manual `WorkerOptions`  | You know your workload shape                 | I/O-heavy: 200 concurrent activities, 20 tasks |
| `ResourceBasedTuner`    | Mixed workload, autoscale-friendly           | Target 75% memory, 80% CPU                    |
| Virtual threads (JDK 21+) | Many blocking Activities; reduce thread cost | `setUsingVirtualThreads(true)`                |

**Landing examples.**

```bash
scripts/show-example.sh 05-production/worker_options_manual.java
scripts/show-example.sh 05-production/worker_tuner.java
scripts/show-example.sh 05-production/virtual_threads.java
```

For each: point at *one* knob and explain what would happen if you doubled it. Make it concrete.

**Discussion prompt.** "You have two Workflow types: one fans out 100 Activities, one calls 2. Same Worker pool or split?" If both fit the same resource profile, same pool with capacity to spare for the fan-out. If not, split into two Task Queues. The deciding question is always "what resource does this Worker actually contend on?"

**Common student error.** Sizing for peak load and then running 10 idle Workers. Workers are cheap to scale horizontally - prefer more Workers with smaller capacity over one giant Worker that pins a host.

### 5. Observability - Prometheus, Grafana, Micrometer, OpenTelemetry (35 min) `[lab]`

**Talking point.** Temporal SDK ships with metrics built in - you just have to wire a reporter. Three layers:

- **Server metrics** - the Temporal cluster's own Prometheus endpoint. Use the bundled Grafana dashboard.
- **SDK metrics** - workflow task latency, sticky cache, Activity success/failure rates. Wire via Micrometer + `MicrometerClientStatsReporter`.
- **Custom Activity metrics** - your business counters and gauges. Plain Micrometer, registered from inside `ActivitiesImpl`.

**Landing examples.**

```bash
scripts/show-example.sh 05-production/micrometer_metrics.java
scripts/show-example.sh 05-production/custom_activity_metric.java
```

Walk the wiring: `PrometheusMeterRegistry` -> `MicrometerClientStatsReporter` -> `Scope` -> `WorkflowServiceStubsOptions.setMetricsScope(...)`. Then a custom counter incremented in an Activity.

**Lab.**

Open Grafana (`http://127.0.0.1:3000`) and the bundled Temporal dashboard. Run the Hello example a few times. Watch `temporal_workflow_completed_total` increment. Then run a Workflow that fails on purpose and watch `temporal_workflow_failed_total`.

**OpenTelemetry note.** The SDK supports OTLP-format tracing. For most teams, Prometheus metrics + standard JVM tracing covers 80% of needs; add OTLP when you already have an OpenTelemetry collector.

**Discussion prompt.** "What single metric tells you Workers are undersized?" `temporal_workflow_task_schedule_to_start_latency` rising. Tasks are queueing because nobody's polling. Show it on the dashboard.

---

## Break (15 min)

---

## Afternoon - 2 hrs

### 6. Namespace strategy - multi-tenancy, isolation, retention (20 min)

**Talking point.** A namespace is the unit of isolation, retention, quota, and access control. It is **not** a replacement for Task Queues (which is the routing primitive).

Pick a namespace shape based on what you actually need to isolate:

```bash
scripts/show-example.sh 05-production/namespace_strategy.md
```

| Scenario                       | Namespace shape                                          |
| ------------------------------ | -------------------------------------------------------- |
| Dev / staging / prod           | One namespace per environment: `orders-dev`, `orders-prod` |
| Regulated tenant isolation     | One namespace per tenant (audit boundary)                |
| Shared SaaS tenants            | One namespace per env, tenant ID in Search Attributes    |
| Different retention SLAs       | Separate namespace per retention class                   |

**Discussion prompt.** "Your compliance team says PHI Workflows have 7-year retention; the rest are 90 days. One namespace or two?" Two. Retention is a namespace setting; you can't mix.

### 7. Testing Workflows - the Temporal Java testing framework (35 min) `[lab]`

**Talking point.** `TestWorkflowEnvironment` is an in-process Temporal server you spin up in JUnit. No Docker, no network. It supports **time skipping** - `Workflow.sleep(Duration.ofDays(30))` returns instantly. This is the killer feature for testing scheduled or long-running Workflows.

Two test shapes:

- **Workflow test** - register Workflow and (real or mocked) Activities, start the Workflow, assert. Time-skipped automatically when the Workflow sleeps or awaits.
- **Activity test** - test Activity logic in isolation as plain Java. No Temporal involved.

**Lab.**

```bash
scripts/run-example.sh testing
```

Read through `ReminderWorkflowTest`. Show the time-skip: a 24-hour reminder is asserted in milliseconds.

**Discussion prompt.** "How do you test that an Activity retries 3 times?" Two ways: (1) inject a mock Activity that throws twice; assert the final result. (2) use the env's `ActivityCompletionClient` to inspect execution. Both work; (1) is usually what you want.

**Common student error.** Calling `Thread.sleep` instead of `Workflow.sleep` in tests - tests run in real time and pass slowly, masking the time-skipping benefit.

### 8. Workflow replay testing - catching determinism regressions (35 min) `[lab]`

**Talking point.** Replay testing is the single safety net that prevents a code change from breaking Workflows already in flight. The mechanic:

1. Capture a real production Workflow's event history to JSON.
2. In CI, replay it against your *new* Workflow code using `WorkflowReplayer`.
3. If the new code makes a different decision than the recorded history, the replay throws and CI fails.

Run replay tests against a corpus of histories that represent the variations your code handles. New histories should be added every time you fix a bug or release a new feature.

**Landing example.**

```bash
scripts/show-example.sh 05-production/replay_test.java
```

The test is two lines: `WorkflowReplayer.replayWorkflowExecutionFromResource("histories/order-1001.json", OrderSagaWorkflowImpl.class)`. The complexity is curating the histories.

**Discussion prompt.** "You want to refactor a Workflow. How many histories should you have in the replay corpus?" At minimum: one happy path, one with each branch, one with each Signal path, one with each retry scenario. More if your Workflow has user-driven branching.

**How to capture histories.** `temporal workflow show --workflow-id X --output json > histories/X.json`. Strip secrets first.

### 9. Migrating Airflow DAGs - a decision framework (20 min) `[airflow]`

**Talking point.** Not every DAG should become a Temporal Workflow. Apply the framework before you commit migration effort:

| DAG shape                                                | Migration verdict                              |
| -------------------------------------------------------- | ---------------------------------------------- |
| Simple ETL on a fixed schedule, no human interaction     | Stay on Airflow or move to Schedules + Activities |
| Cross-system orchestration with retries and human steps  | Migrate; this is Temporal's sweet spot         |
| Kafka-triggered, one-execution-per-key                   | Migrate (Day 3 pattern)                        |
| Pure data transformation, no orchestration               | Don't migrate; this is Spark/dbt territory     |
| Long-running waits (hours, days, human approval)         | Migrate; Airflow handles this badly            |
| Tightly coupled to Airflow operators (Snowflake, BigQuery, etc.) | Wrap in Activities; treat the operator as the unit |

**The migration order that works:**

1. **Pick one DAG** that is annoying you in production (retry pain, manual recovery, scheduler drift).
2. **Map operators -> Activities** mechanically. Don't redesign the DAG yet.
3. **Run both side by side** for a release cycle. Compare outcomes.
4. **Cut over** when the Temporal version has been clean for two weeks.
5. **Redesign** only after the migration is stable. Now you can use Signals, Updates, Schedules to simplify.

**Discussion prompt.** "What's the smallest DAG in your stack? Walk me through migrating it." Have the cohort pick one; sketch operator-to-Activity mapping on the board. This is the bridge to the Day 5 capstone.

---

## Windows PowerShell shortcuts

```powershell
scripts/show-example.ps1 05-production/get_version_patch.java
scripts/show-example.ps1 05-production/versioning_behavior.java
scripts/show-example.ps1 05-production/worker_options_manual.java
scripts/show-example.ps1 05-production/worker_tuner.java
scripts/show-example.ps1 05-production/virtual_threads.java
scripts/show-example.ps1 05-production/micrometer_metrics.java
scripts/show-example.ps1 05-production/custom_activity_metric.java
scripts/show-example.ps1 05-production/replay_test.java
scripts/run-example.ps1 testing
scripts/test-runnable.ps1
```

---

## Instructor Notes

- Versioning is about preserving old histories, not just deploying new code. Pin to drain; auto-upgrade to evolve.
- Change-point names in `getVersion` are *identity*. Treat them like migration filenames - never recycle.
- Use separate Task Queues when workloads have meaningfully different latency or resource profiles. Same profile, same pool.
- `TestWorkflowEnvironment` gives deterministic time skipping without Docker. Tests for month-long Workflows still complete in milliseconds.
- Replay tests are the single safety net for Workflow code changes. Add a history every time you fix a bug.
- Migration framework lands the day: not everything is a Workflow. Pick fights where Temporal earns its keep.
