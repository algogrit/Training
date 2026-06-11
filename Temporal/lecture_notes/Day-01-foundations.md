# Day 1 - Foundations

## Objectives

- Reframe orchestration from DAG scheduling to durable execution.
- Introduce Workflows, Activities, Workers, Task Queues, and the Temporal Web UI.
- Explain the Temporal cluster architecture and how state is persisted.
- Explain deterministic replay and event history.
- Run the first Java Workflow locally and read its history.

## Prerequisites Check (5 min)

Before the morning starts, confirm every laptop has:

- JDK 17+ (`java -version`)
- Maven 3.9+ (`mvn -v`)
- Docker Desktop running (`docker ps`)
- Temporal CLI on `PATH` (`temporal --version`)
- The repo cloned and `scripts/check-local.sh` (or `.ps1` on Windows) green

If a student is red on any of these, pair them with a neighbour for the day and resolve at the first break. Do not block the cohort.

---

## Morning - 2 hrs

### 1. Why Temporal exists - the failure modes of cron-based orchestration (30 min)

**Talking point.** Airflow (and any cron/DAG scheduler) treats orchestration as *scheduling work*. The scheduler decides when a task runs; the operator owns recovery when it fails halfway. State lives in three places that drift apart: the scheduler's metadata DB, the task's checkpoints (S3/XCom), and operator memory.

Temporal treats orchestration as *durable application code*. The Workflow is a Java function whose state is the event history. There is no "retry the failed task" button to press, because the Workflow is the same long-running function from the worker's perspective - it resumes from the next decision after a crash.

**Landing example.**

```bash
scripts/show-example.sh 01-foundations/airflow_dag_vs_temporal_workflow.py
scripts/show-example.sh 01-foundations/airflow_dag_vs_temporal_workflow.java
```

Walk through both files side by side. The Python DAG declares *shape*; the Java Workflow declares *behavior*. Point out that XCom (line: `context["ti"].xcom_pull(...)`) is replaced by a normal Java return value - because the Workflow owns the variable, not the scheduler.

**Discussion prompt.** "Where in your current Airflow stack do you check 'did the task actually run?' If your scheduler died for 10 minutes, what is your recovery procedure?" Aim for war stories - this is the moment the cohort buys in.

**Common student question.** "Doesn't Airflow have a database too?" Yes, but Airflow's DB stores *scheduler decisions* ("this task ran at 03:14"). Temporal's DB stores *the application's history of events* - which is enough to reconstruct the running program.

### 2. Core concepts - Workflows, Activities, Workers, Task Queues (30 min)

**Talking point.** Four primitives, mapped to the audience's Airflow vocabulary:

| Airflow                        | Temporal                                 | Why it matters                                                |
| ------------------------------ | ---------------------------------------- | ------------------------------------------------------------- |
| DAG                            | Workflow (`@WorkflowInterface`)          | Durable, deterministic Java code                              |
| Operator / task                | Activity (`@ActivityInterface`)          | Unreliable work; retried independently                        |
| Worker / executor              | Worker process polling a Task Queue      | Long-lived JVM, not a per-task container                      |
| `default_queue`, `KubernetesQueue` | Task Queue (string name)             | Routes work to a Worker pool; the unit of capacity            |

Stress: **the Task Queue is not a topic**. It is not durable storage of events. It is a routing key that says "any Worker polling this name can take this task."

**Landing example.**

```bash
scripts/show-example.sh 01-foundations/core_primitives.java
```

Show all four roles in one file. Spend time on `WorkerFactory.newInstance(client).newWorker("loan-decisions")` - that string is the Task Queue.

**Discussion prompt.** "Today, how do you decide which Airflow worker runs which task?" Lead them to see Task Queues as the same concept, but explicit and code-owned rather than queue-name-by-convention.

### 3. Event sourcing and deterministic replay (30 min)

**Talking point.** The Temporal server stores **events**, not snapshots. When a Worker picks up a Workflow, it re-runs the Workflow code from the start and replays the events to rebuild local state. This is why Workflow code must be **deterministic**: any side effect that could differ between executions (random, system clock, file I/O, network) would produce a different decision and a different history, and the server would reject it.

This is the single concept that breaks the most Airflow brains. Spend time here.

**Landing examples.**

```bash
scripts/show-example.sh 01-foundations/deterministic_replay_bad.java
scripts/show-example.sh 01-foundations/deterministic_replay_good.java
```

Walk line by line:

- `System.currentTimeMillis()` -> `Workflow.currentTimeMillis()` (recorded as a marker event)
- `new Random()` -> `Workflow.newRandom()` (seeded from history)
- `Files.writeString(...)` in Workflow -> move I/O into an Activity (Activity *results* are recorded, the *call* isn't re-executed)

**Discussion prompt.** "If a Workflow calls `Math.random()`, what happens during replay?" Answer: a different value. Show that this would break the workflow's decision tree and cause a non-determinism error - Temporal's safety net, but one you really want to avoid hitting in prod.

### 4. Temporal architecture overview (30 min)

**Talking point.** Four services on the cluster, one persistence backend:

- **Frontend** - gRPC API the SDK talks to. Stateless. Authn/authz lives here.
- **History** - source of truth for Workflow state machines. Owns event history.
- **Matching** - dispatches tasks to Workers polling Task Queues. The "queue" of "Task Queue" lives here in memory; durability is via History.
- **Worker (system)** - Temporal's own internal Workflows (visibility indexing, archival). Different from *your* Workers.
- **Persistence** - PostgreSQL, MySQL, or Cassandra. PostgreSQL in the local dev stack.

Draw it on the board. Trace one Workflow start: SDK -> Frontend -> History (write `WorkflowExecutionStarted`) -> Matching (enqueue WorkflowTask) -> *your* Worker (poll, execute, return decisions) -> Frontend -> History (write Activity scheduled, etc.).

**Local stack mapping.** The local Docker Compose uses `temporalio/auto-setup` which bundles Frontend + History + Matching + internal Worker in one container, plus PostgreSQL alongside. That's three containers total for the dev loop.

**Discussion prompt.** "If the History service goes down mid-Workflow, what happens to a running Workflow on a Worker?" Answer: the Worker can't write its next decision, so the WorkflowTask retries until History comes back. The Workflow is paused, not lost. This is the durability promise made concrete.

---

## Break (15 min)

---

## Afternoon - 2 hrs

### 5. Local dev setup - Temporal CLI, Docker Compose, Web UI (20 min) `[lab]`

**Talking point.** Two ways to run Temporal locally; both should work on every laptop.

- `temporal server start-dev` - single binary, in-memory persistence, fastest to start. Good for solo demos.
- The Docker Compose stack in `scripts/start-temporal.sh` - PostgreSQL-backed, Web UI on `http://127.0.0.1:8233`, closer to production shape.

**Lab steps.**

```bash
scripts/start-temporal.sh
open http://127.0.0.1:8233   # macOS; Linux: xdg-open, Windows: start
```

Have students confirm they can:

1. See the `default` namespace in the Web UI.
2. List namespaces from the CLI: `temporal operator namespace list`.
3. Stop and restart the stack and observe that `default` survives (it's in PostgreSQL).

**Common student errors.**

- Port 7233 or 8233 already taken (often by an old `temporal server start-dev` left running). Kill it.
- Docker Desktop not running - `scripts/check-local.sh` catches this; if skipped, the error is opaque.
- On Windows, antivirus quarantining the `temporal.exe` binary. Whitelist it.

### 6. First Workflow in Java - "Hello Temporal" (45 min) `[lab]`

**Talking point.** Run the canonical first program. The point isn't the code - it's connecting the Java method call to the Web UI event history.

**Lab steps.**

```bash
scripts/run-example.sh hello
```

In a second terminal, watch the Web UI fill in. Walk students through:

1. Click into the new Workflow execution.
2. Open the **Event History** tab. Identify `WorkflowExecutionStarted`, `ActivityTaskScheduled`, `ActivityTaskCompleted`, `WorkflowExecutionCompleted`.
3. Click into the Activity completion event - show the recorded result.

**Discussion prompt.** "Restart the Worker mid-Workflow. What do you see in the history?" Have one student do it live. Workflow resumes; history shows no gap. This is the lesson.

**Common student errors.**

- Worker not started, or started against a different Task Queue name. Workflow stays in `Running` state with no progress. Fix: confirm the Task Queue string matches between client and Worker.
- `WorkflowExecutionAlreadyStarted` if they re-run without changing the Workflow ID. Acknowledge it, explain it's a *feature* (Workflow IDs are durable identity), and have them pick a new ID.

### 7. Reading the Event History (35 min) `[lab]`

**Talking point.** Workflow Event History is the audit trail. Every long-running production Temporal debugging session starts here. Two views: Web UI (human) and CLI/JSON (machine, replay tests).

**Lab steps.**

```bash
examples/01-foundations/history_cli.sh hello-temporal-demo
temporal workflow show --workflow-id hello-temporal-demo --output json | jq '.events[].eventType'
```

Have students explain what each event type means. Specifically point out:

- `WorkflowTaskScheduled` / `WorkflowTaskStarted` / `WorkflowTaskCompleted` - the *decision* loop. Your Workflow code ran once between these.
- `ActivityTaskScheduled` - the Workflow asked for an Activity.
- `ActivityTaskCompleted` - the Worker reported back. **The Workflow does not re-execute the Activity on replay; this recorded result is reused.**

**Discussion prompt.** "What would the history look like if the Activity failed twice and succeeded on the third attempt?" Answer: three `ActivityTaskScheduled` + two `ActivityTaskFailed` + one `ActivityTaskCompleted`. Show one if you have a failing example handy.

**Bridge to Day 2.** "Tomorrow we make Activities deliberately fail and survive it."

---

## Windows PowerShell shortcuts

```powershell
scripts/check-local.ps1
scripts/start-temporal.ps1
scripts/show-example.ps1 01-foundations/airflow_dag_vs_temporal_workflow.py
scripts/show-example.ps1 01-foundations/airflow_dag_vs_temporal_workflow.java
scripts/show-example.ps1 01-foundations/deterministic_replay_bad.java
scripts/show-example.ps1 01-foundations/deterministic_replay_good.java
scripts/run-example.ps1 hello
temporal workflow show --workflow-id hello-temporal-demo --output json
```

---

## Instructor Notes

- The single sentence to leave the room with: **"Workflow code re-executes during replay. Activity results are recorded in history."** Repeat it three times across the day.
- Keep direct I/O, random values, system time, and network calls out of Workflow code - the determinism rule.
- Use the Web UI event history to physically *point at* the connection between Java method calls and recorded events. Don't just describe it.
- Resist showing Signals/Queries/Updates today. They are Day 2; mentioning them now creates more questions than the cohort can hold.
- If a student asks "what's the difference between this and Step Functions?" the short answer is: Step Functions states are JSON; Workflow states are application code. Save the long answer for Day 6.
