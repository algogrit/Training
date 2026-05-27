# Day 4 - Production

## Objectives

- Teach safe Workflow versioning.
- Discuss worker sizing, Task Queue design, and virtual threads.
- Introduce metrics and replay testing.
- Explain namespace strategy.
- Run in-process Workflow tests.

## Lecture Flow

1. Why changing Workflow code can break replay.
2. `Workflow.getVersion` patching.
3. Versioning behavior annotations.
4. Worker options and resource-based tuning.
5. Metrics with Micrometer and custom Activity metrics.
6. Replay tests and in-process tests.
7. Namespace strategy.

## Commands

Show patching with `Workflow.getVersion`:

```bash
scripts/show-example.sh 05-production/get_version_patch.java
```

Show versioning behavior:

```bash
scripts/show-example.sh 05-production/versioning_behavior.java
```

Show manual worker options:

```bash
scripts/show-example.sh 05-production/worker_options_manual.java
```

Show resource-based tuning:

```bash
scripts/show-example.sh 05-production/worker_tuner.java
```

Show virtual threads:

```bash
scripts/show-example.sh 05-production/virtual_threads.java
```

Show Micrometer wiring:

```bash
scripts/show-example.sh 05-production/micrometer_metrics.java
```

Show a custom Activity metric:

```bash
scripts/show-example.sh 05-production/custom_activity_metric.java
```

Show replay testing:

```bash
scripts/show-example.sh 05-production/replay_test.java
```

Show namespace strategy:

```bash
scripts/show-example.sh 05-production/namespace_strategy.md
```

Run the in-process testing example:

```bash
scripts/run-example.sh testing
```

Compile all runnable examples:

```bash
scripts/test-runnable.sh
```

## Windows PowerShell

```powershell
scripts/show-example.ps1 05-production/get_version_patch.java
scripts/show-example.ps1 05-production/worker_tuner.java
scripts/show-example.ps1 05-production/replay_test.java
scripts/run-example.ps1 testing
scripts/test-runnable.ps1
```

## Instructor Notes

- Versioning is about preserving old histories, not only deploying new code.
- Use separate Task Queues when workloads have meaningfully different latency or
  resource profiles.
- `TestWorkflowEnvironment` gives deterministic time skipping without Docker.

