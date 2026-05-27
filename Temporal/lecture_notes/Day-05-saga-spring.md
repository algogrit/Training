# Day 5 - Saga and Spring

## Objectives

- Explain orchestration versus choreography for distributed transactions.
- Show explicit compensation ordering.
- Compare sync Update APIs and async Signal APIs.
- Show Spring Boot wiring and Kafka listener triggers.
- Explain continue-as-new for long-running sagas.

## Lecture Flow

1. Order-processing Saga: payment, inventory, shipment, notification.
2. Compensation stack and failure handling.
3. Sync Saga via Update.
4. Async Saga via Signal.
5. Spring `WorkflowClient`, `WorkerFactory`, and Activity beans.
6. Kafka listener as event-driven trigger.
7. Continue-as-new for history control.

## Commands

Show Saga compensation:

```bash
scripts/show-example.sh 06-saga-spring/saga_compensation.java
```

Show sync Saga Update:

```bash
scripts/show-example.sh 06-saga-spring/sync_saga_update.java
```

Show async Saga Signal:

```bash
scripts/show-example.sh 06-saga-spring/async_saga_signal.java
```

Show Spring Temporal configuration:

```bash
scripts/show-example.sh 06-saga-spring/spring_temporal_config.java
```

Show Kafka listener trigger:

```bash
scripts/show-example.sh 06-saga-spring/kafka_listener_trigger.java
```

Show continue-as-new:

```bash
scripts/show-example.sh 06-saga-spring/continue_as_new.java
```

Compile the runnable Saga example:

```bash
scripts/run-example.sh saga
```

## Windows PowerShell

```powershell
scripts/show-example.ps1 06-saga-spring/saga_compensation.java
scripts/show-example.ps1 06-saga-spring/spring_temporal_config.java
scripts/show-example.ps1 06-saga-spring/continue_as_new.java
scripts/run-example.ps1 saga
```

## Instructor Notes

- Compensation must be designed with business semantics, not generated
  mechanically.
- Register compensations immediately after the forward step succeeds.
- Sync Updates are useful when the caller needs the final business result.
- Signals are better for fire-and-forget event-driven interactions.

