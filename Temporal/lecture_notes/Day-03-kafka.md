# Day 3 - Kafka

## Objectives

- Clarify how Temporal and Kafka responsibilities differ.
- Show Kafka as an external event source that starts or Signals Workflows.
- Explain Kafka consumption inside Activities and offset commits.
- Demonstrate producer idempotence and outbox-style Activity design.
- Show partition fan-out and DLQ routing patterns.

## Lecture Flow

1. Temporal state machine versus Kafka event log.
2. Consumer bridge sends Signals to Workflows.
3. Kafka consumer inside an Activity.
4. Kafka producer from an Activity.
5. Outbox pattern for DB write plus event publication.
6. Partition fan-out and DLQ after Temporal retry exhaustion.

## Commands

Show the Signal bridge:

```bash
scripts/show-example.sh 04-kafka/signal_bridge.java
```

Show Kafka consumer Activity:

```bash
scripts/show-example.sh 04-kafka/kafka_consumer_activity.java
```

Show idempotent producer Activity:

```bash
scripts/show-example.sh 04-kafka/producer_activity_idempotent.java
```

Show outbox Activity:

```bash
scripts/show-example.sh 04-kafka/outbox_activity.java
```

Show partition fan-out:

```bash
scripts/show-example.sh 04-kafka/partition_fanout.java
```

Show DLQ routing:

```bash
scripts/show-example.sh 04-kafka/dlq_after_retry_exhaustion.java
```

Compile the runnable Kafka bridge project:

```bash
scripts/run-example.sh kafka
```

## Windows PowerShell

```powershell
scripts/show-example.ps1 04-kafka/signal_bridge.java
scripts/show-example.ps1 04-kafka/kafka_consumer_activity.java
scripts/show-example.ps1 04-kafka/producer_activity_idempotent.java
scripts/run-example.ps1 kafka
```

## Instructor Notes

- Kafka retains events; Temporal owns durable execution state.
- Prefer Signals when Kafka events are commands for a specific Workflow instance.
- Commit Kafka offsets only after the Activity has completed the unit of work.
- Temporal retry exhaustion and Kafka DLQs solve different problems; use DLQ
  routing when an event needs operator triage.

