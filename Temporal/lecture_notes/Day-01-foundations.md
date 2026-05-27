# Day 1 - Foundations

## Objectives

- Reframe orchestration from DAG scheduling to durable execution.
- Introduce Workflows, Activities, Workers, Task Queues, and the Temporal Web UI.
- Explain deterministic replay and event history.
- Run the first Java Workflow locally.

## Lecture Flow

1. Airflow mental model versus Temporal mental model.
2. Core primitives: Workflow, Activity, Worker, Task Queue.
3. Event history and deterministic replay.
4. Local server and Web UI.
5. Run Hello Temporal.
6. Inspect Workflow history.

## Commands

Start Temporal if it is not already running:

```bash
scripts/start-temporal.sh
```

Show the Airflow DAG:

```bash
scripts/show-example.sh 01-foundations/airflow_dag_vs_temporal_workflow.py
```

Show the Temporal equivalent:

```bash
scripts/show-example.sh 01-foundations/airflow_dag_vs_temporal_workflow.java
```

Show the core primitives:

```bash
scripts/show-example.sh 01-foundations/core_primitives.java
```

Show unsafe replay code:

```bash
scripts/show-example.sh 01-foundations/deterministic_replay_bad.java
```

Show replay-safe code:

```bash
scripts/show-example.sh 01-foundations/deterministic_replay_good.java
```

Run Hello Temporal:

```bash
scripts/run-example.sh hello
```

Inspect the Workflow in the Web UI:

```text
http://127.0.0.1:8233/namespaces/default/workflows/hello-temporal-demo
```

Dump event history with Temporal CLI:

```bash
temporal workflow show --workflow-id hello-temporal-demo --output json
```

Or use the helper:

```bash
examples/01-foundations/history_cli.sh hello-temporal-demo
```

## Windows PowerShell

```powershell
scripts/start-temporal.ps1
scripts/show-example.ps1 01-foundations/airflow_dag_vs_temporal_workflow.py
scripts/show-example.ps1 01-foundations/airflow_dag_vs_temporal_workflow.java
scripts/run-example.ps1 hello
temporal workflow show --workflow-id hello-temporal-demo --output json
```

## Instructor Notes

- Emphasize that Workflow code re-executes during replay, while Activity results
  are recorded in history.
- Keep direct I/O, random values, system time, and network calls out of Workflow
  code.
- Use the Web UI event history to connect the Java method calls to recorded
  events.

