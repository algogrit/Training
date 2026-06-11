# Day 5 - Saga Pattern, Spring Boot & Capstone

## Objectives

- Implement a real-world order-processing Saga with explicit compensation.
- Choose between orchestration and choreography for distributed transactions.
- Wire Temporal into Spring Boot (manual + starter-style).
- Trigger Workflows from `@KafkaListener`s using `signalWithStart`.
- Use `startUpdateWithStart` for sync request/response APIs.
- Apply continue-as-new to keep long-running saga histories bounded.
- Run the capstone: redesign a Kafka-triggered Airflow DAG as a Temporal Saga.

## Prerequisites Check (5 min)

- Day 3's Kafka stack is still up.
- Spring Boot 3.x (or 2.x with `temporal-spring-boot-starter` 1.32.1).
- The runnable saga project compiles: `scripts/run-example.sh saga --dry-run` (or equivalent).

---

## Morning - 2 hrs

### 1. Real-world Workflow walkthrough - order processing Saga (35 min) `[lab]`

**Talking point.** A Saga is a long-running transaction where each step has an explicit **compensation** to undo it. Temporal turns Saga from a design pattern into ordinary Java code: register a compensation right after each forward step succeeds; on failure, run them in LIFO order.

The order Saga is the canonical demo: authorize payment, reserve inventory, ship. If shipping fails, restore inventory, cancel payment, notify the customer. No happy-path hopium; failure handling is the work.

**Landing example.**

```bash
scripts/show-example.sh 06-saga-spring/saga_compensation.java
```

Walk top to bottom:

1. Forward step succeeds.
2. Register the compensation **immediately**, before any other step runs.
3. On exception, `saga.compensate()` unwinds in reverse order.
4. The compensation Activities have their own retry policies - compensation itself can fail and retry.

**Lab.**

```bash
scripts/run-example.sh saga
```

Start a Workflow that will succeed; then start one that will fail at the shipping step (use a magic order ID like `fail-at-ship`). In the Web UI, point at the compensation Activities firing in reverse order.

**Discussion prompt.** "Authorizing payment succeeded but the response was lost; the Workflow times out and retries. Now you've authorized twice." Answer: idempotency on the Activity. The Activity must accept a stable key (orderId + step name) and dedupe at the payment provider. Compensation cancels by that same key.

### 2. Orchestration vs choreography (20 min)

**Talking point.** Two distributed-transaction styles:

- **Orchestration** - one central Workflow coordinates all steps and compensations. Easier to reason about, single audit trail, easier to monitor. **Temporal's natural shape.**
- **Choreography** - each service reacts to events and emits its own. No central state. More resilient to one team's outage, but state lives in your Grafana correlation IDs and tribal knowledge.

For most cohorts coming from Airflow + Kafka, the right answer is orchestration in Temporal for cross-team flows, with each team owning its Activities. Use choreography when the teams are strictly independent and the flow is unidirectional.

**Discussion prompt.** "When would you regret choreography?" War story: when the third team in the chain silently drops an event and nobody notices for 36 hours. The Workflow log would have shown it immediately.

### 3. Sync Saga via Update (25 min)

**Talking point.** When the caller needs the Saga's outcome synchronously - "did the order ship?" - Update is the right primitive. `startUpdateWithStart` creates the Workflow on first call and runs the update in one round trip, returning the result.

**Landing example.**

```bash
scripts/show-example.sh 06-saga-spring/sync_saga_update.java
```

Walk: `WithStartWorkflowOperation` describes the start; `startUpdateWithStart` runs both atomically; `update.getResult()` blocks until the update method returns.

**Discussion prompt.** "Your REST endpoint must respond in 500ms. The Saga takes 30 seconds. What do you do?" Don't use sync Update. Either return an order ID immediately (async Signal pattern) and let the caller poll, or split the Workflow so the synchronous part returns quickly and the rest runs asynchronously.

### 4. Async Saga via Signal (20 min)

**Talking point.** When the caller doesn't need the outcome synchronously - they just need to know the order was accepted - Signal is the right primitive. Pair it with `signalWithStart` so the first event creates the Workflow and later events signal the existing execution.

**Landing example.**

```bash
scripts/show-example.sh 06-saga-spring/async_saga_signal.java
```

Same `BatchRequest` pattern as Day 3's Kafka bridge. The order endpoint adds the start args and the initial signal, then fires.

**Discussion prompt.** "Sync vs async - which one matches your current REST/event model?" Most teams have both: a REST POST that returns 202 + an ID (async), and a status endpoint (Query). Show both side by side.

### 5. Spring Boot wiring (20 min)

**Talking point.** Two ways to wire Temporal into Spring Boot:

- **Manual `@Configuration`** - explicit `WorkflowServiceStubs`, `WorkflowClient`, `WorkerFactory` beans. Good for teaching; visible plumbing.
- **`temporal-spring-boot-starter`** - autoconfigured beans, properties-based config, automatic Worker registration. Production-preferred.

The starter (no `-alpha` suffix in SDK 1.24+) handles graceful shutdown, JMX, and metrics integration. For teaching today, walk the manual config so the wiring is visible; mention the starter is what you'd ship.

**Landing example.**

```bash
scripts/show-example.sh 06-saga-spring/spring_temporal_config.java
```

Three beans: `WorkflowServiceStubs`, `WorkflowClient`, `WorkerFactory` with `initMethod="start"` and `destroyMethod="shutdown"`. Stress: graceful shutdown matters - in-flight Activities should finish, not crash on container stop.

**Starter-equivalent properties** (mention, don't dwell):

```yaml
spring.temporal.connection.target: 127.0.0.1:7233
spring.temporal.namespace: default
spring.temporal.workers:
  - task-queue: orders
    workflow-classes: [com.example.OrderSagaWorkflowImpl]
```

### 6. Kafka listener as event-driven trigger (20 min)

**Talking point.** Bringing Day 3's `signalWithStart` pattern into Spring: a `@KafkaListener` consumes order events, looks up the Workflow by orderId, and signals-with-start. The bridge is now Spring-managed - no separate consumer process to deploy.

**Landing example.**

```bash
scripts/show-example.sh 06-saga-spring/kafka_listener_trigger.java
```

Same `BatchRequest` shape as the standalone bridge. Spring's `@KafkaListener` handles offset commit on method return - line up the commit with `signalWithStart` succeeding (don't return successfully if the signal failed).

**Common student error.** Default Spring Kafka commits on method return regardless of exception handling. Throwing from inside the listener triggers retry, but check your `KafkaListenerContainerFactory` for `AckMode.RECORD` vs `BATCH` - it matters when one record in a batch fails.

### 7. Continue-as-new - long-running sagas and history size limits (20 min)

**Talking point.** Workflow history has a hard limit (~50,000 events, ~50 MB recommended). For Workflows that loop forever (subscription cycles, monitoring, multi-year sagas), continue-as-new starts a fresh run with carryover state and the same Workflow ID.

The pattern:

1. Cap a loop counter or event count.
2. When the cap is reached, call `Workflow.continueAsNew(carryoverArgs)`.
3. The current run completes; a new run starts immediately with the carryover.
4. From outside, it's still "the same Workflow" by ID.

**Landing example.**

```bash
scripts/show-example.sh 06-saga-spring/continue_as_new.java
```

Walk: `eventCount >= 1000 -> continueAsNew(subscriptionId, 0)`. The 1000 is a heuristic - tune to your history size, not as a fixed number.

**Discussion prompt.** "What state do you carry over?" Only what the next run needs. Big working sets (collected events, partial results) should be persisted to a DB or S3 and read back, not carried in args. Continue-as-new is a *checkpoint*, not a memory dump.

---

## Break (15 min)

---

## Afternoon - 2 hrs - Capstone

### 8. Capstone - design and implement a transactional saga (75 min) `[lab]` `[airflow]` `[kafka]`

**The task.** Each student (or pair) picks a Kafka-triggered Airflow DAG from their own stack - or uses the provided template. Redesign it as a Temporal Saga in Spring Boot. Implement enough of it to demonstrate the failure path.

**The template** (for students without a DAG to bring):

> A customer signup flow. A Kafka event `customer-signup` arrives with `{userId, email, plan}`. The Airflow DAG runs four tasks: create user record, charge first month, provision tenant, send welcome email. Today, failure handling is ad hoc - the team has a runbook for "I think charge succeeded but provision failed."

**Deliverables (75 min budget):**

| Time      | Deliverable                                                                |
| --------- | -------------------------------------------------------------------------- |
| 0-10 min  | Sketch the Workflow signature, Activity interface, and compensation order on paper. |
| 10-50 min | Implement enough Java to compile and run the happy path + one failure path.|
| 50-65 min | Wire the Kafka trigger with `signalWithStart` (Spring `@KafkaListener` style). |
| 65-75 min | Run it end-to-end against the local stack; demo one compensation.          |

**Acceptance criteria.** A working `OrderSagaWorkflow`-style class with:

1. At least three forward steps.
2. Compensation registered immediately after each step.
3. A `@KafkaListener` triggering via `signalWithStart`.
4. One demonstrated failure -> compensation in the Web UI history.

**Common student traps.**

- Designing all Activities first, then realizing the Workflow shape doesn't fit. Sketch the Workflow first.
- Forgetting idempotency on forward steps. The retry will run them twice.
- Using `start` instead of `signalWithStart` in the listener. Second event for the same key crashes.
- Compensation Activities that fail and are caught silently - they need retry too.

### 9. Capstone review - compare implementations, discuss trade-offs (25 min)

**Format.** Two or three volunteer pairs share screen and walk their solution for 5 minutes each. The room critiques. Cover:

- How did they decide what was a Workflow vs an Activity?
- Where did they put idempotency keys?
- Did they use orchestration or choreography? Why?
- What would they change if the Saga had to run for 30 days?

**Instructor steering.** Resist correcting code style; focus on the four questions above. They're the ones the cohort will face on real systems.

### 10. Q&A and open migration planning - bring your own workflow (20 min)

**Format.** Open floor. Students bring real questions about their migration path. Use the framework from Day 4 (which DAGs to migrate, in what order) and steer toward concrete next steps.

**Anchor questions to ask if the room is quiet:**

- "Pick one Airflow DAG. What's the first thing that would break in Temporal?"
- "What's your team's hardest distributed-transaction failure today? Would a Saga have caught it?"
- "Where in your stack does 'I think it ran but I'm not sure' happen? That's a Workflow."

**Close.** Tomorrow's Day 6 covers AWS migration (Glue, Lambda, Step Functions) and containerized Worker deployment. Anyone with an EKS or Glue stack will see their patterns mapped.

---

## Windows PowerShell shortcuts

```powershell
scripts/show-example.ps1 06-saga-spring/saga_compensation.java
scripts/show-example.ps1 06-saga-spring/sync_saga_update.java
scripts/show-example.ps1 06-saga-spring/async_saga_signal.java
scripts/show-example.ps1 06-saga-spring/spring_temporal_config.java
scripts/show-example.ps1 06-saga-spring/kafka_listener_trigger.java
scripts/show-example.ps1 06-saga-spring/continue_as_new.java
scripts/run-example.ps1 saga
```

---

## Instructor Notes

- Compensation must be designed with business semantics, not generated mechanically. "Undo payment" is not the same as "refund" - business choice.
- Register compensations immediately after the forward step succeeds. Delay = lost compensation on crash.
- Sync Updates via `startUpdateWithStart` are the right tool when the caller needs the final business result.
- Signals via `signalWithStart` are the right tool for fire-and-forget event-driven interactions. **Never bare `start` from a Kafka listener.**
- Continue-as-new is a checkpoint, not a memory dump. Pass forward only what the next run needs.
- For the capstone: the goal is *exposure to the design choices*, not production-ready code. If a pair gets stuck on Spring config, unstick them - the lesson is in the saga shape, not the wiring.
