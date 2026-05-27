# Day 2 - Reliability

## Objectives

- Explain asynchronous and parallel Activity execution.
- Demonstrate Activity retries, timeouts, and heartbeats.
- Show cancellation scopes and partial failure handling.
- Reinforce deterministic time APIs.

## Lecture Flow

1. Activity stubs and `Async.function`.
2. Fan-out/fan-in with `Promise.allOf`.
3. Partial failure and retry exhaustion.
4. Timeout selection: `startToCloseTimeout` versus `scheduleToCloseTimeout`.
5. Heartbeating long-running Activities.
6. Replay-safe time and timers.

## Commands

Show async Activity invocation:

```bash
scripts/show-example.sh 02-reliability/async_activity.java
```

Show parallel fan-out:

```bash
scripts/show-example.sh 02-reliability/parallel_fanout_allof.java
```

Show partial failure handling:

```bash
scripts/show-example.sh 02-reliability/partial_failure.java
```

Show cancellation:

```bash
scripts/show-example.sh 02-reliability/cancellation_scope.java
```

Show retries and timeouts:

```bash
scripts/show-example.sh 02-reliability/retry_and_timeouts.java
```

Show heartbeat handling:

```bash
scripts/show-example.sh 02-reliability/heartbeat_long_activity.java
```

Show replay-safe time:

```bash
scripts/show-example.sh 02-reliability/workflow_time.java
```

Run the async and parallel Activity example:

```bash
scripts/run-example.sh async
```

## Windows PowerShell

```powershell
scripts/show-example.ps1 02-reliability/async_activity.java
scripts/show-example.ps1 02-reliability/parallel_fanout_allof.java
scripts/show-example.ps1 02-reliability/heartbeat_long_activity.java
scripts/run-example.ps1 async
```

## Instructor Notes

- Explain that `Promise.get()` blocks Workflow logic, not an operating-system
  worker thread.
- Heartbeats are the recovery checkpoint for long Activities.
- Timeouts should describe the real operational contract, not just an arbitrary
  default.

